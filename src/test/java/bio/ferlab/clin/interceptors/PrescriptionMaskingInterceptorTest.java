package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.builder.nanuq.AbstractPrescriptionDataBuilder;
import bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SimplePreResourceShowDetails;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static bio.ferlab.clin.interceptors.PrescriptionMaskingInterceptor.RESTRICTED_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionMaskingInterceptorTest {
  
  private final MetaTagResourceAccess metaTagResourceAccess = Mockito.mock(MetaTagResourceAccess.class);
  private final SameRequestInterceptor sameRequestInterceptor = Mockito.mock(SameRequestInterceptor.class);
  private final PrescriptionMaskingInterceptor prescriptionMaskingInterceptor = 
      new PrescriptionMaskingInterceptor(metaTagResourceAccess, sameRequestInterceptor);
  
  @Test
  void doNothing() {
    final RequestDetails req1 = Mockito.mock(RequestDetails.class);
    when(sameRequestInterceptor.get(any())).thenReturn(List.of());
    prescriptionMaskingInterceptor.preShow(null, req1);
    verify(sameRequestInterceptor).get(req1);
  }

  @Test
  void notValidPrescription() {
    final RequestDetails req1 = Mockito.mock(RequestDetails.class);
    final ServiceRequest sr = new ServiceRequest();
    when(sameRequestInterceptor.get(any())).thenReturn(List.of(sr));
    prescriptionMaskingInterceptor.preShow(null, req1);
    verify(sameRequestInterceptor).get(req1);
  }

  @Test
  void validPrescription() {
    final RequestDetails req1 = Mockito.mock(RequestDetails.class);
    final ServiceRequest sr = new ServiceRequest();
    sr.getMeta().addProfile(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value);
    sr.setSubject(new Reference("Patient/p1"));
    final Patient p = new Patient();
    p.setId("p1");
    final Person pers = new Person();
    pers.setId("pers1");
    pers.addLink().setTarget(new Reference("Patient/p1"));
    final SimplePreResourceShowDetails details = new SimplePreResourceShowDetails(pers);
    when(sameRequestInterceptor.get(any())).thenReturn(List.of(sr, p, pers));
    prescriptionMaskingInterceptor.preShow(details, req1);
    verify(metaTagResourceAccess).getUserTags(req1);
  }

  @Test
  void validPrescription_notMasked_linked_tags_match() {
    final RequestDetails req1 = Mockito.mock(RequestDetails.class);
    final ServiceRequest sr = new ServiceRequest();
    sr.getMeta().addProfile(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value);
    sr.setSubject(new Reference("Patient/p1"));
    final Patient p = new Patient();
    p.setId("p1");
    final Person pers = new Person();
    pers.setId("pers1");
    pers.addLink().setTarget(new Reference("Patient/p1"));
    final SimplePreResourceShowDetails details = new SimplePreResourceShowDetails(pers);
    when(metaTagResourceAccess.getUserTags(any())).thenReturn(List.of("tag1", "tag2"));
    when(metaTagResourceAccess.getResourceTags(any())).thenReturn(List.of("tag1"));
    when(sameRequestInterceptor.get(any())).thenReturn(List.of(sr, p, pers));
    prescriptionMaskingInterceptor.preShow(details, req1);
    verify(sameRequestInterceptor).get(req1);
    verify(metaTagResourceAccess).getUserTags(req1);
    verify(metaTagResourceAccess).getResourceTags(sr);
    assertEquals("pers1", pers.getId());  // not masked, because tags match
  }

  @Test
  void validPrescription_notMasked_linked_tags_not_match() {
    final RequestDetails req1 = Mockito.mock(RequestDetails.class);
    final ServiceRequest sr = new ServiceRequest();
    sr.getMeta().addProfile(AbstractPrescriptionDataBuilder.Type.ANALYSIS.value);
    sr.setSubject(new Reference("Patient/p1"));
    final Patient p = new Patient();
    p.setId("p1");
    final Person pers = new Person();
    pers.setId("pers1");
    pers.addLink().setTarget(new Reference("Patient/p1"));
    final SimplePreResourceShowDetails details = new SimplePreResourceShowDetails(pers);
    when(metaTagResourceAccess.getUserTags(any())).thenReturn(List.of("tag1", "tag2"));
    when(metaTagResourceAccess.getResourceTags(any())).thenReturn(List.of("tag3"));
    when(sameRequestInterceptor.get(any())).thenReturn(List.of(sr, p, pers));
    prescriptionMaskingInterceptor.preShow(details, req1);
    verify(sameRequestInterceptor).get(req1);
    verify(metaTagResourceAccess).getUserTags(req1);
    verify(metaTagResourceAccess).getResourceTags(sr);
    assertEquals(RESTRICTED_FIELD, pers.getId());  // masked, because tags not match
  }
}