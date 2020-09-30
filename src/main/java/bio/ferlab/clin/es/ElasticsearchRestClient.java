package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import bio.ferlab.clin.es.data.UpdateResponseListener;
import ca.uhn.fhir.context.FhirContext;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.HashMap;

public class ElasticsearchRestClient {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchRestClient.class);
    private final ElasticsearchData data;

    public ElasticsearchRestClient(ElasticsearchData data) {
        this.data = data;
    }

    public <T extends Resource> void index(T resource) {
        final String content = FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
        final String id = resource.getIdElement().getIdPart();
        logger.info(String.format("Indexing resource id[%s]", id));
        this.data.client.performRequestAsync(
                HttpMethod.PUT.name(),
                String.format("/%s/_doc/%s", this.data.index, id),
                new HashMap<>(),
                new NStringEntity(content, ContentType.APPLICATION_JSON),
                new UpdateResponseListener(this.data.index)
        );
    }

    public <T extends Resource> void delete(T resource) {
        final String id = resource.getIdElement().getIdPart();
        logger.info(String.format("Deleting resource id[%s]", id));
        this.data.client.performRequestAsync(
                HttpMethod.DELETE.name(),
                String.format("/%s/_doc/%s", this.data.index, id),
                new HashMap<>(),
                new UpdateResponseListener(this.data.index)
        );
    }
}
