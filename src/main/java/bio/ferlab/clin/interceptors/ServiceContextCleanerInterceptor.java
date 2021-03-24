package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.context.ThreadLocalServiceContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Interceptor
@Service
public class ServiceContextCleanerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(AccessTokenInterceptor.class);

    @Hook( Pointcut.SERVER_PROCESSING_COMPLETED )
    public void clean( RequestDetails theRequestDetails ){
        logger.debug("Cleaning thread local service context.");
        ThreadLocalServiceContext.clear();
    }

}
