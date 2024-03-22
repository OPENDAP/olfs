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

package opendap.bes;

import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.http.mediaTypes.TextHtml;
import opendap.io.HyraxStringEncoding;
import opendap.namespaces.BES;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Iterator;


/**
 * Thrown when something BAD happens in the BES - primarily used to wrap BES
 * errors in a way that the servlet can manage.
 *
 *
 *
 *
 *
 *
 */
public class BESError extends OPeNDAPException {

    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;

    private Logger d_log;

    public static final String BES_ERROR = "BESError";

    /**
     * The error code.
     *
     * @serial
     */
    private int _besErrorCode;

    /**
     * The error is of an known type. This should NEVER happen and represents
     * a serious problem.
     */
    public static final int INVALID_ERROR         = -1;

    /**
     * Bad Things happened in the BES. Probably an Exception was thrown.
     */
    public static final int INTERNAL_ERROR        = 1;

    /**
     * Really Bad Things happened in the BES. The BES listener will be exiting,
     * and a new connection will have to be established.
     */
    public static final int INTERNAL_FATAL_ERROR  = 2;

    /**
     * Some part of the (possibly user supplied) syntax of the BES request was
     * incorrrect and could not be handled.
     */
    public static final int USER_SYNTAX_ERROR     = 3;


    /**
     * The user (or possibly the BES) does not have the required permissions
     * to access the requested resource.
     */
    public static final int FORBIDDEN_ERROR       = 4;

    /**
     * The BES could not find the requested resource.
     *
     */
    public static final int NOT_FOUND_ERROR       = 5;

    /**
     * The BES timed out.
     *
     */
    public static final int TIME_OUT_ERROR = 6;

    /**
     * The BES encountered an HttpError, aka a service chain error when
     * attempting to access a remote service.
     *
     */
    public static final int HTTP_ERROR = 7;


    /**
     *
     */
    private Element besErrorElement = null;


    /**
     *
     * @param besErrorDoc
     */
    public BESError(Document besErrorDoc) {
        this(besErrorDoc,new TextHtml());
    }


    /**
     *
     */
    private BESError() {
        d_log = LoggerFactory.getLogger(this.getClass());
    }


    /**
     *
     * @param besErrorDoc
     * @param mt
     */
    public BESError(Document besErrorDoc, MediaType mt) {
        this();
        setResponseMediaType(mt);
        besErrorElement = getBESErrorElement(besErrorDoc);


    }

    /**
     *
     * @param is
     */
    public BESError( InputStream is) {

        this(is,new TextHtml());

    }

    /**
     * In an attempt to recover from a possibly corrupt stream, or
     * FIXME - Covering for a bug in the way that PPT Client handles error chunks.
     * This method  Drops bytes off the front of the stream is until the stream begins with
     * the bytes "&lt;?xml "
     * @param is The stream to cue.
     * @return The cued up stream.
     * @throws IOException
     */
    private InputStream cueErrorStreamToXmlStart(InputStream is) throws IOException {

        byte xml_hdr[] ="<?xml ".getBytes(HyraxStringEncoding.getCharset());
        PushbackInputStream pis = new PushbackInputStream(is,xml_hdr.length);

        boolean found_xml_hdr = false;
        while(!found_xml_hdr && pis.available()>=xml_hdr.length){
            int retVal = pis.read();
            while(retVal != '<' &&  retVal!=-1) {
                retVal = pis.read();
            }
            if(retVal == '<'){
                pis.unread(retVal);

                byte look_ahead[] = new byte[xml_hdr.length];
                int laRetVal = pis.read(look_ahead);
                if(laRetVal == xml_hdr.length){
                    found_xml_hdr = true;
                    for(int i=0; i<xml_hdr.length; i++){
                        found_xml_hdr = found_xml_hdr && (look_ahead[i] == xml_hdr[i]);
                    }
                }
                pis.unread(look_ahead,0,laRetVal);
            }
        }
        if(!found_xml_hdr){
            String msg = "ERROR Failed to locate the BES Error XML header. ";
            throw new IOException(msg);
        }
        return pis;
    }

    /**
     * @param is
     * @param mt
     */
    public BESError(InputStream is, MediaType mt) {
        this();
        setResponseMediaType(mt);

        SAXBuilder sb = new SAXBuilder();
        String rawBesError = null;

        try {
            is = cueErrorStreamToXmlStart(is);

            rawBesError = IOUtils.toString(is, HyraxStringEncoding.getCharset());
            is = new ByteArrayInputStream(rawBesError.getBytes(HyraxStringEncoding.getCharset()));

            Document besErrorDoc = sb.build(is);
            besErrorElement = getBESErrorElement(besErrorDoc);
            if (besErrorElement == null) {
                StringBuilder msg = new StringBuilder();
                msg.append("ERROR - Failed to locate <BESError> object in XML document parsed from stream! ");
                if(d_log.isDebugEnabled())
                    msg.append(" RawInput: ").append(rawBesError);
                becomeInvalidError(msg.toString());
            }

        }
        catch (JDOMException e) {
            StringBuilder msg = new StringBuilder();
            msg.append("ERROR - Unable to parse expected <BESError> object from stream!");
            msg.append(" Message: ").append(e.getMessage());
            if(d_log.isDebugEnabled())
                msg.append(" RawInput: ").append(rawBesError);
            becomeInvalidError(msg.toString());
        }
        catch (IOException e) {
            StringBuilder msg = new StringBuilder();
            msg.append("ERROR - Failed to locate expected <BESError> object from stream.");
            msg.append(" Message: ").append(e.getMessage());
            if(d_log.isDebugEnabled())
                msg.append(" RawInput: ").append(rawBesError);
            becomeInvalidError(msg.toString());
        }

    }


