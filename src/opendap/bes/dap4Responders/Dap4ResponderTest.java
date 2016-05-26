/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2016 OPeNDAP, Inc.
 * // Author: James Gallagher <jgallagher@opendap.org>
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

package opendap.bes.dap4Responders;

// This uses JUnit 4
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import opendap.bes.dap2Responders.BesApi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @brief Test Dap4Responder.
 * This currently only tests getDownloadFileName() which now (5/26/16)
 * has a new behavior that it can selectively replace an existing extension
 * or use the old behavior and always apply an extension.
 * 
 * Run the test using "ant check"
 * 
 * @author jimg
 */
public class Dap4ResponderTest {

	private BesApi besApi;
	private Dap4Responder old_rule, new_rule;
	
    @Before
    // Dap4responder is abstract; I added stubs for the unimplemented methods
    // so I can test the code I hacked in getDownloadFileName(). jhrg 5/26/16
    public void setUp() throws Exception {
    	old_rule = new Dap4Responder("", "", ".nc", besApi) {

			@Override
			public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
			}

			@Override
			public boolean isMetadataResponder() {
				return false;
			}

			@Override
			public boolean isDataResponder() {
				return false;
			}
        };
        
        new_rule = new Dap4Responder("", "", ".nc", besApi) {

			@Override
			public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
			}

			@Override
			public boolean isMetadataResponder() {
				return false;
			}

			@Override
			public boolean isDataResponder() {
				return false;
			}
        };
        
        new_rule.addTypeSuffixToDownloadFilename(true);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetDownloadFileName() throws Exception {
        Assert.assertEquals(old_rule.getDownloadFileName("file"), "file.nc");
        Assert.assertEquals(old_rule.getDownloadFileName("file.nc"), "file.nc.nc");
        Assert.assertEquals(old_rule.getDownloadFileName("file.hdf"), "file.hdf.nc");
        Assert.assertEquals(old_rule.getDownloadFileName("file.html"), "file.html.nc");
        
        Assert.assertEquals(new_rule.getDownloadFileName("file"), "file.nc");
        Assert.assertEquals(new_rule.getDownloadFileName("file.nc"), "file.nc");
        Assert.assertEquals(new_rule.getDownloadFileName("file.hdf"), "file.nc");
        Assert.assertEquals(new_rule.getDownloadFileName("file.html"), "file.html.nc");
    }
}