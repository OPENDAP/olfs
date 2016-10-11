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

package opendap.wcs.v2_0.http;

import opendap.dap.Request;
import opendap.wcs.v2_0.WcsException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 21, 2010
 * Time: 3:43:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(Util.class);
    }


    /***************************************************************************/




    public static String getServiceUrlString(HttpServletRequest request, String prefix){
        String serviceURL = getServiceUrl(request);

        if (!prefix.equals("")) {
            if (!serviceURL.endsWith("/")) {
                if (prefix.startsWith("/"))
                    serviceURL += prefix;
                else
                    serviceURL += "/" + prefix;

            } else {
                if (prefix.startsWith("/"))
                    serviceURL += serviceURL.substring(0, serviceURL.length() - 1) + prefix;
                else
                    serviceURL += prefix;

            }
        }
        return serviceURL;

    }

    public static String getServiceUrl(HttpServletRequest request){
        return new Request(null,request).getServiceUrl();
    }






    public static final int DEFAULT_BUFFER_SIZE = 10240; // 10k read buffer

    public static int drainInputStream(InputStream is, OutputStream os) throws IOException {
        return drainInputStream(is,os,DEFAULT_BUFFER_SIZE);
    }

    public static int drainInputStream(InputStream is, OutputStream os, int bufferSize) throws IOException {

        byte[] buf = new byte[bufferSize];

        boolean done = false;
        int totalBytesRead = 0;
        int totalBytesWritten = 0;
        int bytesRead;

        //ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while (!done) {
            bytesRead = is.read(buf);
            if (bytesRead == -1) {
             //   if (totalBytesRead == 0)
              //      totalBytesRead = -1;
                done = true;
            } else {
                totalBytesRead += bytesRead;
                os.write(buf, 0, bytesRead);
                // baos.write(buf,0,bytesRead);
                totalBytesWritten += bytesRead;
            }
        }

        if (totalBytesRead != totalBytesWritten)
            throw new IOException("Failed to write as many bytes as I read! " +
                    "Read: " + totalBytesRead + " Wrote: " + totalBytesWritten);


        //System.out.println("################################################################");
        //System.out.write(baos.toByteArray());
        //System.out.println("################################################################");


        return totalBytesRead;

    }


    public static void forwardUrlContent(
            String url,
            HttpServletResponse response,
            boolean transferHttpHeaders)
            throws URISyntaxException, IOException, WcsException {
        forwardUrlContent(url,response, DEFAULT_BUFFER_SIZE, transferHttpHeaders);
    }



    public static void forwardUrlContent(
            String url,
            HttpServletResponse response,
            int bufferSize,
            boolean transferHttpHeaders)
            throws URISyntaxException, IOException, WcsException {

        log.debug("Retrieving URL: "+url);

        GetMethod contentRequest = new GetMethod(url);
        InputStream is = null;
        try {

            HttpClient httpClient = new HttpClient();

            // Execute the method.
            int statusCode = httpClient.executeMethod(contentRequest);

            if (statusCode != HttpStatus.SC_OK) {


                String msg = "Unable to read data from primary data source. Data URL: '"+url+
                        "' Data server returned the HTTP status line: '"+contentRequest.getStatusLine()+"'";

                log.error(msg);


                throw new WcsException(msg, WcsException.NO_APPLICABLE_CODE, "DataAccessUrl");

            }
            else {


                if(transferHttpHeaders){
                    Header[] headers = contentRequest.getResponseHeaders();
                    String name, value;


                    for(Header h:headers){
                        name = h.getName();
                        value = h.getValue();

                        // DO NOT Transfer the Transfer-Encoding header cause if you do you'll bone what ever Tomcat
                        // is doing.
                        if(!name.equalsIgnoreCase("Transfer-Encoding"))
                            response.setHeader(name,value);
                    }
                }

                ServletOutputStream os = response.getOutputStream();

                is = contentRequest.getResponseBodyAsStream();
                drainInputStream(is, os, bufferSize);

            }

        }
        finally {
            if(is!=null)
                is.close();
            log.debug("Releasing Http connection.");
            contentRequest.releaseConnection();
        }

    }


    public static void forwardUrlContent(String url, ServletOutputStream os) throws URISyntaxException, IOException {
        forwardUrlContent(url,os, DEFAULT_BUFFER_SIZE);
    }
    public static void forwardUrlContent(String url, ServletOutputStream os, int bufferSize) throws URISyntaxException, IOException {

        log.debug("Retrieving URL: "+url);

        GetMethod request = new GetMethod(url);
        InputStream is = null;
        try {

            HttpClient httpClient = new HttpClient();

            // Execute the method.
            int statusCode = httpClient.executeMethod(request);

            if (statusCode != HttpStatus.SC_OK) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is = request.getResponseBodyAsStream();
                drainInputStream(is, baos, bufferSize);


                String msg = "HttpClient failed to executeMethod(). Status: " + request.getStatusLine();
                msg += " Response Body: " + baos.toString();

                log.error(msg);
                throw new IOException(msg);
            }
            else {
                is = request.getResponseBodyAsStream();
                drainInputStream(is, os, bufferSize);

            }

        }
        finally {
            if(is!=null)
                is.close();
            log.debug("Releasing Http connection.");
            request.releaseConnection();
        }

    }



    public static  void sendDocument(Document doc,OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(doc, os);
    }



}
