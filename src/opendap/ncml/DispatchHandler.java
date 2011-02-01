package opendap.ncml;

import opendap.bes.*;
import opendap.coreServlet.*;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * DispatchHandler for ISO responses from Hyrax
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler {



    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;
    private String ncmlRequestPatternRegexString;
    private Pattern ncmlRequestPattern;

    private Element _config;


    public DispatchHandler(){
        log = LoggerFactory.getLogger(getClass());
    }




    public void init(HttpServlet servlet,Element config) throws Exception {

        if(initialized) return;

        _config = config;
        dispatchServlet = servlet;

        ncmlRequestPatternRegexString = ".*\\.ncml";
        ncmlRequestPattern = Pattern.compile(ncmlRequestPatternRegexString, Pattern.CASE_INSENSITIVE);


        initialized = true;

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return ncmlDispatch(request, null, false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

       if(!ncmlDispatch(request, response, true))
           log.debug("FileDispatch request failed inexplicably!");

    }


    public long getLastModified(HttpServletRequest req) {

        String name = ReqInfo.getRelativeUrl(req);

        log.debug("getLastModified(): Tomcat requesting getlastModified() for collection: " + name );


        try {
            DataSourceInfo dsi = new BESDataSource(name);
            log.debug("getLastModified(): Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("getLastModified(): Returning: -1");
            return -1;
        }


    }



    public void destroy() {
        log.info("Destroy complete.");

    }

    /**
     * Performs dispatching for iso requests. ]
     *
     * @param request      The HttpServletRequest for this transaction.
     * @param response     The HttpServletResponse for this transaction
     * @param sendResponse If this is true a response will be sent. If it is
     *                     the request will only be evaluated to determine if a response can be
     *                     generated.
     * @return true if the request was serviced as a file request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean ncmlDispatch(HttpServletRequest request,
                                 HttpServletResponse response,
                                 boolean sendResponse) throws Exception {


        String requestURL = request.getRequestURL().toString();

        boolean isNcmlRequest = false;

        if(ncmlRequestPattern.matcher(requestURL).matches())   {
            String relativeUrl = ReqInfo.getRelativeUrl(request);
            DataSourceInfo dsi = new BESDataSource(relativeUrl);



            if (dsi.sourceExists() && dsi.isDataset() ) {
                isNcmlRequest = true;
                if (sendResponse) {
                    sendNcmlResponse(request, response);
                }
            }

        }

        return isNcmlRequest;

    }


    /**
     * This method is responsible for sending ISO metadata responses to the client.
     * @param request
     * @param response
     * @throws Exception
     */
    private void sendNcmlResponse(HttpServletRequest request,
                                  HttpServletResponse response)
            throws Exception {




        String serviceUrl = ReqInfo.getFullServiceContext(request);


        String name = ReqInfo.getRelativeUrl(request);


        Document ncml = getNcmlDocument(name);

        String besPrefix = BesXmlAPI.getBESprefix(name);

        String location;
        Element e;

        Iterator i = ncml.getDescendants(new ElementFilter());
        while(i.hasNext()){
            e  = (Element) i.next();
            location = e.getAttributeValue("location");
            if(location!=null){
                while(location.startsWith("/"))
                    location = location.substring(1);
                location = serviceUrl + besPrefix + location;
                e.setAttribute("location",location);
            }
        }



        XMLOutputter xmlo = new XMLOutputter();


        xmlo.output(ncml,response.getOutputStream());



    }


    private Document getNcmlDocument(String name)
            throws BESError, IOException, BadConfigurationException, PPTException, JDOMException {

        SAXBuilder sb = new SAXBuilder();

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document ncmlDocument;


        ByteArrayOutputStream baos = new ByteArrayOutputStream();



        if(!BesXmlAPI.writeFile(name, baos, erros)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            ByteArrayInputStream errorDoc = new ByteArrayInputStream(erros.toByteArray());
            ncmlDocument = sb.build(errorDoc);
        }
        else {
            ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());
            ncmlDocument = sb.build(is);
        }

        return ncmlDocument;

    }





}
