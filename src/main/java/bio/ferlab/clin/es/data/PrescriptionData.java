package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class PrescriptionData implements WithFullText{
    
    public enum State {
        incomplete,
        submitted,
        active,
        completed,
        revoked;
    }
    
    private static final String EMPTY_STRING = "";

    private String cid = EMPTY_STRING;
    private String mrn = EMPTY_STRING;
    private String ethnicity = EMPTY_STRING;
    private boolean bloodRelationship = false;
    private String timestamp = Instant.now().toString();
    private String status = EMPTY_STRING;
    private AnalysisData analysis = new AnalysisData();
    private boolean submitted = false;
    private String authoredOn = null;
    private String laboratory = EMPTY_STRING;
    private PractitionerData prescriber = new PractitionerData();
    private PractitionerData approver = new PractitionerData();
    private OrganizationData organization = new OrganizationData();
    private PatientData patientInfo = new PatientData();
    private FamilyGroupInfoData familyInfo = new FamilyGroupInfoData();
    private State state = null;
    private List<String> securityTags = new ArrayList<>();

    /*
     * The following fields will be copied with the same value
     * as the original and use a type: text mapping in ES
     * instead of keyword.
     */
    private String cidText;
    private String mrnText;
    
    public void buildState() {
        if ("on-hold".equals(status)) {
            if (submitted) {
                this.state = State.submitted;
            } else {
                this.state = State.incomplete;
            }
        } else if ("active".equals(status)) {
            this.state = State.active;
        } else if ("completed".equals(status)) {
            this.state = State.completed;
        } else if ("revoked".equals(status)) {
            this.state = State.revoked;
        } else {
            this.state = null;
        }
    }

    @Override
    public void applyFullText() {
        this.cidText = cid;
        this.mrnText = mrn;
        Optional.ofNullable(patientInfo).ifPresent(WithFullText::applyFullText);
        Optional.ofNullable(prescriber).ifPresent(WithFullText::applyFullText);
        Optional.ofNullable(approver).ifPresent(WithFullText::applyFullText);
        Optional.ofNullable(organization).ifPresent(WithFullText::applyFullText);
        Optional.ofNullable(familyInfo).ifPresent(WithFullText::applyFullText);
    }

}
