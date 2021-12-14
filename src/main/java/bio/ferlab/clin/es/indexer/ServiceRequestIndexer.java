package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.builder.PrescriptionDataBuilder;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.es.extractor.ServiceRequestIdExtractor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ServiceRequestIndexer extends Indexer {
    private final ServiceRequestIdExtractor serviceRequestIdExtractor;
    private final PrescriptionDataBuilder prescriptionDataBuilder;
    private final IndexerTools tools;
    private final PatientIndexer patientIndexer;

    public ServiceRequestIndexer(ServiceRequestIdExtractor serviceRequestIdExtractor, 
                                 PrescriptionDataBuilder prescriptionDataBuilder, 
                                 IndexerTools tools,
                                 PatientIndexer patientIndexer) {
        this.tools = tools;
        this.serviceRequestIdExtractor = serviceRequestIdExtractor;
        this.prescriptionDataBuilder = prescriptionDataBuilder;
        this.patientIndexer = patientIndexer;
    }

    @Override
    protected void doIndex(RequestDetails requestDetails, IBaseResource resource) {
        final Set<String> ids = this.serviceRequestIdExtractor.extract(resource);
        if (ids != null && !ids.isEmpty()) {
            final List<PrescriptionData> prescriptionDataList = this.prescriptionDataBuilder.fromIds(ids, requestDetails);
            prescriptionDataList.forEach(this::indexToEs);
            // re-index the patients linked to these prescriptions
            final Set<String> patientIds = prescriptionDataList.stream().map(p -> p.getPatientInfo().getCid())
                .collect(Collectors.toSet());
            this.patientIndexer.doIndex(patientIds, requestDetails);
        }
    }

    private void indexToEs(PrescriptionData prescriptionData) {
        final ElasticsearchRestClient.IndexData data = new ElasticsearchRestClient.IndexData(
                prescriptionData.getCid(),
                tools.jsonGenerator.toString(prescriptionData)
        );
        tools.client.index(tools.bioProperties.getEsPrescriptionsIndex(), data);
    }
}
