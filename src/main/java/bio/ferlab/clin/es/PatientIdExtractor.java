package bio.ferlab.clin.es;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PatientIdExtractor {
    private final List<Handler<? extends IBaseResource>> handlers = Arrays.asList(
            new Handler<>(Patient.class, this::extractPatientId),
            new Handler<>(ServiceRequest.class, this::extractServiceRequestSubject),
            new Handler<>(ClinicalImpression.class, this::extractClinicalImpressionSubject),
            new Handler<>(Group.class, this::extractGroupMembers),
            new Handler<>(Bundle.class, this::extractIdsFromBundle)
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Set<String> extract(IBaseResource resource) {
        for (Handler handler : this.handlers) {
            if (handler.tClass.isInstance(resource)) {
                return (Set<String>) handler.callback.apply(handler.tClass.cast(resource));
            }
        }
        return null;
    }

    private Set<String> extractPatientId(Patient patient) {
        return Collections.singleton(patient.getIdElement().getIdPart());
    }

    private Set<String> extractServiceRequestSubject(ServiceRequest serviceRequest) {
        return Collections.singleton(getIdFromReference(serviceRequest.getSubject().getReference()));
    }

    private Set<String> extractClinicalImpressionSubject(ClinicalImpression clinicalImpression) {
        return Collections.singleton(getIdFromReference(clinicalImpression.getSubject().getReference()));
    }

    private Set<String> extractGroupMembers(Group group) {
        return group.getMember().stream().map(member -> getIdFromReference(member.getEntity().getReference())).collect(Collectors.toSet());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<String> extractIdsFromBundle(Bundle bundle) {
        final Set<String> ids = new HashSet<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (IndexerHelper.isModifiableBundleEntry(entry)) {
                final Resource resource = entry.getResource();
                for (Handler handler : handlers) {
                    if (handler.tClass.isInstance(resource)) {
                        ids.addAll((Set<String>) handler.callback.apply(handler.tClass.cast(resource)));
                    }
                }
            }
        }
        return ids;
    }

    private String getIdFromReference(String reference) {
        return reference.split("/")[1];
    }

    private static class Handler<T extends IBaseResource> {
        public final Class<T> tClass;
        public final Function<T, Set<String>> callback;

        public Handler(Class<T> tClass, Function<T, Set<String>> callback) {
            this.tClass = tClass;
            this.callback = callback;
        }
    }
}
