package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.builder.PatientDataBuilder;
import bio.ferlab.clin.es.builder.PrescriptionDataBuilder;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.es.extractor.PatientIdExtractor;
import bio.ferlab.clin.es.extractor.ServiceRequestIdExtractor;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LegacyIndexer extends Indexer {
  
  private final ServiceRequestIdExtractor serviceRequestIdExtractor;
  private final PrescriptionDataBuilder prescriptionDataBuilder;
  private final PatientIdExtractor patientIdExtractor;
  private final PatientDataBuilder patientDataBuilder;
  private final BioProperties bioProperties;
  private final IndexerTools tools;
  
  public LegacyIndexer(ServiceRequestIdExtractor serviceRequestIdExtractor,
                       PrescriptionDataBuilder prescriptionDataBuilder,
                       PatientIdExtractor patientIdExtractor,
                       PatientDataBuilder patientDataBuilder,
                       BioProperties bioProperties,
                       IndexerTools tools) {
    this.serviceRequestIdExtractor = serviceRequestIdExtractor;
    this.prescriptionDataBuilder = prescriptionDataBuilder;
    this.patientIdExtractor = patientIdExtractor;
    this.patientDataBuilder = patientDataBuilder;
    this.bioProperties = bioProperties;
    this.tools = tools;
  }
  
  @Override
  protected void doIndex(RequestDetails requestDetails, IBaseResource resource) {
    final Set<String> patientIds = patientIdExtractor.extract(resource);
    List<PatientData> patients = this.indexPatients(requestDetails, patientIds);
    
    final Set<String> prescriptionIds = serviceRequestIdExtractor.extract(resource);
    List<PrescriptionData> prescriptions = this.indexPrescriptions(requestDetails, prescriptionIds);

    // re-index linked distinct prescriptions
    final Set<String> linkedPrescriptions = patients.stream().flatMap(p -> p.getRequests().stream().map(PrescriptionData::getCid)).collect(Collectors.toSet());
    linkedPrescriptions.removeAll(prescriptionIds); // ignore if indexed before
    this.indexPrescriptions(requestDetails, linkedPrescriptions);

    // re-index linked distinct patients
    final Set<String> linkedPatients = prescriptions.stream().map(p -> p.getPatientInfo().getCid()).collect(Collectors.toSet());
    linkedPatients.removeAll(patientIds); // ignore if indexed before
    this.indexPatients(requestDetails, linkedPatients);
  }
  
  private List<PatientData> indexPatients(RequestDetails requestDetails, Set<String> ids) {
    final List<PatientData> patients = patientDataBuilder.fromIds(ids, requestDetails);
    patients.forEach(e -> indexToEs(e.getCid(), e, bioProperties.getEsPatientsIndex()));
    return patients;
  }

  private List<PrescriptionData> indexPrescriptions(RequestDetails requestDetails, Set<String> ids) {
    final List<PrescriptionData> prescriptions = prescriptionDataBuilder.fromIds(ids, requestDetails);
    prescriptions.forEach(e -> indexToEs(e.getCid(), e, bioProperties.getEsPrescriptionsIndex()));
    return prescriptions;
  }
  
  private void indexToEs(String id, Object document, String indexName) {
    final ElasticsearchRestClient.IndexData data = new ElasticsearchRestClient.IndexData(id, tools.jsonGenerator.toString(document));
    tools.client.index(indexName, data);
  }
  
}
