package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.ServiceRequest;

import java.util.List;

public class ServiceRequestValidator extends SchemaValidator<ServiceRequest> {

  public ServiceRequestValidator() {
    super(ServiceRequest.class);
  }

  @Override
  public List<String> validateResource(ServiceRequest serviceRequest) {
    return List.of();
  }
}