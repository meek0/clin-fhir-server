package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess;
import bio.ferlab.clin.utils.MaskingUtils;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess.USER_ALL_TAGS;

@Component
@Interceptor
@RequiredArgsConstructor
public class PrescriptionMaskingInterceptor {
  
  public static final String RESTRICTED_FIELD = "*****";
  
  private final MetaTagResourceAccess metaTagResourceAccess;
  private final SameRequestInterceptor sameRequestInterceptor;
  
  @Hook(Pointcut.STORAGE_PRESHOW_RESOURCES)
  public void preShow(IPreResourceShowDetails preResourceShowDetails, RequestDetails requestDetails) {
    final List<IBaseResource> resources = sameRequestInterceptor.get(requestDetails);
    if (resources.size() >= 3) {  // minimal requirements
      List<ServiceRequest> serviceRequests = MaskingUtils.extractAllOfType(resources, ServiceRequest.class);
      List<Patient> patients = MaskingUtils.extractAllOfType(resources, Patient.class);
      List<Person> persons = MaskingUtils.extractAllOfType(resources, Person.class);
      if (isValidPrescriptionRequest(serviceRequests, patients, persons)) {
        final List<String> userTags = metaTagResourceAccess.getUserTags(requestDetails);
        maskSensitiveData(userTags, serviceRequests.get(0), persons.get(0));
      }
    }
  }
  
  private boolean isValidPrescriptionRequest(List<ServiceRequest> serviceRequests, List<Patient> patients, List<Person> persons) {
    // the following algorithm detects if the current request is about displaying a Prescription
    final ServiceRequest analysis = serviceRequests.stream().filter(MaskingUtils::isAnalysis).findFirst().orElse(null);
    if (analysis != null && patients.size() == 1 && persons.size() == 1) {
      Patient patient = patients.get(0);
      Person pers = persons.get(0);
      return !isAlreadyMasked(pers) && MaskingUtils.areLinked(analysis, patient) && MaskingUtils.areLinked(pers, patient);
    }
    return false;
  }
  
  private boolean isAlreadyMasked(Person person) {
    return Optional.ofNullable(person).map(Person::getId).stream().anyMatch(id -> id.equals(RESTRICTED_FIELD));
  }
  
  private void maskSensitiveData(List<String> userTags, ServiceRequest sr, Person person) {
    final List<String> resourceTags = metaTagResourceAccess.getResourceTags(sr);
    if (!userTags.contains(USER_ALL_TAGS) && resourceTags.stream().noneMatch(userTags::contains)) {
      person.setId(RESTRICTED_FIELD);
      person.getNameFirstRep().setFamily(RESTRICTED_FIELD);
      person.getNameFirstRep().setGiven(List.of(new StringType(RESTRICTED_FIELD)));
      person.getIdentifierFirstRep().setValue(RESTRICTED_FIELD);
    }
  }

}
