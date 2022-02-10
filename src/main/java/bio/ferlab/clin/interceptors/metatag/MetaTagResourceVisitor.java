package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class MetaTagResourceVisitor {

  private final ResourceDaoConfiguration configuration;

  public MetaTagResourceVisitor(ResourceDaoConfiguration configuration) {
    this.configuration = configuration;
  }

  public String extractEpCode(IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractEpCode((ServiceRequest) resource);
    } else if(resource instanceof Patient) {
      return extractEpCode((Patient) resource);
    } else if(resource instanceof Observation) {
      return extractEpCode((Observation) resource);
    } else if(resource instanceof ClinicalImpression) {
      return extractEpCode((ClinicalImpression) resource);
    }
    // add implementation of resource with EP or return null
    throw new NotImplementedOperationException(String.format("Missing extract EP implementation for meta tag of %s", resource.getClass().getSimpleName()));
  }

  public String extractLdmCode(IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractLdmCode((ServiceRequest) resource);
    } else if(resource instanceof Patient) {
      return null;
    } else if(resource instanceof Observation) {
      return null;
    } else if(resource instanceof ClinicalImpression) {
      return null;
    }
    // add implementation of resource with LDM or return null
    throw new NotImplementedOperationException(String.format("Missing extract LDM implementation for meta tag of %s", resource.getClass().getSimpleName()));
  }

  private String extractEpCode(ServiceRequest resource) {
    return Optional.ofNullable(resource.getIdentifier()).orElse(Collections.emptyList()).stream()
        .filter(i -> i.getType().getCoding().stream().anyMatch(c -> "MR".equals(c.getCode())))
        .findFirst().map(p -> getReferenceId(p.getAssigner()))
        .orElseThrow(() -> formatExtractException(resource, "identifier of type MR"));
  }

  private String extractEpCode(Patient resource) {
    return Optional.ofNullable(resource.getManagingOrganization()).map(this::getReferenceId)
        .orElseThrow(() -> formatExtractException(resource, "managing organization"));
  }

  private String extractEpCode(Observation resource) {
    final String patientRef = Optional.ofNullable(resource.getSubject()).map(Reference::getReference)
        .orElseThrow(() -> formatExtractException(resource, "subject"));
    final Patient patient = this.configuration.patientDAO.read(new IdType(patientRef));
    return Optional.ofNullable(patient).map(this::extractEpCode).orElseThrow(() -> formatResourceNotFoundException("Patient", patientRef));
  }

  private String extractEpCode(ClinicalImpression resource) {
    final String patientRef = Optional.ofNullable(resource.getSubject()).map(Reference::getReference)
        .orElseThrow(() -> formatExtractException(resource, "subject"));
    final Patient patient = this.configuration.patientDAO.read(new IdType(patientRef));
    return Optional.ofNullable(patient).map(this::extractEpCode).orElseThrow(() -> formatResourceNotFoundException("Patient", patientRef));
  }

  private String extractLdmCode(ServiceRequest resource) {
    return Optional.ofNullable(resource.getPerformer()).orElse(Collections.emptyList()).stream()
        .filter(p -> StringUtils.isNotBlank(p.getReference()))
        .map(this::getReferenceId)
        .findFirst().orElseThrow(() -> formatExtractException(resource, "performer"));
  }
  
  private String getReferenceId(Reference reference) {
   return Optional.ofNullable(reference).map(Reference::getReference)
        .map(r -> r.split("/")).filter(s -> s.length > 1).map(s -> s[1]).orElse(null);
  }
  
  private InvalidRequestException formatExtractException(IBaseResource resource, String missingField) {
    return new InvalidRequestException(String.format("Resource '%s' field '%s' is required for meta tag", resource.getClass().getSimpleName(), missingField));
  }

  private InvalidRequestException formatResourceNotFoundException(String resourceName, String resourceReference) {
    return new InvalidRequestException(String.format("Resource not found '%s' with reference '%s'", resourceName, resourceReference));
  }
}
