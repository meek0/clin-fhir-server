package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.builder.CommonDataBuilder;
import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import bio.ferlab.clin.es.data.nanuq.SequencingRequestData;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AnalysisDataBuilder extends AbstractPrescriptionDataBuilder {

  private final ResourceDaoConfiguration configuration;
  private final CommonDataBuilder commonDataBuilder;

  public AnalysisDataBuilder(ResourceDaoConfiguration configuration, CommonDataBuilder commonDataBuilder) {
    super(Type.ANALYSIS, configuration);
    this.configuration = configuration;
    this.commonDataBuilder = commonDataBuilder;
  }

  public List<AnalysisData> fromIds(Set<String> ids, RequestDetails requestDetails) {
    final List<AnalysisData> analyses = new ArrayList<>();
    for (final String serviceRequestId : ids) {
      final AnalysisData analysisData = new AnalysisData();
      final ServiceRequest serviceRequest = this.configuration.serviceRequestDAO.read(new IdType(serviceRequestId), requestDetails);
      if (this.isValidType(serviceRequest)) {
        
        this.handlePrescription(serviceRequest, analysisData);

        final SearchParameterMap searchMap = SearchParameterMap.newSynchronous("based-on", new ReferenceParam(serviceRequestId));
        final IBundleProvider srProvider = this.configuration.serviceRequestDAO.search(searchMap);
        final List<ServiceRequest> serviceRequests = this.commonDataBuilder.getListFromProvider(srProvider);
        for (ServiceRequest sr : serviceRequests) {
          SequencingRequestData srd = new SequencingRequestData();
          srd.setRequestId(sr.getIdElement().getIdPart());
          if(sr.hasStatus()) {
            srd.setStatus(sr.getStatus().toCode());
          }
          analysisData.getSequencingRequest().add(srd);
        }
                
        analyses.add(analysisData);
      }
    }
    return analyses;
  }

}
