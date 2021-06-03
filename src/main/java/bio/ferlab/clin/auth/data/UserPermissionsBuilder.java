package bio.ferlab.clin.auth.data;

import org.hl7.fhir.r4.model.BaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserPermissionsBuilder {
    private static final Logger log = LoggerFactory.getLogger(UserPermissionsBuilder.class);
    public static final String RESOURCES_MODULE = "org.hl7.fhir.r4.model";
    public static final String READ_SCOPE = "read";
    public static final String UPDATE_SCOPE = "update";
    public static final String CREATE_SCOPE = "create";
    public static final String DELETE_SCOPE = "delete";

    private final List<Permission<? extends BaseResource>> permissions = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private Permission<? extends BaseResource> permissionFromResourceName(String resource, Set<String> scopes) {
        final var completeName = String.format("%s.%s", RESOURCES_MODULE, resource);
        try {
            final var resourceClass = (Class<? extends BaseResource>) Class.forName(completeName);
            return new Permission<>(
                    resourceClass,
                    scopes.contains(CREATE_SCOPE),
                    scopes.contains(READ_SCOPE),
                    scopes.contains(UPDATE_SCOPE),
                    scopes.contains(DELETE_SCOPE)
            );
        } catch (ClassNotFoundException e) {
            log.warn("[{}] is not a valid FHIR resource, permission ignored.", completeName);
            return null;
        }
    }

    public UserPermissionsBuilder allowResource(String resource, Set<String> scopes) {
        final var permission = permissionFromResourceName(resource, scopes);
        if (permission != null) {
            this.permissions.add(permission);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public UserPermissions build() {
        return new UserPermissions(this.permissions.toArray(Permission[]::new));
    }
}
