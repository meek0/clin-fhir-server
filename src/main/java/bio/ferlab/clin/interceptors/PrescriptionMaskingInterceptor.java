package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess;
import bio.ferlab.clin.utils.FhirUtils;
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
    // extract the Person object, this one can be edited (the one in resources can not)
    final Person personToShow = MaskingUtils.extractPerson(preResourceShowDetails);
    if (personToShow != null && MaskingUtils.isValidPrescriptionRequest(resources)) {
      // this one can't be edited but at least should reference the same Person
      final Person personFromRequest = FhirUtils.extractAllOfType(resources, Person.class).get(0);
      if (FhirUtils.equals(personFromRequest, personToShow) && !MaskingUtils.isAlreadyMasked(personToShow)) {
        final List<String> userTags = metaTagResourceAccess.getUserTags(requestDetails);
        maskSensitiveData(userTags, MaskingUtils.extractAnalysis(resources), personToShow);
      }
    }
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
