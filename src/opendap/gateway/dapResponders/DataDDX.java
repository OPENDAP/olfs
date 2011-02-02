package opendap.gateway.dapResponders;

import opendap.bes.BesXmlAPI;
import opendap.bes.Version;
import opendap.coreServlet.MimeBoundary;
import opendap.coreServlet.ReqInfo;
import opendap.gateway.BesGatewayApi;
import opendap.gateway.HttpResponder;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;


public class DataDDX extends HttpResponder {



    private Logger log;

    private static String defaultRegex = ".*\\.dap";


    public DataDDX(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public DataDDX(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        sendSomeStuff(response);



    }

    private void sendSomeStuff(HttpServletResponse response) throws Exception {

        response.setContentType("text/html");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        XMLOutputter xmlo = new XMLOutputter();


        pw.println("<h2>DAP4 Data DDX Response</h2>");
        pw.println("<p>This request is being handled by: "+getClass().getName()+"</p>");

        pw.flush();


    }



    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDataDDX(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {

        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = request.getRequestURL().toString();


        MimeBoundary mb = new MimeBoundary();
        String startID = mb.newContentID();

        log.debug("sendDataDDX() for dataset: " + dataSource+
                "    CE: '" + constraintExpression + "'");


        response.setContentType("Multipart/Related;  "+
                                "type=\"text/xml\";  "+
                                "start=\"<"+startID+">\";  "+
                                "boundary=\""+mb.getBoundary()+"\"");


        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dap4_data_ddx");

        // This hedaer indicates to the client that the content of this response
        // is dependant on the HTTP request header XDAP-Accept
        response.setHeader("Vary", "XDAP-Accept");

        // Because the content of this response is dependant on a client provided
        // HTTP header (XDAP-Accept) it is useful to include this Cach-Control
        // header to make caching work correctly...
        response.setHeader("Cache-Control", "public");


        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");


        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        if(!BesXmlAPI.writeDataDDX(
                dataSource,
                constraintExpression,
                xdap_accept,
                xmlBase,
                startID,
                mb.getBoundary(),
                os,
                erros)){
            response.setHeader("Content-Description", "dods_error");
            String msg = new String(erros.toByteArray());
            log.error("BES Error. Message: \n"+msg);
            os.write(msg.getBytes());

        }


        os.print(mb.getClosingBoundary());
        os.flush();
        log.info("Sent DataDDX");

    }


}
