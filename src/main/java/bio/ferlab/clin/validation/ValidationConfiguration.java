package bio.ferlab.clin.validation;

import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.validation.validators.ObservationValidator;
import bio.ferlab.clin.validation.validators.PatientValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfiguration {
    
    @Autowired
    private BioProperties bioProperties;
    
    @Bean
    public ValidationChain validationChain() {
        if (bioProperties.isNanuqEnabled()) {
            return new ValidationChain();
        } else {
            return new ValidationChain()
                .withValidator(new PatientValidator())
                .withValidator(new ObservationValidator());
        }
    }
}
