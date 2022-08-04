package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.utils.ValidatorUtils;
import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.StringType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PersonValidator extends SchemaValidator<Person> {

  public static final String RAMQ_CODE = "JHN";

  public PersonValidator() {
    super(Person.class);
  }

  @Override
  public List<String> validateResource(Person person) {
    List<String> errors = new ArrayList<>();
    if(!validateBirthDate(person)) formatError(errors, person, "Invalid birth date");
    if(!validateRAMQ(person)) formatError(errors, person, "Invalid RAMQ");
    if(!validateNames(person)) formatError(errors, person, "Invalid names");
    return errors;
  }

  private boolean validateBirthDate(Person person) {
    final Date date = person.getBirthDate();
    final Date today = new Date();
    return date == null || DateUtils.isSameDay(date, today) || date.before(today);
  }

  private boolean validateRAMQ(Person person) {
    Optional<Identifier> ramq = person.getIdentifier().stream().filter(identifier -> RAMQ_CODE.equals(identifier.getType().getCodingFirstRep().getCode())).findFirst();
    return ramq.isEmpty() || ramq.map(Identifier::getValue).filter(m -> StringUtils.isNotBlank(m) && ValidatorUtils.isValidRAMQ(m) && ValidatorUtils.isTrimmed(m)).isPresent();
  }

  private boolean validateNames(Person person) {
    final List<HumanName> names = person.getName();
    return !names.isEmpty() && names.stream().allMatch(this::validateName);
  }

  private boolean validateName(HumanName name) {
    final String family = name.getFamilyElement().getValue();
    final List<String> givens = name.getGiven().stream().map(StringType::getValue).collect(Collectors.toList());
    return isValidName(family) && givens.stream().allMatch(this::isValidName);
  }

  private boolean isValidName(String name) {
    return name != null &&
        name.length() >= 2 &&
        !ValidatorUtils.hasSpecialCharacters(name) &&
        ValidatorUtils.isTrimmed(name);
  }
}