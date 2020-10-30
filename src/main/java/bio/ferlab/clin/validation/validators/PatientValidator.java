package bio.ferlab.clin.validation.validators;

import bio.ferlab.clin.validation.utils.ValidatorUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PatientValidator extends SchemaValidator<Patient> {
    public PatientValidator() {
        super(Patient.class);
    }

    @Override
    public boolean validateResource(Patient patient) {
        return validateNames(patient) &&
                validateBirthDate(patient) &&
                validateMRN(patient) &&
                validateRAMQ(patient);
    }

    private boolean validateBirthDate(Patient patient) {
        return patient.hasBirthDate() && isValidBirthDate(patient.getBirthDate());
    }

    private boolean isValidBirthDate(Date date) {
        final Date today = new Date();
        if (DateUtils.isSameDay(date, today)) {
            return true;
        } else return date.before(today);
    }

    private boolean validateMRN(Patient patient) {
        if (patient.getIdentifier().isEmpty()) {
            return false;
        }

        final Identifier identifier = patient.getIdentifier().get(0);
        final String mrn = identifier.getValue();

        return mrn != null &&
                mrn.length() > 0 &&
                ValidatorUtils.isTrimmed(mrn) &&
                !ValidatorUtils.hasSpecialCharacters(mrn);
    }

    private boolean validateRAMQ(Patient patient) {
        if (patient.getIdentifier().size() < 2) {
            return true;
        }
        final Identifier identifier = patient.getIdentifier().get(1);
        final String ramq = identifier.getValue();
        return ramq != null &&
                ValidatorUtils.isTrimmed(ramq) &&
                ValidatorUtils.isValidRAMQ(ramq);
    }

    private boolean validateNames(Patient patient) {
        final List<HumanName> names = patient.getName();

        if (names.size() == 0) {
            return false;
        }

        return names.stream().allMatch(this::validateName);
    }

    private boolean validateName(HumanName name) {
        final String family = name.getFamilyElement().getValue();
        final List<String> givens = name.getGiven().stream().map(StringType::getValue).collect(Collectors.toList());

        return isValidName(family) && givens.stream().allMatch(this::isValidName);
    }

    private boolean isValidName(String name) {
        return name != null &&
                name.length() > 2 &&
                !ValidatorUtils.hasSpecialCharacters(name) &&
                ValidatorUtils.isTrimmed(name);
    }
}
