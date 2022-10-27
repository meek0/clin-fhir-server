package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Interceptor
@RequiredArgsConstructor
public class PersonGenderInterceptor {

  private final ResourceDaoConfiguration configuration;

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && oldResource instanceof Person && newResource instanceof Person;  // instanceof is null-safe
  }

  /**
   * Gender information is duplicated, when updating Person we should update all Patients as well (even if not inside the same EP)
   */
  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, oldResource, newResource)) {
      final Person oldPerson = (Person) oldResource;
      final Person newPerson = (Person) newResource;
      boolean needUpdate = newPerson.hasGender() && (!oldPerson.hasGender() || (oldPerson.hasGender() && !newPerson.getGender().toCode().equals(oldPerson.getGender().toCode())));
      if (needUpdate) {
        final String newGenderCode = newPerson.getGender().toCode();
        // all linked patients
        List<String> linkedPatients = extractPatientIds(newPerson);
        linkedPatients.forEach(id -> updatePatientGender(id, newGenderCode));
      }
    }
  }
  
  private List<String> extractPatientIds(Person person) {
    return person.getLink()
        .stream().map(Person.PersonLinkComponent::getTarget)
        .map(Reference::getReference)
        .filter(ref -> ref.startsWith("Patient/")).collect(Collectors.toList());
  }
  
  private void updatePatientGender(String patientId, String newGenderCode) {
    final Patient patient = configuration.patientDAO.read(new IdType(patientId));
    // patient can be null if inside the same request for example, so not yet in DB, can be ignored
    if (patient != null && (!patient.hasGender() || !patient.getGender().toCode().equals(newGenderCode))) {
      patient.setGender(Enumerations.AdministrativeGender.fromCode(newGenderCode));
      this.configuration.patientDAO.update(patient);
    }
  }
}
