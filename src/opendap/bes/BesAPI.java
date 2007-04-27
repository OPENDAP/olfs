/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;

import java.io.*;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;


/**
 * BES transaction code and client pool management are contained  in this class.
 * All of the methods are static, but the class is not stateless, as it
 * maintains an internal pool of client connections to the BES. Since all of the
 * BES transaction code for the OLFS is wrapped in this class, future
 * optimizations should be easier... (right?)
 */
public class BesAPI {


    public static String XML_ERRORS  = "xml";
    public static String DAP2_ERRORS = "dap2";



    private static Logger log;
    private static boolean _initialized = false;



    /**
     * The name of the BES Exception Element.
     */
    private static String BES_EXCEPTION = "BESException";


    /**
     * Initializes logging for the BesAPI class.
     *
     */
    public static void init() {

        if(_initialized) return;

            log = org.slf4j.LoggerFactory.getLogger(BesAPI.class);

        _initialized = true;


    }


    /**
     * Writes an OPeNDAP DDX for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     * the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeDDX(String dataSource,
                                String constraintExpression,
                                OutputStream os,
                                String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        besGetTransaction(
                getAPINameForDDX(),
                dataSource,
                constraintExpression,
                os,
                errorMsgFormat);
    }


    public static Document getDDXDocument(String dataSource,
                                          String constraintExpression)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException,
            BESException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        besGetTransaction(
                getAPINameForDDX(),
                dataSource,
                constraintExpression,
                os,
                "xml");

        SAXBuilder sb = new SAXBuilder();


        log.debug("getDDXDocument got this array:\n" +
                    os.toString());

        Document ddx = sb.build(new ByteArrayInputStream(os.toByteArray()));

        // Check for an exception:
        besExceptionHandler(ddx);


        return ddx;
    }


    /**
     * Writes an OPeNDAP DDS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     * the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeDDS(String dataSource,
                                String constraintExpression,
                                OutputStream os,
                                String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        besGetTransaction(
                getAPINameForDDS(),
                dataSource,
                constraintExpression,
                os,
                errorMsgFormat);
    }


    /**
     * Writes the source data (it is often a file, thus the method name) to
     * the passed stream.
     *
     * @param dataSource The requested DataSource
     * @param os         The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeFile(String dataSource,
                                 OutputStream os,
                                 String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        boolean trouble = false;

        BES bes = BESManager.getBES(dataSource);

        if(bes==null)
            throw new BadConfigurationException("There is no BES to handle the requested data source: "+dataSource);

        OPeNDAPClient oc = bes.getClient();

        try {

            String besDataSource = bes.trimPrefix(dataSource);
            configureTransaction(oc, besDataSource, null, "stream",errorMsgFormat);

            getDataProduct(oc, getAPINameForStream(), os);
            resetBES(oc);
        }
        catch (PPTException e){
            trouble = true;
            String msg = "writeFile()  Problem with OPeNDAPClient.";
            log.error(msg);
            throw new PPTException(msg);
        }
        finally{
            bes.returnClient(oc,trouble);
        }
    }


    /**
     * Writes an OPeNDAP DAS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     * the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeDAS(String dataSource,
                                String constraintExpression,
                                OutputStream os,
                                String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        besGetTransaction(
                getAPINameForDAS(),
                dataSource,
                constraintExpression,
                os,
                errorMsgFormat);
    }

    /**
     * Writes an OPeNDAP DAP2 data response for the dataSource to the
     * passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     * the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeDap2Data(String dataSource,
                                     String constraintExpression,
                                     OutputStream os,
                                     String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        besGetTransaction(
                getAPINameForDODS(),
                dataSource,
                constraintExpression,
                os,
                errorMsgFormat);
    }


    /**
     * Writes the ASCII representation os the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     * the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeASCII(String dataSource,
                                  String constraintExpression,
                                  OutputStream os,
                                  String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        besGetTransaction(
                getAPINameForASCII(),
                dataSource,
                constraintExpression,
                os,
                errorMsgFormat);
    }


    /**
     * Writes the HTML data request form (aka the I.F.H.) for the OPeNDAP the
     * dataSource to the passed stream.
     *
     * @param dataSource The requested DataSource
     * @param url        The URL to refernence in the HTML form.
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeHTMLForm(String dataSource,
                                     String url,
                                     OutputStream os)
            throws BadConfigurationException, PPTException {


        boolean trouble = false;
        BES bes = BESManager.getBES(dataSource);

        if(bes==null)
            throw new BadConfigurationException("There is no BES to handle the requested data source: "+dataSource);


        OPeNDAPClient oc = bes.getClient();

        try {

            String besDataSource = bes.trimPrefix(dataSource);
            configureTransaction(oc, besDataSource, null, DAP2_ERRORS);

            String cmd = "get " + getAPINameForHTMLForm() +
                    " for d1 using " + url + ";\n";

            log.debug("Sending command: " + cmd);

            oc.setOutput(os, false);
            oc.executeCommand(cmd);
            resetBES(oc);

        }
        catch (PPTException e){
            trouble = true;
            String msg = "writeHTMLForm() Problem with OPeNDAPClient.";
            log.error(msg);
            throw new PPTException(msg);
        }
        finally{
            bes.returnClient(oc,trouble);
        }
    }


    /**
     * Writes the OPeNDAP INFO response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param os         The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @throws BadConfigurationException .
     * @throws PPTException .
     */
    public static void writeINFOPage(String dataSource,
                                     OutputStream os,
                                     String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        besGetTransaction(
                getAPINameForINFOPage(),
                dataSource,
                null,
                os,
                errorMsgFormat);
    }


