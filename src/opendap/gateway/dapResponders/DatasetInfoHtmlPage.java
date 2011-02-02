package opendap.gateway.dapResponders;

import opendap.bes.BESError;
import opendap.bes.BesXmlAPI;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import opendap.gateway.BesGatewayApi;
import opendap.gateway.HttpResponder;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;






public class DatasetInfoHtmlPage extends HttpResponder {



    private Logger log;



    private static String defaultRegex = ".*\\.info";


    public DatasetInfoHtmlPage(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public DatasetInfoHtmlPage(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String dataSourceUrl = BesGatewayApi.getDataSourceUrl(request, getPathPrefix());


        log.debug("sendINFO() for dataset: " + dataSource);

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = BesGatewayApi.getRequestDocument(
                                                        BesGatewayApi.INFO_PAGE,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        null,
                                                        null,
                                                        null,
                                                        BesGatewayApi.XML_ERRORS);

        if(!BesGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_systemPath,response);
            log.error("sendINFO() encountered a BESError: "+besError.getMessage());

        }


        os.flush();
        log.info("Sent DAP Info page.");




    }

}
