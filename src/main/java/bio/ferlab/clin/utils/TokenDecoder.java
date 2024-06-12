package bio.ferlab.clin.utils;

import bio.ferlab.clin.auth.JwkProviderService;
import bio.ferlab.clin.context.ServiceContext;
import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.user.RequesterData;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Locale;

@Component
public class TokenDecoder {
    private static final Logger log = LoggerFactory.getLogger(TokenDecoder.class);
    
    private final JwkProviderService provider;
    private final BioProperties bioProperties;
    private final String issuer;

    public TokenDecoder(JwkProviderService jwkProviderService, BioProperties bioProperties) {   
        this.provider = jwkProviderService;
        this.bioProperties = bioProperties;
        this.issuer = StringUtils.appendIfMissing(bioProperties.getAuthServerUrl(), "/") + "realms/" + bioProperties.getAuthRealm();
    }

    public RequesterData decode(String authorization, Locale locale) {
        try {
            final var accessToken = Helpers.extractAccessTokenFromBearer(authorization);
            final DecodedJWT decodedJWT = JWT.decode(accessToken);
            ServiceContext.build(decodedJWT.getSubject(), locale);

            final Jwk jwk = provider.get(decodedJWT.getKeyId());
            final Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            final Verification verifier = JWT.require(algorithm);

            verifier.acceptLeeway(bioProperties.getAuthLeeway()).build().verify(decodedJWT);
            verifier.withAudience(bioProperties.getAuthClientId());
            verifier.withIssuer(issuer);
            final String decodedBody = new String(new Base64(true).decode(decodedJWT.getPayload()));
            return new ObjectMapper().readValue(decodedBody, RequesterData.class);
        } catch (JwkException | JWTDecodeException | JsonProcessingException e) {
            log.warn("Invalid token: {}", e.getMessage()); // hide from the user + log the reason
            throw new RptIntrospectionException("invalid token");
        } catch (JWTVerificationException e) {
            throw new RptIntrospectionException(e.getMessage());
        }
    }
}
