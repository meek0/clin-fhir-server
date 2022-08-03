package bio.ferlab.clin.validation.validators;

import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

public abstract class SchemaValidator<T extends IBaseResource> {
    private final Class<T> tClass;

    public SchemaValidator(Class<T> tClass) {
        this.tClass = tClass;
    }
    
    public List<String> validate(IBaseResource resource) {
        if(tClass.isInstance(resource)){
            return this.validateResource(tClass.cast(resource));
        }
        return List.of();
    }

    public abstract List<String> validateResource(T resource);
    
    protected void formatError(List<String> errors, IBaseResource resource, String error) {
        errors.add(String.format("%s/%s : %s", resource.getClass().getSimpleName(), resource.getIdElement().getIdPart(), error));
    }
}
