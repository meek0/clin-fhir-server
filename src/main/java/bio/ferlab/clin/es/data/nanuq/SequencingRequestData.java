package bio.ferlab.clin.es.data.nanuq;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SequencingRequestData {

  private String requestId = EMPTY_STRING;
  private String prescriptionId = EMPTY_STRING;
  private String status = EMPTY_STRING;
  private String sample = EMPTY_STRING;
  private String patientId = EMPTY_STRING;
  private String patientMRN = EMPTY_STRING;
  private String taskRunname = EMPTY_STRING;
  private String patientRelationship = EMPTY_STRING;
  private String patientDiseaseStatus = EMPTY_STRING;

}
