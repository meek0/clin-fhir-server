package bio.ferlab.clin.es;

import bio.ferlab.clin.es.ElasticsearchRestClient.IndexData;
import bio.ferlab.clin.es.data.ElasticsearchData;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.context.FhirContext;
import org.apache.http.HttpEntity;
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

import static org.mockito.Mockito.*;

public class ElasticsearchRestClientTest {
    private static final String INDEX_NAME = "test";

    @Mock
    private RestClient client;
    @Captor
    private ArgumentCaptor<HttpEntity> httpEntityCaptor;

    private Patient patient;

    private JsonGenerator jsonGenerator;

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

        this.jsonGenerator = new JsonGenerator(FhirContext.forR4());
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
            public void shouldMakeAsyncCall() throws IOException {
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), jsonGenerator.toString(patient));
                elasticsearchRestClient.index(INDEX_NAME, data);
                verify(client, times(1))
                        .performRequest(any());
            }


            @Test
            @DisplayName("Should make the correct request")
            public void shouldMakeCorrectRequest() throws IOException {
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), jsonGenerator.toString(patient));
                elasticsearchRestClient.index(INDEX_NAME, data);
                verify(client, times(1))
                        .performRequest(any());
            }
        }


        @Nested
        @DisplayName("ElasticsearchRestClient::Delete function")
        class Delete {
            @Test
            @DisplayName("Should make an async call to the Elasticsearch API")
            public void shouldMakeAsyncCall() throws IOException {
                elasticsearchRestClient.delete(INDEX_NAME, patient.getIdElement().getIdPart());
                verify(client, times(1))
                        .performRequest(any());
            }

            @Test
            @DisplayName("Should make the correct delete request")
            public void shouldMakeCorrectRequest() throws IOException {
                elasticsearchRestClient.delete(INDEX_NAME, patient.getIdElement().getIdPart());
                verify(client, times(1))
                        .performRequest(any());
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
                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), jsonGenerator.toString(patient));

                try {
                    when(client.performRequest(any())).thenThrow(new IOException());

                    Assertions.assertThrows(
                            ca.uhn.fhir.rest.server.exceptions.InternalErrorException.class,
                            () -> elasticsearchRestClient.index(INDEX_NAME, data)
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        @Nested
        @DisplayName("ElasticsearchRestClient::Delete function")
        class Delete {
            @Test
            @DisplayName("Should handle any exception during request")
            public void shouldHandleAnyException() {
                try {
                    when(client.performRequest(any())).thenThrow(new IOException());

                    Assertions.assertThrows(
                            ca.uhn.fhir.rest.server.exceptions.InternalErrorException.class,
                            () -> elasticsearchRestClient.delete(INDEX_NAME, patient.getIdElement().getIdPart())
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
