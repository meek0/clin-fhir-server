package bio.ferlab.clin.es.data.nanuq;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SequencingData extends AbstractPrescriptionData {

  private String requestId = EMPTY_STRING;
  private String sample = EMPTY_STRING;
  private String prescriptionStatus = EMPTY_STRING;
  private String taskRunname = EMPTY_STRING;
  private String patientRelationship = EMPTY_STRING;
  private String patientDiseaseStatus = EMPTY_STRING;
}
