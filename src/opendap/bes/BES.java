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

package opendap.bes;

import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.io.IOException;

import org.slf4j.Logger;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;

/**
 * User: ndp
 * Date: Mar 19, 2007
 * Time: 11:39:18 AM
 */
public class BES {

    private Logger log;


    private ArrayBlockingQueue<OPeNDAPClient> _clientQueue;
    private Semaphore _checkOutFlag;
    private BESConfig _config;


    private Document _serverVersionDocument;
    private ReentrantLock _versionDocLock;


    private DevNull devNull = new DevNull();


    public BES(BESConfig config) throws Exception {
        _config = config.copy();
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        _clientQueue = new ArrayBlockingQueue<OPeNDAPClient>(getMaxClients());
        _checkOutFlag = new Semaphore(getMaxClients());

        _versionDocLock = new ReentrantLock(true);


        log.debug("BES built with configuration: \n" + _config);
        _serverVersionDocument = null;

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
        return (Document) _serverVersionDocument.clone();
    }


    private void cacheServerVersionDocument() throws IOException,
            PPTException,
            BadConfigurationException,
            JDOMException, BESException {

        log.debug("Getting Server Version Document.");

        Document doc = BesAPI.showVersion(getPrefix());

        Element bes = doc.getRootElement().getChild("BES");


        List guts = bes.removeContent();


        Element prefix = new Element("prefix");
        prefix.addContent(getPrefix());
        Element host = new Element("host");
        host.addContent(getHost());
        Element port = new Element("port");
        port.addContent(getPort() + "");


        bes.addContent(prefix);
        bes.addContent(host);
        bes.addContent(port);

        bes.addContent(guts);

        _serverVersionDocument = doc;


    }


    public String trimPrefix(String dataset) {

        String trim = dataset.substring(getPrefix().length());

        if (trim.indexOf("/") != 0)
            trim = "/" + trim;
        return trim;
    }


//------------------------------------------------------------------------------
//-------------------------- CLIENT POOL CODE ----------------------------------
//------------------------------------------------------------------------------


    /**
     * The pool of available OPeNDAPClient connections starts empty. When this
     * method is called the pool is checked. If no client is available, and the
     * number of clients has not reached the cap, then a new one is made,
     * started, and returned. If no client is available and the cap has been
     * reached then this method will BLOCK until a client becomes available.
     * If a client is available, it is returned.
     *
     * @return The next available OPeNDAPClient.
     * @throws opendap.ppt.PPTException  .
     * @throws BadConfigurationException .
     */
    public OPeNDAPClient getClient()
            throws PPTException, BadConfigurationException {

        OPeNDAPClient odc = null;

        try {
            _checkOutFlag.acquire();

            if (_clientQueue.size() == 0) {
                odc = new OPeNDAPClient();
                log.debug("getClient() - " +
                        "Made new OPeNDAPClient. Starting...");

                odc.startClient(getHost(), getPort());
                log.debug("OPeNDAPClient started.");


            } else {

                odc = _clientQueue.take();
                log.debug("getClient() - Retrieved " +
                        "OPeNDAPClient from queue.");
            }

            //if (Debug.isSet("BES"))
            //    odc.setOutput(System.out, true);
            //else
            odc.setOutput(devNull, true);

            //odc.isProperlyConnected();
            //odc.setOutput(devNull, false);
            //odc.executeCommand("show status;");


            return odc;
        }
        catch (Exception e) {
            log.error("ERROR encountered.");
            discardClient(odc);
            throw new PPTException(e);
        }

    }

    /**
     * When a piece of code is done using an OPeNDAPClient, it should return it
     * to the pool using this method.
     *
     * @param odc The OPeNDAPClient to return to the client pool.
     * @throws PPTException .
     */
    public void returnClient(OPeNDAPClient odc) throws PPTException {

        try {


            String cmd = "delete definitions;\n";
            odc.executeCommand(cmd);

            cmd = "delete containers;\n";
            odc.executeCommand(cmd);


            odc.setOutput(null, false);

            _clientQueue.put(odc);
            _checkOutFlag.release();
            log.debug("Returned OPeNDAPClient to queue.");
        }
        catch (InterruptedException e) {
            e.printStackTrace(); // Don't do a thing
        } catch (PPTException e) {


            String msg = "\n*** BES - WARNING! Problem with " +
                    "OPeNDAPClient, discarding.";

            discardClient(odc);

            throw new PPTException(msg, e);
        }

    }


    public void discardClient(OPeNDAPClient odc) {
        if (odc != null && odc.isRunning()) {
            try {
                BesAPI.shutdownClient(odc);
            } catch (PPTException e) {
                log.debug("BES: Discarding client " +
                        "encountered problems shutting down an " +
                        "OPeNDAPClient connection to the BES\n");

            }
        }
        // By releasing the flag and not checking the OPeNDAPClient back in
        // we essentially throw the client away. A new one will be made
        // the next time it's needed.
        _checkOutFlag.release();

    }


    /**
     * This method is meant to be called at program exit. It waits until all
     * clients are checked into the pool and then gracefully shuts down each
     * client's connection to the BES.
     */
    public void shutdownBES() {


        try {

            log.debug("shutdownBES() - " +
                    "Waiting for BES client check in to complete.");
            _checkOutFlag.acquireUninterruptibly(getMaxClients());
            log.debug(" All clients checked in.");

            log.debug("BES.shutdownBES() - " + _clientQueue.size() +
                    " client(s) to shutdown.");


            int i = 0;
            while (_clientQueue.size() > 0) {
                OPeNDAPClient odc = _clientQueue.take();
                log.debug("Retrieved OPeNDAPClient["
                        + i++ + "] from queue.");

                BesAPI.shutdownClient(odc);


            }

        } catch (InterruptedException e) {
            e.printStackTrace(); // Do nothing
        } catch (PPTException e) {
            e.printStackTrace();  // Do nothing..
        }


    }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------


}
