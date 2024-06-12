package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.interceptors.SameRequestInterceptor;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess.USER_ALL_TAGS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaTagPersonTest {
  
  final SameRequestInterceptor sameRequestInterceptor = Mockito.mock(SameRequestInterceptor.class);
  final IFhirResourceDao<Person> pesonDao = Mockito.mock(IFhirResourceDao.class);
  final IFhirResourceDao<ServiceRequest> serviceRequestDao = Mockito.mock(IFhirResourceDao.class);
  final ResourceDaoConfiguration daoConfiguration = new ResourceDaoConfiguration(null, pesonDao, serviceRequestDao, null, null
      , null, null, null, null, null, null, null);
  final MetaTagPerson metaTagPerson = new MetaTagPerson(sameRequestInterceptor, daoConfiguration);
  
  @Test
  void canSeeResource_not_only_prescriber() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final MetaTagResourceAccess access = Mockito.mock(MetaTagResourceAccess.class);
    when(access.getUserRoles(any())).thenReturn(List.of("clin_prescriber", "clin_genetician"));
    final Person person = new Person();
    assertTrue(metaTagPerson.canSeeResource(access, requestDetails, person));
  }

  @Test
  void canSeeResource_system() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final MetaTagResourceAccess access = Mockito.mock(MetaTagResourceAccess.class);
    when(access.getUserTags(any())).thenReturn(List.of(USER_ALL_TAGS));
    final Person person = new Person();
    assertTrue(metaTagPerson.canSeeResource(access, requestDetails, person));
  }

  @Test
  void canSeeResource_only_genetician() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final MetaTagResourceAccess access = Mockito.mock(MetaTagResourceAccess.class);
    when(access.getUserRoles(any())).thenReturn(List.of());
    when(access.getUserTags(any())).thenReturn(List.of("LDM-1"));
    final Person person = new Person();
    when(sameRequestInterceptor.get(any())).thenReturn(List.of(person));
    
    final IBundleProvider personBundle = Mockito.mock(IBundleProvider.class);
    when(pesonDao.search(any())).thenReturn(personBundle);
    when(personBundle.isEmpty()).thenReturn(false);
    when(personBundle.getAllResources()).thenReturn(List.of(new Patient()));
    
    final ServiceRequest sr1 = new ServiceRequest();
    when(access.getResourceTags(any())).thenReturn(List.of("LDM-1"));
    
    final IBundleProvider serviceRequestBundle = Mockito.mock(IBundleProvider.class);
    when(serviceRequestDao.search(any())).thenReturn(serviceRequestBundle);
    when(serviceRequestBundle.isEmpty()).thenReturn(false);
    when(serviceRequestBundle.getAllResources()).thenReturn(List.of(sr1));
    
    assertTrue(metaTagPerson.canSeeResource(access, requestDetails, person));

    verify(access).getUserRoles(requestDetails);
    verify(access).getUserTags(requestDetails);
    verify(access).getResourceTags(sr1);
  }

}