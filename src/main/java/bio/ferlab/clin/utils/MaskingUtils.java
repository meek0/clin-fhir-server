package bio.ferlab.clin.utils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaskingUtils {
  
  private MaskingUtils(){}
  
  public static boolean areLinked(ServiceRequest sr, Patient p) {
    return Optional.ofNullable(sr).map(ServiceRequest::getSubject).map(MaskingUtils::extractId).stream()
        .anyMatch(id -> p != null && id.equals(p.getIdElement().getIdPart()));
  }

  public static boolean areLinked(Person pers, Patient patient) {
    return pers != null && pers.getLink().stream().anyMatch(l -> patient != null && extractId(l.getTarget()).equals(patient.getIdElement().getIdPart()));
  }
  
  public static <T extends IBaseResource> List<T> extractAllOfType(List<IBaseResource> resources, Class<T> c) {
    List<T> results = new ArrayList<>();
    resources.forEach(res -> {
      if (res.getClass().equals(c)) {
        results.add((T)res);
      } else if (res instanceof Bundle) {
        ((Bundle) res).getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(c::isInstance)
            .forEach(r ->  results.add((T)r));
      }
    });
    return results;
  }

  public static String extractId(Reference reference) {
    if (reference != null) {
      final String ref = reference.getReference();
      if (StringUtils.isNotBlank(ref) && ref.contains("/")) {
        return ref.split("/")[1];
      }
    }
    return null;
  }
}
