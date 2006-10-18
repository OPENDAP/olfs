/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
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


package opendap.olfs;

import opendap.coreServlet.Debug;

import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;

import java.io.*;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;


/**
 * BES transaction code and client pool management are contained  in this class.
 * All of the methods are static, but the class is not stateless, as it maintains an intenral pool
 * of client connections to the BES. Since all of the BES transaction code for the OLFS is
 * wrapped in this class, future optimizations should be easier... (right?)
 */
public class BesAPI {

    private static int _besPort = -1;
    private static String _besHost = "Not Configured!";
    private static boolean _configured = false;
    private static final Object syncLock = new Object();

    private static ArrayBlockingQueue<OPeNDAPClient> _clientQueue;
    private static Semaphore _checkOutFlag;
    private static int _maxClients;

    private static boolean useClientPool =  true;

    /**
     * The name of the BES Exception Element.
     */
    private static String BES_EXCEPTION  = "BESException";







//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------- CLIENT POOL CODE -----------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------







    /**
     * The pool of available OPeNDAPClient connections starts empty. When this method is called the pool is checked.
     * If no client is available, and the number of clients has not reached the cap, then a new one is made, started,
     * and returned. If no client is available and the cap has been reached then this method will BLOCK until a
     * client becomes available. If a client is available, it is returned.
     * @return The next available OPeNDAPClient.
     * @throws PPTException
     * @throws BadConfigurationException
     */
    private static OPeNDAPClient getClient() throws PPTException, BadConfigurationException {

        OPeNDAPClient odc;

        try {
            _checkOutFlag.acquire();

            if(_clientQueue.size()==0){
                odc = new OPeNDAPClient();
                odc.startClient(getHost(), getPort());
                if (Debug.isSet("BES")) System.out.println("BesAPI - Made new OPeNDAPClient.");


            }
            else {

                odc = _clientQueue.take();
                if (Debug.isSet("BES")) System.out.println("BesAPI - Retrieved OPeNDAPClient from queue.");
            }

            if (Debug.isSet("BES"))
                odc.setOutput(System.out, true);
            else {
                DevNull devNull = new DevNull();
                odc.setOutput(devNull, true);
            }

            return odc;
        }
        catch (InterruptedException e){
           return null;
        }

    }

    /**
     * When a piece of code is done using an OPeNDAPClient, it should return it to the pool using this method.
     *
     *
     *
     * @param odc The OPeNDAPClient to return to the client pool.
     * @throws PPTException
     */
    private static void returnClient(OPeNDAPClient odc) throws PPTException {

        try {



            String cmd = "delete definitions;\n";
            odc.executeCommand(cmd);

            cmd = "delete containers;\n";
            odc.executeCommand(cmd);


            odc.setOutput(null, false);

            _clientQueue.put(odc);
            _checkOutFlag.release();
            if (Debug.isSet("BES")) System.out.println("BesAPI - Returned OPeNDAPClient to queue.");
        }
        catch (InterruptedException e){
            e.printStackTrace(); // Don't do a thing
        }

    }





    /**
     * This method is meant to be called at program exit. It waits until all clients are checked into the pool and then
     * gracefully shuts down each client's connection to the BES.
     */
    public static void shutdownBES(){


        try {
            _checkOutFlag.acquireUninterruptibly(_maxClients);

            int i = 0;
            while (_clientQueue.size() > 0) {


                OPeNDAPClient odc = _clientQueue.take();
                if (Debug.isSet("BES")) System.out.println("BesAPI - Retrieved OPeNDAPClient["+ i++ +"] from queue.");

                shutdownClient(odc);


            }

        } catch (InterruptedException e) {
            e.printStackTrace(); // Do nothing
        } catch (PPTException e) {
            e.printStackTrace();  // Do nothing..
        }


    }

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------


    /**
     *  Configures the BES. The BES may configured ONCE. Subsequent calls to configure() will be ignored.
     *
     * @param host The host name/ip of the BES
     * @param port The port on which the BES is listening
     * @param maxClients The maximum number of concurrent client connections that will be allowed to the BES.
     * @return False if configure() mas been called previously, True otherwise.
     */
    public static boolean configure(String host, int port, int maxClients) {

        synchronized (syncLock) {

            if (isConfigured())
                return false;

            _besHost = host;
            _besPort = port;
            _maxClients = maxClients;

            _clientQueue = new ArrayBlockingQueue<OPeNDAPClient>(_maxClients);
            _checkOutFlag = new Semaphore(_maxClients);

            _configured = true;


            System.out.println("BES is configured - Host: " + _besHost + "   Port: " + _besPort);

        }

        return true;

    }


