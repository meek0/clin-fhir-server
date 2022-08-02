package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static bio.ferlab.clin.validation.validators.nanuq.PatientValidator.MRN_CODE;
import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class ImmutableMrnInterceptorTest {
  
  final ImmutableMrnInterceptor interceptor = new ImmutableMrnInterceptor();
  
  @Test
  void update_error() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    
    final Patient before = new Patient();
    before.setId("1");
    before.getIdentifierFirstRep().setValue("oldMrn").getType().getCodingFirstRep().setCode(MRN_CODE);
    final Patient after = new Patient();
    after.setId("1");
    after.getIdentifierFirstRep().setValue("newMrn").getType().getCodingFirstRep().setCode(MRN_CODE);

    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.updated(requestDetails, before, after)
    );
    assertEquals("Can't change the MRN of Patient/1", ex.getMessage());
  }

  @Test
  void update_ok() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.PUT);

    final Patient before = new Patient();
    before.getIdentifierFirstRep().setValue("sameMrn").getType().getCodingFirstRep().setCode(MRN_CODE);
    final Patient after = new Patient();
    after.getIdentifierFirstRep().setValue("sameMrn").getType().getCodingFirstRep().setCode(MRN_CODE);

    interceptor.updated(requestDetails, before, after);
  }

}