package bio.ferlab.clin.es.data;

import bio.ferlab.clin.exceptions.FailedToUpdateIndexException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class IndexResponseListener implements ResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(IndexResponseListener.class);
    private final String index;

    public IndexResponseListener(String index) {
        this.index = index;
    }

    @Override
    public void onSuccess(Response response) {
        try {
            final String content = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            final ElasticResponse.Index parsed = new ObjectMapper().readValue(content, ElasticResponse.Index.class);
            if(parsed.getShards().getSuccessful() != 1 || parsed.getShards().getFailed() > 0){
                throw new FailedToUpdateIndexException(this.index);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(Exception e) {
        logger.error(e.getLocalizedMessage());
    }
}
