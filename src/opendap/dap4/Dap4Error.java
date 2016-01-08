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

package opendap.dap4;

import opendap.io.HyraxStringEncoding;
import opendap.namespaces.DAP4;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
public class Dap4Error {



    private String message;
    private String context;
    private String otherInfo;

    private int _httpStatusCode;



    Document errorDoc = null;







    public Dap4Error(){

        setHttpCode(-1);
        message = null;
        context = null;
        otherInfo = null;
    }

    public Dap4Error(String msg) {


        setHttpCode(-1);
        message = msg;
        context = null;
        otherInfo = null;

    }


    public Dap4Error(Document error) {

        Iterator i = error.getDescendants(new ElementFilter(DAP4.ERROR, DAP4.NS));

        if(i.hasNext()){
            Element e = (Element)i.next();
            e.detach();
            error.detachRootElement();
            error.setRootElement(e);
        }

        errorDoc = ingestError(error);


    }

    public Dap4Error(Element error) {

        errorDoc = new Document(error);
        ingestError(error);


    }

    public Dap4Error( InputStream is) {
        SAXBuilder sb = new SAXBuilder();
        try {
            Document error = sb.build(is);

            errorDoc = ingestError(error);

            if(errorDoc==null){
                setHttpCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                setMessage("Unable to locate <Error> object in stream.");
            }


        } catch (JDOMException | IOException e) {
            setHttpCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            setMessage("Unable to read/parse BES <Error> object in stream. Something BAD happened and no additional information is available. :(");
        }


    }









    public int setHttpCode(int code){
        //@TODO Make this thing look at the code and QC it's HTTP codyness.

        _httpStatusCode = code;
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

        return _httpStatusCode;
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




    // public boolean notFound(){ return getHttpCode()==HttpServletResponse.SC_NOT_FOUND;  }

    public boolean forbidden(){
        return getHttpCode()==HttpServletResponse.SC_FORBIDDEN;
    }


    public Element getErrorElement(){
        Element error = new Element("Error",DAP4.NS);

        error.setAttribute("httpcode",getHttpCode()+"");

        Element e = new Element("Message",DAP4.NS);
        e.setText(getMessage());
        error.addContent(e);

        e = new Element("Context",DAP4.NS);
        e.setText(getContext());
        error.addContent(e);

        e = new Element("OtherInformation",DAP4.NS);
        e.setText(getOtherInformation());
        error.addContent(e);



        return error;
    }

    public Document getErrorDocument(){
         return new Document(getErrorElement());
    }


    public void print(OutputStream os) throws IOException {
        os.write(toString().getBytes(HyraxStringEncoding.getCharset()));
    }



    public  String toString() {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(getErrorDocument());

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
     * @param systemPath The location, on disk, of the top level directory that holds the required XSLT documents.
     * @param context The "context" in which the server is running.
     * @param response The response object to populate with the error.
     * @return The HTTP status code returned to client.
     * @throws java.io.IOException
     */
    /*
    public int sendErrorResponse(String systemPath, String context, HttpServletResponse response)
            throws IOException {


        int errorVal = getHttpCode();


        try {
            if(!response.isCommitted())
                response.reset();

            String xsltDoc = systemPath + "/docs/xsl/error"+errorVal+".xsl";


            boolean done = false;
            File xsltFile = new File(xsltDoc);
            if(xsltFile.exists()){

                XSLTransformer transformer = new XSLTransformer(xsltDoc);
                Document errorPage = transformer.transform(errorDoc);

                if(errorPage!=null) {
                    response.setContentType("text/html");
                    response.setStatus(errorVal);
                    XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                    xmlo.output(errorPage, response.getOutputStream());
                    xmlo.output(errorPage, System.out);
                    xmlo.output(errorDoc, System.out);
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
                HttpResponder.sendHttpErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage(),systemPath + "/error/error.html.proto",context,response);
            }
            catch(Exception e1){
                response.sendError(errorVal,e1.getMessage());
            }

        }

        return errorVal;

    }
    */


}
