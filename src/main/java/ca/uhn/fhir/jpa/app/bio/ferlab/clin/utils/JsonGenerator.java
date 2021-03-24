package bio.ferlab.clin.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

@Component
public class JsonGenerator {
    private final IParser fhirParser;
    private final ObjectMapper objectParser;

    public JsonGenerator(FhirContext context) {
        this.fhirParser = context.newJsonParser();
        this.objectParser = new ObjectMapper();
    }

    public String toString(Object object) {
        try {
            return this.objectParser.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ca.uhn.fhir.rest.server.exceptions.InvalidRequestException("Failed to parse object");
        }
    }

    public String toString(Resource resource) {
        return this.fhirParser.encodeResourceToString(resource);
    }
}
