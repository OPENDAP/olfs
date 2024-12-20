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

package opendap.bes;


import opendap.PathBuilder;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.namespaces.THREDDS;
import opendap.services.Service;
import opendap.services.ServicesRegistry;
import opendap.services.WebServiceHandler;
import opendap.threddsHandler.InheritedMetadataManager;
import opendap.viewers.NcWmsService;
import opendap.viewers.WcsService;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 11:28:25 AM
 */
public class BESThreddsDispatchHandler implements DispatchHandler {

    private HttpServlet d_servlet;
    private String d_systemPath;


    private org.slf4j.Logger d_log;
    //private Pattern matchPattern =  Pattern.compile(".*.catalog.xml");

    private boolean d_initialized;

    private BesApi d_besApi;

    private  boolean d_allowDirectDataSourceAccess = false;

    public BESThreddsDispatchHandler(){
        d_servlet = null;
        d_log = org.slf4j.LoggerFactory.getLogger(getClass());
        d_initialized = false;
    }


    public void init(HttpServlet s,Element config) throws Exception{
        init(s,config, new BesApi());
    }



    public void init(HttpServlet s,Element config, BesApi besApi) throws Exception{

        if(d_initialized) return;

        d_servlet = s;
        d_systemPath = ServletUtil.getSystemPath(d_servlet,"");

        d_besApi = besApi;

        d_allowDirectDataSourceAccess = false;
        Element dv = config.getChild("AllowDirectDataSourceAccess");
        if (dv != null) {
            d_allowDirectDataSourceAccess = true;
        }
        d_log.info("AllowDirectDataSourceAccess: {}", d_allowDirectDataSourceAccess);

        d_initialized = true;
        d_log.info("END.");

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception{


        boolean isThreddsRequest = false;


        String datasetname = ReqInfo.getDataSetName(request);
        String reqSuffix = ReqInfo.getRequestSuffix(request);



        if(     datasetname!=null &&
                datasetname.equalsIgnoreCase("catalog") &&
                reqSuffix!=null   &&
                reqSuffix.equalsIgnoreCase("xml")
                ){
            isThreddsRequest = true;
        }

        if(isThreddsRequest){
            d_log.debug("Identified a THREDDS request.");
            return true;
        }

        d_log.debug("Not a THREDDS request.");
        return false;
    }


    /**
     * Handles a request for a THREDDS catalog.
     * @param request The request to be handled.
     * @param response The response object into which the response information
     * will be placed.
     * @throws Exception
     */
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {


        d_log.debug("handleRequest() - Processing THREDDS request.");
        Request oreq = new Request(d_servlet,request);

        d_log.debug(ServletUtil.probeRequest(d_servlet, request));

        // Construct catalog name
        String besCatalogName = Scrub.urlContent(oreq.getRelativeUrl());
        if (besCatalogName.endsWith("/catalog.xml")) {
            besCatalogName = besCatalogName.substring(0, besCatalogName.lastIndexOf("catalog.xml"));
        }
        if (!besCatalogName.endsWith("/"))
            besCatalogName += "/";

        besCatalogName = PathBuilder.normalizePath(besCatalogName,true,false);
        d_log.debug("handleRequest() - besCatalogName:  " + besCatalogName);


        // Get the BES catalog for this node.
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        Document showNodeDoc = new Document();
        d_besApi.getBesNode(besCatalogName, showNodeDoc);
        d_log.debug(xmlo.outputString(showNodeDoc));

        // Load the XSL for BESCatalog -> THREDDS catalog
        String xsltDoc = d_systemPath + "/xsl/node_catalog.xsl";
        Transformer showNodeToThreddsCatalog = new Transformer(xsltDoc);

        //////////////////////////////////////////////////////////////////////
        // Configure services

        // Add a DAP service, because we are a DAP server above all else.
        showNodeToThreddsCatalog.setParameter("dapService",oreq.getServiceLocalId());

        String base = null;
        String dsId;
        String matchRegex;

        // Add WMS if we have it
        Service s = ServicesRegistry.getWebServiceById(NcWmsService.ID);
        if(s!=null && s instanceof NcWmsService){
            NcWmsService nws = (NcWmsService) s;
            base  = nws.getBase();
            dsId = nws.getDynamicServiceId();
            showNodeToThreddsCatalog.setParameter("ncWmsServiceBase",base);
            showNodeToThreddsCatalog.setParameter("ncWmsDynamicServiceId",dsId);
        }
        d_log.debug("handleRequest() - ncWMS service base: {}",base);


        // Add WCS Services - We know there may be more than one...
        Vector<WebServiceHandler> wcsServices = ServicesRegistry.getWebServicesLike(WcsService.ID);
        if(!wcsServices.isEmpty()) {

            Element wcsServicesElement = new Element("WcsServices");
            for(WebServiceHandler wsh : wcsServices){
                if (wsh instanceof WcsService) {
                    WcsService wcs = (WcsService) wsh;
                    base = wcs.getBase();
                    while(base.endsWith("/")&&base.length()>1)
                        base = base.substring(0,base.length()-1);
                    dsId = wcs.getDynamicServiceId();
                    matchRegex =  wcs.getMatchRegexString();
                    Element wcsService = new Element("Wcs");
                    wcsService.setAttribute("name",wsh.getServiceId());
                    wcsService.setAttribute("base",base);
                    wcsService.setAttribute("dynamicServiceId",dsId);
                    wcsService.setAttribute("matchRegex",matchRegex);
                    wcsServicesElement.addContent(wcsService);
                }
                d_log.debug("handleRequest() - WCS service base: {}", base);

            }
            if(wcsServicesElement.getContentSize()>0){
                showNodeToThreddsCatalog.setParameter(wcsServicesElement);
            }
        }

        if(d_allowDirectDataSourceAccess)
            showNodeToThreddsCatalog.setParameter("allowDirectDataSourceAccess","true");

        showNodeToThreddsCatalog.setParameter("datasetUrlResponseType",BesDapDispatcher.datasetUrlResponseActionStr());

        JDOMSource besCatalog = new JDOMSource(showNodeDoc);

        String threddsCatalogID = oreq.getServiceLocalId() + (besCatalogName.startsWith("/")?"":"/") + besCatalogName;


        response.setContentType("text/xml");
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "thredds_catalog");


        if(InheritedMetadataManager.hasInheritedMetadata(threddsCatalogID)){
            d_log.debug("handleRequest() - Found inherited metadata for collection '"+ besCatalogName +"'");

            // Go get the inherited metadata elements.
            Vector<Element> metadata = InheritedMetadataManager.getInheritedMetadata(threddsCatalogID);

            // Transform the BES  showCatalog response into a thredds catalog
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            showNodeToThreddsCatalog.transform(besCatalog, baos);

            // Parse the thredds catalog into a JDOM document.
            SAXBuilder sb = new SAXBuilder();
            Document threddsCatalog = sb.build(new ByteArrayInputStream(baos.toByteArray()));

            // Get the top level dataset
            Element catalog = threddsCatalog.getRootElement();
            Element topDataset = catalog.getChild("dataset", THREDDS.NS);

            // Add the metadata content to the dataset element.
            d_log.debug("handleRequest() - Adding inherited metadata to catalog");
            topDataset.addContent(1,metadata);

            // Get the service definitions (if any) used by the inherited metadata?
            Element inheritedServicesElement = InheritedMetadataManager.getInheritedServices(threddsCatalogID);
            d_log.debug("handleRequest() - Collecting inherited services.");
            Iterator i = inheritedServicesElement.getDescendants(new ElementFilter("service",THREDDS.NS));
            HashMap<String, Element> inheritedServices = new HashMap<String, Element>();
            Element service;
            while(i.hasNext()){
                service = (Element)i.next();
                inheritedServices.put(service.getAttributeValue("name"),service);
            }

            if(!inheritedServices.isEmpty()){
                d_log.debug("handleRequest() - Collecting existing services.");
                i = threddsCatalog.getDescendants(new ElementFilter("service",THREDDS.NS));
                HashMap<String, Element> existingServices = new HashMap<String, Element>();
                while(i.hasNext()) {
                    service = (Element) i.next();
                    existingServices.put(service.getAttributeValue("name"), service);
                }

                String iServiceName;

                for(Element inheritedService: inheritedServices.values()){
                    iServiceName = inheritedService.getAttributeValue("name");
                    d_log.debug("handleRequest() - Inherited service has service '"+iServiceName+"' - Checking existing services...");

                    Element existingService = existingServices.get(iServiceName);

                    if(existingService!=null){
                        String iServiceType = inheritedService.getAttributeValue("serviceType");
                        String iServiceBase = inheritedService.getAttributeValue("base");

                        String eServiceType = existingService.getAttributeValue("serviceType");
                        String eServiceBase = existingService.getAttributeValue("base");

                        if(!iServiceType.equalsIgnoreCase(eServiceType) || !iServiceBase.equals(eServiceBase)){
                            d_log.warn("Removing conflicting service definition for service '"+iServiceName+"' from inherited services");
                            inheritedService.detach();
                        }

                    }
                }
                @SuppressWarnings("unchecked") //suppressed the 'unchecked' warning being thrown here, SBL 9.13.21
                Collection<Element> servicesToAdd = (Collection<Element>) inheritedServicesElement.getChildren("service",THREDDS.NS);
                Vector<Element> services = new Vector<Element>();
                for(Element e : servicesToAdd){
                    services.add(e);
                }
                for(Element e : services){
                    e.detach();
                }
               threddsCatalog.getRootElement().addContent(1,services);
            }
            // Transmit the catalog.
            xmlo.output(threddsCatalog,response.getOutputStream());
        }
        else {
            // Transform the BES showCatalog response intp a THREDDS catalog and send it off to the client.
            showNodeToThreddsCatalog.transform(besCatalog, response.getOutputStream());
        }
        d_log.debug("handleRequest() - THREDDS showCatalogDoc request processed.");
    }


    /**
     * Since the user can modify the THREDDS catalogs without
     * changing the underlying data source, AND we can't ask the THREDDS
     * library to tell us about the last modified times of the catalog, AND we
     * don't know which time to return (catalog modified time, OR dataset
     * modified time) we punt and return -1.
     *
     * @param req The request for which to get the last modified time.
     * @return The last time the thing refered to in the request was modified.
     */
    public long getLastModified(HttpServletRequest req){
        String name = ReqInfo.getLocalUrl(req);

        d_log.debug("getLastModified(): Tomcat requesting getlastModified() for collection: {}", name );
        d_log.debug("getLastModified(): Returning current date/time" );

        return new Date().getTime();
    }



    public void destroy(){
        d_servlet = null;
        d_initialized = false;
        d_log.info("Destroy complete.");

    }




}
