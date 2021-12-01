package bio.ferlab.clin.es.data;

import org.elasticsearch.client.RestClient;

import java.io.IOException;

public class ElasticsearchData {
    public static final String EMPTY_STRING = "";
    
    public final RestClient client;
    public final String host;

    public ElasticsearchData(RestClient client, String host) {
        this.client = client;
        this.host = host;
    }

    public void close() throws IOException {
        this.client.close();
    }
}
