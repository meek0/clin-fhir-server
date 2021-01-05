package bio.ferlab.clin.es.data.builder;

import bio.ferlab.clin.es.config.PatientDataConfiguration;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.utils.Extensions;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

@Component
public class PatientDataBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PatientDataBuilder.class);
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final String ID_SEPARATOR = "/";

    private final List<Handle<?>> handles = Arrays.asList(
            new Handle<>(Patient.class, this::handlePatient),
            new Handle<>(ServiceRequest.class, this::handleServiceRequest),
            new Handle<>(Group.class, this::handleFamilyGroup)
    );
    private final PatientDataConfiguration configuration;
    private final IParser parser;

    private PatientData patientData;

    public PatientDataBuilder(PatientDataConfiguration configuration, FhirContext context) {
        this.configuration = configuration;
        this.parser = context.newJsonParser();
    }

    public PatientData fromJson(byte[] content) {
        return this.fromJson(new String(content));
    }

    public PatientData fromJson(String content) {
        try {
            return this.fromBundle(this.parser.parseResource(Bundle.class, content));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public PatientData fromBundle(Bundle bundle) {
        this.patientData = new PatientData();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            final Resource resource = entry.getResource();
            for (Handle handle : handles) {
                if (handle.tClass.isInstance(resource)) {
                    handle.callback.accept(handle.tClass.cast(resource));
                }
            }
        }
        return this.patientData;
    }


    void handlePatient(Patient patient) {
        patientData.setId(patient.getId());
        patientData.setMrn(patient.getIdentifier().get(0).getValue());
        patientData.setGender(patient.getGender().getDisplay());
        patientData.setFamilyType("trio");

        if (patient.hasName()) {
            final Name name = extractName(patient.getName());
            patientData.setLastName(name.lastName);
            patientData.setFirstName(name.firstName);
        }
        if (patient.getIdentifier().size() > 1) {
            patientData.setRamq(patient.getIdentifier().get(1).getValue());
        }

        if (patient.hasExtension(Extensions.BLOOD_RELATIONSHIP)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.BLOOD_RELATIONSHIP);
            if (extension.hasValue() && extension.getValue() instanceof Coding) {
                final Coding value = (Coding) extension.getValue();
                patientData.setBloodRelationship(value.getDisplay());
            }
        }

        if (patient.hasExtension(Extensions.ETHNICITY)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.ETHNICITY);
            if (extension.hasValue() && extension.getValue() instanceof Coding) {
                final Coding value = (Coding) extension.getValue();
                patientData.setEthnicity(value.getDisplay());
            }
        }

        if (patient.hasExtension(Extensions.IS_PROBAND)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.IS_PROBAND);
            if (extension.hasValue()) {
                final BooleanType value = (BooleanType) extension.getValue();
                patientData.setPosition(value.booleanValue() ? "Proband" : "Parent");
            }
        }

        if (patient.hasExtension(Extensions.FAMILY_ID)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.FAMILY_ID);
            if (extension.hasValue() && extension.getValue() instanceof Reference) {
                final Reference value = (Reference) extension.getValue();
                final String[] idParts = value.getReference().split(ID_SEPARATOR);
                patientData.setFamilyId(idParts.length > 1 ? idParts[1] : idParts[0]);
            }
        }

        if (patient.hasGeneralPractitioner()) {
            final String id = patient.getGeneralPractitioner().get(0).getReference();
            final Practitioner practitioner = configuration.practitionerDao.read(new IdType(id));
            final Name name = extractName(practitioner.getName());
            patientData.getPractitioner().setId(id);
            patientData.getPractitioner().setLastName(name.lastName);
            patientData.getPractitioner().setFirstName(name.firstName);
        }

        if (patient.hasManagingOrganization()) {
            final String id = patient.getManagingOrganization().getReference();
            final Organization organization = configuration.organizationDAO.read(new IdType(id));
            patientData.getOrganization().setId(id);
            patientData.getOrganization().setName(organization.hasName() ? organization.getName() : "");
        }

        if (patient.hasBirthDate()) {
            patientData.setBirthDate(simpleDateFormat.format(patient.getBirthDate()));
        }
    }

    void handleServiceRequest(ServiceRequest serviceRequest) {
        patientData.setRequest(serviceRequest.getId());
        patientData.setStatus(serviceRequest.getStatus().toCode());
        if (serviceRequest.hasCode()) {
            final CodeableConcept code = serviceRequest.getCode();
            if (code.hasCoding()) {
                patientData.setTest(code.getCoding().get(0).getCode());
            }
        }

        if (serviceRequest.hasExtension(Extensions.IS_SUBMITTED)){
            final Extension extension = serviceRequest.getExtensionByUrl(Extensions.IS_SUBMITTED);
            final BooleanType valueBoolean = (BooleanType) extension.getValue();
            patientData.setSubmitted(valueBoolean.getValue());
        }

        if (serviceRequest.hasAuthoredOn()) {
            patientData.setPrescription(simpleDateFormat.format(serviceRequest.getAuthoredOn()));
        }
    }

    void handleFamilyGroup(Group group) {
        patientData.setFamilyId(group.getId());
    }

    private static Name extractName(List<HumanName> humanNames) {
        final HumanName name = humanNames.get(0);
        return new Name(name.getFamily(), name.getGivenAsSingleString());
    }

    private static class Name {
        public final String lastName;
        public final String firstName;

        public Name(String lastName, String firstName) {
            this.lastName = lastName;
            this.firstName = firstName;
        }
    }
}
