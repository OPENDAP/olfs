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

package opendap.wcs.v1_1_2;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * <p>
 * This class contains the machinery to build globally unique (within this WcsService) coverage ID strings
 * for different DAP datasets that are being served as WCS Coverages. The DAP datasets are identified by their
 * URL. This implementation removes the protocol and server information from the DAP URL and replaces it with a
 * simple string (S1, S2, S3, etc...)
 * </p>
 * <p>
 * An alternate implementation might build a hash string for each URL.
 * </p>
 *
 *
 *
 */
public class CoverageIdGenerator {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(CoverageIdGenerator.class);

    /**
     * Keeps track of the Server ID prefixes by associating them with a the server's URL.<br/>
     * key: ServerURL<br/>
     * value: serverID<br/>
     *
     */
    private static ConcurrentSkipListMap<String, String> serverIDs = new ConcurrentSkipListMap<String,String>();

    /**
     * Keeps track of the WCS ID strings
     *  by associating them with a the server's URL.<br/>
     * key: datasetURL<br/>
     * value: wcsID<br/>
     *
     */
    private static ConcurrentHashMap<String, String> wcsIDs = new ConcurrentHashMap<String,String>();


    /**
     * Used to make static methods thread safe.
     */
    private static ReentrantLock genLock = new  ReentrantLock();


    /**
     * Get the collection of DAP server URLs that are mapped to a serverID string.
     * @return A sorted ascending list of serverURLs as an Array.
     */
    public static Vector<String> getServerURLs(){
        return new Vector<String>(serverIDs.keySet().descendingSet());
    }


    /**
     *
     * Get the server ID associated with the pass severURL
     * 
     * @param serverURL
     * @return The Server ID
     */
    public static String getServerID(String serverURL){
        return serverIDs.get(serverURL);
    }

    /**
     * Returns the server ID strings for all of the DAP datasets that have been ingested as coverages.
     * @return
     */
    public static String[] getServerIDs(){
        return serverIDs.values().toArray(new String[ serverIDs.size()]);
    }


    /**
     * Decomposes a URL and returns just the protocol and server sections.<br/>
     * For Example: <br/>
     * <code>http://localhost:8080/opendap/data/nc/fnoc1.nc</code><br/>
     * Becomes:</br>
     * <code>http://localhost:8080</code><br/>
     * <br/>
     *
     *
     *
     * @param url The URL to decompose.
     * @return  A string containing the protocol and server parts of the URL.
     */
    private static String getServerUrlString(URL url) {

        String baseURL = null;

        String protocol = url.getProtocol();

        if (protocol.equalsIgnoreCase("file")) {
            log.debug("Protocol is FILE.");

        } else if (protocol.equalsIgnoreCase("http")) {
            log.debug("Protocol is HTTP.");

            String host = url.getHost();
            String path = url.getPath();
            int port = url.getPort();

            baseURL = protocol + "://" + host;

            if (port != -1)
                baseURL += ":" + port;
        }

        log.debug("ServerURL: " + baseURL);

        return baseURL;

    }




    /***************************************************************************
     * This wraps the getWcsID function for use as a processing function by the
     * semantic catalog code.
     *
     * @param RDFList
     * @param createValue
     * @return
     */
     public static Value getWcsId(List<String> RDFList,  ValueFactory createValue) {

        String targetObj = "";

        targetObj = RDFList.get(0); // rdf list has only one element
        targetObj = getWcsIdString(targetObj);
        Value stObjStr;
        stObjStr = createValue.createLiteral(targetObj);
        return stObjStr;
    }





