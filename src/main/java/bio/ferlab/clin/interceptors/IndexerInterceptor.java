package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.ElasticsearchData;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.TransactionDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;

@Interceptor
public class IndexerInterceptor {
    @Autowired
    ElasticsearchData elasticSearchData;


    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
    public void resourceCreated(IBaseResource baseResource,
                                RequestDetails requestDetails,
                                ServletRequestDetails servletRequestDetails,
                                TransactionDetails transactionDetails) {


    }
}
