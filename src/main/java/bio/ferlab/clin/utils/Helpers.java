package bio.ferlab.clin.utils;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class Helpers {

    @NotNull
    public static String extractAccessTokenFromBearer(String bearer) {
        String authorization = null;
        if (StringUtils.isNotBlank(bearer)) {
            authorization = bearer.split(" ")[1];
        }
        if (authorization == null) {
            throw new AuthenticationException("Missing bearer token in header");
        }

        return authorization;
    }
}
