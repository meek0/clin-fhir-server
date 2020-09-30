package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.TransactionDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.context.ApplicationContext;

@Interceptor
public class IndexerInterceptor {
    private final ElasticsearchRestClient client;

    public IndexerInterceptor(ApplicationContext appContext) {
        // Autowired doesn't work, so we need the ApplicationContext to get the bean.
        this.client = appContext.getBean(ElasticsearchRestClient.class);
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
    public void resourceCreated(IBaseResource resource) {
        if (resource instanceof Patient) {
            client.index("test", (Resource) resource);
        }
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED)
    public void resourceUpdated(IBaseResource oldResource, IBaseResource newResource) {
        if (newResource instanceof Patient) {
            client.index("test", (Resource) newResource);
        }
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_DELETED)
    public void resourceDeleted(IBaseResource resource, RequestDetails requestDetails) {
        if (resource instanceof Patient) {
            client.delete("test", (Resource) resource);
        }
    }
}
