package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static bio.ferlab.clin.validation.validators.nanuq.PatientValidator.MRN_CODE;
import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ImmutableRamqInterceptorTest {

  final IFhirResourceDao<Person> personDao = Mockito.mock(IFhirResourceDao.class);
  final ResourceDaoConfiguration configuration = new ResourceDaoConfiguration(null, personDao,null, null, null
      , null, null, null, null, null, null);
  final ImmutableRamqInterceptor interceptor = new ImmutableRamqInterceptor(configuration);

  @Test
  void create_error() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    
    final Person person = new Person();
    person.setId("1");
    person.getIdentifierFirstRep().setValue("ramq").getType().getCodingFirstRep().setCode(RAMQ_CODE);

    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(personDao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(false);

    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, person)
    );
    assertEquals("Duplicated person with same RAMQ ramq...", ex.getMessage());
  }

  @Test
  void create_ok() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);

    final Person person = new Person();
    person.setId("1");
    person.getIdentifierFirstRep().setValue("ramq").getType().getCodingFirstRep().setCode(RAMQ_CODE);

    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(personDao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(true);

    interceptor.created(requestDetails, person);
  }
  
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
    assertEquals("Can't change the RAMQ (oldR...) of Person/1", ex.getMessage());
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