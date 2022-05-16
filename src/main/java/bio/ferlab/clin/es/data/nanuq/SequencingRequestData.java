package bio.ferlab.clin.es.data.nanuq;

import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class SequencingRequestData {
  
  private String requestId = EMPTY_STRING;
  private String status = EMPTY_STRING;
  
}
