package opendap.wcs.v1_1_2;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.HashMap;

import opendap.coreServlet.ReqInfo;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 8, 2009
 * Time: 12:03:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class KvpHandler {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(KvpHandler.class);


    public static void processKvpWcsRequest(String serviceURL, String dataAccessBase, String query, ServletOutputStream os) throws IOException {


        HashMap<String,String> keyValuePairs = new HashMap<String,String>();

        Document wcsResponse;

        try {

            switch(getRequestType(query,keyValuePairs)){

                case  WCS.GET_CAPABILITIES:
                    wcsResponse = getCapabilities(keyValuePairs, serviceURL);
                    break;

                case  WCS.DESCRIBE_COVERAGE:
                    wcsResponse = describeCoverage(keyValuePairs);
                    break;

                case WCS.GET_COVERAGE:
                    wcsResponse = getCoverage(keyValuePairs, dataAccessBase);
                    break;

                default:
                    throw new WcsException("INTERNAL ERROR: getRequestType() returned an invalid value.",
                            WcsException.NO_APPLICABLE_CODE);
            }

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            try {
                xmlo.output(wcsResponse,os);
            } catch (IOException e) {
                throw new WcsException(e.getMessage(), WcsException.NO_APPLICABLE_CODE);
            }


        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            os.println(er.toString());
        }



    }





    /**
     *
     * @param keyValuePairs   Key Value Pairs from WCS URL
     * @throws WcsException When bad things happen.
     */
    public static Document getCapabilities(HashMap<String,String> keyValuePairs, String serviceUrl) throws WcsException {
        GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(keyValuePairs);

            return CapabilitiesRequestProcessor.processGetCapabilitiesRequest(wcsRequest, serviceUrl);
    }


    /**
     *
     * @param keyValuePairs     Key Value Pairs from WCS URL
     * @throws WcsException  When bad things happen.
     */
    public static Document describeCoverage(HashMap<String,String> keyValuePairs ) throws WcsException {
        DescribeCoverageRequest wcsRequest = new DescribeCoverageRequest(keyValuePairs);

            return DescribeCoverageRequestProcessor.processDescribeCoveragesRequest(wcsRequest);
    }



    /**
     *
     * @param keyValuePairs    Key Value Pairs from WCS URL
     * @throws WcsException  When bad things happen.
     */
    public static Document getCoverage(HashMap<String,String> keyValuePairs, String urlBase) throws WcsException {

        GetCoverageRequest req = new GetCoverageRequest(keyValuePairs);

            return CoverageRequestProcessor.processCoverageRequest(req,urlBase);
    }






    public static int getRequestType(String query, HashMap<String,String> keyValuePairs) throws WcsException{

        if(query==null)
            throw new WcsException("Missing WxS query string.",
                    WcsException.MISSING_PARAMETER_VALUE,"service");

        String[] pairs = query.split("&");

        String[] tmp;

        for(String pair: pairs){
            tmp = pair.split("=");
            if(tmp.length != 2)
                throw new WcsException("Poorly formatted request URL.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        tmp[0]);

            keyValuePairs.put(tmp[0],tmp[1]);
        }

        // Make sure the client is looking for a WCS service....
        String s = keyValuePairs.get("service");
        if(s==null || !s.equals(WCS.SERVICE))
            throw new WcsException("Only the WCS service (version "+
                    WCS.CURRENT_VERSION+") is supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,s);


        s = keyValuePairs.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(s.equals("GetCapabilities")){
            return WCS.GET_CAPABILITIES;
        }
        else if(s.equals("DescribeCoverage")){
            return WCS.DESCRIBE_COVERAGE;
        }
        else if(s.equals("GetCoverage")){
            return WCS.GET_COVERAGE;
        }
        else {
            throw new WcsException("The parameter 'request' has an invalid " +
                    "value of '"+s+"'.",
                    WcsException.INVALID_PARAMETER_VALUE,"request");
        }


    }


}
