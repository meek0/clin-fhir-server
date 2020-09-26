package bio.ferlab.clin.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfiguration.class);

    @Bean(destroyMethod = "close")
    public ElasticsearchData esData(){
        logger.info("Initializing ES configuration");
        return new ElasticsearchData();
    }
}
