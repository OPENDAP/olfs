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
public class OAuthAccessToken {

    public static final String OAUTH_ACCESS_TOKEN  = "oauth_access_token";
    public static final String ACCESS_TOKEN  = "access_token";
    public static final String ENDPOINT  = "endpoint";
    public static final String EXPIRES_IN  = "expires_in";
    public static final String TOKEN_TYPE  = "token_type";
    public static final String REFRESH_TOKEN  = "refresh_token";

    private String _accessToken;
    private String _endPoint;
    private long _expiresIn;
    private String _tokenType;
    private String _refreshToken;
    private Date _creationTime;



    public OAuthAccessToken(JsonObject json)  {
        _creationTime = new Date();
        _accessToken  = json.get(ACCESS_TOKEN).getAsString();
        _endPoint     = json.get(ENDPOINT).getAsString();
        _expiresIn    = json.get(EXPIRES_IN).getAsLong();
        _tokenType    = json.get(TOKEN_TYPE).getAsString();
        _refreshToken = json.get(REFRESH_TOKEN).getAsString();
    }


    public OAuthAccessToken(OAuthAccessToken oat)  {
        _creationTime = oat._creationTime;
        _accessToken  = oat._accessToken;
        _endPoint     = oat._endPoint;
        _expiresIn    = oat._expiresIn;
        _tokenType    = oat._tokenType;
        _refreshToken = oat._refreshToken;
    }
    public OAuthAccessToken() {
        _creationTime = new Date();
        _accessToken  = "ASPECIALURSACCESSTOKENSTRING";
        _endPoint     = "http://endpoint.url";
        _expiresIn    = 3600;
        _tokenType    = "token_type";
        _refreshToken = "ASPECIALURSREFRESHTOKEN";

    }


    public String getAccessToken(){
        return _accessToken;
    }

    public void setAccessToken(String at){
        _accessToken = at;
    }


    public String getEndPoint(){
        return _endPoint;
    }

    public void setEndPoint(String ep){
        _endPoint = ep;
    }


    public String getTokenType(){
        return _tokenType;
    }

    public void setTokenType(String tt){
        _tokenType = tt;
    }


    public String getRefreshToken(){
        return _refreshToken;
    }

    public void setRefreshToken(String rt){
        _refreshToken = rt;
    }


    public long getExpiresIn(){
        return _expiresIn;
    }

    public void setExpiresIn(long ei){
        _expiresIn = ei;
    }



    public long expiresIn(){
        Date now = new Date();

        return _expiresIn - (now.getTime() - _creationTime.getTime());

    }
    public String toString() {
        return toString("","    ");

    }

    public String toString(String indent, String indent_inc){
        StringBuilder sb = new StringBuilder();
        String l1i = indent +indent_inc;
        sb.append(indent).append("\"").append(this.getClass().getName()).append("\" : {\n");
        sb.append(l1i).append("\"creationTime\" : \"").append(_creationTime).append("\",\n");
        sb.append(l1i).append("\"").append(ACCESS_TOKEN).append("\" : \"").append(_accessToken).append("\",\n");
        sb.append(l1i).append("\"").append(ENDPOINT).append("\" : \"").append(_endPoint).append("\",\n");
        sb.append(l1i).append("\"").append(EXPIRES_IN).append("\" : \"").append(_expiresIn).append("\",\n");
        sb.append(l1i).append("\"").append(TOKEN_TYPE).append("\" : \"").append(_tokenType).append("\",\n");
        sb.append(l1i).append("\"").append(REFRESH_TOKEN).append("\" : \"").append(_refreshToken).append("\"\n");
        sb.append(indent).append("}\n");
        return sb.toString();
    }








}
