package opendap.bes.dapResponders;

import opendap.bes.BESError;
import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import opendap.dap.Request;
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
public class IsoRubric extends BesDapResponder {
    private Logger log;

    private static String _preferredRequestSuffix = ".rubric";
    private static String defaultRequestSuffixRegex = "\\"+ _preferredRequestSuffix;


    public IsoRubric(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public IsoRubric(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }

    public IsoRubric(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        setServiceRoleId("http://services.opendap.org/dap2/iso-19115-score");
        setServiceMediaType("text/html");
        setServiceTitle("ISO-19115-Score");
        setServiceDescription("ISO 19115 Metadata Representation conformance score for this dataset.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_ISO_Conformance_Score_Service");
        setPreferredServiceSuffix(_preferredRequestSuffix);
    }


    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        BesApi besApi = getBesApi();

        Request oreq = new Request(null,request);


        // This first bit just collects a bunch of information about the request

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSourceId = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        String context = request.getContextPath();


        String xmlBase = request.getRequestURL().toString();
        int suffix_start = xmlBase.lastIndexOf("." + requestSuffix);
        xmlBase = xmlBase.substring(0, suffix_start);


        log.debug("Sending rubric Response() for dataset: " + dataSourceId);


        // Set up up the response header
        String accepts = request.getHeader("Accepts");

        response.setContentType(getServiceMediaType());

        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", "text/html");


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
            error.sendErrorResponse(_systemPath,context, response);
        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",dataSourceId);

            String currentDir = System.getProperty("user.dir");
            log.debug("Cached working directory: "+currentDir);


            String xslDir = _systemPath + "/nciso/xsl";


            log.debug("Changing working directory to "+ xslDir);
            System.setProperty("user.dir",xslDir);

            String xsltDocName = "OPeNDAPDDCount-HTML.xsl";


            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);


            transformer.setParameter("docsService",oreq.getDocsServiceLocalID());
            transformer.setParameter("HyraxVersion",Version.getHyraxVersionString());

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform( new JDOMSource(ddx),os);




            os.flush();
            log.info("Sent Rubric version of DDX.");
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }


    }



}