    /**
     * Configures the BES using the passed BESConfig object. The BES may configured ONCE.
     * Subsequent calls to configure() will be ignored.
     * @param bc
     * @return False if configure() mas been called previously, True otherwise.
     */
    public static boolean configure(BESConfig bc) {

        synchronized (syncLock) {

            if (isConfigured())
                return false;

            _besHost = bc.getBESHost();
            _besPort = bc.getBESPort();

            _maxClients = bc.getBESMaxClients();

            _clientQueue = new ArrayBlockingQueue<OPeNDAPClient>(_maxClients);
            _checkOutFlag = new Semaphore(_maxClients);

            _configured = true;


            System.out.println("BES is configured - Host: " + _besHost + "   Port: " + _besPort);
        }

        return true;

    }

    /**
     *
     * @return True is the BES had been configured. False otherwise.
     */
    public static boolean isConfigured() {
        return _configured;
    }


    /**
     *
     * @return The host/ip of the BES.
     * @throws BadConfigurationException If the BES has not been configured prior to calling this method.
     */
    public static String getHost() throws BadConfigurationException {
        if (!isConfigured())
            throw new BadConfigurationException("BES must be configured before use!\n");

        return _besHost;
    }

    /**
     *
     * @return The port number of the BES.
     * @throws BadConfigurationException If the BES has not been configured prior to calling this method.
     */
    public static int getPort() throws BadConfigurationException {
        if (!isConfigured())
            throw new BadConfigurationException("BES must be configured before use!\n");
        return _besPort;
    }


