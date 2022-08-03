package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.validation.ValidationChain;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FieldValidatorInterceptor {
    private final ValidationChain validationChain;

    public FieldValidatorInterceptor(ValidationChain validationChain) {
        this.validationChain = validationChain;
    }

    @Hook(value = Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED, order = -1)
    public void resourceCreated(IBaseResource resource) {
        final List<String> errors = this.validationChain.isValid(resource);
        if (!errors.isEmpty()) {
            throw new ca.uhn.fhir.rest.server.exceptions.InvalidRequestException(StringUtils.join(errors, ", "));
        }
    }

    @Hook(value = Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED, order = -1)
    public void resourceUpdated(IBaseResource oldResource, IBaseResource newResource) {
        final List<String> errors = this.validationChain.isValid(newResource);
        if (!errors.isEmpty()) {
            throw new ca.uhn.fhir.rest.server.exceptions.InvalidRequestException(StringUtils.join(errors, ", "));
        }
    }

}
