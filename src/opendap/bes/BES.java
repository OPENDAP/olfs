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

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.OPeNDAPException;
import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;

import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * User: ndp
 * Date: Mar 19, 2007
 * Time: 11:39:18 AM
 */
public class BES {

    private Logger log;


    private ArrayBlockingQueue<OPeNDAPClient> _clientQueue;
    private ConcurrentHashMap<String, OPeNDAPClient> _clients;
    private Semaphore _checkOutFlag;
    private BESConfig _config;
    private int totalClients;
    private ReentrantLock _adminLock;
    private OPeNDAPClient adminClient;


    private Document _serverVersionDocument;
    private ReentrantLock _versionDocLock;
    private ReentrantLock _clientsMapLock;
    private ReentrantLock _clientCheckoutLock;

    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;
    private static final Namespace BES_ADMIN_NS = opendap.namespaces.BES.BES_ADMIN_NS;


    public BES(BESConfig config) throws Exception {
        _config = config.copy();
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        _clientQueue = new ArrayBlockingQueue<OPeNDAPClient>(getMaxClients(), true);
        _clientCheckoutLock = new ReentrantLock(true);

        _checkOutFlag = new Semaphore(getMaxClients(), true);
        totalClients = 0;

        _adminLock = new ReentrantLock(true);
        _versionDocLock = new ReentrantLock(true);
        _clientsMapLock = new ReentrantLock(true);
        _clients = new ConcurrentHashMap<String, OPeNDAPClient>();


        log.debug("BES built with configuration: \n" + _config);
        _serverVersionDocument = null;


    }


    public Vector<BesConfigurationModule> getConfigurationModules() throws BesAdminFail {

        Vector<BesConfigurationModule> configurationModules = new Vector<BesConfigurationModule>();

        String configString = getConfiguration(null);

        ByteArrayInputStream bais = new ByteArrayInputStream(configString.getBytes());

        try {
            Document confDoc = opendap.xml.Util.getDocument(bais);

            Element root = confDoc.getRootElement();

            List moduleConfigs = root.getChildren("BesConfig", BES.BES_ADMIN_NS);

            for (Object o : moduleConfigs) {
                Element moduleConfigElement = (Element) o;

                BesConfigurationModule bm = new BesConfigurationModule(moduleConfigElement);

                configurationModules.add(bm);

            }

        } catch (JDOMException e) {
            log.error("Failed to parse BES response. Msg: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Failed to ingest BES response. Msg: {}", e.getMessage());
        }


        return configurationModules;

    }

    /**
     * Checks to see if it's possible to communicate with the BES.
     * @return  True if communication is with the besdaemon (aka admin port) port. False otherwise.
     *
     */
    public boolean checkBesAdminConnection() {

        boolean besIsOk = true;

        //@todo Make and use a specially designed BES command!
        try {
            getConfiguration(null);
        } catch (BesAdminFail besAdminFail) {
            log.error(besAdminFail.getMessage());
            besIsOk = false;
        }

        return besIsOk;

    }


    public int getAdminPort() {
        return _config.getAdminPort();
    }

    public boolean isAdminPortConfigured() {
        return _config.getAdminPort() > 0;
    }

    public int getPort() {
        return _config.getPort();
    }

    public String getHost() {
        return _config.getHost();
    }

    public String getPrefix() {
        return _config.getPrefix();
    }

    public int getMaxClients() {
        return _config.getMaxClients();
    }

    public int getMaxResponseSize() {
        return _config.getMaxResponseSize();
    }


    public TreeMap<String, BesLogger> getBesLoggers() throws BesAdminFail {
        TreeMap<String, BesLogger> besLoggers = new TreeMap<String, BesLogger>();

        String getLogContextsCmd = getSimpleBesAdminCommand("GetLogContexts");

        String besResponse = executeBesAdminCommand(getLogContextsCmd);
        ByteArrayInputStream bais = new ByteArrayInputStream(besResponse.getBytes());

        SAXBuilder saxBuilder = new SAXBuilder(false);

        try {
            Document loggerContextsDoc = saxBuilder.build(bais);
            List loggers = loggerContextsDoc.getRootElement().getChildren("LogContext", BES_ADMIN_NS);

            Iterator i = loggers.iterator();
            while (i.hasNext()) {
                Element logger = (Element) i.next();
                String name = logger.getAttributeValue("name");
                String state = logger.getAttributeValue("state");
                if (name != null && state != null)
                    besLoggers.put(name, new BesLogger(name, state));
                else
                    log.error("BES responded with unrecognized content structure. Response: {}", besResponse);
            }
        } catch (JDOMException e) {
            log.error("Failed to parse BES response! Msg: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read BES response! Msg: {}", e.getMessage());
        }


        return besLoggers;
    }

