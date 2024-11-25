package bio.ferlab.clin.properties;

import bio.ferlab.clin.es.MigrationManager;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "bio")
@Data
public class BioProperties {
    private final boolean isBioEsEnabled;
    private final String esHost;
    private final int esPort;
    private final String esScheme;
    private final String esPatientsIndex;
    private final String esPrescriptionsIndex;
    private final boolean isAuthEnabled;
    private final String authServerUrl;
    private final String authRealm;
    private final Integer authRetry;
    private final Long authLeeway;
    private final boolean isDisableSslValidation;
    private final boolean isAuditsEnabled;
    private final boolean isTaggingEnabled;
    private final boolean isTaggingQueryParam;
    private final boolean isTaggingMasking;
    private final boolean isParamHasForbidden;
    private final boolean isAuthorizationEnabled;
    private final String authClientId;
    private final String authSystemId;
    private final String authClientSecret;
    private final MigrationManager.Type nanuqReindex;
    private final String nanuqEsAnalysesIndex;
    private final String nanuqEsSequencingsIndex;

    public BioProperties(
            @Value("${bio.elasticsearch.enabled}") boolean isBioEsEnabled,
            @Value("${bio.elasticsearch.host}") String esHost,
            @Value("${bio.elasticsearch.port}") int esPort,
            @Value("${bio.elasticsearch.scheme}") String esScheme,
            @Value("${bio.elasticsearch.patients-index}") String esPatientsIndex,
            @Value("${bio.elasticsearch.prescriptions-index}") String esPrescriptionsIndex,
            @Value("${bio.auth.enabled}") boolean isAuthEnabled,
            @Value("${bio.auth.server-url}") String authServerUrl,
            @Value("${bio.auth.realm}") String authRealm,
            @Value("${bio.auth.retry}") Integer authRetry,
            @Value("${bio.auth.leeway}") Long authLeeway,
            @Value("${bio.auth.disable-ssl-validation}") boolean isDisableSslValidation,
            @Value("${bio.audits.enabled}") boolean isAuditsEnabled,
            @Value("${bio.tagging.enabled}") boolean isTaggingEnabled,
            @Value("${bio.tagging.queryParam}") boolean isTaggingQueryParam,
            @Value("${bio.tagging.masking}") boolean isTaggingMasking,
            @Value("${bio.tagging.param-has-forbidden}") boolean isParamHasForbidden,
            @Value("${bio.auth.authorization.enabled}") boolean isAuthorizationEnabled,
            @Value("${bio.auth.authorization.client-id}") String authClientId,
            @Value("${bio.auth.authorization.client-secret}") String authClientSecret,
            @Value("${bio.auth.authorization.system-id}") String authSystemId,
            @Value("${bio.nanuq.reindex}") MigrationManager.Type nanuqReindex,
            @Value("${bio.nanuq.analyses-index}") String nanuqEsAnalysesIndex,
            @Value("${bio.nanuq.sequencings-index}") String nanuqEsSequencingsIndex

    ) {
        this.isBioEsEnabled = isBioEsEnabled;
        this.esHost = esHost;
        this.esPort = esPort;
        this.esScheme = esScheme;
        this.esPatientsIndex = esPatientsIndex;
        this.esPrescriptionsIndex = esPrescriptionsIndex;
        this.isAuthEnabled = isAuthEnabled;
        this.authServerUrl = authServerUrl;
        this.authRealm = authRealm;
        this.authRetry = authRetry;
        this.authLeeway = Optional.ofNullable(authLeeway).orElse(0L);
        this.isDisableSslValidation = isDisableSslValidation;
        this.isAuditsEnabled = isAuditsEnabled;
        this.isTaggingEnabled = isTaggingEnabled;
        this.isTaggingQueryParam = isTaggingQueryParam;
        this.isTaggingMasking = isTaggingMasking;
        this.isParamHasForbidden = isParamHasForbidden;
        this.isAuthorizationEnabled = isAuthorizationEnabled;
        this.authClientId = authClientId;
        this.authClientSecret = authClientSecret;
        this.authSystemId = authSystemId;
        this.nanuqReindex = nanuqReindex;
        this.nanuqEsAnalysesIndex = nanuqEsAnalysesIndex;
        this.nanuqEsSequencingsIndex = nanuqEsSequencingsIndex;
    }
}
