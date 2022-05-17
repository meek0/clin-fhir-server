package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AbstractPrescriptionData;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
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

import static bio.ferlab.clin.es.builder.CommonDataBuilder.MRN_CODE;
import static bio.ferlab.clin.interceptors.ServiceRequestPerformerInterceptor.ANALYSIS_REQUEST_CODE;

public abstract class AbstractPrescriptionDataBuilder {

  public enum Type {
    ANALYSIS("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-analysis-request"),
    SEQUENCING("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-sequencing-request");
    
    public final String value;

    Type(String value){
      this.value = value;
    }
  }

  // thread confinement because SimpleDateFormat not thread-safe
  private final ThreadLocal<SimpleDateFormat> formatter = ThreadLocal
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
      prescriptionData.setRequester(""); // TODO need documentation
    }
    
    if(serviceRequest.hasPerformer()) {
      prescriptionData.setLdm(serviceRequest.getPerformer().get(0).getReferenceElement().getIdPart());
    }

    final Reference subjectRef = serviceRequest.getSubject();
    final Patient patient = this.configuration.patientDAO.read(new IdType(subjectRef.getReference()));

    final Optional<String> mrn = Objects.requireNonNull(patient.getIdentifier()).stream()
        .filter(id -> id.getType().getCodingFirstRep().getCode().contentEquals(MRN_CODE))
        .map(Identifier::getValue).findFirst();
    mrn.ifPresent(prescriptionData::setPatientMRN);
    
    final Reference organizationRef = patient.getManagingOrganization();
    final Organization organization = configuration.organizationDAO.read(new IdType(organizationRef.getReference()));
    
    if(organization.hasAlias()) {
      prescriptionData.setEp(organization.getAlias().get(0).getValue());
    }
    
    if(serviceRequest.hasAuthoredOn()) {
      prescriptionData.setCreatedOn(formatter.get().format(serviceRequest.getAuthoredOn()));
    }
    
  }

  @SuppressWarnings("unchecked")
  protected <T extends IBaseResource> List<T> getListFromProvider(IBundleProvider provider) {
    final List<T> resources = new ArrayList<>();
    if (!provider.isEmpty()) {
      for (IBaseResource sr : provider.getResources(0, provider.size())) {
        resources.add((T) sr);
      }
    }
    return resources;
  }
}
