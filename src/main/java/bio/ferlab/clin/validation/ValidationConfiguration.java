package bio.ferlab.clin.validation;

import bio.ferlab.clin.validation.validators.ObservationValidator;
import bio.ferlab.clin.validation.validators.PatientValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfiguration {
    @Bean
    public ValidationChain validationChain() {
        return new ValidationChain()
                .withValidator(new PatientValidator())
                .withValidator(new ObservationValidator());
    }
}
