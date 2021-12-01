package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class OrganizationData implements WithFullText{
  public String cid = EMPTY_STRING;
  public String name = EMPTY_STRING;

  public String cidText;

  @Override
  public void applyFullText() {
    this.cidText = cid;
  }
}