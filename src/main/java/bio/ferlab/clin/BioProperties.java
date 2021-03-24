package bio.ferlab.clin;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bio")
@Data
public class BioProperties {
	private final String esHost;
	private final int esPort;
	private final String esScheme;
	private final String esPatientIndex;
	private final boolean esEnabled;

	public BioProperties(
		@Value("${bio.elasticsearch.host}")
		String esHost,
		@Value("${bio.elasticsearch.port}")
		int esPort,
		@Value("${bio.elasticsearch.scheme}")
		String esScheme,
		@Value("${bio.elasticsearch.patient-index}")
		String esPatientIndex,
		@Value("${bio.elasticsearch.enabled}")
		boolean esEnabled) {
		this.esHost = esHost;
		this.esPort = esPort;
		this.esScheme = esScheme;
		this.esPatientIndex = esPatientIndex;
		this.esEnabled = esEnabled;
	}
}