    /**
     * Returns an InputStream that holds an OPeNDAP DAP2 data for the requested
     * dataSource. The DDS header is stripped, so the InputStream holds ONLY
     * the XDR encoded binary data.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression .
     * @param errorMsgFormat .
     * @return A DAP2 data stream, no DDS just the XDR encoded binary data.
     * @throws BadConfigurationException .
     * @throws PPTException .
     * @throws IOException .
     */
    public static InputStream getDap2DataStream(String dataSource,
                                                String constraintExpression,
                                                String errorMsgFormat)
            throws BadConfigurationException, PPTException, IOException {

        //@todo Make this more efficient by adding support to the BES that reurns this stream. Caching the resposnse in memory is a BAD BAD thing.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeDap2Data(dataSource, constraintExpression, baos, errorMsgFormat);

        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        HeaderInputStream his = new HeaderInputStream(is);

        boolean done = false;
        int val;
        while (!done) {
            val = his.read();
            if (val == -1)
                done = true;

        }

        return is;

    }






    /**
     * Look for and process an Exception in the response from the BES.
     *
     * @param doc .
     * @throws BESException .
     */
    private static void besExceptionHandler(Document doc) throws BESException {


        Element exception;
        String msg = "";

        ElementFilter exceptionFilter = new ElementFilter(BES_EXCEPTION);
        Iterator i = doc.getDescendants(exceptionFilter);
        if (i.hasNext()) {
            int j = 0;
            while (i.hasNext()) {
                if (j > 0)
                    msg += "\n";
                exception = (Element) i.next();
                msg += makeBesExceptionMsg(exception, j++);
            }

            throw new BESException(msg);
        }

    }

    private static String makeBesExceptionMsg(Element exception, int number) {
        String msg = "";

        msg += "[";
        msg += "[BESException: " + number + "]";
        msg += "[Type: " + exception.getChild("Type").getTextTrim() + "]";
        msg += "[Message: " + exception.getChild("Message").getTextTrim() + "]";
        msg += "[Location: ";
        msg += exception.getChild("Location").getChild("File").getTextTrim()
                + " line ";
        msg += exception.getChild("Location").getChild("Line").getTextTrim()
                + "]";
        msg += "]";


        log.warn("Exception Message: " + msg);

        return msg;
    }


    private static void configureTransaction(OPeNDAPClient oc,
                                             String dataset,
                                             String constraintExpression,
                                             String errorMsgFormat)

            throws PPTException {
        configureTransaction(oc, dataset, constraintExpression, null, errorMsgFormat);
    }


