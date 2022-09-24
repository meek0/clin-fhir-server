package bio.ferlab.clin.utils;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskingUtilsTest {

  @Test
  void isLinkedTo_ServiceRequest_Patient() {
    final ServiceRequest sr1 = new ServiceRequest();
    sr1.setSubject(new Reference("Patient/p1"));
    final ServiceRequest sr2 = new ServiceRequest();
    sr2.setSubject(new Reference("Patient/p2"));
    final Patient p1 = new Patient();
    p1.setId("p1");
    final Patient p2 = new Patient();
    p2.setId("p2");
    
    assertTrue(MaskingUtils.isLinkedTo(sr1, p1));
    assertTrue(MaskingUtils.isLinkedTo(sr2, p2));

    assertFalse(MaskingUtils.isLinkedTo(sr1, null));
    assertFalse(MaskingUtils.isLinkedTo((ServiceRequest) null, p1));

    assertFalse(MaskingUtils.isLinkedTo(sr1, p2));
    assertFalse(MaskingUtils.isLinkedTo(sr2, p1));
  }

  @Test
  void isLinkedTo_Person_Patient() {
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

    assertTrue(MaskingUtils.isLinkedTo(pers1, p1));
    assertTrue(MaskingUtils.isLinkedTo(pers2, p2));

    assertFalse(MaskingUtils.isLinkedTo(pers1, null));
    assertFalse(MaskingUtils.isLinkedTo((ServiceRequest) null, p1));

    assertFalse(MaskingUtils.isLinkedTo(pers1, p2));
    assertFalse(MaskingUtils.isLinkedTo(pers2, p1));
  }
  
}