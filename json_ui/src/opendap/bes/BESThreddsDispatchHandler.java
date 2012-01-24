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

package opendap.bes;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.namespaces.THREDDS;
import opendap.threddsHandler.InheritedMetadataManager;
import opendap.xml.Transformer;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.JDOMSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 11:28:25 AM
 */
public class BESThreddsDispatchHandler implements DispatchHandler {

    private DispatchServlet _servlet;
    private String _systemPath;


    private org.slf4j.Logger _log;
    private Pattern matchPattern =  Pattern.compile(".*.catalog.xml");

    private boolean _initialized;

    private BesApi _besApi;



    public BESThreddsDispatchHandler(){

        _servlet = null;

        _log = org.slf4j.LoggerFactory.getLogger(getClass());

        _initialized = false;


    }


    public void init(HttpServlet s,Element config) throws Exception{
        if(s instanceof DispatchServlet){
            init(((DispatchServlet)s),config);
        }
        else {
            throw new Exception(getClass().getName()+" must be used in " +
                    "conjunction with a "+DispatchServlet.class.getName());
        }
    }


    public void init(DispatchServlet s,Element config) throws Exception{

        if(_initialized) return;

        _servlet = s;
        _systemPath = ServletUtil.getSystemPath(_servlet,"");

        _besApi = new BesApi();

        _log.info("Initialized.");
        _initialized = true;

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
            _log.debug("Identified a THREDDS request.");
            return true;
        }

        _log.debug("Not a THREDDS request.");
        return false;
    }





    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {


        _log.debug("Processing THREDDS request.");

        Request oreq = new Request(_servlet,request);

        String context = request.getContextPath();

        

        _log.debug(Util.probeRequest(_servlet,request));


        String besCatalogName = Scrub.urlContent(oreq.getRelativeUrl());

        if (besCatalogName.endsWith("/catalog.xml")) {
            besCatalogName = besCatalogName.substring(0, besCatalogName.lastIndexOf("catalog.xml"));
        }

        if (!besCatalogName.endsWith("/"))
            besCatalogName += "/";

        if (besCatalogName.startsWith("/"))
            besCatalogName = besCatalogName.substring(1, besCatalogName.length());

        _log.debug("sendThreddsCatalog() - besCatalogName:  " + besCatalogName);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Document showCatalogDoc = new Document();

        if (_besApi.getCatalog(besCatalogName, showCatalogDoc)) {


            _log.debug(xmlo.outputString(showCatalogDoc));

            String xsltDoc = _systemPath + "/xsl/catalog.xsl";
            Transformer showCatalogToThreddsCatalog = new Transformer(xsltDoc);

            showCatalogToThreddsCatalog.setParameter("dapService",oreq.getDapServiceLocalID());
            if(FileDispatchHandler.allowDirectDataSourceAccess())
                showCatalogToThreddsCatalog.setParameter("allowDirectDataSourceAccess","true");

            JDOMSource besCatalog = new JDOMSource(showCatalogDoc);

            String threddsCatalogID = oreq.getDapServiceLocalID() + (besCatalogName.startsWith("/")?"":"/") + besCatalogName;


            response.setContentType("text/xml");
            Version.setOpendapMimeHeaders(request,response,_besApi);
            response.setHeader("Content-Description", "thredds_catalog");


            if(InheritedMetadataManager.hasInheritedMetadata(threddsCatalogID)){
                _log.debug("Found inherited metadata for collection '"+ besCatalogName +"'");

                // Go get the inherited metadata elements.
                Vector<Element> metadata = InheritedMetadataManager.getInheritedMetadata(threddsCatalogID);

                // Transform the BES  showCatalog response into a thredds catalog
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                showCatalogToThreddsCatalog.transform(besCatalog, baos);

                // Parse the thredds catalog into a JDOM document.
                SAXBuilder sb = new SAXBuilder();
                Document threddsCatalog = sb.build(new ByteArrayInputStream(baos.toByteArray()));

                // Get the top level dataset
                Element catalog = threddsCatalog.getRootElement();
                Element topDataset = catalog.getChild("dataset", THREDDS.NS);

                // Add the metadata content to the dataset element.
                _log.debug("Adding inherited metadata to catalog");
                topDataset.addContent(1,metadata);

                // Get the service definitions (if any) used by the inherited metadata?
                Element inheritedServicesElement = InheritedMetadataManager.getInheritedServices(threddsCatalogID);
                _log.debug("Collecting inherited services.");
                Iterator i = inheritedServicesElement.getDescendants(new ElementFilter("service",THREDDS.NS));
                HashMap<String, Element> inheritedServices = new HashMap<String, Element>();
                Element service;
                while(i.hasNext()){
                    service = (Element)i.next();
                    inheritedServices.put(service.getAttributeValue("name"),service);
                }

                if(!inheritedServices.isEmpty()){


                    
                    _log.debug("Collecting existing services.");
                    i = threddsCatalog.getDescendants(new ElementFilter("service",THREDDS.NS));
                    HashMap<String, Element> existingServices = new HashMap<String, Element>();
                    while(i.hasNext()){
                        service = (Element)i.next();
                        existingServices.put(service.getAttributeValue("name"),service);
                    }


                    String iServiceName;

                    for(Element inheritedService: inheritedServices.values()){
                        iServiceName = inheritedService.getAttributeValue("name");
                        _log.debug("Inherited service has service '"+iServiceName+"' - Checking existing services...");

                        Element existingService = existingServices.get(iServiceName);

                        if(existingService!=null){
                            String iServiceType = inheritedService.getAttributeValue("serviceType");
                            String iServiceBase = inheritedService.getAttributeValue("base");

                            String eServiceType = existingService.getAttributeValue("serviceType");
                            String eServiceBase = existingService.getAttributeValue("base");

                            if(!iServiceType.equalsIgnoreCase(eServiceType) || !iServiceBase.equals(eServiceBase)){
                                _log.warn("Removing conflicting service definition for service '"+iServiceName+"' from inherited services");
                                inheritedService.detach();
                            }

                        }
                    }

                    Collection<Element> servicesToAdd = inheritedServicesElement.getChildren("service",THREDDS.NS);
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
                showCatalogToThreddsCatalog.transform(besCatalog, response.getOutputStream());
            }



        } else {
            BESError besError = new BESError(showCatalogDoc);
            besError.sendErrorResponse(_systemPath, context, response);
            _log.error(besError.getMessage());

        }

        _log.debug("THREDDS showCatalogDoc request processed.");


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

        _log.debug("getLastModified(): Tomcat requesting getlastModified() for " +
                "collection: " + name );
        _log.debug("getLastModified(): Returning: -1" );

        return -1;
    }



    public void destroy(){
        _servlet = null;
        _initialized = false;
        _log.info("Destroy complete.");

    }




}
