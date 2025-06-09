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

// import java.util.UUID;
import static org.junit.Assert.*;
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
        // expect
        // is present for downstream tests...and/or if we add future tests for loading
        // the
        // public key values from a config file
        UrsIdP ursIdP = new UrsIdP();

        assertEquals(ursIdP.getUrsClientAppPublicKeys(), null);

        String testValue = "thisStringIsAString";
        ursIdP.setUrsClientAppPublicKeys(testValue);
        assertEquals(ursIdP.getUrsClientAppPublicKeys(), testValue);
    }

    @Test
    public void testGetValueFromEncodedJson() {
        UrsIdP ursIdP = new UrsIdP();

        /*
         * - getStringValueFromEncodedString:
         * - valid key, valid input str
         * - input isn't encoded
         * - encoded input isn't json
         * - key isn't in json
         * - value in json isn't a string
         */

    }
}