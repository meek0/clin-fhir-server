package bio.ferlab.clin.es.data;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;

public class ElasticResponse {
    @Data
    public static class Index {
        @JsonProperty("_index")
        private String index;
        @JsonProperty("_type")
        private String type;
        @JsonProperty("_id")
        private String id;
        @JsonProperty("_version")
        private long version;
        @JsonProperty("result")
        private String result;
        @JsonProperty("_shards")
        private Shards shards;
        @JsonProperty("_seq_no")
        private long seqNo;
        @JsonProperty("_primary_term")
        private long primaryTerm;
    }

    @Data
    public static class Shards {
        private long total;
        private long successful;
        private long failed;
    }
}
