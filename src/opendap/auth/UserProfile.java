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

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;


/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile implements Serializable {

    //private static final String CLIENT_HOST_KEY = "client-host";
    private static final String USER_AGENT_KEY = "User-Agent";

    /* @serial */
    private Date objectCreationTime;
    /* @serial */
    private String d_jsonStr;
    /* @serial */
    private HashSet<String> d_groups;
    /* @serial */
    private HashSet<String> d_roles;

    /* @serial */
    private String d_authContext;
    /* @serial */
    private EarthDataLoginAccessToken d_edlAccessToken;

    /* @serial */
    private String d_uid;

    private transient JsonObject d_profile;

    // private String edlClientAppId;

    private String d_clientIp;
    private String d_clientUserAgent;

    public UserProfile() {
        objectCreationTime = new Date();
        d_groups = new HashSet<>();
        d_roles = new HashSet<>();

        d_clientIp = null;
        d_clientUserAgent = null;
        d_profile = null;
        d_authContext = null;
        d_edlAccessToken = null;
        // edlClientAppId ="";
        d_uid = null;
    }

    public UserProfile(HttpServletRequest request) {
        this();
        setUserFootPrint(request);
    }

    /**
     *  Parse the json to extract the user id, first and last names,
     * and email address. We store these in the session. These four
     * parameters are mandatory, and will always exist in the user
     * d_profile.
     * @param jsonStr
     */
    public UserProfile(String jsonStr){
        this();
        ingestJsonProfileString(jsonStr);
    }

    private JsonObject getProfile(){
        if(d_profile == null && d_jsonStr != null){
            ingestJsonProfileString();
        }
        return d_profile;
    }

    public void ingestJsonProfileString(String jsonStr){
        d_jsonStr = jsonStr;
        ingestJsonProfileString();
    }

    private  void ingestJsonProfileString(){
        JsonParser jparse = new JsonParser();
        d_profile = jparse.parse(d_jsonStr).getAsJsonObject();
        JsonElement uid = d_profile.get("uid");
        d_uid = uid.getAsString();
    }

    public void setEDLAccessToken(EarthDataLoginAccessToken oat){
        d_edlAccessToken = new EarthDataLoginAccessToken(oat);
    }

    // public void setEDLClientAppId(String clientAppId){ edlClientAppId = clientAppId; }

    // public String getEDLClientAppId(){ return edlClientAppId; }

    public EarthDataLoginAccessToken getEDLAccessToken(){
        if(d_edlAccessToken ==null)
            return null;

        return new EarthDataLoginAccessToken(d_edlAccessToken);
    }


    public String getAttribute(String attrName){
        JsonObject profile = getProfile();
        if(profile != null) {
            JsonElement val = profile.get(attrName);
            if (val == null)
                return null;
            return val.toString();
        }
        return null;
    }

    public void setAttribute(String attrName, String value){
        JsonObject profile = getProfile();
        if(profile !=null) {
            profile.add(attrName, new JsonPrimitive(value));
        }
    }

    public Vector<String> getAttributeNames(){
        Vector<String> keys = new Vector<>();
        JsonObject profile = getProfile();
        if(profile !=null) {
            for (Map.Entry<String, JsonElement> e : profile.entrySet()) {
                keys.add(e.getKey());
            }
        }
        return keys;
    }

    public String getUID() {
        return d_uid;
    }

    public void setUID(String user_id) {
        d_uid = user_id;
    }

    public IdProvider getIdP(){
        return IdPManager.getProvider(d_authContext);
    }
    public void setAuthContext(String context){
        d_authContext = context;
    }


    public void addGroups(HashSet<String> groupMemberships){
        d_groups.addAll(groupMemberships);

    }

    public void addGroup(String group){
        d_groups.add(group);

    }

    public void addRoles(HashSet<String> roles){
        this.d_roles.addAll(roles);

    }
    public void addRole(String role){
        d_roles.add(role);

    }


    public HashSet<String> getGroups(){
        return new HashSet<String>(d_groups);
    }

    public HashSet<String> getRoles(){
        return new HashSet<String>(d_roles);
    }


    /**
     * Collects user specific information from the request for later examination.
     * @param request The request to exam,ine.
     */
    private void setUserFootPrint(HttpServletRequest request) {
        d_clientIp =  request.getRemoteHost();
        d_clientUserAgent = request.getHeader(USER_AGENT_KEY);
    }

    /**
     *
     * @param request The request to validate.
     * @return True if the request matches the existing client footprint, false otherwise
     */
    boolean validateUserFootPrint(HttpServletRequest request) {
        String s = request.getRemoteHost();
        if( d_clientIp == null ) {
            return false;
        }
        else if( !d_clientIp.equals(s) ) {
            return false;
        }

        s = request.getHeader(USER_AGENT_KEY);
        if( d_clientUserAgent == null ) {
            return false;
        }
        else if( !d_clientUserAgent.equals(s) ) {
            return false;
        }

        return true;
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
        _profile.put("d_uid", s);
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

        JsonObject profile = getProfile();
        if(profile != null) {
            boolean comma = false;
            for (Map.Entry<String, JsonElement> e : profile.entrySet()) {
                sb.append(comma ? ",\n" : "\n");
                sb.append(l1i).append("\"").append(e.getKey()).append("\" : ").append(e.getValue());
                comma = true;
            }
            sb.append(indent).append("\n");
        }
        if(d_edlAccessToken !=null){
            sb.append(d_edlAccessToken.toString(l1i,indent_inc));
        }
        sb.append(indent).append("}\n");
        return sb.toString();
    }



    public static void main(String args[]){
        String ursUserProfile = "{\"d_uid\":\"ndp_opendap\",\"first_name\":\"Nathan\",\"last_name\":\"Potter\",\"registered_date\":\"23 Sep 2014 17:33:09PM\",\"email_address\":\"ndp@opendap.org\",\"country\":\"United States\",\"study_area\":\"Other\",\"user_type\":\"Public User\",\"affiliation\":\"Non-profit\",\"authorized_date\":\"24 Oct 2017 15:01:18PM\",\"allow_auth_app_emails\":true,\"agreed_to_meris_eula\":false,\"agreed_to_sentinel_eula\":false,\"user_groups\":[],\"user_authorized_apps\":2}";

        UserProfile up = new UserProfile(ursUserProfile);
        up.setEDLAccessToken(new EarthDataLoginAccessToken());
        System.out.println(up.toString());

    }

}
