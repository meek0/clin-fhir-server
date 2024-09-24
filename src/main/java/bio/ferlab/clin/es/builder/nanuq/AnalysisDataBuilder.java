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
import java.util.Set;

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
        if (serviceRequest.hasPerformer()) {
          analysisData.setAssignments(FhirUtils.getPerformerIds(serviceRequest, PractitionerRole.class));
        }

        final SearchParameterMap searchMap = SearchParameterMap.newSynchronous("based-on", new ReferenceParam(serviceRequestId));
        final IBundleProvider srProvider = this.configuration.serviceRequestDAO.search(searchMap);
        final List<ServiceRequest> serviceRequests = this.getListFromProvider(srProvider);
        for (ServiceRequest sr : serviceRequests) {
          SequencingRequestData srd = new SequencingRequestData();
          srd.setRequestId(sr.getIdElement().getIdPart());
          srd.setPrescriptionId(serviceRequest.getIdElement().getIdPart());

          Set<String> taskRunNames = this.addTasks(sr, analysisData);

          if (taskRunNames.size() > 0) {
            srd.setTaskRunname(taskRunNames.iterator().next());
          }

          if(sr.hasStatus()) {
            srd.setStatus(sr.getStatus().toCode());
          }
          if(sr.hasSpecimen()) {
            srd.setSample(getSampleValue(sr, requestDetails));
          }
          if (sr.hasSubject()) {
            final Reference subjectRef = sr.getSubject();
            final Patient patient = this.configuration.patientDAO.read(new IdType(subjectRef.getReference()));
            if (patient.hasIdentifier()) {
              extractMRN(patient).ifPresent(srd::setPatientMRN);
            }
            srd.setPatientId(sr.getSubject().getReferenceElement().getIdPart());
          }

          this.addRunInfo(serviceRequest, sr, srd);
          analysisData.getSequencingRequests().add(srd);
        }
        analyses.add(analysisData);
      }
    }
    return analyses;
  }

}
