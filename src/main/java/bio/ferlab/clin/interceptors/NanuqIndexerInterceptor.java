package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.indexer.NanuqIndexer;
import bio.ferlab.clin.es.indexer.PatientIndexer;
import bio.ferlab.clin.es.indexer.ServiceRequestIndexer;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Service;

@Interceptor
@Service
public class NanuqIndexerInterceptor {
    private final ElasticsearchRestClient client;
    private final BioProperties bioProperties;
    private final NanuqIndexer nanuqIndexer;
    
    public NanuqIndexerInterceptor(ElasticsearchRestClient client,
                                   BioProperties bioProperties,
                                   NanuqIndexer nanuqIndexer) {
        this.client = client;
        this.bioProperties = bioProperties;
        this.nanuqIndexer = nanuqIndexer;
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
        this.nanuqIndexer.index(requestDetails);
        return true;
    }
}
