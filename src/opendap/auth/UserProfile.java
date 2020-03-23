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

import java.io.Serializable;
import java.util.*;


/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile implements Serializable {

    private Date objectCreationTime; // Serializable.
    private String jsonStr; // Serializable.
    private JsonObject profile;   // NOT Serializable?
    private HashSet<String> groups; // Serializable.
    private HashSet<String> roles;  // Serializable.

    private IdProvider _idp; //Serializable

    private EarthDataLoginAccessToken _token; // Serializable

    private String edlClientAppId; // Serializable.


    public UserProfile() {
        objectCreationTime = new Date();
        profile = new JsonObject();
        groups = new HashSet<>();
        roles = new HashSet<>();
        _idp  = null;
        _token = null;
        edlClientAppId ="";
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
        profile = jparse.parse(jsonStr).getAsJsonObject();
        this.jsonStr = jsonStr;

    }

    public void setEDLAuthToken(EarthDataLoginAccessToken oat){
        _token = new EarthDataLoginAccessToken(oat);
    }

    public void setEDLClientAppId(String clientAppId){
        edlClientAppId = clientAppId;
    }
    public String getEDLClientAppId(){
        return edlClientAppId;
    }

    public EarthDataLoginAccessToken getEDLAuthToken(){
        return new EarthDataLoginAccessToken(_token);
    }


    public String getAttribute(String attrName){
        JsonElement val =  profile.get(attrName);
        if(val==null)
            return null;
        return val.toString();
    }

    public void setAttribute(String attrName, String value){
        profile.add(attrName, new JsonPrimitive(value));
    }

    public Vector<String> getAttributeNames(){
        Vector<String> keys = new Vector<>();
        for(Map.Entry<String,JsonElement> e: profile.entrySet()){
            keys.add(e.getKey());
        }
        return keys;
    }

    public String getUID() {
        return profile.get("uid").getAsString();
    }

    public IdProvider getIdP(){
        return _idp;
    }
    public void setIdP(IdProvider idProvider){
        _idp = idProvider;
    }


    public void addGroups(HashSet<String> groupMemberships){
        groups.addAll(groupMemberships);

    }

    public void addGroup(String group){
        groups.add(group);

    }

    public void addRoles(HashSet<String> roles){
        this.roles.addAll(roles);

    }
    public void addRole(String role){
        roles.add(role);

    }


    public HashSet<String> getGroups(){
        return new HashSet<String>(groups);
    }

    public HashSet<String> getRoles(){
        return new HashSet<String>(roles);
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
        for(Map.Entry<String,JsonElement> e: profile.entrySet()) {
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
        up.setEDLAuthToken(new EarthDataLoginAccessToken());
        System.out.println(up.toString());

    }

}
