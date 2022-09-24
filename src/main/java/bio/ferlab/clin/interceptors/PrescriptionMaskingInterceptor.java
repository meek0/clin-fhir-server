package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
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
    final List<String> userTags = metaTagResourceAccess.getUserTags(requestDetails);
    final List<IBaseResource> resources = sameRequestInterceptor.get(requestDetails);
    List<ServiceRequest> serviceRequests = MaskingUtils.extractAllOfType(resources, ServiceRequest.class);
    List<Patient> patients = MaskingUtils.extractAllOfType(resources, Patient.class);
    List<Person> persons = MaskingUtils.extractAllOfType(resources, Person.class);
    if (isPrescriptionRequest(serviceRequests, patients, persons)) {
      maskSensitiveData(userTags, serviceRequests.get(0), persons.get(0));
    }
  }
  
  private boolean isPrescriptionRequest(List<ServiceRequest> serviceRequests, List<Patient> patients, List<Person> persons) {
    // the following algorithm detects if the current request is about displaying a Prescription
    if (serviceRequests.size() == 1 && patients.size() == 1 && persons.size() == 1) {
      ServiceRequest sr = serviceRequests.get(0);
      Patient patient = patients.get(0);
      Person pers = persons.get(0);
      return MaskingUtils.isLinkedTo(sr, patient) && MaskingUtils.isLinkedTo(pers, patient);
    }
    return false;
  }
  
  private void maskSensitiveData(List<String> userTags, ServiceRequest sr, Person person) {
    final List<String> resourceTags = metaTagResourceAccess.getResourceTags(sr);
    if (!userTags.contains(USER_ALL_TAGS) && resourceTags.stream().noneMatch(userTags::contains)) {
      person.getNameFirstRep().setFamily(RESTRICTED_FIELD);
      person.getNameFirstRep().setGiven(List.of(new StringType(RESTRICTED_FIELD)));
      person.getIdentifierFirstRep().setValue(RESTRICTED_FIELD);
    }
  }

}
