package bio.ferlab.clin.audit;

import bio.ferlab.clin.user.UserData;
import lombok.Data;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAction;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventAgentComponent;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AuditEventsBuilder {
    private final List<AuditResource> resources = new ArrayList<>();
    private final UserData userData;

    public AuditEventsBuilder(UserData userData) {
        this.userData = userData;
    }

    private AuditEventAction getActionFromBundleVerb(Bundle.HTTPVerb verb) {
        switch (verb) {
            case GET:
            case NULL:
            case HEAD:
                return AuditEventAction.R;
            case POST:
                return AuditEventAction.C;
            case PUT:
            case PATCH:
                return AuditEventAction.U;
            case DELETE:
                return AuditEventAction.D;

        }
        throw new IllegalStateException("HTTP Verb is unknown");
    }

    public AuditEventsBuilder addBundle(Bundle bundle) {
        bundle.getEntry().forEach(entry -> {
            final AuditEventAction action = this.getActionFromBundleVerb(entry.getRequest().getMethod());
            if (action == AuditEventAction.R) {
                final String resourceType = entry.getResource().fhirType();
                this.resources.add(AuditResource.fromReadAction(entry.getRequest().getUrl(), resourceType));
            } else {
                this.resources.add(new AuditResource(entry.getResource(), action));
            }
        });
        return this;
    }

    public AuditEventsBuilder addResource(Resource resource, AuditEventAction action) {
        if (action == AuditEventAction.R) {
            this.resources.add(AuditResource.fromReadAction(resource.getId(), resource.fhirType()));
        } else {
            this.resources.add(new AuditResource(resource, action));
        }

        return this;
    }

    public AuditEventsBuilder addReadAction(String resource) {
        this.resources.add(AuditResource.fromReadAllAction(resource));
        return this;
    }

    private AuditEvent generateAuditEvent(AuditResource auditResource) {

        final AuditEvent event = new AuditEvent();
        event.setRecorded(Date.from(Instant.now()));

        final AuditEventAgentComponent agent = new AuditEventAgentComponent();
        agent.setName(userData.getName());
        agent.setWho(new Reference().setReference(String.format("Practitioner/%s", userData.getPractitionerId())));
        event.addAgent(agent);

        final AuditEvent.AuditEventEntityComponent target = new AuditEvent.AuditEventEntityComponent();

        target.setType(auditResource.getType());
        if (!auditResource.isReadAll()) {
            target.setWhat(auditResource.getReference());
        }

        event.addEntity(target);
        event.setAction(auditResource.getAction());

        return event;
    }

    public List<AuditEvent> build() {
        return this.resources.stream().map(this::generateAuditEvent).collect(Collectors.toList());
    }

    @Data
    static class AuditResource {
        private final Resource resource;
        private final AuditEventAction action;
        private Reference reference;
        private Coding type;

        public AuditResource(Resource resource, AuditEventAction action) {
            this.resource = resource;
            this.action = action;
            if (resource != null) {
                this.reference = new Reference().setReference(resource.getId());
                this.type = createTypeCoding().setCode(resource.getResourceType().toString());
            }

        }

        public static AuditResource fromReadAction(String url, String type) {
            final AuditResource auditResource = new AuditResource(null, AuditEventAction.R);
            auditResource.setReference(new Reference(url));
            auditResource.setType(createTypeCoding().setCode(type));
            return auditResource;
        }


        public static AuditResource fromReadAllAction(String type) {
            final AuditResource auditResource = new AuditResource(null, AuditEventAction.R);
            auditResource.setType(createTypeCoding().setCode(type));
            return auditResource;
        }

        private static Coding createTypeCoding() {
            return new Coding().setSystem("https://www.hl7.org/fhir/valueset-audit-entity-type.html");
        }

        public boolean isReadAll() {
            return this.action == AuditEventAction.R && this.reference == null;
        }
    }
}
