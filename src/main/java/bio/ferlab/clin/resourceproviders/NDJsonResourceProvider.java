package bio.ferlab.clin.resourceproviders;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NDJsonResourceProvider {

    private final Logger logger = LoggerFactory.getLogger(NDJsonResourceProvider.class);

    private ApplicationContext applicationContext;
    private FhirContext fhirContext;
    private IParser parser;
    private AmazonS3 s3Client;

    public NDJsonResourceProvider(ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
        this.fhirContext = this.applicationContext.getBean(FhirContext.class);

        parser = this.fhirContext.newJsonParser();

        BasicAWSCredentials credentials = new BasicAWSCredentials(HapiProperties.getS3AccessKey(), HapiProperties.getS3SecretKey());

        s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(HapiProperties.getS3ServiceEndpoint(), HapiProperties.getS3SigninRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

    }

    //idempotent = true : accept GET
    @Operation(name="$s3export", idempotent = true, manualResponse = true)
    public void export(@OperationParam(name = "_entity",min = 1,max = 1,typeName = "string") IPrimitiveType<String> entity,
                       @OperationParam(name = "_multipart", min = 0, max = 1,typeName = "boolean") IPrimitiveType<Boolean> multipart,
                       @OperationParam(name = "_async", min = 0, max = 1,typeName = "boolean") IPrimitiveType<Boolean> async,
                       HttpServletResponse response) throws Exception {


        //Dynamically find the model class for the version of HAPI Fhir defined in the hapi.properties
        String modelPackage = "org.hl7.fhir." + HapiProperties.getFhirVersion().name().toLowerCase() + ".model.";
        Class<IDomainResource> resourceClass = (Class<IDomainResource>)Class.forName(modelPackage + entity);

        DaoRegistry daoRegistry = applicationContext.getBean(DaoRegistry.class);

        IFhirResourceDao dao = daoRegistry.getResourceDao(resourceClass);
        SearchParameterMap searchCriteria = SearchParameterMap.newSynchronous();

        IBundleProvider bundleProvider = dao.search(searchCriteria);
        if(bundleProvider != null && bundleProvider.size() != null && bundleProvider.size() > 0){
            // Launch in new Thread and process in background in order to prevent http timeouts
            List<IBaseResource> resources = bundleProvider.getResources(0, bundleProvider.size());

            if(resources != null && resources.size() > 0){
                createBucketIfRequired();

                if(async == null || async.getValue().booleanValue()){
                    (new Thread(() -> {
                        try {
                            upload(resources, entity, multipart);
                        } catch (Exception e) {
                            // No need to rethrow... since it's async, it's already too late.
                            // HTTP response was already sent back.
                            logger.error("Failed exporting entities of type " + entity);
                        }
                    })).start();
                }else{
                    upload(resources, entity, multipart);
                }
            }


        }

        response.setContentType("text/plain");
        response.setStatus(HttpStatus.SC_OK);
    }

    private void upload(List<IBaseResource> resources, IPrimitiveType<String> entity, IPrimitiveType<Boolean> multipart) throws Exception {
        if(multipart == null || !multipart.getValue().booleanValue()){
            String ndjson = String.join(
                    "\n",
                    resources.stream().map(res -> this.parser.encodeResourceToString(res)).collect(Collectors.toList())
            );
            byte[] ndjsonBytes = ndjson.getBytes(StandardCharsets.UTF_8);

            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType("application/json; charset=utf-8");
            meta.setContentEncoding("utf-8");
            meta.setContentLength(ndjsonBytes.length);

            s3Client.putObject(
                    HapiProperties.getNdjsonExportS3Bucket(),
                    StringUtils.appendIfMissing(HapiProperties.getNdjsonExportS3Prefix(),"/") + entity.getValue() + ".json",
                    new ByteArrayInputStream(ndjsonBytes),
                    meta);
        }else{
            multipartUpload(entity.getValue(), resources);
        }
    }

    /**
     * For larger exports.
     * @param entity
     * @param resources
     */
    private void multipartUpload(String entity, List<IBaseResource> resources) throws Exception {

        String tmpFilePath = StringUtils.appendIfMissing(System.getProperty("java.io.tmpdir"), File.separator);
        tmpFilePath += UUID.randomUUID().toString();

        try(Writer fw = new FileWriter(tmpFilePath, false);) {
            for(IBaseResource res : resources){
                this.parser.encodeResourceToWriter(res, fw);
            }
            fw.flush();
        }

        TransferManager tm = TransferManagerBuilder
                .standard()
                .withS3Client(s3Client)
                .withMultipartUploadThreshold((long) (5 * 1024 * 1025)) //5mb
                .build();

        File tmpFile = new File(tmpFilePath);

        Upload upload = tm.upload(
                HapiProperties.getNdjsonExportS3Bucket(),
                StringUtils.appendIfMissing(HapiProperties.getNdjsonExportS3Prefix(),"/") + entity + ".json",
                tmpFile
        );

        try {
            upload.waitForCompletion();
        } catch (AmazonClientException | InterruptedException e) {
            logger.error("Upload failed.", e);
            throw e;
        } finally{
            tmpFile.delete();
        }
    }

    private void createBucketIfRequired() {
        if (!s3Client.doesBucketExistV2(HapiProperties.getNdjsonExportS3Bucket())) {
            // Because the CreateBucketRequest object doesn't specify a region, the
            // bucket is created in the region specified in the client.
            s3Client.createBucket(new CreateBucketRequest(HapiProperties.getNdjsonExportS3Bucket()));
        }
    }

}
