package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Person;

public class PersonValidator extends SchemaValidator<Person> {

  public PersonValidator() {
    super(Person.class);
  }

  @Override
  public boolean validateResource(Person patient) {
    return true;
  }
}