    /**
     *  Writes an OPeNDAP DDX for the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeDDX(String dataSource,
                                String constraintExpression,
                                OutputStream os)
            throws BadConfigurationException, PPTException {

        besGetTransaction(getAPINameForDDX(), dataSource, constraintExpression, os);
    }


    public static Document getDDXDocument(String dataset,
                                          String constraintExpression)
            throws PPTException, BadConfigurationException, IOException, JDOMException, BESException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BesAPI.writeDDX(dataset, constraintExpression, os);
        SAXBuilder sb = new SAXBuilder();


        if(Debug.isSet("BES")) System.out.println("getDDXDocument got this array:\n"+os.toString());

        Document ddx = sb.build(new ByteArrayInputStream(os.toByteArray()));

        // Check for an exception:
        besExceptionHandler(ddx);


        return ddx ;
    }



    /**
     *  Writes an OPeNDAP DDS for the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeDDS(String dataSource,
                                String constraintExpression,
                                OutputStream os)
            throws BadConfigurationException, PPTException {

        besGetTransaction(getAPINameForDDS(), dataSource, constraintExpression, os);
    }


    /**
     *  Writes the source data (it is often a file, thus the method name) to the passed stream.
     * @param dataSource The requested DataSource
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeFile(String dataSource,
                                OutputStream os)
            throws BadConfigurationException, PPTException {

        OPeNDAPClient oc;
        if(useClientPool)
            oc = getClient();
        else
            oc = startClient();

        configureTransaction(oc, dataSource, null, "stream");

        getDataProduct(oc, getAPINameForStream(), os);

        if(useClientPool)
            returnClient(oc);
        else
            shutdownClient(oc);
    }


    /**
     *  Writes an OPeNDAP DAS for the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeDAS(String dataSource,
                                String constraintExpression,
                                OutputStream os)
            throws BadConfigurationException, PPTException {

        besGetTransaction(getAPINameForDAS(), dataSource, constraintExpression, os);
    }

    /**
     *  Writes an OPeNDAP DAP2 data response for the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeDap2Data(String dataSource,
                                     String constraintExpression,
                                     OutputStream os)
            throws BadConfigurationException, PPTException {

        besGetTransaction(getAPINameForDODS(), dataSource, constraintExpression, os);
    }


    /**
     *  Writes the ASCII representation os the  OPeNDAP data response for the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeASCII(String dataSource,
                                    String constraintExpression,
                                    OutputStream os)
            throws BadConfigurationException, PPTException {

        besGetTransaction(getAPINameForASCII(), dataSource, constraintExpression, os);
    }


    /**
     *  Writes the HTML data request form (aka the I.F.H.) for the OPeNDAP the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param url The URL to refernece in the HTML form.
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeHTMLForm(String dataSource, String url, OutputStream os)
            throws BadConfigurationException, PPTException {


        OPeNDAPClient oc;
        if(useClientPool)
            oc = getClient();
        else
            oc = startClient();

        configureTransaction(oc, dataSource, null);

        String cmd = "get "+ getAPINameForHTMLForm()+ " html_form for d1 using "+url+";\n";

        if (Debug.isSet("BES")) System.err.print("Sending command: " + cmd);

        oc.setOutput(os, false);
        oc.executeCommand(cmd);


        if(useClientPool)
            returnClient(oc);
        else
            shutdownClient(oc);

    }




    /**
     *  Writes the OPeNDAP INFO response for the dataSource to the passed stream.
     * @param dataSource The requested DataSource
     * @param os The Strem to which to write the response.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static void writeINFOPage(String dataSource,
                                    OutputStream os)
            throws BadConfigurationException, PPTException {

        besGetTransaction(getAPINameForINFOPage(), dataSource, null, os);
    }




    /**
     *  Returns an InputStream that holds an OPeNDAP DAP2 data for the requested dataSource. The DDS header
     * is stripped, so the InputStream holds ONLY the XDR encoded binary data.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static InputStream getDap2DataStream(String dataSource,
                                    String constraintExpression)
            throws BadConfigurationException, PPTException, IOException {
        //@todo Make this more efficient by adding support to the BES that reurns this stream. Caching the resposnse in memory is a BAD BAD thing.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeDap2Data(dataSource, constraintExpression, baos);

        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        HeaderInputStream his  = new HeaderInputStream(is);

        boolean done = false;
        int val;
        while(!done){
            val = his.read();
            if(val==-1)
                done = true;

        }

        return is;

    }


    /**
     *  Returns an InputStream that holds an OPeNDAP DDX for the requested dataSource.
     * @param dataSource The requested DataSource
     * @param constraintExpression
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static InputStream getDDXStream(String dataSource,
                                    String constraintExpression)
            throws BadConfigurationException, PPTException {
        //@todo Make this more efficient by adding support to the BES that reurns this stream. Caching the resposnse in memory is a BAD BAD thing.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        besGetTransaction(getAPINameForDDX(), dataSource, constraintExpression, baos);

       return new ByteArrayInputStream(baos.toByteArray());


    }





    /**
     *  Returns an the version document for the BES.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static Document showVersion()
            throws BadConfigurationException, PPTException, IOException, JDOMException, BESException {

        // Get the version response from the BES (an XML doc)
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        besShowTransaction("version", os);

        if (Debug.isSet("BES")) System.out.println(os);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(os.toByteArray()));

        // Check for an exception:
        besExceptionHandler(doc);


        // Tweak it!

        // First find the response Element
        Element ver = doc.getRootElement().getChild("response");

        // Disconnect it from it's parent and then rename it.
        ver.detach();
        ver.setName("OPeNDAP-Version");

        doc.detachRootElement();
        doc.setRootElement(ver);

        return doc;
    }


    /**
     * Returns the BES INFO document for the spcified dataSource.
     * @param dataSource
     * @return The BES info document, stripped of it's <response> parent.
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws IOException
     * @throws JDOMException
     * @throws BESException
     */
    public static Document showInfo(String dataSource)
            throws PPTException, BadConfigurationException, IOException, JDOMException, BESException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = "info for " + "\"" + dataSource + "\"";

        if (Debug.isSet("BES")) System.out.println("BESCrawlableDataset sending BES cmd: show " + product);
        BesAPI.besShowTransaction(product, baos);


        if (Debug.isSet("BES")) System.out.println("BES returned:\n" + baos);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

        // Check for an exception:
        besExceptionHandler(doc);

        // Prepare the response:

        // First find the response Element

        Element topDataset = doc.getRootElement().getChild("response").getChild("dataset");

        // Disconnect it from it's parent and then rename it.
        topDataset.detach();
        doc.detachRootElement();
        doc.setRootElement(topDataset);

