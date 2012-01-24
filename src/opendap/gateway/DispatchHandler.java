/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.gateway;

import opendap.gateway.dapResponders.*;
import org.slf4j.Logger;
import org.jdom.Element;
import opendap.coreServlet.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;


/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:44:39 PM
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler{

    private Logger log;
    private boolean _initialized;
    private HttpServlet dispatchServlet;

    private String systemPath;
    private String _prefix = "gateway/";

    private Element _config;

    private Vector<String> trustedHosts;
    private Vector<HttpResponder> responders;






    public DispatchHandler() {

        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        trustedHosts = new Vector<String>();
        responders = new Vector<HttpResponder>();
        _initialized = false;

    }

    public String getDataSourceURL(HttpServletRequest req) throws Exception {


        String relativeURL = ReqInfo.getLocalUrl(req);
        String requestSuffix = ReqInfo.getRequestSuffix(req);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String dataSourceUrl = relativeURL.substring(_prefix.length(),relativeURL.length());

        dataSourceUrl = dataSourceUrl.substring(0,dataSourceUrl.lastIndexOf("."+requestSuffix));

        dataSourceUrl = HexAsciiEncoder.hexToString(dataSourceUrl);

        return dataSourceUrl;

        /*

        if(!isTrustedHost(dataSourceUrl){
            log.error("No trusted hosts found to match: "+dataSourceUrl);
            return null;
        }
        else
            return dataSourceUrl;
        */

    }


    public void init(HttpServlet servlet, Element config) throws Exception {
        if (_initialized) return;

        String msg;
        Element host;
        List hosts;
        URL url;
        URI uri;


        dispatchServlet = servlet;
        systemPath = ServletUtil.getSystemPath(dispatchServlet,"");

        _config = config;

        ingestPrefix();
        //ingestTrustedHosts(config);


        HttpResponder hr;
        Pattern p;

        BesGatewayApi besApi = new BesGatewayApi();

        hr = new DDX(systemPath,_prefix,besApi);

        responders.add(hr);

        responders.add(new DDS(systemPath, _prefix, besApi));
        responders.add(new DAS(systemPath,_prefix, besApi));
        responders.add(new RDF(systemPath,_prefix,besApi));

        responders.add(new HtmlDataRequestForm(systemPath,_prefix, besApi));
        responders.add(new DatasetInfoHtmlPage(systemPath,_prefix, besApi));

        responders.add(new Dap2Data(systemPath,_prefix, besApi));
        responders.add(new Ascii(systemPath,_prefix, besApi));


        responders.add(new DataDDX(systemPath,_prefix, besApi));
        responders.add(new NetcdfFileOut(systemPath,_prefix, besApi));
        responders.add(new XmlData(systemPath,_prefix, besApi));

        responders.add(new GatewayForm(systemPath,_prefix));



        _initialized = true;
    }







    private void ingestTrustedHosts(Element config) throws URISyntaxException, MalformedURLException {
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



    private void ingestPrefix() throws Exception{

        String msg;

        Element e = _config.getChild("prefix");
        if(e!=null)
            _prefix = e.getTextTrim();

        if(_prefix.equals("/")){
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <prefix>  " +
                    "child element whose value may not be equal to \"/\"";
            log.error(msg);
            throw new Exception(msg);
        }



        if(!_prefix.endsWith("/"))
            _prefix += "/";

        if(_prefix.startsWith("/"))
            _prefix = _prefix.substring(1);

        log.info("Initialized. prefix="+ _prefix);

    }




    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return requestDispatch(request, null, false);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requestDispatch(request, response, true);
    }


    private boolean requestDispatch(HttpServletRequest request,
                                    HttpServletResponse response,
                                    boolean sendResponse)
            throws Exception {


        String serviceContext = ReqInfo.getFullServiceContext(request);



        String relativeURL = ReqInfo.getLocalUrl(request);


        log.debug("serviceContext: "+serviceContext);
        log.debug("relativeURL:    "+relativeURL);



        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        boolean isMyRequest = false;

        boolean itsJustThePrefixWithoutTheSlash = _prefix.substring(0,_prefix.lastIndexOf("/")).equals(relativeURL);


        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix) || itsJustThePrefixWithoutTheSlash ) {
                isMyRequest = true;
                if (sendResponse) {

                    if(itsJustThePrefixWithoutTheSlash){
                        response.sendRedirect(_prefix);
                        log.debug("Sent redirect to service prefix: "+_prefix);
                    }
                    else {
                        sendGatewayResponse(request, response);
                        log.info("Sent gateway Response");
                    }
                }
            }
        }

        return isMyRequest;

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    public void destroy() {
        log.info("Destroy Complete");
    }


    private String getName(HttpServletRequest req) {
        return req.getPathInfo();
    }



    private void sendGatewayResponse(HttpServletRequest request,
                                    HttpServletResponse response) throws Exception{


        String name = getName(request);

        log.debug("The client requested this: " + name);

        String requestURL = request.getRequestURL().toString();

        for (HttpResponder r : responders) {
            if (r.matches(requestURL)) {
                log.info("The request URL: " + requestURL + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                r.respondToHttpGetRequest(request, response);
                return;

            }
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        log.info("Sent BAD URL - No responder matched the request..");

    }




}
