package bio.ferlab.clin.validation;

import bio.ferlab.clin.validation.validators.ObservationValidator;
import bio.ferlab.clin.validation.validators.PatientValidator;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.mockito.Mockito.*;

public class ValidationChainTest {
    @Spy
    private PatientValidator patientValidator;
    @Spy
    private ObservationValidator observationValidator;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    @DisplayName("When Validator is chained")
    class WhenValidatorChained {
        @Test
        @DisplayName("ValidationChain::validate should call Validator::validateResource")
        void shouldBeCalled() {
            final ValidationChain validationChain = new ValidationChain()
                    .withValidator(patientValidator)
                    .withValidator(observationValidator);

            final Patient resource = new Patient();
            when(patientValidator.validateResource(resource)).thenReturn(false);
            final boolean result = validationChain.isValid(resource);
            verify(patientValidator, times(1)).validateResource(any());
            Assertions.assertFalse(result);
        }
    }

    @Nested
    @DisplayName("When multiple validators are chained")
    class AllAdded {
        @Test
        @DisplayName("ValidationChain::validate should call each Validator::validateResource")
        void shouldBeCalled() {
            final ValidationChain validationChain = new ValidationChain()
                    .withValidator(patientValidator)
                    .withValidator(observationValidator);

            final Patient patient = new Patient();
            final Observation observation = new Observation();

            when(patientValidator.validateResource(patient)).thenReturn(false);
            when(observationValidator.validateResource(observation)).thenReturn(false);

            boolean result = validationChain.isValid(patient);
            verify(patientValidator, times(1)).validateResource(patient);
            Assertions.assertFalse(result);

            result = validationChain.isValid(observation);
            verify(observationValidator, times(1)).validateResource(observation);
            Assertions.assertFalse(result);
        }
    }

    @Nested
    @DisplayName("When Validator is not chained")
    class NotAdded {
        @Test
        @DisplayName("ValidationChain::validate should return true")
        void shouldBeCalled() {
            final ValidationChain validationChain = new ValidationChain();

            final Patient patient = new Patient();
            final Observation observation = new Observation();

            final boolean patientResult = validationChain.isValid(patient);
            final boolean observationResult = validationChain.isValid(observation);

            Assertions.assertTrue(patientResult);
            Assertions.assertTrue(observationResult);
        }
    }
}
