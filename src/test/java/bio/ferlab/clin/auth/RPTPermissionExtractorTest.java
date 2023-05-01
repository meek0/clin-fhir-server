package bio.ferlab.clin.auth;

import bio.ferlab.clin.auth.data.UserPermissions;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.*;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RPTPermissionExtractorTest {

  private final Algorithm algorithm = Algorithm.HMAC256("secret");
  
  //final KeycloakClient keycloakClient = Mockito.mock(KeycloakClient.class);
  //final TokenIntrospectionResponse introspectionResponse = Mockito.mock(TokenIntrospectionResponse.class);
  final RPTPermissionExtractor extractor = new RPTPermissionExtractor();
  
  /*@BeforeEach
  void beforeEach() {
    when(keycloakClient.introspectRpt(anyString())).thenReturn(introspectionResponse);
  }*/
  
  @Nested
  class Extract {
    @Test
    void missing_token() {
      final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
      when(requestDetails.getHeader(any())).thenReturn(null);
                    
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> extractor.extract(requestDetails)
      );
      assertTrue(ex.getMessage().equals("Missing bearer token in header"));
    }

    @Test
    void checkAuthorization_ok_token() {
      final var perms = new HashMap<String, List<Map<String, Object>>>();
      perms.put("permissions", List.of(Map.of("rsname", "Bundle", "scopes", List.of("read", "create"))));
      final String token = JWT.create().withClaim("authorization", perms).sign(algorithm);
      final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
      when(requestDetails.getHeader(any())).thenReturn("Bearer " + token);
      UserPermissions userPerms = extractor.extract(requestDetails);
      assertEquals(1, userPerms.getPermissions().length);
      final var res1 = Arrays.stream(userPerms.getPermissions()).findFirst().get();
      assertEquals(Bundle.class, res1.getResourceType());
      assertTrue(res1.read);
      assertTrue(res1.create);
      assertFalse(res1.update);
      assertFalse(res1.delete);
    }

    /*@Test
    void rpt_token_required() {
      final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
      when(requestDetails.getHeader(any())).thenReturn("Bearer a.b.c");
      when(introspectionResponse.getPermissions()).thenReturn(null);

      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> extractor.extract(requestDetails)
      );
      assertTrue(ex.getMessage().equals("not active rpt token"));
    }
    @Test
    void not_active_token() {
      final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
      when(requestDetails.getHeader(any())).thenReturn("Bearer a.b.c");
      when(introspectionResponse.getPermissions()).thenReturn(new ArrayList<>());
      when(introspectionResponse.isActive()).thenReturn(false);

      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> extractor.extract(requestDetails)
      );
      assertTrue(ex.getMessage().equals("not active rpt token"));
    }*/
  }

}