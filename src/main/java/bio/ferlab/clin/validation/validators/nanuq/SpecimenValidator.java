package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Specimen;

public class SpecimenValidator extends SchemaValidator<Specimen> {

  public SpecimenValidator() {
    super(Specimen.class);
  }

  @Override
  public boolean validateResource(Specimen patient) {
    return true;
  }
}