    public String getLoggerState(String loggerName) throws BesAdminFail {

        TreeMap<String, BesLogger> besLoggers = getBesLoggers();

        BesLogger logger = besLoggers.get(loggerName);

        if (logger != null && logger.getIsEnabled())
            return "on";

        return "off";

    }

    public String setLoggerState(String loggerName, String loggerState) throws BesAdminFail {

        String setLoggerStateCmd = getSetBesLoggersStateCommand(loggerName, loggerState);

        String besResponse = executeBesAdminCommand(setLoggerStateCmd);
        ByteArrayInputStream bais = new ByteArrayInputStream(besResponse.getBytes());

        SAXBuilder saxBuilder = new SAXBuilder(false);

        String status = besResponse;
        try {
            Document besResponseDoc = saxBuilder.build(bais);
            Element statusElement = besResponseDoc.getRootElement().getChild("OK", BES_ADMIN_NS);
            if (statusElement != null)
                status = new StringBuilder().append("OK: The BES logger '").append(loggerName).append("' has been set to ").append(loggerState).toString();

        } catch (JDOMException e) {
            log.error("Failed to parse BES response! Msg: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read BES response! Msg: {}", e.getMessage());
        }

        return status;

    }

    public String getSetBesLoggersStateCommand(String loggerName, String loggerState) {
        Element docRoot = new Element("BesAdminCmd", BES_ADMIN_NS);
        Element cmd = new Element("SetLogContext", BES_ADMIN_NS);

        cmd.setAttribute("name", loggerName);
        cmd.setAttribute("state", loggerState);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
    }

    public class BesLogger {

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


    public int getBesClientCount() {
        return _clients.size();
    }

    public Enumeration<OPeNDAPClient> getClients() {
        return _clients.elements();
    }

    public String toString() {

        return "[BES prefix: " + getPrefix() +
                " host: " + getHost() +
                " port: " + getPort() +
                " maxClients: " + getMaxClients() +
                " maxClientCommands: " + _config.getMaxCommands() +
                "]";


    }


    public String executeBesAdminCommand(String besCmd) throws BesAdminFail {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();


        if (!isAdminPortConfigured()) {
            sb.append("BES Admin Service is not configured! Port number for admin connection has not been set!");
            return sb.toString();
        }


        OPeNDAPClient admin = null;

        try {
            _adminLock.lock();

            log.debug("Getting new admin client...");

            admin = new OPeNDAPClient();
            log.debug("Starting new admin client. Host: {} Port: {}", getHost(), getAdminPort());

            admin.startClient(getHost(), getAdminPort());
            log.debug("BES admin client started, sending command:\n{}", besCmd);


            admin.executeCommand(besCmd, baos, baos);

            log.debug("BES returned:\n{}", baos.toString());

            return baos.toString();

        } catch (PPTException e) {

            sb.append("Failed to execute BES command. Message: ")
                    .append(e.getMessage());


            throw new BesAdminFail("Failed to execute BES command. Message: " + e.getMessage(), e);


        } finally {
            if (admin != null) {
                try {
                    admin.shutdownClient();
                } catch (PPTException e) {
                    sb.append("FAILED TO SHUTDOWN CLIENT! Msg: ").append(e.getMessage());
                    admin.killClient();
                }
            }
            _adminLock.unlock();
        }

    }

    public String start() throws BesAdminFail {
        String cmd = getStartCommand();
        return executeBesAdminCommand(cmd);
    }

    public String stopNow() throws BesAdminFail {
        String cmd = getStopNowCommand();
        return executeBesAdminCommand(cmd);
    }

    public String getStartCommand() {
        return getSimpleBesAdminCommand("Start");
    }

    public String getStopNowCommand() {
        return getSimpleBesAdminCommand("StopNow");
    }

