package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import bio.ferlab.clin.utils.FhirUtils;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.ferlab.clin.utils.Extensions.FAMILY_MEMBER_FATHER_CODE;
import static bio.ferlab.clin.utils.Extensions.FAMILY_MEMBER_MOTHER_CODE;

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
        }
        
        if(serviceRequest.hasSpecimen()) {
          sequencingData.setSample(getSampleValue(serviceRequest, requestDetails));
        }

        this.addTasks(serviceRequest, sequencingData);

        sequencings.add(sequencingData);
      }
    }
    return sequencings;
  }

}
