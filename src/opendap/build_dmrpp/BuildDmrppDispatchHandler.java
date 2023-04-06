/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.build_dmrpp;

import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapDispatcher;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import org.apache.catalina.util.XMLWriter;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 * User: dan
 * Date: 2/13/20
 * Time: 1:35 PM
 * Cloned from: opendap.gateway
 * To change this template use File | Settings | File Templates.
 */
public class BuildDmrppDispatchHandler extends BesDapDispatcher {

    private static final AtomicLong build_dmrppServiceEndpointCounter;
    static {
        build_dmrppServiceEndpointCounter = new AtomicLong(0);
    }

    private static final AtomicLong dapServiceCounter;
    static {
        dapServiceCounter = new AtomicLong(0);
    }

    private static final AtomicLong reqCounter;
    static {
        reqCounter = new AtomicLong(0);
    }

    private Logger log;
    private boolean _initialized;
    private String _prefix = "build_dmrpp";
    private BuildDmrppBesApi _besApi;

    private static final String d_landingPage="/docs/ngap/ngap.html";

    public BuildDmrppDispatchHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;
        _besApi = null;
    }

    @Override
    public void init(HttpServlet servlet, Element config) throws Exception {
        init(servlet,config,null);
    }


    @Override
    public void init(HttpServlet servlet, Element config, BesApi ignored) throws Exception {

        if(_initialized)
            return;

        ingestPrefix(config);

        _besApi = new BuildDmrppBesApi(_prefix);
        super.init(servlet, config, _besApi);
        _initialized=true;
    }

    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        User user = new User(request);
        QueryParameters qp = new QueryParameters(request);

        String relativeURL = ReqInfo.getLocalUrl(request);
        log.debug("relativeURL:    "+relativeURL);

        while(relativeURL.startsWith("/") && relativeURL.length()>1)
            relativeURL = relativeURL.substring(1,relativeURL.length());
        boolean itsJustThePrefixWithoutTheSlash = _prefix.substring(0,_prefix.lastIndexOf("/")).equals(relativeURL);
        boolean itsJustThePrefix = _prefix.equals(relativeURL);
        boolean isMyRequest = itsJustThePrefixWithoutTheSlash || relativeURL.startsWith(_prefix);

        if(isMyRequest) {
            if (sendResponse) {

                if(itsJustThePrefixWithoutTheSlash ){
                    response.sendRedirect(_prefix);
                    log.debug("Sent redirect to service prefix: "+_prefix);
                    return true;
                }

                reqCounter.incrementAndGet();
                if(itsJustThePrefix){
                    sendSimpleNgapLandingPage(response);
                }
                else {
                    log.info("Sending build_dmrpp Response");

                    if(relativeURL.startsWith(_prefix)){
                        relativeURL = relativeURL.substring(_prefix.length());
                    }

                    Document build_dmrpp_cmd = _besApi.getBuildDmrppDocument(user, relativeURL, qp);

                    XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                    log.error(xmlo.outputString(build_dmrpp_cmd));

                    _besApi.besTransaction(relativeURL, build_dmrpp_cmd, response.getOutputStream());

                    log.info("Sent DAP build_dmrpp Response.");
                    dapServiceCounter.incrementAndGet();
                }
            }

        }
        return isMyRequest;
    }


    private void ingestPrefix(Element config) throws BadConfigurationException {

        _prefix = "build_dmrpp";

        if (config != null) {

            Element build_dmrppService = config.getChild("build_dmrppService");
            if (build_dmrppService != null) {
                Element e = build_dmrppService.getChild("prefix");
                if (e != null) {
                    _prefix = e.getTextTrim();
                    if (_prefix.equals("/")) {
                        String msg = "Bad Configuration. The <Handler> " +
                                "element that declares " + this.getClass().getName() +
                                " MUST provide 1 <prefix>  " +
                                "child element whose value may not be equal to \"/\"";
                        log.error(msg);
                        throw new BadConfigurationException(msg);
                    }
                }
            }
        }
        if (!_prefix.endsWith("/"))
            _prefix += "/";

        if (_prefix.startsWith("/"))
            _prefix = _prefix.substring(1);

        log.info("Using prefix=" + _prefix);

    }


    private void sendSimpleNgapLandingPage(HttpServletResponse response) throws IOException {
        // This could be made a real page (JSP?), but having something
        // simple in place should reduce problems caused by ELB health
        // check clients beating on the endpoint.
        response.setContentType("text/html");
        ServletOutputStream sos = response.getOutputStream();
        sos.println("<html>");
        sos.println("<head>");
        sos.println("<meta http-equiv=\"refresh\" content=\"60\">");
        sos.println("<title>OPeNDAP Hyrax: NGAP Service</title>");
        sos.println("</head>");
        sos.println("<body>");
        sos.print("<p style='");
        sos.print("font-family: Courier; ");
        sos.print("text-align: center; ");
        sos.print("transform: translate(0px, 100px); ");
        sos.println("'>");
        sos.println("-------------------------------------<br/>");
        sos.println("      NGAP Service Endpoint<br/>");
        sos.println(". . . . . . . . . . . . . . . . .<br/>");
        sos.println("All Requests: " + reqCounter.get() + "<br/>");
        sos.println(" DAP Service: " + dapServiceCounter.get() + "<br/>");
        sos.println("   This page: " + build_dmrppServiceEndpointCounter.incrementAndGet() + "<br/>");
        sos.println("-------------------------------------<br/>");
        sos.println("</p>");
        sos.println("</body>");
        sos.println("</html>");
        sos.flush();

    }


}
