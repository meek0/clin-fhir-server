package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LogInterceptor.class);

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public void log(RequestDetails request) {
        final var requestContent = request.getRequestContentsIfLoaded();
        if(requestContent != null){
            logger.info(new String(requestContent));
        }
    }
}
