package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static bio.ferlab.clin.interceptors.metatag.MetaTagResourceAccess.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaTagResourceAccessTest {
  
  private final BioProperties bioProperties = Mockito.mock(BioProperties.class);
  private final MetaTagPerson metaTagPerson = Mockito.mock(MetaTagPerson.class);
  private final PrescriptionMasking prescriptionMasking = Mockito.mock(PrescriptionMasking.class);
  private final MetaTagResourceAccess metaTagResourceAccess = new MetaTagResourceAccess(bioProperties, metaTagPerson, prescriptionMasking);
  
  @BeforeEach
  void beforeEach() {
    when(bioProperties.isTaggingEnabled()).thenReturn(true);
    when(bioProperties.getAuthSystemId()).thenReturn("system");
  }
  
  @Test
  void isResourceWithTags() {
    assertTrue(metaTagResourceAccess.isResourceWithTags(RESOURCES_WITH_TAGS.get(0)));
    assertFalse(metaTagResourceAccess.isResourceWithTags("AuditEvent"));
    assertFalse(metaTagResourceAccess.isResourceWithTags(new AuditEvent()));
    assertFalse(metaTagResourceAccess.isResourceWithTags(""));
  }
  
  @Test
  void getUserRoles() {
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    String bearer = JWT.create()
        .withClaim("realm_access", Map.of("roles", List.of("foo", "bar", "clin-1", "clin_2")))
        .sign(Algorithm.HMAC256("secret"));
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    List<String> roles = metaTagResourceAccess.getUserRoles(requestDetails);
    assertEquals(2, roles.size());
    assertEquals("clin-1", roles.get(0));
    assertEquals("clin_2", roles.get(1));
  }

  @Test
  void getUserRoles_missing_roles() {
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    String bearer = JWT.create()
        .sign(Algorithm.HMAC256("secret"));
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    Exception ex = Assertions.assertThrows(
        RptIntrospectionException.class,
        () ->  metaTagResourceAccess.getUserRoles(requestDetails)
    );
    assertEquals("missing realm_access", ex.getMessage());
  }
  
  @Test
  void getUserTags_missing_fhir_organization_id() {
    String bearer = JWT.create()
        .sign(Algorithm.HMAC256("secret"));
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    Exception ex = Assertions.assertThrows(
        RptIntrospectionException.class,
        () ->  metaTagResourceAccess.getUserTags(requestDetails)
    );
    assertEquals("missing fhir_organization_id", ex.getMessage());
  }

  @Test
  void getUserTags_all_tags() {
    String bearer = JWT.create()
        .withClaim("azp", "system")
        .sign(Algorithm.HMAC256("secret"));
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    List<String> tags = metaTagResourceAccess.getUserTags(requestDetails);
    assertTrue(tags.size() == 1 && tags.contains(USER_ALL_TAGS));
  }

  @Test
  void getUserTags() {
    String bearer = JWT.create()
        .withClaim(TOKEN_ATTR_FHIR_ORG_ID, List.of("tag1", "tag2"))
        .sign(Algorithm.HMAC256("secret"));
    RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    List<String> tags = metaTagResourceAccess.getUserTags(requestDetails);
    assertTrue(tags.containsAll(List.of("tag1", "tag2")));
  }
  
  @Test
  void canSeeResource() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
    String bearer = JWT.create()
        .withClaim(TOKEN_ATTR_FHIR_ORG_ID, List.of("tag1", "tag2"))
        .withClaim("realm_access", Map.of("roles", List.of()))
        .sign(Algorithm.HMAC256("secret"));
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    final Patient resource = new Patient();
    resource.getMeta().addSecurity().setCode("tag3"); // not allowed to see this resource
    assertFalse(metaTagResourceAccess.canSeeResource(requestDetails, resource));
    resource.getMeta().addSecurity().setCode("tag1"); // allowed to see this resource
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, resource));
  }

  @Test
  void canSeeResource_all_tags() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    String bearer = JWT.create()
        .withClaim("azp", "system")
        .withClaim("realm_access", Map.of("roles", List.of()))
        .sign(Algorithm.HMAC256("secret"));
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    final ServiceRequest resource = new ServiceRequest();
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, resource));
  }

  @Test
  void canSeeResource_Genetician_tags() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    String bearer = JWT.create()
        .withClaim(TOKEN_ATTR_FHIR_ORG_ID, List.of())
        .withClaim("realm_access", Map.of("roles", List.of("clin_genetician")))
        .sign(Algorithm.HMAC256("secret"));
    when(requestDetails.getHeader("Authorization")).thenReturn("Bearer "+ bearer);
    final ServiceRequest resource = new ServiceRequest();
    resource.getMeta().addSecurity().setCode("anyTag"); // allowed because LDM can see all of them
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, resource));
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, new Patient()));
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, new Observation()));
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, new ClinicalImpression()));
  }

  @Test
  void canSeeResource_tagging_disabled() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(bioProperties.isTaggingEnabled()).thenReturn(false);
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, null));
  }

  @Test
  void canSeeResource_not_tagged_resource() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(bioProperties.isTaggingEnabled()).thenReturn(true);
    assertTrue(metaTagResourceAccess.canSeeResource(requestDetails, new AuditEvent()));
  }
  
  @Test
  void canSeeResource_Person() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final Person pers = new Person();
    metaTagResourceAccess.canSeeResource(requestDetails, pers);
    verify(metaTagPerson).canSeeResource(metaTagResourceAccess, requestDetails, pers);
  }
  
  @Test
  void preShow_Person() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final Person person = new Person();
    metaTagResourceAccess.preShow(requestDetails, person);
    verify(prescriptionMasking).preShow(metaTagResourceAccess, person, requestDetails);
  }
}