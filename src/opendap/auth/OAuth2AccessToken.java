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
public class OAuth2AccessToken {

    public static final String OAUTH_ACCESS_TOKEN  = "oauth_access_token";
    public static final String ACCESS_TOKEN  = "access_token";
    public static final String ENDPOINT  = "endpoint";
    public static final String EXPIRES_IN  = "expires_in";
    public static final String TOKEN_TYPE  = "token_type";
    public static final String REFRESH_TOKEN  = "refresh_token";

    private String accessToken;
    private String endPoint;
    private long expiresIn;
    private String tokenType;
    private String refreshToken;
    private Date creationTime;



    public OAuth2AccessToken(JsonObject json)  {
        creationTime = new Date();
        accessToken = json.get(ACCESS_TOKEN).getAsString();
        endPoint = json.get(ENDPOINT).getAsString();
        expiresIn = json.get(EXPIRES_IN).getAsLong();
        tokenType = json.get(TOKEN_TYPE).getAsString();
        refreshToken = json.get(REFRESH_TOKEN).getAsString();
    }


    public OAuth2AccessToken(OAuth2AccessToken oat)  {
        creationTime = oat.creationTime;
        accessToken = oat.accessToken;
        endPoint = oat.endPoint;
        expiresIn = oat.expiresIn;
        tokenType = oat.tokenType;
        refreshToken = oat.refreshToken;
    }
    public OAuth2AccessToken() {
        creationTime = new Date();
        accessToken = "ASPECIALURSACCESSTOKENSTRING";
        endPoint = "http://endpoint.url";
        expiresIn = 3600;
        tokenType = "token_type";
        refreshToken = "ASPECIALURSREFRESHTOKEN";

    }


    public String getAccessToken(){
        return accessToken;
    }

    public void setAccessToken(String at){
        accessToken = at;
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
        sb.append(l1i).append("\"").append(ACCESS_TOKEN).append("\" : \"").append(accessToken).append("\",\n");
        sb.append(l1i).append("\"").append(ENDPOINT).append("\" : \"").append(endPoint).append("\",\n");
        sb.append(l1i).append("\"").append(EXPIRES_IN).append("\" : \"").append(expiresIn).append("\",\n");
        sb.append(l1i).append("\"").append(TOKEN_TYPE).append("\" : \"").append(tokenType).append("\",\n");
        sb.append(l1i).append("\"").append(REFRESH_TOKEN).append("\" : \"").append(refreshToken).append("\"\n");
        sb.append(indent).append("}\n");
        return sb.toString();
    }








}
