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

package opendap.ngap;

import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapDispatcher;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
public class NgapDispatchHandler extends BesDapDispatcher {

    private static final AtomicLong ngapServiceEndpointCounter;
    static {
        ngapServiceEndpointCounter = new AtomicLong(0);
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
    private String _prefix = "ngap/";
    private NgapBesApi _besApi;
    //private NGAPForm _ngapForm;

    public NgapDispatchHandler() {
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

        _besApi = new NgapBesApi(_prefix);
        super.init(servlet, config, _besApi);
        //_ngapForm  =  new NGAPForm(getSystemPath(), _prefix);
        _initialized=true;
    }

    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        String relativeURL = ReqInfo.getLocalUrl(request);
        log.debug("relativeURL:    "+relativeURL);

        if(relativeURL.startsWith("/"))
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
                    // This could be made a real page, but having something
                    // Should reduce problems with health check clients
                    // beating on the endpoint.
                    ServletOutputStream sos = response.getOutputStream();
                    sos.println("-------------------------------------");
                    sos.println("      NGAP Service Endpoint");
                    sos.println("-   -   -   -   -   -   -   -   -   -");
                    sos.println("       This page: " + ngapServiceEndpointCounter.incrementAndGet());
                    sos.println("NGAP DAP Service: " + dapServiceCounter.get());
                    sos.println("    All Requests: " + reqCounter.get());
                    sos.println("-------------------------------------");
                    sos.flush();
                }
                else {
                    log.info("Sending NGAP Response");
                    if (!super.requestDispatch(request, response, true)) {
                        if (!response.isCommitted()) {
                            String s = Util.dropSuffixFrom(relativeURL, Pattern.compile(NgapBesApi.MATCH_LAST_DOT_SUFFIX_REGEX_STRING));
                            throw new opendap.http.error.BadRequest("The requested DAP response suffix of '" +
                                    relativeURL.substring(s.length()) + "' is not recognized by this server.");
                        } else {
                            isMyRequest = false;
                            log.error("The response was committed prior to encountering a problem. Unable to send a 404 error. Giving up...");
                        }
                    }
                    log.info("Sent DAP NGAP Response.");
                    dapServiceCounter.incrementAndGet();
                }
            }

        }
        return isMyRequest;
    }


    private void ingestPrefix(Element config) throws BadConfigurationException {

        _prefix = "ngap";

        if (config != null) {

            Element ngapService = config.getChild("NgapService");
            if (ngapService != null) {
                Element e = ngapService.getChild("prefix");
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


}
