package bio.ferlab.clin.es.data;

import lombok.Data;

@Data
public class PatientData {
    private static final String N_A = "N/A";
    private String id;
    private String firstName;
    private String lastName;
    private String mrn;
    private String ramq;
    private String status;
    private String organization;
    private String gender;
    private String birthDate;
    private String familyId;
    private String familyComposition;
    private String familyType;
    private String ethnicity;
    private String bloodRelationship;
    private String proband;
    private String position;
    private String practitioner;
    private String request;
    private String test;
    private String prescription;

    public PatientData() {
        this.id = N_A;
        this.firstName = N_A;
        this.lastName = N_A;
        this.mrn = N_A;
        this.ramq = N_A;
        this.status = N_A;
        this.organization = N_A;
        this.gender = N_A;
        this.birthDate = N_A;
        this.familyId = N_A;
        this.familyComposition = N_A;
        this.familyType = N_A;
        this.ethnicity = N_A;
        this.bloodRelationship = N_A;
        this.proband = N_A;
        this.position = N_A;
        this.practitioner = N_A;
        this.request = N_A;
        this.test = N_A;
        this.prescription = N_A;
    }
}
