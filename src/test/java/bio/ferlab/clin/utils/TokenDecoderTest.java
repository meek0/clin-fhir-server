package bio.ferlab.clin.utils;

import bio.ferlab.clin.auth.JwkProviderService;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TokenDecoderTest {
  
  final JwkProviderService jwkProviderService = Mockito.mock(JwkProviderService.class);
  final Jwk jwk = Mockito.mock(Jwk.class);
  final TokenDecoder decoder = new TokenDecoder(jwkProviderService);
  
  @BeforeEach
  void beforeEach() throws JwkException {
    when(jwkProviderService.get(anyString())).thenReturn(jwk);
  }
  
  @Nested
  class Decode {
    @Test
    void missing_bearer_keyword() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode("foo", Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().contains("out of bounds"));
    }
    @Test
    void missing_token() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode(null, Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().contains("Missing bearer token in header"));
    }
    @Test
    void malformed_token() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode("Bearer a.b.c", Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().contains("malformed token"));
    }
  }
}