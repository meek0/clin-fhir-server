package bio.ferlab.clin.interceptors.metatag;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class MetaTagResourceVisitor {

  public String extractEpCode(IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractEpCode((ServiceRequest) resource);
    }
    // add implementation of resource with EP or return null
    throw new NotImplementedOperationException(String.format("Missing extract EP implementation for meta tag of %s", resource.getClass().getSimpleName()));
  }

  public String extractLdmCode(IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractLdmCode((ServiceRequest) resource);
    }
    // add implementation of resource with LDM or return null
    throw new NotImplementedOperationException(String.format("Missing extract LDM implementation for meta tag of %s", resource.getClass().getSimpleName()));
  }

  private String extractEpCode(ServiceRequest resource) {
    return Optional.ofNullable(resource.getIdentifier()).orElse(Collections.emptyList()).stream()
        .filter(i -> i.getType().getCoding().stream().anyMatch(c -> "MR".equals(c.getCode())))
        .findFirst().map(p -> p.getAssigner().getReference().split("/")[1])
        .orElseThrow(() -> new InvalidRequestException("Identifier of type MR with assigner organization is required for meta tag"));
  }

  private String extractLdmCode(ServiceRequest resource) {
    return Optional.ofNullable(resource.getPerformer()).orElse(Collections.emptyList()).stream()
        .filter(p -> StringUtils.isNotBlank(p.getReference()))
        .map(p -> p.getReference().split("/")[1])
        .findFirst().orElseThrow(() -> new InvalidRequestException("Performer reference is required for meta tag"));
  }
}
