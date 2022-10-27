package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IAnyResource;
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
    // search instead of read because Patient could be in the request and not yet in DB
    // read will cause not found exception and rollback the transaction
   final IBundleProvider bundle = configuration.patientDAO.search(SearchParameterMap.newSynchronous()
        .add(IAnyResource.SP_RES_ID, new TokenParam(patientId)));
   if (!bundle.isEmpty()) {
     final IBaseResource resource = bundle.getAllResources().get(0);
     if (resource instanceof Patient){
       final Patient patient = (Patient) resource; 
       if (!patient.hasGender() || !patient.getGender().toCode().equals(newGenderCode)) {
         patient.setGender(Enumerations.AdministrativeGender.fromCode(newGenderCode));
         this.configuration.patientDAO.update(patient);
       }
     }
   }
  }
}
