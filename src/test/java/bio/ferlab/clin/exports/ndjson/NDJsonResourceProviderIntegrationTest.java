package bio.ferlab.clin.exports.ndjson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.test.utilities.JettyUtil;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.*;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("NDJsonResourceProvider::Test upload to S3")
public class NDJsonResourceProviderIntegrationTest {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NDJsonResourceProviderIntegrationTest.class);

    //MINIO
    private static GenericContainer minioServer;
    private static String minioServerUrl;

    //HAPI FHIR
    private static IGenericClient fhirClient;
    private static FhirContext fhirCtx;
    private static Server fhirServer;

    //S3 Client
    private static final BasicAWSCredentials credentials = new BasicAWSCredentials(HapiProperties.getS3AccessKey(), HapiProperties.getS3SecretKey());
    private static AmazonS3 s3Client;

    static {
        HapiProperties.forceReload();

        // MINIO
        int port = 9000;

        minioServer = new GenericContainer("minio/minio")
                .withEnv("MINIO_ACCESS_KEY", HapiProperties.getS3AccessKey())
                .withEnv("MINIO_SECRET_KEY", HapiProperties.getS3SecretKey())
                .withCommand("server /data")
                .withExposedPorts(port)
                .waitingFor(new HttpWaitStrategy()
                        .forPath("/minio/health/live")
                        .forPort(port)
                        .withStartupTimeout(Duration.ofSeconds(50)));
        minioServer.start();

        Integer mappedPort = minioServer.getFirstMappedPort();
        Testcontainers.exposeHostPorts(mappedPort);
        try {
            minioServerUrl = String.format("http://%s:%s", Inet4Address.getLocalHost().getHostAddress(), mappedPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(minioServerUrl, HapiProperties.getS3SigninRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

        HapiProperties.setProperty(HapiProperties.S3_SERVICE_ENDPOINT, minioServerUrl);

        fhirCtx = FhirContext.forR4();
    }

    @BeforeAll
    static void setUp() throws Exception {
        HapiProperties.setProperty(HapiProperties.S3_SERVICE_ENDPOINT, minioServerUrl);

        // HAPI FHIR
        int fhirPort = 8888;
        String path = Paths.get("").toAbsolutePath().toString();

        LOGGER.info("Project base path is: {}", path);

        fhirServer = new Server(fhirPort);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/hapi-fhir-jpaserver");
        webAppContext.setDisplayName("HAPI FHIR");
        webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
        webAppContext.setResourceBase(path + "/target/hapi");
        webAppContext.setParentLoaderPriority(true);

        fhirServer.setHandler(webAppContext);
        fhirServer.start();

        fhirPort = JettyUtil.getPortForStartedServer(fhirServer);

        fhirCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        fhirCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
        String fhirServerBase = "http://localhost:" + fhirPort + "/hapi-fhir-jpaserver/fhir/";

        fhirClient = fhirCtx.newRestfulGenericClient(fhirServerBase);

        loadPatients();
    }


    private static void loadPatients() throws InterruptedException {
        Date bday = DateUtils.addDays(new Date(), -8000);

        ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for(int j=0; j<10; j++){
            WORKER_THREAD_POOL.submit(() ->{
                for(int i=0; i<15; i++){
                    Patient pt = new Patient();
                    pt.addIdentifier()
                            .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                            .setValue(String.valueOf(i));
                    pt.setBirthDate(bday);
                    pt.setActive(true);
                    pt.addName().setFamily("LastName" + i).addGiven("GivenName" + i);
                    pt.setIdElement(IdType.newRandomUuid());
                    pt.setGender(i % 2 == 0 ? Enumerations.AdministrativeGender.MALE: Enumerations.AdministrativeGender.FEMALE);
                    pt.setId(IdType.newRandomUuid());

                    IIdType id = fhirClient.create().resource(pt).execute().getId();
                    LOGGER.info("Patient created with id : " + id.getValue());
                }

                latch.countDown();
            });
        }

        latch.await();
    }

    @Test
    @DisplayName("Should write patient NDJson to s3")
    public void shouldExportPatientsToObjectStore() throws Exception{
        Parameters outParams = fhirClient.operation()
                .onServer()
                .named("$s3export?_entity=Patient&_async=false")
                .withNoParameters(Parameters.class)
                .useHttpGet()
                .execute();

        Thread.sleep(5000);

        ObjectMetadata meta = s3Client.getObjectMetadata(HapiProperties.getNdjsonExportS3Bucket(), HapiProperties.getNdjsonExportS3Prefix() + "/Patient.json");
        Assertions.assertNotNull(meta);
        Assertions.assertTrue(meta.getContentLength() > 0);
    }

    @Test
    @DisplayName("Should write patient NDJson to s3 using multipart")
    public void shouldExportPatientsToObjectStoreMultipart() throws Exception{
        Parameters outParams = fhirClient.operation()
                .onServer()
                .named("$s3export?_entity=Patient&_multipart=true&_async=false")
                .withNoParameters(Parameters.class)
                .useHttpGet()
                .execute();

        Thread.sleep(5000);

        ObjectMetadata meta = s3Client.getObjectMetadata(HapiProperties.getNdjsonExportS3Bucket(), HapiProperties.getNdjsonExportS3Prefix() + "/Patient.json");
        Assertions.assertNotNull(meta);
        Assertions.assertTrue(meta.getContentLength() > 0);
    }

    @AfterAll
    static void shutDown() throws Exception{
        if (minioServer !=null && minioServer.isRunning()) {
            minioServer.stop();
        }
        if(fhirServer != null && fhirServer.isRunning()) {
            fhirServer.stop();
        }
    }

    public static void main(String[] args) throws Exception{
        setUp();
    }
}
