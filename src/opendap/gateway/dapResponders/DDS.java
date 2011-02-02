package opendap.gateway.dapResponders;

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
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDS extends HttpResponder {


    private Logger log;


    private static String defaultRegex = ".*\\.dds";


    public DDS(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public DDS(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String dataSourceUrl = BesGatewayApi.getDataSourceUrl(request, getPathPrefix());


        log.debug("sendDDS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document reqDoc = BesGatewayApi.getRequestDocument(
                                                        BesGatewayApi.DDS,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        null,
                                                        null,
                                                        null,
                                                        BesGatewayApi.DAP2_ERRORS);

        if(!BesGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){

            String msg = new String(erros.toByteArray());
            log.error("sendDDS() encountered a BESError: "+msg);
            os.write(msg.getBytes());
        }


        os.flush();
        log.debug("Sent DAP DDS.");




    }

}
