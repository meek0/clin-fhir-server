package bio.ferlab.clin.auth;

import bio.ferlab.clin.auth.data.UserPermissions;
import bio.ferlab.clin.auth.data.UserPermissionsBuilder;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.utils.Helpers;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;


@Component
public class RPTPermissionExtractor {
    private final KeycloakClient client;
    private static final String BEARER_TOKEN_ERROR_MSG = "Failed to extract user perms from token: %s";
    public RPTPermissionExtractor(KeycloakClient client) {
        this.client = client;
    }

    public UserPermissions extract(RequestDetails requestDetails) {
        try {
            final var bearer = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
            final var rpt = Helpers.extractAccessTokenFromBearer(bearer);
            final var response = this.client.introspectRpt(rpt);
            
            if (Optional.ofNullable(response.getPermissions()).isEmpty()) {
                throw new AuthenticationException("RPT token is required");
            }

            if (!response.isActive()) {
                throw new AuthenticationException("not-active");
            }

            if (response.isExpired()) {
                throw new AuthenticationException("expired");
            }

            final var builder = new UserPermissionsBuilder();
            Optional.ofNullable(response.getPermissions())
                    .orElse(Collections.emptyList())
                    .forEach(permission -> builder.allowResource(permission.getResourceName(), permission.getScopes()));
            return builder.build();
        } catch (Exception e) {
            throw new RptIntrospectionException(String.format(BEARER_TOKEN_ERROR_MSG, e.getMessage()));
        }
    }
}
