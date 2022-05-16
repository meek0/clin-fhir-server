package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AnalysisDataBuilder extends AbstractPrescriptionDataBuilder {

  private final ResourceDaoConfiguration configuration;

  public AnalysisDataBuilder(ResourceDaoConfiguration configuration) {
    super(RequestType.ANALYSIS, configuration);
    this.configuration = configuration;
  }

  public List<AnalysisData> fromIds(Set<String> ids, RequestDetails requestDetails) {
    final List<AnalysisData> analyses = new ArrayList<>();
    for (final String serviceRequestId : ids) {
      final AnalysisData analysisData = new AnalysisData();
      final ServiceRequest serviceRequest = this.configuration.serviceRequestDAO.read(new IdType(serviceRequestId), requestDetails);
      if (this.isValidType(serviceRequest)) {
        
        this.handlePrescription(serviceRequest, analysisData);
        
        analyses.add(analysisData);
      }
    }
    return analyses;
  }

}
