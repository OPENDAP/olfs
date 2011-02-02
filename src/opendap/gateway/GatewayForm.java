package opendap.gateway;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/1/11
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayForm extends HttpResponder {



    Logger log;

    private static String defaultRegex = ".*";


    public GatewayForm(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public GatewayForm(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }




    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String contextPath = request.getContextPath();

        String gatewayFormFile = _systemPath + "/gateway/gateway_form.html";

        String form = readFileAsString(gatewayFormFile);

        form = form.replaceAll("<CONTEXT_PATH />",contextPath);
        form = form.replaceAll("<SERVLET_NAME />","/docs");


        log.debug("respondToHttpRequest(): Sending Gateway Page ");

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description", "gateway_form");

        ServletOutputStream sos  = response.getOutputStream();

        sos.println(form);

    }


}
