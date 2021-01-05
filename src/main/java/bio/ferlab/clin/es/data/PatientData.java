package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
public class PatientData {
    private static final String EMPTY_STRING = "";
    private String id;
    private String status;
    private Organization organization;
    private String lastName;
    private String firstName;
    private String gender;
    private String birthDate;
    private Practitioner practitioner;
    private String test;
    private String prescription;
    private String mrn;
    private String ramq;
    private String position;
    private String familyId;
    private String familyType;
    private String ethnicity;
    private String bloodRelationship;
    private String request;
    private String timestamp;
    private boolean submitted;

    public PatientData() {
        this.id = EMPTY_STRING;
        this.status = EMPTY_STRING;
        this.organization = new Organization();
        this.lastName = EMPTY_STRING;
        this.firstName = EMPTY_STRING;
        this.gender = EMPTY_STRING;
        this.birthDate = EMPTY_STRING;
        this.practitioner = new Practitioner();
        this.test = EMPTY_STRING;
        this.prescription = EMPTY_STRING;
        this.mrn = EMPTY_STRING;
        this.ramq = EMPTY_STRING;
        this.position = EMPTY_STRING;
        this.familyId = EMPTY_STRING;
        this.familyType = EMPTY_STRING;
        this.ethnicity = EMPTY_STRING;
        this.bloodRelationship = EMPTY_STRING;
        this.request = EMPTY_STRING;
        this.timestamp = Instant.now().toString();
        this.submitted = false;
    }

    @Data
    @NoArgsConstructor
    public static class Organization {
        public String id = EMPTY_STRING;
        public String name = EMPTY_STRING;
    }

    @Data
    @NoArgsConstructor
    public static class Practitioner {
        public String id = EMPTY_STRING;
        public String lastName = EMPTY_STRING;
        public String firstName = EMPTY_STRING;
    }
}
