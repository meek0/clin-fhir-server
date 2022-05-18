package bio.ferlab.clin.utils;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ResourceFinder {

  private final ResourceDaoConfiguration configuration;

  public ResourceFinder(ResourceDaoConfiguration configuration) {
    this.configuration = configuration;
  }
  
  private Optional<Bundle.BundleEntryComponent> findEntryFromBundle(RequestDetails requestDetails, String fullUrl) {
    Optional<Bundle.BundleEntryComponent> fromBundle = Optional.empty();
    if (requestDetails != null && requestDetails.getResource() != null && requestDetails.getResource() instanceof Bundle) {
      final Bundle bundle = (Bundle) requestDetails.getResource();
      fromBundle = bundle.getEntry().stream().filter(e -> e.getFullUrl().equals(fullUrl))
          .findFirst();
    }
    return fromBundle;
  }

  public Optional<Patient> findPatientFromDAO(RequestDetails requestDetails, String ref) {
    try{
      return Optional.ofNullable(this.configuration.patientDAO.read(new IdType(ref), requestDetails));
    } catch(ResourceNotFoundException e) {
      return Optional.empty();
    }
  }

  public Optional<Patient> findPatientFromRequest(RequestDetails requestDetails, String fullUrl) {
    return findEntryFromBundle(requestDetails, fullUrl)
        .map(e -> (Patient) e.getResource());
  }

  public Optional<Patient> findPatientFromRequestOrDAO(RequestDetails requestDetails, String fullUrlOrRef) {
    return findPatientFromRequest(requestDetails, fullUrlOrRef)
        .or(() -> findPatientFromDAO(requestDetails, fullUrlOrRef));
  }
  
}
