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

package opendap.coreServlet;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.rmi.server.UID;
import java.io.*;
import java.util.Vector;

import opendap.soap.XMLNamespaces;

/**
 * This is used to hold the various parts of a MutlipartMIME response used by the OPeNDAP
 * servlet when replying to a SOAP request. The first part will contain the SOAP envelope
 * and the other parts (if oresent) may hold serialized binary data and/or other documents
 * requested by the client.
 *
 * User: ndp
 * Date: Apr 27, 2006
 * Time: 9:19:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultipartResponse {


    private Element soapEnvelope;
    private HttpServletResponse servResponse;
    private HttpServletRequest servRequest;
    private Vector<Attachment> attachments;
    private String mimeBoundary;
    private String startID;
    private Logger log;
    private OpendapMimeHeaders omhi;


    /**
     *
     *
     * @param request The HttpServletRequest to which we are replying
     * @param response The HttpServletResponse that we are going to send back.
     * @param omhInstance An instance of a class that implements the OpendapMimeHeaders interface.
     */
    MultipartResponse(HttpServletRequest request, HttpServletResponse response, OpendapMimeHeaders omhInstance){
        servResponse = response;
        servRequest = request;
        omhi = omhInstance;
        attachments  = new Vector<Attachment>();
        mimeBoundary = getNewMimeBoundary();
        startID = newUidString();
        soapEnvelope = null;
        log = org.slf4j.LoggerFactory.getLogger(getClass());

    }

    /**
     * Adds the passed <cod>Element</code> to the SOAP body.
     * @param e The Element to add to the SOAP body.
     */
    public void addSoapBodyPart(Element e){
        soapEnvelope.getChild("Body",XMLNamespaces.getDefaultSoapEnvNamespace()).addContent(e);
    }

    /**
     * Sets the SOAP Envelope to the passed Element. No Effort is made to ensure that the passed Element
     * actually represents a valid SOAP Envelope (which should at minimum contain a SOAP Body Element and
     * be in the correct namespace). The SOAP Envelope passed WILL BE CLONED and the clone kept by the instance
     * of <code>MultipartResponse</code>, thus subsequent modifications to the passed SOAP Envelope Element will NOT
     * be reflected in the SOAP Enveope help by this class instance.
     *
     * <P><b>This method may be called only once for a particular instance of <code>MultipartResponse</code>.
     * Subsequent calls will cause an exception to be thrown.</b></p>
     *
     *
     * @param se The SOAP element to clone for use by this instance.
     * @throws BadUsageException Thrown is this method is called more than once on an instance of this class.
     */
    public void setSoapEnvelope(Element se) throws BadUsageException {
        if(soapEnvelope == null)
            soapEnvelope = (Element) se.clone();
        else
            throw new BadUsageException("This method may only be called once for each instance of MultipartResponse.");
    }


    /**
     *
     * @return The MIME Boundary that will be used by this MultipartResponse.
     */
    public String getMimeBoundary(){
        //Date date = new Date();
        return mimeBoundary;
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public static String getNewMimeBoundary(){
        //Date date = new Date();
        return "----=_Part_0_"+newUidString();
    }

    /**
     *
     * @return Returns a new UID String
     */
    public static String newUidString(){
        UID uid = new UID();

        byte[] val = uid.toString().getBytes();

        String suid  = "";
        int v;

        for (byte aVal : val) {
            v = aVal;
            suid += Integer.toHexString(v);
        }

        return suid;
    }


    /**
     * Send the Multipart MIME docuemtn response to the client.
     * @throws IOException When things go wrong
     */
    public void send() throws Exception {
        log.debug("Sending Response...");

        log.debug("MIME Boundary: "+mimeBoundary);



        servResponse.setContentType("Multipart/related;  "+
                                "type=\"text/xml\";  "+
                                "start=\""+startID+"\";  "+
                                "boundary=\""+mimeBoundary+"\"");

        omhi.setOpendapMimeHeaders(servRequest,servResponse);
        servResponse.setHeader("Content-Description", "OPeNDAP WebServices");

        ServletOutputStream os = servResponse.getOutputStream();

        writeSoapPart(os);

        for (Attachment a : attachments)
            a.write(mimeBoundary,os);


        closeMimeDoc(os);


    }

    private void writeSoapPart(ServletOutputStream sos) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        sos.println("--"+mimeBoundary);
        sos.println("Content-Type: text/xml; charset=UTF-8");
        sos.println("Content-Transfer-Encoding: binary");
        sos.println("Content-Id: "+startID);
        sos.println();

        xmlo.output(new Document(soapEnvelope),sos);



    }


    public void addAttachment(String contentType, String contentId, InputStream is){

            attachments.add(new Attachment(contentType,contentId,is));
    }




    private void closeMimeDoc(ServletOutputStream sos) throws IOException {
        sos.println("--"+mimeBoundary+"--");
    }





}
