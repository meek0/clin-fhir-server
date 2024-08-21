package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AbstractPrescriptionData;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import bio.ferlab.clin.es.data.nanuq.SequencingRequestData;
import bio.ferlab.clin.utils.FhirUtils;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static bio.ferlab.clin.interceptors.ServiceRequestPerformerInterceptor.ANALYSIS_REQUEST_CODE;
import static bio.ferlab.clin.utils.Extensions.FAMILY_MEMBER;
import static bio.ferlab.clin.utils.Extensions.SEQUENCING_EXPERIMENT;

public abstract class AbstractPrescriptionDataBuilder {

  public enum Type {
    ANALYSIS("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-analysis-request"),
    SEQUENCING("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-sequencing-request");

    public final String value;

    Type(String value){
      this.value = value;
    }
  }

  public static final String MRN_CODE = "MR";

  // thread confinement because SimpleDateFormat not thread-safe
  public final ThreadLocal<SimpleDateFormat> formatter = ThreadLocal
      .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

  private final Type type;
  private final ResourceDaoConfiguration configuration;

  private String[] getParentInfo(ServiceRequest analysis, ServiceRequest sequencing) {
    String subjectIdPart = sequencing.getSubject().getReferenceElement().getIdPart();
    String parentRelation = "CasIndex";
    String parentStatus = "IND";

    for(var member: analysis.getExtensionsByUrl(FAMILY_MEMBER)) {
      var parentPatientRef = ((Reference) member.getExtensionByUrl("parent").getValue());

      if (parentPatientRef.getReference().equals(sequencing.getSubject().getReference())) {
        parentRelation = ((CodeableConcept) member.getExtensionByUrl("parent-relationship").getValue()).getCodingFirstRep().getCode();
      }
    }

    var orParams = new ReferenceOrListParam();
    analysis.getSupportingInfo().forEach(info -> orParams.add(new ReferenceParam(info.getReferenceElement().getIdPart())));

    final SearchParameterMap sm = SearchParameterMap.newSynchronous("subject", new ReferenceParam(subjectIdPart));
    sm.add("_id", orParams);
    sm.addInclude(new Include("ClinicalImpression:investigation"));

    final IBundleProvider clinicalImpressionProvider = this.configuration.clinicalImpressionDAO.search(sm);

    if (clinicalImpressionProvider != null && !clinicalImpressionProvider.isEmpty()) {
      List<String> statuses = clinicalImpressionProvider.getAllResources().stream()
      .filter(i -> i instanceof Observation && ((Observation) i).getCode().getCodingFirstRep().getCode().equals("DSTA"))
      .map(i -> ((Observation) i).getInterpretationFirstRep().getCodingFirstRep().getCode())
      .collect(Collectors.toList());

      if (statuses.size() == 1) {
        parentStatus = statuses.get(0);
      }
    }

    String[] result = { parentRelation, parentStatus };

    return result;
  }

  public AbstractPrescriptionDataBuilder(Type type, ResourceDaoConfiguration configuration) {
    this.type = type;
    this.configuration = configuration;
  }

  protected boolean isValidType(ServiceRequest serviceRequest) {
    return serviceRequest.getMeta().getProfile().stream().anyMatch(s -> this.type.value.equals(s.getValue()));
  }

