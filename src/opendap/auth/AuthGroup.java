package opendap.auth;

import java.util.Vector;

/**
 *
 */
public class AuthGroup {
    private String name;
    private Vector<AuthUser> users;

    public AuthGroup(String name){
        this.name = name;
        users = new Vector<>();
    }

    public String name(){ return name; }

    public boolean isMember(String userId, String authContext){
        for(AuthUser authUser : users){
            if(authUser.matches(userId, authContext))
                return true;
        }
        return false;
    }

    void addUserPattern(String uidPatternString, String authContextPatternString){

        if(containsUserPattern(uidPatternString,authContextPatternString)) {
            return;
        }
        users.add(new AuthUser(uidPatternString,authContextPatternString));
    }

    private boolean containsUserPattern(String uidPatternString, String authContextPatternString){
        for(AuthUser authUser : users){
            if(authUser.isSamePattern(uidPatternString,authContextPatternString))
                return true;
        }
        return false;
    }
}
