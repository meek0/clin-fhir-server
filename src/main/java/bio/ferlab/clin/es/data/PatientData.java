package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static bio.ferlab.clin.es.data.ElasticsearchData.EMPTY_STRING;

@Data
@NoArgsConstructor
public class PatientData implements WithFullText {

    private String cid = EMPTY_STRING;
    private OrganizationData organization = new OrganizationData();
    private String lastName = EMPTY_STRING;
    private String firstName = EMPTY_STRING;
    private String lastNameFirstName = EMPTY_STRING;
    private String gender = EMPTY_STRING;
    private String birthDate = null;
    private PractitionerData practitioner = new PractitionerData();
    private List<String> mrn = new ArrayList<>();
    private String ramq = EMPTY_STRING;
    private String position = EMPTY_STRING;
    private String familyId = EMPTY_STRING;
    private String familyType = EMPTY_STRING;
    private String ethnicity = EMPTY_STRING;
    private String bloodRelationship = EMPTY_STRING;
    private String timestamp = Instant.now().toString();
    private boolean fetus = false;
    private List<PrescriptionData> requests = new ArrayList<>();
    private List<String> securityTags = new ArrayList<>();

    /*
     * The following fields will be copied with the same value
     * as the original and use a type: text mapping in ES
     * instead of keyword.
     */
    private String cidText;
    private String lastNameText;
    private String firstNameText;
    private String lastNameFirstNameText;
    private String ramqText;
    private List<String> mrnText; 
    
    public void applyFullText() {
        this.cidText = cid;
        this.lastNameText = lastName;
        this.firstNameText = firstName;
        this.lastNameFirstNameText = lastNameFirstName;
        this.ramqText = ramq;
        this.mrnText = mrn;
        Optional.ofNullable(organization).ifPresent(WithFullText::applyFullText);
        Optional.ofNullable(practitioner).ifPresent(WithFullText::applyFullText);
        Optional.ofNullable(requests).orElse(Collections.emptyList()).forEach(WithFullText::applyFullText);
    }
    
}
