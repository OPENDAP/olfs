package opendap.bes.dap4Responders.DataResponse;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.dap.User;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CsvDR extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".csv";



    public CsvDR(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public CsvDR(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public CsvDR(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/data/csv");
        setServiceTitle("CSV Data Response");
        setServiceDescription("A comma separated values (CSV) representation of the DAP4 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Data_Service");

        setNormativeMediaType(new ServiceMediaType("text","plain", defaultRequestSuffix));

        log.debug("defaultRequestSuffix: '{}'", defaultRequestSuffix);

    }







    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String context = request.getContextPath();
        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String resourceID = getResourceId(requestedResourceId, false);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", "dap4:Dataset");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");



        String xdap_accept = "3.2";

        User user = new User(request);
        int maxRS = user.getMaxResponseSize();



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc =
                besApi.getRequestDocument(
                        BesApi.ASCII,
                        resourceID,
                        constraintExpression,
                        xdap_accept,
                        maxRS,
                        null,
                        null,
                        null,
                        BesApi.XML_ERRORS);

        if(!besApi.besTransaction(resourceID,reqDoc,os,erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_systemPath,context, response);
            log.error("respondToHttpGetRequest() encountered a BESError: "+besError.getMessage());
        }


        os.flush();
        log.debug("Sent {}",getServiceTitle());



    }


}
