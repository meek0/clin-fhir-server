package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class PatientData {
    private static final String EMPTY_STRING = "";
    private String id;
    private Organization organization;
    private String lastName;
    private String firstName;
    private String gender;
    private String birthDate;
    private Practitioner practitioner;
    private List<String> mrn;
    private String ramq;
    private String position;
    private String familyId;
    private String familyType;
    private String ethnicity;
    private String bloodRelationship;
    private String timestamp;
    private boolean fetus;
    private List<RequestData> requests;

    public PatientData() {
        this.id = EMPTY_STRING;
        this.organization = new Organization();
        this.lastName = EMPTY_STRING;
        this.firstName = EMPTY_STRING;
        this.gender = EMPTY_STRING;
        this.birthDate = null;
        this.practitioner = new Practitioner();
        this.mrn = new ArrayList<>();
        this.ramq = EMPTY_STRING;
        this.position = EMPTY_STRING;
        this.familyId = EMPTY_STRING;
        this.familyType = EMPTY_STRING;
        this.ethnicity = EMPTY_STRING;
        this.bloodRelationship = EMPTY_STRING;
        this.timestamp = Instant.now().toString();
        this.fetus = false;
        this.requests = new ArrayList<>();
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

    @Data
    @NoArgsConstructor
    public static class RequestData {
        private String request = EMPTY_STRING;
        private String status = EMPTY_STRING;
        private String test = EMPTY_STRING;
        private boolean submitted = false;
        private String prescription = null;
    }
}
