package bio.ferlab.clin.validation.validators;


import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;

public abstract class SchemaValidator<T extends IBaseResource> {
    private final Class<T> tClass;

    public SchemaValidator(Class<T> tClass) {
        this.tClass = tClass;
    }


    public boolean validate(IBaseResource resource) {
        if(tClass.isInstance(resource)){
            return this.validateResource(tClass.cast(resource));
        }
        return true;
    }

    public abstract boolean validateResource(T resource);
}
