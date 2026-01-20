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
import opendap.coreServlet.ByteArrayOutputStreamTransmitCoordinator;
import opendap.coreServlet.TransmitCoordinator;
import opendap.io.HyraxStringEncoding;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ndp
 * Date: Mar 19, 2007
 * Time: 11:39:18 AM
 */
public class BES {

    private Logger log;
    private BESConfig config;

    private ArrayBlockingQueue<OPeNDAPClient> clientQueue;
    private ConcurrentHashMap<String, OPeNDAPClient> clientsMap;
    private ReentrantLock clientCheckoutLock;
    private Semaphore clientCheckOutFlag;
    private ReentrantLock clientsMapLock;
    private int totalClients;

    private ReentrantLock adminLock;

    private AdminInfo administratorInfo;
    private String supportEmail;


    private ReentrantLock versionDocLock;
    private Document serverVersionDocument;

    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;


    public BES(BESConfig config) {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        this.config = config.copy();

        clientQueue = new ArrayBlockingQueue<>(getMaxClients(), true);
        clientsMap = new ConcurrentHashMap<>();
        clientCheckoutLock = new ReentrantLock(true);
        clientCheckOutFlag = new Semaphore(getMaxClients(), true);
        clientsMapLock = new ReentrantLock(true);
        totalClients = 0;

        adminLock = new ReentrantLock(true);
        administratorInfo= null;
        supportEmail = null;
        versionDocLock = new ReentrantLock(true);
        serverVersionDocument = null;

        log.debug("BES built with configuration:\n{}", this.config);

    }

    /**
     * Returns the number of times to retry a command transaction.
     * Set dynamically to the number of client connections plus 2.
     * @return
     */
    int getMaxCommandAttempts(){
        return getBesClientCount() + 2;
    }


    public AdminInfo getAdministratorInfo() throws JDOMException, BESError, PPTException, IOException {
        if(administratorInfo==null){
            adminLock.lock();
            try {
                if(administratorInfo==null) {
                    administratorInfo = retrieveAdminInfo();
                }
            }
            finally {
                adminLock.unlock();
            }
        }
        return new AdminInfo(administratorInfo);
    }

    private AdminInfo retrieveAdminInfo() throws JDOMException, BESError, PPTException, IOException {
        Document showBesKeyCmd = BesApi.getShowBesKeyRequestDocument(BesApi.BES_SERVER_ADMINISTRATOR_KEY);
        Document response = new Document();
        besTransaction(showBesKeyCmd,response);
        Element showBesKey = response.getRootElement().getChild("showBesKey",BES_NS);
        showBesKey.detach();
        Map<String,String> pmap = BesApi.processBesParameterMap(showBesKey);
        return new AdminInfo(pmap);
    }

    public String getSupportEmail(){
        if(supportEmail==null){
            adminLock.lock();
            try {
                if(supportEmail==null) {
                    supportEmail = retrieveSupportEmail();
                }
            }
            finally {
                adminLock.unlock();
            }
        }
        return supportEmail;
    }

    private String retrieveSupportEmail() {
        String emailAddress = BesApi.DEFAULT_SUPPORT_EMAIL_ADDRESS;
        Document showBesKeyCmd = BesApi.getShowBesKeyRequestDocument(BesApi.BES_SUPPORT_EMAIL_KEY);
        Document response = new Document();
        try {
            besTransaction(showBesKeyCmd,response);
            Element rootE = response.getRootElement();
            if(rootE!=null) {
                Element showBesKey = rootE.getChild("showBesKey", BES_NS);
                if (showBesKey != null) {
                    if (log.isDebugEnabled()) {
                        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                        log.debug("BES Support Email Key for \"{}\"\n{}", getPrefix(), xmlo.outputString(showBesKey));
                    }
                    Element value = showBesKey.getChild(BesApi.VALUE, opendap.namespaces.BES.BES_NS);
                    if (value != null) {
                        emailAddress = value.getTextTrim();
                    }
                }
            }
            else {
                log.error("Bes response document did not contain a root element.");
            }
        }
        catch (PPTException | IOException | JDOMException | BESError e) {
            log.error("Failed to get {} from BES. Message: {}", BesApi.BES_SUPPORT_EMAIL_KEY,e.getMessage());
        }
        return emailAddress;
    }




