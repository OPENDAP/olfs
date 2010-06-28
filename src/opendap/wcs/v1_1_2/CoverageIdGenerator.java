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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jun 24, 2010
 * Time: 9:18:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoverageIdGenerator {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(CoverageIdGenerator.class);

    private static ConcurrentHashMap<String, String> serverIDs = new ConcurrentHashMap<String,String>();
    private static ConcurrentHashMap<String, String> wcsIDs = new ConcurrentHashMap<String,String>();

    private static ReentrantLock genLock = new  ReentrantLock();


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
     * function getWcsID
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
