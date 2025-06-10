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
import opendap.io.HyraxStringEncoding;

// import java.util.UUID;
// import com.google.gson.JsonArray;
// import com.google.gson.JsonElement;
// import com.google.gson.JsonObject;
// import com.google.gson.JsonParser;
// import com.google.gson.JsonSyntaxException;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrsIdPTest {

    private Logger log;

    public UrsIdPTest() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    /*
     * Okay, here are the things we want to test:
     * For each failing test, test that it fails AND that it posts an error
     * message?? or maybe that doesn't matter....
     * - getEdlUserIdFromToken:
     * - case that succeeds!
     * 
     * - token empty
     * - token wrong format?
     * 
     * - token doesn't have sig key
     * - keys empty
     * - keys valid but don't contain requested key
     * 
     * - token invalid compared to key
     * - token verified, doesn't have uid field (?)
     * - valid publicKeys, access token
     * 
     * - case where it isn't RSA256, to show that it doesn't fail
     * catastrophically....
     */

    @Test
    public void testGetSetUrsClientAppPublicKeys() {
        // This test is almost trivially silly, but it demonstrates that the behavior we
        // expect is present for downstream tests...and will become less trivial if we
        // add testing around loading the public key values from a config file
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
        assertEquals("baa", UrsIdP.getStringValueFromEncodedJson(encodedStr, "sheep"));

        // Demonstrate that `null` is returned and exceptions are not thrown when...
        // ...input string is empty:
        assertEquals(null, UrsIdP.getStringValueFromEncodedJson("", ""));

        // ...input string is not valid Base64-encoded:
        assertEquals(null, UrsIdP.getStringValueFromEncodedJson("aBcDe", ""));

        // ...input string does not contain JSON:
        assertEquals(null, UrsIdP.getStringValueFromEncodedJson("aBcDeF", ""));

        // ...in string contains malformed JSON:
        String decodedMalStr = "{\"comma\":\"pain\",}";
        String encodedMalStr = Base64.getEncoder().encodeToString(decodedMalStr.getBytes());
        assertEquals(null, UrsIdP.getStringValueFromEncodedJson(encodedMalStr, "comma"));

        // ...input string does not contain requested key:
        assertEquals(null, UrsIdP.getStringValueFromEncodedJson(encodedStr, "cow"));

        // ...value for key is not a string:
        String decodedStr2 = "{\"fish\":[\"one\",\"two\",\"red\",\"blue\"]}";
        String encodedStr2 = Base64.getEncoder().encodeToString(decodedStr2.getBytes());
        assertEquals(null, UrsIdP.getStringValueFromEncodedJson(encodedStr2, "fish"));
    }
}