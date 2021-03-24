package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.context.ServiceContext;
import bio.ferlab.clin.utils.TokenDecoder;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

@Interceptor
@Service
public class AccessTokenInterceptor {
    public static final String FORBIDDEN = "Forbidden";
    private final Logger logger = LoggerFactory.getLogger(AccessTokenInterceptor.class);
    private final TokenDecoder decoder;

    public AccessTokenInterceptor(TokenDecoder decoder) {
        this.decoder = decoder;
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public void validateToken(HttpServletRequest request, HttpServletResponse response) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = null;
        if (StringUtils.isNotBlank(bearer)) {
            //The access token is always preceeded by "Bearer "
            accessToken = bearer.split(" ")[1];
        }
        if (accessToken == null) {
            logger.info("No access token provided in header");
            throw new ca.uhn.fhir.rest.server.exceptions.AuthenticationException(FORBIDDEN);
        }
        try {
            DecodedJWT decodedJWT = JWT.decode(accessToken);

            //Save user info in ThreadLocal for later use
            ServiceContext.build(decodedJWT.getSubject(), request.getLocale());
            //TODO: Add the roles so we can eventually use them to build a list of IAuthRule => https://hapifhir.io/hapi-fhir/docs/security/authorization_interceptor.html

            //Validate access token based on public key
            Jwk jwk = decoder.getProvider().get(decodedJWT.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            Verification verifier = JWT.require(algorithm);

            //Will throw exception if invalid
            verifier.build().verify(decodedJWT);
        } catch (Exception e) {
            logger.warn("Exception during authentication", e);
            throw new ca.uhn.fhir.rest.server.exceptions.AuthenticationException(FORBIDDEN, e);
        }
    }

    /**
     * DO NOT USE IN PRODUCTION.  Local development environment ONLY.
     *
     * @return All-trusting SSLContext
     */
    private static SSLContext getDisabledSSLContext() {
        //Disable SSL Validation during local development with self signed certificates.
        SSLContext sc = null;
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        return sc;
    }
}
