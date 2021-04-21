package bio.ferlab.clin.es.builder;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.utils.Extensions;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PrescriptionDataBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PrescriptionDataBuilder.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final String ID_SEPARATOR = "/";
    public static final String MRN_CODE = "MR";

    private final ResourceDaoConfiguration configuration;

    public PrescriptionDataBuilder(ResourceDaoConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<PrescriptionData> fromIds(Set<String> ids, RequestDetails requestDetails) {
        final List<PrescriptionData> prescriptionDataList = new ArrayList<>();
        for (final String serviceRequestId : ids) {
            final PrescriptionData prescriptionData = new PrescriptionData();

            final ServiceRequest serviceRequest = this.configuration.serviceRequestDAO.read(new IdType(serviceRequestId), requestDetails);
            if (!serviceRequest.hasSubject()) {
                logger.error(String.format("Cannot index ServiceRequest [%s]: resource has no subject.", serviceRequestId));
                continue;
            }
            final Reference subject = serviceRequest.getSubject();
            final Patient patient = this.configuration.patientDAO.read(new IdType(subject.getReference()), requestDetails);
            final String patientId = patient.getIdElement().getIdPart();

            this.handlePatient(patient, prescriptionData.getPatientInfo());
            this.handleServiceRequest(serviceRequest, prescriptionData);
            if (patient.hasExtension(Extensions.FAMILY_ID)) {
                final Extension extension = patient.getExtensionByUrl(Extensions.FAMILY_ID);
                final Reference ref = (Reference) extension.getValue();
                final Group group = this.configuration.groupDao.read(ref.getReferenceElement());
                this.handleFamilyGroup(group, prescriptionData.getFamilyInfo());
            }
            prescriptionDataList.add(prescriptionData);
        }
        return prescriptionDataList;
    }

    private boolean isObservationOfCode(Observation observation, String code) {
        final List<Coding> coding = observation.getCode().getCoding();
        if (coding.isEmpty()) {
            return false;
        }
        return coding.get(0).getCode().contentEquals(code);
    }

    void handlePatient(Patient patient, PrescriptionData.PatientInformation patientInfo) {
        patientInfo.setId(patient.getIdElement().getIdPart());
        final List<String> mrns = Objects.requireNonNull(patient.getIdentifier()).stream()
                .filter(id -> id.getType().getCodingFirstRep().getCode().contentEquals(MRN_CODE))
                .map(Identifier::getValue).collect(Collectors.toList());

        patientInfo.getMrn().addAll(mrns);
        patientInfo.setGender(patient.getGender().getDisplay());

        if (patient.hasName()) {
            final Name name = extractName(patient.getName());
            patientInfo.setLastName(name.lastName);
            patientInfo.setFirstName(name.firstName);
        }
        if (patient.getIdentifier().size() > 1) {
            patientInfo.setRamq(patient.getIdentifier().get(1).getValue());
        }

        if (patient.hasExtension(Extensions.IS_PROBAND)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.IS_PROBAND);
            if (extension.hasValue()) {
                final BooleanType value = (BooleanType) extension.getValue();
                patientInfo.setPosition(value.booleanValue() ? "Proband" : "Parent");
            }
        }

        if (patient.hasExtension(Extensions.IS_FETUS)) {
            final Extension extension = patient.getExtensionByUrl(Extensions.IS_FETUS);
            if (extension.hasValue()) {
                final BooleanType value = (BooleanType) extension.getValue();
                patientInfo.setFetus(value.booleanValue());
            }
        }

        if (patient.hasManagingOrganization()) {
            final String id = patient.getManagingOrganization().getReference();
            final Organization organization = configuration.organizationDAO.read(new IdType(id));
            patientInfo.getOrganization().setId(id);
            patientInfo.getOrganization().setName(organization.hasName() ? organization.getName() : "");
        }

        if (patient.hasBirthDate()) {
            patientInfo.setBirthDate(simpleDateFormat.format(patient.getBirthDate()));
        }

    }

    void handleServiceRequest(ServiceRequest serviceRequest, PrescriptionData prescriptionData) {
        prescriptionData.setId(serviceRequest.getIdElement().getIdPart());
        prescriptionData.setStatus(serviceRequest.getStatus().toCode());
        if (serviceRequest.hasCode()) {
            final CodeableConcept code = serviceRequest.getCode();
            if (code.hasCoding()) {
                prescriptionData.setTest(code.getCoding().get(0).getCode());
            }
        }

        if (serviceRequest.hasExtension(Extensions.IS_SUBMITTED)) {
            final Extension extension = serviceRequest.getExtensionByUrl(Extensions.IS_SUBMITTED);
            final BooleanType valueBoolean = (BooleanType) extension.getValue();
            prescriptionData.setSubmitted(valueBoolean.getValue());
        }

        if (serviceRequest.hasAuthoredOn()) {
            prescriptionData.setAuthoredOn(simpleDateFormat.format(serviceRequest.getAuthoredOn()));
        }

        if (serviceRequest.hasRequester()) {
            final String id = serviceRequest.getRequester().getReference();
            final PractitionerRole practitionerRole = configuration.practitionerRoleDao.read(new IdType(id));
            final Practitioner practitioner = configuration.practitionerDao.read(practitionerRole.getPractitioner().getReferenceElement());
            final Name name = extractName(practitioner.getName());
            prescriptionData.getPractitioner().setId(id);
            prescriptionData.getPractitioner().setLastName(name.lastName);
            prescriptionData.getPractitioner().setFirstName(name.firstName);
        }
    }

    void handleFamilyGroup(Group group, PrescriptionData.FamilyGroupInfo familyGroupInfo) {
        familyGroupInfo.setId(group.getIdElement().getIdPart());
        if (group.hasExtension(Extensions.FAMILY_ID)) {
            final Extension extension = group.getExtensionByUrl(Extensions.FAMILY_ID);
            final Coding coding = (Coding) extension.getValue();
            familyGroupInfo.setType(coding.getDisplay());
        } else {
            familyGroupInfo.setType("solo");
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
