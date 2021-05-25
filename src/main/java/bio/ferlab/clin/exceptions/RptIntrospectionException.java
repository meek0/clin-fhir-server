package bio.ferlab.clin.exceptions;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RptIntrospectionException extends AuthenticationException {
    private static final Logger log = LoggerFactory.getLogger(RptIntrospectionException.class);

    public RptIntrospectionException(String rpt) {
        super("Invalid or expired RPT token");
        log.warn("Failed to introspect rpt [{}]", rpt);
    }
}
