package opendap.auth;

import java.util.Vector;

/**
 *
 */
public class AuthGroup {
    private String _name;
    private Vector<AuthUser> _users;

    public AuthGroup(String name){
        _name = name;
        _users = new Vector<>();
    }

    public String name(){ return _name; }

    public boolean isMember(String userId, String authContext){
        for(AuthUser authUser : _users){
            if(authUser.matches(userId, authContext))
                return true;
        }
        return false;
    }

    void addUserPattern(String uidPatternString, String authContextPatternString){

        if(containsUserPattern(uidPatternString,authContextPatternString)) {
            return;
        }
        _users.add(new AuthUser(uidPatternString,authContextPatternString));
    }

    private boolean containsUserPattern(String uidPatternString, String authContextPatternString){
        for(AuthUser authUser : _users){
            if(authUser.isSamePattern(uidPatternString,authContextPatternString))
                return true;
        }
        return false;
    }
}