    public String stopNice(long timeOut) throws BesAdminFail {
        StringBuilder sb = new StringBuilder();


        long stopNiceMinTimeOut = 1000;
        if (timeOut < stopNiceMinTimeOut)
            timeOut = stopNiceMinTimeOut;

        long stopNiceMaxTimeOut = 30000;
        if (timeOut > stopNiceMaxTimeOut)
            timeOut = stopNiceMaxTimeOut;

        String besResponse = null;


        String msg = "Attempting to acquire client checkOut lock...";
        log.info(msg);
        sb.append(msg).append("\n");
        _clientCheckoutLock.lock();
        try {

            Date startTime = new Date();

            boolean done = false;
            msg = "Attempting to acquire all BES clients...";
            log.info(msg);
            sb.append(msg).append("\n");

            while (!done) {

                Collection<OPeNDAPClient> clients = _clients.values();

                boolean allClientsAcquired = true;
                for (OPeNDAPClient client : clients) {
                    boolean inQue = _clientQueue.remove(client);
                    if (!inQue) {
                        allClientsAcquired = false;
                    } else {
                        msg = "Shutting down client connection '" + client.getID() + "'...";
                        log.info(msg);
                        sb.append(msg).append("\n");

                        try {

                            discardClient(client);
                            //client.shutdownClient();
                            msg = "Client connection '" + client.getID() + "'shutdown normally";
                            log.info(msg);
                            sb.append(msg).append("\n");

                        } catch (PPTException e) {
                            msg = "Shutdown FAILED for client connection '" + client.getID() + "'Trying to kill connection.";
                            log.info(msg);
                            sb.append(msg).append("\n");

                            client.killClient();

                            msg = "Killed client connection '" + client.getID() + "'.";
                            log.info(msg);
                            sb.append(msg).append("\n");


                        }
                        msg = "Removing client connection '" + client.getID() + "' from clients list.";
                        log.info(msg);
                        sb.append(msg).append("\n");
                        _clients.remove(client.getID());
                    }
                }

                if (!allClientsAcquired) {
                    Date endTime = new Date();

                    long elapsedTime = endTime.getTime() - startTime.getTime();

                    if (elapsedTime > timeOut) {
                        done = true;
                        msg = "Timeout Has Expired. Shutting down BES NOW...";
                        log.info(msg);
                        sb.append(msg).append("\n");
                    } else {
                        msg = "Did not acquire all clients. Sleeping...";
                        log.info(msg);
                        sb.append(msg).append("\n");
                        Thread.sleep(timeOut / 3);
                    }


                } else {
                    done = true;


                    msg = "Stopped all BES client connections.";
                    log.info(msg);
                    sb.append(msg).append("\n");

                }


            }

            msg = "Stopping BES...";
            log.info(msg);
            sb.append(msg).append("\n");
            besResponse = stopNow();
            //log.info(besResponse);


        } catch (InterruptedException e) {

            sb.append(e.getMessage());

        } finally {
            msg = "Releasing client checkout lock...";
            log.info(msg);
            sb.append(msg).append("\n");
            _clientCheckoutLock.unlock();
        }


        return besResponse;
    }


    public String getConfiguration(String moduleName) throws BesAdminFail {
        String cmd = getGetConfigurationCommand(moduleName);
        return executeBesAdminCommand(cmd);
    }

    public String setConfiguration(String moduleName, String configuration) throws BesAdminFail {
        String cmd = getSetConfigurationCommand(moduleName, configuration);
        return executeBesAdminCommand(cmd);
    }

    public String getGetConfigurationCommand(String moduleName) {
        Element docRoot = new Element("BesAdminCmd", BES_ADMIN_NS);
        Element cmd = new Element("GetConfig", BES_ADMIN_NS);

        if (moduleName != null)
            cmd.setAttribute("module", moduleName);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
    }

    public String getSetConfigurationCommand(String moduleName, String configuration) {
        Element docRoot = new Element("BesAdminCmd", BES_ADMIN_NS);
        Element cmd = new Element("SetConfig", BES_ADMIN_NS);

        if (moduleName != null)
            cmd.setAttribute("module", moduleName);

        cmd.setText(configuration);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
    }


    public String getLog(String lines) throws BesAdminFail {
        String cmd = getGetLogCommand(lines);
        return executeBesAdminCommand(cmd);
    }

    public String getGetLogCommand(String lines) {
        Element docRoot = new Element("BesAdminCmd", BES_ADMIN_NS);
        Element cmd = new Element("TailLog", BES_ADMIN_NS);

        if (lines != null)
            cmd.setAttribute("lines", lines);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
    }


    public String getSimpleBesAdminCommand(String besCmd) {

        Element docRoot = new Element("BesAdminCmd", BES_ADMIN_NS);
        Element cmd = new Element(besCmd, BES_ADMIN_NS);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);

    }


