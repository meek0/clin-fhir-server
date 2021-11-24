package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.builder.PatientDataBuilder;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.extractor.PatientIdExtractor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PatientIndexer extends Indexer {
    private final PatientIdExtractor patientIdExtractor;
    private final PatientDataBuilder patientDataBuilder;
    private final IndexerTools tools;


    public PatientIndexer(PatientIdExtractor patientIdExtractor, PatientDataBuilder patientDataBuilder, IndexerTools tools) {
        this.patientIdExtractor = patientIdExtractor;
        this.patientDataBuilder = patientDataBuilder;
        this.tools = tools;
    }

    @Override
    protected void doIndex(RequestDetails requestDetails, IBaseResource resource) {
        final Set<String> ids = this.patientIdExtractor.extract(resource);
        if (ids != null) {
            final List<PatientData> patientDataList = this.patientDataBuilder.fromIds(ids, requestDetails);
            patientDataList.forEach(System.out::println);

            patientDataList.forEach(this::indexToEs);
        }
    }

    private void indexToEs(PatientData patientData) {
        final ElasticsearchRestClient.IndexData data = new ElasticsearchRestClient.IndexData(
                patientData.getCid(),
                tools.jsonGenerator.toString(patientData)
        );
        tools.client.index(tools.bioProperties.getEsPatientsIndex(), data);
    }
}
