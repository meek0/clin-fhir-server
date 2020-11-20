package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.HashMap;

public class ElasticsearchRestClient {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchRestClient.class);
    public static final String FAILED_TO_SAVE_RESOURCE = "Failed to save resource";
    public static final String FAILED_TO_DELETE_RESOURCE = "Failed to delete resource";
    private final ElasticsearchData data;

    public ElasticsearchRestClient(ElasticsearchData data) {
        this.data = data;
    }

    public void index(String index, IndexData data) {
        final String id = data.id;
        logger.info(String.format("Indexing resource id[%s]", id));
        try {
            this.data.client.performRequest(
                    HttpMethod.PUT.name(),
                    String.format("/%s/_doc/%s", index, id),
                    new HashMap<>(),
                    new NStringEntity(data.jsonContent, ContentType.APPLICATION_JSON)
            );
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
            throw new ca.uhn.fhir.rest.server.exceptions.InternalErrorException(FAILED_TO_SAVE_RESOURCE);
        }
    }

    public void delete(String index, String id) {
        logger.info(String.format("Deleting resource id[%s]", id));
        try {
            this.data.client.performRequest(
                    HttpMethod.DELETE.name(),
                    String.format("/%s/_doc/%s", index, id),
                    new HashMap<>()
            );
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
            throw new ca.uhn.fhir.rest.server.exceptions.InternalErrorException(FAILED_TO_DELETE_RESOURCE);
        }
    }

    public static class IndexData {
        public final String id;
        public final String jsonContent;

        public IndexData(String id, String jsonContent) {
            this.id = id;
            this.jsonContent = jsonContent;
        }
    }
}
