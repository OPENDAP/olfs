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

import com.google.gson.*;
//import org.json.simple.JSONObject;

// import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile {

    protected Date   _objectCreationTime;
    protected JsonObject _jsonInit;
    protected HashSet<String> _groups;
    protected HashSet<String> _roles;

    protected IdProvider _idp;




    public UserProfile() {
        _objectCreationTime = new Date();
        _jsonInit = new JsonObject();
        _groups = new HashSet<>();
        _roles = new HashSet<>();
        _idp  = null;
    }

    public UserProfile(JsonObject json){
        this();

        Gson gson = new Gson();
        String  jsonString = gson.toJson(json);

        _jsonInit = gson.fromJson(jsonString, JsonObject.class);
    }


    public String getAttribute(String attrName){
        return _jsonInit.get(attrName).toString();
    }

    public void setAttribute(String attrName, String value){
         _jsonInit.add(attrName, new JsonPrimitive(value));
    }

    public String getUID() {
        return (String) _jsonInit.get("uid").getAsString();
    }

    public IdProvider getIdP(){
        return _idp;
    }
    public void setIdP(IdProvider idProvider){
        _idp = idProvider;
    }


    public void addGroups(HashSet<String> groupMemberships){
        _groups.addAll(groupMemberships);

    }

    public void addGroup(String group){
        _groups.add(group);

    }

    public void addRoles(HashSet<String> roles){
        _roles.addAll(roles);

    }
    public void addRole(String role){
        _roles.add(role);

    }


    public HashSet<String> getGroups(){
        return new HashSet<String>(_groups);
    }

    public HashSet<String> getRoles(){
        return new HashSet<String>(_roles);
    }


/**
    public String getAffiliation() {
        return (String) _jsonInit.get("affiliation");
    }

    public void setAffiliation(String s) {
        _jsonInit.put("affiliation", s);
    }

    public String getFirstName() {
        return (String) _jsonInit.get("first_name");
    }

    public void setFirstName(String s) {
        _jsonInit.put("first_name", s);
    }

    public String getStudyArea() {
        return (String) _jsonInit.get("study_area");
    }

    public void setStudyArea(String s) {
        _jsonInit.put("study_area", s);
    }


    public void setUID(String s) {
        _jsonInit.put("uid", s);
    }

    public String getUserType() {
        return (String) _jsonInit.get("user_type");
    }

    public void setUserType(String s) {
        _jsonInit.put("user_type", s);
    }

    public String getLastName() {
        return (String) _jsonInit.get("last_name");
    }

    public void setLastName(String s) {
        _jsonInit.put("last_name", s);
    }

    public String getEmailAddress() {
        return (String) _jsonInit.get("email_address");
    }

    public void setEmailAddress(String s) {
        _jsonInit.put("email_address", s);
    }

    public String getCountry() {
        return (String) _jsonInit.get("country");
    }

    public void setCountry(String s) {
        _jsonInit.put("country", s);
    }

 **/

    public String toString(){
        StringBuilder sb = new StringBuilder();
        Gson gson = new Gson();
        String  jsonString = gson.toJson(_jsonInit);
        com.google.gson.JsonObject externalRepresentation = gson.fromJson(jsonString, JsonObject.class);;

        JsonArray myGroups = gson.fromJson(gson.toJson(_groups),JsonArray.class);
        externalRepresentation.add("groups",myGroups);

        JsonArray myRoles = gson.fromJson(gson.toJson(_roles),JsonArray.class);
        externalRepresentation.add("roles",myRoles);

        sb.append("UserProfile:").append(externalRepresentation.toString());
        return sb.toString();


    }

}
