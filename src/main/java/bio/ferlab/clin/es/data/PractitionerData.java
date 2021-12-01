package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class PractitionerData implements WithFullText{
  public String cid = EMPTY_STRING;
  public String lastName = EMPTY_STRING;
  public String firstName = EMPTY_STRING;
  public String lastNameFirstName = EMPTY_STRING;

  public String cidText;
  private String lastNameText;
  private String firstNameText;
  private String lastNameFirstNameText;

  @Override
  public void applyFullText() {
    this.cidText = cid;
    this.lastNameText = lastName;
    this.firstNameText = firstName;
    this.lastNameFirstNameText = lastNameFirstName;
  }
}