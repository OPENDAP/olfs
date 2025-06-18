/*
* /////////////////////////////////////////////////////////////////////////////
* This file is part of the "Hyrax Data Server" project.
*
*
* Copyright (c) 2025 OPeNDAP, Inc.
* Author: Nathan David Potter  <ndp@opendap.org>
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*
* You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
* /////////////////////////////////////////////////////////////////////////////
*/

package opendap.auth;

import opendap.auth.UrsIdP;

import com.auth0.jwk.InvalidPublicKeyException;
import com.google.gson.JsonParseException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.Assert.*;
import org.junit.Test;

public class UrsIdPTest {
    // Public test token and JWKS provided by EDL:
    // https://urs.earthdata.nasa.gov/documentation/for_integrators/verify_edl_jwt_token
    public static final String EDL_TEST_JWKS = "{" +
            "  \"keys\": [" +
            "    {" +
            "      \"kty\": \"RSA\"," +
            "      \"n\": \"xSxiOkM8m8oCyWn-sNNZxBVTUcPAlhXRjKpLTYIM21epMC9rqEnrgL7iuntmp3UcffOIKtFHOtCG-jWUkyzxZHPPMo0kYZVHKRjGj-AVAy3FA-d2AtUc1dPlrQ0TpdDoTzew_6-48BcbdFEQI3161wcMoy40unYYYfzo3KuUeNcCY3cmHzSkYn4iQHaBy5zTAzKTIcYCTpaBGDk4_IyuysvaYmgwdeNO26hNV9dmPx_rWgYZYlashXZ_kRLirDaGpnJJHyPrYaEJpMIWuIfsh_UoMjsyuoQGe4XU6pG8uNnUd31mHa4VU78cghGZGrCz_YkPydfFlaX65LBp9aLdCyKkA66pDdnCkm8odVMgsH2x_kGM7sNlQ6ELTsT-dtJoiEDI_z3fSZehLw469QpTGQjfsfXUCYm8QrGckJF4bJc935TfGU86qr2Ik2YoipP_L4K_oqUf8i6bwO0iomo_C7Ukr4l-dh4D5O7szAb9Ga804OZusFk3JENlc1-RlB20S--dWrrO-v_L8WI2d72gizOKky0Xwzd8sseEqfMWdktyeKoaW0ANkBJHib4E0QxgedeTca0DH_o0ykMjOZLihOFtvDuCsbHG3fv41OQr4qRoX97QO2Hj1y3EBYtUEypan46g-fUyLCt-sYP66RkBYzCJkikCbzF_ECBDgX314_0\","
            +
            "      \"e\": \"AQAB\"," +
            "      \"kid\": \"edljwtpubkey_development\"" +
            "    }" +
            "  ]" +
            "}";

    public static final String EDL_TEST_TOKEN = "eyJ0eXAiOiJKV1QiLCJvcmlnaW4iOiJFYXJ0aGRhdGEgTG9naW4iLCJzaWciOiJlZGxqd3RwdWJrZXlfZGV2ZWxvcG1lbnQiLCJhbGciOiJSUzI1NiJ9.eyJ0eXBlIjoiVXNlciIsImNsaWVudF9pZCI6ImxvY2FsZGV2IiwiZXhwIjoxMTY2MTU0NzA5NCwiaWF0IjoxNjYxNTQ3MDk0LCJpc3MiOiJFYXJ0aGRhdGEgTG9naW4iLCJ1aWQiOiJtc3RhcnR6ZWx0ZXN0ZXIifQ.tAA4zu2K3NFJVX96_M1qb1lrDQ1l7uMjMWNE5Jkxw16RnHTgIt6dcEdX-YcXUbJqKDU39RLcB_O8hgzd9e7pHBdxrwhjl4LnuNcSj1XZGTLh-RD5VW1jRkQe_pcH1noniUqkXaYuVGUybnbhDZa_37Oxs-vr0lT06Qk82elV5Y1dq_YLQeABv4O0BiktpPoCSjSIfTW6jEUS-ONk07r5N5O_Me7H-QbPhEtiax1N3zcTRWyNn7lM6vQxl7d-ywRKqQaeA9Iy-ufmGXoczLvvN4HsaKbVhQ_llw6Xnj7cKpd4WJ6VABDETlMlcjwtyvSt-q3aToy9N4_EkMGQbxkbsQrvf9LI2VM7J799uhW9E4VvOEl-CafkFnOgjo1nvMpq3fZq1zIfG4eA6UrYpQQz_gcdFfoL-p5ZI_BbMO0PK_8XAfE8O0w7b7i7QJ_EmKKUA2QibJLK8qdlOhbLNu6ORTyqvxbawMAjW_ZzJIZnDwjyuIoJNBFJQxiz2SMBdQwAuJDGcyzIGEAheF0ffB-mJG28HyVvhbjQhP2ByE0mZoDFhqmgk47FnQNFL7mTdtSbI-KvOXb3rBEaELUdWDuqjgnOxQehJzFlbqETRZfZDuEUq7q1Zl227k2178lvVVPQuco8Auo180qVaJcAs9Fd2k-i6oNkalC6MNjgmEpBUSE";

