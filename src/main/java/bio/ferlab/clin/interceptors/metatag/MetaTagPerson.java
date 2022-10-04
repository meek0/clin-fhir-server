package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.interceptors.SameRequestInterceptor;
import bio.ferlab.clin.utils.MaskingUtils;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess.LDM_TAG_PREFIX;

@Component
@RequiredArgsConstructor
public class MetaTagPerson {
  
  private final SameRequestInterceptor sameRequestInterceptor;
  private final ResourceDaoConfiguration daoConfiguration;
  
  // custom implementation of canSeeResource for Person
  public boolean canSeeResource(MetaTagResourceAccess metaTagResourceAccess, RequestDetails requestDetails, Person person) {
    final List<String> userTags = metaTagResourceAccess.getUserTags(requestDetails);
    if (isOnlyLDM(userTags)) {  // restrict access only for LDMs, EP always see Person
      final List<IBaseResource> resources = sameRequestInterceptor.get(requestDetails);
      // only if not a prescription (cf PrescriptionMaskingInterceptor)
      if (resources.contains(person) && !MaskingUtils.isValidPrescriptionRequest(resources)) {
        // Person resources aren't tagged with EP, instead let's find out all related Patients
        final List<Patient> patients = fetchPatients(person);
        final List<ServiceRequest> serviceRequests = fetchServiceRequests(patients);

        final Set<String> resourceTags = patients.stream().flatMap(p -> metaTagResourceAccess.getResourceTags(p).stream()).collect(Collectors.toSet());
        resourceTags.addAll(serviceRequests.stream().flatMap(sr -> metaTagResourceAccess.getResourceTags(sr).stream()).collect(Collectors.toSet()));
        // if user has no reasons to see this person data, mask them
        return resourceTags.stream().anyMatch(userTags::contains);
      }
    }
    return true;
  }
  
  private boolean isOnlyLDM(List<String> userTags) {
    return userTags.stream().allMatch(t -> t.startsWith(LDM_TAG_PREFIX));
  }

  private List<Patient> fetchPatients(Person person) {
    final IBundleProvider results = daoConfiguration.personDAO.search(SearchParameterMap.newSynchronous()
        .add(IAnyResource.SP_RES_ID, new TokenParam(person.getIdElement().getIdPart()))
        .addInclude(Person.INCLUDE_PATIENT));
    return results.isEmpty() ? List.of() : results.getAllResources().stream()
        .filter(Patient.class::isInstance).map(Patient.class::cast).collect(Collectors.toList());
  }

  private List<ServiceRequest> fetchServiceRequests(List<Patient> patients) {
    final List<ServiceRequest> serviceRequests = new ArrayList<>();
    patients.forEach(patient -> {
      final IBundleProvider results = daoConfiguration.serviceRequestDAO.search(SearchParameterMap.newSynchronous()
          .add(ServiceRequest.SP_PATIENT, new ReferenceParam(patient.getIdElement().getIdPart())));
      serviceRequests.addAll(results.isEmpty() ? List.of() :
          results.getAllResources().stream().map(ServiceRequest.class::cast).collect(Collectors.toList()));
    });
    return serviceRequests;
  }
}
