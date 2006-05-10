/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.olfs;

import opendap.coreServlet.OpendapSoapDispatchHandler;
import opendap.coreServlet.MultipartResponse;
import opendap.coreServlet.DispatchServlet;
import opendap.soap.XMLNamespaces;
import opendap.soap.ExceptionElementUtil;
import opendap.util.Debug;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Document;


import thredds.cataloggen.SimpleCatalogBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: May 2, 2006
 * Time: 10:42:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class SoapDispatchHandler implements OpendapSoapDispatchHandler {

    /**
     *
     * @param ds
     * @throws ServletException
     */
    public void init(DispatchServlet ds) throws ServletException{

    }


    /**
     *
     * @param reqID
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getDATA( String reqID,  Element cmd, MultipartResponse mpr) throws Exception {

        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        System.out.println("Received GetDATA reqElement.");
        Element dataSet = cmd.getChild("DataSet", osnms);

        System.out.println("Dataset:\n"+dataSet.toString());


        String datasetname = dataSet.getChild("name",osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression",osnms).getTextTrim();

        System.out.println("Processing DataSet - path: "+datasetname+"   ce: "+ce);

        Element respElement = new Element("Response",osnms);
        respElement.setAttribute("reqID",reqID,osnms);


        Element ddx = BesAPI.getDDXDocument(datasetname, ce).detachRootElement();
        //@todo Fix The BES use of dodsBLOB!

        Element blob = ddx.getChild("dodsBLOB", XMLNamespaces.getOpendapDAP2Namespace());

        String contentId = MultipartResponse.getUidString();

        //@todo Add the namespace to the href - first we must add it to the schema!
        blob.setAttribute("href", "cid:" + contentId);



        respElement.addContent(ddx);


        mpr.addAttachment("application/octet-stream",
                contentId,
                BesAPI.getDap2DataStream(datasetname, ce));

        mpr.addSoapBodyPart(respElement);
    }


    /**
     *
     * @param reqID
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getDDX(String reqID, Element cmd, MultipartResponse mpr) throws Exception {
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();
        System.out.println("Received GetDDX reqElement.");


        Element dataSet = cmd.getChild("DataSet", osnms);

        System.out.println("Dataset:\n"+dataSet.toString());


        String datasetname = dataSet.getChild("name",osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression",osnms).getTextTrim();

        System.out.println("Processing DataSet - path: "+datasetname+"   ce: "+ce);

        Element respElement = new Element("Response",osnms);
        respElement.setAttribute("reqID",reqID,osnms);

        respElement.addContent(BesAPI.getDDXDocument(datasetname, ce).detachRootElement());

        mpr.addSoapBodyPart(respElement);

    }


    /**
     *
     * @param srvReq
     * @param reqID
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getTHREDDSCatalog(HttpServletRequest srvReq, String reqID, Element cmd, MultipartResponse mpr) throws Exception {

        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();
        Element respElement;



        System.out.println("Received GetTHREDDSCatalog reqElement.");

        String path = cmd.getChild("path",osnms).getTextTrim();

        path = BESCrawlableDataset.besPath2ThreddsPath(path);

        BESCrawlableDataset s4cd = new BESCrawlableDataset(path, null);

        String reqURI   = srvReq.getRequestURI();
        String pathInfo = srvReq.getPathInfo();

/*
        respElement =  ExceptionElementUtil.makeExceptionElement(
                "CatalogGenError",

                        "getRequestURI(): "+srvReq.getRequestURI()+"     " +
                        "getPathInfo(): "+srvReq.getPathInfo()+"\n",
                "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
        );
        mpr.addSoapBodyPart(respElement);
*/

        
        String baseURL;
        if(pathInfo != null)
            baseURL = reqURI.substring(0, reqURI.lastIndexOf(pathInfo) + 1);
        else
            baseURL = reqURI;


        if (s4cd.isCollection()) {

            SimpleCatalogBuilder scb = new SimpleCatalogBuilder(
                    "",                                   // CollectionID, which for us needs to be empty.
                    BESCrawlableDataset.getRootDataset(), // Root dataset of this collection
                    "OPeNDAP-Server4",                    // Service Name
                    "OPeNDAP",                            // Service Type Name
                    baseURL ); // Base URL for this service

            if (Debug.isSet("showResponse")) {
                System.out.println("SOAPRequestDispatcher:GetTHREDDSCatalog - Generating catalog");
            }


            Document catalog = scb.generateCatalogAsDocument(s4cd);

            if(catalog == null){
                System.out.println("SimpleCatalogBuilder.generateCatalogAsDocument("+path+") returned null.");
                respElement =  ExceptionElementUtil.makeExceptionElement(
                        "BadSOAPRequest",
                        "Requested catalog ("+cmd.getChild("path").getTextTrim()+" is not available.",
                        "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                );
            }
            else {
                respElement = new Element("Response",osnms);
                respElement.setAttribute("reqID",reqID,osnms);
                respElement.addContent(catalog.detachRootElement());
            }

        } else {

            String msg = "ERROR: THREDDS catalogs may only be requested for collections, " +
                    "not for individual data sets. The path: \""+cmd.getChild("path").getTextTrim()+
                    "\" does not resolve to a collection.";

            respElement =  ExceptionElementUtil.makeExceptionElement(
                    "BadSOAPRequest",
                    msg,
                    "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
            );

        }
        mpr.addSoapBodyPart(respElement);


    }
}
