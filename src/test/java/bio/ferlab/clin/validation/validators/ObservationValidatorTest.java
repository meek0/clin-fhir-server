package bio.ferlab.clin.validation.validators;

import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.*;

import java.util.Collections;

public class ObservationValidatorTest {
    private final ObservationValidator observationValidator = new ObservationValidator();
    private final Observation observation = new Observation();


    private CodeableConcept createInterpretation(String code) {
        final CodeableConcept interpretation = new CodeableConcept();
        interpretation.addCoding().setCode(code);
        return interpretation;
    }

    @Nested
    @DisplayName("Valid")
    class Valid {
        @Nested
        @DisplayName("CGH Observation")
        class CGH {
            @BeforeEach
            void setup() {
                final CodeableConcept code = new CodeableConcept();
                code.addCoding().setCode("CGH");
                observation.setCode(code);
            }

            @Test
            @DisplayName("With abnormal interpretation")
            void validAbnormal() {
                observation.setInterpretation(Collections.singletonList(createInterpretation("A")));
                observation.addNote().setText("Note");
                Assertions.assertTrue(observationValidator.validateResource(observation));
            }

            @Test
            @DisplayName("With normal interpretation")
            void validNormal() {
                observation.setInterpretation(Collections.singletonList(createInterpretation("N")));
                Assertions.assertTrue(observationValidator.validateResource(observation));
            }

            @Test
            @DisplayName("With Indeterminate interpretation")
            void validIndeterminate() {
                observation.setInterpretation(Collections.singletonList(createInterpretation("IND")));
                Assertions.assertTrue(observationValidator.validateResource(observation));
            }
        }
    }

    @Nested
    @DisplayName("With Invalid")
    class Invalid {
        @Nested
        @DisplayName("Observation Note")
        class Note{
            @Nested
            @DisplayName("When not trimmed")
            class NotTrimmed{
                @Test
                @DisplayName("Should return false")
                void returnFalse(){
                    observation.setInterpretation(Collections.singletonList(createInterpretation("IND")));
                    final Annotation annotation = observation.addNote();
                    annotation.setText(" ");
                    Assertions.assertFalse(observationValidator.validateResource(observation));
                    annotation.setText(" note");
                    Assertions.assertFalse(observationValidator.validateResource(observation));
                    annotation.setText("note ");
                    Assertions.assertFalse(observationValidator.validateResource(observation));
                    annotation.setText(" note ");
                    Assertions.assertFalse(observationValidator.validateResource(observation));
                }
            }
        }

        @Nested
        @DisplayName("CGH Observation")
        class CGH {
            @BeforeEach
            void setup() {
                final CodeableConcept code = new CodeableConcept();
                code.addCoding().setCode("CGH");
                observation.setCode(code);
            }

            @Nested
            @DisplayName("When abnormal interpretation")
            class Abnormal{

                @Nested
                @DisplayName("Without Precision")
                class NoPrecision{
                    @Test
                    @DisplayName("Should return false")
                    void withoutPrecision() {
                        observation.setInterpretation(Collections.singletonList(createInterpretation("A")));
                        Assertions.assertFalse(observationValidator.validateResource(observation));
                    }
                }
                @Nested
                @DisplayName("With malformed precision")
                class MalformedPrecision{
                    @Test
                    @DisplayName("Should return false")
                    void withoutPrecision() {
                        observation.setInterpretation(Collections.singletonList(createInterpretation("A")));
                        observation.addNote().setText("  Note");
                        Assertions.assertFalse(observationValidator.validateResource(observation));
                    }
                }
            }
        }
    }
}
