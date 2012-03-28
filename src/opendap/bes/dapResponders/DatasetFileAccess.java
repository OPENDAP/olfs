package opendap.bes.dapResponders;

import opendap.bes.BESDataSource;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapResponder;
import opendap.coreServlet.DataSourceInfo;
import opendap.coreServlet.MimeTypes;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.ppt.PPTException;
import org.apache.commons.httpclient.HttpStatus;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/18/12
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatasetFileAccess extends BesDapResponder {

    private Logger log;

    private static String defaultRequestSuffixRegex = "\\.file";

    private boolean allowDirectDataSourceAccess;

    private String  ncmlFilePatternRegexString = ".*\\.ncml";
    private Pattern ncmlRequestPattern = Pattern.compile(ncmlFilePatternRegexString, Pattern.CASE_INSENSITIVE);


    public DatasetFileAccess(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public DatasetFileAccess(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }

    public DatasetFileAccess(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }

    public boolean needsBesToMatch(){
        return true;
    }

    public boolean needsBesToRespond(){
        return true;
    }


    public void setAllowDirectDataSourceAccess(boolean allowed){
        allowDirectDataSourceAccess = allowed;
    }

    public boolean allowDirectDataSourceAccess(){
        return allowDirectDataSourceAccess;
    }


    boolean _isDap2Responder = false;
    void setDap2Response(boolean val){
        _isDap2Responder = val;
    }
    boolean isDap2Response(){
        return _isDap2Responder;
    }


    /**
     * @param relativeUrl
     * @return
     */
    @Override
    public boolean matches(String relativeUrl) {


        if(isDap2Response()){

            String besDataSourceId = relativeUrl;

            Pattern requestMatchPattern = getRequestMatchPattern();
            Matcher m = requestMatchPattern.matcher(besDataSourceId);
            if (m.matches()) {
                try {
                    DataSourceInfo dsi = new BESDataSource(besDataSourceId, getBesApi());
                    if (dsi.isDataset()) {
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("matches() failed with an Exception. Msg: '{}'", e.getMessage());
                }
            }
        }
        else {
            return super.matches(relativeUrl);
        }


        return false;

    }


    public void respondToHttpGetRequest(HttpServletRequest req, HttpServletResponse response) throws Exception {

        String requestUrl = req.getRequestURL().toString();
        String dataSourceId = ReqInfo.getLocalUrl(req);
        String serviceUrl = ReqInfo.getFullServiceContext(req);

        if(!isDap2Response()){
            dataSourceId = ReqInfo.getBesDataSourceID(dataSourceId);
        }



        BesApi besApi = getBesApi();

        try {
            DataSourceInfo dsi = new BESDataSource(dataSourceId, besApi);
            if (dsi.sourceExists()) {
                if (!dsi.isNode()) {
                    if (dsi.sourceIsAccesible()) {
                        if (dsi.isDataset()) {
                            if (allowDirectDataSourceAccess()) {


                                if(ncmlRequestPattern.matcher(dataSourceId).matches())   {
                                    sendNcmlFile(dataSourceId, serviceUrl, response);
                                } else {
                                    sendDatasetFile(dataSourceId, response);
                                }

                            }
                            else {
                                log.warn("respondToHttpGetRequest() - Sending Access Denied for resource: "+Scrub.completeURL(requestUrl));
                                sendDirectAccessDenied(req,response);

                            }

                        }
                        else {
                            String errMsg = "Unable to locate BES resource: " + Scrub.completeURL(dataSourceId);
                            log.info("respondToHttpGetRequest() - {}", errMsg);
                            sendHttpErrorResponse(HttpStatus.SC_NOT_FOUND, errMsg, "docs", response);
                        }
                    }
                    else {
                        String errMsg = "BES data source {} is not accessible." + Scrub.completeURL(dataSourceId);
                        log.info("respondToHttpGetRequest() - {}", errMsg);
                        sendHttpErrorResponse(HttpStatus.SC_NOT_FOUND, errMsg, "docs", response);
                    }

                }
                else {
                    String errMsg = "You may not download nodes/directories, only files." + Scrub.completeURL(dataSourceId);
                    log.info("respondToHttpGetRequest() - {}", errMsg);
                    sendHttpErrorResponse(HttpStatus.SC_FORBIDDEN, errMsg, "docs", response);
                }

            }
            else {
                String errMsg = "Unable to locate BES resource: " + Scrub.completeURL(dataSourceId);
                log.info("matches() - {}", errMsg);
                sendHttpErrorResponse(HttpStatus.SC_NOT_FOUND, errMsg, "docs", response);
            }


        } catch (Exception e) {
            log.error("Problem communicating with BES meg: {}", e.getMessage());
        }





    }




    private void sendDatasetFile(String dataSourceId, HttpServletResponse response) throws IOException, BESError, BadConfigurationException, PPTException {
        log.debug("sendDatasetFile() - Sending dataset file \"" + dataSourceId + "\"");

        String downloadFileName = Scrub.fileName(dataSourceId.substring(dataSourceId.lastIndexOf("/") + 1));

        log.debug("respondToHttpGetRequest() - downloadFileName: " + downloadFileName);

        String contentDisposition = " attachment; filename=\"" + downloadFileName + "\"";

        response.setHeader("Content-Disposition", contentDisposition);


        String suffix = ReqInfo.getSuffix(dataSourceId);

        if (suffix != null) {
            String mType = MimeTypes.getMimeType(suffix);

            if (mType != null)
                response.setContentType(mType);

            log.debug("respondToHttpGetRequest() -    MIME type: " + mType + "  ");
        }

        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        BesApi besApi = getBesApi();

        ServletOutputStream sos = response.getOutputStream();
        if (!besApi.writeFile(dataSourceId, sos, erros)) {
            String msg = new String(erros.toByteArray());
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }

    }





    private void sendNcmlFile(String name, String serviceUrl, HttpServletResponse response) throws JDOMException, BESError, IOException, BadConfigurationException, PPTException {
        log.debug("sendNcmlFile() - Sending NcML File response for  \"" + name + "\"");


        Document ncml = getNcmlDocument(name);

        BesApi besApi = getBesApi();
        String besPrefix = besApi.getBESprefix(name);

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

        BesApi besApi = getBesApi();


        if(!besApi.writeFile(name, baos, erros)){
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






    private void sendDirectAccessDenied(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setContentType("text/html");
        response.setHeader("Content-Description", "BadURL");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        String serviceUrl = ReqInfo.getServiceUrl(request);


        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\"> ");
        pw.println("<head>  ");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />");
        pw.println("    <link rel='stylesheet' href='/opendap/docs/css/contents.css' type='text/css' />");
        pw.println("<title>Hyrax:  Access Denied</title>");
        pw.println("</head>");
        pw.println("");
        pw.println("<body>");
        pw.println("<img alt=\"OPeNDAP Logo\" src=\"/opendap/docs/images/logo.gif\"/>");

        pw.println("<h1>Hyrax : Access Denied (403) </h1>");
        pw.println("<hr align=\"left\" size=\"1\" noshade=\"noshade\" />");



        pw.println("<div class=\"large\">The requested URL directly references a data source. </div>");
        pw.println("<p>You must use the OPeNDAP request interface to get data from the data source.</p>");


        pw.println("<p>If you would like to start at the top level of this server, go here:");
        pw.println("<a href='" + Scrub.completeURL(serviceUrl) + "'>" + Scrub.completeURL(serviceUrl) + "</a></p>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@opendap.org\">support@opendap.org</a></p>");

        pw.println("<hr align=\"left\" size=\"1\" noshade=\"noshade\" />");
        pw.println("<h1 >Hyrax : Access Denied (403) </h1>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();

        /*
        pw.println("<table width=\"100%\" border=\"0\">");
        pw.println("  <tr>");
        pw.println("    <td><img src=\"/opendap/docs/images/forbidden.png\" alt=\"Forbidden!\" width=\"350\" height=\"313\" /></td> ");
        pw.println("    <td align=\"center\"><strong>You do not have permission to access the requested resource. </strong>");
        pw.println("      <p align=\"left\">&nbsp;</p>");
        pw.println("      <p align=\"left\">&nbsp;</p></td>");
        pw.println("  </tr>");
        pw.println("</table>");
        */

    }

































}







