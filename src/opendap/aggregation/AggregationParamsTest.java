/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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

package opendap.aggregation;

// This uses JUnit 4
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AggregationParamsTest {

    AggregationParams params_1, params_2;
    AggregationParams bbox_1, bbox_2, bbox_error;

    private Map<String, String[]> buildMap(String file_values[], String var_values[]) {
        Map<String, String[]> theMap = new HashMap<String, String[]>();

        theMap.put("file", file_values);
        theMap.put("var", var_values);

        return theMap;
    }

    private Map<String, String[]> buildMap(String file_values[], String var_values[], String bbox_values[]) {
        Map<String, String[]> theMap = buildMap(file_values, var_values);

        theMap.put("bbox", bbox_values);

        return theMap;
    }

    @Before
    public void setUp() throws Exception {
        String file_values[] = {"/file1.nc", "/file2.nc", "/file3.nc"};
        String var_values[]  = {"lat1,lon1,time1", "lat2,lon2,time2", "lat3,lon3,time3"};
        params_1 = new AggregationParams(buildMap(file_values, var_values));

        String var[]  = {"lat,lon,time"};
        params_2 = new AggregationParams(buildMap(file_values, var));

        String bbox_values[] = {"[110,X,120][15,Y,17]", "[210,X,220][25,Y,27]", "[310,X,320][35,Y,37]"};
        bbox_1 = new AggregationParams(buildMap(file_values, var_values, bbox_values));

        String bbox[] = {"[10,X,20][5,Y,7]"};
        bbox_2 = new AggregationParams(buildMap(file_values, var, bbox));
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetFileNumber() throws Exception {
        Assert.assertEquals(params_1.getNumberOfFiles(), 3);
    }

    @Test
    public void testGetFilename() throws Exception {
        try {
            Assert.assertEquals("/file1.nc", params_1.getFilename(0));
            Assert.assertEquals("/file3.nc", params_1.getFilename(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(params_1.getFilename(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetCE() throws Exception {
        try {
            Assert.assertEquals("lat1,lon1,time1", params_1.getArrayCE(0));
            Assert.assertEquals("lat3,lon3,time3", params_1.getArrayCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(params_1.getArrayCE(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertEquals("lat,lon,time", params_2.getArrayCE(0));
            Assert.assertEquals("lat,lon,time", params_2.getArrayCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetCE_bbox_version() throws Exception {
        try {
            Assert.assertEquals("roi(lat1,lon1,time1,bbox_union(bbox(X,110,120),bbox(Y,15,17),\"intersection\"))", bbox_1.getArrayCE(0));
            Assert.assertEquals("roi(lat3,lon3,time3,bbox_union(bbox(X,310,320),bbox(Y,35,37),\"intersection\"))", bbox_1.getArrayCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(bbox_1.getArrayCE(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertEquals("roi(lat,lon,time,bbox_union(bbox(X,10,20),bbox(Y,5,7),\"intersection\"))", bbox_2.getArrayCE(0));
            Assert.assertEquals("roi(lat,lon,time,bbox_union(bbox(X,10,20),bbox(Y,5,7),\"intersection\"))", bbox_2.getArrayCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetCE_simple_tabular() throws Exception {
        try {
            Assert.assertEquals("tabular(lat1,lon1,time1)", params_1.getTableCE(0));
            Assert.assertEquals("tabular(lat3,lon3,time3)", params_1.getTableCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(params_1.getArrayCE(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertEquals("tabular(lat,lon,time)", params_2.getTableCE(0));
            Assert.assertEquals("tabular(lat,lon,time)", params_2.getTableCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetCE_tabular_bbox_version() throws Exception {
        try {
            Assert.assertEquals("tabular(lat1,lon1,time1)&X>=110&X<=120&Y>=15&Y<=17", bbox_1.getTableCE(0));

            Assert.assertEquals("tabular(lat3,lon3,time3)&X>=310&X<=320&Y>=35&Y<=37", bbox_1.getTableCE(2));
        }        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(bbox_1.getArrayCE(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertEquals("tabular(lat,lon,time)&X>=10&X<=20&Y>=5&Y<=7", bbox_2.getTableCE(0));
            Assert.assertEquals("tabular(lat,lon,time)&X>=10&X<=20&Y>=5&Y<=7", bbox_2.getTableCE(2));
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }


}