package opendap.nciso;

import opendap.bes.BESDataSource;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesXmlAPI;
import opendap.coreServlet.*;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * DispatchHandler for ISO responses from Hyrax
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler {



    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;
    private String isoRequestPatternRegexString;
    private Pattern isoRequestPattern;

    private Element _config;


    public DispatchHandler(){
        log = LoggerFactory.getLogger(getClass());
    }




    public void init(HttpServlet servlet,Element config) throws Exception {

        if(initialized) return;

        _config = config;
        dispatchServlet = servlet;

        isoRequestPatternRegexString = ".*\\.iso";
        isoRequestPattern = Pattern.compile(isoRequestPatternRegexString, Pattern.CASE_INSENSITIVE);


        initialized = true;

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return isoDispatch(request, null, false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

       if(!isoDispatch(request, response, true))
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
    private boolean isoDispatch(HttpServletRequest request,
                               HttpServletResponse response,
                               boolean sendResponse) throws Exception {


        String requestURL = request.getRequestURL().toString();

        boolean isIsoResponse = false;

        if(isoRequestPattern.matcher(requestURL).matches())   {
            String relativeUrl = ReqInfo.getRelativeUrl(request);
            String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
            DataSourceInfo dsi = new BESDataSource(dataSource);

            if (dsi.sourceExists() && dsi.isDataset()) {
                isIsoResponse = true;
                if (sendResponse) {
                    sendIsoResponse(request,response);
                }
            }

        }

        return isIsoResponse;

    }


    /**
     * This method is responsible for sending ISO metadata responses to the client.
     * @param request
     * @param response
     * @throws Exception
     */
    private void sendIsoResponse(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {



        Document ddx = getDDX(request);



        sendSomeStuff(ddx,response);






    }


    private Document getDDX(HttpServletRequest request)
            throws JDOMException, IOException, BadConfigurationException, PPTException {

        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        String xmlBase = request.getRequestURL().toString();
        int suffix_start = xmlBase.lastIndexOf("." + requestSuffix);
        xmlBase = xmlBase.substring(0, suffix_start);

        String xdap_accept = "3.2";


        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document ddx = new Document();


        if(!BesXmlAPI.getDDXDocument(
                dataSource,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx)){

            log.error("BES Error. Message: \n"+erros.toString());

            ByteArrayInputStream errorDoc = new ByteArrayInputStream(erros.toByteArray());

            SAXBuilder sb = new SAXBuilder();
            ddx = sb.build(errorDoc);

        }

        return ddx;

    }



    private void sendSomeStuff(Document ddx,  HttpServletResponse response) throws Exception {

        response.setContentType("text/html");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        XMLOutputter xmlo = new XMLOutputter();


        pw.println("<h2>ISO Response</h2>");
        pw.println("<p>The request URL has been directed to the "+getClass().getName()+" </p>");
        pw.println("<p>The DDX associated with the data holding is:</p>");
        pw.println("<pre>");
        org.apache.commons.lang.StringEscapeUtils.escapeHtml(pw, xmlo.outputString(ddx));
        pw.println("</pre>");

        pw.flush();


    }





}
