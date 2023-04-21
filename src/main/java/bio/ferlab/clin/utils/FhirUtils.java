package bio.ferlab.clin.utils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FhirUtils {
  
  private FhirUtils(){}
  
  public static String formatResource(IBaseResource resource) {
    return String.format("%s/%s", resource.fhirType(), resource.getIdElement().getIdPart());
  }

  public static Reference toReference(IBaseResource resource) {
    return new Reference(formatResource(resource));
  }
  
  public static String extractId(String url) {
    return extractId(new Reference(url));
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

  public static <T extends IBaseResource> List<T> extractAllOfType(List<? extends IBaseResource> resources, Class<T> c) {
    List<T> results = new ArrayList<>();
    resources.forEach(res -> {
      if (res != null && res.getClass().equals(c)) {
        results.add((T)res);
      } else if (res instanceof Bundle) {
        ((Bundle) res).getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(c::isInstance)
            .forEach(r ->  results.add((T)r));
      }
    });
    return results;
  }

  public static boolean equals(IBaseResource res1 , IBaseResource res2) {
    return res1!=null && res2 != null && formatResource(res1).equals(formatResource(res2));
  }

  public static List<String> getPerformerIds(ServiceRequest serviceRequest, Class<? extends IBaseResource> type){
    return serviceRequest.getPerformer().stream().filter(p -> type != null && p.getReference().startsWith(type.getSimpleName()))
      .map(p -> p.getReferenceElement().getIdPart())
      .collect(Collectors.toList());
  }
}
