package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.dao.DaoConfiguration;
import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.builder.nanuq.AnalysisDataBuilder;
import bio.ferlab.clin.es.builder.nanuq.SequencingDataBuilder;
import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.es.data.PrescriptionData;
import bio.ferlab.clin.es.data.nanuq.AnalysisData;
import bio.ferlab.clin.es.data.nanuq.SequencingData;
import bio.ferlab.clin.es.extractor.ServiceRequestIdExtractor;
import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static bio.ferlab.clin.es.TemplateIndexer.ANALYSES_TEMPLATE;
import static bio.ferlab.clin.es.TemplateIndexer.SEQUENCINGS_TEMPLATE;

@Component
@RequiredArgsConstructor
public class NanuqIndexer extends Indexer {
  
  private final ServiceRequestIdExtractor serviceRequestIdExtractor;
  private final ResourceDaoConfiguration configuration;
  private final AnalysisDataBuilder analysisDataBuilder;
  private final SequencingDataBuilder sequencingDataBuilder;
  private final BioProperties bioProperties;
  private final IndexerTools tools;

  public void migrate(String analysesIndex, String sequencingIndex) {
   final IBundleProvider bundle = this.configuration.serviceRequestDAO.search(SearchParameterMap.newSynchronous().setCount(Integer.MAX_VALUE));
   final Set<String> prescriptionIds = bundle.getAllResources().stream().map(r -> r.getIdElement().getIdPart()).collect(Collectors.toSet());
   this.doIndex(prescriptionIds, analysesIndex, sequencingIndex);
  }

  @Override
  protected void doIndex(RequestDetails requestDetails, IBaseResource resource) {
    final Set<String> prescriptionIds = serviceRequestIdExtractor.extract(resource);
    this.doIndex(prescriptionIds, bioProperties.getNanuqEsAnalysesIndex(), bioProperties.getNanuqEsSequencingsIndex());
  }

  private void doIndex(Set<String> prescriptionIds, String analysesIndex, String sequencingIndex) {
    List<AnalysisData> analyses = this.indexAnalyses(null, prescriptionIds, analysesIndex);
    List<SequencingData> sequencings = this.indexSequencings(null, prescriptionIds, sequencingIndex);

    // re-index parent analyses
    final Set<String> linkedParentAnalyses = sequencings.stream().map(SequencingData::getPrescriptionId).collect(Collectors.toSet());
    final Set<String> alreadyIndexedAnalyses = analyses.stream().map(AnalysisData::getPrescriptionId).collect(Collectors.toSet());
    linkedParentAnalyses.removeAll(alreadyIndexedAnalyses); // ignore if indexed before
    this.indexAnalyses(null, linkedParentAnalyses, analysesIndex);
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
