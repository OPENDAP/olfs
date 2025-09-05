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


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ndp on 9/24/14.
 */
public class EarthDataLoginAccessToken implements Serializable {

    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String ENDPOINT_KEY = "endpoint";
    public static final String EXPIRES_IN_KEY = "expires_in";
    public static final String TOKEN_TYPE_KEY = "token_type";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";

    /* @serial */
    private String accessToken;
    /* @serial */
    private String endPoint;
    /* @serial */
    private long expiresIn;
    /* @serial */
    private String tokenType;
    /* @serial */
    private String refreshToken;
    /* @serial */
    private Date creationTime;
    /* @serial */
    private String edlClientAppId;

    public EarthDataLoginAccessToken() {
        creationTime = new Date();
        accessToken = null;
        endPoint = null;
        expiresIn = 3600;
        tokenType = null;
        refreshToken = null;
        edlClientAppId = null;
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
        tokenType = AuthorizationHeader.getScheme(authorizationHeader);
        accessToken = AuthorizationHeader.getPayload(authorizationHeader);
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

        String classname = getClass().getName();
        sb.append(indent).append(classname).append(": \n");
        sb.append(l1i).append("creationTime: ").append(creationTime).append("\n");
        sb.append(l1i).append(ACCESS_TOKEN_KEY).append(": ").append(accessToken).append("\n");
        sb.append(l1i).append(ENDPOINT_KEY).append(": ").append(endPoint).append("\n");
        sb.append(l1i).append(EXPIRES_IN_KEY).append(": ").append(expiresIn).append("\n");
        sb.append(l1i).append(TOKEN_TYPE_KEY).append(": ").append(tokenType).append("\n");
        sb.append(l1i).append(REFRESH_TOKEN_KEY).append(": ").append(refreshToken).append("\n");
        return sb.toString();
    }

    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static String toJson(Object o){
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    public static EarthDataLoginAccessToken fromJson(String jsonStr){
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, EarthDataLoginAccessToken.class);
    }




    public String getEchoTokenValue(){
        return  getAccessToken() + ":" + getEdlClientAppId();
    }

    public String getAuthorizationHeaderValue(){
        return  getTokenType() + " " + getAccessToken();
    }





}
