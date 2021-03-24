package bio.ferlab.clin.validation.utils;

public class ValidatorUtils {
    public static boolean isTrimmed(String value) {
        if (value.length() == 0) {
            return true;
        }
        return !Character.isWhitespace(value.charAt(0)) && !Character.isWhitespace(value.charAt(value.length() - 1));
    }

    public static boolean hasSpecialCharacters(String value) {
        return !ValidationPatterns.NO_SPECIAL_CHARACTERS.matcher(value).find();
    }

    public static boolean isValidRAMQ(String value) {
        return ValidationPatterns.VALID_RAMQ.matcher(value).find();
    }

}
