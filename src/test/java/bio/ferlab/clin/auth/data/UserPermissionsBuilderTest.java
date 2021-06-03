package bio.ferlab.clin.auth.data;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserPermissionsBuilderTest {
    private final UserPermissionsBuilder builder = new UserPermissionsBuilder();
    private final Set<String> allScopes = new HashSet<>();

    @BeforeEach
    void setup() {
        allScopes.clear();
        allScopes.addAll(List.of("create", "read", "update", "delete"));
    }

    public void validateScopes(Permission<? extends BaseResource> permission, Set<String> scopes) {
        scopes.forEach(scope -> {
            switch (scope) {
                case "create":
                    Assertions.assertTrue(permission.isCreate());
                    break;
                case "read":
                    Assertions.assertTrue(permission.isRead());
                    break;
                case "update":
                    Assertions.assertTrue(permission.isUpdate());
                    break;
                case "delete":
                    Assertions.assertTrue(permission.isDelete());
                    break;
            }
        });
        allScopes.removeAll(scopes);
        allScopes.forEach(scope -> {
            switch (scope) {
                case "create":
                    Assertions.assertFalse(permission.isCreate());
                    break;
                case "read":
                    Assertions.assertFalse(permission.isRead());
                    break;
                case "update":
                    Assertions.assertFalse(permission.isUpdate());
                    break;
                case "delete":
                    Assertions.assertFalse(permission.isDelete());
                    break;
            }
        });
    }

    private void validatePermission(Class<? extends BaseResource> resource, Set<String> scopes) {
        final var userPermissions = builder
                .allowResource(resource.getSimpleName(), scopes)
                .build();
        Assertions.assertEquals(1, userPermissions.getPermissions().length);
        final var permission = userPermissions.getPermissions()[0];
        Assertions.assertSame(permission.resourceType, resource);
        validateScopes(permission, scopes);
    }

    @Nested
    @DisplayName("Patient Permissions")
    class Patients {
        @Test
        @DisplayName("Create")
        void permissionCreatePatient() {
            validatePermission(Patient.class, Set.of("create"));
        }

        @Test
        @DisplayName("Read")
        void permissionReadPatient() {
            validatePermission(Patient.class, Set.of("read"));
        }

        @Test
        @DisplayName("Update")
        void permissionUpdatePatient() {
            validatePermission(Patient.class, Set.of("update"));
        }

        @Test
        @DisplayName("Delete")
        void permissionDeletePatient() {
            validatePermission(Patient.class, Set.of("delete"));
        }

        @Test
        @DisplayName("No Permissions")
        void noPatientPermissions() {
            validatePermission(Patient.class, Set.of());
        }
    }

    @Nested
    @DisplayName("ServiceRequests Permissions")
    class ServiceRequests {
        @Test
        @DisplayName("Create")
        void permissionCreateServiceRequest() {
            validatePermission(ServiceRequest.class, Set.of("create"));
        }

        @Test
        @DisplayName("Read")
        void permissionReadServiceRequest() {
            validatePermission(ServiceRequest.class, Set.of("read"));
        }

        @Test
        @DisplayName("Update")
        void permissionUpdateServiceRequest() {
            validatePermission(ServiceRequest.class, Set.of("update"));
        }

        @Test
        @DisplayName("Delete")
        void permissionDeleteServiceRequest() {
            validatePermission(ServiceRequest.class, Set.of("delete"));
        }

        @Test
        @DisplayName("No Permissions")
        void noServiceRequestPermissions() {
            validatePermission(ServiceRequest.class, Set.of());
        }
    }


    @Nested
    @DisplayName("ServiceRequests Permissions")
    class ClinicalImpressions {
        @Test
        @DisplayName("Create")
        void permissionCreateClinicalImpression() {
            validatePermission(ClinicalImpression.class, Set.of("create"));
        }

        @Test
        @DisplayName("Read")
        void permissionReadClinicalImpression() {
            validatePermission(ClinicalImpression.class, Set.of("read"));
        }

        @Test
        @DisplayName("Update")
        void permissionUpdateClinicalImpression() {
            validatePermission(ClinicalImpression.class, Set.of("update"));
        }

        @Test
        @DisplayName("Delete")
        void permissionDeleteClinicalImpression() {
            validatePermission(ClinicalImpression.class, Set.of("delete"));
        }

        @Test
        @DisplayName("No Permissions")
        void noClinicalImpressionPermissions() {
            validatePermission(ClinicalImpression.class, Set.of());
        }
    }


    @Nested
    @DisplayName("ServiceRequests Permissions")
    class Groups {
        @Test
        @DisplayName("Create")
        void permissionCreateGroup() {
            validatePermission(Group.class, Set.of("create"));
        }

        @Test
        @DisplayName("Read")
        void permissionReadGroup() {
            validatePermission(Group.class, Set.of("read"));
        }

        @Test
        @DisplayName("Update")
        void permissionUpdateGroup() {
            validatePermission(Group.class, Set.of("update"));
        }

        @Test
        @DisplayName("Delete")
        void permissionDeleteGroup() {
            validatePermission(Group.class, Set.of("delete"));
        }

        @Test
        @DisplayName("No Permissions")
        void noGroupPermissions() {
            validatePermission(Group.class, Set.of());
        }
    }
}