    private static void configureTransaction(OPeNDAPClient oc,
                                             String dataset,
                                             String constraintExpression,
                                             String type,
                                             String errorMsgFormat)
            throws PPTException {


        log.debug("Setting error context to: \""+errorMsgFormat+"\"");
        oc.executeCommand("set context errors to "+errorMsgFormat+";\n");



        String cmd = "set container in catalog values "
                + dataset
                + ", "
                + dataset
                + (type == null ? "" : ", " + type) + ";\n";


        log.debug("Sending BES command: " + cmd);


        oc.executeCommand(cmd);


        log.debug("ConstraintExpression: " + constraintExpression);


        if (constraintExpression == null ||
                constraintExpression.equalsIgnoreCase("")) {

            cmd = "define d1 as " + dataset + ";\n";

        } else {
            cmd = "define d1 as "
                    + dataset
                    + " with "
                    + dataset
                    + ".constraint=\""
                    + constraintExpression + "\"  ;\n";

        }

        log.debug("Sending BES command: " + cmd);
        oc.executeCommand(cmd);

    }

    private static String getGetCmd(String product) {

        return "get " + product + " for d1;\n";

    }

    private static String getAPINameForDDS() {
        return "dds";
    }

    private static String getAPINameForDAS() {
        return "das";
    }

    private static String getAPINameForDODS() {
        return "dods";
    }

    private static String getAPINameForDDX() {
        return "ddx";
    }


    private static String getAPINameForStream() {
        return "stream";
    }


    private static String getAPINameForASCII() {
        return "ascii";
    }

    private static String getAPINameForHTMLForm() {
        return "html_form";
    }

    private static String getAPINameForINFOPage() {
        return "info_page";
    }


    private static void getDataProduct(OPeNDAPClient oc,
                                       String product,
                                       OutputStream os) throws PPTException {

        String cmd = getGetCmd(product);
        log.debug("Sending command: " + cmd);

        oc.setOutput(os, false);
        oc.executeCommand(cmd);

    }








    private static void besGetTransaction(String product,
                                          String dataset,
                                          String constraintExpression,
                                          OutputStream os,
                                          String errorMsgFormat)
            throws BadConfigurationException, PPTException {

        boolean trouble = false;



        log.debug("besGetTransaction started.");

        BES bes = BESManager.getBES(dataset);

        if(bes==null)
            throw new BadConfigurationException("There is no BES to handle the requested data source: "+dataset);



        OPeNDAPClient oc = bes.getClient();


        try {

            String besDataset  = bes.trimPrefix(dataset);

            configureTransaction(oc,
                                 besDataset,
                                 constraintExpression,
                                 errorMsgFormat);

            getDataProduct(oc, product, os);
            resetBES(oc);

        }
        catch (PPTException e){
            trouble = true;

            String msg = "besGetTransaction()  Problem encountered with OPeNDAPCLient. " +
                        "OPeNDAPClient executed "+oc.getCommandCount()+ " commands";
            log.error(msg);

            throw new PPTException(msg);
        }
        finally{
            bes.returnClient(oc,trouble);

        }
        log.debug("besGetTransaction complete.");

    }



















    private static void besShowTransaction(String product, String dataset, OutputStream os)
            throws PPTException, BadConfigurationException {

        log.debug("besShowTransaction started.");

        boolean trouble = false;



        String besDataset;

        BES bes = BESManager.getBES(dataset);

        if(bes==null){
            String msg = "There is no BES to handle the requested data source: "+dataset;
            log.error(msg);
            throw new BadConfigurationException(msg);
        }


        OPeNDAPClient oc = bes.getClient();

        try {

            String cmd = "show " + product;

            if(product.equals("version")){

                cmd += ";\n";
            }
            else {

                besDataset  = bes.trimPrefix(dataset);


                cmd += " for \""+besDataset+"\";\n";
            }




            log.debug("Sending command: " + cmd);
            oc.setOutput(os, false);
            log.debug("Setting error context to: \""+XML_ERRORS+"\"");
            oc.executeCommand("set context errors to "+XML_ERRORS+";\n");
            oc.executeCommand(cmd);
            resetBES(oc);

        }
        catch (PPTException e){
            trouble = true;
            String msg = "besShowTransaction() Problem with OPeNDAPClient. " +
                    "OPeNDAPClient executed "+oc.getCommandCount()+" commands";

            log.error(msg);
            throw new PPTException(msg);
        }
        finally{
            bes.returnClient(oc,trouble);
        }

        log.debug("besShowTransaction complete.");

    }

