package opendap.bes.dap4Responders.Iso19115;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.dap.Request;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 8:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class IsoRubricDMR extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".dmr.rubric";



    public IsoRubricDMR(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public IsoRubricDMR(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public IsoRubricDMR(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("ISO-19115 Conformance Score.");
        setServiceDescription("ISO-19115 Conformance Score for the Dataset Metadata Response document.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata");

        setNormativeMediaType(new ServiceMediaType("text","html", defaultRequestSuffix));

        log.debug("defaultRequestSuffix: '{}'", defaultRequestSuffix);

    }







    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String context = request.getContextPath();
        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);

        String resourceID = getResourceId(requestedResourceId, false);

        Request oreq = new Request(null,request);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", "dap4:Dataset");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");


        OutputStream os = response.getOutputStream();


        String xdap_accept = "3.2";



        Document ddx = new Document();


        if(!besApi.getDDXDocument(
                resourceID,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx)){
            response.setHeader("Content-Description", "dap_error");

            BESError error = new BESError(ddx);
            error.sendErrorResponse(_systemPath,context, response);
        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",resourceID);

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
            log.info("Sent {}",getServiceTitle());
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }



    }


}
