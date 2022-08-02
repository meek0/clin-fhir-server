package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Specimen;

import java.util.List;

public class SpecimenValidator extends SchemaValidator<Specimen> {

  public SpecimenValidator() {
    super(Specimen.class);
  }

  @Override
  public List<String> validateResource(Specimen specimen) {
    return List.of();
  }
}