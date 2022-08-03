package bio.ferlab.clin.validation.validators;

import bio.ferlab.clin.utils.Extensions;
import bio.ferlab.clin.validation.utils.ValidatorUtils;
import org.apache.commons.lang3.EnumUtils;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ObservationValidator extends SchemaValidator<Observation> {
    private static final String AGE_AT_ONSET_PREFIX_VALUE = "HP:";
    public static final String ETHNICITY_SYSTEM = "http://fhir.cqgc.ferlab.bio/CodeSystem/qc-ethnicity";

    private enum SupportedCodesEnum {
        CGH,
        INDIC,
        PHENO,
        INVES,
        ETH,
        CONS,
    }

    private enum PhenotypeInterpretationEnum {
        NEG,
        POS,
        IND,
    }

    public ObservationValidator() {
        super(Observation.class);
    }

    @Override
    public List<String> validateResource(Observation resource) {
        List<String> errors = new ArrayList<>();
        final String resourceCode = getCode(resource);
        final SupportedCodesEnum code = EnumUtils.getEnum(SupportedCodesEnum.class, resourceCode);
        if (resourceCode == null || code == null) {
            this.formatError(errors, resource, "Missing code");
        } else {
            switch (code) {
                case CGH:
                    if (!validateCgh(resource)) this.formatError(errors, resource, "Invalid CGH");
                    break;
                case INDIC:
                    if (!validateIndications(resource)) this.formatError(errors, resource, "Invalid indications");
                    break;
                case PHENO:
                    if (!validatePhenotype(resource)) this.formatError(errors, resource, "Invalid phenotype");
                    break;
                case INVES:
                    if (!validateInvestigations(resource)) this.formatError(errors, resource, "Invalid investigations");
                    break;
                case ETH:
                    if (!validateEthnicity(resource)) this.formatError(errors, resource, "Invalid ethnicity");
                    break;
                case CONS:
                    if (!validateConsumption(resource)) this.formatError(errors, resource, "Invalid consumption");
                    break;
                default:
                    this.formatError(errors, resource, "Invalid code");
            }
        }
        return errors;
    }

    private boolean validateCgh(Observation resource) {
        // Make sure the note is valid if present
        return validateNote(resource);
    }

    private boolean validateIndications(Observation resource) {
        return validateNote(resource);
    }

    private boolean validatePhenotype(Observation resource) {
        final boolean validInterpretation = resource.hasInterpretation() &&
                resource.getInterpretation().size() == 1 &&
                isValidPhenotypeInterpretation(resource.getInterpretation().get(0));

        return validInterpretation && hasValidAgAtOnset(resource) && validateNote(resource);
    }

    private boolean hasValidAgAtOnset(Observation observation) {
        if (!observation.hasExtension(Extensions.AGE_AT_ONSET)) {
            return true;
        }
        final Coding coding = (Coding) observation.getExtensionByUrl(Extensions.AGE_AT_ONSET).getValue();
        return coding.getCode().startsWith(AGE_AT_ONSET_PREFIX_VALUE);
    }

    private boolean isValidPhenotypeInterpretation(CodeableConcept interpretation) {
        if (interpretation.isEmpty() || interpretation.getCoding().size() != 1) {
            return false;
        }

        return EnumUtils.isValidEnum(PhenotypeInterpretationEnum.class, interpretation.getCoding().get(0).getCode());
    }

    private boolean validateNote(Observation resource) {
        if (resource.hasNote()) {
            final Annotation note = resource.getNote().get(0);
            return !note.isEmpty() &&
                    note.getText() != null &&
                    ValidatorUtils.isTrimmed(note.getText());
        }

        return true;
    }

    private boolean validateInvestigations(Observation resource) {
        return validateNote(resource);
    }

    private boolean validateEthnicity(Observation resource) {
        try{
            final CodeableConcept concept = (CodeableConcept) resource.getValue();
            return !concept.getCoding().isEmpty() && concept.getCoding().get(0).getSystem().contentEquals(ETHNICITY_SYSTEM);
        }catch(Exception e){
            return false;
        }
    }

    private boolean validateConsumption(Observation resource) {
        try{
            final BooleanType concept = (BooleanType) resource.getValue();
            return true;
        }catch(Exception e){
            return false;
        }
    }

    private String getCode(Observation resource) {
        final CodeableConcept code = resource.getCode();
        if (code.isEmpty() || code.getCoding().isEmpty()) {
            return null;
        }
        final Coding coding = code.getCoding().get(0);
        return coding.getCode();
    }
}
