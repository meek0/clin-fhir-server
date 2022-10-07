package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.interceptors.SameRequestInterceptor;
import bio.ferlab.clin.utils.FhirUtils;
import bio.ferlab.clin.utils.MaskingUtils;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

import static bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess.USER_ALL_TAGS;

@Component
@RequiredArgsConstructor
public class PrescriptionMasking {
  
  public static final String RESTRICTED_FIELD = "*****";
  
  private final SameRequestInterceptor sameRequestInterceptor;
  
  public void preShow(MetaTagResourceAccess metaTagResourceAccess, Person personToShow, RequestDetails requestDetails) {
    final List<IBaseResource> resources = sameRequestInterceptor.get(requestDetails);
    if (personToShow != null && MaskingUtils.isValidPrescriptionRequest(resources)) {
      // the query may contain several Person, find the good one
      // check if personToShow and personFromRequest are the same (because not same JAVA object)
      final Person personFromRequest = FhirUtils.extractAllOfType(resources, Person.class).stream()
          .filter(p -> FhirUtils.equals(p, personToShow)).findFirst().orElse(null);
      if (personFromRequest != null && !MaskingUtils.isAlreadyMasked(personToShow)) {
        final List<String> userTags = metaTagResourceAccess.getUserTags(requestDetails);
        maskSensitiveData(metaTagResourceAccess, userTags, MaskingUtils.extractAnalysis(resources), personToShow);
      }
    }
  }
  
  private void maskSensitiveData(MetaTagResourceAccess metaTagResourceAccess, List<String> userTags, ServiceRequest sr, Person person) {
    final List<String> resourceTags = metaTagResourceAccess.getResourceTags(sr);
    if (!userTags.contains(USER_ALL_TAGS) && resourceTags.stream().noneMatch(userTags::contains)) {
      person.setId(RESTRICTED_FIELD);
      person.getNameFirstRep().setFamily(RESTRICTED_FIELD);
      person.getNameFirstRep().setGiven(List.of(new StringType(RESTRICTED_FIELD)));
      person.getIdentifierFirstRep().setValue(RESTRICTED_FIELD);
    }
  }

}
