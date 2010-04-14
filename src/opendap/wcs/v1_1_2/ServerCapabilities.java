package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 16, 2009
 * Time: 11:28:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerCapabilities {

    private static String[] sf = {"application/x-netcdf-cf1.0","application/x-dap-3.2"};



    /**
     *
     * @return
     * @param dapServer
     */
    public static String[] getSupportedFormatStrings(URL dapServer){
        return sf;
    }



    /**
     *
     * @param coverageID
     * @param fieldID
     * @return
     */
    static String[] getInterpolationMethods(String coverageID, String fieldID){
        String[] im = {"nearest"};
        return im;
        
    }


    public static void main(String[] args) throws Exception{


        String[] sf = getSupportedFormatStrings(null);
        for(String s : sf)
            System.out.println("Supported Format: "+s);


        String[] im = getInterpolationMethods(null,null);
        for(String s : im)
            System.out.println("InterpolationMethods: "+s);

    }
}
