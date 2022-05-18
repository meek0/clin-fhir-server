package bio.ferlab.clin.es.builder;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PrescriptionData;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PrescriptionDataBuilder {
    private static final Logger log = LoggerFactory.getLogger(PrescriptionDataBuilder.class);

    private final ResourceDaoConfiguration configuration;
    private final CommonDataBuilder commonDataBuilder;

    public PrescriptionDataBuilder(ResourceDaoConfiguration configuration, CommonDataBuilder commonDataBuilder) {
        this.configuration = configuration;
        this.commonDataBuilder = commonDataBuilder;
    }

    public List<PrescriptionData> fromIds(Set<String> ids, RequestDetails requestDetails) {
        final List<PrescriptionData> prescriptionDataList = new ArrayList<>();
        for (final String serviceRequestId : ids) {
            final PrescriptionData prescriptionData = new PrescriptionData();
            final ServiceRequest serviceRequest = this.configuration.serviceRequestDAO.read(new IdType(serviceRequestId), requestDetails);
            if (!serviceRequest.hasSubject()) {
                log.error(String.format("Cannot index ServiceRequest [%s]: resource has no subject.", serviceRequestId));
                continue;
            }
            final Reference subject = serviceRequest.getSubject();
            final Patient patient = this.configuration.patientDAO.read(new IdType(subject.getReference()), requestDetails);

            this.commonDataBuilder.handlePatient(patient, prescriptionData.getPatientInfo());
            this.commonDataBuilder.finalize(prescriptionData.getPatientInfo());
            
            this.commonDataBuilder.handleServiceRequest(serviceRequest, prescriptionData);
            this.commonDataBuilder.finalize(prescriptionData);
            
            prescriptionDataList.add(prescriptionData);
        }
        return prescriptionDataList;
    }

}
