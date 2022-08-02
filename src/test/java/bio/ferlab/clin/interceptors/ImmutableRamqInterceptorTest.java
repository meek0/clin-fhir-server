package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.hl7.fhir.r4.model.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ImmutableRamqInterceptorTest {
  
  final ImmutableRamqInterceptor interceptor = new ImmutableRamqInterceptor();
  
  @Test
  void update_error() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    
    final Person before = new Person();
    before.setId("1");
    before.getIdentifierFirstRep().setValue("oldRamq").getType().getCodingFirstRep().setCode(RAMQ_CODE);
    final Person after = new Person();
    after.setId("1");
    after.getIdentifierFirstRep().setValue("newRamq").getType().getCodingFirstRep().setCode(RAMQ_CODE);

    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.updated(requestDetails, before, after)
    );
    assertEquals("Can't change the RAMQ of Person/1", ex.getMessage());
  }

  @Test
  void update_ok() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.PUT);

    final Person before = new Person();
    before.getIdentifierFirstRep().setValue("sameRamq").getType().getCodingFirstRep().setCode(RAMQ_CODE);
    final Person after = new Person();
    after.getIdentifierFirstRep().setValue("sameRamq").getType().getCodingFirstRep().setCode(RAMQ_CODE);

    interceptor.updated(requestDetails, before, after);
  }

}