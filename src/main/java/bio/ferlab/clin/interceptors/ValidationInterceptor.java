package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

@Service
public class ValidationInterceptor extends RequestValidatingInterceptor {
    
    public ValidationInterceptor(JpaValidationSupportChain jpaValidationSupportChain) {
        FhirInstanceValidator module = new FhirInstanceValidator(jpaValidationSupportChain);
        module.setAnyExtensionsAllowed(false);
        module.setErrorForUnknownProfiles(true);
        module.setNoTerminologyChecks(false);
        module.setNoBindingMsgSuppressed(true);
        module.setAssumeValidRestReferences(true);
        this.setValidatorModules(Collections.singletonList(module));
    }

    @Hook(value = Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    @Override
    public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (requestDetails.getRestOperationType() == RestOperationTypeEnum.GRAPHQL_REQUEST) {
            return true;
        }
        return super.incomingRequestPostProcessed(requestDetails, request, response);
    }
}
