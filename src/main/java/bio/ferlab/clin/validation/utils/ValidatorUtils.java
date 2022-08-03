package bio.ferlab.clin.validation.utils;

import org.apache.commons.lang3.StringUtils;

public class ValidatorUtils {
    public static boolean isTrimmed(String value) {
        return StringUtils.isEmpty(value) || value.trim().equals(value);
    }

    public static boolean hasSpecialCharacters(String value) {
        return !ValidationPatterns.NO_SPECIAL_CHARACTERS.matcher(value).find();
    }

    public static boolean isValidRAMQ(String value) {
        return ValidationPatterns.VALID_RAMQ.matcher(value).find();
    }

}
