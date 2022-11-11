package bio.ferlab.clin.es;

import bio.ferlab.clin.es.indexer.NanuqIndexer;
import bio.ferlab.clin.properties.BioProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static bio.ferlab.clin.es.TemplateIndexer.ANALYSES_TEMPLATE;
import static bio.ferlab.clin.es.TemplateIndexer.SEQUENCINGS_TEMPLATE;

@Component
@RequiredArgsConstructor
public class MigrationManager {

  private static final Logger log = LoggerFactory.getLogger(MigrationManager.class);

  private final TemplateIndexer templateIndexer;
  private final BioProperties bioProperties;
  private final ElasticsearchRestClient esClient;
  private final NanuqIndexer nanuqIndexer;

  @EventListener(ApplicationReadyEvent.class)
  public void startMigration() {
    Map<String, String> templates = this.templateIndexer.indexTemplates();
    Map<String, String> aliases = esClient.aliases();

    // indexes that will be used as aliases at the end of the process
    final String analysesIndex = bioProperties.getNanuqEsAnalysesIndex();
    final String sequencingsIndex = bioProperties.getNanuqEsSequencingsIndex();

    // indexes with templates hash from this version of FHIR
    final String analysesIndexWithHash = formatIndexWithHash(analysesIndex, templates.get(ANALYSES_TEMPLATE));
    final String sequencingIndexWithHash = formatIndexWithHash(sequencingsIndex, templates.get(SEQUENCINGS_TEMPLATE));

    // indexes with templates hash known by elastic-search
    final String currentESAnalysesIndexWithHash = aliases.get(analysesIndex);
    final String currentESSequencingIndexWithHash = aliases.get(sequencingsIndex);

    // compare current template hash with ES
    final boolean analysesHasChanged = !analysesIndexWithHash.equals(currentESAnalysesIndexWithHash);
    final boolean sequencingsHasChanged = !sequencingIndexWithHash.equals(currentESSequencingIndexWithHash);

    // Perform migration if any of them is different
    if (analysesHasChanged || sequencingsHasChanged) {
      log.info("Migrate: {} {}", analysesIndexWithHash, sequencingIndexWithHash);
      this.nanuqIndexer.migrate(analysesIndexWithHash, sequencingIndexWithHash);

      // remove the add the aliases referring the new indexes + hash
      this.publish(analysesIndexWithHash, currentESAnalysesIndexWithHash, analysesIndex);
      this.publish(sequencingIndexWithHash, currentESSequencingIndexWithHash, sequencingsIndex);

      // remove previous indexes
      this.cleanup(analysesIndex, sequencingsIndex, analysesHasChanged, currentESAnalysesIndexWithHash, sequencingsHasChanged, currentESSequencingIndexWithHash);
    } else {
      log.info("Nothing to migrate");
    }
  }

  private void cleanup(String analysesIndex, String sequencingsIndex, boolean analysesHasChanged, String currentESAnalysesIndexWithHash, boolean sequencingsHasChanged, String currentESSequencingIndexWithHash) {
    log.info("Cleanup ES indexes ...");
    final List<String> indexesToCleanup = new ArrayList<>();
    indexesToCleanup.add(analysesIndex);
    indexesToCleanup.add(sequencingsIndex);
    if (analysesHasChanged) {  // ignore if was the same
      indexesToCleanup.add(currentESAnalysesIndexWithHash);
    }
    if (sequencingsHasChanged) { // ignore if was the same
      indexesToCleanup.add(currentESSequencingIndexWithHash);
    }
    this.esClient.delete(indexesToCleanup.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }

  private void publish(String indexWithHash, String currentESIndexWithHash, String index) {
    List<String> aliasesToRemove = new ArrayList<>();
    log.info("Publish: {} <=> {}", index, indexWithHash);
    Optional.ofNullable(currentESIndexWithHash).ifPresent(aliasesToRemove::add);
    this.esClient.setAlias(List.of(indexWithHash), aliasesToRemove, index);
  }

  private String formatIndexWithHash(String index, String templateHash) {
    return String.format("%s-%s", index, templateHash);
  }

}
