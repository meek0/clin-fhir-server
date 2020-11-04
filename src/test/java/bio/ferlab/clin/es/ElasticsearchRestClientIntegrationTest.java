package bio.ferlab.clin.es;

import bio.ferlab.clin.es.ElasticsearchRestClient.IndexData;
import bio.ferlab.clin.es.data.ElasticsearchData;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElasticsearchRestClientIntegrationTest {
    @Nested
    @DisplayName("ElasticsearchRestClient::Index")
    class Index {
        @Test
        @DisplayName("Should create a patient resource in ES")
        public void shouldCreatePatient() {
            try (ElasticsearchContainer container = new ElasticsearchContainer()) {
                container.start();
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials("elastic", "changeme"));
                final RestClient client = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                        .setHttpClientConfigCallback(
                                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        ).build();

                client.performRequest("PUT", "/patient/");
                final JsonGenerator jsonGenerator = new JsonGenerator(FhirContext.forR4());
                final ElasticsearchRestClient restClient = new ElasticsearchRestClient(
                        new ElasticsearchData(client, container.getHttpHostAddress())
                );

                final Patient patient = new Patient();
                patient.addIdentifier().setSystem("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-patient").setValue("12345");
                patient.addName().setFamily("Test").addGiven("Test").addGiven("Testing");
                patient.setIdElement(IdType.newRandomUuid());
                patient.setGender(Enumerations.AdministrativeGender.MALE);
                patient.setId(IdType.newRandomUuid());

                final IndexData data = new IndexData(patient.getIdElement().getIdPart(), jsonGenerator.toString(patient));
                restClient.index("patient", data);

                Thread.sleep(1000);
                final Response response = client.performRequest("GET", "/patient/_search");
                final String content = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                final Map<String, Object> parsed = new ObjectMapper().readValue(content, new TypeReference<Map<String, Object>>() {});
                final Map<String, Object> hits = (Map<String, Object>) parsed.get("hits");
                final int total = (int) hits.get("total");
                final ArrayList<Map<String, Object>> hitsHits = (ArrayList<Map<String, Object>>) hits.get("hits");

                final String id = (String) hitsHits.get(0).get("_id");
                Assertions.assertEquals(total, 1);
                Assertions.assertTrue(id.contentEquals(patient.getIdElement().getIdPart()));
            } catch (Exception e) {
                Assertions.fail(e.getLocalizedMessage());
            }
        }
    }

    @Nested
    @DisplayName("ElasticsearchRestClient::Delete")
    class Delete{
        @Test
        @DisplayName("Should delete the patient instance from the container")
        public void shouldDeleteInstance(){
            try (ElasticsearchContainer container = new ElasticsearchContainer()) {
                container.start();
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials("elastic", "changeme"));
                final RestClient client = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                        .setHttpClientConfigCallback(
                                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        ).build();

                client.performRequest("PUT", "/patient/");
                ElasticsearchRestClient restClient = new ElasticsearchRestClient(
                        new ElasticsearchData(client, container.getHttpHostAddress())
                );

                final Patient patient = new Patient();
                patient.addIdentifier().setSystem("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-patient").setValue("12345");
                patient.addName().setFamily("Test").addGiven("Test").addGiven("Testing");
                patient.setIdElement(IdType.newRandomUuid());
                patient.setGender(Enumerations.AdministrativeGender.MALE);
                patient.setId(IdType.newRandomUuid());

                final String indexContent = FhirContext.forR4().newJsonParser().encodeResourceToString(patient);
                final String id = patient.getIdElement().getIdPart();
                client.performRequest(
                        HttpMethod.PUT.name(),
                        String.format("/%s/_doc/%s", "patient", id),
                        new HashMap<>(),
                        new NStringEntity(indexContent, ContentType.APPLICATION_JSON)
                );

                restClient.delete("patient", patient.getIdElement().getIdPart());
                Thread.sleep(1000);

                final Response response = client.performRequest("GET", "/patient/_search");
                final String content = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                final Map<String, Object> parsed = new ObjectMapper().readValue(content, new TypeReference<Map<String, Object>>() {});
                final Map<String, Object> hits = (Map<String, Object>) parsed.get("hits");
                final int total = (int) hits.get("total");

                Assertions.assertEquals(total, 0);
            } catch (Exception e) {
                Assertions.fail(e.getLocalizedMessage());
            }
        }
    }
}
