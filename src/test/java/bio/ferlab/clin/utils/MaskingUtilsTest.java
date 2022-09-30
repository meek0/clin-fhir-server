package bio.ferlab.clin.utils;

import bio.ferlab.clin.es.builder.nanuq.AbstractPrescriptionDataBuilder;
import ca.uhn.fhir.rest.api.server.SimplePreResourceShowDetails;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static bio.ferlab.clin.interceptors.PrescriptionMaskingInterceptor.RESTRICTED_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class MaskingUtilsTest {

  @Test
  void areLinked_ServiceRequest_Patient() {
    final ServiceRequest sr1 = new ServiceRequest();
    sr1.setSubject(new Reference("Patient/p1"));
    final ServiceRequest sr2 = new ServiceRequest();
    sr2.setSubject(new Reference("Patient/p2"));
    final Patient p1 = new Patient();
    p1.setId("p1");
    final Patient p2 = new Patient();
    p2.setId("p2");
    
    assertTrue(MaskingUtils.areLinked(sr1, p1));
    assertTrue(MaskingUtils.areLinked(sr2, p2));

    assertFalse(MaskingUtils.areLinked(sr1, null));
    assertFalse(MaskingUtils.areLinked((ServiceRequest) null, p1));

    assertFalse(MaskingUtils.areLinked(sr1, p2));
    assertFalse(MaskingUtils.areLinked(sr2, p1));
  }

  @Test
  void areLinked_Person_Patient() {
    final Person pers1 = new Person();
    pers1.addLink().setTarget(new Reference("Patient/p1"));
    final Person pers2 = new Person();
    pers2.addLink().setTarget(new Reference("Patient/p2"));
    final Person pers3 = new Person();
    pers3.addLink().setTarget(new Reference("Patient/p1"));
    pers3.addLink().setTarget(new Reference("Patient/p2"));
    
    final Patient p1 = new Patient();
    p1.setId("p1");
    final Patient p2 = new Patient();
    p2.setId("p2");

    assertTrue(MaskingUtils.areLinked(pers1, p1));
    assertTrue(MaskingUtils.areLinked(pers2, p2));

    assertFalse(MaskingUtils.areLinked(pers1, null));
    assertFalse(MaskingUtils.areLinked((ServiceRequest) null, p1));

    assertFalse(MaskingUtils.areLinked(pers1, p2));
    assertFalse(MaskingUtils.areLinked(pers2, p1));
  }
  
  @Test
  void isAlreadyMasked() {
    final Person p1 = new Person();
    p1.setId(RESTRICTED_FIELD);
    assertTrue(MaskingUtils.isAlreadyMasked(p1));
    p1.setId("foo");
    assertFalse(MaskingUtils.isAlreadyMasked(p1));
  }
  
  @Test
  void isValidPrescriptionRequest_valid() {
    final ServiceRequest sr = new ServiceRequest();
    sr.getMeta().addProfile(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value);
    sr.setSubject(new Reference("Patient/p1"));
    final Patient p = new Patient();
    p.setId("p1");
    final Person pers = new Person();
    pers.setId("pers1");
    pers.addLink().setTarget(new Reference("Patient/p1"));
    assertTrue(MaskingUtils.isValidPrescriptionRequest(List.of(sr, p, pers)));
  }

  @Test
  void isValidPrescriptionRequest_not_linked() {
    final ServiceRequest sr = new ServiceRequest();
    sr.getMeta().addProfile(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value);
    sr.setSubject(new Reference("Patient/p1"));
    final Patient p = new Patient();
    p.setId("p2");
    final Person pers = new Person();
    pers.setId("pers1");
    pers.addLink().setTarget(new Reference("Patient/p3"));
    assertFalse(MaskingUtils.isValidPrescriptionRequest(List.of(sr, p, pers)));
  }

  @Test
  void isValidPrescriptionRequest_not_analysis() {
    final ServiceRequest sr = new ServiceRequest();
    assertFalse(MaskingUtils.isValidPrescriptionRequest(List.of(sr)));
  }
  
  @Test
  void extractAnalysis() {
    final ServiceRequest analysis = new ServiceRequest();
    analysis.getMeta().addProfile(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value);
    ServiceRequest result = MaskingUtils.extractAnalysis(List.of(new ServiceRequest(), new ServiceRequest(), analysis));
    assertEquals(analysis, result);
  }
  
  @Test
  void extractPerson() {
    final Person person = new Person();
    final SimplePreResourceShowDetails details = new SimplePreResourceShowDetails(person);
    assertEquals(person, MaskingUtils.extractPerson(details));
    
    assertNull(MaskingUtils.extractPerson(new SimplePreResourceShowDetails(new ServiceRequest())));
  }
  
}