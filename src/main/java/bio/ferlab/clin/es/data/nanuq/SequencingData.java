package bio.ferlab.clin.es.data.nanuq;

import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class SequencingData extends AbstractPrescriptionData {
  
  private String prescriptionId = EMPTY_STRING;
  private String sample = EMPTY_STRING;
}
