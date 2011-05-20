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

import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;

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


    public int getAdminPort() {
        return _config.getAdminPort();
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

    public int getBesClientCount(){
        return _clients.size();
    }

    public Enumeration<OPeNDAPClient> getClients(){
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

    public String executeBesAdminCommand(String besCmd){
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if(getAdminPort()<=0){
            sb.append("BES Admin Service is not configured! Port number for admin connection has not been set!");
            return sb.toString();
        }

        try {
            _adminLock.lock();

            log.debug("Sending BES admin commend:\n{}", besCmd);

            OPeNDAPClient admin = new OPeNDAPClient();
            admin.startClient(getHost(), getAdminPort());


            admin.executeCommand(besCmd, baos, baos);

            log.debug("BES returned:\n{}",baos.toString());

            return baos.toString();

        } catch (PPTException e) {

            sb.append("Failed to execute BES command. Message: ")
            .append(e.getMessage());


            log.error(sb.toString());
            log.error("BES returned:\n{}",baos.toString());

            return sb.toString();
        }
        finally {
            _adminLock.unlock();
        }

    }
    public String start(){
        String cmd = getStartCommand();
        return executeBesAdminCommand(cmd);
    }

    public String stopNice(long timeOut){
        String cmd = getStopNiceCommand();
        return executeBesAdminCommand(cmd);
    }

    public String stopNow(){
        String cmd = getStopNowCommand();
        return executeBesAdminCommand(cmd);
    }

    public String getStartCommand(){
        return  getBesAdminCommand("Start");
    }

    public String getStopNowCommand(){
        return  getBesAdminCommand("StopNow");
    }

    public String getStopNiceCommand(){
        return  getBesAdminCommand("StopNice");
    }


    public String getBesAdminCommand(String besCmd){

        Element docRoot = new Element("BesAdminCmd",BES_ADMIN_NS);
        Element cmd = new Element(besCmd,BES_ADMIN_NS);

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
                    log.info("BES Version: \n" + _serverVersionDocument);
                }
            }
            finally {
                // Unlock the resource.
                _versionDocLock.unlock();
            }
        }
        // Return a copy so nobody can break our stuff!
        if(_serverVersionDocument !=null){
            return (Document)  _serverVersionDocument.clone();
        }

        return null;
    }


    /**
     *
     *
     *
     *
     *
     *
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

        if(BesXmlAPI.getVersion(getPrefix(),version)){


            Element ver = version.getRootElement().getChild("showVersion",BES_NS);


            // Disconnect it from it's parent.
            ver.detach();

            ver.setName("BES");
            ver.setAttribute("prefix",getPrefix());

            version.detachRootElement();
            version.setRootElement(ver);
            

            _serverVersionDocument = version;
        }
        else {

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
            throws PPTException, BadConfigurationException {

        OPeNDAPClient odc = null;
        String clientId;

        if (_checkOutFlag == null)
            return null;

        try {
            _clientCheckoutLock.lock();

            // Aquiring this semaphore is what limits the number
            // of clients that will be in the pool. The number of
            // semaphores available is set to MaxClients.
            _checkOutFlag.acquire();

            if (_clientQueue.size() == 0) {
                odc = new OPeNDAPClient();
                log.debug("getClient() - " +
                        "Made new OPeNDAPClient. Starting...");

                odc.startClient(getHost(), getPort());

                try {
                    _clientsMapLock.lock();
                    clientId="besC-"+totalClients;
                    odc.setID(clientId);
                    _clients.put(clientId, odc);
                    totalClients++;
                }
                finally {
                    _clientsMapLock.unlock();
                }


                log.debug("OPeNDAPClient started.");


            } else {

                odc = _clientQueue.take();
                log.debug("getClient() - Retrieved " +
                        "OPeNDAPClient (id:"+odc.getID()+" from Pool.");
            }


            return odc;

        }
        catch (Exception e) {
            log.error("ERROR encountered: "+e.getMessage());
            if(odc!=null){
                log.error("Attempting to discard OPeNDAPClient (id:"+odc.getID()+")");
                discardClient(odc);
            }
            throw new PPTException(e);
        }
        finally{
            _clientCheckoutLock.unlock();
        }


    }



    /**
     * When a piece of code is done using an OPeNDAPClient, it should return it
     * to the pool using this method.
     *
     * @param dapClient     The OPeNDAPClient to return to the client pool.
     * @param discard Pitch it, it's broken.
     * @throws PPTException .
     */
    public void returnClient(OPeNDAPClient dapClient, boolean discard) throws PPTException {


        if (dapClient == null)
            return;

        //log.debug("returnClient() clientID="+dapClient.getID()+"  discard="+discard);


        try {

            if (discard){
                discardClient(dapClient);
            }
            else {
                checkInClient(dapClient);
            }

        } catch (PPTException e) {


            String msg = "returnClient() *** BES - WARNING! Problem with " +
                    "OPeNDAPClient, discarding.";

            log.error(msg);
            try {
                _clientsMapLock.lock();
                _clients.remove(dapClient.getID());
            }
            finally {
                _clientsMapLock.unlock();
            }

            throw new PPTException(msg, e);
        }
        finally {
            _checkOutFlag.release();
        }


    }


    private void checkInClient(OPeNDAPClient dapClient) throws PPTException {


        if (_config.getMaxCommands() > 0 && dapClient.getCommandCount() > _config.getMaxCommands()) {
            discardClient(dapClient);
            log.debug("checkInClient() This instance of OPeNDAPClient (id:"+
                    dapClient.getID()+") has " +
                    "excecuted " + dapClient.getCommandCount() +
                    " commands which is in excess of the maximum command " +
                    "limit of " + _config.getMaxCommands() + ", discarding client.");

        }
        else {

            if(_clientQueue.offer(dapClient)){
            log.debug("checkInClient() Returned OPeNDAPClient (id:"+
                    dapClient.getID()+") to Pool.");
            }
            else {
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

        log.debug("discardClient() Discarding OPeNDAPClient #" +dapClient.getID());

        try {
            _clientsMapLock.lock();
            if(dapClient!=null && dapClient.getID()!=null)
                _clients.remove(dapClient.getID());
        }
        finally {
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
            if(_clientCheckoutLock.tryLock(10,TimeUnit.SECONDS)){
                gotClientCheckoutLock = true;
                Semaphore permits = _checkOutFlag;

                log.debug("destroy() Attempting to acquire all clients...");


                if (permits.tryAcquire(getMaxClients(), 10, TimeUnit.SECONDS)) {
                    log.debug("destroy() All clients aquired.");

                    log.debug("destroy() " + _clientQueue.size() +
                            " client(s) to shutdown.");


                    int i = 0;
                    while (_clientQueue.size() > 0) {
                        OPeNDAPClient odc = _clientQueue.take();
                        log.debug("destroy() Retrieved OPeNDAPClient["
                                + i++ + "] (id:"+odc.getID()+") from queue.");

                        try {
                            shutdownClient(odc);
                        }
                        catch (Throwable t){
                            log.error("destroy() Failed to shutdown " +
                                    "OPeNDAPClient (id:"+odc.getID()+") msg: "+
                                    t.getMessage(),t);
                        }


                    }
                    nicely = true;
                }


            }
        }
        catch (Throwable e) {
            log.error("destroy() OUCH! Problem shutting down BESPool",e);
        }
        finally {
            _checkOutFlag = null;
            if(gotClientCheckoutLock)
                _clientCheckoutLock.unlock();
        }


       if(!nicely) {
            log.debug("destroy() Timed Out. Destroying Clients.");

            try {
                _clientsMapLock.lock();
                for (int i = 0; i < totalClients; i++) {
                    OPeNDAPClient oc = _clients.get(i);
                    if (oc != null) {
                        log.debug("destroy() Killing OPeNDAPClient (id:"+
                                oc.getID()+")");
                        oc.killClient();
                    } else {
                        log.debug("destroy() OPeNDAPClient (id:"+
                                i+")already discarded.");

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