    /**
     *
     * @param message
     */
    private void becomeInvalidError(String message){
        besErrorElement = makeBesErrorElement(INVALID_ERROR,message,null, null, -1);
    }

    /**
     *
     * @param error
     */
    public BESError(String error) {
        this(error, new TextHtml());
    }

    /**
     *
     * @param error
     * @param mt
     */
    public BESError(String error, MediaType mt) {
        this();
        setResponseMediaType(mt);

        SAXBuilder sb = new SAXBuilder();

        try {
            Document edoc = sb.build(error);

            besErrorElement = getBESErrorElement(edoc);

            if(besErrorElement ==null){
                becomeInvalidError("Unable to locate <BESError> object in stream.");
            }

        } catch (JDOMException | IOException e) {
            becomeInvalidError("Unable to process <BESError> object in stream.");
        }
    }


    public boolean notFound(){
        return getHttpStatusCode() == HttpServletResponse.SC_NOT_FOUND;
    }

    public boolean forbidden(){
        return getHttpStatusCode() == HttpServletResponse.SC_FORBIDDEN;
    }

    public boolean syntax(){
        return getHttpStatusCode() == HttpServletResponse.SC_BAD_REQUEST;
    }

    public boolean internal(){
        return getHttpStatusCode() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
    public boolean httpError(){
        return getBesErrorCode() == HTTP_ERROR;
    }



    /**
     *
     * @return
     */
    @Override
    public int getHttpStatusCode(){
        int httpStatus;

        switch(getBesErrorCode()){

            case BESError.INTERNAL_ERROR:
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

            case BESError.INTERNAL_FATAL_ERROR:
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

            case BESError.NOT_FOUND_ERROR:
                httpStatus = HttpServletResponse.SC_NOT_FOUND;
                break;

            case BESError.FORBIDDEN_ERROR:
                httpStatus = HttpServletResponse.SC_FORBIDDEN;
                break;

            // Since BES timeout conditions are mostly from users asking for too
            // much stuff from something like fileout_netcdf we map the
            // BES timeout to BAD_REQUEST because the user can change their
            // request to make it work. ndp 2/2/2223
            case BESError.TIME_OUT_ERROR:
            case BESError.USER_SYNTAX_ERROR:
                httpStatus = HttpServletResponse.SC_BAD_REQUEST;
                break;

            case BESError.HTTP_ERROR:
                String status = get_value("http_status");
                try {
                    httpStatus = Integer.parseUnsignedInt(status);
                }
                catch(NumberFormatException nfe){
                    d_log.error("BESError is an instance of BesHttpError. " +
                            "Yet, http_status has an un-parsable or non-integer" +
                            " value. status: {}",status);
                    httpStatus =HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }
                break;

            default:
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;
        }
        return httpStatus;
    }


    /**
     *
     * @param besErrorCode
     * @param message
     * @param admin
     * @param file
     * @param line
     * @return
     */
    private Element makeBesErrorElement(int besErrorCode, String message, String admin, String file, int line) {


        Element besErrorElement = new Element("BESError", BES.BES_NS);

        Element typeElement = new Element("Type", BES.BES_NS);
        typeElement.setText(Integer.toString(besErrorCode));
        besErrorElement.addContent(typeElement);

        Element messageElement = new Element("Message", BES.BES_NS);
        messageElement.setText(message);
        besErrorElement.addContent(messageElement);

        if (admin != null) {
            Element administratorElement = new Element("Administrator", BES.BES_NS);
            administratorElement.setText(admin);
            besErrorElement.addContent(administratorElement);
        }

        if (file != null) {
            Element locationElement = new Element("Location", BES.BES_NS);

            Element fileElement = new Element("File", BES.BES_NS);
            fileElement.setText(file);
            locationElement.addContent(fileElement);

            Element lineElement = new Element("Line", BES.BES_NS);
            lineElement.setText(Integer.toString(line));
            locationElement.addContent(lineElement);

            besErrorElement.addContent(locationElement);

        }
        return besErrorElement;
    }

    public String get_value(String key){
        Element e = besErrorElement.getChild(key,BES_NS);
        if(e!=null) {
            return e.getTextTrim();
        }
        return null;
    }

    public String getAdmin(){
        return get_value("Administrator");
    }

    public String getMessage(){
        return get_value("Message");
    }

    private Element getLocationElement(){
        return besErrorElement.getChild("Location",BES_NS);
    }

    public String getFile(){
        Element location = besErrorElement.getChild("Location",BES_NS);
        if(location!=null){
            Element e = location.getChild("File",BES_NS);
            if(e!=null){
                return e.getTextTrim();
            }
        }
        return null;
    }
    public String getLine(){
        Element location = besErrorElement.getChild("Location",BES_NS);
        if(location!=null){
            Element e = location.getChild("Line",BES_NS);
            if(e!=null){
                return e.getTextTrim();
            }
        }
        return null;
    }


    public int getBesErrorCode(){
        // <Type>
        Element e = besErrorElement.getChild("Type",BES_NS);
        if(e!=null){
            String s = e.getTextTrim();
            try {
                return Integer.parseInt(s);
            }
            catch(NumberFormatException nfe){
                d_log.error("BESError element has a non-integer value for Type: {}",s);
            }
        }
        return -1;
    }

    /**
     *
     * @param besErrorDoc
     * @return
     */
    private Element getBESErrorElement(Document besErrorDoc){
        Iterator i = besErrorDoc.getDescendants(new ElementFilter(BES_ERROR));
        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
            return e;
        }
        else {
            return null;
        }
    }



}
