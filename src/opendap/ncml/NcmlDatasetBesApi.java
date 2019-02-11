/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) $year OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.ncml;

import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.Scrub;
import opendap.namespaces.BES;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/11/11
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class NcmlDatasetBesApi extends BesApi implements Cloneable {

     Logger log;


    public NcmlDatasetBesApi(){
        log = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    /**
     * This child class of opendap.bes.BesApi provides an implementation of the
     * getRequestDocument method that utilizes the BES ncml dataset container API.
     * @param type The type of thing being requested. For example a DDX would be
     * opendap.bes.BesXmlAPI.DDX
     * @param localDataSourceId See opendap.bes.BesXmlAPI.DDX
     * @param ce See opendap.bes.BesXmlAPI
     * @param xdap_accept See opendap.bes.BesXmlAPI
     * @param xmlBase See opendap.bes.BesXmlAPI
     * @param formURL See opendap.bes.BesXmlAPI
     * @param returnAs See opendap.bes.BesXmlAPI
     * @param errorContext See opendap.bes.BesXmlAPI
     * @return The request Document
     * @throws opendap.bes.BadConfigurationException When the bad things happen.
     * @see opendap.bes.dap2Responders.BesApi
     */
    @Override
    public  Document getDap2RequestDocument(String type,
                                            String localDataSourceId,
                                            String ce,
                                            String xdap_accept,
                                            int maxResponseSize,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {


        log.debug("Building  BES  request. localDataSourceId: "+ localDataSourceId);
        Element e, request = new Element("request", BES.BES_NS);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":"+log.getName()+"]";
        request.setAttribute("reqID",reqID);


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(maxResponseSize>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,maxResponseSize+""));

        Element ncmlDatasetContainer =  NcmlManager.getNcmlDatasetContainer(localDataSourceId);

        if(ncmlDatasetContainer!=null)
            request.addContent(ncmlDatasetContainer);
        else
            log.error("Failed to locate ncml dataset: {}", Scrub.urlContent(localDataSourceId));


        Element def = defineElement("d1","default");
        e = containerElement(localDataSourceId);

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        log.debug("Built BES request.");


        return new Document(request);

    }


     /*
    <showInfo>
        <dataset catalog="catalog" lastModified="2008-05-29T23:31:56" name="NWAtlanticDec_1km.nc" node="false" size="852">
            <serviceRef>dap</serviceRef>
        </dataset>
    </showInfo>
    */

    /*
    @Override
    public void getInfo(String dataSource, Document response) throws
            PPTException,
                        BadConfigurationException,
                        IOException,
                        JDOMException {



        if(NcmlManager.isNcmlDataset(dataSource)){
            Element responseElement = new Element("response",BES.BES_NS);
            Element showInfo = new Element("showInfo",BES.BES_NS);
            Element dataset = new Element("dataset",BES.BES_NS);
            Element srvcRef = new Element("serviceRef",BES.BES_NS);

            showInfo.addContent(dataset);
            dataset.addContent(srvcRef);

            dataset.setAttribute("name",dataSource);
            dataset.setAttribute("catalog","catalog");
            dataset.setAttribute("lastModified","2008-05-29T23:31:56");
            dataset.setAttribute("node","false");
            dataset.setAttribute("size","-1");
            dataset.setAttribute("prefix", getBESprefix(dataSource));

            srvcRef.setText("dap");

            responseElement.addContent(showInfo);
            responseElement.setAttribute("reqID",(new Date())+dataSource);

            responseElement.addContent(showInfo);
            response.setRootElement(responseElement);


        }
        else {

            Element responseElement = new Element("response", BES.BES_NS);
            Element besElement = new Element("BES", BES.BES_NS);
            Element besErrorElement = new Element("BESError", BES.BES_NS);
            Element typeElement = new Element("type", BES.BES_NS);
            Element messageElement = new Element("Message", BES.BES_NS);
            Element adminElement = new Element("Administrator", BES.BES_NS);

            messageElement.setText("Unable to access node" + dataSource + ": No such file or directory");
            typeElement.setText("5");
            adminElement.setText("support@opendap.org");

            besErrorElement.addContent(typeElement);
            besErrorElement.addContent(messageElement);
            besErrorElement.addContent(adminElement);

            besElement.addContent(besErrorElement);

            responseElement.addContent(besElement);

            responseElement.setAttribute("reqID", (new Date()) + dataSource);


            response.setRootElement(responseElement);
        }


    }


                          */


}
