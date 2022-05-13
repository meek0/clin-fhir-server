package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.ServiceRequest;

public class ServiceRequestValidator extends SchemaValidator<ServiceRequest> {

  public ServiceRequestValidator() {
    super(ServiceRequest.class);
  }

  @Override
  public boolean validateResource(ServiceRequest patient) {
    return true;
  }
}