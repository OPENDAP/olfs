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

    public static void addGroup(Element groupElem) throws ConfigurationException {
        String gid = groupElem.getAttributeValue("id");
        if (gid == null) {
            throw new ConfigurationException("init(): Every <group> must have an \"id\" attribute.");
        }

        Iterator uItr = groupElem.getChildren("user").iterator();
        if(uItr.hasNext()){
            Group group = _groups.get(gid);
            if (group == null) {
                group = new Group(gid);
                _groups.put(gid, group);
            }

            while (uItr.hasNext()) {
                Element user = (Element) uItr.next();
                String uid = user.getAttributeValue("id");
                String uidPatternStr = user.getAttributeValue("idPattern");
                if ((uid==null && uidPatternStr == null) || (uid!=null && uidPatternStr!=null)) {
                    throw new ConfigurationException("init(): Every <user> MUST have either an \"id\" attribute " +
                            "or an \"idPattern\" attribute, but NOT both.");
                }

                String authContext = user.getAttributeValue("authContext");
                String authContextPatternStr = user.getAttributeValue("authContextPattern");
                if (authContext == null) {
                    throw new ConfigurationException("init(): Every <user> must have an \"authContext\" attribute.");
                }
                if ((authContext==null && authContextPatternStr == null) || (authContext!=null && authContextPatternStr!=null)) {
                    throw new ConfigurationException("init(): Every <user> MUST have either an \"authContext\" attribute " +
                            "or an \"authContextPattern\" attribute, but NOT both.");
                }

                if(uid!=null){
                    uidPatternStr = Pattern.quote(uid);
                }
                if(authContext!=null){
                    authContextPatternStr = Pattern.quote(authContext);
                }
                group.addUserPattern(uidPatternStr,authContextPatternStr);
            }

        }


    }
    public static void addRole(Element roleElem) throws ConfigurationException {
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



    public static HashSet<String> getUserGroups(String uid, String authContext){

        HashSet<String> groupMemberships = new HashSet<>();
        for(Group group: _groups.values()){
            if(group.isMember(uid, authContext)){
                groupMemberships.add(group._name);
            }
        }
        return groupMemberships;
    }

    public static HashSet<String> getUserRoles(String uid, String authContext){

        HashSet<String> userGroups = getUserGroups(uid,authContext);

        HashSet<String> roles = new HashSet<>();
        for(String rid: _roles.keySet()){
            HashSet<String> members = _roles.get(rid);

            for(String gid: userGroups) {
                if (members.contains(gid)) {
                    roles.add(rid);
                    break;
                }
            }
        }
         return roles;
    }

}
