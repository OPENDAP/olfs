/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2018 OPeNDAP, Inc.
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

import java.util.*;

//import org.json.simple.JSONObject;
// import java.util.ArrayList;

/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile {

    private Date   _objectCreationTime;
    private String _jsonStr;
    private JsonObject _profile;
    private HashSet<String> _groups;
    private HashSet<String> _roles;

    private IdProvider _idp;

    private OAuth2AccessToken _token;


    public UserProfile() {
        _objectCreationTime = new Date();
        _profile = new JsonObject();
        _groups = new HashSet<>();
        _roles = new HashSet<>();
        _idp  = null;
        _token = null;
    }

    /**
     *  Parse the json to extract the user id, first and last names,
     * and email address. We store these in the session. These four
     * parameters are mandatory, and will always exist in the user
     * profile.
     * @param jsonStr
     */
    public UserProfile(String jsonStr){
        this();
        JsonParser jparse = new JsonParser();
        _profile = jparse.parse(jsonStr).getAsJsonObject();
        _jsonStr = jsonStr;

    }

    public void setOAuth2Token(OAuth2AccessToken oat){
        _token = new OAuth2AccessToken(oat);
    }

    public OAuth2AccessToken getOAuth2Token(){
        return new OAuth2AccessToken(_token);
    }


    public String getAttribute(String attrName){
        JsonElement val =  _profile.get(attrName);
        if(val==null)
            return null;
        return val.toString();
    }

    public void setAttribute(String attrName, String value){
         _profile.add(attrName, new JsonPrimitive(value));
    }

    public Vector<String> getAttributeNames(){
        Vector<String> keys = new Vector<>();
        for(Map.Entry<String,JsonElement> e: _profile.entrySet()){
            keys.add(e.getKey());
        }
        return keys;
    }

    public String getUID() {
        return _profile.get("uid").getAsString();
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
        return (String) _profile.get("affiliation");
    }

    public void setAffiliation(String s) {
        _profile.put("affiliation", s);
    }

    public String getFirstName() {
        return (String) _profile.get("first_name");
    }

    public void setFirstName(String s) {
        _profile.put("first_name", s);
    }

    public String getStudyArea() {
        return (String) _profile.get("study_area");
    }

    public void setStudyArea(String s) {
        _profile.put("study_area", s);
    }


    public void setUID(String s) {
        _profile.put("uid", s);
    }

    public String getUserType() {
        return (String) _profile.get("user_type");
    }

    public void setUserType(String s) {
        _profile.put("user_type", s);
    }

    public String getLastName() {
        return (String) _profile.get("last_name");
    }

    public void setLastName(String s) {
        _profile.put("last_name", s);
    }

    public String getEmailAddress() {
        return (String) _profile.get("email_address");
    }

    public void setEmailAddress(String s) {
        _profile.put("email_address", s);
    }

    public String getCountry() {
        return (String) _profile.get("country");
    }

    public void setCountry(String s) {
        _profile.put("country", s);
    }

 **/

    public String toString(){
        return toString("","    ");
    }


    public String toString(String indent, String indent_inc){
        StringBuilder sb = new StringBuilder();

        String l1i = indent +indent_inc;

        sb.append(indent).append("\"").append(getClass().getName()).append("\" : {");

        boolean comma = false;
        for(Map.Entry<String,JsonElement> e: _profile.entrySet()) {
            sb.append(comma?",\n":"\n");
            sb.append(l1i).append("\"").append(e.getKey()).append("\" : ").append(e.getValue());
            comma =true;
        }
        sb.append(indent).append("\n");
        if(_token!=null){
            sb.append(_token.toString(l1i,indent_inc));
        }
        sb.append(indent).append("}\n");
        return sb.toString();
    }



    public static void main(String args[]){
        String ursUserProfile = "{\"uid\":\"ndp_opendap\",\"first_name\":\"Nathan\",\"last_name\":\"Potter\",\"registered_date\":\"23 Sep 2014 17:33:09PM\",\"email_address\":\"ndp@opendap.org\",\"country\":\"United States\",\"study_area\":\"Other\",\"user_type\":\"Public User\",\"affiliation\":\"Non-profit\",\"authorized_date\":\"24 Oct 2017 15:01:18PM\",\"allow_auth_app_emails\":true,\"agreed_to_meris_eula\":false,\"agreed_to_sentinel_eula\":false,\"user_groups\":[],\"user_authorized_apps\":2}";

        UserProfile up = new UserProfile(ursUserProfile);
        up.setOAuth2Token(new OAuth2AccessToken());
        System.out.println(up.toString());

    }

}
