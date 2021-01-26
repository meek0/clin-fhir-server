package bio.ferlab.clin.es;

import bio.ferlab.clin.es.config.PatientDataConfiguration;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.builder.PatientDataBuilder;
import bio.ferlab.clin.utils.Extensions;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class PatientDataBuilderTest {
    private JsonGenerator jsonGenerator;
    private PatientDataBuilder patientDataBuilder;

    private PatientDataConfiguration patientDataConfiguration;

    @Mock
    private IFhirResourceDao<Practitioner> practitionerDao;

    @Mock
    private IFhirResourceDao<PractitionerRole> practitionerRoleDao;

    @Mock
    private IFhirResourceDao<Organization> organizationDao;

    private ServiceRequest serviceRequest;
    private Practitioner practitioner;
    private PractitionerRole practitionerRole;
    private Organization organization;
    private Patient patient;
    private Bundle bundle;
    private PatientData expectedPatientData;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.practitioner = new Practitioner();
        this.practitioner.addName().setFamily("PractitionerFamilyName").addGiven("PractitionerGiven");
        this.practitioner.setId("PR12345");

        this.practitionerRole = new PractitionerRole();
        this.practitionerRole.setPractitioner(new Reference(this.practitioner));

        this.organization = new Organization();
        this.organization.setId("OR12345");
        this.organization.setName("Organization");

        this.serviceRequest = new ServiceRequest();
        this.serviceRequest.setId("SR12345");
        final CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://fhir.cqgc.ferlab.bio/CodeSystem/service-request-code")
                .setCode("WGS")
                .setDisplay("Whole Genome Sequencing");
        this.serviceRequest.setCode(code);
        this.serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
        this.serviceRequest.setAuthoredOn(new Date());

        this.patient = new Patient();
        this.patient.setId("PA12345");
        this.patient.addIdentifier()
                .setSystem("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-patient")
                .setValue("12345");
        this.patient.addName()
                .setFamily("Test")
                .addGiven("Test");

        this.patient.addExtension().setUrl(Extensions.ETHNICITY)
                .setValue(new Coding()
                        .setCode("EU")
                        .setDisplay("European Caucasia"));
        this.patient.addExtension().setUrl(Extensions.IS_PROBAND)
                .setValue(new BooleanType(true));
        this.patient.addExtension().setUrl(Extensions.FAMILY_ID)
                .setValue(new Reference("GR12345"));

        this.patient.setGender(Enumerations.AdministrativeGender.MALE);
        this.patient.addGeneralPractitioner().setReference(this.practitioner.getId());
        this.patient.setManagingOrganization(new Reference(this.organization.getId()));
        this.patient.setBirthDate(new Date());

        when(this.organizationDao.read(any())).thenReturn(this.organization);
        when(this.practitionerDao.read(any())).thenReturn(this.practitioner);
        when(this.practitionerRoleDao.read(any())).thenReturn(this.practitionerRole);

        this.patientDataConfiguration = new PatientDataConfiguration(null, null, this.organizationDao, this.practitionerDao, this.practitionerRoleDao);
        this.bundle = new Bundle();
        this.bundle.setId("bundle-id");
        this.bundle.addEntry().setResource(patient);
        this.bundle.addEntry().setResource(serviceRequest);

        this.jsonGenerator = new JsonGenerator(FhirContext.forR4());
        this.patientDataBuilder = new PatientDataBuilder(patientDataConfiguration, FhirContext.forR4());

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        this.expectedPatientData = new PatientData();
        this.expectedPatientData.setId(String.format("Patient/%s", patient.getId()));
        this.expectedPatientData.setMrn(patient.getIdentifier().get(0).getValue());
        this.expectedPatientData.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED.toCode());
        this.expectedPatientData.setGender("Male");
        this.expectedPatientData.setLastName(patient.getName().get(0).getFamily());
        this.expectedPatientData.setFirstName(patient.getName().get(0).getGiven().get(0).asStringValue());
        this.expectedPatientData.setRamq("");
        this.expectedPatientData.setBloodRelationship("");
        this.expectedPatientData.setEthnicity("European Caucasia");
        this.expectedPatientData.setPosition("Proband");
        this.expectedPatientData.setFamilyId("GR12345");
        this.expectedPatientData.setFamilyType("trio");
        this.expectedPatientData.setBirthDate(simpleDateFormat.format(patient.getBirthDate()));
        this.expectedPatientData.setRequest(String.format("ServiceRequest/%s", this.serviceRequest.getId()));
        this.expectedPatientData.setTest("WGS");
        this.expectedPatientData.setPrescription(simpleDateFormat.format(serviceRequest.getAuthoredOn()));
        this.expectedPatientData.getPractitioner().setId(practitioner.getId());
        this.expectedPatientData.getPractitioner().setLastName(practitioner.getName().get(0).getFamily());
        this.expectedPatientData.getPractitioner().setFirstName(practitioner.getName().get(0).getGiven().get(0).asStringValue());
        this.expectedPatientData.getOrganization().setId(organization.getId());
        this.expectedPatientData.getOrganization().setName(organization.getName());
    }

    @Nested
    @DisplayName("Valid bundle")
    class Valid {
        @Test
        @DisplayName("Should extract the data correctly")
        public void shouldExtractCorrectly() {
            final String content = jsonGenerator.toString(bundle);
            final PatientData patientData = patientDataBuilder.fromJson(content);

            final String output = jsonGenerator.toString(patientData);

            //Ignore the timestamp
            expectedPatientData.setTimestamp(patientData.getTimestamp());

            final String expectedJson = jsonGenerator.toString(expectedPatientData);

            Assertions.assertTrue(expectedJson.contentEquals(output));
        }
    }

    @Nested
    @DisplayName("Invalid bundle")
    class InValid {
        @Test
        @DisplayName("Should return null")
        public void shouldExtractCorrectly() {
            final PatientData patientData = patientDataBuilder.fromJson("");
            Assertions.assertNull(patientData);
        }
    }
}
