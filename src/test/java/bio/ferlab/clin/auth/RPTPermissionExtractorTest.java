package bio.ferlab.clin.auth;

import bio.ferlab.clin.exceptions.RptIntrospectionException;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.junit.jupiter.api.*;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RPTPermissionExtractorTest {
  
  final KeycloakClient keycloakClient = Mockito.mock(KeycloakClient.class);
  final TokenIntrospectionResponse introspectionResponse = Mockito.mock(TokenIntrospectionResponse.class);
  final RPTPermissionExtractor extractor = new RPTPermissionExtractor();
  
  @BeforeEach 
  void beforeEach() {
    when(keycloakClient.introspectRpt(anyString())).thenReturn(introspectionResponse);
  }
  
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