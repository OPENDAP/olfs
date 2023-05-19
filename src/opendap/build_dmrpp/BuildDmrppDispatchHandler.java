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

import opendap.bes.*;
import opendap.bes.BesApi;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.*;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.Dmrpp;
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
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by IntelliJ IDEA.
 * User: dan
 * Date: 2/13/20
 * Time: 1:35 PM
 * Cloned from: opendap.gateway
 * To change this template use File | Settings | File Templates.
 */
public class BuildDmrppDispatchHandler implements DispatchHandler {

    private static final String DEFAULT_PREFIX = "build_dmrpp";
    private static final String THE_SLASH = "/";
    private static final AtomicLong buildDmrppServiceEndpointCounter;
    static {
        buildDmrppServiceEndpointCounter = new AtomicLong(0);
    }

    private static final AtomicLong buildDmrppServiceCounter;
    static {
        buildDmrppServiceCounter = new AtomicLong(0);
    }

    private static final AtomicLong reqCounter;
    static {
        reqCounter = new AtomicLong(0);
    }

    private final Logger log;
    private boolean _initialized;
    private String _prefix;

    public BuildDmrppDispatchHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;
        _prefix = DEFAULT_PREFIX;
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

        _initialized=true;
    }

    @Override
    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return requestDispatch(request,null,false);
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requestDispatch(request,response,true);
    }

    @Override
    public long getLastModified(HttpServletRequest req) {
        return new Date().getTime();
    }

    @Override
    public void destroy() {

    }

    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        User user = new User(request);
        QueryParameters qp = new QueryParameters(request);

        String invocation = request.getRequestURL().toString();
        String qs = request.getQueryString();
        if(qs!=null)
            invocation += "?" + qs;
        log.debug("invocation:    " + invocation);

        String resourceID = ReqInfo.getLocalUrl(request);
        log.debug("resourceID:    " + resourceID);

        while(resourceID.startsWith(THE_SLASH) && resourceID.length()>1)
            resourceID = resourceID.substring(1);

        boolean itsJustThePrefixWithoutTheSlash = false;
        boolean itsJustThePrefix = false;
        boolean itsJustTheSlash = resourceID.equals(THE_SLASH) && _prefix.equals(THE_SLASH);
        if(!itsJustTheSlash){
            itsJustThePrefix = _prefix.equals(resourceID);
            if(!_prefix.isEmpty()) {
                itsJustThePrefixWithoutTheSlash = _prefix.substring(0, _prefix.lastIndexOf(THE_SLASH)).equals(resourceID);
            }
        }
        boolean isMyRequest = itsJustThePrefixWithoutTheSlash || resourceID.startsWith(_prefix) || itsJustTheSlash ;

        if(isMyRequest) {
            if (sendResponse) {

                if(itsJustThePrefixWithoutTheSlash ){
                    response.sendRedirect(_prefix);
                    log.debug("Sent redirect to service prefix: "+_prefix);
                    return true;
                }

                reqCounter.incrementAndGet();
                if(itsJustThePrefix || itsJustTheSlash){
                    sendLandingPage(response);
                }
                else {
                    log.info("Sending build_dmrpp Response");

                    if(resourceID.startsWith(_prefix)){
                        resourceID = resourceID.substring(_prefix.length());
                    }
                    MediaType responseMediaType =  new Dmrpp();
                    // Stash the Media type in case there's an error downstream.
                    // That way the error handler will know how to encode the error.
                    RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

                    String downloadFileName = Scrub.fileName(resourceID.substring(resourceID.lastIndexOf(THE_SLASH) + 1));
                    downloadFileName += responseMediaType.getMediaSuffix();
                    log.debug("downloadFileName:  {}",downloadFileName );
                    response.setHeader("Content-Disposition", " attachment; filename=\"" +downloadFileName+"\"");

                    response.setContentType(responseMediaType.getMimeType());
                    response.setHeader("Content-Description", responseMediaType.getMimeType());

                    // Version.setOpendapMimeHeaders(request, response);


                    BuildDmrppBesApi buildDmrppBesApi = new BuildDmrppBesApi(_prefix);
                    Document buildDmrppCmdDoc;
                    BES bes = BESManager.getBES(resourceID);
                    int bes_timeout_seconds = bes.getTimeout() / 1000;

                    buildDmrppCmdDoc = buildDmrppBesApi.getBuildDmrppDocument(user, resourceID, qp, invocation, bes_timeout_seconds);

                    log.debug("Beginning BES transaction.");
                    if(log.isDebugEnabled()){
                        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                        log.debug("BES command document: \n-----------\n" + xmlo.outputString(buildDmrppCmdDoc) + "-----------\n");
                    }
                    ServletResponseTransmitCoordinator tc = new ServletResponseTransmitCoordinator(response);
                    bes.besTransaction(buildDmrppCmdDoc, response.getOutputStream(), tc);

                    long reqNum = buildDmrppServiceCounter.incrementAndGet();
                    log.info("Sent DAP build dmr++ response {}",reqNum);
                }
            }

        }
        return isMyRequest;
    }




    private void ingestPrefix(Element config) throws BadConfigurationException {

        _prefix = DEFAULT_PREFIX;

        if (config != null) {

            Element buildDmrppService = config.getChild("BuildDmrppService");
            if (buildDmrppService != null) {
                String prefix = buildDmrppService.getAttributeValue("prefix");
                if (prefix != null) {
                    _prefix = prefix;
                }
            }
        }
        if(_prefix.equals("")){
            _prefix=THE_SLASH;
        }
        if (!_prefix.endsWith(THE_SLASH))
            _prefix += THE_SLASH;

        if (_prefix.startsWith(THE_SLASH) && _prefix.length()>1)
            _prefix = _prefix.substring(1);

        log.info("Using prefix=" + _prefix);
    }


    private void sendLandingPage(HttpServletResponse response) throws IOException {
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
        sos.println("      Build dmr++ service endpoint<br/>");
        sos.println(". . . . . . . . . . . . . . . . .<br/>");
        sos.println("       All Requests: " + reqCounter.get() + "<br/>");
        sos.println("Build dmr++ service: " + buildDmrppServiceCounter.get() + "<br/>");
        sos.println("          This page: " + buildDmrppServiceEndpointCounter.incrementAndGet() + "<br/>");
        sos.println("-------------------------------------<br/>");
        sos.println("</p>");
        sos.println("</body>");
        sos.println("</html>");
        sos.flush();

    }


}