        return doc;

    }


    /**
     * Look for and process an Exception in the response from the BES.
     * @param doc
     * @throws BESException
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

    private static String makeBesExceptionMsg(Element exception, int number){
        String msg  = "";

        msg += "[";
        msg += "[BESException: " + number + "]";
        msg += "[Type: " + exception.getChild("Type").getTextTrim() + "]";
        msg += "[Message: " + exception.getChild("Message").getTextTrim() + "]";
        msg += "[Location: ";
        msg += exception.getChild("Location").getChild("File").getTextTrim() + " line ";
        msg += exception.getChild("Location").getChild("Line").getTextTrim() + "]";
        msg += "]";


        System.out.println("BES Exception Message: "+msg);

        return msg;
    }


    /**
     * Returns the BES catalog Document for the specified dataSource, striped of the <response> parent element.
     * @param dataSource
     * @return The BES catalog Document.
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws IOException
     * @throws JDOMException
     * @throws BESException
     */
    public static Document showCatalog(String dataSource)
            throws PPTException, BadConfigurationException, IOException, JDOMException, BESException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = "catalog for " + "\"" + dataSource + "\"";

        BesAPI.besShowTransaction(product, baos);

        if (Debug.isSet("BES")) System.out.println("BES returned:\n" + baos);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

        // Check for an exception:
        besExceptionHandler(doc);
        // Tweak it!

        // First find the response Element

        Element topDataset = doc.getRootElement().getChild("response").getChild("dataset");

        // Disconnect it from it's parent and then rename it.
        topDataset.detach();
        doc.detachRootElement();
        doc.setRootElement(topDataset);

        return doc;

    }

    /**
     * Creates a new OPeNDAPClient connectio to the BES and starts up the connection.
     * @return A new OPeNDAP Client.
     * @throws BadConfigurationException
     * @throws PPTException
     */
    public static OPeNDAPClient startClient()
            throws BadConfigurationException, PPTException {


        OPeNDAPClient oc = new OPeNDAPClient();
        oc.startClient(getHost(), getPort());


        if (Debug.isSet("BES"))
            System.out.println("Got OPeNDAPClient. BES - Host: " + _besHost + "  Port:" + _besPort);


        if (Debug.isSet("BES"))
            oc.setOutput(System.out, true);
        else {
            DevNull devNull = new DevNull();
            oc.setOutput(devNull, true);
        }


        return oc;
    }




    private static void configureTransaction(OPeNDAPClient oc, String dataset, String constraintExpression)
            throws PPTException {
        configureTransaction(oc, dataset, constraintExpression, null);
    }


    private static void configureTransaction(OPeNDAPClient oc, String dataset, String constraintExpression, String type)
            throws PPTException {


        String cmd = "set container in catalog values " + dataset + ", " + dataset + (type==null?"":", "+type)+ ";\n";
        if (Debug.isSet("BES")) System.out.print("Sending BES command: " + cmd);
        oc.executeCommand(cmd);


        if (Debug.isSet("BES")) System.out.println("ConstraintExpression: " + constraintExpression);


        if (constraintExpression == null || constraintExpression.equalsIgnoreCase("")) {
            cmd = "define d1 as " + dataset + ";\n";
        } else {
            cmd = "define d1 as " + dataset + " with " + dataset + ".constraint=\"" + constraintExpression + "\"  ;\n";

        }

        if (Debug.isSet("BES")) System.out.print("Sending BES command: " + cmd);
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
        if (Debug.isSet("BES")) System.err.print("Sending command: " + cmd);

        oc.setOutput(os, false);
        oc.executeCommand(cmd);

    }






    private static void shutdownClient(OPeNDAPClient oc) throws PPTException {
        if (Debug.isSet("BES")) System.out.print("Shutting down client...");

        oc.setOutput(null, false);

        oc.shutdownClient();
        if (Debug.isSet("BES")) System.out.println("Done.");


    }

    private static void besGetTransaction(String product,
                                          String dataset, String constraintExpression,
                                          OutputStream os)
            throws BadConfigurationException, PPTException {

        if (Debug.isSet("BES")) System.out.println("Entered besGetTransaction().");


        OPeNDAPClient oc;

        if(useClientPool)
            oc = getClient();
        else
            oc = startClient();



        configureTransaction(oc, dataset, constraintExpression);

        getDataProduct(oc, product, os);



        if(useClientPool)
            returnClient(oc);
        else
            shutdownClient(oc);


    }


    private static void besShowTransaction(String product, OutputStream os)
            throws PPTException, BadConfigurationException {


        OPeNDAPClient oc;
        if(useClientPool){
            oc = getClient();
        }
        else {
            oc = new OPeNDAPClient();

            //System.out.println("BES - Host: "+_besHost+"  Port:"+_besPort);

            oc.startClient(getHost(), getPort());

            if (Debug.isSet("BES"))
                oc.setOutput(System.out, true);
            else {
                DevNull devNull = new DevNull();
                oc.setOutput(devNull, true);
            }

        }



        String cmd = "show " + product + ";\n";
        if (Debug.isSet("BES")) System.err.print("Sending command: " + cmd);
        oc.setOutput(os, false);
        oc.executeCommand(cmd);



        if(useClientPool){
            returnClient(oc);
        }
        else {
            if (Debug.isSet("BES")) System.out.print("Shutting down client...");
            oc.setOutput(null, false);
            oc.shutdownClient();
        }



        if (Debug.isSet("BES")) System.out.println("Done.");

    }



















}
