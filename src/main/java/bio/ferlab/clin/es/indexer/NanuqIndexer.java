package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.dao.DaoConfiguration;
import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.builder.nanuq.AnalysisDataBuilder;
import bio.ferlab.clin.es.builder.nanuq.SequencingDataBuilder;
import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import bio.ferlab.clin.es.data.nanuq.SequencingRequestData;
import bio.ferlab.clin.es.extractor.ServiceRequestIdExtractor;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static bio.ferlab.clin.es.TemplateIndexer.ANALYSES_TEMPLATE;
import static bio.ferlab.clin.es.TemplateIndexer.SEQUENCINGS_TEMPLATE;

@Component
@RequiredArgsConstructor
public class NanuqIndexer extends Indexer {
  
  private final ServiceRequestIdExtractor serviceRequestIdExtractor;
  private final AnalysisDataBuilder analysisDataBuilder;
  private final SequencingDataBuilder sequencingDataBuilder;
  private final BioProperties bioProperties;
  private final IndexerTools tools;

  @Override
  protected void doIndex(RequestDetails requestDetails, IBaseResource resource) {
    final Set<String> prescriptionIds = serviceRequestIdExtractor.extract(resource);
    this.doIndex(requestDetails, prescriptionIds, bioProperties.getNanuqEsAnalysesIndex(), bioProperties.getNanuqEsSequencingsIndex(), true);
  }

  public void doIndex(RequestDetails requestDetails, Set<String> prescriptionIds, String analysesIndex, String sequencingIndex, boolean indexLinked) {
    List<AnalysisData> analyses = this.indexAnalyses(requestDetails, prescriptionIds, analysesIndex);
    List<SequencingData> sequencings = this.indexSequencings(requestDetails, prescriptionIds, sequencingIndex);

    if (indexLinked) {
      // re-index linked analyses
      final Set<String> linkedAnalyses = sequencings.stream().map(SequencingData::getPrescriptionId).collect(Collectors.toSet());
      final Set<String> alreadyIndexedAnalyses = analyses.stream().map(AnalysisData::getPrescriptionId).collect(Collectors.toSet());
      linkedAnalyses.removeAll(alreadyIndexedAnalyses); // ignore if indexed before
      this.indexAnalyses(requestDetails, linkedAnalyses, analysesIndex);

      // re-index linked sequencings
      final Set<String> linkedSequencings = analyses.stream().flatMap(a -> a.getSequencingRequests().stream().map(SequencingRequestData::getRequestId)).collect(Collectors.toSet());
      final Set<String> alreadyIndexedSequencings = sequencings.stream().map(SequencingData::getRequestId).collect(Collectors.toSet());
      linkedSequencings.removeAll(alreadyIndexedSequencings); // ignore if indexed before
      this.indexSequencings(requestDetails, linkedSequencings, sequencingIndex);
    }
  }

  private List<AnalysisData> indexAnalyses(RequestDetails requestDetails, Set<String> ids, String index) {
    final List<AnalysisData> analyses = analysisDataBuilder.fromIds(ids, requestDetails);
    analyses.forEach(e -> indexToEs(e.getPrescriptionId(), e, index));
    return analyses;
  }

  private List<SequencingData> indexSequencings(RequestDetails requestDetails, Set<String> ids, String index) {
    final List<SequencingData> sequencings = sequencingDataBuilder.fromIds(ids, requestDetails);
    sequencings.forEach(e -> indexToEs(e.getRequestId(), e, index));
    return sequencings;
  }
  
  private void indexToEs(String id, Object document, String indexName) {
    final ElasticsearchRestClient.IndexData data = new ElasticsearchRestClient.IndexData(id, tools.jsonGenerator.toString(document));
    tools.client.index(indexName, data);
  }
  
}
