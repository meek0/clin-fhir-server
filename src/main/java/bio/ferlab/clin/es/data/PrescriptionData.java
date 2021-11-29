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

    private String cid = EMPTY_STRING;
    private String mrn = EMPTY_STRING;
    private String ethnicity = EMPTY_STRING;
    private boolean bloodRelationship = false;
    private String timestamp = Instant.now().toString();
    private String status = EMPTY_STRING;
    private Analysis analysis = new Analysis();
    private boolean submitted = false;
    private String authoredOn = null;
    private String laboratory = EMPTY_STRING;
    private Practitioner prescriber = new Practitioner();
    private Practitioner approver = new Practitioner();
    private Organization organization = new Organization();
    private PatientInformation patientInfo = new PatientInformation();
    private FamilyGroupInfo familyInfo = new FamilyGroupInfo();

    @Data
    @NoArgsConstructor
    public static class PatientInformation {
        private String cid = EMPTY_STRING;
        private String lastName = EMPTY_STRING;
        private String firstName = EMPTY_STRING;
        private String lastNameFirstName = EMPTY_STRING;
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
        private String cid = EMPTY_STRING;
        private String type = EMPTY_STRING;
    }

    @Data
    @NoArgsConstructor
    public static class Organization {
        public String cid = EMPTY_STRING;
        public String name = EMPTY_STRING;
    }

    @Data
    @NoArgsConstructor
    public static class Practitioner {
        public String cid = EMPTY_STRING;
        public String lastName = EMPTY_STRING;
        public String firstName = EMPTY_STRING;
        public String lastNameFirstName = EMPTY_STRING;
    }

    @Data
    @NoArgsConstructor
    public static class Analysis {
        public String code = EMPTY_STRING;
        public String display = EMPTY_STRING;
    }
}
