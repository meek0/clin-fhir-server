package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.ClinicalImpression;

public class ClinicalImpressionValidator extends SchemaValidator<ClinicalImpression> {

  public ClinicalImpressionValidator() {
    super(ClinicalImpression.class);
  }

  @Override
  public boolean validateResource(ClinicalImpression patient) {
    return true;
  }
}