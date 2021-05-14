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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

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


    private String _adminEmail;
    private String _message;
    private String _file;
    private String _line;

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
    public static final int TIME_OUT              = 6;


    /**
     *
     */
    private Document besErrorDoc = null;


    /**
     *
     * @param error
     */
    public BESError(Document error) {
        this(error,new TextHtml());
    }


    /**
     *
     */
    private BESError() {
        _adminEmail = "support@opendap.org";
        _message = "Unknown Error";
        _file = "Unknown File";
        _line = "Unknown Line";
        setBesErrorCode(INVALID_ERROR);
    }


    /**
     *
     * @param error
     * @param mt
     */
    public BESError(Document error, MediaType mt) {
        this();
        setResponseMediaType(mt);

        Iterator i = error.getDescendants(new ElementFilter(BES_ERROR));

        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
            error.detachRootElement();
            error.setRootElement(e);
        }

        besErrorDoc = processError(error);


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

        try {
            is = cueErrorStreamToXmlStart(is);
            Document error = sb.build(is);
            besErrorDoc = processError(error);

            if (besErrorDoc == null) {
                String msg = "ERROR - Failed to locate <BESError> object in XML document parsed from stream!";
                becomeInvalidError(msg);
            }

        }
        catch (JDOMException e) {
            String msg = "ERROR - Unable to parse expected <BESError> object from stream!";
            msg += " Message: " + e.getMessage();
            becomeInvalidError(msg);
        }
        catch (IOException e) {
            StringBuilder msg = new StringBuilder();
            msg.append("ERROR - Failed to locate expected <BESError> object from stream.");
            msg.append(" Message: ").append(e.getMessage());
            becomeInvalidError(msg.toString());
        }

    }


    /**
     *
     * @param message
     */
    private void becomeInvalidError(String message){

        besErrorDoc = makeBesErrorDoc(INVALID_ERROR,message,null, null, -1);
        processError(besErrorDoc);

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

            besErrorDoc = processError(edoc);

            if(besErrorDoc ==null){
                becomeInvalidError("Unable to locate <BESError> object in stream.");
            }

        } catch (JDOMException | IOException e) {
            becomeInvalidError("Unable to process <BESError> object in stream.");
        }
    }


    /**
     * Returns the error code.
     *
     * @return the error code.
     */
    public final int getBesErrorCode() {
        return _besErrorCode;
    }
    /**
     * Sets the error code.
     *
     * @param code the error code.
     */
    public final void setBesErrorCode(int code) {
        _besErrorCode = code;
    }


    /**
     *
     * @return
     */
    public boolean notFound(){
        return getBesErrorCode()==NOT_FOUND_ERROR;
    }

    /**
     *
     * @return
     */
    public boolean forbidden(){
        return getBesErrorCode()==FORBIDDEN_ERROR;
    }

    /**
     *
     * @return
     */
    public boolean syntax(){
        return getBesErrorCode()==USER_SYNTAX_ERROR;
    }

    /**
     *
     * @return
     */
    public boolean internal(){
        return getBesErrorCode()==INTERNAL_FATAL_ERROR || getBesErrorCode()==INTERNAL_ERROR;
    }


    /**
     *
     * @return
     */
    public int convertBesErrorCodeToHttpStatusCode(){
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

            case BESError.USER_SYNTAX_ERROR:
                httpStatus = HttpServletResponse.SC_BAD_REQUEST;
                break;

            case BESError.TIME_OUT:
                // Not a great semantic match as it's not a server error per say,
                // but this what the users wanted.
                httpStatus = HttpServletResponse.SC_GATEWAY_TIMEOUT;
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
    private Document makeBesErrorDoc(int besErrorCode, String message, String admin, String file, int line) {


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
        return new Document(besErrorElement);
    }


    /**
     *
     * @param error
     */
    private void processError(Element error){
        try {
            Element e;
            // <Type>
            e = error.getChild("Type",BES_NS);
            if(e!=null){
                String s = e.getTextTrim();
                setBesErrorCode(Integer.valueOf(s));
            }

            // <Administrator>
            e = error.getChild("Administrator",BES_NS);
            if(e!=null){
                _adminEmail = e.getTextTrim();
            }

            // <Message>
            e = error.getChild("Message",BES_NS);
            if(e!=null){
                setErrorMessage(e.getTextTrim());
            }

            // <Location>
            Element location = error.getChild("Location",BES_NS);
            if(location!=null){
                // <File>
                e = error.getChild("File",BES_NS);
                if(e!=null){
                    _file = e.getTextTrim();
                }
                // <Line>
                e = error.getChild("Line",BES_NS);
                if(e!=null){
                    _line = e.getTextTrim();
                }
            }
        }
        catch(NumberFormatException nfe){
            setBesErrorCode(-1);
        }

        int httpStatus = convertBesErrorCodeToHttpStatusCode();
        setHttpStatusCode(httpStatus);
        // setErrorMessage(makeBesErrorMsg(error));
    }


    /**
     *
     * @param error
     * @return
     */
    private Document processError(Document error){
        Iterator i = error.getDescendants(new ElementFilter(BES_ERROR));
        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
            error.detachRootElement();
            error.setRootElement(e);
            processError(e);
            return error;
        }
        else {
            return null;
        }
    }



}
