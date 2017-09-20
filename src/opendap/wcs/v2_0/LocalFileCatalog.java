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
package opendap.wcs.v2_0;


import opendap.coreServlet.Scrub;
import org.apache.http.client.CredentialsProvider;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.annotation.*;


/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 6, 2009
 * Time: 9:06:40 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name="LocalFileCatalog")
public class LocalFileCatalog implements WcsCatalog {


    public static final String NamespaceName = "http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#";
    public static final String NamespacePrefix = "lfc";
    public static final Namespace NS = Namespace.getNamespace(NamespacePrefix,NamespaceName);

    private CredentialsProvider _credsProvider;


    private  Logger log;

    @XmlAttribute(name = "validateCatalog")
    private boolean validateContent = false;

    private  String _catalogDir;
    private  String _catalogConfigFile;
    private  String _capabilitiesMetadataDir;

    //private static boolean _useMemoryCache;
    private  Date _cacheTime;
    private  long _lastModified;

    private  boolean  intitialized = false;

    private  ConcurrentHashMap<String,CoverageDescription> coveragesMap = new  ConcurrentHashMap<>();
    private  ConcurrentHashMap<String,EOCoverageDescription> eoCoveragesMap = new  ConcurrentHashMap<>();
    private  ConcurrentHashMap<String,EODatasetSeries> datasetSeriesMap = new  ConcurrentHashMap<>();



    private  ReentrantReadWriteLock _catalogLock;


