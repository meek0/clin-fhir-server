package bio.ferlab.clin.es.data;

import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateResponseListener implements ResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(UpdateResponseListener.class);
    private final String index;

    public  UpdateResponseListener(String index) {
        this.index = index;
    }

    @Override
    public void onSuccess(Response response) {
        logger.info(String.format("Index [%s] successfully updated.", this.index));
    }

    @Override
    public void onFailure(Exception e) {
        logger.error(e.getLocalizedMessage());
    }
}