    public Document getVersionDocument() throws Exception {

        // Have we cached it already?
        if (_serverVersionDocument == null) {

            // Apparently not, so lets lock the resource.
            _versionDocLock.lock();

            try {
                // Make sure someone didn't change it when we weren't looking.
                if (_serverVersionDocument == null) {
                    // Cache it!
                    cacheServerVersionDocument();
                    XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat() );
                    log.info("BES Version Document: \n" + xmlo.outputString(_serverVersionDocument));
                }
            } finally {
                // Unlock the resource.
                _versionDocLock.unlock();
            }
        }
        // Return a copy so nobody can break our stuff!
        if (_serverVersionDocument != null) {
            return (Document) _serverVersionDocument.clone();
        }

        return null;
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

        if (besApi.getVersion(getPrefix(), version)) {


            Element ver = version.getRootElement().getChild("showVersion", BES_NS);


            // Disconnect it from it's parent.
            ver.detach();

            ver.setName("BES");
            ver.setAttribute("prefix", getPrefix());

            version.detachRootElement();
            version.setRootElement(ver);


            _serverVersionDocument = version;
        } else {

            BESError besError = new BESError(version);
            log.error(besError.getErrorMessage());
            throw besError;

        }


    }


    public String trimPrefix(String dataset) {

        String trim;
        if (getPrefix().equals("/"))
            trim = dataset;
        else
            trim = dataset.substring(getPrefix().length());

        //if (trim.indexOf("/") != 0)
        //    trim = "/" + trim;


        return trim;


    }

