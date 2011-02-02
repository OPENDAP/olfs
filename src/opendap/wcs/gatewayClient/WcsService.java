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
package opendap.wcs.gatewayClient;

import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpClient;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.InputStream;
import java.io.IOException;


/**
 * User: ndp
 * Date: Mar 12, 2008
 * Time: 4:35:34 PM
 */
public class WcsService {
    private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private Element config;

    private Document capabilitiesDocument;
    private Date capabilitiesDocumentTimeAcquired;

    //private Document coverageDescriptionDocument;
    //private Date coverageDescriptionDocumentTimeAcquired;


    private long cacheTime;
    private ReentrantReadWriteLock capablitiesDocLock;
    //private ReentrantReadWriteLock coverageDescDocLock;

    private HashMap<String,Element> coverages;
    private UpdateCapabilitiesThread  capabilitiesUpdater;
    //private UpdateCoverageThread  coverageUpdater;






    /**
     * A value of 1000 makes the cacheTime attribute's units seconds.
     * A value of 1 would make the cachTime attributes's units milliseconds.
     */
    private static int SECONDS = 1000;




    public WcsService(Element configuration) throws Exception{

        log.debug("Configuring...");

        config = (Element) configuration.clone();

        config();

        log.debug("\n"+this);

        coverages = new HashMap<String,Element>();


        capablitiesDocLock = new ReentrantReadWriteLock();
        log.debug("Created capablitiesDocLock.");

        log.debug("Creating UpdateCapabilitiesThread thread.");
        capabilitiesUpdater = new UpdateCapabilitiesThread();
        log.debug("Created UpdateCapabilitiesThread thread.");


        log.debug("Calling UpdateCapabilitiesThread.update(true)...");
        capabilitiesUpdater.update(true);
        log.debug("UpdateCapabilitiesThread.update(true) completed");
        capabilitiesDocumentTimeAcquired = new Date();

        capabilitiesUpdater.start();


/*

        coverageDescDocLock = new ReentrantReadWriteLock();
        log.debug("Created coverageDescDocLock.");

        log.debug("Creating UpdateCoverageThread thread.");
        coverageUpdater = new UpdateCoverageThread();
        log.debug("Created UpdateCoverageThread thread.");


        log.debug("Calling UpdateCoverageThread.update(true)...");
        coverageUpdater.update(true);
        log.debug("UpdateCoverageThread.update(true) completed");
        coverageDescriptionDocumentTimeAcquired = new Date();

        coverageUpdater.start();

*/


    }


    public ReentrantReadWriteLock.ReadLock getReadLock(){
        return capablitiesDocLock.readLock();
    }




    public Document getCapabilitiesDocument(){
        return capabilitiesDocument;
    }




    public String OLDgetWcsRequestURL(Site site,
                                WcsCoverageOffering coverage,
                                String dateName) {



        //"http://g0dup05u.ecs.nasa.gov/cgi-bin/ceopAIRX2RET?
        // service=WCS
        // &version=1.0.0
        // &request=GetCoverage
        // &coverage=TSurfAir
        // &TIME=2002-10-01

        String url = getServiceURL();

        if(!url.endsWith("?"))
            url += "?";


        url += "service="+getService();
        url += "&version="+getVersion();
        url += "&request=GetCoverage";
        url += "&coverage="+coverage.getName();
        if(dateName!=null)
            url += "&TIME="+dateName;



        // &crs=WGS84
        // &bbox=-107.375000,51.625000,-102.625000,56.375000
        // &format=netCDF
        // &resx=0.25
        // &resy=0.25
        // &interpolationMethod=Nearest%20neighbor"/>

        List params = site.getWCSParameters();

        for (Object param1 : params) {
            Element param = (Element) param1;
            if (!param.getName().equals("time"))
                url += "&" + param.getName() + "=" + param.getTextTrim();
        }


        log.debug("WCS REQUEST URL: "+ url);
        return url;

    }



