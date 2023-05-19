/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0.http;

import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.coreServlet.ServletResponseTransmitCoordinator;
import opendap.io.HyraxStringEncoding;
import opendap.ppt.PPTException;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        int v;
        UID uid = new UID();
        StringBuilder suid = new StringBuilder();
        byte[] val = uid.toString().getBytes(HyraxStringEncoding.getCharset());
        for (byte aVal : val) {
            v = aVal;
            suid.append(Integer.toHexString(v));
        }
        return suid.toString();
    }


    /**
     * Send the Multipart MIME document response to the client.
     *
     * @throws java.io.IOException When things go wrong
     */
    public void send(HttpServletResponse servResponse) throws IOException, URISyntaxException, PPTException, BadConfigurationException, BESError {
        log.debug("Sending Response...");

        log.debug("MIME Boundary: " + mimeBoundary);

        Attachment firstPart = attachments.firstElement();


        servResponse.setContentType("multipart/related;  " +
                "type=\""+ firstPart.getContentType() +"\";  " +
                "start=\"" + startID + "\";  " +
                "boundary=\"" + mimeBoundary + "\"");

        servResponse.setHeader("Content-Description", "WCS 2.0 Response");

        ServletOutputStream os = servResponse.getOutputStream();
        ServletResponseTransmitCoordinator tc = new ServletResponseTransmitCoordinator(servResponse);
        for (Attachment a : attachments)
            a.write(mimeBoundary, os, tc);


        closeMimeDoc(os);


    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    private void closeMimeDoc(ServletOutputStream sos) throws IOException {
        sos.println("--" + mimeBoundary + "--");
    }


}
