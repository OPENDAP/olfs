/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
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


        String relativeURL = ReqInfo.getRelativeUrl(req);
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

    public static String urlInfo(URL url){
        String msg = "\n";

        msg += "URL: "+url.toString()+"\n";
        msg += "  protocol:      "+url.getProtocol()+"\n";
        msg += "  host:          "+url.getHost()+"\n";
        msg += "  port:          "+url.getPort()+"\n";
        msg += "  default port:  "+url.getDefaultPort()+"\n";
        msg += "  path:          "+url.getPath()+"\n";
        msg += "  query:         "+url.getQuery()+"\n";
        msg += "  file:          "+url.getFile()+"\n";
        msg += "  ref:           "+url.getRef()+"\n";
        msg += "  user info:     "+url.getUserInfo()+"\n";
        msg += "  hash code:     "+url.hashCode()+"\n";

        try {
            msg += "  URI:           "+url.toURI().toASCIIString()+"\n";
        } catch (URISyntaxException e) {
            msg += "  URI:            error: Could not express the URL as URI because: "+e.getMessage()+"\n";
        }

        return msg;
    }






    public static String uriInfo(URI uri){

        String msg = "\n";


        msg += "URI: "+uri.toString()+"\n";
        msg += "  Authority:              "+uri.getAuthority()+"\n";
        msg += "  Host:                   "+uri.getHost()+"\n";
        msg += "  Port:                   "+uri.getPort()+"\n";
        msg += "  Path:                   "+uri.getPath()+"\n";
        msg += "  Query:                  "+uri.getQuery()+"\n";
        msg += "  hashCode:               "+uri.hashCode()+"\n";
        msg += "  Fragment:               "+uri.getFragment()+"\n";
        msg += "  RawAuthority:           "+uri.getRawAuthority()+"\n";
        msg += "  RawFragment:            "+uri.getRawFragment()+"\n";
        msg += "  RawPath:                "+uri.getRawPath()+"\n";
        msg += "  RawQuery:               "+uri.getRawQuery()+"\n";
        msg += "  RawSchemeSpecificPart:  "+uri.getRawSchemeSpecificPart()+"\n";
        msg += "  RawUSerInfo:            "+uri.getRawUserInfo()+"\n";
        msg += "  Scheme:                 "+uri.getScheme()+"\n";
        msg += "  SchemeSpecificPart:     "+uri.getSchemeSpecificPart()+"\n";
        msg += "  UserInfo:               "+uri.getUserInfo()+"\n";
        msg += "  isAbsoulte:             "+uri.isAbsolute()+"\n";
        msg += "  isOpaque:               "+uri.isOpaque()+"\n";
        msg += "  ASCIIString:            "+uri.toASCIIString()+"\n";

        try {
            msg += "  URL:                    "+uri.toURL()+"\n";
        } catch (Exception e) {
            msg += "  URL:                    uri.toURL() FAILED msg="+e.getMessage();
        }

        return msg;
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

        hr = new DDX(systemPath,_prefix);

        responders.add(hr);

        responders.add(new DDS(systemPath,_prefix));
        responders.add(new DAS(systemPath,_prefix));
        responders.add(new RDF(systemPath,_prefix));

        responders.add(new HtmlDataRequestForm(systemPath,_prefix));
        responders.add(new DatasetInfoHtmlPage(systemPath,_prefix));

        responders.add(new Dap2Data(systemPath,_prefix));
        responders.add(new Ascii(systemPath,_prefix));


        responders.add(new DataDDX(systemPath,_prefix));
        responders.add(new NetcdfFileOut(systemPath,_prefix));
        responders.add(new XmlData(systemPath,_prefix));

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
                log.debug(urlInfo(url));


                uri = new URI(host);
                log.debug(uriInfo(uri));

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
            _prefix = _prefix.substring(1, _prefix.length());

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

        String relativeURL = ReqInfo.getRelativeUrl(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        boolean isMyRequest = false;

        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix)) {
                isMyRequest = true;
                if (sendResponse) {
                    sendGatewayResponse(request, response);
                    log.info("Sent gateway Response");
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

        String relativeUrl = ReqInfo.getRelativeUrl(request);

        String requestURL = request.getRequestURL().toString();

        for (HttpResponder r : responders) {
            if (r.matches(requestURL)) {
                log.info("The request URL: " + requestURL + " matches " +
                        "the pattern: \"" + r.getPattern() + "\"");

                r.respondToHttpRequest(request, response);
                return;

            }
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        log.info("Sent BAD URL - No responder matched the request..");

    }




}