    public String getWcsRequestURL(Site site,
                                WcsCoverageOffering coverage,
                                String dateName) {



        //"http://g0dup05u.ecs.nasa.gov/cgi-bin/ceopAIRX2RET?
        // service=WCS
        // &version=1.0.0
        // &request=GetCoverage
        // &coverage=TSurfAir
        // &TIME=2002-10-01

        String url = getServiceURL();

        if(!url.endsWith("?"))
            url += "?";


        url += "service="+getService();
        url += "&version="+getVersion();
        url += "&request=GetCoverage";

        url += "&coverage="+coverage.getName();

        //url += "&"+coverage.getSpatialDomainConstraint();


        if(dateName!=null)
            url += "&TIME="+dateName;



        // &crs=WGS84
        // &bbox=-107.375000,51.625000,-102.625000,56.375000
        // &format=netCDF
        // &resx=0.25
        // &resy=0.25
        // &interpolationMethod=Nearest%20neighbor"/>

        List params = site.getWCSParameters();

        for (Object param1 : params) {
            Element param = (Element) param1;
            if (!param.getName().equals("time"))
                url += "&" + param.getName() + "=" + param.getTextTrim();
        }

        log.debug("WCS REQUEST URL: "+ url);
        return url;

    }






    public void destroy(){
        capabilitiesUpdater.interrupt();
        //coverageUpdater.interrupt();
        log.debug("Destroyed");
    }





/*


    public WcsCoverageOffering getCoverageOffering(String coverageName) throws Exception {

        Element coverageElement  = coverages.get(coverageName);
        if(coverageElement==null){
            throw new Exception("Coverage \""+coverageName+"\" is not " +
                    "avaliable on the WCS Service: "+getName());
        }
        return new WcsCoverageOffering(coverageElement);

    }

*/



    public WcsCoverageOffering getCoverageOffering(String hashedName) throws Exception{
        Element coverageOfferingBrief = coverages.get(hashedName);

        if(coverageOfferingBrief==null){
            return null;
        }

        String[] name = new String[1];
        name[0] = coverageOfferingBrief.getChild(WCS.NAME,WCS.NS).getTextTrim();

        Document doc = httpDescribeCoverage(name);
        Element coverageOffering = doc.getRootElement().getChild(WCS.COVERAGE_OFFERING,WCS.NS);
        return new WcsCoverageOffering(coverageOffering);
    }


    public Document httpDescribeCoverage(String[] coverages) throws Exception {

        HttpClient httpClient = new HttpClient();

        String q;
        if(getServiceURL().endsWith("?"))
            q="";
        else
            q="?";

        String requestURI = getServiceURL() + q +
                         "service="+getService()+
                         "&version="+getVersion()+
                         "&request=DescribeCoverage";

        if(coverages != null){
            String coverageString = "&coverage=";
            for(int i=0; i< coverages.length ; i++){
                if(i>0)
                    coverageString += ",";
                coverageString += coverages[i];
            }
            requestURI+=coverageString;
        }


        log.debug("requestURI: "+requestURI);

        GetMethod request = new GetMethod(requestURI);


        try {
            // Execute the method.
            int statusCode = httpClient.executeMethod(request);

            if (statusCode != HttpStatus.SC_OK) {
              log.error("Method failed: " + request.getStatusLine());
            }

            // Parse the XML doc into a Document object.
            SAXBuilder sb = new SAXBuilder();
            InputStream is = request.getResponseBodyAsStream();
            try {
                Document doc = sb.build(is);
                log.debug("Got and parsed coverage description document.");
                //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                //xmlo.output(doc, System.out);
                return doc;
           }
            finally {
                try{
                    is.close();
                }
                catch(IOException e){
                    log.error("Failed to InputStream for "+requestURI+" Error MEssage: "+e.getMessage());
                }

            }



        }
        finally {
            request.releaseConnection();
        }


    }



    /**
     *
     */
    private class UpdateCapabilitiesThread extends Thread {

        private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
        private HttpClient httpClient;

        UpdateCapabilitiesThread(){
            super();
            log.debug("Constructor.");
            httpClient = new HttpClient();
        }



        public void run() {
            log.info("Starting.");


            boolean done = false;

            while(!done) {

                try {
                    update(false);
                    done = interrupted();
                    Thread.sleep(cacheTime);
                } catch (InterruptedException e) {
                    log.info("Interrupted Exception.");
                    done = true;

                }
            }
            log.info("Exiting");

        }

        public long getCacheAge(){
            Date now = new Date();
            return now.getTime() - capabilitiesDocumentTimeAcquired.getTime();
        }

        private void update(boolean force){

            int biffCount=0;
            log.debug("Attempting to update cached " +
                    "capablities document. force="+force);

            if(force || cacheTime<getCacheAge()){

            	ReentrantReadWriteLock.WriteLock lock = capablitiesDocLock.writeLock();

                try {
                	lock.lock();
                	log.debug("capablitiesDocLock WriteLock Acquired.");
                    if(force || cacheTime<getCacheAge()){
                        log.debug("Updating Capabilities Document.");
                        Document doc = httpGetCapabilities();
                        ingestCapabilitiesDocument(doc);
                    }
                }
                catch(Exception e){
                    log.error("I has a problem: "+
                            e.getMessage() +
                            " biffCount: "+ (++biffCount) );
                }
                finally {
                    lock.unlock();
                    log.debug("WriteLock Released.");
                }

            }

        }


