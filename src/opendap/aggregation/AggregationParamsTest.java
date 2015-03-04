package opendap.aggregation;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

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
        Assert.assertEquals(params_1.getFileNumber(), 3);
    }

    @Test
    public void testGetFilename() throws Exception {
        try {
            Assert.assertEquals(params_1.getFilename(0), "/file1.nc");
            Assert.assertEquals(params_1.getFilename(2), "/file3.nc");
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
            Assert.assertEquals(params_1.getCE(0), "lat1,lon1,time1");
            Assert.assertEquals(params_1.getCE(2), "lat3,lon3,time3");
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(params_1.getCE(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertEquals(params_2.getCE(0), "lat,lon,time");
            Assert.assertEquals(params_2.getCE(2), "lat,lon,time");
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }
    }

    @Test
    public void testGetCE_bbox_version() throws Exception {
        try {
            Assert.assertEquals(bbox_1.getCE(0), "roi(lat1,lon1,time1,bbox_union(bbox(X,110,120),bbox(Y,15,17),\"intersection\"))");
            Assert.assertEquals(bbox_1.getCE(2), "roi(lat3,lon3,time3,bbox_union(bbox(X,310,320),bbox(Y,35,37),\"intersection\"))");
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        try {
            // There is no '3'
            Assert.fail(bbox_1.getCE(3));
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertEquals(bbox_2.getCE(0), "roi(lat,lon,time,bbox_union(bbox(X,10,20),bbox(Y,5,7),\"intersection\"))");
            Assert.assertEquals(bbox_2.getCE(2), "roi(lat,lon,time,bbox_union(bbox(X,10,20),bbox(Y,5,7),\"intersection\"))");
        }
        catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

    }
}