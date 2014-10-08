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
package opendap.ncml;

import opendap.bes.BESError;
import opendap.bes.BESResource;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.bes.BesDapDispatcher;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ResourceInfo;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * NcmlFileDispatcher for NcML files held in Hyrax
 *
 * @deprecated No longer used. Replaced by code in opendap.bes.dapResponders.DatasetFileAccess
 */
public class NcmlFileDispatcher implements opendap.coreServlet.DispatchHandler {



    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;
    private String ncmlRequestPatternRegexString;
    private Pattern ncmlRequestPattern;

    private Element _config;

    private BesApi _besApi;


    public NcmlFileDispatcher(){
        log = LoggerFactory.getLogger(getClass());
    }




    public void init(HttpServlet servlet,Element config) throws Exception {

        if(initialized) return;

        _config = config;
        dispatchServlet = servlet;

        ncmlRequestPatternRegexString = ".*\\.ncml";
        ncmlRequestPattern = Pattern.compile(ncmlRequestPatternRegexString, Pattern.CASE_INSENSITIVE);

        _besApi = new BesApi();

        initialized = true;

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return ncmlDispatch(request, null, false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

       if(!ncmlDispatch(request, response, true))
           log.debug("FileDispatch request failed inexplicably!");

    }


    public long getLastModified(HttpServletRequest req) {

        String name = ReqInfo.getLocalUrl(req);

        log.debug("getLastModified(): Tomcat requesting getlastModified() for collection: " + name );


        try {
            ResourceInfo dsi = new BESResource(name, _besApi);
            log.debug("getLastModified(): Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("getLastModified(): Returning: -1");
            return -1;
        }


    }



    public void destroy() {
        log.info("Destroy complete.");

    }

    /**
     * Performs dispatching for ncml requests. ID
     *
     * @param request      The HttpServletRequest for this transaction.
     * @param response     The HttpServletResponse for this transaction
     * @param sendResponse If this is true a response will be sent. If it is
     *                     the request will only be evaluated to determine if a response can be
     *                     generated.
     * @return true if the request was serviced as a file request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean ncmlDispatch(HttpServletRequest request,
                                 HttpServletResponse response,
                                 boolean sendResponse) throws Exception {


        String requestURL = request.getRequestURL().toString();

        boolean isNcmlRequest = false;

        if(ncmlRequestPattern.matcher(requestURL).matches())   {
            String relativeUrl = ReqInfo.getLocalUrl(request);
            ResourceInfo dsi = new BESResource(relativeUrl,_besApi);

            if (dsi.sourceExists() && dsi.isDataset() && BesDapDispatcher.allowDirectDataSourceAccess()) {
                isNcmlRequest = true;
                if (sendResponse) {
                    sendNcmlResponse(request, response);
                }
            }

        }

        return isNcmlRequest;

    }


    /**
     * This method is responsible for sending ISO metadata responses to the client.
     * @param request
     * @param response
     * @throws Exception
     */
    private void sendNcmlResponse(HttpServletRequest request,
                                  HttpServletResponse response)
            throws Exception {




        String serviceUrl = ReqInfo.getFullServiceContext(request);


        String name = ReqInfo.getLocalUrl(request);


        Document ncml = getNcmlDocument(name);

        String besPrefix = _besApi.getBESprefix(name);

        String location;
        Element e;

        Iterator i = ncml.getDescendants(new ElementFilter());
        while(i.hasNext()){
            e  = (Element) i.next();
            location = e.getAttributeValue("location");
            if(location!=null){
                while(location.startsWith("/"))
                    location = location.substring(1);
                location = serviceUrl + besPrefix + location;
                e.setAttribute("location",location);
            }
        }



        XMLOutputter xmlo = new XMLOutputter();


        xmlo.output(ncml,response.getOutputStream());



    }


    private Document getNcmlDocument(String name)
            throws BESError, IOException, BadConfigurationException, PPTException, JDOMException {

        SAXBuilder sb = new SAXBuilder();

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document ncmlDocument;


        ByteArrayOutputStream baos = new ByteArrayOutputStream();



        if(!_besApi.writeFile(name, baos, erros)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            ByteArrayInputStream errorDoc = new ByteArrayInputStream(erros.toByteArray());
            ncmlDocument = sb.build(errorDoc);
        }
        else {
            ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());
            ncmlDocument = sb.build(is);
        }

        return ncmlDocument;

    }





}
