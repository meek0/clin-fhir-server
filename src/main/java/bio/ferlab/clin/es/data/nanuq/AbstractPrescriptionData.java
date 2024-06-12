package bio.ferlab.clin.es.data.nanuq;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class AbstractPrescriptionData {

  private String prescriptionId = EMPTY_STRING;
  private String patientId = EMPTY_STRING;
  private String patientMRN = EMPTY_STRING;
  private String status = EMPTY_STRING;
  private String priority = EMPTY_STRING;
  private boolean prenatal = false;
  private String analysisCode = EMPTY_STRING;
  private String requester = EMPTY_STRING;
  private String ldm = EMPTY_STRING;
  private String ep = EMPTY_STRING;
  private String createdOn = EMPTY_STRING;
  private String timestamp = Instant.now().toString();
  private List<String> securityTags = new ArrayList<>();
  private Collection<String> tasks = new HashSet<>();
}
