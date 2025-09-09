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

import java.io.*;
import java.util.*;


/**
 * Created by ndp on 9/25/14.
 */
public class UserProfile implements Serializable {

    /* @serial */
    private final Date d_objectCreationTime;
    /* @serial */
    private String d_EdlProfileJsonStr;
    /* @serial */
    private final HashSet<String> d_groups;
    /* @serial */
    private final HashSet<String> d_roles;

    /* @serial */
    private String d_authContext;
    /* @serial */
    private EarthDataLoginAccessToken d_edlAccessToken;

    /* @serial */
    private String d_uid;

    /* The transient tag tells the serializer to skip this variable.*/
    private transient JsonObject d_EdlProfile;


    public UserProfile() {
        d_objectCreationTime = new Date();
        d_groups = new HashSet<>();
        d_roles = new HashSet<>();

        d_EdlProfile = null;
        d_authContext = null;
        d_edlAccessToken = null;
        d_uid = null;
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
        if(d_EdlProfile == null && d_EdlProfileJsonStr != null){
            ingestEDLProfileStringJson(d_EdlProfileJsonStr);
        }
        return d_EdlProfile;
    }

    public void ingestEDLProfileStringJson(String jsonStr){
        d_EdlProfileJsonStr = jsonStr;
        d_EdlProfile = JsonParser.parseString(d_EdlProfileJsonStr).getAsJsonObject();
        JsonElement uid = d_EdlProfile.get("uid");
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


    protected void addGroups(HashSet<String> groupMemberships){
        d_groups.addAll(groupMemberships);
    }

    protected void addGroup(String group){
        d_groups.add(group);
    }

    protected void addRoles(HashSet<String> roles){
        d_roles.addAll(roles);
    }
    protected void addRole(String role){
        d_roles.add(role);
    }


    public HashSet<String> getGroups(){
        return new HashSet<>(d_groups);
    }

    public HashSet<String> getRoles(){
        return new HashSet<>(d_roles);
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
        sb.append(l1i).append("d_objectCreationTime: ").append(d_objectCreationTime).append(",\n");
        sb.append(l1i).append("d_uid: ").append(d_uid).append(",\n");

        sb.append(l1i).append("d_jsonStr: ").append(d_EdlProfileJsonStr).append(",\n");
        sb.append(l1i).append("d_groups: ").append(d_groups).append(",\n");
        sb.append(l1i).append("d_roles: ").append(d_roles).append(",\n");
        sb.append(l1i).append("d_authContext: ").append(d_authContext).append(",\n");

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
        if(d_edlAccessToken !=null){
            sb.append(d_edlAccessToken.toString(l2i,indent_inc));
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

    public static void main(String[] args){
        String edlUserProfile = "{\"uid\":\"moo\",\"first_name\":\"Imma\",\"last_name\":\"Cow\",\"registered_date\":\"23 Sep 1985 14:63:34PM\",\"email_address\":\"imma.cow@opendap.org\",\"country\":\"United States\",\"study_area\":\"Other\",\"user_type\":\"Public User\",\"affiliation\":\"Non-profit\",\"authorized_date\":\"15 Aug 1998 10:12:37PM\",\"allow_auth_app_emails\":true,\"agreed_to_meris_eula\":false,\"agreed_to_sentinel_eula\":false,\"user_groups\":[],\"user_authorized_apps\":2}";
        String hr0 = "################################################";
        String hr1 = "------------------------------------------------";

        UserProfile up = new UserProfile(edlUserProfile);
        //up.d_clientIp = "10.7.0.1";
        //up.d_clientUserAgent = "ImmaTestHarness";
        up.setEDLAccessToken(new EarthDataLoginAccessToken());
        up.addGroup("fiddle");
        up.addGroup("faddle");
        up.addRole("twiddle");
        up.addRole("piddle");

        // --------------------------------------------------------------------
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
        System.out.println(fromJsonStr);


        // --------------------------------------------------------------------
        System.out.println(hr0);
        System.out.println("Java Native serialize and deserialize user profile...");
        System.out.println("Calling UserProfile.toString() on instance of UserProfile:");
        String baseline = up.toString();
        System.out.print(baseline);
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
            System.exit(1);
        }
        int status = 1;
        if(result != null && result.equals(baseline)){
            System.out.println("Result matched baseline.");
            status = 0;
        }
        System.out.println(hr0);
        System.exit(status);
    }


}
