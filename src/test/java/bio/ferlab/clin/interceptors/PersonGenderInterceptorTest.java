package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonGenderInterceptorTest {

  final IFhirResourceDao<Patient> patientDao = Mockito.mock(IFhirResourceDao.class);
  final ResourceDaoConfiguration configuration = new ResourceDaoConfiguration(patientDao, null,null, null, null
      , null, null, null, null, null, null);
  final PersonGenderInterceptor interceptor = new PersonGenderInterceptor(configuration);
  
  @Test
  void should_not_update_null() {
    shouldUpdate(null, null, false);
  }
  
  @Test
  void should_not_update_new_gender_null() {
    shouldUpdate("male", null, false);
  }

  @Test
  void should_update_new_gender() {
    shouldUpdate(null, "male", true);
  }

  @Test
  void should_update_replace_gender() {
    shouldUpdate("male", "female", true);
  }
  
  void shouldUpdate(String oldGenderCode, String newGenderCode, boolean shouldUpdate) {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    
    final Person oldPerson = new Person();
    oldPerson.setGender(Enumerations.AdministrativeGender.fromCode(oldGenderCode));
    final Person newPerson = new Person();
    newPerson.setGender(Enumerations.AdministrativeGender.fromCode(newGenderCode));
    newPerson.addLink().setTarget(new Reference("Patient/1"));
    newPerson.addLink().setTarget(new Reference("Patient/2"));
    
    final Patient p1 = new Patient();
    final Patient p2 = new Patient();
    
    final IBundleProvider bundle1 = Mockito.mock(IBundleProvider.class);
    when(bundle1.isEmpty()).thenReturn(false);
    when(bundle1.getAllResources()).thenReturn(List.of(p1));

    final IBundleProvider bundle2 = Mockito.mock(IBundleProvider.class);
    when(bundle2.isEmpty()).thenReturn(false);
    when(bundle2.getAllResources()).thenReturn(List.of(p2));
    
    when(patientDao.search(any())).thenReturn(bundle1).thenReturn(bundle2);

    this.interceptor.updated(requestDetails, oldPerson, newPerson);
    
    if (shouldUpdate) {
      verify(patientDao).update(p1);
      verify(patientDao).update(p2);
      Assertions.assertEquals(newGenderCode, p1.getGender().toCode());
      Assertions.assertEquals(newGenderCode, p2.getGender().toCode());
    }else {
      verify(patientDao, never()).update(any(Patient.class));
    }
  }

}