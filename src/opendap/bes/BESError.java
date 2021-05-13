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
import opendap.namespaces.*;
import opendap.namespaces.BES;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
     *
     * @param is
     * @param mt
     */
    public BESError( InputStream is, MediaType mt) {
        this();

        setResponseMediaType(mt);
        SAXBuilder sb = new SAXBuilder();
        try {
            Document error = sb.build(is);
            besErrorDoc = processError(error);
            if(besErrorDoc == null){
                becomeInvalidError("ERROR - Failed to locate <BESError> object in XML document parsed from stream.");
            }
        }
        catch (JDOMException e) {
            becomeInvalidError("ERROR - Unable to parse expected <BESError> object from stream! Message: "+e.getMessage());
        }
        catch (IOException e) {
            becomeInvalidError("ERROR - Failed to read expected <BESError> object from stream. Message: "+e.getMessage());
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
