package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {
    private final String host = HapiProperties.getBioEsAuth();
    private final int port = HapiProperties.getBioEsPort();
    private final int transportPort = HapiProperties.getBioEsTransportPort();
    private final String scheme = HapiProperties.getBioEsScheme();
    private final String authToken = HapiProperties.getBioEsAuthorizationToken();

    @Bean(destroyMethod = "close")
    public ElasticsearchData esData() {
        RestClient client = RestClient.builder(
                new HttpHost(this.host, this.port, this.scheme),
                new HttpHost(this.host, this.transportPort, this.scheme)
        ).build();
        return new ElasticsearchData(client, this.host, this.authToken);
    }

    @Bean
    public ElasticsearchRestClient esRestClient(ElasticsearchData esData){
        return new ElasticsearchRestClient(esData);
    }
}
