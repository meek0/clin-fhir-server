package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.utils.ValidatorUtils;
import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.util.Optional;

public class PatientValidator extends SchemaValidator<Patient> {

  public static final String MRN_CODE = "MR";

  public PatientValidator() {
    super(Patient.class);
  }

  @Override
  public boolean validateResource(Patient patient) {
    return validateMRN(patient);
  }

  private boolean validateMRN(Patient patient) {
    Optional<Identifier> mrn = patient.getIdentifier().stream().filter(identifier -> MRN_CODE.equals(identifier.getType().getCodingFirstRep().getCode())).findFirst();
    return mrn.map(Identifier::getValue).filter(m -> StringUtils.isNotBlank(m) && !ValidatorUtils.hasSpecialCharacters(m) && ValidatorUtils.isTrimmed(m)).isPresent();
  }

}
