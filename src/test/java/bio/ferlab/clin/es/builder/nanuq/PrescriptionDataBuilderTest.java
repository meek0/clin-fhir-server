package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static bio.ferlab.clin.interceptors.ServiceRequestPerformerInterceptor.ANALYSIS_REQUEST_CODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrescriptionDataBuilderTest {

  final IFhirResourceDao<ServiceRequest> serviceRequestDao = Mockito.mock(IFhirResourceDao.class);
  final IFhirResourceDao<Patient> patientDao = Mockito.mock(IFhirResourceDao.class);
  final IFhirResourceDao<Organization> organizationDao = Mockito.mock(IFhirResourceDao.class);
  final IFhirResourceDao<Specimen> specimenDao = Mockito.mock(IFhirResourceDao.class);
  final IFhirResourceDao<Task> taskDao = Mockito.mock(IFhirResourceDao.class);
  final IFhirResourceDao<ClinicalImpression> clinicalImpressionDAO = Mockito.mock(IFhirResourceDao.class);
  final ResourceDaoConfiguration configuration = new ResourceDaoConfiguration(patientDao, null, serviceRequestDao, clinicalImpressionDAO, organizationDao
      , null, null, null, null, null, specimenDao, taskDao);

  private final AnalysisDataBuilder analysisDataBuilder = new AnalysisDataBuilder(configuration);
  private final SequencingDataBuilder sequencingDataBuilder = new SequencingDataBuilder(configuration);

  @Test
  public void analysisDataBuilder_not_analysis() {
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.getMeta().getProfile().add(new CanonicalType(AbstractPrescriptionDataBuilder.Type.SEQUENCING.value));

    when(serviceRequestDao.read(any(), any())).thenReturn(serviceRequest);
    when(clinicalImpressionDAO.search(any())).thenReturn(Mockito.mock(IBundleProvider.class));

    List<AnalysisData> data = analysisDataBuilder.fromIds(Set.of("serviceRequest1"), null);

    assertEquals(0, data.size());
  }

  @Test
  public void sequencingDataBuilder_not_sequencing() {
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.getMeta().getProfile().add(new CanonicalType(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value));

    when(serviceRequestDao.read(any(), any())).thenReturn(serviceRequest);

    List<SequencingData> data = sequencingDataBuilder.fromIds(Set.of("serviceRequest1"), null);

    assertEquals(0, data.size());
  }

  @Test
  public void analysisDataBuilder() {

    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setId("serviceRequest1");

    final Extension familyMemberExt = new Extension("http://fhir.cqgc.ferlab.bio/StructureDefinition/family-member");
    final Extension motherRefExt = new Extension("parent");
    motherRefExt.setValue(new Reference("Patient/motherRef"));
    final Extension motherRelationExt = new Extension("parent-relationship");
    final CodeableConcept motherCode = new CodeableConcept();
    motherCode.getCodingFirstRep().setCode("MTH");
    motherRelationExt.setValue(motherCode);
    familyMemberExt.addExtension(motherRefExt);
    familyMemberExt.addExtension(motherRelationExt);
    serviceRequest.addExtension(familyMemberExt);

    serviceRequest.getMeta().getProfile().add(new CanonicalType(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value));
    serviceRequest.getMeta().getSecurity().add(new Coding().setCode("TAG1"));
    serviceRequest.getSubject().setReference("patient1");
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
    serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ASAP);
    serviceRequest.getCode().getCoding().add(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("code1"));
    serviceRequest.setRequester(new Reference("requester1"));
    serviceRequest.getPerformer().add(new Reference("Organization/performer1"));
    serviceRequest.getPerformer().add(new Reference("PractitionerRole/assigned1"));
    serviceRequest.getPerformer().add(new Reference("PractitionerRole/assigned2"));
    final Date now = new Date();
    serviceRequest.setAuthoredOn(now);
    final Patient patient = new Patient();
    patient.getManagingOrganization().setReference("organization1");
    patient.getIdentifier().add(new Identifier().setType(new CodeableConcept().addCoding(new Coding().setCode("MR"))).setValue("RAMQ"));
    final Organization organization =new Organization();
    organization.getAlias().add(new StringType("orgAlias"));
    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(bundle.isEmpty()).thenReturn(false);
    final ServiceRequest sequencing1 = new ServiceRequest();
    sequencing1.setId("seq1");
    sequencing1.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
    sequencing1.setSubject(new Reference("p2"));
    final ServiceRequest sequencing2 = new ServiceRequest();
    sequencing2.setId("seq2");
    sequencing2.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
    sequencing2.setSubject(new Reference("p3"));
    when(bundle.getAllResources()).thenReturn(List.of(sequencing1, sequencing2));
    final Patient p2 = new Patient();
    p2.setId("p2");
    p2.getIdentifier().add(new Identifier().setType(new CodeableConcept().addCoding(new Coding().setCode("MR"))).setValue("P2"));
    final Patient p3 = new Patient();
    p3.setId("p3");
    p3.getIdentifier().add(new Identifier().setType(new CodeableConcept().addCoding(new Coding().setCode("MR"))).setValue("P3"));

    final Specimen specimen = new Specimen();
    specimen.getAccessionIdentifier().setValue("speciId");
    specimen.addParent().setReference("Specimen/foo");
    sequencing1.getSpecimen().addAll(List.of(new Reference("speci")));
    when(specimenDao.read(eq(new IdType("speci")), any())).thenReturn(specimen);

    final Task task1 = new Task();
    task1.getCode().getCodingFirstRep().setCode("TEBA");
    final Task task2 = new Task();
    task2.getCode().getCodingFirstRep().setCode("TNEBA");
    final IBundleProvider tasksBundle = Mockito.mock(IBundleProvider.class);
    when(tasksBundle.isEmpty()).thenReturn(false);
    when(tasksBundle.getAllResources()).thenReturn(List.of(task1, task2));

    when(serviceRequestDao.read(any(), any())).thenReturn(serviceRequest);
    when(patientDao.read(any())).thenReturn(patient).thenReturn(p2).thenReturn(p3);
    when(organizationDao.read(any())).thenReturn(organization);
    when(serviceRequestDao.search(any())).thenReturn(bundle);
    when(taskDao.search(any())).thenReturn(tasksBundle);

    List<AnalysisData> results = analysisDataBuilder.fromIds(Set.of("serviceRequest1"), requestDetails);

    verify(serviceRequestDao).read(eq(new IdType("serviceRequest1")), eq(requestDetails));
    verify(patientDao).read(eq(new IdType("patient1")));
    verify(patientDao).read(eq(new IdType("p2")));
    verify(patientDao).read(eq(new IdType("p3")));
    verify(organizationDao).read(eq(new IdType("organization1")));
    verify(serviceRequestDao).search(any());
    verify(taskDao, times(2)).search(any());

    assertEquals(1, results.size());
    AnalysisData data1 = results.get(0);
    assertEquals("patient1", data1.getPatientId());
    assertEquals(List.of("TAG1"), data1.getSecurityTags());
    assertEquals("draft", data1.getStatus());
    assertEquals("asap", data1.getPriority());
    assertEquals(false, data1.isPrenatal());
    assertEquals("code1", data1.getAnalysisCode());
    assertEquals("requester1", data1.getRequester());
    assertEquals("performer1", data1.getLdm());
    assertEquals("RAMQ", data1.getPatientMRN());
    assertEquals("orgAlias", data1.getEp());
    final String nowStr = analysisDataBuilder.formatter.get().format(now);
    assertEquals(nowStr, data1.getCreatedOn());
    assertEquals("serviceRequest1", data1.getPrescriptionId());
    assertEquals(2, data1.getSequencingRequests().size());
    assertEquals("p2", data1.getSequencingRequests().get(0).getPatientId());
    assertEquals("P2", data1.getSequencingRequests().get(0).getPatientMRN());
    assertEquals("seq1", data1.getSequencingRequests().get(0).getRequestId());
    assertEquals("draft", data1.getSequencingRequests().get(0).getStatus());
    assertEquals("speciId", data1.getSequencingRequests().get(0).getSample());
    assertEquals("seq2", data1.getSequencingRequests().get(1).getRequestId());
    assertEquals("completed", data1.getSequencingRequests().get(1).getStatus());
    assertEquals("", data1.getSequencingRequests().get(1).getSample());
    assertEquals("p3", data1.getSequencingRequests().get(1).getPatientId());
    assertEquals("P3", data1.getSequencingRequests().get(1).getPatientMRN());
    assertEquals(2, data1.getAssignments().size());
    assertEquals("assigned1", data1.getAssignments().get(0));
    assertEquals("assigned2", data1.getAssignments().get(1));
    assertEquals("[TEBA, TNEBA]", data1.getTasks().toString());
  }

  @Test
  public void sequencingDataBuilder() {

    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setId("serviceRequest1");
    serviceRequest.getMeta().getProfile().add(new CanonicalType(AbstractPrescriptionDataBuilder.Type.SEQUENCING.value));
    serviceRequest.getMeta().getSecurity().add(new Coding().setCode("TAG1"));
    serviceRequest.getSubject().setReference("patient1");
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
    serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ASAP);
    serviceRequest.getCode().getCoding().add(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("code1"));
    serviceRequest.setRequester(new Reference("requester1"));
    serviceRequest.getPerformer().add(new Reference("Organization/performer1"));
    serviceRequest.getBasedOn().add(new Reference().setReference("parentAnalysis"));
    final Date now = new Date();
    serviceRequest.setAuthoredOn(now);
    final Patient patient = new Patient();
    patient.getManagingOrganization().setReference("organization1");
    patient.getIdentifier().add(new Identifier().setType(new CodeableConcept().addCoding(new Coding().setCode("MR"))).setValue("RAMQ"));
    final Organization organization =new Organization();
    organization.getAlias().add(new StringType("orgAlias"));
    final Specimen specimen1 = new Specimen();
    specimen1.getParent().add(new Reference("parent1"));
    specimen1.getAccessionIdentifier().setValue("speciId1");
    final Specimen specimen2 = new Specimen(); // no parent
    specimen2.getAccessionIdentifier().setValue("speciId2");
    serviceRequest.getSpecimen().addAll(List.of(new Reference("speci1"), new Reference("speci2")));

    final ServiceRequest parentAnalysis = new ServiceRequest();
    parentAnalysis.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
    final Extension familyMemberExt = new Extension("http://fhir.cqgc.ferlab.bio/StructureDefinition/family-member");
    final Extension motherRefExt = new Extension("parent");
    motherRefExt.setValue(new Reference("Patient/motherRef"));
    final Extension motherRelationExt = new Extension("parent-relationship");
    final CodeableConcept motherCode = new CodeableConcept();
    motherCode.getCodingFirstRep().setCode("MTH");
    motherRelationExt.setValue(motherCode);
    familyMemberExt.addExtension(motherRefExt);
    familyMemberExt.addExtension(motherRelationExt);
    parentAnalysis.addExtension(familyMemberExt);

    final Task task1 = new Task();
    task1.getCode().getCodingFirstRep().setCode("TEBA");
    final IBundleProvider tasksBundle = Mockito.mock(IBundleProvider.class);
    when(tasksBundle.isEmpty()).thenReturn(false);
    when(tasksBundle.getAllResources()).thenReturn(List.of(task1));

    when(serviceRequestDao.read(any(), any()))
        .thenReturn(serviceRequest)
        .thenReturn(parentAnalysis);
    when(patientDao.read(any())).thenReturn(patient);
    when(organizationDao.read(any())).thenReturn(organization);
    when(specimenDao.read(eq(new IdType("speci1")), any())).thenReturn(specimen1);
    when(specimenDao.read(eq(new IdType("speci2")), any())).thenReturn(specimen2);
    when(taskDao.search(any())).thenReturn(tasksBundle);

    List<SequencingData> results = sequencingDataBuilder.fromIds(Set.of("serviceRequest1"), requestDetails);

    verify(serviceRequestDao).read(eq(new IdType("serviceRequest1")), eq(requestDetails));
    verify(serviceRequestDao).read(eq(new IdType("parentAnalysis")), eq(requestDetails));
    verify(patientDao).read(eq(new IdType("patient1")));
    verify(organizationDao).read(eq(new IdType("organization1")));
    verify(specimenDao).read(eq(new IdType("speci1")), any());
    verify(specimenDao, never()).read(eq(new IdType("speci2")), any());
    verify(taskDao).search(any());

    assertEquals(1, results.size());
    SequencingData data1 = results.get(0);
    assertEquals("patient1", data1.getPatientId());
    assertEquals(List.of("TAG1"), data1.getSecurityTags());
    assertEquals("draft", data1.getStatus());
    assertEquals("asap", data1.getPriority());
    assertEquals(false, data1.isPrenatal());
    assertEquals("code1", data1.getAnalysisCode());
    assertEquals("requester1", data1.getRequester());
    assertEquals("performer1", data1.getLdm());
    assertEquals("RAMQ", data1.getPatientMRN());
    assertEquals("orgAlias", data1.getEp());
    final String nowStr = analysisDataBuilder.formatter.get().format(now);
    assertEquals(nowStr, data1.getCreatedOn());
    assertEquals("serviceRequest1", data1.getRequestId());
    assertEquals("parentAnalysis", data1.getPrescriptionId());
    assertEquals("active", data1.getPrescriptionStatus());
    assertEquals("speciId1", data1.getSample());
    assertEquals("[TEBA]", data1.getTasks().toString());
  }

}