package bio.ferlab.clin.es;

import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.builder.PatientDataBuilder;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PatientDataExtractor {
    private final PatientDataBuilder patientDataBuilder;

    public PatientDataExtractor(PatientDataBuilder patientDataBuilder) {
        this.patientDataBuilder = patientDataBuilder;
    }

    public List<PatientData> extract(Bundle bundle) {
        final List<Bundle> bundles = new ArrayList<>();

        if (bundle.getId().contentEquals(HapiProperties.getBioEsPatientBundleId())) {
            bundles.addAll(this.fromCreatePatientBundle(bundle));
        } else {
            bundles.add(bundle);
        }

        return bundles.stream().map(this.patientDataBuilder::fromBundle).collect(Collectors.toList());
    }

    private List<Bundle> fromCreatePatientBundle(Bundle bundle) {
        final long patientCount = bundle.getEntry().stream().filter(entry -> entry.getResource() instanceof Patient).count();
        if (patientCount > 1) {
            final Bundle secondBundle = bundle.copy();
            secondBundle.getEntry().remove(1);
            return Arrays.asList(bundle, secondBundle);
        }
        return Collections.singletonList(bundle);
    }
}
