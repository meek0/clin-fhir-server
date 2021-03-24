package bio.ferlab.clin.es.config;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.data.ElasticsearchData;
import ca.uhn.fhir.jpa.app.HapiProperties;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {
    private static final int KEEP_ALIVE_VALUE = 1000 * 30;
    private final String host = HapiProperties.getBioEsHost();
    private final int port = HapiProperties.getBioEsPort();
    private final String scheme = HapiProperties.getBioEsScheme();

    @Bean(destroyMethod = "close")
    public ElasticsearchData esData() {
        RestClient client = RestClient.builder(
                new HttpHost(this.host, this.port, this.scheme)
        ).setHttpClientConfigCallback(httpClientBuilder -> HttpAsyncClientBuilder.create().setKeepAliveStrategy(
                (response, context) -> KEEP_ALIVE_VALUE)
        ).build();
        return new ElasticsearchData(client, this.host);
    }

    @Bean
    public ElasticsearchRestClient esRestClient(ElasticsearchData esData) {
        return new ElasticsearchRestClient(esData);
    }
}
