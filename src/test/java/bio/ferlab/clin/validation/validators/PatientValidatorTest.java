package bio.ferlab.clin.validation.validators;

import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;

import java.util.Calendar;
import java.util.Date;

public class PatientValidatorTest {
    private final PatientValidator patientValidator = new PatientValidator();
    private Patient patient;

    @BeforeEach
    public void setup() {
        this.patient = new Patient();
        this.patient.addIdentifier()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                .setType(new CodeableConcept().addCoding(new Coding().setCode("MR")))
                .setValue("12345");


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

        @Nested
        @DisplayName("Without RAMQ")
        class WithoutRAMQ {
            @Test
            @DisplayName("PatientValidator::validate should return true")
            public void validateShouldReturnTrue() {
                Assertions.assertTrue(patientValidator.validate(patient).isEmpty());
            }
        }

        @Nested
        @DisplayName("With RAMQ")
        class WithRAMQ {
            @Test
            @DisplayName("PatientValidator::validate should return true")
            public void validateShouldReturnTrue() {

                patient.addIdentifier()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setType(new CodeableConcept().addCoding(new Coding().setCode("JHN")))
                        .setValue("ABCD00000000");
                Assertions.assertTrue(patientValidator.validate(patient).isEmpty());
            }
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
                Date date = new Date();
                final Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DATE, 1);
                patient.setBirthDate(calendar.getTime());
                Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
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
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.addName().setFamily(" Test");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.addName().setFamily(" Test ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.addName().setFamily(" ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }


            @Nested
            @DisplayName("Contains special characters")
            class SpecialCharacters {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.addName().setFamily("%^Test");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }
        }

        @Nested
        @DisplayName("RAMQ")
        class InvalidRAMQ {
            @Nested
            @DisplayName("Not trimmed")
            class NotTrimmed {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.addIdentifier()
                            .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                            .setType(new CodeableConcept().addCoding(new Coding().setCode("JHN")))
                            .setValue("ABC00000000 ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.getIdentifier().get(1).setValue(" ABC00000000");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.getIdentifier().get(0).setValue(" ABC00000000 ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.getIdentifier().get(0).setValue(" ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }

            @Nested
            @DisplayName("Invalid syntax")
            class Syntax {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.addIdentifier()
                            .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                            .setType(new CodeableConcept().addCoding(new Coding().setCode("JHN")))
                            .setValue("ABC00000000");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }
        }

        @Nested
        @DisplayName("MRN")
        class InvalidMRN {
            @Nested
            @DisplayName("Not trimmed")
            class NotTrimmed {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.getIdentifier().get(0).setValue(" 12345 ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.getIdentifier().get(0).setValue(" ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.getIdentifier().get(0).setValue("12345 ");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                    patient.getIdentifier().get(0).setValue(" 12345");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }

            @Nested
            @DisplayName("With special characters")
            class SpecialCharacters {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.getIdentifier().get(0).setValue("1234%");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }


            @Nested
            @DisplayName("Empty")
            class Empty {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.getIdentifier().get(0).setValue("");
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }

            @Nested
            @DisplayName("Not provided")
            class NotProvided {
                @Test
                @DisplayName("PatientValidator::validate should return false")
                public void validateShouldReturnFalse() {
                    patient.getIdentifier().clear();
                    Assertions.assertFalse(patientValidator.validate(patient).isEmpty());
                }
            }
        }
    }
}
