package bio.ferlab.clin.validation.validators;

import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.*;

import java.util.Date;

public class PatientValidatorTest {
    private final PatientValidator patientValidator = new PatientValidator();
    private Patient patient;

    @BeforeEach
    public void setup() {
        this.patient = new Patient();
        this.patient.addIdentifier()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                .setValue("12345");

        this.patient.addIdentifier()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                .setValue("ABCD00000000");

        this.patient.setBirthDate(DateUtils.addDays(new Date(), -1));
        this.patient.addName()
                .setFamily("Test")
                .addGiven("Test")
                .addGiven("Testing");
        this.patient.setIdElement(IdType.newRandomUuid());
        this.patient.setGender(Enumerations.AdministrativeGender.MALE);
        this.patient.setId(IdType.newRandomUuid());
    }

    @Nested
    @DisplayName("With valid data")
    class Valid {
        @Test
        @DisplayName("PatientValidator::validate should return true")
        public void validateShouldReturnTrue() {
            Assertions.assertTrue(patientValidator.validate(patient));
        }
    }


    @Nested
    @DisplayName("With invalid data")
    class Invalid {

        @Nested
        @DisplayName("Birthdate")
        class InvalidBirthDate {
            @Test
            @DisplayName("PatientValidator::validate should return false")
            public void validateShouldReturnFalse() {
                patient.setBirthDate(null);
                Assertions.assertFalse(patientValidator.validate(patient));
            }
        }


        @Nested
        @DisplayName("Name")
        class InvalidName {
            @Nested
            @DisplayName("Is not trimmed")
            class NotTrimmed {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.addName().setFamily("Test ");
                    Assertions.assertFalse(patientValidator.validate(patient));
                }
            }


            @Nested
            @DisplayName("Contains special characters")
            class SpecialCharacters {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.addName().setFamily("%^Test");
                    Assertions.assertFalse(patientValidator.validate(patient));
                }
            }
        }


        @Nested
        @DisplayName("RAMQ")
        class InvalidRAMQ {
            @Test
            @DisplayName("PatientValidator::validate should return false")
            public void validateShouldReturnFalse(){
                patient.getIdentifier().get(1).setValue("ABC00000000");
                Assertions.assertFalse(patientValidator.validate(patient));
            }
        }
    }
}
