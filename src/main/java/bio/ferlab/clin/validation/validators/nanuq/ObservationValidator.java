package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Observation;
import java.util.List;

public class ObservationValidator extends SchemaValidator<Observation> {

  public ObservationValidator() {
    super(Observation.class);
  }

  @Override
  public List<String> validateResource(Observation observation) {
    return List.of();
  }
}