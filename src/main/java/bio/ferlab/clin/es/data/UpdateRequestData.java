package bio.ferlab.clin.es.data;

import bio.ferlab.clin.utils.JsonGenerator;

public class UpdateRequestData {
    public final String status;
    public final boolean submitted;

    public UpdateRequestData(String status, boolean submitted) {
        this.status = status;
        this.submitted = submitted;
    }

    public String stringify(JsonGenerator jsonGenerator) {
        return jsonGenerator.toString(new UpdateWrapper(this));
    }

    static class UpdateWrapper {
        public UpdateRequestData doc;

        public UpdateWrapper(UpdateRequestData doc) {
            this.doc = doc;
        }
    }
}
