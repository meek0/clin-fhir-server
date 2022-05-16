package bio.ferlab.clin.es.data.nanuq;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class AbstractPrescriptionData {

  private String requestId = EMPTY_STRING;
  private String patientId = EMPTY_STRING;
  private String status = EMPTY_STRING;
  private String priority = EMPTY_STRING;
  private boolean prenatal = false;
  private String panelCode = EMPTY_STRING;
  private String requester = EMPTY_STRING;
  private String ldm = EMPTY_STRING;
  private String ep = EMPTY_STRING;
  private String createdOn = EMPTY_STRING;
  private String timestamp = Instant.now().toString();
  private List<String> securityTags = new ArrayList<>();
  
}
