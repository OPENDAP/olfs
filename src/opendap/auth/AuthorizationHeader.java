/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2022 OPeNDAP, Inc.
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

//import java.util.Base64;

public class AuthorizationHeader {

    public static final String BASIC_SCHEME = "Basic";
    public static final String BEARER_SCHEME = "Bearer";
    public static final String DIGEST_SCHEME = "Digest";
    public static final String HOBA_SCHEME = "HOBA";
    public static final String MUTUAL_SCHEME = "Mutual";
    public static final String NEGOTIATE_NTLM_SCHEME = "Negotiate/NTLM";
    public static final String VAPID_SCHEME = "VAPID";
    public static final String SCRAM_SCHEME = "SCRAM";
    public static final String AWS4_HMAC_SHA256_SCHEME = "AWS4-HMAC-SHA256";

    // public enum scheme { Basic, Bearer, Digest, HOBA, Mutual, Negotiate_NTLM, VAPID, SCRAM, AWS4_HMAC_SHA256 };

    public static String[] getTokens(String auth_header_value) {
        if (auth_header_value != null) {
            return auth_header_value.split(" ");
        }
        return null;
    }

    public static String getScheme(String auth_header_value) {
        if(auth_header_value == null)
            return null;

        String[] tokens = getTokens(auth_header_value);
        if (tokens.length == 2) {
            return tokens[0];
        }
        return null;
    }

    public static String getPayload(String auth_header_value) {
        if(auth_header_value == null)
            return null;

        String[] tokens = getTokens(auth_header_value);
        if (tokens.length == 2) {
            String payload = tokens[1];
            //if(tokens[0].equalsIgnoreCase(BASIC_SCHEME)){
            //    payload = Base64.getDecoder().decode(payload).toString();
            //}
            return payload;
        }
        return null;
    }



    public static boolean checkAuthorizationHeader(String scheme_type, String auth_header_value){
        boolean retVal = false;
        String scheme = getScheme(auth_header_value);
        if(scheme!=null) {
            retVal = scheme.equalsIgnoreCase(scheme_type);
        }
        return retVal;
    }

    public static boolean isBasic(String auth_header){
        return checkAuthorizationHeader(BASIC_SCHEME,auth_header);
    }
    public static boolean isBearer(String auth_header){
        return checkAuthorizationHeader(BEARER_SCHEME,auth_header);
    }
    public static boolean isDigest(String auth_header){
        return checkAuthorizationHeader(DIGEST_SCHEME,auth_header);
    }
    public static boolean isHOBA(String auth_header){
        return checkAuthorizationHeader(HOBA_SCHEME,auth_header);
    }
    public static boolean isMutual(String auth_header){
        return checkAuthorizationHeader(MUTUAL_SCHEME,auth_header);
    }
    public static boolean isNegotiateNTLM(String auth_header){
        return checkAuthorizationHeader(NEGOTIATE_NTLM_SCHEME,auth_header);
    }
    public static boolean isVAPID(String auth_header){
        return checkAuthorizationHeader(VAPID_SCHEME,auth_header);
    }
    public static boolean isSCRAM(String auth_header){
        return checkAuthorizationHeader(SCRAM_SCHEME,auth_header);
    }

    public static boolean isAWS4_HMAC_SHA256(String auth_header){
        return checkAuthorizationHeader(AWS4_HMAC_SHA256_SCHEME,auth_header);
    }


}
