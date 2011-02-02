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






public class Dap2Data extends HttpResponder {



    private Logger log;


    private static String defaultRegex = ".*\\.dods";


    public Dap2Data(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public Dap2Data(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }



    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String dataSourceUrl = BesGatewayApi.getDataSourceUrl(request, getPathPrefix());


        log.debug("sendDAP2Data() For: " + dataSource+
                "    CE: '" + constraintExpression + "'");

        response.setContentType("application/octet-stream");
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dods_data");


        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = BesGatewayApi.getRequestDocument(
                                                        BesGatewayApi.DAP2,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        null,
                                                        null,
                                                        null,
                                                        BesGatewayApi.DAP2_ERRORS);

        if(!BesGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDAP2Data() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();
        log.info("Sent DAP2 data response.");




    }

}