    public int getPort() {
        return config.getPort();
    }

    public String getHost() {
        return config.getHost();
    }

    public int getTimeout() {
        return config.getTimeOut();
    }

    public String getPrefix() {
        return config.getPrefix();
    }

    public String getNickName() {
        return config.getBesName();
    }
    public void setNickName(String name) {
        config.setBesName(name);
    }


    public int getMaxClients() {
        return config.getMaxClients();
    }

    /**
     *
     * @return The maximum size, in bytes, allowed in a response.
     */
    public long getMaxResponseSize() {
        return config.getMaxResponseSize();
    }

    /**
     *
     * @return The maximum size, in bytes, for a variable in a response.
     */
    public long getMaxVariableSize() {
        return config.getMaxResponseSize();
    }


    /*********************************************************************
     * BES LOGGER BEGIN
     */
    public static class BesLogger {

        public BesLogger(String name, boolean enabled) {
            loggerName = name;
            isEnabled = enabled;
        }

        public BesLogger(String name, String enabled) {
            loggerName = name;
            if (enabled != null && enabled.equalsIgnoreCase("on"))
                isEnabled = true;
            else
                isEnabled = false;
        }

        boolean isEnabled;

        public boolean getIsEnabled() {
            return isEnabled;
        }

        public void setIsEnabled(boolean enabled) {
            isEnabled = enabled;
        }

        String loggerName;

        public String getName() {
            return loggerName;
        }

        public void setName(String name) {
            loggerName = name;
        }

    }
    /*
     * BES LOGGER END
     *********************************************************************/


    public int getBesClientCount() {
        return clientsMap.size();
    }

    public Enumeration<OPeNDAPClient> getClients() {
        return clientsMap.elements();
    }

    public String toString() {

        return "[BES prefix: " + getPrefix() +
                " host: " + getHost() +
                " port: " + getPort() +
                " maxClients: " + getMaxClients() +
                " maxClientCommands: " + config.getMaxCommands() +
                "]";


    }


    public Document getVersionDocument() throws BESError, JDOMException, IOException, BadConfigurationException, PPTException {

        // Lock the resource.
        versionDocLock.lock();
        try {
            // Have we cached it already?
            if (serverVersionDocument == null) {
                // Nope? Then Cache it!
                cacheServerVersionDocument();
                if (log.isInfoEnabled()) {
                    XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                    log.info("BES Version Document: \n{}", xmlo.outputString(serverVersionDocument));
                }

            }
        }
        finally {
            // Unlock the resource.
            versionDocLock.unlock();
        }
        // Return a copy so nobody can break our stuff!
        return (Document) serverVersionDocument.clone();

    }




    /**
     * Helper method that adds the bes client identifier to the request.
     * @param request
     * @param oc
     * @throws IOException
     */
    private void addBesClientInfo(Document request, OPeNDAPClient oc) throws IOException {
        Element reqElement = request.getRootElement();
        if(reqElement==null){
            throw new IOException("The BES Request document must have a root element!");
        }
        reqElement.setAttribute(BesApi.BES_CLIENT_ID_KEY ,oc.getID());
        reqElement.setAttribute(BesApi.BES_CLIENT_CMD_COUNT_KEY , Integer.toString(oc.getCommandCount()));
    }

