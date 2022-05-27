package bio.ferlab.clin.es;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class TemplateIndexer {
  
  private final ElasticsearchRestClient esClient;

  private static final Logger log = LoggerFactory.getLogger(TemplateIndexer.class);
  
  public TemplateIndexer(ElasticsearchRestClient esClient) {
    this.esClient = esClient;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void indextemplates() {
    indexTemplate("clin-analyses-template.json");
    indexTemplate("clin-sequencings-template.json");
  }
  
  private void indexTemplate(String path) {
    log.info("Index ES template from resource: {}", path);
    String templateName = FilenameUtils.getBaseName(path);
    esClient.indexTemplate(templateName, loadTemplate(path));
  }
  
  private String loadTemplate(String path) {
    try(InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
      return IOUtils.toString(is, StandardCharsets.UTF_8.name());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load template from resources: " + path, e);
    }
  }
  
}
