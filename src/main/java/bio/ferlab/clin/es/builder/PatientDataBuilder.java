package bio.ferlab.clin.es.builder;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.utils.Extensions;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PatientDataBuilder {

    private final ResourceDaoConfiguration configuration;
    private final CommonDataBuilder commonDataBuilder;

    public PatientDataBuilder(ResourceDaoConfiguration configuration, CommonDataBuilder commonDataBuilder) {
        this.configuration = configuration;
        this.commonDataBuilder = commonDataBuilder;
    }

    public List<PatientData> fromIds(Set<String> ids, RequestDetails requestDetails) {
        final List<PatientData> patientDataList = new ArrayList<>();
        for (final String patientId : ids) {
            final PatientData patientData = new PatientData();
            final Patient patient = this.configuration.patientDAO.read(new IdType(patientId), requestDetails);
            
            this.commonDataBuilder.handlePatient(patient, patientData);

            final SearchParameterMap searchMap = SearchParameterMap.newSynchronous("subject", new ReferenceParam(patientId));
            final IBundleProvider srProvider = this.configuration.serviceRequestDAO.search(searchMap);

            final List<ServiceRequest> serviceRequests = this.commonDataBuilder.getListFromProvider(srProvider);
            for (ServiceRequest serviceRequest : serviceRequests) {
                PrescriptionData prescriptionData = new PrescriptionData();
                this.commonDataBuilder.handleServiceRequest(serviceRequest, prescriptionData);
                this.commonDataBuilder.finalize(prescriptionData);
                patientData.getRequests().add(prescriptionData);
            }
            
            this.commonDataBuilder.finalize(patientData);
            patientDataList.add(patientData);
        }
        return patientDataList;
    }
    

    
}
