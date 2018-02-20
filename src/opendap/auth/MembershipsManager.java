/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2014 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.auth;

import org.jdom.Element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Created by ndp on 10/1/14.
 */
public class MembershipsManager {

    private static ConcurrentHashMap<String, Group> _groups;
    private static ConcurrentHashMap<String, HashSet<String>> _roles;
    static {
        _groups = new ConcurrentHashMap<>();
        _roles = new ConcurrentHashMap<>();
    }

    public static void init(Element config) throws ConfigurationException {

        for (Object o : config.getChildren("group")) {
            Element groupElem = (Element) o;
            addGroup(groupElem);
        }

        for (Object o : config.getChildren("role")) {
            Element roleElem = (Element) o;
            addRole(roleElem);
        }
    }

    private static void addGroup(Element groupElem) throws ConfigurationException {
        String gid = groupElem.getAttributeValue("id");
        if (gid == null) {
            throw new ConfigurationException("init() - Every <group> MUST have an \"id\" attribute.");
        }

        Group group = _groups.get(gid);
        if (group == null) {
            group = new Group(gid);
            _groups.put(gid, group);
        }

        Iterator userItr = groupElem.getChildren("user").iterator();
        if(!userItr.hasNext()){
            throw new ConfigurationException("init() - Every <group> MUST have at least one <user> element.");
        }

        while (userItr.hasNext()) {
            Element user = (Element) userItr.next();
            String uid = user.getAttributeValue("id");
            String uidPatternStr = user.getAttributeValue("idPattern");
            if ((uid==null && uidPatternStr == null) || (uid!=null && uidPatternStr!=null)) {
                throw new ConfigurationException("init(): Every <user> MUST have either an \"id\" attribute " +
                        "or an \"idPattern\" attribute, but NOT both.");
            }
            if(uid!=null){
                // We turn the uid string into a literal pattern to prevent it from actually being
                // interpreted as a pattern...
                uidPatternStr = Pattern.quote(uid);
            }

            String authContext = user.getAttributeValue("authContext");
            String authContextPatternStr = user.getAttributeValue("authContextPattern");
            if ((authContext==null && authContextPatternStr == null) || (authContext!=null && authContextPatternStr!=null)) {
                throw new ConfigurationException("init(): Every <user> MUST have either an \"authContext\" attribute " +
                        "or an \"authContextPattern\" attribute, but NOT both.");
            }

            if(authContext!=null){
                // We turn the authContext string into a literal pattern to prevent it from actually being
                // interpreted as a pattern...
                authContextPatternStr = Pattern.quote(authContext);
            }
            group.addUserPattern(uidPatternStr,authContextPatternStr);
        }



    }
    private static void addRole(Element roleElem) throws ConfigurationException {
        String rid = roleElem.getAttributeValue("id");
        if (rid == null) {
            throw new ConfigurationException("init(): Every <role> must have an \"id\" attribute.");
        }

        Iterator uItr = roleElem.getChildren("group").iterator();
        if(uItr.hasNext()){
            HashSet<String> members = _roles.get(rid);
            if (members == null) {
                members = new HashSet<>();
                _roles.put(rid, members);
            }
            while (uItr.hasNext()) {
                Element user = (Element) uItr.next();
                String gid = user.getAttributeValue("id");
                if (gid == null) {
                    throw new ConfigurationException("init(): Every <group> must have an \"id\" attribute.");
                }
                if (!members.contains(gid))
                    members.add(gid);
            }
        }
    }



    private static Vector<String> getUserGroups(String uid, String authContext){
        Vector<String> groupMemberships = new Vector<>();
        for(Group group: _groups.values()){
            if(group.isMember(uid, authContext)){
                groupMemberships.add(group.name());
            }
        }
        return groupMemberships;
    }

    public static HashSet<String> getUserRoles(String uid, String authContext){

        HashSet<String> userRoles = new HashSet<>();

        Vector<String> userGroups = getUserGroups(uid,authContext);
        for(String roleId: _roles.keySet()){
            HashSet<String> members = _roles.get(roleId);
            for(String gid: userGroups) {
                if (members.contains(gid)) {
                    userRoles.add(roleId);
                    break;
                }
            }
        }
         return userRoles;
    }

}
