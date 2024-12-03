package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.utils.ResourceFinder;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class MetaTagResourceVisitor {

  private final ResourceFinder resourceFinder;

  public MetaTagResourceVisitor(ResourceFinder resourceFinder) {
    this.resourceFinder = resourceFinder;
  }

  public String extractEpCode(RequestDetails requestDetails, IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractEpCode(requestDetails, (ServiceRequest) resource);
    } else if(resource instanceof Patient) {
      return extractEpCode(requestDetails, (Patient) resource);
    } else if(resource instanceof Observation) {
      return extractEpCode(requestDetails, (Observation) resource);
    } else if(resource instanceof ClinicalImpression) {
      return extractEpCode(requestDetails, (ClinicalImpression) resource);
    }
    // add implementation of resource with EP or return null
    throw new NotImplementedOperationException(String.format("Missing extract EP implementation for meta tag of %s", resource.getClass().getSimpleName()));
  }

  public String extractLdmCode(RequestDetails requestDetails, IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractLdmCode(requestDetails, (ServiceRequest) resource);
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

  private String extractEpCode(RequestDetails requestDetails, ServiceRequest resource) {
    final String patientRef = Optional.ofNullable(resource.getSubject()).map(Reference::getReference)
        .orElseThrow(() -> formatExtractException(resource, "subject"));
    return extractEpCode(requestDetails, resourceFinder.findPatientFromRequestOrDAO(requestDetails, patientRef)
        .orElseThrow(() -> formatResourceNotFoundException("Patient", patientRef)));
  }

  private String extractEpCode(RequestDetails requestDetails, Patient resource) {
    return Optional.ofNullable(resource.getManagingOrganization()).map(this::getReferenceId)
        .orElseThrow(() -> formatExtractException(resource, "managing organization"));
  }

  private String extractEpCode(RequestDetails requestDetails, Observation resource) {
    final String patientRef = Optional.ofNullable(resource.getSubject()).map(Reference::getReference)
        .orElseThrow(() -> formatExtractException(resource, "subject"));
    return extractEpCode(requestDetails, resourceFinder.findPatientFromRequestOrDAO(requestDetails, patientRef)
        .orElseThrow(() -> formatResourceNotFoundException("Patient", patientRef)));
  }

  private String extractEpCode(RequestDetails requestDetails, ClinicalImpression resource) {
    final String patientRef = Optional.ofNullable(resource.getSubject()).map(Reference::getReference)
        .orElseThrow(() -> formatExtractException(resource, "subject"));
    return extractEpCode(requestDetails, resourceFinder.findPatientFromRequestOrDAO(requestDetails, patientRef)
        .orElseThrow(() -> formatResourceNotFoundException("Patient", patientRef)));
  }

  private String extractLdmCode(RequestDetails requestDetails, ServiceRequest resource) {
    return Optional.ofNullable(resource.getPerformer()).orElse(Collections.emptyList()).stream()
        .filter(p -> StringUtils.isNotBlank(p.getReference()))
        .map(this::getReferenceId)
        .findFirst().orElse(null);
  }
  
  private String getReferenceId(Reference reference) {
   return Optional.ofNullable(reference).map(Reference::getReference)
        .map(r -> r.split("/")).filter(s -> s.length > 1).map(s -> s[1]).orElse(null);
  }
  
  private InvalidRequestException formatExtractException(IBaseResource resource, String missingField) {
    return new InvalidRequestException(String.format("Resource '%s' field '%s' is required for meta tag", resource.getClass().getSimpleName(), missingField));
  }

  private ResourceNotFoundException formatResourceNotFoundException(String resourceName, String resourceReference) {
    return new ResourceNotFoundException(String.format("Resource not found '%s' with reference '%s'", resourceName, resourceReference));
  }
}
