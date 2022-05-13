package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Task;

public class TaskValidator extends SchemaValidator<Task> {

  public TaskValidator() {
    super(Task.class);
  }

  @Override
  public boolean validateResource(Task patient) {
    return true;
  }
}