    /**
     * Executes a command/response transaction with the BES and returns the
     * BES response in a JDOM Document. Of course, if the response is NOT
     * parsable as an XML document then some bad things will happen.
     *
     *
     * @param request   The BES request document.
     * @param response  The document into which the BES response will be placed.
     *                 If the passed Document object contains
     * conent, then the content will be discarded.
     * @throws IOException When bad things happen in the talking to the BES.
     * @throws PPTException When bad things happen in the talking to the BES.
     * @throws JDOMException When bad things happen trying to parse XML content
     * received from the BES.
     * @throws BESError When the BES itself returns a BESError document.
     */
    public void  besTransaction(Document request, Document response)
            throws IOException, PPTException, JDOMException, BESError {


        log.debug("BEGIN (Document, Document)");
        SAXBuilder sb = new SAXBuilder();
        Document doc;


        try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream()) {

            ByteArrayOutputStreamTransmitCoordinator baostc = new ByteArrayOutputStreamTransmitCoordinator(responseStream);

            besTransaction(request, responseStream, baostc);
            log.debug("besTransaction() The BES returned this document:\n{}", responseStream);
            if (responseStream.size() != 0) {

                doc = sb.build(new ByteArrayInputStream(responseStream.toByteArray()));

                // Get the root element.
                Element root = doc.getRootElement();

                // Detach it from the document
                root.detach();

                // Pitch the root element that came with the passed catalog.
                // (There may not be one but whatever...)
                response.detachRootElement();

                // Set the root element to be the one sent from the BES.
                response.setRootElement(root);
            }

        }
        log.debug("END (Document, Document)");
    }




    /**
     * Executes a command/response transaction with the BES
     *
     * @param request   The BES request document.
     * @param os   The outputstream to write the BES response to.
     * any error information will be written to the OutputStream err.
     * @throws IOException When bad things happen in the talking to the BES.
     * @throws PPTException When bad things happen in the talking to the BES.
     * @throws BESError When the BES itself returns a BESError document.
     */
    public void besTransaction(Document request, OutputStream os, TransmitCoordinator tc)
            throws IOException, PPTException, BESError {

        log.debug("BEGIN (Document, OutputStream, TransmitCoordinator)");
        int attempts = 0;
        boolean besTrouble;
        PPTException pptException;
        BESError besFatalError;

        do {
            besTrouble = false;
            pptException = null;
            besFatalError = null;
            attempts++;

            OPeNDAPClient oc = null;
            Procedure timedProc=null;

            log.debug("This is attempt: {}", attempts);
            try (ByteArrayOutputStream errorOutputStream = new ByteArrayOutputStream()) {
                oc = getClient();
                if (oc == null) {
                    besTrouble = true;
                    String msg = "FAILED to retrieve a valid OPeNDAPClient instance! " +
                            "BES Prefix: " + getPrefix() + " BES NickName: " + getNickName() + " BES Host: " + getHost();
                    throw new PPTException(msg);
                }

                addBesClientInfo(request, oc);
                if (log.isDebugEnabled()) {
                    log.debug("besTransaction() request document: \n-----------\n{}-----------\n", showRequest(request));
                }
                Logger besCommandLogger = LoggerFactory.getLogger("BesCommandLog");
                if (besCommandLogger.isInfoEnabled()) {
                    besCommandLogger.info("BES COMMAND ({})\n{}\n", new Date(), showRequest(request));
                }

                timedProc= Timer.start();
                boolean result = oc.sendRequest(request, os, errorOutputStream);
                log.debug("besTransaction() - Completed.");
                if (!result) {
                    // We got back an error object from the BES in the baos.
                    // We feed that to the BESError class to build the error object.
                    log.debug("BESError: \n{}", errorOutputStream.toString(HyraxStringEncoding.getCharsetName()));
                    ByteArrayInputStream bais = new ByteArrayInputStream(errorOutputStream.toByteArray());
                    BESError besError = new BESError(bais);

                    log.error("ERROR: BES transaction received a BESError Object. Msg: {}", besError.getMessage());

                    int besErrCode = besError.getBesErrorCode();
                    // If the BES experienced a fatal error then we know we have
                    // to dump the connection to the child besListener.
                    if (besErrCode == BESError.INTERNAL_FATAL_ERROR) {
                        besTrouble = true;
                        besFatalError = besError;
                    }
                    else {
                        // If the error is not fatal then we just throw it and move on.
                        throw besError;
                    }
                }
            }
            catch (PPTException e) {
                besTrouble = true;
                String errmsg = "ERROR: Problem encountered with BES connection. On transaction attempt:" + attempts + " ";
                errmsg += " a PPTException was caught. Message: " + e.getMessage();
                if(oc != null) {
                    errmsg += " (OPeNDAPClient executed " + oc.getCommandCount() + " prior commands.)";
                }
                log.error(errmsg);
                e.setErrorMessage(errmsg);
                pptException = e;
            }
            finally {
                if(oc!=null) returnClient(oc, besTrouble);
                if(timedProc!=null) Timer.stop(timedProc);
            }
        }
        while (besTrouble && attempts < getMaxCommandAttempts() && !tc.isCommitted());

        if (besTrouble) {
            if (besFatalError != null)
                throw besFatalError;

            if (pptException != null)
                throw pptException;
        }
        log.debug("END (Document, OutputStream, TransmitCoordinator)");
    }


    String showRequest(Document request) throws IOException{
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(request);

    }

    private Document getMissingBesResponseErrorDoc(String reqId){


        Element errorResponse = new Element("response",BES_NS);
        Element bes       = new Element("BES",BES_NS);
        Element besError  = new Element("BESError",BES_NS);
        Element type      = new Element("Type",BES_NS);
        Element message   = new Element("Message",BES_NS);
        Element admin     = new Element("Administrator",BES_NS);

        errorResponse.addNamespaceDeclaration(BES_NS);
        errorResponse.setAttribute(BesApi.REQUEST_ID_KEY, reqId);

        type.setText("1");
        message.setText("BES returned an empty error document! That's a bad thing!");
        admin.setText("UNKNOWN Administrator - BES Error response was empty!");

        besError.addContent(type);
        besError.addContent(message);
        besError.addContent(admin);
        bes.addContent(besError);

        errorResponse.addContent(bes);

        return new Document(errorResponse);


    }



    /**
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws JDOMException
     * @throws BESError
     */
    private void cacheServerVersionDocument() throws IOException,
            PPTException,
            BadConfigurationException,
            JDOMException,
            BESError {

        log.debug("Getting Server Version Document.");

        Document version = new Document();

        BesApi besApi = new BesApi();

        besApi.getBesVersion(getPrefix(), version);

        Element root =  version.getRootElement();
        if(root==null)
            throw new IOException("BES version response was emtpy! No root element");

        Element ver = root.getChild("showVersion", BES_NS);
        if(ver==null)
            throw new IOException("BES version response was emtpy! No showVersion element");


        // Disconnect it from it's parent.
        ver.detach();

        ver.setName("BES");
        ver.setAttribute("prefix", getPrefix());
        if(getNickName()!=null)
            ver.setAttribute("name",getNickName());

        version.detachRootElement();
        version.setRootElement(ver);


        serverVersionDocument = version;


    }


    /**
     * Removes the prefix associated with this BES instance from the
     * dataset name and returns the result. For a prefix of "/" this
     * method will return the unchanged dataset name.
     * @param dataset The dataset name from which to remove the prefix
     * @return The dataset nae with the prefix removed.
     */
    public String trimPrefix(String dataset) {
        String trim;
        if (getPrefix().equals("/") || !dataset.startsWith(getPrefix())) {
            trim = dataset;
        }
        else {
            trim = dataset.substring(getPrefix().length());
        }
        return trim;
    }

