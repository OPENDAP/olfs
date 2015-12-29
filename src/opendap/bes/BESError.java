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

import opendap.bes.dap2Responders.BesApi;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.OPeNDAPException;
import opendap.http.mediaTypes.Html;
import opendap.viewers.ViewersServlet;
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

    private String _adminEmail;
    private String _message;
    private String _file;
    private String _line;

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

    /**
     * The BES timed out.
     *
     */
    public static final int TIME_OUT              = 6;


    Document besError = null;


    public BESError(Document error) {
        this(error,new Html());
    }


    private BESError() {
        _adminEmail = "support@opendap.org";
        _message = "Unknown Error";
        _file = "Unknown File";
        _line = "Unknown Line";
        setErrorCode(-1);
    }



    public BESError(Document error, MediaType mt) {
        this();
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

    public BESError( InputStream is) {

        this(is,new Html());


    }
    public BESError( InputStream is, MediaType mt) {
        this();


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



    public BESError(String error) {
        this(error, new Html());
    }

    public BESError(String error, MediaType mt) {
        this();
        _mt = mt;

        SAXBuilder sb = new SAXBuilder();

        try {
            Document edoc = sb.build(error);

            besError = processError(edoc);

            if(besError==null){
                setErrorCode(INVALID_ERROR);
                setErrorMessage("Unable to locate <BESError> object in stream.");
            }

        } catch (JDOMException | IOException e) {
            setErrorCode(INVALID_ERROR);
            setErrorMessage("Unable to process <BESError> object in stream.");
        }
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



    @Override
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

            case BESError.TIME_OUT:
                httpStatus = 418; // I'm a Teapot!!
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

            Element e;


            // <Type>
            e = error.getChild("Type",BES_NS);
            if(e!=null){
                String s = e.getTextTrim();
                setErrorCode(Integer.valueOf(s));
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

            // </Location>


        }
        catch(NumberFormatException nfe){
            setErrorCode(-1);
        }


        // setErrorMessage(makeBesErrorMsg(error));

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
            if(xsltFile.exists()) {
                Transformer transformer = new Transformer(xsltDoc);
                transformer.setParameter("serviceContext", context);
                JDOMSource error = new JDOMSource(besError);
                response.setContentType("text/html");
                response.setStatus(errorVal);
                transformer.transform(error, response.getOutputStream());
                done = true;



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
