package bio.ferlab.clin.auth.data;

import bio.ferlab.clin.utils.AuthResources;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

public class UserPermissionsBuilderTest {
    private final UserPermissionsBuilder builder = new UserPermissionsBuilder();


    private void validatePermissions(UserPermissions userPermissions,
                                     Function<Permission<? extends Resource>, Boolean> func,
                                     int count) {
        Assertions.assertEquals(Arrays.stream(userPermissions.getPermissions()).filter(func::apply).count(), count);
    }

    @Nested
    @DisplayName("Patient Permissions")
    class Patients {

        public static final int FHIR_RESOURCES_PER_PERMISSION = 2;

        @Test
        @DisplayName("Read")
        void permissionReadPatientList() {
            final var userPermissions = builder
                    .allowResource(AuthResources.PATIENT_LIST)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == Patient.class || permission.resourceType == Group.class).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.create, 0);
            validatePermissions(userPermissions, permission -> permission.update, 0);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }


        @Test
        @DisplayName("Create")
        void permissionCreatePatient() {
            final var userPermissions = builder
                    .allowResource(AuthResources.CREATE_PATIENT)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == Patient.class || permission.resourceType == Group.class).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, 0);
            validatePermissions(userPermissions, permission -> permission.create, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.update, 0);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }

        @Test
        @DisplayName("Update and Delete")
        void permissionUpdateAndDeletePatient() {
            final var userPermissions = builder
                    .allowResource(AuthResources.UPDATE_PATIENT)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == Patient.class || permission.resourceType == Group.class).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, 0);
            validatePermissions(userPermissions, permission -> permission.create, 0);
            validatePermissions(userPermissions, permission -> permission.update, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.delete, FHIR_RESOURCES_PER_PERMISSION);
        }
    }

    @Nested
    @DisplayName("Prescription Permissions")
    class Prescriptions {

        public static final int FHIR_RESOURCES_PER_PERMISSION = 4;

        @Test
        @DisplayName("Read")
        void permissionReadPrescriptionList() {
            final var userPermissions = builder
                    .allowResource(AuthResources.PATIENT_PRESCRIPTIONS)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(
                    FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == ServiceRequest.class ||
                                    permission.resourceType == ClinicalImpression.class ||
                                    permission.resourceType == Observation.class ||
                                    permission.resourceType == FamilyMemberHistory.class
                    ).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.create, 0);
            validatePermissions(userPermissions, permission -> permission.update, 0);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }


        @Test
        @DisplayName("Create")
        void permissionCreatePrescription() {
            final var userPermissions = builder
                    .allowResource(AuthResources.CREATE_PRESCRIPTION)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(
                    FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == ServiceRequest.class ||
                                    permission.resourceType == ClinicalImpression.class ||
                                    permission.resourceType == Observation.class ||
                                    permission.resourceType == FamilyMemberHistory.class
                    ).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, 0);
            validatePermissions(userPermissions, permission -> permission.create, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.update, 0);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }

        @Test
        @DisplayName("Update and Delete")
        void permissionUpdateAndDeletePrescription() {
            final var userPermissions = builder
                    .allowResource(AuthResources.UPDATE_PRESCRIPTION)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(
                    FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == ServiceRequest.class ||
                                    permission.resourceType == ClinicalImpression.class ||
                                    permission.resourceType == Observation.class ||
                                    permission.resourceType == FamilyMemberHistory.class
                    ).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, 0);
            validatePermissions(userPermissions, permission -> permission.create, 0);
            validatePermissions(userPermissions, permission -> permission.update, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }
    }

    @Nested
    @DisplayName("Practitioner/PractitionerRole Permissions")
    class Practitioners {

        public static final int FHIR_RESOURCES_PER_PERMISSION = 2;

        @Test
        @DisplayName("Read")
        void permissionReadPrescriptionList() {
            final var userPermissions = builder
                    .allowResource(AuthResources.READ_PRACTITIONER)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(
                    FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == Practitioner.class ||
                                    permission.resourceType == PractitionerRole.class
                    ).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.create, 0);
            validatePermissions(userPermissions, permission -> permission.update, 0);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }
    }

    @Nested
    @DisplayName("Family Group Permissions")
    class FamilyGroup {

        public static final int FHIR_RESOURCES_PER_PERMISSION = 1;

        @Test
        @DisplayName("Read")
        void permissionReadPrescriptionList() {
            final var userPermissions = builder
                    .allowResource(AuthResources.PATIENT_FAMILY)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(
                    FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == Group.class
                    ).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.create, 0);
            validatePermissions(userPermissions, permission -> permission.update, 0);
            validatePermissions(userPermissions, permission -> permission.delete, 0);
        }

        @Test
        @DisplayName("Create, Update and Delete")
        void permissionCreatePrescription() {
            final var userPermissions = builder
                    .allowResource(AuthResources.UPDATE_FAMILY)
                    .build();
            Assertions.assertEquals(FHIR_RESOURCES_PER_PERMISSION, userPermissions.getPermissions().length);
            Assertions.assertEquals(
                    FHIR_RESOURCES_PER_PERMISSION,
                    Arrays.stream(userPermissions.getPermissions()).filter(permission ->
                            permission.resourceType == Group.class
                    ).count()
            );
            validatePermissions(userPermissions, permission -> permission.read, 0);
            validatePermissions(userPermissions, permission -> permission.create, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.update, FHIR_RESOURCES_PER_PERMISSION);
            validatePermissions(userPermissions, permission -> permission.delete, FHIR_RESOURCES_PER_PERMISSION);
        }
    }
}
