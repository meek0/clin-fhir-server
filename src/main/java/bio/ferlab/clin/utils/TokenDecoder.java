package bio.ferlab.clin.utils;

import bio.ferlab.clin.BioProperties;
import bio.ferlab.clin.context.ServiceContext;
import bio.ferlab.clin.user.UserData;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
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
    private static final Logger logger = LoggerFactory.getLogger(TokenDecoder.class);
    private static final String FORBIDDEN = "FORBIDDEN";
    private final JwkProvider provider;

    public TokenDecoder(BioProperties bioProperties) throws MalformedURLException {
        final String url = StringUtils.appendIfMissing(bioProperties.getAuthServerUrl(), "/") + "auth/realms/" + bioProperties.getAuthRealm()
                + "/protocol/openid-connect/certs";
        this.provider = new JwkProviderBuilder(new URL(url)).build();
    }

    public JwkProvider getProvider() {
        return provider;
    }

    public UserData decode(String bearer, Locale locale) throws AuthenticationException {
        String accessToken = null;
        if (StringUtils.isNotBlank(bearer)) {
            accessToken = bearer.split(" ")[1];
        }
        if (accessToken == null) {
            logger.info("No access token provided in header");
            throw new AuthenticationException(FORBIDDEN);
        }
        try {
            final DecodedJWT decodedJWT = JWT.decode(accessToken);
            ServiceContext.build(decodedJWT.getSubject(), locale);

            final Jwk jwk = provider.get(decodedJWT.getKeyId());
            final Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            final Verification verifier = JWT.require(algorithm);

            verifier.build().verify(decodedJWT);
            final String decodedBody = new String(new Base64(true).decode(decodedJWT.getPayload()));
            return new ObjectMapper().readValue(decodedBody, UserData.class);
        } catch (Exception e) {
            logger.warn("Exception during authentication", e);
            throw new AuthenticationException(FORBIDDEN, e);
        }
    }
}
