package bio.ferlab.clin.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UserData {
    @JsonProperty("email")
    private String email;
    @JsonProperty("preferred_username")
    private String username;
    @JsonProperty("name")
    private String name;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("email_verified")
    private boolean isVerified;
    @JsonProperty("fhir_practitioner_id")
    private String practitionerId;
    @JsonProperty("family_name")
    private String familyName;
    @JsonProperty("given_name")
    private String givenName;
    @JsonProperty("resource_access")
    private Access access;
    @JsonProperty("realm_access")
    private Account realmAccount;
    @JsonProperty("azp")
    private String azp;

    @Data
    public static class Access {
        @JsonProperty("account")
        private Account account;
    }

    @Data
    public static class Account {
        @JsonProperty("roles")
        private String[] roles;
    }
}
