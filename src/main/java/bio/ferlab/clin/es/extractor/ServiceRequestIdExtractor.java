package bio.ferlab.clin.es.extractor;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class ServiceRequestIdExtractor extends IdExtractor {
    private final List<ExtractorHandler<? extends IBaseResource>> handlers = Arrays.asList(
            new ExtractorHandler<>(ServiceRequest.class, this::extractServiceRequestId),
            new ExtractorHandler<>(Bundle.class, super::extractIdsFromBundle)
    );

    private Set<String> extractServiceRequestId(ServiceRequest serviceRequest) {
        return Collections.singleton(serviceRequest.getIdElement().getIdPart());
    }

    @Override
    protected List<ExtractorHandler<? extends IBaseResource>> getHandlers() {
        return this.handlers;
    }
}