//------------------------------------------------------------------------------
//-------------------------- CLIENT POOL CODE ----------------------------------
//------------------------------------------------------------------------------


    /**
     * The pool of availableInChunk OPeNDAPClient connections starts empty. When this
     * method is called the pool is checked. If no client is availableInChunk, and the
     * number of clientsMap has not reached the cap, then a new one is made,
     * started, and returned. If no client is availableInChunk and the cap has been
     * reached then this method will BLOCK until a client becomes availableInChunk.
     * If a client is availableInChunk, it is returned.
     *
     * @return The next availableInChunk OPeNDAPClient.
     * @throws opendap.ppt.PPTException  .
     * @throws BadConfigurationException .
     */
    public OPeNDAPClient getClient()
            throws PPTException {

        OPeNDAPClient besClient=null;

        clientCheckoutLock.lock();
        try {

            // Acquiring this semaphore is what limits the number
            // of clientsMap that will be in the pool. The number of
            // semaphores available is set to MaxClients.
            clientCheckOutFlag.acquire();

            log.debug("clientQueue size: '{}'", clientQueue.size());

            if (clientQueue.isEmpty()) {
                besClient = getNewClient();
            } else {

                // Get a client from the client pool.
                besClient = clientQueue.take();
                log.debug("getClient() - Retrieved BES Client (id:{}) from Pool.", besClient.getID());

                // If the bes connection is closed, or the client just is not connected, pitch the client
                // and make a new one, if you can...
                if(besClient.isClosed() || !besClient.isConnected()){
                    log.warn("getClient() - BES Client (id:{}) appears to be dead, discarding...",besClient.getID());
                    discardClient(besClient);
                    besClient = getNewClient();
                }



            }
            return besClient;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Whoops! Thread Interrupted!: {}", e.getMessage());
            if (besClient != null) {
                log.error("Attempting to discard BES Client (id:{}) ", besClient.getID());
                discardClient(besClient);
                clientCheckOutFlag.release(); // Release the client permit because this client is hosed...
            }
            throw new PPTException(e);
        } finally {
            clientCheckoutLock.unlock();
        }


    }

    private OPeNDAPClient getNewClient() throws PPTException, InterruptedException  {

        // Make a new OPeNDAClient to connect to the BES
        OPeNDAPClient besClient = new OPeNDAPClient();

        log.debug("Made new BES Client. (id:{}) Starting Client.",besClient.getID());

        // Start the client by opening the PPT connection to the BES.
        try {
            besClient.startClient(getHost(), getPort(), getTimeout());
            log.debug("BES Client started. (id:{})",besClient.getID());

        }
        catch (PPTException ppte){
            clientCheckOutFlag.release(); // Release the client permit because this client is hosed...
            String msg ="BES Client Failed To Start. Message: '" + ppte.getMessage()+"' ";
            besClient.setID(new Date().toString() + msg);
            log.error(msg);
            throw new PPTException(msg,ppte);
        }


        // Add it to the client pool
        try {
            clientsMapLock.lock();
            String clientId = (getNickName()==null?getPrefix():getNickName());
            if(clientId.isEmpty())
                clientId = "besC";
            clientId += "-" + totalClients;
            besClient.setID(clientId);
            clientsMap.put(clientId, besClient);
            totalClients++;

            log.debug("New BES Client assigned ID: {}", besClient.getID());

        } finally {
            clientsMapLock.unlock();
        }

        return besClient;

    }


    /**
     * When a piece of code is done using an OPeNDAPClient, it should return it
     * to the pool using this method.
     *
     * @param dapClient The OPeNDAPClient to return to the client pool.
     * @param discard   Pitch it, it's broken.
     * @throws PPTException .
     */
    public void returnClient(OPeNDAPClient dapClient, boolean discard) throws PPTException {

        if (dapClient == null)
            return;
        try {

            if (discard) {
                discardClient(dapClient);
            } else {
                checkInClient(dapClient);
            }

        } catch (PPTException e) {
            String msg = "Problem with OPeNDAPClient, discarding.";

            log.error(msg);
            try {
                clientsMapLock.lock();
                clientsMap.remove(dapClient.getID());
            } finally {
                clientsMapLock.unlock();
            }

            throw new PPTException(msg, e);
        } finally {
            clientCheckOutFlag.release();
        }


    }


    private void checkInClient(OPeNDAPClient dapClient) throws PPTException {
        if (
            !dapClient.isOk() ||
            (config.getMaxCommands() > 0 &&
            dapClient.getCommandCount() > config.getMaxCommands())
        ){
            discardClient(dapClient);
            if(log.isDebugEnabled()) {
                String msg = "checkInClient() This instance of OPeNDAPClient (id:" +
                        dapClient.getID() + ") has " +
                        "executed " + dapClient.getCommandCount() +
                        " commands which is in excess of the maximum command " +
                        "limit of " + config.getMaxCommands() + ", discarding client.";
                log.debug(msg);
            }
        }
        else {
            if (clientQueue.offer(dapClient)) {
                log.debug("Returned OPeNDAPClient (id:{}) to Client Pool.", dapClient.getID());
            } else {
                log.error("OUCH! OUCH! OUCH! The Pool is " +
                        "full and I need to check in a client! This Should " +
                        "NEVER Happen!");
            }
        }
    }


    private void discardClient(OPeNDAPClient dapClient) throws PPTException {
        // By failing to put the client into the queue and
        // removing the client from the clientsMap Map the client is
        // discarded.

        if(dapClient != null){
            log.debug("Discarding OPeNDAPClient (id:{})", dapClient.getID());

            try {
                clientsMapLock.lock();
                if (dapClient.getID() != null)
                    clientsMap.remove(dapClient.getID());
            } finally {
                clientsMapLock.unlock();
            }
            if (dapClient.isRunning()) {
                shutdownClient(dapClient);
            }
        }
        else {
            log.error("Received a null valued OPeNDAPClient reference.");
        }
    }


    private void shutdownClient(OPeNDAPClient oc)  {

        try {
            log.debug("Shutting down client...");
            oc.shutdownClient();
            log.debug("Client shutdown.");
        } catch (PPTException e) {
            log.error("Failed to shutdown OPeNDAPClient (id:{}) msg: {}",oc.getID(), e.getMessage());
        }
    }


    /**
     * This method is meant to be called at program exit. It waits until all
     * clientsMap are checked into the pool and then gracefully shuts down each
     * client's connection to the BES.
     */
    public void destroy() {

        boolean nicely = false;
        boolean gotClientCheckoutLock = false;

        try {
            if (clientCheckoutLock.tryLock(10, TimeUnit.SECONDS)) {
                gotClientCheckoutLock = true;

                log.debug("Attempting to acquire all client permits...");


                if (clientCheckOutFlag.tryAcquire(getMaxClients(), 10, TimeUnit.SECONDS)) {
                    log.debug("All {} client permits acquired.",getMaxClients());

                    log.debug("There are {} client(s) to shutdown.", clientQueue.size());


                    int i = 0;
                    while (!clientQueue.isEmpty()) {
                        OPeNDAPClient odc = clientQueue.take();
                        log.debug("Retrieved OPeNDAPClient[{}] (id:{}) from queue.",i++,odc.getID());
                        shutdownClient(odc);
                    }
                    clientCheckOutFlag.release(getMaxClients());
                    nicely = true;
                }


            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OUCH! Interrupted while shutting down BESPool", e);
        } finally {
            if (gotClientCheckoutLock)
                clientCheckoutLock.unlock();
        }


        if (!nicely) {
            log.debug("Timed Out. Destroying BES Clients.");

            try {
                clientsMapLock.lock();
                for (OPeNDAPClient oc: clientsMap.values()) {
                    if (oc != null) {
                        log.debug("Killing BES Client (id:{})", oc.getID());
                        oc.killClient();
                    } else {
                        log.error("Retrieved a 'null' BES Client instance from clientsMap collection!");

                    }

                }
            } finally {
                clientsMapLock.unlock();
            }
        }


    }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------


}
