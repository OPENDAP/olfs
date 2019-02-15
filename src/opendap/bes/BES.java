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


    private ArrayBlockingQueue<OPeNDAPClient> _clientQueue;
    private ConcurrentHashMap<String, OPeNDAPClient> _clients;
    private Semaphore _checkOutFlag;
    private BESConfig _config;
    private int totalClients;
    private ReentrantLock _adminLock;


    private Document _serverVersionDocument;
    private ReentrantLock _versionDocLock;
    private ReentrantLock _clientsMapLock;
    private ReentrantLock _clientCheckoutLock;

    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;
    private static final Namespace BES_ADMIN_NS = opendap.namespaces.BES.BES_ADMIN_NS;

    private static final String BES_ADMIN_COMMAND = "BesAdminCmd";
    private static final String BES_ADMIN_SET_LOG_CONTEXT = "SetLogContext";
    private static final String BES_ADMIN_GET_CONFIG = "GetConfig";
    private static final String BES_ADMIN_SET_CONFIG = "SetConfig";
    private static final String BES_ADMIN_TAIL_LOG = "TailLog";

    
    public BES(BESConfig config) {
        _config = config.copy();
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        _clientQueue = new ArrayBlockingQueue<>(getMaxClients(), true);
        _clientCheckoutLock = new ReentrantLock(true);

        _checkOutFlag = new Semaphore(getMaxClients(), true);
        totalClients = 0;

        _adminLock = new ReentrantLock(true);
        _versionDocLock = new ReentrantLock(true);
        _clientsMapLock = new ReentrantLock(true);
        _clients = new ConcurrentHashMap<>();


        log.debug("BES built with configuration:\n{}", _config);
        _serverVersionDocument = null;

    }





    public List<BesConfigurationModule> getConfigurationModules() throws BesAdminFail {

        ArrayList<BesConfigurationModule> configurationModules = new ArrayList<>();

        String configString = getConfiguration(null);

        ByteArrayInputStream bais = new ByteArrayInputStream(configString.getBytes( HyraxStringEncoding.getCharset()));

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

    public int getTimeout() {
        return _config.getTimeOut();
    }

    public String getPrefix() {
        return _config.getPrefix();
    }

    public String getNickName() {
        return _config.getBesName();
    }
    public void setNickName(String name) {
        _config.setBesName(name);
    }


    public int getMaxClients() {
        return _config.getMaxClients();
    }

    public int getMaxResponseSize() {
        return _config.getMaxResponseSize();
    }


    public TreeMap<String, BesLogger> getBesLoggers() throws BesAdminFail {
        TreeMap<String, BesLogger> besLoggers = new TreeMap<>();

        String getLogContextsCmd = getSimpleBesAdminCommand("GetLogContexts");

        String besResponse = executeBesAdminCommand(getLogContextsCmd);
        ByteArrayInputStream bais = new ByteArrayInputStream(besResponse.getBytes( HyraxStringEncoding.getCharset()));

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

        SortedMap<String, BesLogger> besLoggers = getBesLoggers();

        BesLogger logger = besLoggers.get(loggerName);

        if (logger != null && logger.getIsEnabled())
            return "on";

        return "off";

    }

    public String setLoggerState(String loggerName, String loggerState) throws BesAdminFail {

        String setLoggerStateCmd = getSetBesLoggersStateCommand(loggerName, loggerState);

        String besResponse = executeBesAdminCommand(setLoggerStateCmd);
        ByteArrayInputStream bais = new ByteArrayInputStream(besResponse.getBytes( HyraxStringEncoding.getCharset()));

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


    /**
     *
     * @param loggerName
     * @param loggerState
     * @return
     */
    public String getSetBesLoggersStateCommand(String loggerName, String loggerState) {
        Element docRoot = new Element(BES_ADMIN_COMMAND, BES_ADMIN_NS);
        Element cmd = new Element(BES_ADMIN_SET_LOG_CONTEXT, BES_ADMIN_NS);

        cmd.setAttribute("name", loggerName);
        cmd.setAttribute("state", loggerState);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
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

        _adminLock.lock();
        try {

            try {
                log.debug("Getting new admin client...");

                admin = new OPeNDAPClient();
                log.debug("Starting new admin client. Host: {} Port: {}", getHost(), getAdminPort());

                admin.startClient(getHost(), getAdminPort(), getTimeout());
                log.debug("BES admin client started, sending command:\n{}", besCmd);


                admin.executeCommand(besCmd, baos, baos);


                String besResponse;
                try {
                    besResponse = baos.toString(StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    besResponse = "FAILED to encode BES response as " + StandardCharsets.UTF_8.name();
                }

                log.debug("BES returned:\n{}", besResponse);

                return besResponse;

            } catch (PPTException e) {

                sb.append("Failed to execute BES command. Message: ")
                        .append(e.getMessage());


                throw new BesAdminFail("Failed to execute BES command. Message: " + e.getMessage(), e);


            } finally {
                if (admin != null) {
                    try {
                        admin.shutdownClient(false);
                    } catch (PPTException e) {
                        sb.append("FAILED TO SHUTDOWN CLIENT! Msg: ").append(e.getMessage());
                        admin.killClient();
                    }
                }
            }
        }
        finally {
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

        }
        catch (InterruptedException e) {
            sb.append(e.getMessage());
            Thread.currentThread().interrupt();
        }
        finally {
            sb.append("Releasing client checkout lock...").append("\n");
            log.info("{}",sb);
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
        Element docRoot = new Element(BES_ADMIN_COMMAND, BES_ADMIN_NS);
        Element cmd = new Element(BES_ADMIN_GET_CONFIG, BES_ADMIN_NS);

        if (moduleName != null)
            cmd.setAttribute("module", moduleName);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
    }


    public String getSetConfigurationCommand(String moduleName, String configuration) {
        Element docRoot = new Element(BES_ADMIN_COMMAND, BES_ADMIN_NS);
        Element cmd = new Element(BES_ADMIN_SET_CONFIG, BES_ADMIN_NS);

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
        Element docRoot = new Element(BES_ADMIN_COMMAND, BES_ADMIN_NS);
        Element cmd = new Element(BES_ADMIN_TAIL_LOG, BES_ADMIN_NS);

        if (lines != null)
            cmd.setAttribute("lines", lines);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);
    }


    public String getSimpleBesAdminCommand(String besCmd) {

        Element docRoot = new Element(BES_ADMIN_COMMAND, BES_ADMIN_NS);
        Element cmd = new Element(besCmd, BES_ADMIN_NS);

        docRoot.addContent(cmd);

        Document besCmdDoc = new Document(docRoot);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(besCmdDoc);

    }


    public Document getVersionDocument() throws BESError, JDOMException, IOException, BadConfigurationException, PPTException {

        // Lock the resource.
        _versionDocLock.lock();
        try {
            // Have we cached it already?
            if (_serverVersionDocument == null) {
                // Nope? Then Cache it!
                cacheServerVersionDocument();
                if (log.isInfoEnabled()) {
                    XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                    log.info("BES Version Document: \n{}", xmlo.outputString(_serverVersionDocument));
                }

            }
        }
        finally {
            // Unlock the resource.
            _versionDocLock.unlock();
        }
        // Return a copy so nobody can break our stuff!
        return (Document) _serverVersionDocument.clone();

    }




    /**
     * Helper method that adds the bes client identifier to the request ID.
     * @param request
     * @param oc
     * @throws IOException
     */
    private void tweakRequestId(Document request, OPeNDAPClient oc) throws IOException {
        Element reqElement = request.getRootElement();
        if(reqElement==null){
            throw new IOException("The BES Request document must have a root element!");
        }
        String reqID = reqElement.getAttributeValue(BesApi.REQUEST_ID);
        if(reqID==null)
            reqID="";

        reqID += "[bes_client:"+oc.getID()+"]";
        reqElement.setAttribute(BesApi.REQUEST_ID,reqID);
    }

    /**
     * Executes a command/response transaction with the BES
     *
     * @param request   The BES request document.
     * @param response  The document into which the BES response will be placed. If the passed Document object contains
     * conent, then the content will be discarded.
     * @return true if the request is successful, false if there is a problem fulfilling the request.
     * @throws IOException
     * @throws PPTException
     * @throws JDOMException
     */
    public void  besTransaction(Document request, Document response )
            throws IOException, PPTException, JDOMException, BESError {

        log.debug("besTransaction() -  BEGIN.");


        boolean trouble = false;
        Document doc;

        SAXBuilder sb = new SAXBuilder();

        OPeNDAPClient oc = getClient();

        if(oc==null){
            String msg = "FAILED to retrieve valid OPeNDAPClient (connection to BES)!";
            log.error("besTransaction() - {}", msg);
            throw new IOException(msg);
        }
        tweakRequestId(request,oc);
        if(log.isDebugEnabled()) {
            log.debug("besTransaction() request document: \n{}-----------\n", showRequest(request));
        }
        Logger besCommandLogger = LoggerFactory.getLogger("BesCommandLog");
        if(besCommandLogger.isInfoEnabled()){
            besCommandLogger.info("BES COMMAND ({})\n{}\n",new Date(),showRequest(request));
        }

        Procedure timedProc = Timer.start();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ByteArrayOutputStream erros = new ByteArrayOutputStream()) {


            if (oc.sendRequest(request, baos, erros)) {

                log.debug("besTransaction() The BES returned this document:\n{}", baos);

                if (baos.size() != 0) {

                    doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

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

            } else {
                log.debug("BES returned this ERROR document:\n-----------\n{}-----------", erros);

                BESError besError;
                if (erros.size() != 0) {

                    ByteArrayInputStream bais = new ByteArrayInputStream(erros.toByteArray());
                    besError = new BESError(bais);

                    // This logging statement is turned down to debug because otherwise when the OLFS probes the
                    // the BES in an effort to distinguish requests for files in the BES catalog from DAP and catalog
                    // requests the log gets filled with spurious errors. When we eventually implement a showPathInfo()
                    // method/model for that will allow the OLFS to do this in a more efficient and non error
                    // producing manner we should also:
                    // @TODO Promote this to log.error()
                    log.debug("besTransaction() -  BES Transaction received a BESError Object. Msg: {}", besError.getMessage());

                    int besErrCode = besError.getBesErrorCode();
                    if (besErrCode == BESError.INTERNAL_FATAL_ERROR || besErrCode == BESError.TIME_OUT) {
                        trouble = true;
                    }
                } else {
                    String reqId = request.getRootElement().getAttributeValue(BesApi.REQUEST_ID);
                    Document errDoc = getMissingBesResponseErrorDoc(reqId);
                    besError = new BESError(errDoc);

                }


                int besErrCode = besError.getBesErrorCode();
                if (besErrCode == BESError.INTERNAL_FATAL_ERROR || besErrCode == BESError.TIME_OUT) {
                    trouble = true;
                }

                throw besError;

            }


        } catch (PPTException e) {

            trouble = true;

            log.debug("OLFS Encountered a PPT Problem!", e);
            String msg = "Problem with OPeNDAPClient. OPeNDAPClient executed " + oc.getCommandCount() + " commands";

            log.error(msg);

            e.setErrorMessage(msg);
            throw e;

        } finally {
            returnClient(oc, trouble);

            Timer.stop(timedProc);
            log.debug("besTransaction() -  END.");
        }


    }


    /**
     * Executes a command/response transaction with the BES
     *
     * @param request   The BES request document.
     * @param os   The outputstream to write the BES response to.
     * any error information will be written to the OutputStream err.
     * @throws BadConfigurationException
     * @throws IOException
     * @throws PPTException
     */
    public void besTransaction(Document request, OutputStream os)
            throws IOException, PPTException, BESError {

        log.debug("BEGIN");

        boolean besTrouble = false;
        OPeNDAPClient oc = getClient();
        if(oc==null){
            String msg = "FAILED to retrieve a valid OPeNDAPClient instance! "+
                    "BES Prefix: "+getPrefix()+" BES NickName: "+getNickName()+" BES Host: "+getHost();
            log.error(msg);
            throw new IOException(msg);

        }
        tweakRequestId(request,oc);
        if(log.isDebugEnabled()) {
            log.debug("besTransaction() request document: \n-----------\n{}-----------\n", showRequest(request));
        }
        Logger besCommandLogger = LoggerFactory.getLogger("BesCommandLog");
        if(besCommandLogger.isInfoEnabled()){
            besCommandLogger.info("BES COMMAND ({})\n{}\n",new Date(),showRequest(request));
        }




        Procedure timedProc = Timer.start();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            boolean result = oc.sendRequest(request, os, baos);
            log.debug("besTransaction() - Completed.");
            if (!result) {

                log.debug("BESError: \n{}", baos.toString(HyraxStringEncoding.getCharset().name()));
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                BESError besError = new BESError(bais);

                log.error("besTransaction() -  BES Transaction received a BESError Object. Msg: {}", besError.getMessage());

                int besErrCode = besError.getBesErrorCode();
                if (besErrCode == BESError.INTERNAL_FATAL_ERROR || besErrCode == BESError.TIME_OUT) {
                    besTrouble = true;
                }

                throw besError;
            }

        } catch (PPTException e) {

            besTrouble = true;
            String msg = "Problem encountered with BES connection. Message: '" + e.getMessage() + "' " +
                    "OPeNDAPClient executed " + oc.getCommandCount() + " prior commands.";

            log.error(msg);
            e.setErrorMessage(msg);
            throw e;
        } finally {
            returnClient(oc, besTrouble);
            Timer.stop(timedProc);
            log.debug("END");

        }

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
        errorResponse.setAttribute(BesApi.REQUEST_ID, reqId);

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


        _serverVersionDocument = version;


    }


    public String trimPrefix(String dataset) {
        String trim;
        if (getPrefix().equals("/")) {
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

        OPeNDAPClient besClient=null;

        if (_checkOutFlag == null)
            return null;

        _clientCheckoutLock.lock();
        try {

            // Acquiring this semaphore is what limits the number
            // of clients that will be in the pool. The number of
            // semaphores available is set to MaxClients.
            _checkOutFlag.acquire();

            log.debug("_clientQueue size: '{}'",_clientQueue.size());

            if (_clientQueue.isEmpty()) {
                besClient = getNewClient();
            } else {

                // Get a client from the client pool.
                besClient = _clientQueue.take();
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
                _checkOutFlag.release(); // Release the client permit because this client is hosed...
            }
            throw new PPTException(e);
        } finally {
            _clientCheckoutLock.unlock();
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
            _checkOutFlag.release(); // Release the client permit because this client is hosed...
            String msg ="BES Client Failed To Start. Message: '" + ppte.getMessage()+"' ";
            besClient.setID(new Date().toString() + msg);
            log.error(msg);
            throw new PPTException(msg,ppte);
        }


        // Add it to the client pool
        try {
            _clientsMapLock.lock();
            String clientId = (getNickName()==null?getPrefix():getNickName());
            if(clientId.isEmpty())
                clientId = "besC";
            clientId += "-" + totalClients;
            besClient.setID(clientId);
            _clients.put(clientId, besClient);
            totalClients++;

            log.debug("New BES Client assigned ID: {}", besClient.getID());

        } finally {
            _clientsMapLock.unlock();
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
            if(log.isDebugEnabled()) {
                String msg = "checkInClient() This instance of OPeNDAPClient (id:" +
                        dapClient.getID() + ") has " +
                        "excecuted " + dapClient.getCommandCount() +
                        " commands which is in excess of the maximum command " +
                        "limit of " + _config.getMaxCommands() + ", discarding client.";
                log.debug(msg);
            }

        } else {

            if (_clientQueue.offer(dapClient)) {
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
        // removing the client from the _clients Map the client is
        // discarded.

        if(dapClient != null){
            log.debug("Discarding OPeNDAPClient (id:{})", dapClient.getID());

            try {
                _clientsMapLock.lock();
                if (dapClient.getID() != null)
                    _clients.remove(dapClient.getID());
            } finally {
                _clientsMapLock.unlock();
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
     * clients are checked into the pool and then gracefully shuts down each
     * client's connection to the BES.
     */
    public void destroy() {

        boolean nicely = false;
        boolean gotClientCheckoutLock = false;

        try {
            if (_clientCheckoutLock.tryLock(10, TimeUnit.SECONDS)) {
                gotClientCheckoutLock = true;

                log.debug("Attempting to acquire all client permits...");


                if (_checkOutFlag.tryAcquire(getMaxClients(), 10, TimeUnit.SECONDS)) {
                    log.debug("All {} client permits acquired.",getMaxClients());

                    log.debug("There are {} client(s) to shutdown.", _clientQueue.size());


                    int i = 0;
                    while (!_clientQueue.isEmpty()) {
                        OPeNDAPClient odc = _clientQueue.take();
                        log.debug("Retrieved OPeNDAPClient[{}] (id:{}) from queue.",i++,odc.getID());
                        shutdownClient(odc);
                    }
                    _checkOutFlag.release(getMaxClients());
                    nicely = true;
                }


            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OUCH! Interrupted while shutting down BESPool", e);
        } finally {
            if (gotClientCheckoutLock)
                _clientCheckoutLock.unlock();
        }


        if (!nicely) {
            log.debug("Timed Out. Destroying BES Clients.");

            try {
                _clientsMapLock.lock();
                for (OPeNDAPClient oc: _clients.values()) {
                    if (oc != null) {
                        log.debug("Killing BES Client (id:{})", oc.getID());
                        oc.killClient();
                    } else {
                        log.error("Retrieved a 'null' BES Client instance from _clients collection!");

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
