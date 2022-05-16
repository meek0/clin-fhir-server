package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AbstractPrescriptionData;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.r4.model.*;

import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

public abstract class AbstractPrescriptionDataBuilder {

  public enum RequestType {
    ANALYSIS("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-analysis-request"),
    SEQUENCING("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-sequencing-request");
    public final String value;
    RequestType(String value){
      this.value = value;
    }
  }

  // thread confinement because SimpleDateFormat not thread-safe
  private final ThreadLocal<SimpleDateFormat> formatter = ThreadLocal
      .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
  
  private final RequestType type;
  private final ResourceDaoConfiguration configuration;

  public AbstractPrescriptionDataBuilder(RequestType type, ResourceDaoConfiguration configuration) {
    this.type = type;
    this.configuration = configuration;
  }

  protected boolean isValidType(ServiceRequest serviceRequest) {
    return serviceRequest.getMeta().getProfile().stream().anyMatch(s -> this.type.value.equals(s.getValue()));
  }

  protected void handlePrescription(ServiceRequest serviceRequest, AbstractPrescriptionData prescriptionData) {
    prescriptionData.setRequestId(serviceRequest.getIdElement().getIdPart());
    prescriptionData.setPatientId(serviceRequest.getSubject().getReferenceElement().getIdPart());
    prescriptionData.getSecurityTags().addAll(serviceRequest.getMeta().getSecurity().stream().map(IBaseCoding::getCode)
        .collect(Collectors.toList()));
    
    if(serviceRequest.hasStatus()) {
      prescriptionData.setStatus(serviceRequest.getStatus().toCode());
    }

    if(serviceRequest.hasPriority()) {
      prescriptionData.setPriority(serviceRequest.getPriority().toCode());
    }
    
    prescriptionData.setPrenatal(false);
    prescriptionData.setPanelCode(null);
    prescriptionData.setRequester(null);
    
    if(serviceRequest.hasPerformer()) {
      prescriptionData.setLdm(serviceRequest.getPerformer().get(0).getReference());
    }

    final Reference subjectRef = serviceRequest.getSubject();
    final Patient patient = this.configuration.patientDAO.read(new IdType(subjectRef.getReference()));

    final Reference organizationRef = patient.getManagingOrganization();
    final Organization organization = configuration.organizationDAO.read(new IdType(organizationRef.getReference()));
    
    if(organization.hasAlias()) {
      prescriptionData.setEp(organization.getAlias().get(0).getId());
    }
    
    if(serviceRequest.hasAuthoredOn()) {
      prescriptionData.setCreatedOn(formatter.get().format(serviceRequest.getAuthoredOn()));
    }
    
  }
}
