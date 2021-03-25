package bio.ferlab.clin;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bio")
@Data
public class BioProperties {
    private final boolean isBioEsEnabled;
    private final String esHost;
    private final int esPort;
    private final String esScheme;
    private final String esPatientIndex;
    private final boolean isAuthEnabled;
    private final String authServerUrl;
    private final String authRealm;
    private final boolean isDisableSslValidation;
    private final boolean isAuditsEnabled;

    public BioProperties(
            @Value("${bio.elasticsearch.enabled}") boolean isBioEsEnabled,
            @Value("${bio.elasticsearch.host}") String esHost,
            @Value("${bio.elasticsearch.port}") int esPort,
            @Value("${bio.elasticsearch.scheme}") String esScheme,
            @Value("${bio.elasticsearch.patient-index}") String esPatientIndex,
            @Value("${bio.auth.enabled}") boolean isAuthEnabled,
            @Value("${bio.auth.server-url}") String authServerUrl,
            @Value("${bio.auth.realm}") String authRealm,
            @Value("${bio.auth.disable-ssl-validation}") boolean isDisableSslValidation,
            @Value("${bio.audits.enabled}") boolean isAuditsEnabled
    ) {
        this.isBioEsEnabled = isBioEsEnabled;
        this.esHost = esHost;
        this.esPort = esPort;
        this.esScheme = esScheme;
        this.esPatientIndex = esPatientIndex;
        this.isAuthEnabled = isAuthEnabled;
        this.authServerUrl = authServerUrl;
        this.authRealm = authRealm;
        this.isDisableSslValidation = isDisableSslValidation;
        this.isAuditsEnabled = isAuditsEnabled;
    }
}
