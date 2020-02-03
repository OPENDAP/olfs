package opendap.auth;

import java.util.regex.Pattern;

/**
 * Every user has a user ID and an authentication context from which the user ID was
 * derived/acquired.
 */
class AuthUser {

    private String uidPatternString;
    private Pattern uidPattern;

    private String authContextPatternString;
    private Pattern authContextPattern;

    public AuthUser(String uidPatternString, String authContextPatternString){
        this.uidPatternString = uidPatternString;
        uidPattern = Pattern.compile(this.uidPatternString);
        this.authContextPatternString = authContextPatternString;
        authContextPattern = Pattern.compile(authContextPatternString);
    }

    public boolean matches(String userId, String authContext ){
        return uidPattern.matcher(userId).matches() && authContextPattern.matcher(authContext).matches();
    }

    public boolean isSamePattern(String uidPatternString, String authContextPatternString ){
        return this.uidPatternString.equals(uidPatternString) && this.authContextPatternString.equals(authContextPatternString);
    }
}
