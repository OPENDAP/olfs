/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2007 OPeNDAP, Inc.
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


    public static String XML_ERRORS = "xml";
    public static String DAP2_ERRORS = "dap2";


    private static Logger log;
    private static boolean _initialized = false;


    /**
     * The name of the BES Exception Element.
     */
    private static String BES_ERROR = "BESError";


    /**
     * Initializes logging for the BesAPI class.
     */
    public static void init() {

        if (_initialized) return;

        log = org.slf4j.LoggerFactory.getLogger(BesAPI.class);

        _initialized = true;


    }


    /**
     * Writes an OPeNDAP DDX for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to errors returned by
     *                             the BES..
     * @param errorMsgFormat       The message format scheme for BES generated
     *                             errors.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDDX(String dataSource,
                                String constraintExpression,
                                OutputStream os,
                                OutputStream err,
                                String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besGetTransaction(
                getAPINameForDDX(),
                dataSource,
                constraintExpression,
                os,
                err,
                errorMsgFormat);
    }


    public static boolean getDDXDocument(String dataSource,
                                          String constraintExpression,
                                          Document ddx,
                                          Document err)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        ByteArrayOutputStream os  = new ByteArrayOutputStream();
        ByteArrayOutputStream er = new ByteArrayOutputStream();
        SAXBuilder sb = new SAXBuilder();
        Document doc;

        if(besGetTransaction(
                getAPINameForDDX(),
                dataSource,
                constraintExpression,
                os,
                er,
                XML_ERRORS)){



            log.debug("getDDXDocument got this array:\n" +
                    os.toString());

            doc = sb.build(new ByteArrayInputStream(os.toByteArray()));

            Element dataset = doc.getRootElement();
            dataset.detach();
            doc.detachRootElement();

            ddx.setRootElement(dataset);

            return true;

        }
        else {
            doc = sb.build(new ByteArrayInputStream(er.toByteArray()));
            Element error = doc.getRootElement();
            error.detach();
            doc.detachRootElement();

            err.setRootElement(error);
            return false;
        }
    }


    /**
     * Writes an OPeNDAP DDS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat       The message format scheme for BES generated errors.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDDS(String dataSource,
                                String constraintExpression,
                                OutputStream os,
                                OutputStream err,
                                String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besGetTransaction(
                getAPINameForDDS(),
                dataSource,
                constraintExpression,
                os,
                err,
                errorMsgFormat);
    }


    /**
     * Writes the source data (it is often a file, thus the method name) to
     * the passed stream.
     *
     * @param dataSource     The requested DataSource
     * @param os             The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeFile(String dataSource,
                                 OutputStream os,
                                 OutputStream err,
                                 String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        boolean trouble = false;
        boolean success;

        BES bes = BESManager.getBES(dataSource);

        if (bes == null)
            throw new BadConfigurationException("There is no BES to handle the requested data source: " + dataSource);

        OPeNDAPClient oc = bes.getClient();

        try {

            String besDataSource = bes.trimPrefix(dataSource);

            success = configureTransaction(oc, besDataSource, null, getAPINameForStream(), err, errorMsgFormat);

            if(success){
                success = getDataProduct(oc, getAPINameForStream(), os, err);
            }
            resetBES(oc);

            return success;
        }
        catch (PPTException e) {
            trouble = true;
            String msg = "writeFile()  Problem with OPeNDAPClient.";
            log.error(msg);
            throw new PPTException(msg);
        }
        finally {
            bes.returnClient(oc, trouble);
        }
    }


    /**
     * Writes an OPeNDAP DAS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat       The message format scheme for BES generated errors.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDAS(String dataSource,
                                String constraintExpression,
                                OutputStream os,
                                OutputStream err,
                                String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besGetTransaction(
                getAPINameForDAS(),
                dataSource,
                constraintExpression,
                os,
                err,
                errorMsgFormat);
    }

    /**
     * Writes an OPeNDAP DAP2 data response for the dataSource to the
     * passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat       The message format scheme for BES generated errors.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDap2Data(String dataSource,
                                     String constraintExpression,
                                     OutputStream os,
                                     OutputStream err,
                                     String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besGetTransaction(
                getAPINameForDODS(),
                dataSource,
                constraintExpression,
                os,
                err,
                errorMsgFormat);
    }


    /**
     * Writes the ASCII representation _rawOS the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraint expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @param errorMsgFormat       The message format scheme for BES generated errors.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeASCII(String dataSource,
                                  String constraintExpression,
                                  OutputStream os,
                                  OutputStream err,
                                  String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besGetTransaction(
                getAPINameForASCII(),
                dataSource,
                constraintExpression,
                os,
                err,
                errorMsgFormat);
    }


    /**
     * Writes the HTML data request form (aka the I.F.H.) for the OPeNDAP the
     * dataSource to the passed stream.
     *
     * @param dataSource The requested DataSource
     * @param url The URL to refernence in the HTML form.
     * @param os  The Stream to which to write the response.
     * @param err The Stream to which to write errors returned by the BES.
     * @return True is everything goes well, false if the BES returns an error.
     * @throws BadConfigurationException .
     * @throws PPTException              .
     * @throws IOException              .
     * @throws BESError              .
     */
    public static boolean writeHTMLForm(String dataSource,
                                     String url,
                                     OutputStream os,
                                     OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {


        boolean trouble = false, success = true;
        BES bes = BESManager.getBES(dataSource);

        if (bes == null)
            throw new BadConfigurationException("There is no BES to handle " +
                    "the requested data source: " + dataSource);


        OPeNDAPClient oc = bes.getClient();
        try {

            String besDataSource = bes.trimPrefix(dataSource);
            success = configureTransaction(oc, besDataSource, null, err, XML_ERRORS);
            if(success){
                String cmd = "get " + getAPINameForHTMLForm() +
                        " for d1 using " + url + ";\n";

                log.debug("Sending command: " + cmd);

                success = oc.executeCommand(cmd,os,err);
            }
            resetBES(oc);

        }
        catch (PPTException e) {
            trouble = true;
            String msg = "writeHTMLForm() Problem with " +
                    "OPeNDAPClient: " + e.getMessage();
            log.error(msg);
            throw new PPTException(msg,e);
        }
        finally {
            bes.returnClient(oc, trouble);
        }

        return success;
    }


    /**
     * Writes the OPeNDAP INFO response for the dataSource to the passed
     * stream.
     *
     * @param dataSource     The requested DataSource
     * @param os             The Stream to which to write the response.
     * @param errorMsgFormat The message format scheme for BES generated errors.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeINFOPage(String dataSource,
                                        OutputStream os,
                                        OutputStream err,
                                        String errorMsgFormat)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besGetTransaction(
                getAPINameForINFOPage(),
                dataSource,
                null,
                os,
                err,
                errorMsgFormat);
    }


    /**
     * Returns an InputStream that holds an OPeNDAP DAP2 data for the requested
     * dataSource. The DDS header is stripped, so the InputStream holds ONLY
     * the XDR encoded binary data.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression .
     * @param errorMsgFormat       .
     * @return A DAP2 data stream, no DDS just the XDR encoded binary data.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static InputStream getDap2DataStream(String dataSource,
                                                String constraintExpression,
                                                String errorMsgFormat)
            throws BadConfigurationException, BESError, PPTException, IOException {

        //@todo Make this more efficient by adding support to the BES that reurns this stream. Caching the resposnse in memory is a BAD BAD thing.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeDap2Data(dataSource, constraintExpression, baos, baos, errorMsgFormat);

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








    private static boolean configureTransaction(OPeNDAPClient oc,
                                             String dataset,
                                             String constraintExpression,
                                             OutputStream err,
                                             String errorMsgFormat)

            throws PPTException, IOException {
        return configureTransaction(oc, dataset, constraintExpression, null, err, errorMsgFormat);
    }


    private static boolean configureTransaction(OPeNDAPClient oc,
                                             String dataset,
                                             String constraintExpression,
                                             String type,
                                             OutputStream erros,
                                             String errorMsgFormat)
            throws  PPTException, IOException {


        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String cmd;
        boolean success;

        cmd = "set context errors to " + errorMsgFormat + ";\n";
        log.debug("Sending BES command: " + cmd);



        success = oc.executeCommand(cmd,baos,erros);
        if(success) {

            baos.reset();

            String catalogName = "catalog";
            String container = "catalogContainer";

            if (WcsCatalog.isWcsDataset(dataset)) {
                catalogName = "wcs";
                container = "wcsContainer";
                dataset = "\"" + WcsCatalog.getWcsRequestURL(dataset) + "\"";
            }


            cmd = "set container in " + catalogName + " values "
                    + container
                    + ", "
                    + dataset
                    + (type == null ? "" : ", " + type) + ";\n";


            log.debug("Sending BES command: " + cmd);


            success = oc.executeCommand(cmd,baos,erros);
            if(success){

                baos.reset();

                log.debug("UnEscaped ConstraintExpression: " + constraintExpression);

                if(constraintExpression != null ){

                    // Encode all the double quotes as their hex representation of %22
                    constraintExpression = constraintExpression.replace("\"","%22");

                    // Use the backslash character (hex encoded as %5c) to escape
                    // all of the encoded double quotes
                    constraintExpression = constraintExpression.replace("%22","%5C%22");

                }
                log.debug("Escaped ConstraintExpression: " + constraintExpression);


                if (constraintExpression == null ||
                    constraintExpression.equalsIgnoreCase("")) {

                    cmd = "define d1 as " + container + ";\n";

                } else {
                    cmd = "define d1 as "
                            + container
                            + " with "
                            + container
                            + ".constraint=\""
                            + constraintExpression + "\"  ;\n";

                }

                log.debug("Sending BES command: " + cmd);
                success = oc.executeCommand(cmd,baos,erros);

            }

        }
        return success;


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


    private static boolean  getDataProduct(OPeNDAPClient oc,
                                       String product,
                                       OutputStream os,
                                       OutputStream err)
            throws PPTException {

        String cmd = getGetCmd(product);

        log.debug("Sending command: " + cmd);
        return oc.executeCommand(cmd,os,err);

    }


    private static boolean besGetTransaction(String product,
                                          String dataset,
                                          String constraintExpression,
                                          OutputStream os,
                                          OutputStream err,
                                          String errorMsgFormat)
            throws BadConfigurationException, IOException, PPTException {

        boolean besTrouble = false;


        log.debug("besGetTransaction started.");

        BES bes = BESManager.getBES(dataset);

        if (bes == null)
            throw new BadConfigurationException("There is no BES to handle the requested data source: " + dataset);


        OPeNDAPClient oc = bes.getClient();


        try {

            String besDataset = bes.trimPrefix(dataset);

            if(configureTransaction(oc,
                    besDataset,
                    constraintExpression,
                    err,
                    errorMsgFormat)){

                if(getDataProduct(oc, product, os, err)){
                    resetBES(oc);
                    return true;
                }
                else {
                    resetBES(oc);
                    return false;
                }

            }
            else {
                resetBES(oc);
                return false;
            }


        }
        catch (PPTException e) {
            besTrouble = true;

            String msg = "besGetTransaction()  Problem encountered with OPeNDAPCLient. " +
                    "OPeNDAPClient executed " + oc.getCommandCount() + " commands";
            log.error(msg);

            throw new PPTException(msg);
        }
        finally {
            bes.returnClient(oc, besTrouble);
            log.debug("besGetTransaction complete.");

        }

    }


    private static boolean besShowTransaction(String product,
                                           String dataset,
                                           OutputStream os,
                                           OutputStream err)
            throws PPTException, BadConfigurationException {

        log.debug("besShowTransaction started.");

        boolean trouble = false;
        boolean ret;


        String besDataset;

        BES bes = BESManager.getBES(dataset);

        if (bes == null) {
            String msg = "There is no BES to handle the requested data source: " + dataset;
            log.error(msg);
            throw new BadConfigurationException(msg);
        }


        OPeNDAPClient oc = bes.getClient();

        try {

            String cmd = "set context errors to " + XML_ERRORS + ";\n";
            log.debug("Sending command: " + cmd);
            ret = oc.executeCommand(cmd,os,err);
            if(ret){

                cmd = "show " + product;

                if (product.equals("version")) {
                    cmd += ";\n";
                } else {
                    besDataset = bes.trimPrefix(dataset);
                    cmd += " for \"" + besDataset + "\";\n";
                }


                log.debug("Sending command: " + cmd);
                ret = oc.executeCommand(cmd,os,err);

            }

            resetBES(oc);
            return ret;


        }
        catch (PPTException e) {
            trouble = true;
            String msg = "besShowTransaction() Problem with OPeNDAPClient. " +
                    "OPeNDAPClient executed " + oc.getCommandCount() + " commands";

            log.error(msg);
            throw new PPTException(msg);
        }
        finally {
            bes.returnClient(oc, trouble);
            log.debug("besShowTransaction complete.");
        }


    }



    /**
     * Returns an the version document for the BES.
     *
     * @param path The path prefix for the BES whose version is being sought.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @param version  The version document for the BES associated with the
     * parameter path, or if the request results in an error, the document
     * will contain the BESError.
     * @throws BadConfigurationException .
     * @throws PPTException              .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean  showVersion(String path, Document version) throws
            BadConfigurationException,
            PPTException,
            IOException,
            JDOMException{

        return showVersion(path,version,version);
    }




    /**
     * Returns an the version document for the BES.
     *
     * @param path The path prefix for the BES whose version is being sought.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @param version  The version document for the BES associated with the
     * parameter path, or if the request results in an error, the document
     * will not be changed.
     * @param error  If the request results in an error, the error document is
     * placed here.
     * @throws BadConfigurationException .
     * @throws PPTException              .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean  showVersion(String path, Document version, Document error) throws
            BadConfigurationException,
            PPTException,
            IOException,
            JDOMException{

        // Get the version response from the BES (an XML doc)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        Document doc;
        SAXBuilder sb = new SAXBuilder();

        if(besShowTransaction("version", path, baos,erros)){

            log.debug("BES returned this document:\n" +
                    "-----------\n" + baos + "-----------");

            // Parse the XML doc into a Document object.
            doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));


            // Tweak it!

            // First find the response Element
            Element ver = doc.getRootElement().getChild("response");

            // Disconnect it from it's parent and then rename it.
            ver.detach();
            ver.setName("BES-Version");

            version.detachRootElement();
            version.setRootElement(ver);

            return true;

        }
        else {
            // Parse the XML doc into a Document object.
            doc = sb.build(new ByteArrayInputStream(erros.toByteArray()));
            Iterator i  = doc.getDescendants(new ElementFilter(BES_ERROR));

            Element err;
            if(i.hasNext()){
                err = (Element)i.next();
            }
            else {
                err = doc.getRootElement();
            }

            err.detach();
            error.detachRootElement();
            error.setRootElement(err);
            return false;
        }


    }

    /**
     * Returns the BES catalog Document for the specified dataSource, striped
     * of the <response> parent element.
     *
     * @param dataSource The data set or collection for which catalog information
     * is desired.
     * @param catalog  The catalog associated with the datasource name, or if
     * the request results in an error, the error document.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean showCatalog(String dataSource,
                                      Document catalog) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {


        return showCatalog(dataSource,catalog,catalog);



    }
    /**
     * Returns the BES catalog Document for the specified dataSource, striped
     * of the <response> parent element.
     *
     * @param dataSource The data set or collection for which catalog information
     * is desired.
     * @param catalog  The catalog associated with the datasource name, or if
     * the request results in an error, the document will not be changed.
     * @param error  If the request results in an error, the error document is
     * placed here.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean showCatalog(String dataSource,
                                      Document catalog,
                                      Document error) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        Document doc;

        SAXBuilder sb = new SAXBuilder();

        String product = "catalog";

        if(BesAPI.besShowTransaction(product, dataSource, baos,erros)){

            log.debug("BES returned this document:\n" +
                    "-----------\n" + baos + "-----------");

            doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

            // Tweak it!

            // First find the response Element

            Element topDataset =
                    doc.getRootElement().getChild("response").getChild("dataset");

            // Disconnect it from it's parent and then rename it.
            topDataset.detach();
            catalog.detachRootElement();
            catalog.setRootElement(topDataset);

            // Add the appropriate prefix attribute to the top level dataset
            // element in the catalog
            BES bes = BESManager.getBES(dataSource);
            if(bes!=null)
                catalog.getRootElement().setAttribute("prefix",bes.getPrefix());

            return true;

        }
        else {

            doc = sb.build(new ByteArrayInputStream(erros.toByteArray()));

            Iterator i  = doc.getDescendants(new ElementFilter(BES_ERROR));

            Element err;
            if(i.hasNext()){
                err = (Element)i.next();
            }
            else {
                err = doc.getRootElement();
            }

            err.detach();
            error.detachRootElement();
            error.setRootElement(err);
            return false;

        }


    }



    /**
     * Returns the BES INFO document for the spcified dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param info The information document associated with the datasource name,
     * or if the request results in an error, the error document.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean getInfoDocument(String dataSource, Document info) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        return getInfoDocument(dataSource,info,info);
    }

    /**
     * Returns the BES INFO document for the spcified dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param info The information document.
     * @param err The error document, if an error is returned.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean getInfoDocument(String dataSource, Document info, Document err) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        Document doc;

        SAXBuilder sb = new SAXBuilder();

        String product = "info";


        if(BesAPI.besShowTransaction(product, dataSource, baos,erros)){


            log.debug("BES returned this document:\n" +
                    "-----------\n" + baos + "-----------");

            // Parse the XML doc into a Document object.
            doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

            // Prepare the response:

            // First find the response Element

            Element topDataset =
                    doc.getRootElement().getChild("response").getChild("dataset");

            // Disconnect it from it's parent and then rename it.
            topDataset.detach();
            doc.detachRootElement();

            info.setRootElement(topDataset);

            // Add the appropriate prefix attribute.
            BES bes = BESManager.getBES(dataSource);
            if(bes!=null)
                info.getRootElement().setAttribute("prefix",bes.getPrefix());


            return true;
        }
        else {
            byte[] ba = erros.toByteArray();
            String s = new String(ba);
            doc = sb.build(new ByteArrayInputStream(ba));

            Iterator i  = doc.getDescendants(new ElementFilter(BES_ERROR));

            Element error;
            if(i.hasNext()){
                error = (Element)i.next();
            }
            else {
                error = doc.getRootElement();
            }

            error.detach();
            doc.detachRootElement();
            err.setRootElement(error);

            return false;
        }

    }

    public static void resetBES(OPeNDAPClient odc) throws PPTException {

        DevNull devNull =  new DevNull();

        String cmd = "delete definitions;\n";
        odc.executeCommand(cmd,devNull,devNull);

        cmd = "delete containers;\n";
        odc.executeCommand(cmd,devNull,devNull);

    }


    public static boolean isConfigured() {
        return BESManager.isConfigured();
    }


    public static Document getVersionDocument(String path) throws Exception {
        return BESManager.getVersionDocument(path);
    }

    public static Document getCombinedVersionDocument() throws Exception {
        return BESManager.getCombinedVersionDocument();
    }

    public static void configure(OLFSConfig olfsConfig) throws Exception {

        BESManager.configure(olfsConfig.getBESConfig());

    }

}
