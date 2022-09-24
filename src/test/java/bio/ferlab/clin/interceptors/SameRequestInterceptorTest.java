package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Person;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SimplePreResourceShowDetails;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class SameRequestInterceptorTest {

  @Test
  void concatResources() {
    final RequestDetails rd1 = Mockito.mock(RequestDetails.class);
    final SimplePreResourceShowDetails part11 = new SimplePreResourceShowDetails(new ServiceRequest());
    final SimplePreResourceShowDetails part12 = new SimplePreResourceShowDetails(new Patient());
    final SimplePreResourceShowDetails part13 = new SimplePreResourceShowDetails(new Person());

    final RequestDetails rd2 = Mockito.mock(RequestDetails.class);
    final SimplePreResourceShowDetails part21 = new SimplePreResourceShowDetails(new ServiceRequest());
    final SimplePreResourceShowDetails part22 = new SimplePreResourceShowDetails(new Patient());
    
    final SameRequestInterceptor interceptor = new SameRequestInterceptor();
    interceptor.concatResources(part11, rd1);
    interceptor.concatResources(part12, rd1);
    interceptor.concatResources(part13, rd1);

    interceptor.concatResources(part21, rd2);
    interceptor.concatResources(part22, rd2);
    
    assertEquals(3, interceptor.get(rd1).size());
    assertEquals(2, interceptor.get(rd2).size());
  }
}