package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.ElasticsearchRestClient.IndexData;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.builder.PatientDataBuilder;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.context.ApplicationContext;

@Interceptor
public class IndexerInterceptor {
    private static final String INDEX_PATIENT = HapiProperties.getBioEsIndexPatient();

    private final ElasticsearchRestClient client;
    private final PatientDataBuilder patientDataBuilder;
    private final JsonGenerator parser;

    public IndexerInterceptor(ApplicationContext appContext) {
        this.client = appContext.getBean(ElasticsearchRestClient.class);
        this.patientDataBuilder = appContext.getBean(PatientDataBuilder.class);
        this.parser = appContext.getBean(JsonGenerator.class);
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
    public void resourceCreated(IBaseResource resource, RequestDetails requestDetails) {
        this.indexToEs(resource, requestDetails);
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED)
    public void resourceUpdated(IBaseResource oldResource, IBaseResource newResource, RequestDetails requestDetails) {
        this.indexToEs(newResource, requestDetails);
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_DELETED)
    public void resourceDeleted(IBaseResource resource, RequestDetails requestDetails) {
        if (resource instanceof Patient) {
            client.delete(INDEX_PATIENT, resource.getIdElement().getIdPart());
        }
    }

    private void indexToEs(IBaseResource resource, RequestDetails requestDetails) {
        if (resource instanceof Patient) {
            final PatientData patientData = this.patientDataBuilder.fromJson(requestDetails.getRequestContentsIfLoaded());
            if (patientData != null) {
                final IndexData data = new IndexData(resource.getIdElement().getIdPart(), parser.toString(patientData));
                client.index(INDEX_PATIENT, data);
            }
        }
    }
}
