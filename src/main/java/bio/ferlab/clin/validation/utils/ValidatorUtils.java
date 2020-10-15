package bio.ferlab.clin.validation.utils;

public class ValidatorUtils {
    public static boolean isTrimmed(String value) {
        if (value.length() == 0) {
            return true;
        }
        return value.charAt(0) != ' ' && value.charAt(value.length() - 1) != ' ';
    }

    public static boolean hasSpecialCharacters(String value){
        return !ValidationPatterns.NO_SPECIAL_CHARACTERS.matcher(value).find();
    }
}
