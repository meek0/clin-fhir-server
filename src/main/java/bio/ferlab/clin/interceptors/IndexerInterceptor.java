package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.indexer.PatientIndexer;
import bio.ferlab.clin.es.indexer.ServiceRequestIndexer;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Service;

/**
 * Intercepts requests CREATE/UPDATE/DELETE requests, and index patient data to ES when needed.
 * Subscription couldn't be used in this scenario, as they do not offer a way to handle deletions and to filter out
 * unnecessary attributes.
 */
@Interceptor
@Service
public class IndexerInterceptor {
    private final ElasticsearchRestClient client;
    private final BioProperties bioProperties;
    private final PatientIndexer patientIndexer;
    private final ServiceRequestIndexer serviceRequestIndexer;

    public IndexerInterceptor(ElasticsearchRestClient client,
                              BioProperties bioProperties, PatientIndexer patientIndexer, ServiceRequestIndexer serviceRequestIndexer) {
        this.client = client;
        this.bioProperties = bioProperties;
        this.patientIndexer = patientIndexer;
        this.serviceRequestIndexer = serviceRequestIndexer;
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_DELETED)
    public void resourceDeleted(IBaseResource resource) {
        if (resource instanceof Patient) {
            client.delete(bioProperties.getEsPatientsIndex(), resource.getIdElement().getIdPart());
        } else if (resource instanceof ServiceRequest) {
            client.delete(bioProperties.getEsPrescriptionsIndex(), resource.getIdElement().getIdPart());
        }
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean response(RequestDetails requestDetails) {
        this.patientIndexer.index(requestDetails);
        this.serviceRequestIndexer.index(requestDetails);
        return true;
    }
}
