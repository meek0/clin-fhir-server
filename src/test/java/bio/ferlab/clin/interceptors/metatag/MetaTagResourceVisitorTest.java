package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaTagResourceVisitorTest {
  
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
  private final MetaTagResourceVisitor metaTagResourceVisitor = new MetaTagResourceVisitor(configuration);

  @Test
  void extractLdmCode_null() {
    assertNull(metaTagResourceVisitor.extractLdmCode(new Patient()));
    assertNull(metaTagResourceVisitor.extractLdmCode(new Observation()));
    assertNull(metaTagResourceVisitor.extractLdmCode(new ClinicalImpression()));
  }
  
  @Test
  void extractEpCode_not_implemented() {
    final AuditEvent resource = new AuditEvent();
    Exception ex = Assertions.assertThrows(
        NotImplementedOperationException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Missing extract EP implementation for meta tag of AuditEvent", ex.getMessage());
  }

  @Test
  void extractEpCode_ServiceRequest_missing_field() {
    final ServiceRequest resource = new ServiceRequest();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Resource 'ServiceRequest' field 'identifier of type MR' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_ServiceRequest() {
    final ServiceRequest resource = new ServiceRequest();
    final Identifier identifier = new Identifier();
    final CodeableConcept codeableConcept = new CodeableConcept();
    final Coding coding = new Coding().setCode("MR");
    codeableConcept.getCoding().add(coding);
    identifier.setType(codeableConcept);
    identifier.setAssigner(new Reference().setReference("foo/bar"));
    resource.getIdentifier().add(identifier);
    String code = metaTagResourceVisitor.extractEpCode(resource);
    assertEquals("bar", code);
   }

  @Test
  void extractEpCode_Patient_missing_field() {
    final Patient resource = new Patient();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Resource 'Patient' field 'managing organization' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_Patient() {
    final Patient resource = new Patient();
    resource.getManagingOrganization().setReference("foo/bar");
    String code = metaTagResourceVisitor.extractEpCode(resource);
    assertEquals("bar", code);
  }

  @Test
  void extractLdmCode_not_implemented() {
    final AuditEvent resource = new AuditEvent();
    Exception ex = Assertions.assertThrows(
        NotImplementedOperationException.class,
        () -> metaTagResourceVisitor.extractLdmCode(resource)
    );
    assertEquals("Missing extract LDM implementation for meta tag of AuditEvent", ex.getMessage());
  }

  @Test
  void extractEpCode_Observation() {
    Observation resource = new Observation();
    resource.setSubject(new Reference().setReference("patient1"));
    Patient patient = new Patient();
    patient.setManagingOrganization(new Reference().setReference("Organization/foo"));
    when(patientDAO.read(any())).thenReturn(patient);
    String code = metaTagResourceVisitor.extractEpCode(resource);
    verify(patientDAO).read(eq(new IdType("patient1")));
    assertEquals("foo", code);
  }

  @Test
  void extractEpCode_Observation_missing_subject() {
    Observation resource = new Observation();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Resource 'Observation' field 'subject' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_Observation_patient_not_found() {
    Observation resource = new Observation();
    resource.setSubject(new Reference().setReference("patient1"));
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Resource not found 'Patient' with reference 'patient1'", ex.getMessage());
  }

  @Test
  void extractEpCode_ClinicalImpression() {
    ClinicalImpression resource = new ClinicalImpression();
    resource.setSubject(new Reference().setReference("patient1"));
    Patient patient = new Patient();
    patient.setManagingOrganization(new Reference().setReference("Organization/foo"));
    when(patientDAO.read(any())).thenReturn(patient);
    String code = metaTagResourceVisitor.extractEpCode(resource);
    verify(patientDAO).read(eq(new IdType("patient1")));
    assertEquals("foo", code);
  }

  @Test
  void extractEpCode_ClinicalImpression_missing_subject() {
    ClinicalImpression resource = new ClinicalImpression();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Resource 'ClinicalImpression' field 'subject' is required for meta tag", ex.getMessage());
  }

  @Test
  void extractEpCode_ClinicalImpression_patient_not_found() {
    ClinicalImpression resource = new ClinicalImpression();
    resource.setSubject(new Reference().setReference("patient1"));
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractEpCode(resource)
    );
    assertEquals("Resource not found 'Patient' with reference 'patient1'", ex.getMessage());
  }

  @Test
  void extractLdmCode_ServiceRequest() {
    final ServiceRequest resource = new ServiceRequest();
    resource.getPerformer().add(new Reference().setReference("foo/bar"));
    String code = metaTagResourceVisitor.extractLdmCode(resource);
    assertEquals("bar", code);
  }

  @Test
  void extractLdmCode_ServiceRequest_missing_field() {
    final ServiceRequest resource = new ServiceRequest();
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> metaTagResourceVisitor.extractLdmCode(resource)
    );
    assertEquals("Resource 'ServiceRequest' field 'performer' is required for meta tag", ex.getMessage());
  }

}