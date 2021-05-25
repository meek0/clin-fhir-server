package bio.ferlab.clin.es.config;

import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.data.ElasticsearchData;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {
    private static final int KEEP_ALIVE_VALUE = 1000 * 30;

    @Bean(destroyMethod = "close")
    public ElasticsearchData esData(BioProperties bioProperties) {
        RestClient client = RestClient.builder(
                new HttpHost(bioProperties.getEsHost(), bioProperties.getEsPort(), bioProperties.getEsScheme())
        ).setHttpClientConfigCallback(httpClientBuilder -> HttpAsyncClientBuilder.create().setKeepAliveStrategy(
                (response, context) -> KEEP_ALIVE_VALUE)
        ).build();
        return new ElasticsearchData(client, bioProperties.getEsHost());
    }

    @Bean
    public ElasticsearchRestClient esRestClient(ElasticsearchData esData) {
        return new ElasticsearchRestClient(esData);
    }
}