    @Test
    public void testGetSetUrsClientAppPublicKeys() {
        // This test is almost trivially silly, but it demonstrates that the behavior
        // expected is present for downstream tests. It will become less trivial if we
        // ever add testing around optionally loading the public key values from a
        // config file
        UrsIdP ursIdP = new UrsIdP();
        assertEquals(null, ursIdP.getUrsClientAppPublicKeys());

        String testValue = "thisStringIsAString";
        ursIdP.setUrsClientAppPublicKeys(testValue);
        assertEquals(testValue, ursIdP.getUrsClientAppPublicKeys());
    }

    @Test
    public void testGetValueFromEncodedJson() {
        // Success case:
        String decodedStr = "{\"sheep\":\"baa\"}";
        String encodedStr = Base64.getEncoder().encodeToString(decodedStr.getBytes());
        assertEquals("baa", UrsIdP.getStringValueFromEncodedJson(encodedStr,
                "sheep"));

        // Failure cases:
        assertEquals("Should return null when input string is empty", null,
                UrsIdP.getStringValueFromEncodedJson("", ""));

        assertEquals("Should return null when input string is input string is not valid Base64-encoded", null,
                UrsIdP.getStringValueFromEncodedJson("aBcDe", ""));

        assertEquals("Should return null when input string does not contain JSON", null,
                UrsIdP.getStringValueFromEncodedJson("aBcDeF", ""));

        String decodedMalStr = "{\"comma\":\"pain\",}";
        String encodedMalStr = Base64.getEncoder().encodeToString(decodedMalStr.getBytes());
        assertEquals("Should return null when input string contains malformed JSON", null,
                UrsIdP.getStringValueFromEncodedJson(encodedMalStr,
                        "comma"));

        assertEquals("Should return null when input string does not contain requested key", null,
                UrsIdP.getStringValueFromEncodedJson(encodedStr, "cow"));

        String decodedStr2 = "{\"fish\":[\"one\",\"two\",\"red\",\"blue\"]}";
        String encodedStr2 = Base64.getEncoder().encodeToString(decodedStr2.getBytes());
        assertEquals("Should return null when value for key is not a string", null,
                UrsIdP.getStringValueFromEncodedJson(encodedStr2,
                        "fish"));
    }

