package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.BioProperties;
import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.utils.JsonGenerator;
import org.springframework.stereotype.Component;

@Component
public class IndexerTools {
    public final ElasticsearchRestClient client;
    public final BioProperties bioProperties;
    public final JsonGenerator jsonGenerator;

    public IndexerTools(ElasticsearchRestClient client, BioProperties bioProperties, JsonGenerator jsonGenerator) {
        this.client = client;
        this.bioProperties = bioProperties;
        this.jsonGenerator = jsonGenerator;
    }
}
