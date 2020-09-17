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


import com.google.gson.JsonObject;

import java.util.Date;

/**
 * Created by ndp on 9/24/14.
 */
public class EarthDataLoginAccessToken {

    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String ENDPOINT_KEY = "endpoint";
    public static final String EXPIRES_IN_KEY = "expires_in";
    public static final String TOKEN_TYPE_KEY = "token_type";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String BEARER_TOKEN_TYPE = "Bearer";

    private String accessToken;
    private String endPoint;
    private long expiresIn;
    private String tokenType;
    private String refreshToken;
    private Date creationTime;
    private String edlClientAppId;

    public EarthDataLoginAccessToken() {
        creationTime = new Date();
        accessToken = "ASPECIALURSACCESSTOKENSTRING";
        endPoint = "http://endpoint.url";
        expiresIn = 3600;
        tokenType = "token_type";
        refreshToken = "ASPECIALURSREFRESHTOKEN";
        edlClientAppId="ThatSecretSauceFromEDL";
    }



    public EarthDataLoginAccessToken(JsonObject json, String appID)  {
        this();
        creationTime = new Date();
        edlClientAppId = appID;
        accessToken = json.get(ACCESS_TOKEN_KEY).getAsString();
        endPoint = json.get(ENDPOINT_KEY).getAsString();
        expiresIn = json.get(EXPIRES_IN_KEY).getAsLong();
        tokenType = json.get(TOKEN_TYPE_KEY).getAsString();
        refreshToken = json.get(REFRESH_TOKEN_KEY).getAsString();
    }


    public EarthDataLoginAccessToken(String authorizationHeader, String appID)  {
        this();
        creationTime = new Date();
        edlClientAppId = appID;
        String[] tokens = authorizationHeader.split(" ");
        if(tokens.length == 2){
            accessToken = tokens[1];
            tokenType = tokens[0];
        }
    }


    public EarthDataLoginAccessToken(EarthDataLoginAccessToken oat)  {
        this();
        creationTime = oat.creationTime;
        accessToken = oat.accessToken;
        endPoint = oat.endPoint;
        expiresIn = oat.expiresIn;
        tokenType = oat.tokenType;
        refreshToken = oat.refreshToken;
        edlClientAppId = oat.getEdlClientAppId();
    }

    public static boolean checkAuthorizationHeader(String auth_header_value){
        boolean retVal = false;
        if(auth_header_value!=null) {
            String[] tokens = auth_header_value.split(" ");
            if(tokens.length == 2){
                retVal = tokens[0].equalsIgnoreCase(BEARER_TOKEN_TYPE);
            }
        }
        return retVal;
    }

    public String getAccessToken(){
        return accessToken;
    }

    public void setAccessToken(String at){
        accessToken = at;
    }

    public String getEdlClientAppId(){
        return edlClientAppId;
    }

    public void setEdlClientAppId(String at){
        edlClientAppId = at;
    }


    public String getEndPoint(){
        return endPoint;
    }

    public void setEndPoint(String ep){
        endPoint = ep;
    }


    public String getTokenType(){
        return tokenType;
    }

    public void setTokenType(String tt){
        tokenType = tt;
    }


    public String getRefreshToken(){
        return refreshToken;
    }

    public void setRefreshToken(String rt){
        refreshToken = rt;
    }


    public long getExpiresIn(){
        return expiresIn;
    }

    public void setExpiresIn(long ei){
        expiresIn = ei;
    }



    public long expiresIn(){
        Date now = new Date();

        return expiresIn - (now.getTime() - creationTime.getTime());

    }
    public String toString() {
        return toString("","    ");

    }

    public String toString(String indent, String indent_inc){
        StringBuilder sb = new StringBuilder();
        String l1i = indent +indent_inc;
        sb.append(indent).append("\"").append(this.getClass().getName()).append("\" : {\n");
        sb.append(l1i).append("\"creationTime\" : \"").append(creationTime).append("\",\n");
        sb.append(l1i).append("\"").append(ACCESS_TOKEN_KEY).append("\" : \"").append(accessToken).append("\",\n");
        sb.append(l1i).append("\"").append(ENDPOINT_KEY).append("\" : \"").append(endPoint).append("\",\n");
        sb.append(l1i).append("\"").append(EXPIRES_IN_KEY).append("\" : \"").append(expiresIn).append("\",\n");
        sb.append(l1i).append("\"").append(TOKEN_TYPE_KEY).append("\" : \"").append(tokenType).append("\",\n");
        sb.append(l1i).append("\"").append(REFRESH_TOKEN_KEY).append("\" : \"").append(refreshToken).append("\"\n");
        sb.append(indent).append("}\n");
        return sb.toString();
    }



    public String getEchoTokenValue(){
        return  getAccessToken() + ":" + getEdlClientAppId();
    }

    public String getAuthorizationHeaderValue(){
        return  getTokenType() + " " + getAccessToken();
    }





}
