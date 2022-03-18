package bio.ferlab.clin.auth;

import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KeycloakClient {
    private static final Logger log = LoggerFactory.getLogger(KeycloakClient.class);
    public static final String AUTH_CLIENT_SECRET_KEY = "secret";
    private final AuthzClient authzClient;
    private final BioProperties bioProperties;

    public KeycloakClient(BioProperties bioProperties) {
        final Configuration configuration = new Configuration();
        configuration.setRealm(bioProperties.getAuthRealm());
        configuration.setAuthServerUrl(bioProperties.getAuthServerUrl());
        configuration.setResource(bioProperties.getAuthClientId());
        configuration.getCredentials().put(AUTH_CLIENT_SECRET_KEY, bioProperties.getAuthClientSecret());
        this.authzClient = AuthzClient.create(configuration);
        this.bioProperties = bioProperties;
    }

    public TokenIntrospectionResponse introspectRpt(String rpt) {
        return this.retryableIntrospectRpt(rpt, bioProperties.getAuthRetry());
    }
    
    private TokenIntrospectionResponse retryableIntrospectRpt(String rpt, int retry) {
        try {
            return this.authzClient.protection().introspectRequestingPartyToken(rpt);
        } catch (Exception e) {
            log.error("Failed to introspect token: {}", e.getMessage());
            if (retry > 0) {
                log.info("Retry to introspect token: {}", retry);
                return retryableIntrospectRpt(rpt, retry - 1);
            } else {
                throw e;
            }
        }
    }
}
