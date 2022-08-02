package bio.ferlab.clin.validation.validators.nanuq;

import org.hl7.fhir.convertors.conv10_30.Patient10_30;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientValidatorTest {
  
  final PatientValidator validator = new PatientValidator();
  
  @Test
  void validateResource() {
    final Patient patient = new Patient();
    patient.getIdentifierFirstRep().getType().getCodingFirstRep().setCode(PatientValidator.MRN_CODE);

    patient.getIdentifierFirstRep().setValue(null);
    assertFalse(validator.validate(patient).isEmpty());
    
    patient.getIdentifierFirstRep().setValue("");
    assertFalse(validator.validate(patient).isEmpty());

    patient.getIdentifierFirstRep().setValue(" foo");
    assertFalse(validator.validate(patient).isEmpty());

    patient.getIdentifierFirstRep().setValue("foo");
    assertTrue(validator.validate(patient).isEmpty());
  }

}