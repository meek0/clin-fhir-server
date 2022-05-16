package bio.ferlab.clin.es.data.nanuq;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AnalysisData extends AbstractPrescriptionData {

  private List<SequencingRequestData> sequencingRequest = new ArrayList<>();
  
}
