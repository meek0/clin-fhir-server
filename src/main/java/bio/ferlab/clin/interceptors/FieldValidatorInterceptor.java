package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.validation.ValidationChain;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Service;

@Service
public class FieldValidatorInterceptor {
    private final ValidationChain validationChain;

    public FieldValidatorInterceptor(ValidationChain validationChain) {
        this.validationChain = validationChain;
    }

    @Hook(value = Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED, order = -1)
    public void resourceCreated(IBaseResource resource) {
        final boolean validation = this.validationChain.isValid(resource);
        if (!validation) {
            throw new ca.uhn.fhir.rest.server.exceptions.InvalidRequestException("Validation failed");
        }
    }

    @Hook(value = Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED, order = -1)
    public void resourceUpdated(IBaseResource oldResource, IBaseResource newResource) {
        final boolean validation = this.validationChain.isValid(newResource);
        if (!validation) {
            throw new ca.uhn.fhir.rest.server.exceptions.InvalidRequestException("Validation failed");
        }
    }

}