        public Document httpGetCapabilities() throws Exception {


            String q;
            if(getServiceURL().endsWith("?"))
                q="";
            else
                q="?";

            String requestURI = getServiceURL() + q +
                             "service="+getService()+
                             "&version="+getVersion()+
                             "&request=GetCapabilities";


            log.debug("requestURI: "+requestURI);

            GetMethod request = new GetMethod(requestURI);

            log.debug("HttpClient: "+httpClient);


            try {
                // Execute the method.
                int statusCode = httpClient.executeMethod(request);

                if (statusCode != HttpStatus.SC_OK) {
                  log.error("Method failed: " + request.getStatusLine());
                }

                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                InputStream is = request.getResponseBodyAsStream();
                try {
                    Document doc = sb.build(is);
                    log.debug("Got and parsed capabilites document.");
                    //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                    //xmlo.output(doc, System.out);
                    return doc;
                }
                finally {
                    try{
                        is.close();
                    }
                    catch(IOException e){
                        log.error("Failed to InputStream for "+requestURI+" Error MEssage: "+e.getMessage());
                    }
                }


            }
            finally {
                log.debug("Releasing Http connection.");
                request.releaseConnection();
            }


        }


    }





    /*




    private class UpdateCoverageThread extends Thread {
        private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
        private HttpClient httpClient = new HttpClient();

        UpdateCoverageThread(){
            super();
            log.debug("Creating.");
        }



        public void run() {
            log.info("Starting.");

            boolean done = false;

            while(!done) {

                try {
                    update(false);
                    done = interrupted();
                    Thread.sleep(cacheTime);
                } catch (InterruptedException e) {
                    log.info("Caught Interrupted " +
                            "Exception.");
                    done = true;

                }
            }
            log.info("Exiting");
        }

        public long getCatalogAge(){
            Date now = new Date();
            return now.getTime() - coverageDescriptionDocumentTimeAcquired.getTime();
        }

        public void update(boolean force){

            int biffCount=0;

            log.debug("Attempting to update cached " +
                    "coverage description document. force="+force);
            if(force || cacheTime<getCatalogAge()){

                coverageDescDocLock.writeLock().lock();
                log.debug("WriteLock Acquired.");

                try {
                    if(force || cacheTime<getCatalogAge()){
                        log.debug("Updating Coverage Description Document.");
                        Document doc = httpDescribeCoverage(null);
                        ingestCoverageDesription(doc);
                    }

                }
                catch(Exception e){
                    log.error("I HAS A PROBLEM: "+
                            e.getMessage() +
                            " biffCount: "+ (++biffCount) );
                }
                finally {
                    coverageDescDocLock.writeLock().unlock();
                    log.debug("WriteLock Released..");
                }

            }

        }



    }

*/

    /*

    public void ingestCoverageDesription(Document doc){

        coverageDescriptionDocument = doc;

        Element coverageOffering;
        String name, hashedName;
        List coverageList = coverageDescriptionDocument.getRootElement().getChildren(WCS.COVERAGE_OFFERING,WCS.NS);
        for (Object cvrgOffrElem : coverageList) {
            coverageOffering = (Element) cvrgOffrElem;

            name = coverageOffering.getChild(WCS.NAME, WCS.NS).getTextTrim();

            hashedName = getHashedName(name);

            log.debug("Adding coverage "+name+" to coverage HashMap. hashMapName: "+hashedName);
            coverages.put(hashedName, coverageOffering);


            log.debug("Adding link name "+hashedName+" to CoverageOffering Element.");
            Element link  = new Element("DapLink",WCS.DAPWCS_NS);
            link.setText(hashedName);
            coverageOffering.addContent(link);
        }


    }

*/




