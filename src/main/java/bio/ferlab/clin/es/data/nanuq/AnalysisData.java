package bio.ferlab.clin.es.data.nanuq;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AnalysisData extends AbstractPrescriptionData {

  private List<String> assignments = new ArrayList<>();
  private List<SequencingRequestData> sequencingRequests = new ArrayList<>();
  
}
