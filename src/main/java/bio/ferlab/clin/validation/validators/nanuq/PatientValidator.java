package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Patient;

public class PatientValidator extends SchemaValidator<Patient> {

  public PatientValidator() {
    super(Patient.class);
  }

  @Override
  public boolean validateResource(Patient patient) {
    return true;
  }

}
