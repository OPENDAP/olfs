package opendap.auth;

import java.util.regex.Pattern;

/**
 * Every user has a user ID and an authentication context from which the user ID was
 * derived/acquired.
 */
class AuthUser {

    private String _uidPatternString;
    private Pattern _uidPattern;

    private String _authContextPatternString;
    private Pattern _authContextPattern;

    public AuthUser(String uidPatternString, String authContextPatternString){
        _uidPatternString = uidPatternString;
        _uidPattern = Pattern.compile(_uidPatternString);
        _authContextPatternString = authContextPatternString;
        _authContextPattern = Pattern.compile(authContextPatternString);
    }

    public boolean matches(String userId, String authContext ){
        return _uidPattern.matcher(userId).matches() && _authContextPattern.matcher(authContext).matches();
    }

    public boolean isSamePattern(String uidPatternString, String authContextPatternString ){
        return _uidPatternString.equals(uidPatternString) && _authContextPatternString.equals(authContextPatternString);
    }
}
