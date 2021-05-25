package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.utils.Constants;
import bio.ferlab.clin.utils.TokenDecoder;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
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

@Interceptor
@Service
public class AccessTokenInterceptor {
    public static final String FORBIDDEN = "Forbidden";
    private final Logger logger = LoggerFactory.getLogger(AccessTokenInterceptor.class);
    private final TokenDecoder decoder;

    public AccessTokenInterceptor(BioProperties bioProperties, TokenDecoder decoder) {
        if (bioProperties.isDisableSslValidation()) {
            getDisabledSSLContext();
        }
        this.decoder = decoder;
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public void validateToken(HttpServletRequest request, HttpServletResponse response) {
        final var bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        final var requesterData = decoder.decode(bearer, request.getLocale());
        request.setAttribute(Constants.REQUESTER_DATA_KEY, requesterData);
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
