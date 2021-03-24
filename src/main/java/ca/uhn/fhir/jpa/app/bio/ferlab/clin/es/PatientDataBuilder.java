package bio.ferlab.clin.es;

import bio.ferlab.clin.es.config.PatientDataConfiguration;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.utils.Extensions;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PatientDataBuilder {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final String ID_SEPARATOR = "/";

    private final PatientDataConfiguration configuration;

    public PatientDataBuilder(PatientDataConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<PatientData> fromIds(Set<String> ids) {
        final List<PatientData> patientDataList = new ArrayList<>();
        for (final String patientId : ids) {
            final PatientData patientData = new PatientData();
            final Patient patient = this.configuration.patientDAO.read(new IdType(patientId));

            this.handlePatient(patient, patientData);
            if (patient.hasExtension(Extensions.FAMILY_ID)) {
                final Extension extension = patient.getExtensionByUrl(Extensions.FAMILY_ID);
                final Reference ref = (Reference) extension.getValue();
                final Group group = this.configuration.groupDao.read(ref.getReferenceElement());
                this.handleFamilyGroup(group, patientData);
            }

            final SearchParameterMap searchMap = SearchParameterMap.newSynchronous("subject", new ReferenceParam(patientId));
            final IBundleProvider srProvider = this.configuration.serviceRequestDAO.search(searchMap);

            final List<ServiceRequest> serviceRequests = getListFromProvider(srProvider);
            for (ServiceRequest serviceRequest : serviceRequests) {
                patientData.getRequests().add(this.handleServiceRequest(serviceRequest));
            }
            patientDataList.add(patientData);
        }
        return patientDataList;
    }

    void handlePatient(Patient patient, PatientData patientData) {
        patientData.setId(patient.getIdElement().getIdPart());
        patientData.setMrn(patient.getIdentifier().get(0).getValue());
        patientData.setGender(patient.getGender().getDisplay());

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

        if (patient.hasExtension(Extensions.IS_FETUS)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.IS_FETUS);
            if (extension.hasValue()) {
                final BooleanType value = (BooleanType) extension.getValue();
                patientData.setFetus(value.booleanValue());
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
            final PractitionerRole practitionerRole = configuration.practitionerRoleDao.read(new IdType(id));
            final Practitioner practitioner = configuration.practitionerDao.read(practitionerRole.getPractitioner().getReferenceElement());
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

    PatientData.RequestData handleServiceRequest(ServiceRequest serviceRequest) {
        final PatientData.RequestData requestData = new PatientData.RequestData();
        requestData.setRequest(serviceRequest.getIdElement().getIdPart());
        requestData.setStatus(serviceRequest.getStatus().toCode());
        if (serviceRequest.hasCode()) {
            final CodeableConcept code = serviceRequest.getCode();
            if (code.hasCoding()) {
                requestData.setTest(code.getCoding().get(0).getCode());
            }
        }

        if (serviceRequest.hasExtension(Extensions.IS_SUBMITTED)) {
            final Extension extension = serviceRequest.getExtensionByUrl(Extensions.IS_SUBMITTED);
            final BooleanType valueBoolean = (BooleanType) extension.getValue();
            requestData.setSubmitted(valueBoolean.getValue());
        }

        if (serviceRequest.hasAuthoredOn()) {
            requestData.setPrescription(simpleDateFormat.format(serviceRequest.getAuthoredOn()));
        }
        return requestData;
    }

    void handleFamilyGroup(Group group, PatientData patientData) {
        patientData.setFamilyId(group.getIdElement().getIdPart());
        if (group.hasExtension(Extensions.FAMILY_ID)) {
            final Extension extension = group.getExtensionByUrl(Extensions.FAMILY_ID);
            final Coding coding = (Coding) extension.getValue();
            patientData.setFamilyType(coding.getDisplay());
        } else {
            patientData.setFamilyType("Solo");
        }
    }

    private static Name extractName(List<HumanName> humanNames) {
        final HumanName name = humanNames.get(0);
        return new Name(name.getFamily(), name.getGivenAsSingleString());
    }

    @SuppressWarnings("unchecked")
    private <T extends IBaseResource> List<T> getListFromProvider(IBundleProvider provider) {
        final List<T> resources = new ArrayList<>();
        if (!provider.isEmpty()) {
            for (IBaseResource sr : provider.getResources(0, provider.size())) {
                resources.add((T) sr);
            }
        }
        return resources;
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
