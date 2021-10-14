package bio.ferlab.clin.audits;

import bio.ferlab.clin.audit.AuditEventsBuilder;
import bio.ferlab.clin.user.RequesterData;
import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditEventsBuilderTest {
    private AuditEventsBuilder auditEventsBuilder;
    private RequesterData requesterData;

    @BeforeEach
    public void setup() {
        this.requesterData = new RequesterData();
        this.requesterData.setName("FirstName LastName");
    }

    @Nested
    @DisplayName("AuditEventsBuilder::build")
    class AuditEventCreation {
        @Test
        @DisplayName("From a bundle")
        public void createAuditEventFromBundle() {
            auditEventsBuilder = new AuditEventsBuilder(requesterData);
            final Bundle bundle = new Bundle();
            final Patient patient = new Patient();
            patient.setId("Patient/TestId");
            final String patientId = patient.getId();

            bundle.addEntry().setResource(patient).setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setMethod(Bundle.HTTPVerb.GET)
                            .setUrl(patientId)
            );
            final List<AuditEvent> events = auditEventsBuilder.addBundle(bundle).build();
            final Reference what = events.get(0).getEntity().get(0).getWhat();

            assertEquals(events.size(), 1);
            assertTrue(ResourceType.Patient.toString().contentEquals(events.get(0).getEntity().get(0).getType().getCode()));
        }

        @Test
        @DisplayName("From a specific resource")
        public void createAuditEventFromResource() {
            auditEventsBuilder = new AuditEventsBuilder(requesterData);
            final Patient patient = new Patient();
            patient.setIdElement(new IdType().setValue("Patient/TestId"));
            final String patientId = patient.getId();

            final List<AuditEvent> events = auditEventsBuilder.addResource(patient, AuditEvent.AuditEventAction.R).build();
            final Reference what = events.get(0).getEntity().get(0).getWhat();

            assertEquals(events.size(), 1);
            assertSame(ResourceType.Patient.toString(), events.get(0).getEntity().get(0).getType().getCode());
        }

        @Test
        @DisplayName("From all resources")
        public void createAuditEventFromReadAction() {
            auditEventsBuilder = new AuditEventsBuilder(requesterData);

            final List<AuditEvent> events = auditEventsBuilder.addReadAction("Patient").build();
            final String type = events.get(0).getEntity().get(0).getType().getCode();

            assertEquals(events.size(), 1);
            assertSame(ResourceType.Patient.toString(), type);
        }

        @Test
        @DisplayName("From Id and Action type")
        public void deleteAuditEventFromIdAndActonType() {
            auditEventsBuilder = new AuditEventsBuilder(requesterData);
            final IIdType idType = new IdDt("Task/001");
            final List<AuditEvent> events = auditEventsBuilder.addByIdAndActionType(idType, AuditEvent.AuditEventAction.D).build();
            assertEquals(events.size(), 1);
            final AuditEvent.AuditEventEntityComponent entity = events.get(0).getEntity().get(0);
            assertEquals(idType.getValue(), entity.getDescription());
            assertEquals(ResourceType.Task.toString(), entity.getType().getCode());
        }
    }

    @Nested
    @DisplayName("AuditEventsBuilder CRUD")
    class AuditEventCRUD {
        @Nested
        @DisplayName("From a resource")
        class Resource {
            @Test
            @DisplayName("Read")
            public void createReadAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Patient patient = new Patient();
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addResource(patient, AuditEvent.AuditEventAction.R).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.R);
            }

            @Test
            @DisplayName("Create")
            public void createWriteAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Patient patient = new Patient();
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addResource(patient, AuditEvent.AuditEventAction.C).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.C);

            }

            @Test
            @DisplayName("Update")
            public void createUpdateAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Patient patient = new Patient();
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addResource(patient, AuditEvent.AuditEventAction.U).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.U);
            }

            @Test
            @DisplayName("Delete")
            public void createDeleteAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Patient patient = new Patient();
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addResource(patient, AuditEvent.AuditEventAction.D).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.D);
            }
        }

        @Nested
        @DisplayName("From a bundle")
        class WithBundle {
            @Test
            @DisplayName("Read")
            public void createReadAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Bundle bundle = new Bundle();

                final Patient patient = new Patient();
                patient.setIdElement(new IdType().setValue("Patient/TestId"));
                bundle.addEntry().setResource(patient).setRequest(
                        new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.GET)
                                .setUrl(patient.getId()));

                final List<AuditEvent> events = auditEventsBuilder.addBundle(bundle).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.R);
            }

            @Test
            @DisplayName("Create")
            public void createWriteAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Bundle bundle = new Bundle();

                final Patient patient = new Patient();
                bundle.addEntry().setResource(patient).setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST));
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addBundle(bundle).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.C);
            }

            @Test
            @DisplayName("Update")
            public void createUpdateAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Bundle bundle = new Bundle();

                final Patient patient = new Patient();
                bundle.addEntry().setResource(patient).setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.PUT));
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addBundle(bundle).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.U);
            }

            @Test
            @DisplayName("Delete")
            public void createDeleteAuditEvent() {
                auditEventsBuilder = new AuditEventsBuilder(requesterData);
                final Bundle bundle = new Bundle();

                final Patient patient = new Patient();
                bundle.addEntry().setResource(patient).setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE));
                patient.setIdElement(new IdType().setValue("Patient/TestId"));

                final List<AuditEvent> events = auditEventsBuilder.addBundle(bundle).build();

                assertEquals(events.size(), 1);
                assertSame(events.get(0).getAction(), AuditEvent.AuditEventAction.D);
            }
        }

    }
}
