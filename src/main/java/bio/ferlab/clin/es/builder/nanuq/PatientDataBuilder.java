package bio.ferlab.clin.es.builder.nanuq;

import bio.ferlab.clin.es.builder.CommonDataBuilder;
import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PatientData;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component("NanuqPatientDataBuilder")
public class PatientDataBuilder {

  private static final Logger log = LoggerFactory.getLogger(PatientDataBuilder.class);

  private final ResourceDaoConfiguration configuration;
  private final CommonDataBuilder commonDataBuilder;

  public PatientDataBuilder(ResourceDaoConfiguration configuration,
                            CommonDataBuilder commonDataBuilder) {
    this.configuration = configuration;
    this.commonDataBuilder = commonDataBuilder;
  }

  public List<PatientData> fromIds(Set<String> ids, RequestDetails requestDetails) {
    final List<PatientData> patientDataList = new ArrayList<>();
    for (final String patientId : ids) {
      final PatientData patientData = new PatientData();
      final Patient patient = this.configuration.patientDAO.read(new IdType(patientId), requestDetails);
      
      patientDataList.add(patientData);
    }
    return patientDataList;
  }
  
}
