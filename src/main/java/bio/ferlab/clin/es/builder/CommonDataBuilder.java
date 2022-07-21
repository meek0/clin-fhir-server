package bio.ferlab.clin.es.builder;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.es.data.nanuq.AbstractPrescriptionData;
import bio.ferlab.clin.utils.Extensions;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bio.ferlab.clin.interceptors.ServiceRequestPerformerInterceptor.ANALYSIS_REQUEST_CODE;

@Component
public class CommonDataBuilder {

  // thread confinement because SimpleDateFormat not thread-safe
  private final ThreadLocal<SimpleDateFormat> formatter = ThreadLocal
      .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

  private static final String ID_SEPARATOR = "/";
  public static final String MRN_CODE = "MR";
  public static final String JHN_CODE = "JHN";

  private final ResourceDaoConfiguration configuration;
  
  public CommonDataBuilder(ResourceDaoConfiguration configuration) {
    this.configuration = configuration;
  }

  public void handlePatient(Patient patient, PatientData patientData) {
    patientData.setCid(patient.getIdElement().getIdPart());
    final List<String> mrns = Objects.requireNonNull(patient.getIdentifier()).stream()
        .filter(id -> id.getType().getCodingFirstRep().getCode().contentEquals(MRN_CODE))
        .map(Identifier::getValue).collect(Collectors.toList());

    patientData.getMrn().addAll(mrns);
    patientData.setGender(patient.getGender().getDisplay());
    patientData.getSecurityTags().addAll(extractMetaTags(patient));

    if (patient.hasName()) {
      final Name name = extractName(patient.getName());
      patientData.setLastName(name.lastName);
      patientData.setFirstName(name.firstName);
      patientData.setLastNameFirstName(name.lastNameFirstName);
    }

    Optional<String> ramq = Objects.requireNonNull(patient.getIdentifier()).stream()
        .filter(id -> id.getType().getCodingFirstRep().getCode().contentEquals(JHN_CODE))
        .map(Identifier::getValue).findFirst();
    ramq.ifPresent(patientData::setRamq);

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

    if (patient.hasExtension(Extensions.FAMILY_ID)) {
      final Extension extension = patient.getExtensionByUrl(Extensions.FAMILY_ID);
      final Reference ref = (Reference) extension.getValue();
      final Group group = this.configuration.groupDao.read(ref.getReferenceElement());
      this.handleFamilyGroup(group, patientData);
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

    if (patient.hasManagingOrganization()) {
      final String id = patient.getManagingOrganization().getReference();
      final Organization organization = configuration.organizationDAO.read(new IdType(id));
      patientData.getOrganization().setCid(organization.getIdElement().getIdPart());
      patientData.getOrganization().setName(organization.hasName() ? organization.getName() : "");
    }

    if (patient.hasGeneralPractitioner()) {
      final String id = patient.getGeneralPractitioner().get(0).getReference();
      final PractitionerRole practitionerRole = configuration.practitionerRoleDao.read(new IdType(id));
      final Practitioner practitioner = configuration.practitionerDao.read(practitionerRole.getPractitioner().getReferenceElement());
      final Name name = extractName(practitioner.getName());
      patientData.getPractitioner().setCid(practitioner.getIdElement().getIdPart());
      patientData.getPractitioner().setLastName(name.lastName);
      patientData.getPractitioner().setFirstName(name.firstName);
      patientData.getPractitioner().setLastNameFirstName(name.lastNameFirstName);
    }

    if (patient.hasBirthDate()) {
      patientData.setBirthDate(formatter.get().format(patient.getBirthDate()));
    }

  }

  void handleFamilyGroup(Group group, PatientData patientData) {
    patientData.setFamilyId(group.getIdElement().getIdPart());
    if (group.hasExtension(Extensions.FM_STRUCTURE)) {
      final Extension extension = group.getExtensionByUrl(Extensions.FM_STRUCTURE);
      final Coding coding = (Coding) extension.getValue();
      patientData.setFamilyType(coding.getDisplay());
    } else {
      patientData.setFamilyType("SOLO");
    }
  }

  public void handleServiceRequest(ServiceRequest serviceRequest, PrescriptionData prescriptionData) {
    prescriptionData.setCid(serviceRequest.getIdElement().getIdPart());
    prescriptionData.setStatus(serviceRequest.getStatus().toCode());
    prescriptionData.getSecurityTags().addAll(extractMetaTags(serviceRequest));
    
    if (serviceRequest.hasCode() && serviceRequest.getCode().hasCoding()) {
      for (Coding coding: serviceRequest.getCode().getCoding()) {
        if (ANALYSIS_REQUEST_CODE.equals(coding.getSystem()) && StringUtils.isNotBlank(coding.getCode())) {
          prescriptionData.getAnalysis().setCode(coding.getCode());
          prescriptionData.getAnalysis().setDisplay(coding.getDisplay());
        }
      }
    }

    if (serviceRequest.hasAuthoredOn()) {
      prescriptionData.setAuthoredOn(formatter.get().format(serviceRequest.getAuthoredOn()));
    }

    if (serviceRequest.hasExtension(Extensions.IS_SUBMITTED)) {
      final Extension extension = serviceRequest.getExtensionByUrl(Extensions.IS_SUBMITTED);
      final BooleanType valueBoolean = (BooleanType) extension.getValue();
      prescriptionData.setSubmitted(valueBoolean.getValue());
    }

    if (serviceRequest.hasPerformer()) {
      prescriptionData.setLaboratory(serviceRequest.getPerformer().get(0).getReference());
    }

    if (serviceRequest.hasRequester()) {
      final String id = serviceRequest.getRequester().getReference();
      final Practitioner practitioner = configuration.practitionerDao.read(new IdType(id));
      final Name name = extractName(practitioner.getName());
      prescriptionData.getPrescriber().setCid(practitioner.getIdElement().getIdPart());
      prescriptionData.getPrescriber().setLastName(name.lastName);
      prescriptionData.getPrescriber().setFirstName(name.firstName);
      prescriptionData.getPrescriber().setLastNameFirstName(name.lastNameFirstName);
    }

    if (serviceRequest.hasExtension(Extensions.PROCEDURE_DIRECTED_BY)) {
      final Extension extension = serviceRequest.getExtensionByUrl(Extensions.PROCEDURE_DIRECTED_BY);
      final Reference ref = (Reference) extension.getValue();
      final PractitionerRole role = this.configuration.practitionerRoleDao.read(ref.getReferenceElement());
      final Practitioner practitioner = this.configuration.practitionerDao.read(role.getPractitioner().getReferenceElement());
      final Name name = extractName(practitioner.getName());
      prescriptionData.getApprover().setCid(practitioner.getIdElement().getIdPart());
      prescriptionData.getApprover().setFirstName(name.firstName);
      prescriptionData.getApprover().setLastName(name.lastName);
      prescriptionData.getApprover().setLastNameFirstName(name.lastNameFirstName);
    }

    if (serviceRequest.hasIdentifier()) {
      final var identifier = serviceRequest.getIdentifier().get(0);
      if (identifier.hasValue()) {
        prescriptionData.setMrn(identifier.getValue());
      }
      if (identifier.hasAssigner()) {
        final String id = identifier.getAssigner().getReference();
        final Organization organization = configuration.organizationDAO.read(new IdType(id));
        prescriptionData.getOrganization().setCid(organization.getIdElement().getIdPart());
        prescriptionData.getOrganization().setName(organization.hasName() ? organization.getName() : "");
      }
    }
  }

  public void finalize(PatientData patientData) {
    patientData.getRequests().forEach(r -> {
      r.setPatientInfo(null);
      r.applyFullText();
    });
    patientData.applyFullText();
  }
  
  public void finalize(PrescriptionData prescriptionData) {
    prescriptionData.getPatientInfo().setRequests(null);
    prescriptionData.applyFullText();
    prescriptionData.getPatientInfo().applyFullText();
    prescriptionData.buildState();
  }

  public Name extractName(List<HumanName> humanNames) {
    final HumanName name = humanNames.get(0);
    return new Name(name.getFamily(), name.getGivenAsSingleString());
  }

  public List<String> extractMetaTags(IBaseResource resource) {
    return resource.getMeta().getSecurity().stream().map(IBaseCoding::getCode).collect(Collectors.toList());
  }

  public class Name {
    public final String lastName;
    public final String firstName;
    public final String lastNameFirstName;

    public Name(String lastName, String firstName) {
      this.lastName = lastName;
      this.firstName = firstName;
      this.lastNameFirstName = lastName + ", " + firstName;
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends IBaseResource> List<T> getListFromProvider(IBundleProvider provider) {
    final List<T> resources = new ArrayList<>();
    if (!provider.isEmpty()) {
      for (IBaseResource sr : provider.getResources(0, provider.size())) {
        resources.add((T) sr);
      }
    }
    return resources;
  }
  
}
