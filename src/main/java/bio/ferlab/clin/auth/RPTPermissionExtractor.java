package bio.ferlab.clin.auth;

import bio.ferlab.clin.auth.data.UserPermissions;
import bio.ferlab.clin.auth.data.UserPermissionsBuilder;
import bio.ferlab.clin.utils.Helpers;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.auth0.jwt.JWT;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.authorization.Permission;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class RPTPermissionExtractor {
    //private final KeycloakClient client;

    @Data
    private static class Authorization {
        private List<Permission> permissions;
    }

    public UserPermissions extract(RequestDetails requestDetails) {
        final var bearer = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
        final var rpt = Helpers.extractAccessTokenFromBearer(bearer);

        var jwt = JWT.decode(rpt);
        final var claim = jwt.getClaim("authorization");
        final var authorization = claim.as(Authorization.class);
    
        final var builder = new UserPermissionsBuilder();
        authorization.permissions.forEach(permission -> builder.allowResource(permission.getResourceName(), permission.getScopes()));
        return builder.build();
    }
}
