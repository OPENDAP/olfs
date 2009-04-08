/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;

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
    private HashMap<Integer, OPeNDAPClient> _clients;
    private Semaphore _checkOutFlag;
    private BESConfig _config;
    private int totalClients;


    private Document _serverVersionDocument;
    private ReentrantLock _versionDocLock;
    private ReentrantLock _mapLock;
    private ReentrantLock _clientCheckoutLock;

    private int clientMaxCommands;

    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;


    private DevNull devNull = new DevNull();


    public BES(BESConfig config) throws Exception {
        _config = config.copy();
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        _clientQueue = new ArrayBlockingQueue<OPeNDAPClient>(getMaxClients(), true);
        _clientCheckoutLock = new ReentrantLock(true);

        _checkOutFlag = new Semaphore(getMaxClients(), true);
        totalClients = 0;

        _versionDocLock = new ReentrantLock(true);
        _mapLock = new ReentrantLock(true);
        _clients = new HashMap<Integer, OPeNDAPClient>();


        log.debug("BES built with configuration: \n" + _config);
        _serverVersionDocument = null;

        clientMaxCommands = 2000;

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


    public String toString() {

        return "[BES prefix: " + getPrefix() +
                " host: " + getHost() +
                " port: " + getPort() +
                " maxClients: " + getMaxClients() +
                " maxClientCommands: " + clientMaxCommands +
                "]";


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

        if (_checkOutFlag == null)
            return null;

        try {
            _clientCheckoutLock.lock();

            _checkOutFlag.acquire();

            if (_clientQueue.size() == 0) {
                odc = new OPeNDAPClient();
                log.debug("getClient() - " +
                        "Made new OPeNDAPClient. Starting...");

                odc.startClient(getHost(), getPort());

                try {
                    _mapLock.lock();
                    odc.setID(totalClients);
                    _clients.put(totalClients, odc);
                    totalClients++;
                }
                finally {
                    _mapLock.unlock();
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
            log.error("ERROR encountered.");
            discardClient(odc);
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
     * @param odc     The OPeNDAPClient to return to the client pool.
     * @param discard Pitch it, it's broken.
     * @throws PPTException .
     */
    public void returnClient(OPeNDAPClient odc, boolean discard) throws PPTException {


        if (odc == null)
            return;

        //log.debug("returnClient() clientID="+odc.getID()+"  discard="+discard);


        try {

            if (discard){
                discardClient(odc);
            }
            else {
                checkInClient(odc);
            }

        } catch (PPTException e) {


            String msg = "returnClient() *** BES - WARNING! Problem with " +
                    "OPeNDAPClient, discarding.";

            log.error(msg);
            try {
                _mapLock.lock();
                _clients.remove(odc.getID());
            }
            finally {
                _mapLock.unlock();
            }

            throw new PPTException(msg, e);
        }
        finally {
            _checkOutFlag.release();
        }


    }


    private void checkInClient(OPeNDAPClient odc) throws PPTException {


        if (clientMaxCommands > 0 && odc.getCommandCount() > clientMaxCommands) {
            discardClient(odc);
            log.debug("checkInClient() This instance of OPeNDAPClient (id:"+
                    odc.getID()+") has " +
                    "excecuted " + odc.getCommandCount() +
                    " commands which is in excess of the maximum command " +
                    "limit of " + clientMaxCommands + ", discarding client.");

        }
        else {

            if(_clientQueue.offer(odc)){
            log.debug("checkInClient() Returned OPeNDAPClient (id:"+
                    odc.getID()+") to Pool.");
            }
            else {
                log.error("checkInClient(): OUCH! OUCH! OUCH! The Pool is " +
                        "full and I need to check in a client! This Should " +
                        "NEVER Happen!");
            }

        }


    }


    private void discardClient(OPeNDAPClient odc) throws PPTException {
        // By failing to put the client into the queue and
        // removing the client from the _clients Map the client is
        // discarded.

        log.debug("discardClient() Discarding OPeNDAPClient #"+odc.getID());

        try {
            _mapLock.lock();
            _clients.remove(odc.getID());
        }
        finally {
            _mapLock.unlock();
        }


        if (odc.isRunning()) {
            shutdownClient(odc);
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
                _mapLock.lock();
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
                _mapLock.unlock();
            }
        }



    }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------


}
