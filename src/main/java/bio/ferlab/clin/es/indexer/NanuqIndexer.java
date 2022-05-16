package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.builder.nanuq.AnalysisDataBuilder;
import bio.ferlab.clin.es.builder.nanuq.SequencingDataBuilder;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import bio.ferlab.clin.es.extractor.ServiceRequestIdExtractor;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NanuqIndexer extends Indexer {
  
  private final ServiceRequestIdExtractor serviceRequestIdExtractor;
  private final AnalysisDataBuilder analysisDataBuilder;
  private final SequencingDataBuilder sequencingDataBuilder;
  private final BioProperties bioProperties;
  private final IndexerTools tools;
  
  public NanuqIndexer(ServiceRequestIdExtractor serviceRequestIdExtractor,
                      AnalysisDataBuilder analysisDataBuilder,
                      SequencingDataBuilder sequencingDataBuilder,
                      BioProperties bioProperties,
                      IndexerTools tools) {
    this.serviceRequestIdExtractor = serviceRequestIdExtractor;
    this.analysisDataBuilder = analysisDataBuilder;
    this.sequencingDataBuilder = sequencingDataBuilder;
    this.bioProperties = bioProperties;
    this.tools = tools;
  }
  
  @Override
  protected void doIndex(RequestDetails requestDetails, IBaseResource resource) {
    final Set<String> prescriptionIds = serviceRequestIdExtractor.extract(resource);
    List<AnalysisData> analyses = this.indexAnalyses(requestDetails, prescriptionIds);
    List<SequencingData> sequencings = this.indexSequencings(requestDetails, prescriptionIds);
    
    // re-index parent analyses
    final Set<String> linkedParentAnalyses = sequencings.stream().map(SequencingData::getPrescriptionId).collect(Collectors.toSet());
    final Set<String> alreadyIndexedAnalyses = analyses.stream().map(AnalysisData::getRequestId).collect(Collectors.toSet());
    linkedParentAnalyses.removeAll(alreadyIndexedAnalyses); // ignore if indexed before
    this.indexAnalyses(requestDetails, linkedParentAnalyses);
  }

  private List<AnalysisData> indexAnalyses(RequestDetails requestDetails, Set<String> ids) {
    final List<AnalysisData> analyses = analysisDataBuilder.fromIds(ids, requestDetails);
    analyses.forEach(e -> indexToEs(e.getRequestId(), e, bioProperties.getNanuqEsAnalysesIndex()));
    return analyses;
  }

  private List<SequencingData> indexSequencings(RequestDetails requestDetails, Set<String> ids) {
    final List<SequencingData> sequencings = sequencingDataBuilder.fromIds(ids, requestDetails);
    sequencings.forEach(e -> indexToEs(e.getRequestId(), e, bioProperties.getNanuqEsSequencingsIndex()));
    return sequencings;
  }
  
  private void indexToEs(String id, Object document, String indexName) {
    final ElasticsearchRestClient.IndexData data = new ElasticsearchRestClient.IndexData(id, tools.jsonGenerator.toString(document));
    tools.client.index(indexName, data);
  }
  
}
