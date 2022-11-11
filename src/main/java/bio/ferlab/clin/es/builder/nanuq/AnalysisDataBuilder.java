package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import bio.ferlab.clin.es.data.nanuq.SequencingRequestData;
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

import static bio.ferlab.clin.utils.Extensions.*;

@Component
public class AnalysisDataBuilder extends AbstractPrescriptionDataBuilder {

  private final ResourceDaoConfiguration configuration;

  public AnalysisDataBuilder(ResourceDaoConfiguration configuration) {
    super(Type.ANALYSIS, configuration);
    this.configuration = configuration;
  }

  public List<AnalysisData> fromIds(Set<String> ids, RequestDetails requestDetails) {
    final List<AnalysisData> analyses = new ArrayList<>();
    for (final String serviceRequestId : ids) {
      final AnalysisData analysisData = new AnalysisData();
      final ServiceRequest serviceRequest = this.configuration.serviceRequestDAO.read(new IdType(serviceRequestId), requestDetails);
      if (this.isValidType(serviceRequest)) {
        
        this.handlePrescription(serviceRequest, analysisData);
        analysisData.setPrescriptionId(serviceRequest.getIdElement().getIdPart());

        final SearchParameterMap searchMap = SearchParameterMap.newSynchronous("based-on", new ReferenceParam(serviceRequestId));
        final IBundleProvider srProvider = this.configuration.serviceRequestDAO.search(searchMap);
        final List<ServiceRequest> serviceRequests = this.getListFromProvider(srProvider);
        for (ServiceRequest sr : serviceRequests) {
          SequencingRequestData srd = new SequencingRequestData();
          srd.setRequestId(sr.getIdElement().getIdPart());
          if(sr.hasStatus()) {
            srd.setStatus(sr.getStatus().toCode());
          }
          if(sr.hasSpecimen()) {
            srd.setSample(getSampleValue(sr, requestDetails));
          }
          analysisData.getSequencingRequests().add(srd);
        }
        
        final Reference motherRef = extractParentReference(serviceRequest, FAMILY_MEMBER_MOTHER_CODE);
        Optional.ofNullable(motherRef).map(FhirUtils::extractId).ifPresent(analysisData::setMotherId);
        
        final Reference fatherRef = extractParentReference(serviceRequest, FAMILY_MEMBER_FATHER_CODE);
        Optional.ofNullable(fatherRef).map(FhirUtils::extractId).ifPresent(analysisData::setFatherId);
        
        analyses.add(analysisData);
      }
    }
    return analyses;
  }

}
