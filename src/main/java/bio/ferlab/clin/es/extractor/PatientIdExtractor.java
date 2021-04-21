package bio.ferlab.clin.es.extractor;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PatientIdExtractor extends IdExtractor {
    private final List<ExtractorHandler<? extends IBaseResource>> handlers = Arrays.asList(
            new ExtractorHandler<>(Patient.class, this::extractPatientId),
            new ExtractorHandler<>(Group.class, this::extractGroupMembers),
            new ExtractorHandler<>(Bundle.class, super::extractIdsFromBundle)
    );

    private Set<String> extractPatientId(Patient patient) {
        return Collections.singleton(patient.getIdElement().getIdPart());
    }

    private Set<String> extractGroupMembers(Group group) {
        return group.getMember().stream().map(member -> getIdFromReference(member.getEntity().getReference())).collect(Collectors.toSet());
    }

    private String getIdFromReference(String reference) {
        return reference.split("/")[1];
    }

    @Override
    protected List<ExtractorHandler<? extends IBaseResource>> getHandlers() {
        return this.handlers;
    }
}