    @Override
    public String getDataAccessUrl(String coverageID) {

        CoverageDescription cd = coveragesMap.get(coverageID);

        if(cd==null)
            return null;

        URL dataAccessUrl = cd.getDapDatasetUrl();

        return dataAccessUrl.toExternalForm();
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

    @Override
    public void init(Element config, String persistentContentPath, String contextPath) throws Exception{

        if(intitialized)
            return;


        Element e1;
        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        e1 = config.getChild("CatalogDirectory");
        if(e1==null){

            String defaultCatalogDirectory = persistentContentPath + this.getClass().getSimpleName();

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


        e1 = config.getChild("Credentials");
        if(e1==null){
            _credsProvider = opendap.http.Util.getNetRCCredentialsProvider();
            log.debug("Using Credentials file: ~/.netrc");
        }
        else {
            String credsFilename  = e1.getTextTrim();
            _credsProvider = opendap.http.Util.getNetRCCredentialsProvider(credsFilename,true);
            log.debug("Using Credentials file: {}",credsFilename );
        }

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
        if(!Util.isReadableDir(catalogDir)){
            msg = "ingestCatalog(): Catalog directory "+ catalogDir +" is not accessible.";
            log.error(msg);
            throw new IOException(msg);
        }

        // QC the Catalog file.
        File catalogFile = new File(_catalogConfigFile);
        if(!Util.isReadableFile(catalogFile)) {
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

        int i=0;
        for (Object o : lfcConfig.getChildren(CoverageDescription.CONFIG_ELEMENT_NAME, NS)) {
            Element wcsCoverageConfig = (Element) o;
            log.debug("Processing {}[{}]",CoverageDescription.CONFIG_ELEMENT_NAME,i++);
            ingestCoverageDescription(wcsCoverageConfig,  validateContent);
        }
        i=0;
        for (Object o : lfcConfig.getChildren(EOCoverageDescription.CONFIG_ELEMENT_NAME, NS)) {
            Element eoWcsCoverageConfig = (Element) o;
            log.debug("Processing {}[{}]",EOCoverageDescription.CONFIG_ELEMENT_NAME,i++);
            ingestEOCoverageDescription(eoWcsCoverageConfig,  validateContent);
        }
        i=0;
        for (Object o : lfcConfig.getChildren(EODatasetSeries.CONFIG_ELEMENT_NAME, NS)) {
            Element eoDatasetSeries = (Element) o;
            log.debug("Processing {}[{}]",EODatasetSeries.CONFIG_ELEMENT_NAME,i++);
            ingestDatasetSeries(eoDatasetSeries,  validateContent);
        }

        _lastModified = catalogFile.lastModified();

        log.info("Ingested WCS Catalog Configuration: {}",catalogFile);


    }

    private void ingestDatasetSeries(Element eowcsDatasetSeriesConfig, boolean validateContent)  {

        String msg;
        EODatasetSeries eoDatasetSeries = null;
        try {
            eoDatasetSeries = new EODatasetSeries(eowcsDatasetSeriesConfig, _catalogDir, validateContent);
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
        } catch (WcsException e) {
            msg = "ingestCoverageDescription(): When ingesting the CoverageDescription it failed a validation test. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        }



        Vector<EOCoverageDescription> processed = new Vector<>();


        if(eoDatasetSeries!=null){
            String datasetSeriesId = eoDatasetSeries.getId();
            boolean conflict = false;

            for(EOCoverageDescription eocd:eoDatasetSeries.getMembers()) {

                String coverageId = eocd.getCoverageId();

                CoverageDescription cd = coveragesMap.get(coverageId)==null?eoCoveragesMap.get(coverageId):coveragesMap.get(coverageId);
                if(cd!=null) {
                    StringBuilder sb = new StringBuilder("ingestDatasetSeries() -");
                    sb.append(" SKIPPING new coverage with duplicate coverageID '").append(coverageId);
                    sb.append("' and dataset URL: ").append(eocd.getDapDatasetUrl());
                    sb.append(" The coverageId is already in use and associated with dataset URL: ").append(cd.getDapDatasetUrl());
                    log.error(sb.toString());
                    conflict = true;

                }
                else {
                    coveragesMap.put(coverageId, eocd);
                    eoCoveragesMap.put(coverageId, eocd);
                    processed.add(eocd);
                }
            }

            if(conflict) {

                for(EOCoverageDescription eocd : processed){
                    coveragesMap.remove(eocd);
                    eoCoveragesMap.remove(eocd);

                }


                StringBuilder sb = new StringBuilder("ingestDatasetSeries() - ");
                sb.append("CoverageId conflicts were found in the DatasetSeries '").append(datasetSeriesId).append("' ");
                sb.append(" !!SKIPPING!");
                log.error(sb.toString());

            }
            else {
                datasetSeriesMap.put(datasetSeriesId, eoDatasetSeries);
                log.info("ingestDatasetSeries() - Ingested EODatasetSeries '{}'",datasetSeriesId);
            }
        }

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
        } catch (WcsException e) {
            msg = "ingestCoverageDescription(): When ingesting the CoverageDescription it failed a validation test. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        }

        if(coverageDescription!=null){
            String coverageId = coverageDescription.getCoverageId();

            CoverageDescription cd = coveragesMap.get(coverageId);
            if(cd!=null){
                StringBuilder sb = new StringBuilder("ingestCoverageDescription() -");
                sb.append(" SKIPPING new coverage with duplicate coverageID '").append(coverageId);
                sb.append("' and dataset URL: ").append(coverageDescription.getDapDatasetUrl());
                sb.append(" The coverageId is already in use and associated with dataset URL: ").append(cd.getDapDatasetUrl());
                log.error(sb.toString());

            }
            else {
                coveragesMap.put(coverageId, coverageDescription);
            }
        }





    }



    private void ingestEOCoverageDescription(Element wcsCoverageConfig, boolean validateContent)  {

        String msg;
        EOCoverageDescription eoCoverageDescription = null;
        try {
            eoCoverageDescription = new EOCoverageDescription(wcsCoverageConfig, _catalogDir, validateContent);
        } catch (JDOMException e) {
            msg = "ingestEOCoverageDescription(): CoverageDescription file either did not parse or did not validate. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        } catch (IOException e) {
            msg = "ingestEOCoverageDescription(): Attempting to access CoverageDescription file  generated an IOException. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        } catch (ConfigurationException e) {
            msg = "ingestEOCoverageDescription(): Encountered a configuration error in the configuration file "+ _catalogConfigFile +" Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        } catch (WcsException e) {
            msg = "ingestEOCoverageDescription(): When ingesting the CoverageDescription it failed a validation test. Msg: " +
                    e.getMessage()+"  SKIPPING";
            log.error(msg);
        }

        if(eoCoverageDescription!=null){
            String coverageId = eoCoverageDescription.getCoverageId();

            CoverageDescription cd = coveragesMap.get(coverageId)==null?eoCoveragesMap.get(coverageId):coveragesMap.get(coverageId);
            if(cd!=null){
                StringBuilder sb = new StringBuilder("ingestEOCoverageDescription() -");
                sb.append(" SKIPPING new coverage with duplicate coverageID '").append(coverageId);
                sb.append("' and dataset URL: ").append(eoCoverageDescription.getDapDatasetUrl());
                sb.append(" The coverageId is already in use and associated with dataset URL: ").append(cd.getDapDatasetUrl());
                log.error(sb.toString());

            }
            else {
                eoCoveragesMap.put(coverageId, eoCoverageDescription);
                coveragesMap.put(coverageId, eoCoverageDescription);
            }
        }

    }




    @Override
    public boolean hasCoverage(String id){

        log.debug("Looking for a coverage with ID: "+id);

        return coveragesMap.containsKey(id);
    }

    @Override
    public boolean hasEoCoverage(String id){

        log.debug("Looking for a coverage with ID: "+id);

        return eoCoveragesMap.containsKey(id);
    }


    @Override
    public  Element getCoverageDescriptionElement(String id) throws WcsException{

        CoverageDescription coverage = coveragesMap.get(id);

        if(coverage==null){
            throw new WcsException("No such coverage.",
                    WcsException.INVALID_PARAMETER_VALUE,"wcs:CoverageId");

        }

        return coverage.getCoverageDescriptionElement();
    }


    @Override
    public  CoverageDescription getCoverageDescription(String id) throws WcsException {

        CoverageDescription cd = coveragesMap.get(id);
        if(cd==null)
            throw new WcsException("No such wcs:Coverage: "+ Scrub.fileName(id),
                    WcsException.INVALID_PARAMETER_VALUE,"wcs:CoverageId");


        return cd;
    }


    public  Element getCoverageSummaryElement(String id) throws WcsException {
        return coveragesMap.get(id).getCoverageSummary();
    }

    @Override
    public  Collection<Element> getCoverageSummaryElements() throws WcsException {


        TreeMap<String, Element> coverageSummaries = new TreeMap<>();

        Enumeration e = coveragesMap.elements();

        CoverageDescription cd;

        while(e.hasMoreElements()){
            cd = (CoverageDescription)e.nextElement();

            coverageSummaries.put(cd.getCoverageId(),cd.getCoverageSummary());

        }

        return coverageSummaries.values();
    }

    public  Collection<Element> getEOCoverageSummaryElements() throws WcsException {


        TreeMap<String, Element> eoCoverageSummaries = new TreeMap<>();

        Enumeration e = eoCoveragesMap.elements();

        EOCoverageDescription eoCoverageDescription;

        while(e.hasMoreElements()){
            eoCoverageDescription = (EOCoverageDescription)e.nextElement();

            eoCoverageSummaries.put(eoCoverageDescription.getCoverageId(),eoCoverageDescription.getCoverageSummary());

        }

        return eoCoverageSummaries.values();
    }

    @Override
    public Collection<Element> getDatasetSeriesSummaryElements() throws InterruptedException, WcsException {

        TreeMap<String, Element> datasetSeriesElements = new TreeMap<>();

        Enumeration e = datasetSeriesMap.elements();

        EODatasetSeries dss;

        while(e.hasMoreElements()){
            dss = (EODatasetSeries) e.nextElement();

            String id = dss.getId();
            Element e3 = dss.getDatasetSeriesSummaryElement();
            datasetSeriesElements.put(id,e3);

        }

        return datasetSeriesElements.values();
    }


    @Override
    public long getLastModified(){
        return _lastModified;
    }



    @Override
    public void destroy(){}


    @Override
    public void update(){
    }


    @Override
    public  EOCoverageDescription getEOCoverageDescription(String id){
        return eoCoveragesMap.get(id);
    }

    @Override
    public   EODatasetSeries getEODatasetSeries(String id){
        return datasetSeriesMap.get(id);
    }

    @XmlElement(name = "WcsCoverage")
    public List<CoverageDescription> getCoverageDescriptionElements() {
    	return Collections.list(coveragesMap.elements());
    }
  
    public void setCoverageDescriptionElements(ConcurrentHashMap<String,CoverageDescription> covs)
    {
      this.coveragesMap = covs;	
    }

    @XmlElement(name = "EOWcsCoverage")
    public List<EOCoverageDescription> getEoCoverageDescriptionElements() {
    	return Collections.list(eoCoveragesMap.elements());
    }
  
    public void setEoCoverageDescriptionElements(ConcurrentHashMap<String,EOCoverageDescription> ecovs)
    {
      this.eoCoveragesMap = ecovs;	
    }
    
    @XmlElement(name = "EODatasetSeries")
    public List<EODatasetSeries> getEoDataSeriesElements() {
    	return Collections.list(datasetSeriesMap.elements());
    }
  
    public void setEoDataSeriesElements(ConcurrentHashMap<String,EODatasetSeries> dataSeries)
    {
      this.datasetSeriesMap = dataSeries;	
    }

    public CredentialsProvider getCredentials() {
        return _credsProvider;
    }
}