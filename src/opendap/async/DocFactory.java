/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
package opendap.async;

import opendap.namespaces.DAP;
import opendap.namespaces.XML;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 8/14/12
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class DocFactory {

    static Logger log = LoggerFactory.getLogger(DocFactory.class);

    public enum asyncStatus {
        REQUIRED("required"),
        ACCEPTED("accepted"),
        PENDING("pending"),
        GONE("gone"),
        REJECTED("rejected")
        ;

        private asyncStatus(final String text) {
            this.text = text;
        }

        private final String text;

        @Override
        public String toString() {
            return text;
        }
    }


    public enum reasonCode {
        TIME("time");

        private reasonCode(final String text) {
            this.text = text;
        }

        private final String text;

        @Override
        public String toString() {
            return text;
        }
    }






    private static Element getExpectedDelayElement(long expectedDelay){
        Element expectedDelayElement    = new Element("expectedDelay",DAP.DAPv40_NS);
        expectedDelayElement.setText(expectedDelay+"");
        return  expectedDelayElement;
    }

    private static Element getResponseLifeTimeElement(long responseLifeTime){
        Element responseLifeTimeElement = new Element("responseLifeTime",DAP.DAPv40_NS);
        responseLifeTimeElement.setText(responseLifeTime+"");
        return  responseLifeTimeElement;
    }

    private static Element getLinkElement(String url){
        Element linkElement = new Element("link",DAP.DAPv40_NS);
        linkElement.setAttribute("href", url);
        return  linkElement;
    }

    private static Element getReasonElement(reasonCode code){
        Element reasonElement = new Element("reason",DAP.DAPv40_NS);
        reasonElement.setAttribute("code", code.toString());
        return  reasonElement;
    }


    private static Element getDescriptionElement(String description){
        Element descriptionElement = new Element("description",DAP.DAPv40_NS);
        descriptionElement.setText(description);
        return  descriptionElement;
    }


    private static Element getAsynchronousResponseElement(String xmlBase, asyncStatus status){
        Element asyncResponseElement    = new Element("AsynchronousResponse",DAP.DAPv40_NS);
        asyncResponseElement.setNamespace(DAP.DAPv40_NS);
        asyncResponseElement.setAttribute("base", xmlBase, XML.NS);

        asyncResponseElement.setAttribute("status", status.toString());

        return  asyncResponseElement;
    }



    private static Document getAsynchronousResponseDocument(String xmlBase, String context, asyncStatus status){

        Element asyncResponseElement = getAsynchronousResponseElement(xmlBase, status);

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", context+"xsl/asyncResponse.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        Document asyncResponse = new Document() ;
        asyncResponse.addContent(pi);

        asyncResponse.setRootElement(asyncResponseElement);

        return  asyncResponse;
    }





    public static Document getAsynchronousResponseRequired(HttpServletRequest req, long expectedDelay, long responseLifeTime){
        return getAsynchronousResponseRequired(getXmlBase(req),getContext(req),expectedDelay,responseLifeTime);
    }

    public static Document getAsynchronousResponseRequired(String xmlBase, String context, long expectedDelay, long responseLifeTime){

        Document asyncResponse = getAsynchronousResponseDocument(xmlBase,context,asyncStatus.REQUIRED);
        Element asyncResponseElement = asyncResponse.getRootElement();
        asyncResponseElement.addContent(getExpectedDelayElement(expectedDelay));
        asyncResponseElement.addContent(getResponseLifeTimeElement(responseLifeTime));

        return asyncResponse;
    }



    public static Document getAsynchronousResponseAccepted(HttpServletRequest req, String resultLinkUrl, long expectedDelay, long responseLifeTime){




        return getAsynchronousResponseAccepted(getXmlBase(req), getContext(req),  resultLinkUrl, expectedDelay,responseLifeTime);
    }

    public static Document getAsynchronousResponseAccepted(String xmlBase, String context, String resultLinkUrl, long expectedDelay, long responseLifeTime){

        Document asyncResponse = getAsynchronousResponseDocument(xmlBase,context,asyncStatus.ACCEPTED);
        Element asyncResponseElement = asyncResponse.getRootElement();
        asyncResponseElement.addContent(getExpectedDelayElement(expectedDelay));
        asyncResponseElement.addContent(getResponseLifeTimeElement(responseLifeTime));
        asyncResponseElement.addContent(getLinkElement(resultLinkUrl));

        return asyncResponse;

    }





    public static Document getAsynchronousResponsePending(HttpServletRequest req){
        return getAsynchronousResponsePending(getXmlBase(req),getContext(req));
    }

    public static Document getAsynchronousResponsePending(String xmlBase, String context ){

        return getAsynchronousResponseDocument(xmlBase,context,asyncStatus.PENDING);

    }



    public static Document getAsynchronousResponseGone(HttpServletRequest req){
        return getAsynchronousResponseGone(getXmlBase(req),getContext(req));
    }

    public static Document getAsynchronousResponseGone(String xmlBase, String context ){

        return getAsynchronousResponseDocument(xmlBase,context,asyncStatus.GONE);

    }





    public static Document getAsynchronousResponseRejected(HttpServletRequest req, reasonCode reason, String description){
        return getAsynchronousResponseRejected(getXmlBase(req),getContext(req), reason,description);
    }

    public static Document getAsynchronousResponseRejected(String xmlBase, String context, reasonCode code, String description){

        Document asyncResponse = getAsynchronousResponseDocument(xmlBase,context,asyncStatus.REJECTED);
        Element asyncResponseElement = asyncResponse.getRootElement();
        asyncResponseElement.addContent(getReasonElement(code));
        asyncResponseElement.addContent(getDescriptionElement(description));

        return asyncResponse;


    }

    private static String  getContext(HttpServletRequest req){

        return req.getContextPath()+"/";

    }

    public static String  getXmlBase(HttpServletRequest req){

        String forwardRequestUri = (String)req.getAttribute("javax.servlet.forward.request_uri");
        String requestUrl = req.getRequestURL().toString();


        if(forwardRequestUri != null){
            String server = req.getServerName();
            int port = req.getServerPort();
            String scheme = req.getScheme();
            requestUrl = scheme + "://" + server + ":" + port + forwardRequestUri;
        }


        String xmlBase = removeRequestSuffixFromString(requestUrl);


        log.debug("@xml:base='{}'",xmlBase);
        return xmlBase;
    }

    public static String removeRequestSuffixFromString(String requestString){
        String trimmedRequestString;

        trimmedRequestString = requestString.substring(0,requestString.lastIndexOf("."));

        return trimmedRequestString;
    }



}
