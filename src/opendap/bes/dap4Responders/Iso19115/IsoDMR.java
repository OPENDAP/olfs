package opendap.bes.dap4Responders.Iso19115;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 8:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class IsoDMR extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".dmr.iso";


    public IsoDMR(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public IsoDMR(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public IsoDMR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("ISO-19115 Metadata");
        setServiceDescription("ISO-19115 metadata extracted form the normative DMR.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata");

        setNormativeMediaType(new ServiceMediaType("text","xml", defaultRequestSuffix));

        IsoRubricDMR rubric =  new IsoRubricDMR(sysPath, pathPrefix, besApi);

        addAltRepResponder(rubric);

        rubric.setRequestSuffixRegex(buildRequestMatchingRegex(rubric));

        log.debug("defaultRequestSuffix: '{}'", defaultRequestSuffix);

    }




    @Override
    public Element getServiceElement(String datasetUrl){
        Element service = getServiceElement();

        Element link = getNormativeLink(datasetUrl);

        service.addContent(link);

        for(Dap4Responder altRepResponder: getAltRepResponders()){
            MediaType altMediaType = altRepResponder.getNormativeMediaType();
            String href = datasetUrl + altMediaType.getMediaSuffix();
            link = getLinkElement(altMediaType.getMimeType(),href,altRepResponder.getServiceDescription());
            service.addContent(link);
        }

        return service;

    }


    @Override
    public String buildRequestMatchingRegex(Dap4Responder responder){

        StringBuilder s = new StringBuilder();
        s.append(buildRequestMatchingRegexWorker(responder));
        s.append("$");
        log.debug("Request Match Regex: {}",s.toString());
        return s.toString();

    }

    private String buildRequestMatchingRegexWorker(Dap4Responder responder){

        StringBuilder s = new StringBuilder();

        Dap4Responder[] altResponders = responder.getAltRepResponders();
        boolean hasAltRepResponders = altResponders.length>0;

        if(hasAltRepResponders)
            s.append("((");

        if(responder.getNormativeMediaType().getMediaSuffix().startsWith("."))
            s.append("\\");
        s.append(responder.getNormativeMediaType().getMediaSuffix());

        if(hasAltRepResponders)
            s.append(")");

        for(Dap4Responder altResponder: altResponders){

             s.append("|");

            s.append("(");

            s.append(buildRequestMatchingRegexWorker(altResponder));

            s.append(")");

        }

        if(hasAltRepResponders)
            s.append(")");

        return s.toString();

    }





    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String context = request.getContextPath();
        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);

        String resourceID = getResourceId(requestedResourceId, false);


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
            error.sendErrorResponse(_systemPath, context, response);
        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",resourceID);

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
            log.info("Sent {}",getServiceTitle());
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }



    }

}
