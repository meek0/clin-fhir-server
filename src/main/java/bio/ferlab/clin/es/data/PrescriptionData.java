package bio.ferlab.clin.es.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PrescriptionData {
    private static final String EMPTY_STRING = "";

    private String id = EMPTY_STRING;
    private String mrn = EMPTY_STRING;
    private String ethnicity = EMPTY_STRING;
    private boolean bloodRelationship = false;
    private String timestamp = Instant.now().toString();
    private String status = EMPTY_STRING;
    private String test = EMPTY_STRING;
    private boolean submitted = false;
    private String authoredOn = null;
    private Practitioner practitioner = new Practitioner();
    private PatientInformation patientInfo = new PatientInformation();
    private FamilyGroupInfo familyInfo = new FamilyGroupInfo();

    @Data
    @NoArgsConstructor
    public static class PatientInformation {
        private String id = EMPTY_STRING;
        private String lastName = EMPTY_STRING;
        private String firstName = EMPTY_STRING;
        private String gender = EMPTY_STRING;
        private String ramq = EMPTY_STRING;
        private List<String> mrn = new ArrayList<>();
        private String position = EMPTY_STRING;
        private boolean fetus = false;
        private String birthDate = Instant.now().toString();
        private Organization organization = new Organization();
    }

    @Data
    @NoArgsConstructor
    public static class FamilyGroupInfo {
        private String id = EMPTY_STRING;
        private String type = EMPTY_STRING;
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
