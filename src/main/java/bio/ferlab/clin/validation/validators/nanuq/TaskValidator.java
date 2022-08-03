package bio.ferlab.clin.validation.validators.nanuq;

import bio.ferlab.clin.validation.validators.SchemaValidator;
import org.hl7.fhir.r4.model.Task;

import java.util.List;

public class TaskValidator extends SchemaValidator<Task> {

  public TaskValidator() {
    super(Task.class);
  }

  @Override
  public List<String> validateResource(Task task) {
    return List.of();
  }
}