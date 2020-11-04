package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import bio.ferlab.clin.es.data.UpdateResponseListener;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
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

    public void index(String index, IndexData data) {
        final String id = data.id;
        logger.info(String.format("Indexing resource id[%s]", id));
        this.data.client.performRequestAsync(
                HttpMethod.PUT.name(),
                String.format("/%s/_doc/%s", index, id),
                new HashMap<>(),
                new NStringEntity(data.jsonContent, ContentType.APPLICATION_JSON),
                new UpdateResponseListener(index)
        );
    }

    public void delete(String index, String id) {
        logger.info(String.format("Deleting resource id[%s]", id));
        this.data.client.performRequestAsync(
                HttpMethod.DELETE.name(),
                String.format("/%s/_doc/%s", index, id),
                new HashMap<>(),
                new UpdateResponseListener(index)
        );
    }

    public static class IndexData{
        public final String id;
        public final String jsonContent;

        public IndexData(String id, String jsonContent) {
            this.id = id;
            this.jsonContent = jsonContent;
        }
    }
}
