package bio.ferlab.clin.validation.validators.nanuq;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatedPersonValidatorTest {
  
  final RelatedPersonValidator validator = new RelatedPersonValidator();
  
  @Test
  void validateResource() {
    final RelatedPerson ok = getValidRelatedPerson();
    assertTrue(validator.validate(ok).isEmpty());

    final RelatedPerson invalidRamq = getValidRelatedPerson();
    invalidRamq.getIdentifierFirstRep().setValue("badRamq");
    assertFalse(validator.validate(invalidRamq).isEmpty());

    final RelatedPerson missingPatient = getValidRelatedPerson();
    missingPatient.setPatient(null);
    assertFalse(validator.validate(missingPatient).isEmpty());
  }

  private RelatedPerson getValidRelatedPerson() {
    final RelatedPerson relatedPerson = new RelatedPerson();
    relatedPerson.setPatient(new Reference("Patient/bar"));
    relatedPerson.getIdentifierFirstRep().setValue("XXXX12345678").getType().getCodingFirstRep().setCode(PersonValidator.RAMQ_CODE);
    return relatedPerson;
  }

}