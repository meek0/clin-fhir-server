package bio.ferlab.clin.es;

import bio.ferlab.clin.es.ElasticsearchRestClient.IndexData;
import bio.ferlab.clin.es.data.ElasticsearchData;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.context.FhirContext;
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
import static org.mockito.Mockito.*;

public class ElasticsearchRestClientTest {
    private static final String INDEX_NAME = "test";

    @Mock
    private RestClient client;
    @Captor
    private ArgumentCaptor<ResponseListener> responseListenerCaptor;
    @Captor
    private ArgumentCaptor<HttpEntity> httpEntityCaptor;

    private Patient patient;

    private JsonGenerator parser;

    private ElasticsearchRestClient elasticsearchRestClient;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.patient = new Patient();
        this.patient.addIdentifier()
                .setSystem("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-patient")
                .setValue("12345");
        this.patient.addName()
                .setFamily("Test")
                .addGiven("Test")
                .addGiven("Testing");
        this.patient.setIdElement(IdType.newRandomUuid());
        this.patient.setGender(Enumerations.AdministrativeGender.MALE);
        this.patient.setId(IdType.newRandomUuid());

        this.parser = new JsonGenerator(FhirContext.forR4());
        this.elasticsearchRestClient = new ElasticsearchRestClient(new ElasticsearchData(this.client, "localhost"));
    }

    @Nested
    @DisplayName("Valid")
    class ValidRequest {
        @Nested
        @DisplayName("ElasticsearchRestClient::Index function")
        class Index {
            @Test
            @DisplayName("Should make an async call to the Elasticsearch API")
            public void shouldMakeAsyncCall() {
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), parser.toString(patient));
                elasticsearchRestClient.index(INDEX_NAME, data);
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
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), parser.toString(patient));
                elasticsearchRestClient.index(INDEX_NAME, data);
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

            @Test
            @DisplayName("Should make the correct request")
            public void shouldMakeCorrectRequest() {
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), parser.toString(patient));
                elasticsearchRestClient.index(INDEX_NAME, data);
                verify(client, times(1))
                        .performRequestAsync(
                                eq("PUT"),
                                eq(String.format("/%s/_doc/%s", INDEX_NAME, patient.getIdElement().getIdPart())),
                                anyMapOf(String.class, String.class),
                                httpEntityCaptor.capture(),
                                responseListenerCaptor.capture()
                        );
            }
        }


        @Nested
        @DisplayName("ElasticsearchRestClient::Delete function")
        class Delete {
            @Test
            @DisplayName("Should make an async call to the Elasticsearch API")
            public void shouldMakeAsyncCall() {
                elasticsearchRestClient.delete(INDEX_NAME, patient.getIdElement().getIdPart());
                verify(client, times(1))
                        .performRequestAsync(
                                anyString(),
                                anyString(),
                                anyMapOf(String.class, String.class),
                                responseListenerCaptor.capture()
                        );
            }

            @Test
            @DisplayName("Should make the correct delete request")
            public void shouldMakeCorrectRequest() {
                elasticsearchRestClient.delete(INDEX_NAME, patient.getIdElement().getIdPart());
                verify(client, times(1))
                        .performRequestAsync(
                                eq("DELETE"),
                                eq(String.format("/%s/_doc/%s", INDEX_NAME, patient.getIdElement().getIdPart())),
                                anyMapOf(String.class, String.class),
                                responseListenerCaptor.capture()
                        );
            }
        }
    }


    @Nested
    @DisplayName("Invalid request")
    class InvalidRequest {
        @Nested
        @DisplayName("ElasticsearchRestClient::Index function")
        class Index {
            @Test
            @DisplayName("Should handle any exception during request")
            public void shouldHandleAnyException() {
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), parser.toString(patient));
                elasticsearchRestClient.index(INDEX_NAME, data);
                doAnswer(invocationOnMock -> {
                    verify(responseListenerCaptor.getValue(), times(1))
                            .onFailure(any(Exception.class));
                    throw new RuntimeException("Exception");
                }).when(client).performRequestAsync(
                        anyString(),
                        anyString(),
                        anyMapOf(String.class, String.class),
                        httpEntityCaptor.capture(),
                        responseListenerCaptor.capture()
                );
            }
        }


        @Nested
        @DisplayName("ElasticsearchRestClient::Delete function")
        class Delete {
            @Test
            @DisplayName("Should handle any exception during request")
            public void shouldHandleAnyException() {
                elasticsearchRestClient.delete(INDEX_NAME, patient.getIdElement().getIdPart());
                doAnswer(invocationOnMock -> {
                    verify(responseListenerCaptor.getValue(), times(1))
                            .onFailure(any(Exception.class));
                    throw new RuntimeException("Exception");
                }).when(client).performRequestAsync(
                        anyString(),
                        anyString(),
                        anyMapOf(String.class, String.class),
                        responseListenerCaptor.capture()
                );
            }
        }
    }
}
