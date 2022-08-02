package bio.ferlab.clin.validation;

import bio.ferlab.clin.validation.validators.ObservationValidator;
import bio.ferlab.clin.validation.validators.PatientValidator;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ValidationChainTest {
    @Spy
    private PatientValidator patientValidator;
    @Spy
    private ObservationValidator observationValidator;
    private AutoCloseable autoCloseable;

    @BeforeEach
    public void setup() {
        this.autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void clean() throws Exception {
        this.autoCloseable.close();
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
            //when(patientValidator.validateResource(resource)).thenReturn(List.of("error"));
            final boolean result = validationChain.isValid(resource).isEmpty();
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

            //when(patientValidator.validateResource(patient)).thenReturn(List.of("error"));
            //when(observationValidator.validateResource(observation)).thenReturn(List.of("error"));

            boolean result = validationChain.isValid(patient).isEmpty();
            verify(patientValidator, times(1)).validateResource(patient);
            Assertions.assertFalse(result);

            result = validationChain.isValid(observation).isEmpty();
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

            final boolean patientResult = validationChain.isValid(patient).isEmpty();
            final boolean observationResult = validationChain.isValid(observation).isEmpty();

            Assertions.assertTrue(patientResult);
            Assertions.assertTrue(observationResult);
        }
    }
}