    /**
     * Returns an the version document for the BES.
     *
     * @param path The path prefix for the BES whose version is being sought.
     *
     * @return The version Document
     * @throws BadConfigurationException .
     * @throws PPTException .
     * @throws IOException .
     * @throws JDOMException .
     * @throws BESException .
     */
    public static Document showVersion(String path) throws
            BadConfigurationException,
            PPTException,
            IOException,
            JDOMException,
            BESException {

        // Get the version response from the BES (an XML doc)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        besShowTransaction("version", path, baos);

        log.debug("BES returned this document:\n"+
                "-----------\n" + baos +"-----------");


        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

        // Check for an exception:
        besExceptionHandler(doc);

        // Tweak it!

        // First find the response Element
        Element ver = doc.getRootElement().getChild("response");

        // Disconnect it from it's parent and then rename it.
        ver.detach();
        ver.setName("BES-Version");

        doc.detachRootElement();
        doc.setRootElement(ver);

        return doc;
    }

    /**
     * Returns the BES catalog Document for the specified dataSource, striped
     * of the <response> parent element.
     *
     * @param dataSource .
     * @return The BES catalog Document.
     * @throws PPTException .
     * @throws BadConfigurationException .
     * @throws IOException .
     * @throws JDOMException .
     * @throws BESException .
     */
    public static Document showCatalog(String dataSource) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException,
            BESException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = "catalog";

        BesAPI.besShowTransaction(product, dataSource, baos);

        log.debug("BES returned this document:\n" +
                "-----------\n" + baos +"-----------");

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

        // Check for an exception:
        besExceptionHandler(doc);
        // Tweak it!

        // First find the response Element

        Element topDataset =
                doc.getRootElement().getChild("response").getChild("dataset");

        // Disconnect it from it's parent and then rename it.
        topDataset.detach();
        doc.detachRootElement();
        doc.setRootElement(topDataset);

        return doc;

    }




    /**
     * Returns the BES INFO document for the spcified dataSource.
     *
     * @param dataSource .
     * @return The BES info document, stripped of it's <response> parent.
     * @throws PPTException .
     * @throws BadConfigurationException .
     * @throws IOException .
     * @throws JDOMException .
     * @throws BESException .
     */
    public static Document getInfoDocument(String dataSource) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException,
            BESException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = "info";


        BesAPI.besShowTransaction(product, dataSource, baos);


        log.debug("BES returned this document:\n" +
                    "-----------\n" + baos +"-----------");


        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

        // Check for an exception:
        besExceptionHandler(doc);

        // Prepare the response:

        // First find the response Element

        Element topDataset =
                doc.getRootElement().getChild("response").getChild("dataset");

        // Disconnect it from it's parent and then rename it.
        topDataset.detach();
        doc.detachRootElement();
        doc.setRootElement(topDataset);

        return doc;

    }

    public static void resetBES (OPeNDAPClient odc) throws PPTException {

        odc.setOutput(new DevNull(), false);

        String cmd = "delete definitions;\n";
        odc.executeCommand(cmd);

        cmd = "delete containers;\n";
        odc.executeCommand(cmd);


        odc.setOutput(null, false);

    }


    public static boolean isConfigured(){
        return BESManager.isConfigured();
    }


    public static Document getVersionDocument(String path) throws Exception{
        return BESManager.getVersionDocument(path);
    }
    public static Document getCombinedVersionDocument() throws Exception{
        return BESManager.getCombinedVersionDocument();
    }

    public static void configure(OLFSConfig olfsConfig) throws Exception {

        BESManager.configure(olfsConfig.getBESConfig());

    }

}
