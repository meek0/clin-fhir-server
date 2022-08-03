package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.utils.ResourceFinder;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.auth0.jwk.Jwk;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static bio.ferlab.clin.interceptors.ServiceRequestPerformerInterceptor.ANALYSIS_REQUEST_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceRequestPerformerInterceptorTest {

  final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
  final ResourceFinder resourceFinder = Mockito.mock(ResourceFinder.class);
  final IFhirResourceDao<OrganizationAffiliation> dao = Mockito.mock(IFhirResourceDao.class);
  final ResourceDaoConfiguration configuration = new ResourceDaoConfiguration(null, null, null, null, null
  , dao, null, null, null, null, null);
  final ServiceRequestPerformerInterceptor interceptor = new ServiceRequestPerformerInterceptor(configuration, resourceFinder);
  
  @Test
  @DisplayName("ServiceRequest - missing code")
  void missingCode() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final ServiceRequest serviceRequest = new ServiceRequest().setCode(null);
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, serviceRequest)
    );
    assertEquals("Missing code/coding in service request", ex.getMessage());
  }

  @Test
  @DisplayName("ServiceRequest - missing coding")
  void missingCoding() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final ServiceRequest serviceRequest = new ServiceRequest().setCode(new CodeableConcept().setCoding(null));
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, serviceRequest)
    );
    assertEquals("Missing code/coding in service request", ex.getMessage());
  }

  @Test
  @DisplayName("ServiceRequest - empty code")
  void emptyCode() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final ServiceRequest serviceRequest = new ServiceRequest().setCode(new CodeableConcept().setCoding(List.of(new Coding().setCode(""))));
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, serviceRequest)
    );
    assertEquals("Missing code/coding in service request", ex.getMessage());
  }

  @Test
  @DisplayName("ServiceRequest - no matching affiliation")
  void noAffiliation() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final Patient patient = new Patient().setManagingOrganization(new Reference("ep1"));
    final ServiceRequest serviceRequest = new ServiceRequest().setCode(new CodeableConcept().setCoding(List.of(
        new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("foo"))));
    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(dao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(true);
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, serviceRequest)
    );
    assertEquals("Can't find organization affiliation attached to code: foo with ep: ep1", ex.getMessage());
  }

  @Test
  @DisplayName("ServiceRequest - no matching affiliation from multiple result")
  void noAffiliationFromMultiple() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final Patient patient = new Patient().setManagingOrganization(new Reference("ep3"));
    final ServiceRequest serviceRequest = new ServiceRequest()
        .setSubject(new Reference("patient"))
        .setCode(new CodeableConcept().setCoding(List.of(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("foo"))));
    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(dao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(false);
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    final OrganizationAffiliation affiliation = new OrganizationAffiliation()
        .setOrganization(new Reference("ep1"))
        .setParticipatingOrganization(new Reference("bar"));
    final OrganizationAffiliation affiliation2 = new OrganizationAffiliation()
        .setOrganization(new Reference("ep2"))
        .setParticipatingOrganization(new Reference("bar2"));
    when(bundle.getAllResources()).thenReturn(List.of(affiliation, affiliation2));
    Exception ex = Assertions.assertThrows(
        InvalidRequestException.class,
        () -> interceptor.created(requestDetails, serviceRequest)
    );
    assertEquals("Can't find organization affiliation attached to code: foo with ep: ep3", ex.getMessage());
  }

  @Test
  @DisplayName("ServiceRequest - create found an affiliation")
  void createMatchAnAffiliation() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final Patient patient = new Patient().setManagingOrganization(new Reference("ep2"));
    final ServiceRequest serviceRequest = new ServiceRequest()
        .setSubject(new Reference("patient"))
        .setCode(new CodeableConcept().setCoding(List.of(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("foo"))));
    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(dao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(false);
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    final OrganizationAffiliation affiliation = new OrganizationAffiliation()
        .setOrganization(new Reference("ep1"))
        .setParticipatingOrganization(new Reference("bar"));
    final OrganizationAffiliation affiliation2 = new OrganizationAffiliation()
        .setOrganization(new Reference("ep2"))
        .setParticipatingOrganization(new Reference("bar2"));
    when(bundle.getAllResources()).thenReturn(List.of(affiliation, affiliation2));
    interceptor.created(requestDetails, serviceRequest);
    assertEquals("bar2", serviceRequest.getPerformer().get(0).getReference());
  }

  @Test
  @DisplayName("ServiceRequest - update found an affiliation")
  void updateMatchAnAffiliation() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    final Patient patient = new Patient().setManagingOrganization(new Reference("ep1"));
    final ServiceRequest serviceRequest = new ServiceRequest()
        .setSubject(new Reference("patient"))
        .setCode(new CodeableConcept().setCoding(List.of(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("foo"))));
    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    when(dao.search(any())).thenReturn(bundle);
    when(bundle.isEmpty()).thenReturn(false);
    when(resourceFinder.findPatientFromRequestOrDAO(any(), any())).thenReturn(Optional.ofNullable(patient));
    final OrganizationAffiliation affiliation = new OrganizationAffiliation()
        .setOrganization(new Reference("ep1"))
        .setParticipatingOrganization(new Reference("bar"));
    when(bundle.getAllResources()).thenReturn(List.of(affiliation));
    interceptor.updated(requestDetails, null, serviceRequest);
    assertEquals("bar", serviceRequest.getPerformer().get(0).getReference());
  }

  @Test
  @DisplayName("ServiceRequest - ignore if performer already set")
  void performerAlreadySet() {
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    final Patient patient = new Patient().setManagingOrganization(new Reference("ep1"));
    final ServiceRequest serviceRequest = new ServiceRequest()
        .setSubject(new Reference("patient"))
        .setPerformer(List.of(new Reference("bar")))
        .setCode(new CodeableConcept().setCoding(List.of(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("foo"))));
    final IBundleProvider bundle = Mockito.mock(IBundleProvider.class);
    interceptor.created(requestDetails, serviceRequest);
    verify(resourceFinder,never()).findPatientFromRequestOrDAO(any(), any());
    verify(dao,never()).search(any());
    assertEquals("bar", serviceRequest.getPerformer().get(0).getReference());
  }

}