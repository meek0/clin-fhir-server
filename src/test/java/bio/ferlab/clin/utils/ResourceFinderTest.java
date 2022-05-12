package bio.ferlab.clin.utils;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ResourceFinderTest {
  
  private final IFhirResourceDao patientDAO = Mockito.mock(IFhirResourceDao.class);
  private final ResourceDaoConfiguration configuration = new ResourceDaoConfiguration(
      patientDAO,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null);
  private final ResourceFinder resourceFinder = new ResourceFinder(configuration);

  @Test
  void findPatient_withDAO() {
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(patientDAO.read(any(), any())).thenReturn(new Patient());
    Optional<Patient> res = resourceFinder.findPatientFromRequestOrDAO(requestDetails, "foo");
    verify(patientDAO).read(eq(new IdType("foo")), eq(requestDetails));
    assertNotNull(res);
  }

  @Test
  void findPatient_withBundle() {
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    Bundle bundle = Mockito.mock(Bundle.class);
    Bundle.BundleEntryComponent e1 = Mockito.mock(Bundle.BundleEntryComponent.class);
    when(e1.getFullUrl()).thenReturn("foo");
    when(e1.getResource()).thenReturn(new Patient());
    when(bundle.getEntry()).thenReturn(List.of(e1));
    when(requestDetails.getResource()).thenReturn(bundle);
    Optional<Patient> res = resourceFinder.findPatientFromRequestOrDAO(requestDetails, "foo");
    verify(patientDAO, never()).read(any(), any());
    assertNotNull(res);
  }

  @Test
  void findPatient_empty() {
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(patientDAO.read(any(), any())).thenThrow(new ResourceNotFoundException("foo"));
    Optional<Patient> res = resourceFinder.findPatientFromRequestOrDAO(requestDetails, "foo");
    verify(patientDAO).read(eq(new IdType("foo")), eq(requestDetails));
    assertTrue(res.isEmpty());
  }

}