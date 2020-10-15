package bio.ferlab.clin.validation.utils;

import java.util.regex.Pattern;
public class ValidationPatterns {
    public static Pattern NO_SPECIAL_CHARACTERS = Pattern.compile("^[a-zA-Z0-9- '\\u00C0-\\u00FF]*$");
}
