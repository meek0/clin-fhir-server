package bio.ferlab.clin.auth.data;

import bio.ferlab.clin.utils.AuthResources;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class UserPermissionsBuilder {
    private static final Logger log = LoggerFactory.getLogger(UserPermissionsBuilder.class);

    private enum Scope {
        READ,
        CREATE,
        UPDATE,
        DELETE,
    }

    private enum AuthPermission {
        PATIENT(Patient.class),
        SERVICE_REQUEST(ServiceRequest.class),
        CLINICAL_IMPRESSION(ClinicalImpression.class),
        OBSERVATION(Observation.class),
        FAMILY_HISTORY(FamilyMemberHistory.class),
        GROUP(Group.class),
        PRACTITIONER(Practitioner.class),
        PRACTITIONER_ROLE(PractitionerRole.class),
        ORGANIZATION(Organization.class),
        BUNDLE(Bundle.class),
        SPECIMEN(Specimen.class),
        DOCUMENT_REFERENCE(DocumentReference.class),
        TASK(Task.class);

        public final Class<? extends Resource> type;
        private String id;

        AuthPermission(Class<? extends Resource> type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    private final Map<AuthPermission, Set<Scope>> permissions = new HashMap<>();

    private final List<Pair<AuthPermission, Scope>> defaultRules = List.of(
            Pair.of(AuthPermission.BUNDLE, Scope.READ),
            Pair.of(AuthPermission.SPECIMEN, Scope.READ),
            Pair.of(AuthPermission.SPECIMEN, Scope.UPDATE),
            Pair.of(AuthPermission.SPECIMEN, Scope.CREATE),
            Pair.of(AuthPermission.SPECIMEN, Scope.DELETE),
            Pair.of(AuthPermission.DOCUMENT_REFERENCE, Scope.READ),
            Pair.of(AuthPermission.DOCUMENT_REFERENCE, Scope.UPDATE),
            Pair.of(AuthPermission.DOCUMENT_REFERENCE, Scope.CREATE),
            Pair.of(AuthPermission.DOCUMENT_REFERENCE, Scope.DELETE),
            Pair.of(AuthPermission.TASK, Scope.READ),
            Pair.of(AuthPermission.TASK, Scope.UPDATE),
            Pair.of(AuthPermission.TASK, Scope.CREATE),
            Pair.of(AuthPermission.TASK, Scope.DELETE)
    );

    private final Map<String, List<Pair<AuthPermission, Scope>>> permissionRules = Map.ofEntries(
            Map.entry(AuthResources.PATIENT_LIST, List.of(
                    Pair.of(AuthPermission.PATIENT, Scope.READ),
                    Pair.of(AuthPermission.GROUP, Scope.READ),
                    Pair.of(AuthPermission.ORGANIZATION, Scope.READ)
            )),
            Map.entry(AuthResources.CREATE_PATIENT, List.of(
                    Pair.of(AuthPermission.PATIENT, Scope.CREATE),
                    Pair.of(AuthPermission.GROUP, Scope.CREATE)
            )),
            Map.entry(AuthResources.UPDATE_PATIENT, List.of(
                    Pair.of(AuthPermission.PATIENT, Scope.UPDATE),
                    Pair.of(AuthPermission.PATIENT, Scope.DELETE),
                    Pair.of(AuthPermission.GROUP, Scope.UPDATE),
                    Pair.of(AuthPermission.GROUP, Scope.DELETE)
            )),
            Map.entry(AuthResources.PATIENT_PRESCRIPTIONS, List.of(
                    Pair.of(AuthPermission.SERVICE_REQUEST, Scope.READ),
                    Pair.of(AuthPermission.CLINICAL_IMPRESSION, Scope.READ),
                    Pair.of(AuthPermission.OBSERVATION, Scope.READ),
                    Pair.of(AuthPermission.FAMILY_HISTORY, Scope.READ)
            )),
            Map.entry(AuthResources.CREATE_PRESCRIPTION, List.of(
                    Pair.of(AuthPermission.SERVICE_REQUEST, Scope.CREATE),
                    Pair.of(AuthPermission.CLINICAL_IMPRESSION, Scope.CREATE),
                    Pair.of(AuthPermission.OBSERVATION, Scope.CREATE),
                    Pair.of(AuthPermission.FAMILY_HISTORY, Scope.CREATE)
            )),
            Map.entry(AuthResources.UPDATE_PRESCRIPTION, List.of(
                    Pair.of(AuthPermission.SERVICE_REQUEST, Scope.UPDATE),
                    Pair.of(AuthPermission.CLINICAL_IMPRESSION, Scope.UPDATE),
                    Pair.of(AuthPermission.OBSERVATION, Scope.UPDATE),
                    Pair.of(AuthPermission.FAMILY_HISTORY, Scope.UPDATE)
            )),
            Map.entry(AuthResources.READ_PRACTITIONER, List.of(
                    Pair.of(AuthPermission.PRACTITIONER, Scope.READ),
                    Pair.of(AuthPermission.PRACTITIONER_ROLE, Scope.READ)
            )),
            Map.entry(AuthResources.PATIENT_FAMILY, List.of(
                    Pair.of(AuthPermission.GROUP, Scope.READ)
            )),
            Map.entry(AuthResources.UPDATE_FAMILY, List.of(
                    Pair.of(AuthPermission.GROUP, Scope.CREATE),
                    Pair.of(AuthPermission.GROUP, Scope.UPDATE),
                    Pair.of(AuthPermission.GROUP, Scope.DELETE)
            ))
    );

    public UserPermissionsBuilder allowResource(String resource) {
        if (this.permissionRules.containsKey(resource)) {
            this.permissionRules.get(resource).forEach(entry -> this.allowPermission(entry.getFirst(), entry.getSecond()));
        } else {
            log.warn("Permission [{}] not supported by the server and is ignored.", resource);
        }
        return this;
    }


    public UserPermissionsBuilder allowPermission(AuthPermission permission, Scope scope) {
        this.permissions.putIfAbsent(permission, new HashSet<>());
        this.permissions.get(permission).add(scope);
        return this;
    }

    private Permission<? extends Resource> permissionsFromScopes(Map.Entry<AuthPermission, Set<Scope>> entry) {
        final var scopes = entry.getValue();
        return new Permission<>(
                entry.getKey().type,
                scopes.contains(Scope.READ),
                scopes.contains(Scope.UPDATE),
                scopes.contains(Scope.CREATE),
                scopes.contains(Scope.DELETE)
        );
    }

    private void addDefaultPermissions() {
        this.defaultRules.forEach(entry -> this.allowPermission(entry.getFirst(), entry.getSecond()));
    }

    @SuppressWarnings("unchecked")
    public UserPermissions build() {
        this.addDefaultPermissions();
        final var resources = this.permissions.entrySet().stream().map(this::permissionsFromScopes).collect(Collectors.toList());
        return new UserPermissions(resources.toArray(Permission[]::new));
    }
}
