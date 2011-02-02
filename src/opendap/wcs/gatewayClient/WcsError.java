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

package opendap.wcs.gatewayClient;

import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ServletUtil;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

import javax.servlet.http.HttpServletResponse;
import java.io.*;


/**
 * Thrown when something BAD happens in the BES - primairly used to wrap BES
 * errors in a way that the servlet can manage.
 *
 *
 *
 *
 *
 */
public class WcsError extends OPeNDAPException {


    public static final String WCS_ERROR = "WCSError";


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








    public WcsError(String msg) {
        super(msg);
    }

    public WcsError(String msg, Exception e) {
        super(msg, e);
    }

    public WcsError(String msg, Throwable cause) {
        super(msg, cause);
    }

    public WcsError(Throwable cause) {
        super(cause);
    }



    public boolean notFound(){
        return getErrorCode()==NOT_FOUND_ERROR;
    }

    public boolean forbidden(){
        return getErrorCode()==FORBIDDEN_ERROR;
    }


    /*
        <BESError>
            <Type>3</Type>
            <Message>Command show ass does not have a registered response handler</Message>
            <Administrator>ndp@opendap.org</Administrator>
        </BESError>
     */
    public Document getErrorDocument(){
        Document errDoc = new Document();

        return errDoc;

    }




    public void sendErrorResponse(DispatchServlet dispatchServlet, HttpServletResponse response)
            throws IOException{


        int errorVal;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        switch(getErrorCode()){

            case WcsError.INTERNAL_ERROR:
                errorVal = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

            case WcsError.INTERNAL_FATAL_ERROR:
                errorVal = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

            case WcsError.NOT_FOUND_ERROR:
                errorVal = HttpServletResponse.SC_NOT_FOUND;
                break;

            case WcsError.FORBIDDEN_ERROR:
                errorVal = HttpServletResponse.SC_FORBIDDEN;
                break;

            case WcsError.SYNTAX_USER_ERROR:
                errorVal = HttpServletResponse.SC_BAD_REQUEST;

                break;

            default:
                errorVal = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;

        }


        try {
            String xsltDoc = ServletUtil.getSystemPath(dispatchServlet,
                                                 "/docs/xsl/error"+errorVal+".xsl");

            File xsltFile = new File(xsltDoc);

            if(xsltFile.exists()){
                XSLTransformer transformer = new XSLTransformer(xsltDoc);
                Document errorPage = transformer.transform(getErrorDocument());
                response.setContentType("text/html");
                response.setStatus(errorVal);
                xmlo.output(errorPage, response.getOutputStream());
            }
            else {
                if(!response.isCommitted())
                    response.reset();
                response.sendError(errorVal,getMessage());
            }

        }
        catch(Exception e){
            if(!response.isCommitted())
                response.reset();
            response.sendError(errorVal,getMessage());
        }


    }



}
