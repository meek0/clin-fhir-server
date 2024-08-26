package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SequencingDataBuilder extends AbstractPrescriptionDataBuilder {

  private final ResourceDaoConfiguration configuration;

  public SequencingDataBuilder(ResourceDaoConfiguration configuration) {
    super(Type.SEQUENCING, configuration);
    this.configuration = configuration;
  }

  public List<SequencingData> fromIds(Set<String> ids, RequestDetails requestDetails) {
    final List<SequencingData> sequencings = new ArrayList<>();
    for (final String serviceRequestId : ids) {
      final SequencingData sequencingData = new SequencingData();
      final ServiceRequest serviceRequest = this.configuration.serviceRequestDAO.read(new IdType(serviceRequestId), requestDetails);
      if (this.isValidType(serviceRequest)) {

        this.handlePrescription(serviceRequest, sequencingData);
        sequencingData.setRequestId(serviceRequest.getIdElement().getIdPart());

        if(serviceRequest.hasBasedOn()) {

          final String basedOnId = serviceRequest.getBasedOn().get(0).getReferenceElement().getIdPart();
          sequencingData.setPrescriptionId(basedOnId);

          final ServiceRequest basedOn = this.configuration.serviceRequestDAO.read(new IdType(basedOnId), requestDetails);
          if(basedOn.hasStatus()) {
            sequencingData.setPrescriptionStatus(basedOn.getStatus().toCode());
          }

          this.addRunInfo(basedOn, serviceRequest, sequencingData);
        }

        if(serviceRequest.hasSpecimen()) {
          sequencingData.setSample(getSampleValue(serviceRequest, requestDetails));
        }

        Set<String> taskRunNames = this.addTasks(serviceRequest, sequencingData);
        if (taskRunNames.size() > 0) {
          sequencingData.setTaskRunname(taskRunNames.iterator().next());
        }

        sequencings.add(sequencingData);
      }
    }
    return sequencings;
  }

}
