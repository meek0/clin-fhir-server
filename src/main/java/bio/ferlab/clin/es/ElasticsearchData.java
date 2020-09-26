package bio.ferlab.clin.es;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ElasticsearchData {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchData.class);
//    private final RestClient client;

    public ElasticsearchData() {

//        this.client = new RestHighLevelClient(
//                RestClient.builder(
//                        new HttpHost("localhost", 9200, "http"),
//                        new HttpHost("localhost", 9201, "http")
//                )
//        );
    }

    public void close(){
//        try {
//            this.client.close();
//        } catch (IOException e) {
//            logger.error(e.getLocalizedMessage());
//            e.printStackTrace();
//        }
    }
}
