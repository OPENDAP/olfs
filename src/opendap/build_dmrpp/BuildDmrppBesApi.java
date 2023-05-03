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

package opendap.build_dmrpp;

import opendap.auth.EarthDataLoginAccessToken;
import opendap.auth.UserProfile;
import opendap.bes.BesApi;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.logging.ServletLogUtil;
import opendap.namespaces.BES;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: dan
 * Date: 2/13/20
 * Time: 1:47 PM
 * Cloned from: opendap.gateway
 * To change this template use File | Settings | File Templates.
 */
public class BuildDmrppBesApi implements Cloneable {

    public static final String EDL_AUTH_TOKEN_CONTEXT = "edl_auth_token";
    public static final String EDL_ECHO_TOKEN_CONTEXT = "edl_echo_token";
    public static final String INVOCATION_CONTEXT = "invocation";

    private static final String SPACE_NAME = "builddmrpp";
    private static final String CONTAINER_NAME = "builddmrppContainer";


    public static final String DMRPP = "dmrpp";
    private Logger log;
    private String _servicePrefix;

    public BuildDmrppBesApi() {
        this("");
    }

    public BuildDmrppBesApi(String servicePrefix) {
        super();
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        _servicePrefix = servicePrefix;
    }


    public static Element setContextElement(String name, String value) {
        Element e = new Element("setContext",BES.BES_NS);
        e.setAttribute("name",name);
        e.setText(value);
        return e;
    }

    /**
     * Adds the user id and/or the associated EDL auth token to the request
     * element. If either parameter is the empty string it is omitted.
     *
     * Constructs the EDL/URS Echo-Token and Authorization headers for use
     * when connecting to NGAP infrstructure (like cumulus and CMR) The
     * Echo-Token is made from the
     * EDL access_token returned for the user and the server's EDL Application
     * Client-Id.
     *
     *    Echo-Token: µedl_access_token:Client-Id
     *
     * The Authorization header is made of the sting:
     *
     *    Authorization: Bearer edl_access_token
     *
     * From a bes command:
     *   <bes:setContext name="uid">ndp_opendap</bes:setContext>
     *   <bes:setContext name="edl_echo_token">anecho:tokenvalue</bes:setContext>
     *    <bes:setContext name="edl_auth_token">Bearer Abearertokenvalue</bes:setContext>
     *
     * @param request The BES request in which to set the UID_CONTEXT and
     *                EDL_AUTH_TOKEN_CONTEXT from the user object.
     * @param user The instance of User from which to get the uid, the
     *             auth_token, and the EDL Application Client-Id..
     */
    public static void addEdlAuthToken(Element request, User user) {
        UserProfile up = user.profile();
        if (up != null) {
            request.addContent(setContextElement(BesApi.UID_CONTEXT,user.getUID()==null?"not_logged_in":user.getUID()));

            EarthDataLoginAccessToken oat = up.getEDLAccessToken();
            if (oat != null) {

                // Make and add the @deprecated Echo-Token value
                request.addContent(setContextElement(EDL_ECHO_TOKEN_CONTEXT, oat.getEchoTokenValue()));

                // Add the new service chaining Authorization header value
                request.addContent(setContextElement(EDL_AUTH_TOKEN_CONTEXT, oat.getAuthorizationHeaderValue()));
            }
        }

    }


    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    public Document getBuildDmrppDocument(User user, String dataSource, QueryParameters qp, String invocation, int timeout_seconds) {

        log.debug("Constructing BES build dmr++ request. dataSource: {}",dataSource);
        Element request = new Element("request", BES.BES_NS);

        //String besDataSource = getBES(dataSource).trimPrefix(dataSource);

        String reqID = Thread.currentThread().getName()+":"+ Thread.currentThread().getId();

        request.setAttribute("reqID",reqID);

        request.addContent(setContextElement(BesApi.EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(BesApi.ERRORS_CONTEXT, BesApi.XML_ERRORS));

        request.addContent(setContextElement("bes_timeout", Integer.toString(timeout_seconds)));

        String logEntryForBes = ServletLogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(BesApi.OLFS_LOG_CONTEXT,logEntryForBes));

        if(invocation!=null)
            request.addContent(setContextElement(INVOCATION_CONTEXT,invocation));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(BesApi.MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        addEdlAuthToken(request,user);

        // request.addContent(setContainerElement(CONTAINER_NAME, SPACE_NAME,dataSource,BesApi.DAP4_DATA));
        Element setContainerElem = new Element("setContainer",BES.BES_NS);
        setContainerElem.setAttribute("name",CONTAINER_NAME);
        setContainerElem.setAttribute("space",SPACE_NAME);
        setContainerElem.setText(dataSource);
        request.addContent(setContainerElem);

//         Element def = defineElement("d1","default");
        Element defineElem = new Element("define",BES.BES_NS);
        defineElem.setAttribute("name","d1");
        defineElem.setAttribute("space","default");

        Element containerElem = new Element("container",BES.BES_NS);
        containerElem.setAttribute("name",CONTAINER_NAME);

        if(qp.getCe()!=null && !qp.getCe().equals("")) {
            Element ceElem = new Element("dap4constraint",BES.BES_NS);
            // We replace the space characters in the CE with %20
            // so the libdap ce parsers don't blow a gasket.
            String encoded_ce = qp.getCe().replaceAll(" ","%20");
            ceElem.setText(encoded_ce);
            containerElem.addContent(ceElem);
        }

        if(qp.getFunc()!=null && !qp.getFunc().equals("")) {
            // e.addContent(dap4FunctionElement(qp.getFunc()));
            Element d4FuncElem = new Element("dap4function",BES.BES_NS);
            d4FuncElem.setText(qp.getFunc());
            containerElem.addContent(d4FuncElem);
        }
        defineElem.addContent(containerElem);

        request.addContent(defineElem);

        // Build and add the <get /> element
        Element getElement = new Element("get",BES.BES_NS);
        getElement.setAttribute("type",BesApi.DAP4_DATA);
        getElement.setAttribute("definition","d1");
        getElement.setAttribute("returnAs",DMRPP);

        if(qp.getAsync()!=null && !qp.getAsync().isEmpty())
            getElement.setAttribute("async",qp.getAsync());

        if(qp.getStoreResultRequestServiceUrl()!=null && !qp.getStoreResultRequestServiceUrl().isEmpty())
            getElement.setAttribute("store_result",qp.getStoreResultRequestServiceUrl());

        request.addContent(getElement);

        log.debug("Built request for BES build_dmrpp_module.");

        return new Document(request);
    }

/*
    String stripPrefix(String dataSource){

        while(dataSource.startsWith("/") && !dataSource.equals("/"))
            dataSource = dataSource.substring(1);

        if(dataSource.startsWith(_servicePrefix))
            return dataSource.substring(_servicePrefix.length());

        return dataSource;
    }
*/


}
