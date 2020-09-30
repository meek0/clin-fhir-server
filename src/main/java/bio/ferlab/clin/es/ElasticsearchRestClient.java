package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import bio.ferlab.clin.es.data.IndexResponseListener;
import ca.uhn.fhir.context.FhirContext;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.RestClient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ElasticsearchRestClient {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchRestClient.class);
    private final ElasticsearchData data;

    public ElasticsearchRestClient(ElasticsearchData data) {
        this.data = data;
    }

    public <T extends Resource> void index(String index, T resource) {
        final RestClient client = this.data.client;
        final String content = FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
        final HttpEntity entity = new NStringEntity(content, ContentType.APPLICATION_JSON);
        final String id = resource.getIdElement().getIdPart();
        client.performRequestAsync("PUT",
                String.format("/%s/_doc/%s", index, id),
                new HashMap<>(),
                entity,
                new IndexResponseListener(index)
        );
    }

    public <T extends Resource> void delete(String index, T resource) {
        final RestClient client = this.data.client;
        final String content = FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
        final HttpEntity entity = new NStringEntity(content, ContentType.APPLICATION_JSON);
        final String id = resource.getIdElement().getIdPart();
        client.performRequestAsync("DELETE",
                String.format("/%s/_doc/%s", index, id),
                new HashMap<>(),
                entity,
                new IndexResponseListener(index)
        );
    }
}
