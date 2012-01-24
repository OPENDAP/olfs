package opendap.wcs.v1_1_2.http;

import opendap.coreServlet.ReqInfo;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 21, 2010
 * Time: 3:43:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {
    /***************************************************************************/




    public static String getServiceUrlString(HttpServletRequest request, String prefix){
        String serviceURL = getServiceUrl(request);

        if (!prefix.equals("")) {
            if (!serviceURL.endsWith("/")) {
                if (prefix.startsWith("/"))
                    serviceURL += prefix;
                else
                    serviceURL += "/" + prefix;

            } else {
                if (prefix.startsWith("/"))
                    serviceURL += serviceURL.substring(0, serviceURL.length() - 1) + prefix;
                else
                    serviceURL += prefix;

            }
        }
        return serviceURL;

    }

    public static String getServiceUrl(HttpServletRequest request){
        return ReqInfo.getServiceUrl(request);
    }


}
