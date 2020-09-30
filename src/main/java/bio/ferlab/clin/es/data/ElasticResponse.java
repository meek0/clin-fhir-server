package bio.ferlab.clin.es.data;

import com.fasterxml.jackson.annotation.*;

public class ElasticResponse {
    public static class Index {
        private String index;
        private String type;
        private String id;
        private long version;
        private String result;
        private Shards shards;
        private long seqNo;
        private long primaryTerm;

        @JsonProperty("_index")
        public String getIndex() {
            return index;
        }

        @JsonProperty("_type")
        public String getType() {
            return type;
        }

        @JsonProperty("_id")
        public String getID() {
            return id;
        }

        @JsonProperty("_version")
        public long getVersion() {
            return version;
        }

        @JsonProperty("result")
        public String getResult() {
            return result;
        }

        @JsonProperty("_shards")
        public Shards getShards() {
            return shards;
        }

        @JsonProperty("_seq_no")
        public long getSeqNo() {
            return seqNo;
        }

        @JsonProperty("_primary_term")
        public long getPrimaryTerm() {
            return primaryTerm;
        }
    }


    public static class Shards {
        private long total;
        private long successful;
        private long failed;

        @JsonProperty("total")
        public long getTotal() {
            return total;
        }

        @JsonProperty("successful")
        public long getSuccessful() {
            return successful;
        }

        @JsonProperty("failed")
        public long getFailed() {
            return failed;
        }
    }
}
