package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.utils.ResourceFinder;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaTagResourceVisitorTest {
  
  private final ResourceFinder resourceFinder = Mockito.mock(ResourceFinder.class);
  private final MetaTagResourceVisitor metaTagResourceVisitor = new MetaTagResourceVisitor(resourceFinder);

  @Test
  void extractLdmCode_null() {
    assertNull(metaTagResourceVisitor.extractLdmCode(null, new Patient()));
    assertNull(metaTagResourceVisitor.extractLdmCode(null, new Observation()));
    assertNull(metaTagResourceVisitor.extractLdmCode(null, new ClinicalImpression()));
  }
  
  @Test
  void extractEpCode_not_implemented() {
    final AuditEvent resource = new AuditEvent();
    Exception ex = Assertions.assertThrows(
        NotImplementedOperationException.class,
        () -> metaTagResourceVisitor.extractEpCode(null, resource)
    );
    assertEquals("Missing extract EP implementation for meta tag of AuditEvent", ex.getMessage());
  }

  @Test
  void extractEpCode_ServiceRequest_missing_field() {
    final ServiceRequest resource = new ServiceRequest();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(null, resource)
    );
    assertEquals("Resource 'ServiceRequest' field 'subject' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_ServiceRequest() {
    final ServiceRequest resource = new ServiceRequest();
    resource.setSubject(new Reference().setReference("patient1"));
    Patient patient = new Patient();
    patient.setManagingOrganization(new Reference().setReference("Organization/bar"));
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    String code = metaTagResourceVisitor.extractEpCode(null, resource);
    verify(resourceFinder).findPatientFromRequestOrDAO(any(), eq("patient1"));
    assertEquals("bar", code);
   }

  @Test
  void extractEpCode_Patient_missing_field() {
    final Patient resource = new Patient();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(null, resource)
    );
    assertEquals("Resource 'Patient' field 'managing organization' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_Patient() {
    final Patient resource = new Patient();
    resource.getManagingOrganization().setReference("foo/bar");
    String code = metaTagResourceVisitor.extractEpCode(null, resource);
    assertEquals("bar", code);
  }

  @Test
  void extractLdmCode_not_implemented() {
    final AuditEvent resource = new AuditEvent();
    Exception ex = Assertions.assertThrows(
        NotImplementedOperationException.class,
        () -> metaTagResourceVisitor.extractLdmCode(null, resource)
    );
    assertEquals("Missing extract LDM implementation for meta tag of AuditEvent", ex.getMessage());
  }

  @Test
  void extractEpCode_Observation() {
    Observation resource = new Observation();
    resource.setSubject(new Reference().setReference("patient1"));
    Patient patient = new Patient();
    patient.setManagingOrganization(new Reference().setReference("Organization/foo"));
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    String code = metaTagResourceVisitor.extractEpCode(null, resource);
    verify(resourceFinder).findPatientFromRequestOrDAO(any(), eq("patient1"));
    assertEquals("foo", code);
  }

  @Test
  void extractEpCode_Observation_missing_subject() {
    Observation resource = new Observation();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(null, resource)
    );
    assertEquals("Resource 'Observation' field 'subject' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_Observation_patient_not_found() {
    Observation resource = new Observation();
    resource.setSubject(new Reference().setReference("patient1"));
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.empty());
    Exception ex = Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> metaTagResourceVisitor.extractEpCode(null, resource)
    );
    assertEquals("Resource not found 'Patient' with reference 'patient1'", ex.getMessage());
  }

  @Test
  void extractEpCode_ClinicalImpression() {
    ClinicalImpression resource = new ClinicalImpression();
    resource.setSubject(new Reference().setReference("patient1"));
    Patient patient = new Patient();
    patient.setManagingOrganization(new Reference().setReference("Organization/foo"));
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    String code = metaTagResourceVisitor.extractEpCode(null, resource);
    verify(resourceFinder).findPatientFromRequestOrDAO(any(), eq("patient1"));
    assertEquals("foo", code);
  }

  @Test
  void extractEpCode_ClinicalImpression_missing_subject() {
    ClinicalImpression resource = new ClinicalImpression();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(null,resource)
    );
    assertEquals("Resource 'ClinicalImpression' field 'subject' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_ClinicalImpression_patient_not_found() {
    ClinicalImpression resource = new ClinicalImpression();
    resource.setSubject(new Reference().setReference("patient1"));
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.empty());
    Exception ex = Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> metaTagResourceVisitor.extractEpCode(null, resource)
    );
    assertEquals("Resource not found 'Patient' with reference 'patient1'", ex.getMessage());
  }

  @Test
  void extractLdmCode_ServiceRequest() {
    final ServiceRequest resource = new ServiceRequest();
    resource.getPerformer().add(new Reference().setReference("foo/bar"));
    String code = metaTagResourceVisitor.extractLdmCode(null, resource);
    assertEquals("bar", code);
  }

  @Test
  void extractLdmCode_ServiceRequest_missing_field() {
    final ServiceRequest resource = new ServiceRequest();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractLdmCode(null, resource)
    );
    assertEquals("Resource 'ServiceRequest' field 'performer' is required for meta tag", ex.getMessage());
  }

}