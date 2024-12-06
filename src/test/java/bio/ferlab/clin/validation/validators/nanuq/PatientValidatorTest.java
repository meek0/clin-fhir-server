package bio.ferlab.clin.validation.validators.nanuq;

import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientValidatorTest {
  
  final PatientValidator validator = new PatientValidator();
  
  @Test
  void validateResource() {
    final Patient nullMrn = getValidPatient();
    nullMrn.getIdentifierFirstRep().setValue(null);
    assertFalse(validator.validate(nullMrn).isEmpty());

    final Patient emptyMrn = getValidPatient();
    emptyMrn.getIdentifierFirstRep().setValue("");
    assertFalse(validator.validate(emptyMrn).isEmpty());

    final Patient badFormatMrn = getValidPatient();
    badFormatMrn.getIdentifierFirstRep().setValue(" foo");
    assertFalse(validator.validate(badFormatMrn).isEmpty());

    final Patient okMrn = getValidPatient();
    okMrn.getIdentifierFirstRep().setValue("foo");
    assertTrue(validator.validate(okMrn).isEmpty());

    final Patient noMrn = getValidPatient();
    noMrn.getIdentifier().clear();  // no MRN is OK
    assertTrue(validator.validate(noMrn).isEmpty());
  }

  private Patient getValidPatient() {
    final Patient patient = new Patient();
    patient.getIdentifierFirstRep().setValue("CCCCCC").getType().getCodingFirstRep().setCode(PatientValidator.MRN_CODE);
    return patient;
  }

}