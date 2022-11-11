package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AbstractPrescriptionData;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bio.ferlab.clin.interceptors.ServiceRequestPerformerInterceptor.ANALYSIS_REQUEST_CODE;
import static bio.ferlab.clin.utils.Extensions.FAMILY_MEMBER;

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
      prescriptionData.setLdm(serviceRequest.getPerformer().get(0).getReferenceElement().getIdPart());
    }

    if (serviceRequest.hasSubject()) {
      final Reference subjectRef = serviceRequest.getSubject();
      final Patient patient = this.configuration.patientDAO.read(new IdType(subjectRef.getReference()));

      if (patient.hasIdentifier()) {
        final Optional<String> mrn = patient.getIdentifier().stream()
            .filter(id -> id.hasType() && MRN_CODE.equals(id.getType().getCodingFirstRep().getCode()))
            .map(Identifier::getValue).findFirst();
        mrn.ifPresent(prescriptionData::setPatientMRN);
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
  
  protected String getSampleValue(ServiceRequest serviceRequest, RequestDetails requestDetails) {
    for(Reference specimenRef: serviceRequest.getSpecimen()) {
      final Specimen specimen = this.configuration.specimenDao.read(new IdType(specimenRef.getReference()), requestDetails);
      if(specimen.hasParent() && specimen.hasAccessionIdentifier()) { // specimen with a parent is the good one
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

  protected Reference extractParentReference(ServiceRequest serviceRequest, String relationCode) {
    if (serviceRequest.hasExtension()) {
      // get all family-member exts
      for(Extension ext: serviceRequest.getExtensionsByUrl(FAMILY_MEMBER)) {
        // first is the patient ref
        final Extension parentExt = ext.getExtensionByUrl("parent");
        // second if the relation with the patient mother or father
        final Extension parentRelationExt = ext.getExtensionByUrl("parent-relationship");
        if (parentExt != null && parentRelationExt !=null && parentExt.hasValue() && parentRelationExt.hasValue()) {
          final CodeableConcept relation = (CodeableConcept) parentRelationExt.getValue();
          if (relation.hasCoding() && relationCode.equals(relation.getCodingFirstRep().getCode())) {
            return (Reference) parentExt.getValue();
          }
        }
      }
    }
    return null;
  }
}
