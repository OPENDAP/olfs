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

package opendap.bes.dap4Responders.DatasetServices;

import opendap.bes.BESManager;
import opendap.bes.BesGroup;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.bes.dapResponders.BesApi;
import opendap.namespaces.DAP;
import opendap.namespaces.XML;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/4/12
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class NormativeDSR extends Dap4Responder {

    private Logger log;

    //private static String defaultRequestSuffixRegex = "\\.dmr(((\\.html)?)|((\\.xml)?))?$";
    //private static String defaultRequestSuffixRegex = "\\.dap(((\\.html)?)|((\\.xml)?))?$";


    private static String defaultRequestSuffix = "";


    private Vector<Dap4Responder> allServices = null;


    public NormativeDSR(String sysPath, BesApi besApi, Vector<Dap4Responder> services) {
        this(sysPath,null, defaultRequestSuffix,besApi,services);
    }

    public NormativeDSR(String sysPath, String pathPrefix, BesApi besApi, Vector<Dap4Responder> services) {
        this(sysPath,pathPrefix, defaultRequestSuffix,besApi,services );
    }

    public NormativeDSR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi, Vector<Dap4Responder> services) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        allServices = services;

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        log.debug("defaultRequestSuffix: '{}'", defaultRequestSuffix);


        setServiceRoleId("http://services.opendap.org/dap4/dataset-services");
        setServiceTitle("Dataset Services Response");
        setServiceDescription("An XML document itemizing the Services available for this dataset.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Services_Description_Service");
        setPreferredServiceSuffix("");

        setNormativeMediaType(new ServiceMediaType("application","vnd.opendap.dap4.dataset-services+xml", getRequestSuffix()));

        addAltRepResponder(new HtmlDSR(sysPath, pathPrefix, besApi, this));
        addAltRepResponder(new XmlDSR(sysPath, pathPrefix, besApi, this));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());


    }


    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }



    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String datasetUrl = request.getRequestURL().toString();

        String context = request.getContextPath()+"/";

        log.debug("Sending {} for dataset: {}",getServiceTitle(),datasetUrl);

        Document serviceDescription = new Document();

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", context+"xsl/datasetServices.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        serviceDescription.addContent( pi );

        Element datasetServices;

        datasetServices = getDatasetServicesElement(datasetUrl);

        serviceDescription.setRootElement(datasetServices);

        response.setContentType(getNormativeMediaType().getMimeType());
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        xmlo.output(serviceDescription,response.getOutputStream());

        log.debug("Sent {}",getServiceTitle());

    }


    public Element getDatasetServicesElement(String datasetUrl ) {

        Element datasetServices = new Element("DatasetServices",DAP.DAPv40_DatasetServices_NS);
        datasetServices.setAttribute("base",datasetUrl, XML.NS);
        datasetServices.addNamespaceDeclaration(DAP.DAPv40_DatasetServices_NS);

        datasetServices.addContent(getDapVersionElements(datasetUrl));
        //datasetServices.addContent(getSimpleHyraxVersion());
        datasetServices.addContent(getSimpleServerSoftwareVersionElement());


        if(allServices!=null){
            for(Dap4Responder service : allServices ){
                 datasetServices.addContent(service.getServiceElement(datasetUrl));
            }
        }

        datasetServices.addContent(getServerSideFunctions(datasetUrl));

        return datasetServices;

    }

    private Element getComplexServerSoftwareVersionElement(){
        BesApi besApi = getBesApi();

        Element serverVersion = new Element("ServerSoftwareVersion",DAP.DAPv40_DatasetServices_NS);

        Element hyraxVersion = null;
        try {
            hyraxVersion = besApi.getCombinedVersionDocument().getRootElement();
            hyraxVersion.detach();
        } catch (Exception e) {
            String msg = "Unable to acquire combined server version document from BESManager. Msg: " + e.getMessage();
            log.error(msg);
            hyraxVersion = new Element("Error");
            hyraxVersion.setText(msg);
        }
        serverVersion.addContent(hyraxVersion);
        return serverVersion;

    }

    private Element getSimpleServerSoftwareVersionElement(){

        Element serverVersion = new Element("ServerSoftwareVersion",DAP.DAPv40_DatasetServices_NS);

        String version = "Hyrax-"+opendap.bes.Version.getHyraxVersionString();
        serverVersion.setText(version);
        return serverVersion;

    }


    private Vector<Element> getDapVersionElements(String resourceId) {

        Vector<Element> dapVersions = new Vector<Element>();

        BesGroup besGroup = BESManager.getBesGroup(resourceId);

        TreeSet<String> besDapVersions = besGroup.getCommonDapVersions();

        Element versionElement;

        Iterator<String> i = besDapVersions.descendingIterator();
        while(i.hasNext()){
            String version = i.next();
            versionElement = new Element("DapVersion",DAP.DAPv40_DatasetServices_NS);
            versionElement.setText(version);
            dapVersions.add(versionElement);

        }

        return dapVersions;

    }

    private Vector<String> getDapVersions(List serviceVersionList ){

        Vector<String> dapVersions = new Vector<String>();

        for(Object o_serviceVersion : serviceVersionList){
            Element serviceVersion = (Element) o_serviceVersion;
            if(serviceVersion.getAttributeValue("name").equalsIgnoreCase("dap")){
                for (Object o : serviceVersion.getChildren("version",opendap.namespaces.BES.BES_NS)) {
                    Element dapVersionElement = (Element) o;
                    String dapVersionString = dapVersionElement.getTextTrim();
                    dapVersions.add(dapVersionString);
                }
            }
        }

        return dapVersions;

    }





    private  Vector<Element> getServerSideFunctions(String datasetUrl){


        Vector<Element> extensions = new Vector<Element>();

        Element function;

        function = getFunctionElement("geogrid",
                "http://services.opendap.org/dap4/server-side-function/geogrid",
                "Allows a DAP Grid variable to be sub-sampled using georeferenced values.",
                "http://docs.opendap.org/index.php/Server_Side_Processing_Functions#geogrid");
        extensions.add(function);


        function = getFunctionElement("grid",
                "http://services.opendap.org/dap4/server-side-function/grid",
                "Allows a DAP Grid variable to be sub-sampled using the values of the coordinate axes.",
                "http://docs.opendap.org/index.php/Server_Side_Processing_Functions#grid");
        extensions.add(function);


        function = getFunctionElement("linear_scale",
                "http://services.opendap.org/dap4/server-side-function/linear_scale",
                "Applies a linear scale transform to the named variable.",
                "http://docs.opendap.org/index.php/Server_Side_Processing_Functions#linear_scale");
        extensions.add(function);


        function = getFunctionElement("version",
                "http://services.opendap.org/dap4/server-side-function/version",
                "Returns version information for each server side function.",
                "http://docs.opendap.org/index.php/Server_Side_Processing_Functions#version");
        extensions.add(function);


        function = getExtensionElement("async",
                "http://services.opendap.org/dap4/extension/asynchronousTransactions",
                "This server supports asynchronous transactions..",
                "http://docs.opendap.org/index.php/DAP4:_Asynchronous_Request-Response_Proposal_v3");
        extensions.add(function);



        return extensions;

    }

    public Element getFunctionElement(String name, String role, String description, String descriptionLink ){
        Element function;
        function = new Element("function",DAP.DAPv40_DatasetServices_NS);
        function.setAttribute("name",name);
        function.setAttribute("role",role);
        function.addContent(getDescriptionElement(description,descriptionLink));
        return function;
    }

    public Element getExtensionElement(String name, String role, String description, String descriptionLink ){
        Element function;
        function = new Element("extension",DAP.DAPv40_DatasetServices_NS);
        function.setAttribute("name",name);
        function.setAttribute("role",role);
        function.addContent(getDescriptionElement(description,descriptionLink));
        return function;
    }





}
