package bio.ferlab.clin.utils;

import bio.ferlab.clin.auth.JwkProviderService;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class TokenDecoderTest {
  
  final BioProperties mockBioProperties = Mockito.mock(BioProperties.class);
  final String mockTokenJohnDoe = "Bearer " + JWT.create().withClaim("name", "John Doe")
      .sign(Algorithm.HMAC256("secret"));
  final JwkProviderService jwkProviderService = Mockito.mock(JwkProviderService.class);
  final Jwk jwk = Mockito.mock(Jwk.class);
  final PublicKey publicKey = Mockito.mock(PublicKey.class);
  final TokenDecoder decoder = new TokenDecoder(jwkProviderService, mockBioProperties);
  

  @BeforeEach
  void beforeEach() throws JwkException {
    when(mockBioProperties.getAuthLeeway()).thenReturn(0L);
    when(jwkProviderService.get(anyString())).thenReturn(jwk);
    when(jwk.getPublicKey()).thenReturn(publicKey);
  }
  
  @Nested
  class Decode {
    @Test
    void missing_token() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode(null, Locale.ENGLISH)
      );
      assertEquals("Missing bearer token in header", ex.getMessage());
    }
    @Test
    void token_another_provider() throws JwkException {
      when(jwkProviderService.get(any())).thenThrow(new JwkException(""));
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode(mockTokenJohnDoe, Locale.ENGLISH)
      );
      assertEquals("invalid token", ex.getMessage());
    }
    @Test
    void malformed_token() {
      Exception ex = Assertions.assertThrows(
          RptIntrospectionException.class,
          () -> decoder.decode("Bearer a.b.c", Locale.ENGLISH)
      );
      assertEquals("invalid token", ex.getMessage());
    }
    @Test
    void internal_error() throws JwkException {
      when(jwkProviderService.get(any())).thenThrow(new RuntimeException("internal_error"));
      Exception ex = Assertions.assertThrows(
          RuntimeException.class,
          () -> decoder.decode(mockTokenJohnDoe, Locale.ENGLISH)
      );
      assertEquals("internal_error", ex.getMessage());
    }
    @Test
    void leeway_test() {
      final Date iat = new Date(Instant.now().plusSeconds(5).toEpochMilli());
      final String token = JWT.create().withIssuedAt(iat).withClaim("name", "John Doe").sign(Algorithm.HMAC256("secret"));
      Exception ex = Assertions.assertThrows(
          JWTVerificationException.class,
          () ->  JWT.require(Algorithm.HMAC256("secret")).build().verify(token) // failed because token iat is 5s in the future
      );
      assertTrue(ex.getMessage().startsWith("The Token can't be used before"));
      JWT.require(Algorithm.HMAC256("secret")).acceptLeeway(5).build().verify(token); // success, we allow 5s 
    }
  }
}