//------------------------------------------------------------------------------
//-------------------------- CLIENT POOL CODE ----------------------------------
//------------------------------------------------------------------------------


    /**
     * The pool of availableInChunk OPeNDAPClient connections starts empty. When this
     * method is called the pool is checked. If no client is availableInChunk, and the
     * number of clients has not reached the cap, then a new one is made,
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

        OPeNDAPClient besClient = null;
        String clientId;

        if (_checkOutFlag == null)
            return null;

        try {
            _clientCheckoutLock.lock();

            // Aquiring this semaphore is what limits the number
            // of clients that will be in the pool. The number of
            // semaphores available is set to MaxClients.
            _checkOutFlag.acquire();

            log.debug("_clientQueue size: '{}'",_clientQueue.size());

            if (_clientQueue.size() == 0) {


                // Make a new OPeNDAClient to connect to the BES
                besClient = new OPeNDAPClient();
                log.debug("getClient() - Made new BES Client. Starting...");



                // Start the client by opening the PPT connection to the BES.
                try {
                    besClient.startClient(getHost(), getPort());
                    log.debug("getClient() - BES Client started.");

                }
                catch (PPTException ppte){

                    _checkOutFlag.release(); // Release the client permit because this client is hosed...

                    StringBuilder msg = new StringBuilder().append("BES Client Failed To Start.");
                    msg.append(" msg: '").append(ppte.getMessage()).append("'");
                    besClient.setID(new Date().toString() + msg);
                    msg.insert(0, "getClient() - ");
                    log.error(msg.toString());
                    throw new PPTException(msg.toString(),ppte);
                }


                // Add it to the client pool
                try {
                    _clientsMapLock.lock();
                    clientId = "besC-" + totalClients;
                    besClient.setID(clientId);
                    _clients.put(clientId, besClient);
                    totalClients++;

                    log.debug("getClient() - BES Client assigned ID : " + besClient.getID());

                } finally {
                    _clientsMapLock.unlock();
                }





            } else {

                // Get a client from the client pool.
                besClient = _clientQueue.take();
                log.debug("getClient() - Retrieved " +
                        "BES Client (id: " + besClient.getID() + ") from Pool.");
            }


            return besClient;

        } catch (InterruptedException e) {
            log.error("getClient() - Interrupted!: " + e.getMessage());
            if (besClient != null) {
                log.error("getClient() - Attempting to discard BES Client id: " + besClient.getID());
                discardClient(besClient);
                _checkOutFlag.release(); // Release the client permit because this client is hosed...
            }
            throw new PPTException(e);
        } finally {
            _clientCheckoutLock.unlock();
        }


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

        //log.debug("returnClient() clientID="+dapClient.getID()+"  discard="+discard);


        try {

            if (discard) {
                discardClient(dapClient);
            } else {
                checkInClient(dapClient);
            }

        } catch (PPTException e) {


            String msg = "returnClient() *** BES - WARNING! Problem with " +
                    "OPeNDAPClient, discarding.";

            log.error(msg);
            try {
                _clientsMapLock.lock();
                _clients.remove(dapClient.getID());
            } finally {
                _clientsMapLock.unlock();
            }

            throw new PPTException(msg, e);
        } finally {
            _checkOutFlag.release();
        }


    }


    private void checkInClient(OPeNDAPClient dapClient) throws PPTException {


        if (_config.getMaxCommands() > 0 && dapClient.getCommandCount() > _config.getMaxCommands()) {
            discardClient(dapClient);
            log.debug("checkInClient() This instance of OPeNDAPClient (id:" +
                    dapClient.getID() + ") has " +
                    "excecuted " + dapClient.getCommandCount() +
                    " commands which is in excess of the maximum command " +
                    "limit of " + _config.getMaxCommands() + ", discarding client.");

        } else {

            if (_clientQueue.offer(dapClient)) {
                log.debug("checkInClient() Returned OPeNDAPClient (id:" +
                        dapClient.getID() + ") to Pool.");
            } else {
                log.error("checkInClient(): OUCH! OUCH! OUCH! The Pool is " +
                        "full and I need to check in a client! This Should " +
                        "NEVER Happen!");
            }

        }


    }


    private void discardClient(OPeNDAPClient dapClient) throws PPTException {
        // By failing to put the client into the queue and
        // removing the client from the _clients Map the client is
        // discarded.

        log.debug("discardClient() Discarding OPeNDAPClient #" + dapClient.getID());

        try {
            _clientsMapLock.lock();
            if (dapClient != null && dapClient.getID() != null)
                _clients.remove(dapClient.getID());
        } finally {
            _clientsMapLock.unlock();
        }


        if (dapClient.isRunning()) {
            shutdownClient(dapClient);
        }

    }


    private void shutdownClient(OPeNDAPClient oc) throws PPTException {

        log.debug("shutdownClient() Shutting down client...");
        oc.shutdownClient();
        log.debug("shutdownClient() Client shutdown.");

    }


    /**
     * This method is meant to be called at program exit. It waits until all
     * clients are checked into the pool and then gracefully shuts down each
     * client's connection to the BES.
     */
    public void destroy() {

        boolean nicely = false;
        boolean gotClientCheckoutLock = false;

        try {
            if (_clientCheckoutLock.tryLock(10, TimeUnit.SECONDS)) {
                gotClientCheckoutLock = true;

                log.debug("destroy() Attempting to acquire all clients...");


                if (_checkOutFlag.tryAcquire(getMaxClients(), 10, TimeUnit.SECONDS)) {
                    log.debug("destroy() All "+getMaxClients()+" client permits acquired.");

                    log.debug("destroy() " + _clientQueue.size() +
                            " client(s) to shutdown.");


                    int i = 0;
                    while (_clientQueue.size() > 0) {
                        OPeNDAPClient odc = _clientQueue.take();
                        log.debug("destroy() Retrieved OPeNDAPClient["
                                + i++ + "] (id:" + odc.getID() + ") from queue.");

                        try {
                            shutdownClient(odc);
                        } catch (Throwable t) {
                            log.error("destroy() Failed to shutdown " +
                                    "OPeNDAPClient (id:" + odc.getID() + ") msg: " +
                                    t.getMessage(), t);
                        }


                    }
                    _checkOutFlag.release(getMaxClients());
                    nicely = true;
                }


            }
        } catch (Throwable e) {
            log.error("destroy() OUCH! Problem shutting down BESPool", e);
        } finally {
            //_checkOutFlag = null;
            if (gotClientCheckoutLock)
                _clientCheckoutLock.unlock();
        }


        if (!nicely) {
            log.debug("destroy() Timed Out. Destroying BES Clients.");

            try {
                _clientsMapLock.lock();
                for (int i = 0; i < totalClients; i++) {
                    OPeNDAPClient oc = _clients.get(i);
                    if (oc != null) {
                        log.debug("destroy() Killing BES Client (id:" +
                                oc.getID() + ")");
                        oc.killClient();
                    } else {
                        log.debug("destroy() BES Client (id:" +
                                i + ")already discarded.");

                    }

                }
            } finally {
                _clientsMapLock.unlock();
            }
        }


    }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------


}