    /**
     * Build a wcs:Identifier for the coverage dataset described by the datasetUrl.
     *
     * @param datasetUrl
     * @return A valid and unique to this service wcs:Identifier String for the coverage dataset
     */
    public static String getWcsIdString(String datasetUrl)  {

        String wcsID="FAILED_TO_BUILD_WCS_ID";

        try {
            genLock.lock();
            int i;
            String serverURL, serverID;
            URL dsu = new URL(datasetUrl);


            serverURL = getServerUrlString(dsu);

            log.debug("getWcsIdString(): serverURl is "+serverURL);

            if(serverIDs.containsKey(serverURL)){
                // get server prefix
                serverID = serverIDs.get(serverURL);
                log.debug("getWcsIdString(): serverURL already in use, will reuse serverID '"+serverID+"'");
            }
            else {
                serverID = "S"+ (serverIDs.size()+1) + "";
                // Generate service prefix
                // Store service prefix.
                serverIDs.put(serverURL,serverID);
                log.debug("getWcsIdString(): New serverURL! Created new serverID '"+serverID+"'");

            }


            // Build wcsID
            if(!wcsIDs.containsKey(datasetUrl)){
                // add wcs:Identifier to MAP
                wcsID = serverID + datasetUrl.substring(serverURL.length(),datasetUrl.length());
                log.debug("getWcsIdString(): Dataset had no existing wcsID, adding wcsID: "+wcsID+
                        " for dataset: "+datasetUrl);
                wcsIDs.put(datasetUrl,wcsID);
            }
            else {
                wcsID = wcsIDs.get(datasetUrl);
                log.debug("getWcsIdString(): Dataset already has a wcsID, returning wcsID: "+wcsID+
                        " for dataset: "+datasetUrl);
            }

        } catch (MalformedURLException e) {
            log.error("Cannot Build wcs:Identifier from URL "+datasetUrl+" error msg: "+e.getMessage());
        }
        finally {
            genLock.unlock();
        }


        return wcsID;
    }

    /**
     * Ingests the passed HasMap into the internal caches of server ID, dataset URL, and WCS ID.
     * @param coverageIDServer
     */
    public static void updateIdCaches(HashMap<String, Vector<String>> coverageIDServer){

        try {
            genLock.lock();
            log.debug("Updating datasetUrl/wcsID and datasetUrl/serverID HashMap objects.");
            
            String serverUrl, serviceID, localId;

            for (String coverageID : coverageIDServer.keySet()) {
                log.debug("CoverageID: " + coverageID);
                Vector<String> datasetUrls = coverageIDServer.get(coverageID);
                for (String datasetUrl : datasetUrls) {

                    log.debug("    datasetUrl: " + datasetUrl);

                    serverUrl = getServerUrlString(new URL(datasetUrl));
                    log.debug("    serverUrl:  " + serverUrl);

                    localId = datasetUrl.substring(serverUrl.length(),datasetUrl.length());
                    log.debug("    localID:    "+localId);

                    serviceID = coverageID.substring(0,coverageID.indexOf(localId));
                    log.debug("    serviceID:     "+serviceID);

                    if(!serverIDs.containsKey(serverUrl)){
                        log.debug("Adding to ServiceIDs");
                        serverIDs.put(serverUrl,serviceID);
                    }
                    else if(serviceID.equals(serverIDs.get(serverUrl))){
                        log.info("The serverURL: "+serverUrl+" is already mapped to " +
                                "the serviceID: "+serviceID+" No action taken.");
                    }
                    else {
                        String msg = "\nOUCH! The semantic repository contains multiple serviceID strings " +
                                "for the same serverURL. This may lead to one of the serviceID's being " +
                                "reassigned. This would lead to resources being attributed to the " +
                                "wrong server/service.\n";
                        msg += "serverUrl: "+serverUrl+"\n";
                        msg += "  serviceID(repository) : "+serviceID+"\n";
                        msg += "  serviceID(in-memory):   "+ serverIDs.get(serverUrl)+"\n";

                        log.error(msg);

                    }


                    if(!wcsIDs.containsKey(datasetUrl)){
                        log.debug("Adding to datasetUrl/coverageID to Map");
                        wcsIDs.put(datasetUrl,coverageID);
                    }
                    else if(coverageID.equals(wcsIDs.get(datasetUrl))){
                        log.info("The datasetUrl: "+datasetUrl+" is already mapped to " +
                                "the coverageID: "+coverageID+" No action taken.");
                    }
                    else {
                        String msg = "\nOUCH! The semantic repository contains multiple coverageID strings " +
                                "for the same datasetUrl. This may lead to one of the coverageID's being " +
                                "reassigned. This would lead to resources being attributed to the " +
                                "wrong server/service.\n";

                        msg += "datasetUrl: "+datasetUrl+"\n";
                        msg += "  coverageID(repository) : "+coverageID+"\n";
                        msg += "  coverageID(in-memory):   "+wcsIDs.get(datasetUrl)+"\n";

                        log.error(msg);
                    }

                    //private ConcurrentHashMap<String, String> serverIDs = new ConcurrentHashMap<String,String>();
                    //private ConcurrentHashMap<String, String> wcsIDs = new ConcurrentHashMap<String,String>();


                }
            }

        } catch (MalformedURLException e) {
            log.error("updateIdCaches(): Caught MalformedURLException. msg: "
                    +e.getMessage());
        }
        finally {
            genLock.unlock();
        }

    }




}
