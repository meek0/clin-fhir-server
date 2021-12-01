package bio.ferlab.clin.es.data;


import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class FamilyGroupInfoData implements WithFullText {
  private String cid = EMPTY_STRING;
  private String type = EMPTY_STRING;

  private String cidText;
  @Override
  public void applyFullText() {
    this.cidText = cid;
  }
}