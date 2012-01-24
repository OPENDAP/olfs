/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.bes;

import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ServletUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;
import org.jdom.input.SAXBuilder;
import org.jdom.filter.ElementFilter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.io.*;


/**
 * Thrown when something BAD happens in the BES - primairly used to wrap BES
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
    public static final int SYNTAX_USER_ERROR     = 3;


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

        besError = new Document(error);
        processError(error);


    }

    public BESError( InputStream is) {
        SAXBuilder sb = new SAXBuilder();

        try {
            Document error = sb.build(is);

            besError = processError(error);

            if(besError==null){
                setErrorCode(INVALID_ERROR);
                setErrorMessage("Unable to locate <BESError> object in stream.");
            }

        } catch (Exception e) {

            setErrorCode(INVALID_ERROR);

            setErrorMessage("Unable to process <BESError> object in stream.");


        }


    }






    public BESError(String msg) {
        super(msg);
    }

    public BESError(String msg, Exception e) {
        super(msg, e);
    }

    public BESError(String msg, Throwable cause) {
        super(msg, cause);
    }

    public BESError(Throwable cause) {
        super(cause);
    }



    public boolean notFound(){
        return getErrorCode()==NOT_FOUND_ERROR;
    }

    public boolean forbidden(){
        return getErrorCode()==FORBIDDEN_ERROR;
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


        int errorVal;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        switch(getErrorCode()){

            case BESError.INTERNAL_ERROR:
                errorVal = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

            case BESError.INTERNAL_FATAL_ERROR:
                errorVal = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

            case BESError.NOT_FOUND_ERROR:
                errorVal = HttpServletResponse.SC_NOT_FOUND;
                break;

            case BESError.FORBIDDEN_ERROR:
                errorVal = HttpServletResponse.SC_FORBIDDEN;
                break;

            case BESError.SYNTAX_USER_ERROR:
                errorVal = HttpServletResponse.SC_BAD_REQUEST;

                break;

            default:
                errorVal = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

        }


        try {
            String xsltDoc = systemPath + "/docs/xsl/error"+errorVal+".xsl";


            File xsltFile = new File(xsltDoc);

            if(xsltFile.exists()){
                XSLTransformer transformer = new XSLTransformer(xsltDoc);
                Document errorPage = transformer.transform(besError);

                if(!response.isCommitted())
                    response.reset();

                response.setContentType("text/html");
                response.setStatus(errorVal);
                xmlo.output(errorPage, response.getOutputStream());
                xmlo.output(errorPage, System.out);
                xmlo.output(besError, System.out);
            }
            else {
                if(!response.isCommitted())
                    response.reset();
                HttpResponder.sendHttpErrorResponse(500, getMessage(), systemPath + "/error/error.html.proto",context,response);
            }

        }
        catch(Exception e){
            if(!response.isCommitted())
                response.reset();
            try {
                HttpResponder.sendHttpErrorResponse(500,e.getMessage(),systemPath + "/error/error.html.proto",context,response);
            }
            catch(Exception e1){
                response.sendError(errorVal,e1.getMessage());
            }

        }

        return errorVal;

    }



}