    public void ingestCapabilitiesDocument(Document doc){

        capabilitiesDocument = doc;

        Element coverageOfferingBrief, contentMetatdata;
        String name;
        //String hashedName;
        contentMetatdata = capabilitiesDocument.getRootElement().getChild(WCS.CONTENT_METADATA,WCS.NS);
        List coverageList = contentMetatdata.getChildren(WCS.COVERAGE_OFFERING_BRIEF,WCS.NS);
        for (Object cvrgOffrElem : coverageList) {
            coverageOfferingBrief = (Element) cvrgOffrElem;

            name = coverageOfferingBrief.getChild(WCS.NAME, WCS.NS).getTextTrim();


            /*
            hashedName = getHashedName(name);


            log.debug("Adding coverageOfferingBrief "+name+" to coverage HashMap. hashMapName: "+hashedName);
            coverages.put(hashedName, coverageOfferingBrief);


            log.debug("Adding link name "+hashedName+" to CoverageOfferingBrief Element.");
            Element link  = new Element("DapLink",WCS.DAPWCS_NS);
            link.setText(hashedName);
            coverageOfferingBrief.addContent(link);

            */




            log.debug("Adding coverageOfferingBrief "+name+" to coverage HashMap. hashMapName: "+name);
            coverages.put(name, coverageOfferingBrief);


            log.debug("Adding link name "+name+" to CoverageOfferingBrief Element.");
            Element link  = new Element("DapLink",WCS.DAPWCS_NS);
            link.setText(name);
            coverageOfferingBrief.addContent(link);
        }


    }

    public static String getHashedName(String name){
        String s = "";

        byte[] b =  name.getBytes();
        for (byte aB : b) {
            s += Integer.toHexString(aB);
        }
        return s;
    }








    private void config() throws Exception{

        Attribute attr;
        String s;

        if(!config.getName().equals("WCSService"))
            throw new Exception("Cannot build a "+getClass()+" using " +
                    "<"+config.getName()+"> element.");


        attr = config.getAttribute("name");
        if(attr==null)
            throw new Exception("Missing \"name\" attribute. " +
                    "<WCSService> elements must have a " +
                    "name attribute.");
        log.debug("name: "+attr.getValue());

        attr = config.getAttribute("autoUpdate");
        if(attr==null)
            setAutoUpdate(false);


        if(autoUpdate()){
            attr = config.getAttribute("cacheTime");
            if(attr==null)
                throw new Exception("Missing \"cacheTime\" attribute. " +
                        "<WCSService> elements must have a " +
                        "name attribute if the \"autoUpdate\" element is" +
                        "presentand set to 'true'");
            log.debug("cacheTime: "+attr.getValue());
            cacheTime = Integer.parseInt(attr.getValue());
            cacheTime = cacheTime * SECONDS;
        }
        else
            cacheTime = 600 * SECONDS;


        Element elm = config.getChild("ServiceURL");
        if(elm==null)
            throw new Exception("Missing <ServiceURL> element. " +
                    "<WCSService name=\""+getName()+"\"> elements must have a " +
                    "<ServiceURL>" +
                    " child element.");
        s = elm.getTextTrim();
        if(s==null)
            throw new Exception("Missing <ServiceURL> content. " +
                    "<WCSService name=\""+getName()+"\"> elements must have a " +
                    "valid <ServiceURL>" +
                    " child element.");
        s= getServiceURL();
        log.debug("ServiceURI: "+s);



        elm = config.getChild("service");
        if(elm==null)
            throw new Exception("Missing <service> element. " +
                    "<WCSService name=\""+getName()+"\"> elements must have a " +
                    "<service>" +
                    " child element.");
        s = getService();
        log.debug("service: "+s);



        elm = config.getChild("version");
        if(elm==null)
            throw new Exception("Missing <version> element. " +
                    "<WCSService name=\""+getName()+"\"> elements must have a " +
                    "<version>" +
                    " child element.");
        s=getVersion();
        log.debug("version: "+s);






    }



    public String getName() {
        return config.getAttributeValue("name");
    }

    public boolean autoUpdate(){
        String s;

        s = config.getAttributeValue("autoUpdate");

        return Boolean.parseBoolean(s);
    }

    public void setAutoUpdate(boolean val){
        config.setAttribute("autoUpdate",val+"");
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public String getVersion() {
        return config.getChild("version").getTextTrim();
    }

    public String getService()  {
        return config.getChild("service").getTextTrim();
    }


    public String getServiceURL() {
        return config.getChild("ServiceURL").getTextTrim();

    }








    public String toString(){

        String s = "WcsService: \n";
        try {
            s += "    name:        " + getName() + "\n";
            s += "    serviceURL:  " + getServiceURL() + "\n";
            s += "    service:     " + getService() + "\n";
            s += "    version:     " + getVersion() + "\n";
            s += "    autoUpdate:  " + autoUpdate() + "\n";
            s += "    cacheTime:   " + getCacheTime() + "\n";
        }
        catch (Exception e) {
            log.error(e.getMessage() + "\n"+s);
        }
        return s;

    }

}
