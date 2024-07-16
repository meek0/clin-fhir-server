package bio.ferlab.clin.es;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.List;

public class IndexerHelper {
    public static boolean isIndexable(IBaseResource resource) {
        return resource instanceof Patient ||
                resource instanceof ServiceRequest ||
                resource instanceof Group ||
                resource instanceof Task ||
                resource instanceof Bundle && isModifierBundle((Bundle) resource);
    }

    public static boolean isModifierBundle(Bundle bundle) {
        return bundle.getEntry().stream().findFirst().filter(IndexerHelper::isModifiableBundleEntry).isPresent();
    }

    public static boolean isModifiableBundleEntry(Bundle.BundleEntryComponent entry) {
        return entry.getRequest().getMethod() == Bundle.HTTPVerb.PUT || entry.getRequest().getMethod() == Bundle.HTTPVerb.POST;
    }

    public static Bundle generateUpdatedIdsBundle(Bundle bundle, Bundle responseBundle){
        final Bundle output = bundle.copy();
        final List<Bundle.BundleEntryComponent> entries = output.getEntry();
        for (int i = 0; i < entries.size(); i++) {
            final String id = responseBundle.getEntry().get(i).getResponse().getLocation().split("/")[1];
            entries.get(i).getResource().setId(id);
        }
        return output;
    }
}
