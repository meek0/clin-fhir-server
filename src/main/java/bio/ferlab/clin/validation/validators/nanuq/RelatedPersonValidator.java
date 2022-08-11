package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.utils.ValidatorUtils;
import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RelatedPersonValidator extends SchemaValidator<RelatedPerson> {

  public static final String RAMQ_CODE = "JHN";

  public RelatedPersonValidator() {
    super(RelatedPerson.class);
  }

  @Override
  public List<String> validateResource(RelatedPerson relatedPerson) {
    List<String> errors = new ArrayList<>();
    if(!validateRAMQ(relatedPerson)) formatError(errors, relatedPerson, "Invalid RAMQ");
    if(!validatePatient(relatedPerson)) formatError(errors, relatedPerson, "Invalid Patient");
    return errors;
  }
  
  private boolean validatePatient(RelatedPerson relatedPerson) {
    return StringUtils.isNotBlank(relatedPerson.getPatient().getReference());
  }

  private boolean validateRAMQ(RelatedPerson relatedPerson) {
    Optional<Identifier> ramq = relatedPerson.getIdentifier().stream().filter(identifier -> RAMQ_CODE.equals(identifier.getType().getCodingFirstRep().getCode())).findFirst();
    return ramq.map(Identifier::getValue).filter(m -> StringUtils.isNotBlank(m) && ValidatorUtils.isValidRAMQ(m) && ValidatorUtils.isTrimmed(m)).isPresent();
  }

}