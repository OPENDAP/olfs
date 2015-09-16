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
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.OPeNDAPException;
import opendap.http.mediaTypes.Html;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformer;

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

    private MediaType _mt;


    public static final String BES_ERROR = "BESError";


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


    Document besError = null;


    public BESError(Document error) {
        this(error,new Html());
    }




    public BESError(Document error, MediaType mt) {

        _mt = mt;

        Iterator i = error.getDescendants(new ElementFilter(BES_ERROR));

        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
            error.detachRootElement();
            error.setRootElement(e);
        }

        besError = processError(error);


    }

    public BESError(Element error) {
        this(error,new Html());

    }
    public BESError(Element error, MediaType mt) {

        _mt = mt;

        besError = new Document(error);
        processError(error);

    }

    public BESError( InputStream is) {

        this(is,new Html());


    }
    public BESError( InputStream is, MediaType mt) {


        _mt = mt;

        SAXBuilder sb = new SAXBuilder();

        try {
            Document error = sb.build(is);

            besError = processError(error);

            if(besError==null){
                setErrorCode(INVALID_ERROR);
                setErrorMessage("Unable to locate <BESError> object in stream.");
            }

        } catch (JDOMException | IOException e) {
            setErrorCode(INVALID_ERROR);
            setErrorMessage("Unable to process <BESError> object in stream.");
        }


    }



    public BESError(String msg) {
        this(msg, new Html());
    }

    public BESError(String msg, MediaType mt) {
        super(msg);
        _mt = mt;
    }
    public BESError(String msg, Exception e) {
        this(msg, e, new Html());
    }

    public BESError(String msg, Exception e, MediaType mt) {
        super(msg, e);
        _mt = mt;
    }

    public BESError(String msg, Throwable cause) {
        this(msg,cause, new Html());
    }

    public BESError(String msg, Throwable cause, MediaType mt) {
        super(msg, cause);
        _mt = mt;
    }

    public BESError(Throwable cause) {
        this(cause, new Html());
    }
    public BESError(Throwable cause, MediaType mt) {
        super(cause);
        _mt = mt;

    }


    public void setReturnMediaType(MediaType mt){
        _mt = mt;
    }

    public MediaType getReturnMediaType(){
        return _mt;
    }

    public boolean notFound(){
        return getErrorCode()==NOT_FOUND_ERROR;
    }

    public boolean forbidden(){
        return getErrorCode()==FORBIDDEN_ERROR;
    }

    public boolean syntax(){
        return getErrorCode()==USER_SYNTAX_ERROR;
    }

    public boolean internal(){
        return getErrorCode()==INTERNAL_FATAL_ERROR || getErrorCode()==INTERNAL_ERROR;
    }



    public int getHttpStatus(){
        int httpStatus;
        switch(getErrorCode()){

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

            default:
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

        }

        return httpStatus;

    }





    private  String makeBesErrorMsg(Element besErrorElement) {
        Element e1, e2;

        String msg = "";

        msg += "[";
        msg += "[BESError]";

        e1 = besErrorElement.getChild("Type",BES_NS);
        if(e1!=null)
            msg += "[Type: " + e1.getTextTrim() + "]";


        e1 = besErrorElement.getChild("Message",BES_NS);
        if(e1!=null)
            msg += "[Message: " + e1.getTextTrim() + "]";

        e1 = besErrorElement.getChild("Administrator",BES_NS);
        if(e1!=null)
            msg += "[Administrator: " + e1.getTextTrim() + "]";

        e1 = besErrorElement.getChild("Location",BES_NS);
        if(e1!=null){
            msg += "[Location: ";
            e2 = e1.getChild("File",BES_NS);
            if(e2!=null)
                msg += e2.getTextTrim();

            e2 = e1.getChild("Line",BES_NS);
            if(e2!=null)
                msg += " line " + e2.getTextTrim();

            msg += "]";
        }
        msg += "]";

        return msg;
    }


    private void processError(Element error){
        try {
            Element e = error.getChild("Type",BES_NS);
            if(e!=null){
                String s = e.getTextTrim();
                setErrorCode(Integer.valueOf(s));
            }
            else {
                setErrorCode(-1);
            }
        }
        catch(NumberFormatException nfe){
            setErrorCode(-1);
        }


        setErrorMessage(makeBesErrorMsg(error));

    }


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
        else
            return null;


    }


    /**
     *
     * @param response
     * @return The HTTP status code returned to client.
     * @throws IOException
     */
    public int sendErrorResponse(String systemPath, String context, HttpServletResponse response)
            throws IOException{

        if(_mt.getSubType().equalsIgnoreCase("html"))
            return  sendHtmlErrorResponse(systemPath,context,response);


        if(_mt.getSubType().equalsIgnoreCase("json"))
            return  sendJsonErrorResponse(systemPath, context, response);

        return  sendHtmlErrorResponse(systemPath,context,response);

    }

    private int sendJsonErrorResponse(String systemPath, String context, HttpServletResponse response) throws IOException  {
        int errorVal =  getHttpStatus();


        return errorVal;

    }


    private int sendHtmlErrorResponse(String systemPath, String context, HttpServletResponse response) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        int errorVal =  getHttpStatus();


        try {
            if(!response.isCommitted())
                response.reset();


            String xsltDoc = systemPath + "/xsl/error"+errorVal+".xsl";


            boolean done = false;
            File xsltFile = new File(xsltDoc);
            if(xsltFile.exists()){
                XSLTransformer transformer = new XSLTransformer(xsltDoc);
                Document errorPage = transformer.transform(besError);
                if(errorPage!=null){
                    response.setContentType("text/html");
                    response.setStatus(errorVal);
                    xmlo.output(errorPage, response.getOutputStream());
                    xmlo.output(errorPage, System.out);
                    xmlo.output(besError, System.out);
                    done = true;
                }

            }

            if(!done) {
                HttpResponder.sendHttpErrorResponse(
                        errorVal,
                        getMessage(),
                        systemPath + "/error/error.html.proto",
                        context,
                        response);
            }

        }
        catch(Exception e){
            if(!response.isCommitted())
                response.reset();
            try {
                HttpResponder.sendHttpErrorResponse(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        e.getMessage(),
                        systemPath + "/error/error.html.proto",
                        context,response);
            }
            catch(Exception e1){
                response.sendError(errorVal,e1.getMessage());
            }

        }

        return errorVal;

    }


}