  protected void handlePrescription(ServiceRequest serviceRequest, AbstractPrescriptionData prescriptionData) {
    prescriptionData.setPatientId(serviceRequest.getSubject().getReferenceElement().getIdPart());
    prescriptionData.getSecurityTags().addAll(serviceRequest.getMeta().getSecurity().stream().map(IBaseCoding::getCode)
        .collect(Collectors.toList()));

    if(serviceRequest.hasStatus()) {
      prescriptionData.setStatus(serviceRequest.getStatus().toCode());
    }

    if(serviceRequest.hasPriority()) {
      prescriptionData.setPriority(serviceRequest.getPriority().toCode());
    }

    if(serviceRequest.hasCategory()) {
      prescriptionData.setPrenatal(false); // TODO need documentation
    }

    if(serviceRequest.hasCode() && serviceRequest.getCode().hasCoding()) {
      for (Coding coding: serviceRequest.getCode().getCoding()) {
        if (ANALYSIS_REQUEST_CODE.equals(coding.getSystem()) && StringUtils.isNotBlank(coding.getCode())) {
          prescriptionData.setAnalysisCode(coding.getCode());
        }
      }
    }

    if(serviceRequest.hasRequester()) {
      prescriptionData.setRequester(serviceRequest.getRequester().getReferenceElement().getIdPart());
    }

    if(serviceRequest.hasPerformer()) {
      prescriptionData.setLdm(FhirUtils.getPerformerIds(serviceRequest, Organization.class).stream().findFirst().orElse(null));
    }

    if (serviceRequest.hasSubject()) {
      final Reference subjectRef = serviceRequest.getSubject();
      final Patient patient = this.configuration.patientDAO.read(new IdType(subjectRef.getReference()));

      if (patient.hasIdentifier()) {
        extractMRN(patient).ifPresent(prescriptionData::setPatientMRN);
      }

      if (patient.hasManagingOrganization()) {
        final Reference organizationRef = patient.getManagingOrganization();
        final Organization organization = configuration.organizationDAO.read(new IdType(organizationRef.getReference()));

        if (organization.hasAlias()) {
          prescriptionData.setEp(organization.getAlias().get(0).getValue());
        }
      }
    }

    if(serviceRequest.hasAuthoredOn()) {
      prescriptionData.setCreatedOn(formatter.get().format(serviceRequest.getAuthoredOn()));
    }

  }

  protected Set<String> addTasks(ServiceRequest serviceRequest, AbstractPrescriptionData data) {
    final SearchParameterMap sm = SearchParameterMap.newSynchronous("focus", new ReferenceParam(serviceRequest.getIdElement().getIdPart()));
    final IBundleProvider taskProvider = this.configuration.taskDao.search(sm);
    final List<Task> tasks = this.getListFromProvider(taskProvider);

    final Set<String> runNames = new HashSet<>();

    tasks.forEach(t -> {
      if (t.hasExtension(SEQUENCING_EXPERIMENT)) {
        String runName = t.getExtensionByUrl(SEQUENCING_EXPERIMENT).getExtensionByUrl("runName").getValueAsPrimitive().toString();
        runNames.add(runName);
      }

      data.getTasks().add(t.getCode().getCodingFirstRep().getCode());
    });

    return runNames;
  }

  protected void addRunInfo(ServiceRequest analysis, ServiceRequest sequencing, SequencingData data) {
    String[] parentInfo = this.getParentInfo(analysis, sequencing);

    data.setPatientRelationship(parentInfo[0]);
    data.setPatientDiseaseStatus(parentInfo[1]);
  }

  protected void addRunInfo(ServiceRequest analysis, ServiceRequest sequencing, SequencingRequestData data) {
    String[] parentInfo = this.getParentInfo(analysis, sequencing);

    data.setPatientRelationship(parentInfo[0]);
    data.setPatientDiseaseStatus(parentInfo[1]);
  }

  protected String getSampleValue(ServiceRequest serviceRequest, RequestDetails requestDetails) {
    for(Reference specimenRef: serviceRequest.getSpecimen()) {
      final Specimen specimen = this.configuration.specimenDao.read(new IdType(specimenRef.getReference()), requestDetails);
      // specimen WITH a parent is the sample
      if(specimen.hasParent() && specimen.hasAccessionIdentifier()) {
        return specimen.getAccessionIdentifier().getValue();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  protected <T extends IBaseResource> List<T> getListFromProvider(IBundleProvider provider) {
    final List<T> resources = new ArrayList<>();
    if (!provider.isEmpty()) {
      for (IBaseResource sr : provider.getAllResources()) {
        resources.add((T) sr);
      }
    }
    return resources;
  }

  protected Optional<String> extractMRN(Patient patient) {
    return patient.getIdentifier().stream()
      .filter(id -> id.hasType() && MRN_CODE.equals(id.getType().getCodingFirstRep().getCode()))
      .map(Identifier::getValue).findFirst();
  }

}
