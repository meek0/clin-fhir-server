package bio.ferlab.clin.auth;

import bio.ferlab.clin.auth.data.UserPermissions;
import bio.ferlab.clin.auth.data.UserPermissionsBuilder;
import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.utils.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.springframework.stereotype.Component;

@Component
public class RPTPermissionExtractor {
    private final KeycloakClient client;

    public RPTPermissionExtractor(KeycloakClient client) {
        this.client = client;
    }

    public UserPermissions extract(RequestDetails requestDetails) {
        final var rpt = requestDetails.getHeader(Constants.RPT_HEADER);
        final var response = this.client.introspectRpt(rpt);

        if (!response.getActive()) {
            throw new RptIntrospectionException(rpt);
        }

        final var builder = new UserPermissionsBuilder();
        response.getPermissions().forEach(permission -> builder.allowResource(permission.getResourceName()));
        return builder.build();
    }
}
