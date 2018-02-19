package opendap.auth;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 *
 */
public class Group {
    String _name;
    ConcurrentSkipListSet<User> _users;

    public Group(String name){
        _name = name;
        _users = new ConcurrentSkipListSet<>();
    }



    public boolean isMember(String userId, String authContext){
        for(User user: _users){
            if(user.matches(userId, authContext))
                return true;
        }
        return false;
    }

    void addUserPattern(String uidPatternString, String authContextPatternString){

        if(containsUserPattern(uidPatternString,authContextPatternString))
            return;

        _users.add(new User(uidPatternString,authContextPatternString));
    }

    boolean containsUserPattern(String uidPatternString, String authContextPatternString){
        for(User user: _users){
            if(user.isSamePattern(uidPatternString,authContextPatternString))
                return true;
        }
        return false;
    }



}
