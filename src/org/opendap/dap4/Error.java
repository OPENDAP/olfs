/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package org.opendap.dap4;

import opendap.coreServlet.HttpResponder;
import opendap.namespaces.DAP4;
import org.jdom.Document;
import org.jdom.Element;
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
*
 * \
 *
 *
<grammar xmlns="http://relaxng.org/ns/structure/1.0"
xmlns:doc="http://www.example.com/annotation"
datatypeLibrary="http://xml.opendap.org/datatypes/dap4"
ns="http://xml.opendap.org/ns/DAP/4.0#"
>
<start>
  <ref name="errorresponse"/>
</start>
<define name="errorresponse">
  <element name="Error">
    <optional>
      <attribute name="httpcode"><data type="dap4_integer"/></attribute>
    </optional>
    <optional>
      <interleave>
        <element name = "Message"><text/></Message>
        <element name = "Context"><text/></Message>
        <element name = "OtherInformation"><text/></Message>
      </interleave>
    </optional>
  </element>
</define>

 *
 *
 *
 *
 *
 * */
public class Error {



    private String message;
    private String context;
    private String otherInfo;

    private int httpcode;



    Document errorDoc = null;







    public Error(){

        setHttpCode(-1);
        message = null;
        context = null;
        otherInfo = null;
    }

    public Error(String msg) {


        setHttpCode(-1);
        message = msg;
        context = null;
        otherInfo = null;

    }


    public Error(Document error) {

        Iterator i = error.getDescendants(new ElementFilter(DAP4.ERROR, DAP4.NS));

        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
            error.detachRootElement();
            error.setRootElement(e);
        }

        errorDoc = ingestError(error);


    }

    public Error(Element error) {

        errorDoc = new Document(error);
        ingestError(error);


    }

    public Error( InputStream is) {
        SAXBuilder sb = new SAXBuilder();

        try {
            Document error = sb.build(is);

            errorDoc = ingestError(error);

            if(errorDoc==null){
                setHttpCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                setMessage("Unable to locate <Error> object in stream.");
            }

        } catch (Exception e) {

            setHttpCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            setMessage("Unable to process <Error> object in stream.");


        }


    }









    public int setHttpCode(int code){
        //@TODO Make this thing look at the code and QC it's HTTP codyness.

        httpcode = code;
        return getHttpCode();

    }

    public int setHttpCode(String codeString) {

        if (codeString != null) {
            try {
                setHttpCode(Integer.valueOf(codeString));
            } catch (NumberFormatException nfe) {
                setHttpCode(-1);
            }
        } else {
            setHttpCode(-1);
        }

        return getHttpCode();
    }


    public int getHttpCode(){

        return httpcode;
    }


    public void setMessage(String msg){
        message = msg;
    }


    public String  getMessage(){
        return message;
    }


    public void setContext(String context){
        this.context = context;
    }


    public String  getContext(){
        return context;
    }


    public void setOtherInformation(String otherInfo){
        this.otherInfo = otherInfo;
    }


    public String  getOtherInformation(){
        return otherInfo;
    }




    public boolean notFound(){
        return getHttpCode()==HttpServletResponse.SC_NOT_FOUND;
    }

    public boolean forbidden(){
        return getHttpCode()==HttpServletResponse.SC_FORBIDDEN;
    }







    public  String toString() {

        StringBuilder msg = new StringBuilder();

        msg.append("[dap4:Error ");
        msg.append("[httpcode(").append(getHttpCode()).append(")]");
        msg.append("[Message(").append(getHttpCode()).append(")]");
        msg.append("[Context(").append(getHttpCode()).append(")]");
        msg.append("[OtherInformation:  ").append(getHttpCode()).append(")]");
        msg.append("]");


        return msg.toString();
    }


    private void ingestError(Element error){


        setHttpCode(error.getAttributeValue("httpcode"));

        Element message = error.getChild("Message",DAP4.NS);
        this.message = null;
        if(message!=null){
            this.message = message.getTextTrim();
        }

        Element context = error.getChild("Context",DAP4.NS);
        this.context = null;
        if(context!=null){
            this.context = context.getTextTrim();
        }

        Element otherInfo = error.getChild("OtherInformation",DAP4.NS);
        this.otherInfo = null;
        if(otherInfo!=null){
            this.otherInfo = otherInfo.getTextTrim();
        }

    }


    private Document ingestError(Document error){
        Iterator i = error.getDescendants(new ElementFilter(DAP4.ERROR,DAP4.NS));

        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
//            error.detachRootElement();
//            error.setRootElement(e);
            ingestError(e);
            return error;
        }
        else
            return null;


    }


    /**
     *
     * @param response
     * @return The HTTP status code returned to client.
     * @throws java.io.IOException
     */
    public int sendErrorResponse(String systemPath, String context, HttpServletResponse response)
            throws IOException {


        int errorVal = getHttpCode();


        try {
            String xsltDoc = systemPath + "/docs/xsl/error"+errorVal+".xsl";


            File xsltFile = new File(xsltDoc);

            if(xsltFile.exists()){
                XSLTransformer transformer = new XSLTransformer(xsltDoc);
                Document errorPage = transformer.transform(errorDoc);

                if(!response.isCommitted())
                    response.reset();

                response.setContentType("text/html");
                response.setStatus(errorVal);
                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                xmlo.output(errorPage, response.getOutputStream());
                xmlo.output(errorPage, System.out);
                xmlo.output(errorDoc, System.out);
            }
            else {
                if(!response.isCommitted())
                    response.reset();
                HttpResponder.sendHttpErrorResponse(500, getMessage(), systemPath + "/error/error.html.proto", context, response);
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
