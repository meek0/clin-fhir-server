package bio.ferlab.clin.validation.validators.nanuq;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonValidatorTest {
  
  final PersonValidator validator = new PersonValidator();
  
  @Test
  void validateResource() {
    final Person valid = getValidPerson();
    assertTrue(validator.validateResource(valid));

    final Person badName = getValidPerson();
    badName.getNameFirstRep().setFamily("x");
    assertFalse(validator.validateResource(badName));

    final Person badBirthDate = getValidPerson();
    badBirthDate.setBirthDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    assertFalse(validator.validateResource(badBirthDate));
  }
  
  private Person getValidPerson() {
    final Person person = new Person();
    person.addName(new HumanName().setFamily("foo").setGiven(List.of(new StringType("bar"))));
    person.setBirthDate(new Date());
    person.getIdentifierFirstRep().setValue("XXXX12345678").getType().getCodingFirstRep().setCode(PersonValidator.RAMQ_CODE);
    return person;
  }

}