package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.PatientData;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PatientDataBuilder {
    private static final String BLOOD_RELATIONSHIP_EXTENSION = "http://fhir.cqgc.ferlab.bio/StructureDefinition/blood-relationship";
    private static final String ETHNICITY_EXTENSION = "http://fhir.cqgc.ferlab.bio/StructureDefinition/qc-ethnicity";
    private static final String IS_PROBAND_EXTENSION = "http://fhir.cqgc.ferlab.bio/StructureDefinition/is-proband";
    private static final String FAMILY_ID_EXTENSION = "http://fhir.cqgc.ferlab.bio/StructureDefinition/family-id";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String STATUS_ACTIVE = "active";

    private final PatientDataConfiguration configuration;
    private final IParser parser;
    private PatientData patientData;

    public PatientDataBuilder(PatientDataConfiguration configuration, FhirContext context) {
        this.configuration = configuration;
        this.parser = context.newJsonParser();
    }

    private class Handle<T> {
        final Class<T> tClass;
        final Consumer<T> callback;

        public Handle(Class<T> tClass, Consumer<T> callback) {
            this.tClass = tClass;
            this.callback = callback;
        }
    }

    private final List<Handle<?>> handles = Arrays.asList(
            new Handle<>(Patient.class, this::handlePatient),
            new Handle<>(ServiceRequest.class, this::handleServiceRequest)
    );


    public PatientData fromJson(byte[] content) {
        return this.fromJson(new String(content));
    }

    public PatientData fromJson(String content) {
        return this.fromBundle(this.parser.parseResource(Bundle.class, content));
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


    private void handlePatient(Patient patient) {
        patientData.setId(patient.getId());
        patientData.setMrn(patient.getIdentifier().get(0).getValue());
        patientData.setStatus(patient.getActive() ? STATUS_ACTIVE : STATUS_INACTIVE);
        patientData.setGender(patient.getGender().getDisplay());

        if (patient.hasName()) {
            final Name name = extractName(patient.getName());
            patientData.setLastName(name.lastName);
            patientData.setFirstName(name.firstName);
        }
        if (patient.getIdentifier().size() > 1) {
            patientData.setRamq(patient.getIdentifier().get(1).getValue());
        }

        if (patient.hasExtension(BLOOD_RELATIONSHIP_EXTENSION)) {
            final Extension extension = patient.getExtensionByUrl(BLOOD_RELATIONSHIP_EXTENSION);
            if (extension.hasValue() && extension.getValue() instanceof Coding) {
                final Coding value = (Coding) extension.getValue();
                patientData.setBloodRelationship(value.getDisplay());
            }
        }

        if (patient.hasExtension(ETHNICITY_EXTENSION)) {
            final Extension extension = patient.getExtensionByUrl(ETHNICITY_EXTENSION);
            if (extension.hasValue() && extension.getValue() instanceof Coding) {
                final Coding value = (Coding) extension.getValue();
                patientData.setEthnicity(value.getDisplay());
            }
        }

        if(patient.hasExtension(IS_PROBAND_EXTENSION)){
            final Extension extension = patient.getExtensionByUrl(IS_PROBAND_EXTENSION);
            if (extension.hasValue()) {
                final BooleanType value = (BooleanType) extension.getValue();
                patientData.setPosition(value.booleanValue() ? "Proband" : "Parent");
            }
        }

        if(patient.hasExtension(FAMILY_ID_EXTENSION)){
            final Extension extension = patient.getExtensionByUrl(IS_PROBAND_EXTENSION);
            if(extension.hasValue() && extension.getValue() instanceof Reference){
                final Reference value = (Reference) extension.getValue();
                patientData.setFamilyId(value.getReference());
            }
        }

        if (patient.hasGeneralPractitioner()) {
            final String id = patient.getGeneralPractitioner().get(0).getReference();
            final Practitioner practitioner = configuration.practitionerDao.read(new IdType(id));
            final Name name = extractName(practitioner.getName());
            patientData.setPractitioner(String.format("%s %s", name.firstName, name.lastName));
        }

        if (patient.hasManagingOrganization()) {
            final String id = patient.getManagingOrganization().getReference();
            final Organization organization = configuration.organizationDAO.read(new IdType(id));
            final String name = organization.hasName() ? organization.getName() : id;
            patientData.setOrganization(name);
        }

        if(patient.hasBirthDate()){
            patientData.setBirthDate(patientData.getBirthDate());
        }


    }

    private void handleServiceRequest(ServiceRequest serviceRequest) {
        patientData.setRequest(serviceRequest.getId());
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
