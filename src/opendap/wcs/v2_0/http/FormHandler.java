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

import opendap.wcs.v2_0.WCS;
import opendap.wcs.v2_0.WcsException;
import org.jdom.Element;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLDecoder;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 9:00:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class FormHandler extends XmlRequestHandler {


    public FormHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }

    public void init(HttpServlet servlet, Element config) throws Exception {
        super.init(servlet,config);
    }



    @Override
    public BufferedReader getRequestReader(HttpServletRequest request) throws WcsException {
        BufferedReader sis;
        try {
            sis = request.getReader();
        } catch (IOException e) {
            throw new WcsException("Failed to retrieve WCS Request document input stream. Message: " + e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }
        int length;
        String formTag = "WCS_QUERY=";
        String sb = "";
        StringBuilder reqDocBuilder = new StringBuilder();

        //  Slurp up the document.
        try {
            while(sb!= null){
                length =  sb.length() + reqDocBuilder.length();
                if( length > WCS.MAX_REQUEST_LENGTH){
                    throw new WcsException("Form Content Body too long. Try again with something smaller.",
                            WcsException.INVALID_PARAMETER_VALUE,
                            "WCS Request Document");

                }
                reqDocBuilder.append(sb);
                sb = sis.readLine();
            }
        } catch (IOException e) {
            throw new WcsException("Failed to read request document. Message:"+e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }

        log.debug("Form Interface received: " +reqDocBuilder.toString());

        String reqDoc = reqDocBuilder.toString();
        String encoding = getEncoding(request);
        // If you got a document,
        if(reqDoc.length()>0){
            try {
                // Decode it, because browsers seem to want to URL encode their post content. Whatever...
                reqDoc = URLDecoder.decode(reqDoc,encoding);
            } catch (UnsupportedEncodingException e) {
                throw new WcsException("Failed to Decode request document. Message:"+e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "WCS Request Document");
            }

            // Strip off the form  name
            if(reqDoc.startsWith(formTag)) {
                reqDoc = reqDoc.substring(formTag.length());
            }
            log.debug("XML to Parse: " +reqDoc);
            try {
                InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(reqDoc.getBytes(encoding)),encoding);
                return new BufferedReader(isr);
            }
            catch (UnsupportedEncodingException e) {
                throw new WcsException("Request was submitted in an unsupported " +
                        "character encoding: "+encoding+ " msg: "+e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE);
            }
        }
        else {
            throw new WcsException("Failed locate request document.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }
    }









}