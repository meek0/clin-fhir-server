package bio.ferlab.clin.validation.validators;

import bio.ferlab.clin.validation.utils.ValidatorUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PatientValidator extends SchemaValidator<Patient> {
    public static final String RAMQ_CODE = "JHN";
    public static final String MRN_CODE = "MR";

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
        return isValidBirthDate(patient.getBirthDate());
    }

    private boolean isValidBirthDate(Date date) {
        if (date == null) {
            return true;
        }
        final Date today = new Date();
        if (DateUtils.isSameDay(date, today)) {
            return true;
        } else return date.before(today);
    }

    private boolean validateMRN(Patient patient) {
        if (patient.getIdentifier().isEmpty()) {
            return false;
        }

        return patient.getIdentifier().stream().filter(identifier -> identifier.getType().getCoding().get(0).getCode().contentEquals(MRN_CODE)).map(Identifier::getValue).noneMatch(mrn -> mrn == null || mrn.length() == 0 || !ValidatorUtils.isTrimmed(mrn) || ValidatorUtils.hasSpecialCharacters(mrn));
    }

    private boolean validateRAMQ(Patient patient) {
        final Optional<Identifier> optionalRamq =
                patient.getIdentifier().stream().filter(id -> id.getType().getCoding().get(0).getCode().contentEquals(RAMQ_CODE)).findFirst();
        if (optionalRamq.isPresent()) {
            final Identifier identifier = optionalRamq.get();
            final String ramq = identifier.getValue();
            return ramq != null &&
                    ValidatorUtils.isTrimmed(ramq) &&
                    ValidatorUtils.isValidRAMQ(ramq);
        }
        return true;
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
                name.length() >= 2 &&
                !ValidatorUtils.hasSpecialCharacters(name) &&
                ValidatorUtils.isTrimmed(name);
    }
}
