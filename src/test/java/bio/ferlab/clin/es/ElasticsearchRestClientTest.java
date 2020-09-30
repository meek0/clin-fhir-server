package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.ElasticsearchData;
import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ElasticsearchRestClientTest {
    @Mock
    private RestClient client;
    @Captor
    private ArgumentCaptor<ResponseListener> responseListenerCaptor;
    @Captor
    private ArgumentCaptor<HttpEntity> httpEntityCaptor;

    private Patient patient;

    private ElasticsearchRestClient elasticsearchRestClient;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.patient = new Patient();

        Patient patient = new Patient();
        patient.addIdentifier()
                .setSystem("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-patient")
                .setValue("12345");
        patient.addName()
                .setFamily("Test")
                .addGiven("TTest")
                .addGiven("Testing");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setId(IdType.newRandomUuid());

        this.elasticsearchRestClient = new ElasticsearchRestClient(new ElasticsearchData(this.client, "localhost", ""));
    }

    @Nested
    @DisplayName("Valid")
    class ValidRequest {
        @Nested
        @DisplayName("RestClient::Index function")
        class Index {
            @Test
            @DisplayName("Should make an async call to the Elasticsearch API")
            public void shouldMakeAsyncCall() {
                elasticsearchRestClient.index("test", patient);
                verify(client, times(1))
                        .performRequestAsync(
                                anyString(),
                                anyString(),
                                anyMapOf(String.class, String.class),
                                httpEntityCaptor.capture(),
                                responseListenerCaptor.capture()
                        );
            }

            @Test
            @DisplayName("Should encode the resource to JSON")
            public void shouldEncodeResourceToJson() {
                elasticsearchRestClient.index("test", patient);
                verify(client, times(1))
                        .performRequestAsync(
                                anyString(),
                                anyString(),
                                anyMapOf(String.class, String.class),
                                httpEntityCaptor.capture(),
                                responseListenerCaptor.capture()
                        );
                try {
                    final String encodedPatient = IOUtils.toString(httpEntityCaptor.getValue().getContent(), Charset.defaultCharset());
                    final String ourEncodedPatient = FhirContext.forR4().newJsonParser().encodeResourceToString(patient);
                    Assertions.assertEquals(encodedPatient, ourEncodedPatient);
                } catch (IOException e) {
                    Assertions.fail(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
    }

}
