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
package opendap.wcs.v1_1_2.http;

import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.rmi.server.UID;
import java.util.Vector;

/**
 * This is used to hold the various parts of a MutlipartMIME response used by the OPeNDAP
 * servlet when replying to a SOAP request. The first part will contain the SOAP envelope
 * and the other parts (if oresent) may hold serialized binary data and/or other documents
 * requested by the client.
 * <p/>
 * User: ndp
 * Date: Apr 27, 2006
 * Time: 9:19:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultipartResponse {

    private Vector<Attachment> attachments;
    private String mimeBoundary;
    private String startID;
    private Logger log;


    /**
     */
    public MultipartResponse() {
        attachments = new Vector<Attachment>();
        mimeBoundary = getNewMimeBoundary();
        startID = newUidString();
        log = org.slf4j.LoggerFactory.getLogger(getClass());

    }



    public String getStartID(){
        return startID;
    }

    public void setStartID(String id){
        startID = id;
    }

    /**
     * @return The MIME Boundary that will be used by this MultipartResponse.
     */
    public String getMimeBoundary() {
        //Date date = new Date();
        return mimeBoundary;
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     *
     * @return Returns a NEW MIME Boundary string.
     */
    public static String getNewMimeBoundary() {
        //Date date = new Date();
        return "----=_Part_" + newUidString();
    }

    /**
     * @return Returns a new UID String
     */
    public static String newUidString() {
        UID uid = new UID();

        byte[] val = uid.toString().getBytes();

        String suid = "";
        int v;

        for (byte aVal : val) {
            v = aVal;
            suid += Integer.toHexString(v);
        }

        return suid;
    }


    /**
     * Send the Multipart MIME docuemtn response to the client.
     *
     * @throws java.io.IOException When things go wrong
     */
    public void send(HttpServletResponse servResponse) throws IOException, URISyntaxException {
        log.debug("Sending Response...");

        log.debug("MIME Boundary: " + mimeBoundary);

        Attachment firstPart = attachments.firstElement();


        servResponse.setContentType("Multipart/related;  " +
                "type=\""+ firstPart.getContentType() +"\";  " +
                "start=\"" + startID + "\";  " +
                "boundary=\"" + mimeBoundary + "\"");

        servResponse.setHeader("Content-Description", "OPeNDAP WebServices");

        ServletOutputStream os = servResponse.getOutputStream();

        for (Attachment a : attachments)
            a.write(mimeBoundary, os);


        closeMimeDoc(os);


    }

    private void writeSoapPart(ServletOutputStream sos) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        sos.println("--" + mimeBoundary);
        sos.println("Content-Type: text/xml; charset=UTF-8");
        sos.println("Content-Transfer-Encoding: binary");
        sos.println("Content-Id: " + startID);
        sos.println();

        //xmlo.output(new Document(soapEnvelope), sos);


    }


    public void addAttachment(String contentType, String contentId, InputStream is) {

        attachments.add(new Attachment(contentType, contentId, is));
    }

    public void addAttachment(String contentType, String contentId, String url) {

        attachments.add(new Attachment(contentType, contentId, url));
    }


    public void addAttachment(String contentType, String contentId, Document doc) {

        attachments.add(new Attachment(contentType, contentId, doc));
    }


    private void closeMimeDoc(ServletOutputStream sos) throws IOException {
        sos.println("--" + mimeBoundary + "--");
    }


}