    @Test
    public void testGetPublicKeyForId() throws InvalidPublicKeyException,
            JsonParseException, IllegalStateException {
        UrsIdP ursIdP = new UrsIdP();

        // Success case:
        RSAPublicKey pubKey = ursIdP.getPublicKeyForId(EDL_TEST_JWKS,
                "edljwtpubkey_development");
        assertNotNull(pubKey);
        BigInteger exponent = new BigInteger(1, Base64.getDecoder().decode("AQAB"));
        assertEquals(exponent, pubKey.getPublicExponent());

        // Failure cases:
        assertNull("Should return null when public keys are null",
                ursIdP.getPublicKeyForId(null,
                        "arbitrary_key"));

        assertNull("Should return null when public keys are empty",
                ursIdP.getPublicKeyForId("",
                        "arbitrary_key"));

        assertNull("Should return null when key id is null",
                ursIdP.getPublicKeyForId(EDL_TEST_JWKS, null));

        assertNull("Should return null when key id is empty",
                ursIdP.getPublicKeyForId(EDL_TEST_JWKS, ""));

        assertNull("Should return null when key id is not found in JWKS",
                ursIdP.getPublicKeyForId(EDL_TEST_JWKS,
                        "key_id_that_definitely_does_not_exist"));

        assertThrows("Should throw when public keys string is not JSON",
                IllegalStateException.class,
                () -> ursIdP.getPublicKeyForId("thisStringIsNotAJsonWebKeySet", "myId"));

        assertNull("Should return null when public keys string is not valid JWKS",
                ursIdP.getPublicKeyForId("{\"jsonBlob\": \"butNotAJsonWebKeySet\"}",
                        "myId"));

        // Invalid due to funky modulus (compare to correct EDL_TEST_JWKS)
        String jwksInvalidEncryption = "{" +
                " \"keys\": [" +
                " {" +
                " \"kty\": \"RSA\"," +
                " \"n\": \"fefefefef\","
                +
                " \"e\": \"AQAB\"," +
                " \"kid\": \"edljwtpubkey_development\"" +
                " }" +
                " ]" +
                "}";
        assertNull("Should return null when public key does not contain an RSA key",
                ursIdP.getPublicKeyForId(jwksInvalidEncryption, "myId"));

        String jwksMissingKTY = "{" +
                " \"keys\": [" +
                " {" +
                " \"kid\": \"edljwtpubkey_development\"" +
                " }" +
                " ]" +
                "}";
        assertThrows("Should throw when public key does not contain kty field",
                IllegalArgumentException.class,
                () -> ursIdP.getPublicKeyForId(jwksMissingKTY, "edljwtpubkey_development"));

        String jwksNotRSA = "{" +
                " \"keys\": [" +
                " {" +
                " \"kty\": \"FOO\"," +
                " \"kid\": \"edljwtpubkey_development\"" +
                " }" +
                " ]" +
                "}";
        assertThrows("Should throw when public key's kty is not RSA",
                InvalidPublicKeyException.class,
                () -> ursIdP.getPublicKeyForId(jwksNotRSA, "edljwtpubkey_development"));
    }

    @Test
    public void testGetEdlUserIdFromToken() {
        UrsIdP ursIdP = new UrsIdP();

        // Success case:
        assertEquals("mstartzeltester", ursIdP.getEdlUserIdFromToken(EDL_TEST_JWKS,
                EDL_TEST_TOKEN));

        // Failure cases:
        assertNull("Should return null when public key is not found for given token",
                ursIdP.getEdlUserIdFromToken("", EDL_TEST_TOKEN));

        assertNull("Should return null when access token is null",
                ursIdP.getEdlUserIdFromToken(EDL_TEST_JWKS, null));

        assertNull("Should return null when access token has invalid format",
                ursIdP.getEdlUserIdFromToken(EDL_TEST_JWKS, "token.is.not.token"));

        String irrelevantJson = Base64.getEncoder().encodeToString("{\"foo\": \"bar\"}".getBytes());
        assertNull("Should return null when access token doesn't contain the requisite key id (\"sig\" field)",
                ursIdP.getEdlUserIdFromToken(EDL_TEST_JWKS, irrelevantJson + "." +
                        irrelevantJson + "." + "abc"));

        String[] tokenParts = EDL_TEST_TOKEN.split("\\.");
        String tokenWithBadSignature = tokenParts[0] + "." + tokenParts[1] + "." + "abcdefg";
        assertNull("Should return null when access token fails verification",
                ursIdP.getEdlUserIdFromToken(EDL_TEST_JWKS, tokenWithBadSignature));
    }
}