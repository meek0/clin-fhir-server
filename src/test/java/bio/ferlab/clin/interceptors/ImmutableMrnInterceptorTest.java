package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static bio.ferlab.clin.validation.validators.nanuq.PatientValidator.MRN_CODE;
import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ImmutableMrnInterceptorTest {

  final IFhirResourceDao<Patient> patientDao = Mockito.mock(IFhirResourceDao.class);
  final ResourceDaoConfiguration configuration = new ResourceDaoConfiguration(patientDao, null,null, null, null
      , null, null, null, null, null, null);
  final ImmutableMrnInterceptor interceptor = new ImmutableMrnInterceptor(configuration);
  
  @Test
  void create_error() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);

    final Patient patient = new Patient();
    patient.setId("1");
    patient.getIdentifierFirstRep().setValue("mrn").getType().getCodingFirstRep().setCode(MRN_CODE);

    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(patientDao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(false);

    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, patient)
    );
    assertEquals("Duplicated person with same MRN mrn...", ex.getMessage());
  }

  @Test
  void create_ok() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);

    final Patient patient = new Patient();
    patient.setId("1");
    patient.getIdentifierFirstRep().setValue("mrn").getType().getCodingFirstRep().setCode(MRN_CODE);

    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(patientDao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(true);

    interceptor.created(requestDetails, patient);
  }
  
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
    assertEquals("Can't change the MRN (oldM...) of Patient/1", ex.getMessage());
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