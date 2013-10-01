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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;




/**
 * Holds The type information and a referene to an input stream for the content of a Mutipart
 * MIME attachment.
 *
 * @see opendap.coreServlet.MultipartResponse
 *      User: ndp
 *      Date: Apr 28, 2006
 *      Time: 12:13:19 PM
 */
public class Attachment {

    private enum ContentModel {
        stream, url, document
    }


    private Logger log;
    private String contentTransferEncoding = "binary";
    private String contentId;
    private String contentType;
    private InputStream _istream;
    private String _sourceUrl;
    private Document _doc;
    private int defaultBufferSize = 10240; // 10k read buffer
    private ContentModel _myContentModel;





    /**
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid   String containing the value if the HTTP header Content-Id for this attachment.
     * @param is    A stream containing the content for this attachment.
     */
    public Attachment(String ctype, String cid, InputStream is) {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        contentType = ctype;
        contentId = cid;
        _istream = is;
        _doc = null;
        _myContentModel = ContentModel.stream;

    }


    /**
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid   String containing the value if the HTTP header Content-Id for this attachment.
     * @param url   A URL that when dereferenced will provide the content for this attachment.
     */
    public Attachment(String ctype, String cid, String url) {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        contentType = ctype;
        contentId = cid;
        _istream = null;
        _sourceUrl = url;
        _doc = null;
        _myContentModel = ContentModel.url;
    }

    /**
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid String containing the value if the HTTP header Content-Id for this attachment.
     * @param doc A JDOM XML document to provide the content for this attachment.
     */
    public Attachment(String ctype, String cid, Document doc) {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        contentType = ctype;
        contentId = cid;
        _istream = null;
        _sourceUrl = null;
        _doc = doc;
        _myContentModel = ContentModel.document;
    }


    public String getContentType(){
        return contentType;
    }

    public void setContentType(String cntTyp){
        contentType=cntTyp;
    }


    /**
     * Write the attchment to the indicated stream
     *
     * @param mimeBoundary MIME Boundary for the attachment.
     * @param sos          Stream to which to write the attachment.
     * @throws IOException                 When things can't be read or written.
     * @throws java.net.URISyntaxException If the target URL is hosed.
     */
    public void write(String mimeBoundary, ServletOutputStream sos) throws IOException, URISyntaxException {


            sos.println("--" + mimeBoundary);
            sos.println("Content-Type: " + contentType);
            sos.println("Content-Transfer-Encoding: " + contentTransferEncoding);
            sos.println("Content-Id: <" + contentId + ">");
            sos.println();


            switch (_myContentModel) {
                case stream:
                    try {
                        drainInputStream(_istream, sos);
                    } finally {
                        if (_istream != null) {
                            try {
                                _istream.close();
                            } catch (IOException e) {
                                log.error("Failed to close content source InputStream. " +
                                        "Error Message: " + e.getMessage());

                            }
                        }
                    }
                    break;

                case url:
                    forwardUrlContent(_sourceUrl, sos);
                    break;

                case document:
                    sendDocument(sos);
                    break;

                default:
                    break;

            }

            //MIME Attachments need to end with a newline!
            sos.println();


    }


    private void sendDocument(OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(_doc, os);
    }

    private void forwardUrlContent(String url, OutputStream os) throws URISyntaxException, IOException {

        log.debug("Retrieving URL: "+url);

        GetMethod request = new GetMethod(url);
        InputStream is = null;
        try {

            HttpClient httpClient = new HttpClient();

            // Execute the method.
            int statusCode = httpClient.executeMethod(request);

            if (statusCode != HttpStatus.SC_OK) {
                String msg = "HttpClient failed to executeMethod(). Status: " + request.getStatusLine();
                log.error(msg);
                throw new IOException(msg);
            }
            else {
                is = request.getResponseBodyAsStream();
                drainInputStream(is,os);

            }

        }
        finally {
            if(is!=null)
                is.close();
            log.debug("Releasing Http connection.");
            request.releaseConnection();
        }

    }




    private int drainInputStream(InputStream is, OutputStream os) throws IOException {

        byte[] buf = new byte[defaultBufferSize];

        boolean done = false;
        int totalBytesRead = 0;
        int totalBytesWritten = 0;
        int bytesRead;

        while (!done) {
            bytesRead = is.read(buf);
            if (bytesRead == -1) {
                if (totalBytesRead == 0)
                    totalBytesRead = -1;
                done = true;
            } else {
                totalBytesRead += bytesRead;
                os.write(buf, 0, bytesRead);
                totalBytesWritten += bytesRead;
            }
        }

        if (totalBytesRead != totalBytesWritten)
            throw new IOException("Failed to write as many bytes as I read! " +
                    "Read: " + totalBytesRead + " Wrote: " + totalBytesWritten);

        return totalBytesRead;

    }


}

