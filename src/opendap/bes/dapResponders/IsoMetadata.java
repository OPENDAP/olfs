package opendap.bes.dapResponders;

import opendap.bes.BESError;
import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/25/12
 * Time: 8:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class IsoMetadata extends BesDapResponder {

    private Logger log;

    private static String _preferredRequestSuffix = ".iso";
    private static String defaultRequestSuffixRegex = "\\"+ _preferredRequestSuffix;


    public IsoMetadata(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public IsoMetadata(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }

    public IsoMetadata(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        setServiceRoleId("http://services.opendap.org/dap4/iso-19115#");
        setServiceTitle("ISO-19115");
        setServiceDescription("ISO 19115 Metadata Representation of the Dataset (DDX) response.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_ISO_19115_Service");
        setPreferredServiceSuffix(_preferredRequestSuffix);
    }


    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        BesApi besApi = getBesApi();


        // This first bit just collects a bunch of information about the request

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSourceId = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        String context = request.getContextPath();


        String xmlBase = request.getRequestURL().toString();
        int suffix_start = xmlBase.lastIndexOf("." + requestSuffix);
        xmlBase = xmlBase.substring(0, suffix_start);


        log.debug("Sending ISO Response() for dataset: " + dataSourceId);


        // Set up up the response header
        String accepts = request.getHeader("Accepts");

        if(accepts!=null && accepts.equalsIgnoreCase("application/rdf+xml"))
            response.setContentType("application/rdf+xml");
        else
            response.setContentType("text/xml");

        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", "text/xml");


        ServletOutputStream os = response.getOutputStream();

        // Doing this insures that the DDX that
        String xdap_accept = "3.2";



        Document ddx = new Document();


        if(!besApi.getDDXDocument(
                dataSourceId,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx)){
            response.setHeader("Content-Description", "dap_error");

            BESError error = new BESError(ddx);
            error.sendErrorResponse(_systemPath, context, response);
        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",dataSourceId);

            String currentDir = System.getProperty("user.dir");
            log.debug("Cached working directory: "+currentDir);


            String xslDir = _systemPath + "/nciso/xsl";


            log.debug("Changing working directory to "+ xslDir);
            System.setProperty("user.dir",xslDir);

            String xsltDocName = "ddx2iso.xsl";


            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform( new JDOMSource(ddx),os);




            os.flush();
            os.close();
            log.info("Sent RDF version of DDX.");
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }





    }

}
