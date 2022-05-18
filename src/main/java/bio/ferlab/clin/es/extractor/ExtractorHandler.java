package bio.ferlab.clin.es.extractor;

import bio.ferlab.clin.es.IndexerHelper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

abstract class IdExtractor {
    protected abstract List<ExtractorHandler<? extends IBaseResource>> getHandlers();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Set<String> extract(IBaseResource resource) {
        for (ExtractorHandler handler : this.getHandlers()) {
            if (handler.tClass.isInstance(resource)) {
                return (Set<String>) handler.callback.apply(handler.tClass.cast(resource));
            }
        }
        return new HashSet<>();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Set<String> extractIdsFromBundle(Bundle bundle) {
        final Set<String> ids = new HashSet<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (IndexerHelper.isModifiableBundleEntry(entry)) {
                final Resource resource = entry.getResource();
                for (ExtractorHandler handler : this.getHandlers()) {
                    if (handler.tClass.isInstance(resource)) {
                        ids.addAll((Set<String>) handler.callback.apply(handler.tClass.cast(resource)));
                    }
                }
            }
        }
        return ids;
    }

    protected static class ExtractorHandler<T extends IBaseResource> {
        public final Class<T> tClass;
        public final Function<T, Set<String>> callback;

        public ExtractorHandler(Class<T> tClass, Function<T, Set<String>> callback) {
            this.tClass = tClass;
            this.callback = callback;
        }
    }
}
