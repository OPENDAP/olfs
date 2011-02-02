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






public class DDX extends HttpResponder {



    private Logger log;



    private static String defaultRegex = ".*\\.ddx";


    public DDX(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public DDX(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }


    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
         sendDDX( request,  response);
    }

    public void sendDDX(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = request.getRequestURL().toString();


        String dataSourceUrl = BesGatewayApi.getDataSourceUrl(request, getPathPrefix());


        log.debug("sendDDX() for dataset: " + dataSource);

        response.setContentType("text/xml");
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dods_ddx");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = BesGatewayApi.getRequestDocument(
                                                        BesGatewayApi.DDX,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        xmlBase,
                                                        null,
                                                        null,
                                                        BesGatewayApi.XML_ERRORS);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        log.debug("BesGatewayApi.getRequestDocument() returned:\n "+xmlo.outputString(reqDoc));

        if(!BesGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDDX() encounterd a BESError: "+msg);
            os.write(msg.getBytes());

        }



        os.flush();
        log.info("Sent DAP DDX.");


    }

}
