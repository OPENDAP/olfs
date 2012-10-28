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
package opendap.wcs.v2_0;


import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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


    public static final String NamespaceName = "http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#";
    public static final String NamespacePrefix = "lfc";
    public static final Namespace NS = Namespace.getNamespace(NamespacePrefix,NamespaceName);



    private  Logger log;

    private boolean validateContent = false;

    private  String _catalogDir;
    private  String _catalogConfigFile;
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
               <WcsCatalog className="opendap.wcs.v2_0.LocalFileCatalog">
                    <ServiceIdentification>true</ServiceIdentification>
                    <ServiceProvider>true</ServiceProvider>
                    <OperationsMetadata>true</OperationsMetadata>
                    <CoveragesDirectory>true</CoveragesDirectory>
                </WcsCatalog>

     * @param config
     * @throws Exception
     */

    public void init(Element config, String persistentContentPath, String contextPath) throws Exception{

        if(intitialized)
            return;


        Element e1;
        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        e1 = config.getChild("CatalogDirectory");
        if(e1==null){

            String defaultCatalogDirectory = persistentContentPath + "/"+ this.getClass().getSimpleName();

            File defaultCatDir = new File(defaultCatalogDirectory);

            if(!defaultCatDir.exists()){
                if(!defaultCatDir.mkdirs()){
                    msg = "Default Coverages Directory ("+defaultCatalogDirectory+")does not exist and cannot be " +
                            "created. Could not find CoveragesDirectory element in " +
                            "configuration element: "+ xmlo.outputString(config);
                    log.error(msg);
                    throw new IOException(msg);
                }
            }
            _catalogDir = defaultCatalogDirectory;
        }
        else {
            _catalogDir =  e1.getTextTrim();
        }
        log.debug("WCS-2.0 Coverages Directory: " + _catalogDir);



        e1 = config.getChild("CatalogFile");
        if(e1==null){
            _catalogConfigFile = _catalogDir + "/LFC.xml";
        }
        else {
            _catalogConfigFile =  e1.getTextTrim();
        }
        log.debug("CatalogFile: " + _catalogConfigFile);



        ingestCatalog();

        _cacheTime = new Date();

        intitialized = true;



    }


    /**
     *
     *
     *
     *
     *
     *
     *
     <WcsCoverage
             fileName="200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml.xml"
             coverageID="ioos/200803061600_HFRadar_USEGC_6km_rtv_SIO.nc">

         <field name="u">
             <DapIdOfLatitudeCoordinate>u.longitude</DapIdOfLatitudeCoordinate>
             <DapIdOfLongitudeCoordinate>u.latitude</DapIdOfLongitudeCoordinate>
             <DapIdOfTimeCoordinate>u.time</DapIdOfTimeCoordinate>
         </field>

         <field name="v">
             <DapIdOfLatitudeCoordinate>v.longitude</DapIdOfLatitudeCoordinate>
             <DapIdOfLongitudeCoordinate>v.latitude</DapIdOfLongitudeCoordinate>
             <DapIdOfTimeCoordinate>v.time</DapIdOfTimeCoordinate>
         </field>
         <field name="DOPx">
             <DapIdOfLatitudeCoordinate>DOPx.longitude</DapIdOfLatitudeCoordinate>
             <DapIdOfLongitudeCoordinate>DOPx.latitude</DapIdOfLongitudeCoordinate>
             <DapIdOfTimeCoordinate>DOPx.time</DapIdOfTimeCoordinate>
         </field>
         <field name="DOPy">
             <DapIdOfLatitudeCoordinate>DOPy.longitude</DapIdOfLatitudeCoordinate>
             <DapIdOfLongitudeCoordinate>DOPy.latitude</DapIdOfLongitudeCoordinate>
             <DapIdOfTimeCoordinate>DOPy.time</DapIdOfTimeCoordinate>
         </field>
     </WcsCoverage>


     * @throws java.io.IOException
     * @throws org.jdom.JDOMException
     */
    private void ingestCatalog() throws IOException, JDOMException {

        String msg;

        // QC the Catalog directory.
        File catalogDir = new File(_catalogDir);
        if(!opendap.wcs.v2_0.Util.isReadableDir(catalogDir)){
            msg = "ingestCatalog(): Catalog directory "+ catalogDir +" is not accessible.";
            log.error(msg);
            throw new IOException(msg);
        }

        // QC the Catalog file.
        File catalogFile = new File(_catalogConfigFile);
        if(!opendap.wcs.v2_0.Util.isReadableFile(catalogFile)) {
            msg = "ingestCatalog(): Catalog File "+ _catalogConfigFile +" is not accessible.";
            log.error(msg);
            throw new IOException(msg);
        }

        Element lfcConfig = opendap.xml.Util.getDocumentRoot(catalogFile);


        boolean validate = false;
        String validateAttr = lfcConfig.getAttributeValue("validateCatalog");
        if(validateAttr!=null){
            validate = validateAttr.equalsIgnoreCase("true") || validateAttr.equalsIgnoreCase("on");
        }

        validateContent = validate;




        for (Object o : lfcConfig.getChildren("WcsCoverage", NS)) {
            Element wcsCoverageConfig = (Element) o;
            ingestCoverageDescription(wcsCoverageConfig,  validateContent);
        }

        _lastModified = catalogFile.lastModified();

        log.info("Ingested WCS Coverages directory: "+_catalogDir);


    }



    private void ingestCoverageDescription(Element wcsCoverageConfig, boolean validateContent)  {

        String msg;
        CoverageDescription coverageDescription = null;
        try {
            coverageDescription = new CoverageDescription(wcsCoverageConfig, _catalogDir, validateContent);
        } catch (JDOMException e) {
            msg = "ingestCoverageDescription(): CoverageDescription file either did not parse or did not validate. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        } catch (IOException e) {
            msg = "ingestCoverageDescription(): Attempting to access CoverageDescription file  generated an IOException. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        } catch (ConfigurationException e) {
            msg = "ingestCoverageDescription(): Encountered a configuration error in the configuration file "+ _catalogConfigFile +" Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        }

        if(coverageDescription!=null){
            String coverageId = coverageDescription.getCoverageId();
            coverages.put(coverageId,coverageDescription);
        }

    }




    public boolean hasCoverage(String id){

        log.debug("Looking for a coverage with ID: "+id);

        return coverages.containsKey(id);
    }


    public  Element getCoverageDescriptionElement(String id) throws WcsException{

        CoverageDescription coverage = coverages.get(id);

        if(coverage==null){
            throw new WcsException("No such coverage.",
                    WcsException.INVALID_PARAMETER_VALUE,"wcs:CoverageId");

        }

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