package bio.ferlab.clin.validation;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.List;

public class ValidationChain {
    private List<SchemaValidator<? extends IBaseResource>> validators = new ArrayList<>();

    public ValidationChain withValidator(SchemaValidator<? extends  IBaseResource> validator) {
        this.validators.add(validator);
        return this;
    }

    public <T extends IBaseResource> boolean isValid(T resource){
        return this.validators.stream().allMatch(validator -> validator.validate(resource));
    }
}
