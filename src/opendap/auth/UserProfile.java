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
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;


/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile implements Serializable {

    /* @serial */
    private final Date d_objectCreationTime;
    /* @serial */
    private String edlProfileJsonStr;
    /* @serial */
    private final HashSet<String> groups;
    /* @serial */
    private final HashSet<String> roles;

    /* @serial */
    private String authContext;
    /* @serial */
    private EarthDataLoginAccessToken edlAccessToken;

    /* @serial */
    private String uid;

    /* The transient tag tells the serializer to skip this variable.*/
    private transient JsonObject tEdlProfile;


    public String cerealize(){
        Gson gson = new Gson();

        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        //sb.append("\"d_objectCreationTime\": ").append(d_objectCreationTime.getTime()).append(", ");
        sb.append("\"d_uid\":\"").append(uid).append("\", ");
        sb.append("\"d_authContext\":\"").append(authContext).append("\", ");
        sb.append("\"d_EdlProfileJsonStr\": ").append(gson.toJson(edlProfileJsonStr)).append(", ");
        sb.append("\"d_edlAccessToken\": " + edlAccessToken.cerealize()).append(", ");
        sb.append("\"d_groups\": [");

        boolean first = true;
        for(String group : groups){
            if(!first){ sb.append(", "); }
            sb.append("\"").append(group).append("\"");
            first = false;
        }
        sb.append("],");

        sb.append("\"d_roles\": [");
        first = true;
        for(String role : roles){
            if(!first){ sb.append(", "); }
            sb.append("\"").append(role).append("\"");
            first = false;
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }
    public static UserProfile decerealize(String jsonStr) {
        UserProfile up = new UserProfile();
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        up.uid = json.get("d_uid").getAsString();
        up.authContext = json.get("d_authContext").getAsString();
        up.edlProfileJsonStr = json.get("d_EdlProfileJsonStr").getAsString();

         JsonElement edlAccessTokenElement  = json.get("d_edlAccessToken");
         jsonStr = edlAccessTokenElement.toString();
         up.edlAccessToken = EarthDataLoginAccessToken.decerealize(jsonStr);


        json.get("d_groups").getAsJsonArray().forEach(group -> up.groups.add(group.getAsString()));
        json.get("d_roles").getAsJsonArray().forEach(role -> up.roles.add(role.getAsString()));
        return up;
    }

    public UserProfile() {
        d_objectCreationTime = new Date();
        groups = new HashSet<>();
        roles = new HashSet<>();

        tEdlProfile = null;
        authContext = null;
        edlAccessToken = null;
        uid = null;
    }

    /**
     *  Parse the json to extract the user id, first and last names,
     * and email address. We store these in the session. These four
     * parameters are mandatory, and will always exist in the user
     * d_profile.
     * @param jsonStr \
     */
    public UserProfile(String jsonStr){
        this();
        ingestEDLProfileStringJson(jsonStr);
    }

    private JsonObject getEdlProfile(){
        if(tEdlProfile == null && edlProfileJsonStr != null){
            ingestEDLProfileStringJson(edlProfileJsonStr);
        }
        return tEdlProfile;
    }

    public void ingestEDLProfileStringJson(String jsonStr){
        edlProfileJsonStr = jsonStr;
        tEdlProfile = JsonParser.parseString(edlProfileJsonStr).getAsJsonObject();
        JsonElement uid = tEdlProfile.get("uid");
        this.uid = uid.getAsString();
    }

    public void setEDLAccessToken(EarthDataLoginAccessToken oat){
        edlAccessToken = new EarthDataLoginAccessToken(oat);
    }

    // public void setEDLClientAppId(String clientAppId){ edlClientAppId = clientAppId; }

    // public String getEDLClientAppId(){ return edlClientAppId; }

    public EarthDataLoginAccessToken getEDLAccessToken(){
        if(edlAccessToken ==null)
            return null;

        return new EarthDataLoginAccessToken(edlAccessToken);
    }


    public String getEdlProfileAttribute(String attrName){
        JsonObject profile = getEdlProfile();
        if(profile != null) {
            JsonElement val = profile.get(attrName);
            if (val == null)
                return null;
            return val.toString();
        }
        return null;
    }

    protected void setEdlProfileAttribute(String attrName, String value){
        JsonObject profile = getEdlProfile();
        if(profile !=null) {
            profile.add(attrName, new JsonPrimitive(value));
        }
    }

    public Vector<String> getEdlProfileAttributeNames(){
        Vector<String> keys = new Vector<>();
        JsonObject profile = getEdlProfile();
        if(profile !=null) {
            for (Map.Entry<String, JsonElement> e : profile.entrySet()) {
                keys.add(e.getKey());
            }
        }
        return keys;
    }

    public String getUID() {
        return uid;
    }

    public void setUID(String user_id) {
        uid = user_id;
    }

    public IdProvider getIdP(){
        return IdPManager.getProvider(authContext);
    }
    public void setAuthContext(String context){
        authContext = context;
    }


    protected void addGroups(HashSet<String> groupMemberships){
        groups.addAll(groupMemberships);
    }

    protected void addGroup(String group){
        groups.add(group);
    }

    protected void addRoles(HashSet<String> roles){
        this.roles.addAll(roles);
    }
    protected void addRole(String role){
        roles.add(role);
    }


    public HashSet<String> getGroups(){
        return new HashSet<>(groups);
    }

    public HashSet<String> getRoles(){
        return new HashSet<>(roles);
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
        String l2i = l1i +indent_inc;
        String classname = getClass().getName();
        sb.append(indent).append(classname).append(": \n");
        sb.append(l1i).append("d_objectCreationTime: ").append(/*d_objectCreationTime*/"ELIDED").append(",\n");
        sb.append(l1i).append("d_uid: ").append(uid).append(",\n");

        sb.append(l1i).append("d_jsonStr: ").append(edlProfileJsonStr).append(",\n");
        sb.append(l1i).append("d_groups: ").append(groups).append(",\n");
        sb.append(l1i).append("d_roles: ").append(roles).append(",\n");
        sb.append(l1i).append("d_authContext: ").append(authContext).append(",\n");

        sb.append(l1i).append("").append("edl_profile").append(": \n");
        JsonObject profile = getEdlProfile();
        if(profile != null) {
            boolean comma = false;
            for (Map.Entry<String, JsonElement> e : profile.entrySet()) {
                sb.append(l2i).append(e.getKey().replace("\"",""));
                sb.append(": ");
                JsonElement value = e.getValue();
                if(value !=null && !value.isJsonNull()){
                    if(value.isJsonArray()){
                        sb.append(e.getValue().getAsJsonArray().toString());
                    }
                    else {
                        sb.append(e.getValue().getAsString());
                    }
                    //sb.append((e.getValue()!=null && !e.getValue().isJsonNull())?e.getValue().getAsString():"");
                    sb.append("\n");

                }
                else {
                    sb.append("null");
                }
                comma = true;
            }
            sb.append(indent).append("\n");
        }
        if(edlAccessToken !=null){
            sb.append(edlAccessToken.toString(l2i,indent_inc));
        }
        sb.append(l1i).append("\n");
        sb.append(indent).append("\n");
        return sb.toString();
    }

    /**
     *
     * @return This object serialized as a json string by gson.
     */
    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * @param o The object, o, to serialize as a json string.
     * @return The object, o, serialized as a json string by gson.
     */
    public static String toJson(Object o){
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    /**
     *
     * @param jsonStr A string containing the gson json serialization of an instance of the UserProfile class.
     * @return A UserProfile class built from the passed jsonStr.
     */
    public static UserProfile fromJson(String jsonStr){
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, UserProfile.class);
    }
    private static String javaTest(UserProfile up){
        System.out.println(hr0);
        System.out.println("Java Native serialize and deserialize user profile...");
        System.out.println("Calling UserProfile.toString() on instance of UserProfile:");
        byte serializedObject[] = null;
        String result = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            System.out.println(hr0);
            System.out.println("Serializing instance of UserProfile...");
            oos.writeObject(up);
            serializedObject = new byte[baos.size()];
            serializedObject = baos.toByteArray();
            System.out.println("Serialized data is " + serializedObject.length + " bytes.");
            oos.close();
            baos.close();


        }
        catch (IOException ioe){
            ioe.printStackTrace();
            System.exit(1);
        }
        try {
            if(serializedObject != null){
                System.out.println(hr0);
                System.out.println("Deserializing instance of UserProfile...");
                ByteArrayInputStream bais = new ByteArrayInputStream(serializedObject);
                ObjectInputStream ois = new ObjectInputStream(bais);
                UserProfile deserializedObj = (UserProfile) ois.readObject();
                ois.close();
                bais.close();
                System.out.println("Calling UserProfile.toString() on deserialized UserProfile:");
                result = deserializedObj.toString();
                System.out.print(result);
            }
            else{
                throw new IOException("serialized object is null");
            }
        }
        catch (ClassNotFoundException | IOException ioe){
            ioe.printStackTrace();
        }
        return result;
    }
    private static String gsonTest(UserProfile up){
        System.out.println(hr0);
        System.out.println("GSON serialize and deserialize user profile...");
        String jsonStr1 = UserProfile.toJson(up);
        String jsonStr2 = up.toJson();
        if(!jsonStr1.equals(jsonStr2)){
            System.out.println("The gson json serializations do not match!");
            System.out.println("jsonStr1: " + jsonStr1);
            System.out.println("jsonStr2: " + jsonStr2);
            System.exit(1);
        }

        System.out.println("UserProfile.toJson(): ");
        System.out.println(jsonStr1);

        System.out.println(hr1);
        System.out.println("UserProfile.fromJson().toString(): ");
        UserProfile fromJsonStr = UserProfile.fromJson(jsonStr1);
        String result = fromJsonStr.toString();
        System.out.println(result);

        return result;
    }

    private static String cerealTest(UserProfile up){
        System.out.println(hr0);
        System.out.println("Primitive (string only) cerealize and decerealize using Gson user profile...");

        String jsonStr1 = up.cerealize();
        System.out.println("UserProfile.cerealize(): ");
        System.out.println(jsonStr1);

        System.out.println(hr1);
        UserProfile fromDecerealize = UserProfile.decerealize(jsonStr1);
        String result = fromDecerealize.toString();
        System.out.println("UserProfile.decerealize().toString(): ");
        System.out.println(result);
        return result;
    }

    private static boolean compare(String baseline, String result){
        baseline = baseline.trim();
        result = result.trim();
        boolean testResult = result != null && result.equals(baseline);
        if(testResult){
            System.out.println("PASS - Result matched baseline.");
        }
        else {
            int index = StringUtils.indexOfDifference(baseline,result);
            String remainder = StringUtils.difference(baseline,result);
            System.out.println("!! FAIL - Result did not match baseline. Result differs at index: " + index);
            System.out.println("!!    baseline.length(): " + baseline.length());
            System.out.println("!!      result.length(): " + result.length());
            System.out.println("!!   remainder.length(): " + remainder.length());
            System.out.println("!! Result differs at index: " + index);
            System.out.println("!! Remainder: ");
            System.out.println(remainder);

        }
        return testResult;
    }

    private static final String hr0 = "################################################";
    private static final String hr1 = "------------------------------------------------";

    public static void main(String[] args){
        boolean success = true;
        String baseline;
        String result;
        String edlUserProfile = "{\"uid\":\"moo\",\"first_name\":\"Imma\",\"last_name\":\"Cow\",\"registered_date\":\"23 Sep 1985 14:63:34PM\",\"email_address\":\"imma.cow@opendap.org\",\"country\":\"United States\",\"study_area\":\"Other\",\"user_type\":\"Public User\",\"affiliation\":\"Non-profit\",\"authorized_date\":\"15 Aug 1998 10:12:37PM\",\"allow_auth_app_emails\":true,\"agreed_to_meris_eula\":false,\"agreed_to_sentinel_eula\":false,\"user_groups\":[],\"user_authorized_apps\":2}";

        // --------------------------------------------------------------------
        // Make the UserProfile instance to test.
        UserProfile up = new UserProfile(edlUserProfile);
        //up.d_clientIp = "10.7.0.1";
        //up.d_clientUserAgent = "ImmaTestHarness";
        up.setAuthContext("TestyTesty");
        up.setEDLAccessToken(new EarthDataLoginAccessToken());
        up.addGroup("fiddle");
        up.addGroup("faddle");
        up.addRole("twiddle");
        up.addRole("piddle");
        baseline = up.toString();

        System.out.println(hr0);
        System.out.println("BASELINE - UserProfile.toString():");
        System.out.print(baseline);

        // --------------------------------------------------------------------
        result = cerealTest(up);
        success = compare(baseline, result) && success;

        // --------------------------------------------------------------------
        result = gsonTest(up);
        success = compare(baseline, result) && success;
        // --------------------------------------------------------------------
        result = cerealTest(up);
        success = compare(baseline, result) && success;

        // --------------------------------------------------------------------
        result = javaTest(up);
        success = compare(baseline, result) && success;

        // --------------------------------------------------------------------

        if(!success){
            System.out.println("FAILURE - One or more UserProfile Tests FAILED.");
            System.exit(1);
        }

        System.out.println("SUCCESS - All UserProfile Tests Passed.");
        System.exit(0);
    }


}
