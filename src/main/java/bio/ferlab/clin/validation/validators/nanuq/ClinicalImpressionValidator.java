package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.ClinicalImpression;

import java.util.List;

public class ClinicalImpressionValidator extends SchemaValidator<ClinicalImpression> {

  public ClinicalImpressionValidator() {
    super(ClinicalImpression.class);
  }

  @Override
  public List<String> validateResource(ClinicalImpression clinicalImpression) {
    return List.of();
  }
}