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

package opendap.gateway;

import opendap.bes.dap2Responders.DapDispatcher;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/15/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class DispatchHandler extends DapDispatcher {


    private Logger log;
    private boolean _initialized;
    private String _prefix = "gateway/";

    // private Element _config;

    private Vector<String> trustedHosts;


    private BesGatewayApi _besApi;


    private GatewayForm _gatewayForm;


    public DispatchHandler() {

        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        trustedHosts = new Vector<String>();
        _initialized = false;
        _besApi = null;


    }

    @Override
    public void init(HttpServlet servlet, Element config) throws Exception {

        ingestPrefix(config);

        _besApi = new BesGatewayApi(_prefix);
        init(servlet, config, _besApi);
        _gatewayForm  =  new GatewayForm(getSystemPath(), _prefix);
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


        boolean isMyRequest = false;
        boolean itsJustThePrefixWithoutTheSlash = _prefix.substring(0,_prefix.lastIndexOf("/")).equals(relativeURL);
        boolean itsJustThePrefix = _prefix.equals(relativeURL);

        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix) || itsJustThePrefixWithoutTheSlash ) {
                isMyRequest = true;

                if (sendResponse) {
                    log.info("Sending Gateway Response");

                    if(itsJustThePrefixWithoutTheSlash){
                        response.sendRedirect(_prefix);
                        log.debug("Sent redirect to service prefix: "+_prefix);
                    }
                    else if(itsJustThePrefix){

                        _gatewayForm.respondToHttpGetRequest(request,response);
                        log.info("Sent Gateway Access Form");

                    }
                    else {
                        if(!super.requestDispatch(request,response, true)  && !response.isCommitted()){
                            response.sendError(HttpServletResponse.SC_NOT_FOUND,"Unable to locate requested resource.");
                            log.info("Sent 404 Response.");
                        }
                        else
                            log.info("Sent DAP Gateway Response.");
                    }
                }

            }
        }

        return isMyRequest;
    }


    private void ingestTrustedHosts(Element config) throws URISyntaxException, MalformedURLException {

        if(config==null)
            return;

        String msg;
        Element hostElem;
        URL url;
        URI uri;
        List hosts = config.getChildren("trustedHost");

        if(hosts.isEmpty()){
            msg = "Configuration Warning: The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " did not provide 1 or more <wcsHost> " +
                    "child elements to limit the WCS services that " +
                    "may be accessed. This not recomended.";

            log.warn(msg);
        }
        else {

            for (Object o : hosts) {
                hostElem = (Element) o;
                String host = hostElem.getTextTrim();

                url = new URL(host);
                log.debug(Util.urlInfo(url));


                uri = new URI(host);
                log.debug(Util.uriInfo(uri));

                log.info("Adding " + url + " to allowed hosts list.");
                trustedHosts.add(host);
            }

        }
    }

    private boolean isTrustedHost(String url){

        for(String trustedHost : trustedHosts){
            if(url.startsWith(trustedHost))
                return true;
        }
        return false;
    }


    private void ingestPrefix(Element config) throws Exception {


        if (config != null) {

            String msg;

            Element e = config.getChild("prefix");
            if (e != null)
                _prefix = e.getTextTrim();

            if (_prefix.equals("/")) {
                msg = "Bad Configuration. The <Handler> " +
                        "element that declares " + this.getClass().getName() +
                        " MUST provide 1 <prefix>  " +
                        "child element whose value may not be equal to \"/\"";
                log.error(msg);
                throw new Exception(msg);
            }


            if (!_prefix.endsWith("/"))
                _prefix += "/";

            if (_prefix.startsWith("/"))
                _prefix = _prefix.substring(1);

        }
        log.info("Using prefix=" + _prefix);

    }

    String stripPrefix(String dataSource){

        if(dataSource.startsWith(_prefix))
            return dataSource.substring(_prefix.length(),dataSource.length());

        return dataSource;

    }



}
