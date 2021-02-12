package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.ElasticsearchRestClient.IndexData;
import bio.ferlab.clin.es.IndexerHelper;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.PatientIdExtractor;
import bio.ferlab.clin.es.PatientDataBuilder;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Set;

/**
 * Intercepts requests CREATE/UPDATE/DELETE requests, and index patient data to ES when needed.
 * Subscription couldn't be used in this scenario, as they do not offer a way to handle deletions and to filter out
 * unnecessary attributes.
 */
@Interceptor
public class IndexerInterceptor {
    private static final String INDEX_PATIENT = HapiProperties.getBioEsIndexPatient();

    private final PatientIdExtractor patientIdExtractor;
    private final PatientDataBuilder patientDataBuilder;
    private final ElasticsearchRestClient client;
    private final JsonGenerator jsonGenerator;

    public IndexerInterceptor(ApplicationContext appContext) {
        this.patientIdExtractor = appContext.getBean(PatientIdExtractor.class);
        this.patientDataBuilder = appContext.getBean(PatientDataBuilder.class);
        this.client = appContext.getBean(ElasticsearchRestClient.class);
        this.jsonGenerator = appContext.getBean(JsonGenerator.class);
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_DELETED)
    public void resourceDeleted(IBaseResource resource) {
        if (resource instanceof Patient) {
            client.delete(INDEX_PATIENT, resource.getIdElement().getIdPart());
        }
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean response(RequestDetails requestDetails) {
        if (requestDetails.getRequestType() == RequestTypeEnum.POST || requestDetails.getRequestType() == RequestTypeEnum.PUT) {
            final IBaseResource processedResource = requestDetails.getResource();
            if (IndexerHelper.isIndexable(processedResource)) {
                final Set<String> ids = this.patientIdExtractor.extract(processedResource);
                if (ids != null) {
                    final List<PatientData> patientDataList = this.patientDataBuilder.fromIds(ids);
                    patientDataList.forEach(System.out::println);

                    patientDataList.forEach(patientData -> {
                        final IndexData data = new IndexData(patientData.getId(), jsonGenerator.toString(patientData));
                        client.index(INDEX_PATIENT, data);
                    });
                }
            }
        }
        return true;
    }
}
