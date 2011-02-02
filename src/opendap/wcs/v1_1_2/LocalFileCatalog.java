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

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 6, 2009
 * Time: 9:06:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalFileCatalog implements WcsCatalog {


    private  Logger log;

    private  String _catalogDir;
    private  String _capabilitiesMetadataDir;

    //private static boolean _useMemoryCache;
    private  Date _cacheTime;
    private  long _lastModified;

    private  boolean  intitialized = false;

    private  ConcurrentHashMap<String,CoverageDescription> coverages = new  ConcurrentHashMap<String,CoverageDescription>();



    private  ReentrantReadWriteLock _catalogLock;


    public String getDataAccessUrl(String coverageID){
        return coverageID;
    }

    public LocalFileCatalog(){
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        _catalogLock = new ReentrantReadWriteLock();

    }


    /**
     *
               <WcsCatalog className="opendap.wcs.v1_1_2.LocalFileCatalog">
                    <ServiceIdentification>true</ServiceIdentification>
                    <ServiceProvider>true</ServiceProvider>
                    <OperationsMetadata>true</OperationsMetadata>
                    <CoveragesDirectory>true</CoveragesDirectory>
                </WcsCatalog>

     * @param configFile
     * @throws Exception
     */

    public void init(URL configFile, String persistentContentPath, String contextPath) throws Exception{

        if(intitialized)
            return;


        Element e1, e2;
        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        SAXBuilder sb = new SAXBuilder();
        Element configFileRoot = sb.build(configFile).getRootElement();

        Element catalogConfig = configFileRoot.getChild("WcsCatalog");

        e1 = catalogConfig.getChild("CoveragesDirectory");
        if(e1==null){
            msg = "Cannot find CoveragesDirectory element in " +
                    "configuration element: "+ xmlo.outputString(catalogConfig);
            log.error(msg);
            throw new IOException(msg);
        }
        _catalogDir =  e1.getTextTrim();


        log.debug("WCS Coverages Directory: " + _catalogDir);

        ingestCatalog();

        _cacheTime = new Date();

        intitialized = true;



    }



    public void init(String capabilitiesMetadataDir,String catalogDir) throws Exception{

        if(intitialized)
            return;

        //Catalog._useMemoryCache =  useMemoryCache;
        _catalogDir =  catalogDir;
        log.debug("WCS Coverages Directory: " + _catalogDir);

        ingestCatalog();



        _cacheTime = new Date();

        intitialized = true;
    }






    private void ingestCatalog() throws Exception  {

        String msg;

        File catalogDir = new File(_catalogDir);

        if(!catalogDir.exists()){
            msg = "Cannot find directory: "+ catalogDir;
            log.error(msg);
            throw new IOException(msg);
        }

        if(!catalogDir.canRead()){
            msg = "Cannot read from directory: "+ catalogDir;
            log.error(msg);
            throw new IOException(msg);
        }
        if(!catalogDir.isDirectory()){
            msg = "WCS Coverages directory "+ catalogDir +" is not actually a directory.";
            log.error(msg);
            throw new IOException(msg);
        }



        class DotFileFilter implements FilenameFilter{
           public boolean accept(File dir, String name) {
               if(name.startsWith(".") ) return false;
               return true;
           }
       }

        for(File cd:catalogDir.listFiles( new DotFileFilter())){
            ingestCoverageDescription(cd);
        }

        _lastModified = catalogDir.lastModified();

        log.info("Ingested WCS Coverages directory: "+_catalogDir);


    }
    private void ingestCoverageDescription(File file) throws Exception  {

        CoverageDescription cd;
        String msg;
        cd = new CoverageDescription(file);
        coverages.put(cd.getIdentifier(),cd);
        log.info("Ingested CoverageDescription: "+cd.getIdentifier());
    }

    private void ingestCoverageDescription(Element cde, long lastModified) throws Exception  {

        CoverageDescription cd;
        String msg;
        cd = new CoverageDescription(cde,lastModified);
        coverages.put(cd.getIdentifier(),cd);
        log.info("Ingested CoverageDescription: "+cd.getIdentifier());
    }






    public boolean hasCoverage(String id){

        log.debug("Looking for a coverage with ID: "+id);

        return coverages.containsKey(id);
    }


    public  Element getCoverageDescriptionElement(String id)  {

        return coverages.get(id).getElement();
    }

    public List<Element> getCoverageDescriptionElements() throws WcsException {
        throw new WcsException("getCoverageDescriptionElements() method Not Implemented",WcsException.NO_APPLICABLE_CODE);
    }


    public  CoverageDescription getCoverageDescription(String id)  {
        return coverages.get(id);
    }


    public  Element getCoverageSummaryElement(String id) throws WcsException {
        return coverages.get(id).getCoverageSummary();
    }

    public  List<Element> getCoverageSummaryElements() throws WcsException {


        ArrayList<Element> coverageSummaries = new ArrayList<Element>();

        Enumeration e = coverages.elements();

        CoverageDescription cd;

        while(e.hasMoreElements()){
            cd = (CoverageDescription)e.nextElement();
            coverageSummaries.add(cd.getCoverageSummary());

        }

        return coverageSummaries;
    }


    public  List<Element> getSupportedFormatElements()  {


        ArrayList<Element> supportedFormats = new ArrayList<Element>();
        HashMap<String,Element> uniqueFormats = new HashMap<String,Element>();

        Enumeration enm = coverages.elements();
        Element e;
        Iterator i ;
        CoverageDescription cd;

        // Get all of the unique formats.
        while(enm.hasMoreElements()){
            cd = (CoverageDescription)enm.nextElement();

            i = cd.getSupportedFormatElements().iterator();

            while(i.hasNext()){
                e = (Element) i.next();
                uniqueFormats.put(e.getTextTrim(),e);
            }
        }

        i =  uniqueFormats.values().iterator();
        while(i.hasNext()){
            supportedFormats.add((Element)i.next());
        }


        return supportedFormats;
    }
    public  List<Element> getSupportedCrsElements()  {


        ArrayList<Element> supportedCRSs = new ArrayList<Element>();
        HashMap<String,Element> uniqueCRSs = new HashMap<String,Element>();

        Enumeration enm = coverages.elements();
        Element e;
        Iterator i ;
        CoverageDescription cd;

        // Get all of the unique formats.
        while(enm.hasMoreElements()){
            cd = (CoverageDescription)enm.nextElement();

            i = cd.getSupportedCrsElements().iterator();

            while(i.hasNext()){
                e = (Element) i.next();
                uniqueCRSs.put(e.getTextTrim(),e);
            }
        }

        i =  uniqueCRSs.values().iterator();
        while(i.hasNext()){
            supportedCRSs.add((Element)i.next());
        }


        return supportedCRSs;
    }

    public String getLatitudeCoordinateDapId(String coverageId, String fieldId) {
        return null;
    }

    public String getLongitudeCoordinateDapId(String coverageId, String fieldId) {
        return null;
    }

    public String getElevationCoordinateDapId(String coverageId, String fieldId) {
        return null;
    }

    public String getTimeCoordinateDapId(String coverageId, String fieldId) {
        return null;
    }


    public long getLastModified(){

        /*
        long mostRecent;


        Enumeration enm = coverages.elements();
        CoverageDescription cd;

        try {
            updateCapabilitiesMetadata();
        }
        catch(Exception e){
            log.error("Failed to update Capabilities Metadata!");
        }
        mostRecent = _cacheTime.getTime();

        while(enm.hasMoreElements()){
            cd = (CoverageDescription)enm.nextElement();

            if(mostRecent < cd.lastModified()){
                mostRecent = cd.lastModified();
            }
        }
        return mostRecent;
        */
        return _lastModified;
    }



    public void destroy(){}


    public void update(){
    }


}