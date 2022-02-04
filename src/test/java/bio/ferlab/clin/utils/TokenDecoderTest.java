package bio.ferlab.clin.utils;

import bio.ferlab.clin.auth.JwkProviderService;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TokenDecoderTest {
  
  final String mockTokenJohnDoe = "Bearer " + JWT.create().withClaim("name", "John Doe")
      .sign(Algorithm.HMAC256("secret"));
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
    void missing_token() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode(null, Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().equals("Missing bearer token in header"));
    }
    @Test
    void token_another_provider() throws JwkException {
      when(jwkProviderService.get(any())).thenThrow(new JwkException(""));
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode(mockTokenJohnDoe, Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().equals("token from another provider"));
    }
    @Test
    void malformed_token() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode("Bearer a.b.c", Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().equals("malformed token"));
    }
    @Test
    void internal_error() throws JwkException {
      when(jwkProviderService.get(any())).thenThrow(new RuntimeException("internal_error"));
      Exception ex = Assertions.assertThrows(
          RuntimeException.class,
          () -> decoder.decode(mockTokenJohnDoe, Locale.ENGLISH)
      );
      assertTrue(ex.getMessage().equals("internal_error"));
    }
  }
}