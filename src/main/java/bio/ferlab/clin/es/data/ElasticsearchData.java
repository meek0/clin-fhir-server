package bio.ferlab.clin.es.data;

import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ElasticsearchData {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchData.class);
    public final RestClient client;

    public ElasticsearchData(RestClient client) {
        this.client = client;
    }

    public void close() {
        try {
            this.client.close();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
