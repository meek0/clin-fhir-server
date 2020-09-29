package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {
    @Bean(destroyMethod = "close")
    public ElasticsearchData esData() {
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"),
                new HttpHost("localhost", 9201, "http")
        ).build();
        return new ElasticsearchData(client);
    }

    @Bean
    public ElasticsearchRestClient esRestClient(ElasticsearchData esData){
        return new ElasticsearchRestClient(esData);
    }
}
