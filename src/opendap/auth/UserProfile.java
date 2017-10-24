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
import java.util.Map;

/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile {

    protected Date   _objectCreationTime;
    protected String _jsonStr;
    protected JsonObject _jsonInit;
    protected HashSet<String> _groups;
    protected HashSet<String> _roles;

    protected IdProvider _idp;




    public UserProfile() {
        _objectCreationTime = new Date();
        _jsonInit = new JsonObject();
        _groups = new HashSet<String>();
        _roles = new HashSet<String>();
        _idp  = null;
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
        _jsonInit = jparse.parse(jsonStr).getAsJsonObject();
        _jsonStr = jsonStr;

    }

    public String getAttribute(String attrName){
        JsonElement val =  _jsonInit.get(attrName);
        if(val==null)
            return null;
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
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("\n");
        sb.append("UID: " + getUID()).append("\n");
        sb.append(keyValStr("first_name")).append("\n");
        sb.append(keyValStr("last_name")).append("\n");
        sb.append(keyValStr("registered_date")).append("\n");
        sb.append(keyValStr("email_address")).append("\n");
        sb.append(keyValStr("country")).append("\n");
        sb.append(keyValStr("study_area")).append("\n");
        sb.append(keyValStr("Other")).append("\n");
        sb.append(keyValStr("affiliation")).append("\n");
        sb.append(keyValStr("authorized_date")).append("\n");
        sb.append(keyValStr("allow_auth_app_emails")).append("\n");
        sb.append(keyValStr("agreed_to_meris_eula")).append("\n");
        sb.append(keyValStr("agreed_to_sentinel_eula")).append("\n");
        sb.append(keyValStr("user_authorized_apps")).append("\n");
        sb.append(keyValStr("user_groups")).append("\n");
        /*

        Gson gson = new Gson();

        JsonObject externalRepresentation = gson.fromJson(_jsonInit, JsonObject.class);

        if(externalRepresentation.isJsonArray()) {
            JsonArray jsonArray = externalRepresentation.getAsJsonArray();
            for(JsonElement jsonElement: jsonArray){
                sb.append(jsonElement.getAsString());
            }
        }
        else if(externalRepresentation.isJsonPrimitive()){

        }
        else if(externalRepresentation.isJsonObject()){
            for(Map.Entry<String,JsonElement> mapE: externalRepresentation.entrySet()){
                sb.append("\"").append(mapE.getKey()).append("\": ");
                if(mapE.getValue().isJsonArray()){
                    JsonArray values = mapE.getValue().getAsJsonArray();
                    sb.append("[");
                    for(JsonElement jse : values){
                        if(!jse.isJsonArray())
                            sb.append(jse.getAsString()).append(",");
                    }
                    sb.append("], ");
                }
                else {
                    sb.append(mapE.getValue().getAsString()).append(",");
                }

            }
        }
        */
        return sb.toString();


    }


    public static void main(String args[]){
        String ursUserProfile = "{\"uid\":\"ndp_opendap\",\"first_name\":\"Nathan\",\"last_name\":\"Potter\",\"registered_date\":\"23 Sep 2014 17:33:09PM\",\"email_address\":\"ndp@opendap.org\",\"country\":\"United States\",\"study_area\":\"Other\",\"user_type\":\"Public User\",\"affiliation\":\"Non-profit\",\"authorized_date\":\"24 Oct 2017 15:01:18PM\",\"allow_auth_app_emails\":true,\"agreed_to_meris_eula\":false,\"agreed_to_sentinel_eula\":false,\"user_groups\":[],\"user_authorized_apps\":2}";

        UserProfile up = new UserProfile(ursUserProfile);
        System.out.println(up.toString());

    }
    private String keyValStr(String attributeName){
        return attributeName + ": " + getAttribute(attributeName);

    }

}
