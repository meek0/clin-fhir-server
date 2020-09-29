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

        @JsonProperty("_index")
        public void setIndex(String value) {
            this.index = value;
        }

        @JsonProperty("_type")
        public String getType() {
            return type;
        }

        @JsonProperty("_type")
        public void setType(String value) {
            this.type = value;
        }

        @JsonProperty("_id")
        public String getID() {
            return id;
        }

        @JsonProperty("_id")
        public void setID(String value) {
            this.id = value;
        }

        @JsonProperty("_version")
        public long getVersion() {
            return version;
        }

        @JsonProperty("_version")
        public void setVersion(long value) {
            this.version = value;
        }

        @JsonProperty("result")
        public String getResult() {
            return result;
        }

        @JsonProperty("result")
        public void setResult(String value) {
            this.result = value;
        }

        @JsonProperty("_shards")
        public Shards getShards() {
            return shards;
        }

        @JsonProperty("_shards")
        public void setShards(Shards value) {
            this.shards = value;
        }

        @JsonProperty("_seq_no")
        public long getSeqNo() {
            return seqNo;
        }

        @JsonProperty("_seq_no")
        public void setSeqNo(long value) {
            this.seqNo = value;
        }

        @JsonProperty("_primary_term")
        public long getPrimaryTerm() {
            return primaryTerm;
        }

        @JsonProperty("_primary_term")
        public void setPrimaryTerm(long value) {
            this.primaryTerm = value;
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

        @JsonProperty("total")
        public void setTotal(long value) {
            this.total = value;
        }

        @JsonProperty("successful")
        public long getSuccessful() {
            return successful;
        }

        @JsonProperty("successful")
        public void setSuccessful(long value) {
            this.successful = value;
        }

        @JsonProperty("failed")
        public long getFailed() {
            return failed;
        }

        @JsonProperty("failed")
        public void setFailed(long value) {
            this.failed = value;
        }
    }
}
