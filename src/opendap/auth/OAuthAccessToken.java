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


    private String _accessToken;
    private String _endPoint;
    private long _expiresIn;
    private String _tokenType;
    private String _refreshToken;
    private Date _creationTime;



    public OAuthAccessToken(JsonObject json)  {

        _creationTime = new Date();

        _accessToken  = json.get("access_token").getAsString();
        _endPoint     = json.get("endpoint").getAsString();
        _expiresIn    = json.get("expires_in").getAsLong();
        _tokenType    = json.get("token_type").getAsString();
        _refreshToken = json.get("refresh_token").getAsString();

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










}
