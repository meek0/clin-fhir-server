package bio.ferlab.clin.validation;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationChain {
    private final List<SchemaValidator<? extends IBaseResource>> validators = new ArrayList<>();

    public ValidationChain withValidator(SchemaValidator<? extends  IBaseResource> validator) {
        this.validators.add(validator);
        return this;
    }

    public <T extends IBaseResource> List<String> isValid(T resource){
        return this.validators.stream().flatMap(validator -> validator.validate(resource).stream()).collect(Collectors.toList());
    }
}
