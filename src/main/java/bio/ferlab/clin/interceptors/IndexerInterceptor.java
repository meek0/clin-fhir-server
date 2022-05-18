package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.indexer.LegacyIndexer;
import bio.ferlab.clin.es.indexer.NanuqIndexer;
import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.es.ElasticsearchRestClient;
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
    private final LegacyIndexer legacyIndexer;
    private final NanuqIndexer nanuqIndexer;

    public IndexerInterceptor(ElasticsearchRestClient client,
                              BioProperties bioProperties, 
                              LegacyIndexer legacyIndexer,
                              NanuqIndexer nanuqIndexer) {
        this.client = client;
        this.bioProperties = bioProperties;
        this.legacyIndexer = legacyIndexer;
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
        if (bioProperties.isNanuqEnabled()) {
            this.nanuqIndexer.index(requestDetails);
        } else {
            this.legacyIndexer.index(requestDetails);
        }
        return true;
    }
}
