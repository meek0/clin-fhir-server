package bio.ferlab.clin.auth;

import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.springframework.stereotype.Component;

@Component
public class KeycloakClient {
    public static final String AUTH_CLIENT_SECRET_KEY = "secret";
    private final AuthzClient authzClient;

    public KeycloakClient(BioProperties bioProperties) {
        final Configuration configuration = new Configuration();
        configuration.setRealm(bioProperties.getAuthRealm());
        configuration.setAuthServerUrl(bioProperties.getAuthServerUrl());
        configuration.setResource(bioProperties.getAuthClientId());
        configuration.getCredentials().put(AUTH_CLIENT_SECRET_KEY, bioProperties.getAuthClientSecret());
        this.authzClient = AuthzClient.create(configuration);
    }

    public TokenIntrospectionResponse introspectRpt(String rpt) {
        return this.authzClient.protection().introspectRequestingPartyToken(rpt);
    }
}
