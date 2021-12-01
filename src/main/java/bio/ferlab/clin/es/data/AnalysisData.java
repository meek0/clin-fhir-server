package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class AnalysisData {
  public String code = EMPTY_STRING;
  public String display = EMPTY_STRING;
}