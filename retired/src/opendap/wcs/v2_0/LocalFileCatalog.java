/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
import opendap.dap.User;
import org.apache.http.client.CredentialsProvider;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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
@XmlRootElement(name="LocalFileCatalog")
public class LocalFileCatalog implements WcsCatalog {

    private Logger log;


    public static final String NamespaceName = "http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#";
    public static final String NamespacePrefix = "lfc";
    public static final Namespace NS = Namespace.getNamespace(NamespacePrefix, NamespaceName);

    // private CredentialsProvider _credsProvider;


    // @XmlAttribute(name = "validateCatalog")
    private boolean _validateContent = false;

    private String _catalogDir;
    private String _catalogConfigFile;
    private long _lastModified;

    private boolean _intitialized;

    private ConcurrentHashMap<String, CoverageDescription> _coveragesMap;
    private ConcurrentHashMap<String, EOCoverageDescription> _eoCoveragesMap;
    private ConcurrentHashMap<String, EODatasetSeries> _datasetSeriesMap;

    private String _name;

    private ReentrantReadWriteLock _catalogLock;
    private Date _cacheTime;


    /**
     *
     *
     */
    public LocalFileCatalog() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        _catalogLock = new ReentrantReadWriteLock();
        _name = "LFC";
        _coveragesMap = new ConcurrentHashMap<>();
        _eoCoveragesMap = new ConcurrentHashMap<>();
        _datasetSeriesMap = new ConcurrentHashMap<>();
        _intitialized = false;
    }


    /**
     * @param config
     * @throws Exception
     */
    @Override
    public void init(Element config, String persistentContentPath, String serviceContextPath) throws Exception {

        if (_intitialized)
            return;


        Element e1;
        String s;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        s = config.getAttributeValue("name");
        if (s != null)
            _name = s;

        e1 = config.getChild("CatalogDirectory");
        if (e1 == null) {

            String defaultCatalogDirectory = persistentContentPath + this.getClass().getSimpleName();

            File defaultCatDir = new File(defaultCatalogDirectory);

            if (!defaultCatDir.exists()) {
                if (!defaultCatDir.mkdirs()) {
                    s = "Default Coverages Directory (" + defaultCatalogDirectory + ")does not exist and cannot be " +
                            "created. Could not find CoveragesDirectory element in " +
                            "configuration element: " + xmlo.outputString(config);
                    log.error(s);
                    throw new IOException(s);
                }
            }
            _catalogDir = defaultCatalogDirectory;
        } else {
            _catalogDir = e1.getTextTrim();
        }
        log.debug("WCS-2.0 Coverages Directory: " + _catalogDir);


        e1 = config.getChild("CatalogFile");
        if (e1 == null) {
            _catalogConfigFile = _catalogDir + "/LFC.xml";
        } else {
            _catalogConfigFile = e1.getTextTrim();
        }
        log.debug("CatalogFile: " + _catalogConfigFile);


        /*
        e1 = config.getChild("Credentials");
        if (e1 == null) {
            _credsProvider = opendap.http.Util.getNetRCCredentialsProvider();
            log.debug("Using Credentials file: ~/.netrc");
        } else {
            String credsFilename = e1.getTextTrim();
            _credsProvider = opendap.http.Util.getNetRCCredentialsProvider(credsFilename, true);
            log.debug("Using Credentials file: {}", credsFilename);
        }
        */

        ingestCatalog();

        _cacheTime = new Date();

        _intitialized = true;


    }


    /**
     * <WcsCoverage
     * fileName="200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml.xml"
     * coverageID="ioos/200803061600_HFRadar_USEGC_6km_rtv_SIO.nc">
     * <p>
     * <field name="u">
     * <DapIdOfLatitudeCoordinate>u.longitude</DapIdOfLatitudeCoordinate>
     * <DapIdOfLongitudeCoordinate>u.latitude</DapIdOfLongitudeCoordinate>
     * <DapIdOfTimeCoordinate>u.time</DapIdOfTimeCoordinate>
     * </field>
     * <p>
     * <field name="v">
     * <DapIdOfLatitudeCoordinate>v.longitude</DapIdOfLatitudeCoordinate>
     * <DapIdOfLongitudeCoordinate>v.latitude</DapIdOfLongitudeCoordinate>
     * <DapIdOfTimeCoordinate>v.time</DapIdOfTimeCoordinate>
     * </field>
     * <field name="DOPx">
     * <DapIdOfLatitudeCoordinate>DOPx.longitude</DapIdOfLatitudeCoordinate>
     * <DapIdOfLongitudeCoordinate>DOPx.latitude</DapIdOfLongitudeCoordinate>
     * <DapIdOfTimeCoordinate>DOPx.time</DapIdOfTimeCoordinate>
     * </field>
     * <field name="DOPy">
     * <DapIdOfLatitudeCoordinate>DOPy.longitude</DapIdOfLatitudeCoordinate>
     * <DapIdOfLongitudeCoordinate>DOPy.latitude</DapIdOfLongitudeCoordinate>
     * <DapIdOfTimeCoordinate>DOPy.time</DapIdOfTimeCoordinate>
     * </field>
     * </WcsCoverage>
     *
     * @throws java.io.IOException
     * @throws org.jdom.JDOMException
     */
    private void ingestCatalog() throws IOException, JDOMException {

        String msg;

        // QC the Catalog directory.
        File catalogDir = new File(_catalogDir);
        if (!Util.isReadableDir(catalogDir)) {
            msg = "ingestCatalog(): Catalog directory " + catalogDir + " is not accessible.";
            log.error(msg);
            throw new IOException(msg);
        }

        // QC the Catalog file.
        File catalogFile = new File(_catalogConfigFile);
        if (!Util.isReadableFile(catalogFile)) {
            msg = "ingestCatalog(): Catalog File " + _catalogConfigFile + " is not accessible.";
            log.error(msg);
            throw new IOException(msg);
        }

        Element lfcConfig = opendap.xml.Util.getDocumentRoot(catalogFile);


        boolean validate = false;
        String validateAttr = lfcConfig.getAttributeValue("validateCatalog");
        if (validateAttr != null) {
            validate = validateAttr.equalsIgnoreCase("true") || validateAttr.equalsIgnoreCase("on");
        }

        _validateContent = validate;

        int i = 0;
        for (Object o : lfcConfig.getChildren(CoverageDescription.CONFIG_ELEMENT_NAME, NS)) {
            Element wcsCoverageConfig = (Element) o;
            log.debug("Processing {}[{}]", CoverageDescription.CONFIG_ELEMENT_NAME, i++);
            ingestCoverageDescription(wcsCoverageConfig, _validateContent);
        }
        i = 0;
        for (Object o : lfcConfig.getChildren(EOCoverageDescription.CONFIG_ELEMENT_NAME, NS)) {
            Element eoWcsCoverageConfig = (Element) o;
            log.debug("Processing {}[{}]", EOCoverageDescription.CONFIG_ELEMENT_NAME, i++);
            ingestEOCoverageDescription(eoWcsCoverageConfig, _validateContent);
        }
        i = 0;
        for (Object o : lfcConfig.getChildren(EODatasetSeries.CONFIG_ELEMENT_NAME, NS)) {
            Element eoDatasetSeries = (Element) o;
            log.debug("Processing {}[{}]", EODatasetSeries.CONFIG_ELEMENT_NAME, i++);
            ingestDatasetSeries(eoDatasetSeries, _validateContent);
        }

        _lastModified = catalogFile.lastModified();

        log.info("Ingested WCS Catalog Configuration: {}", catalogFile);


    }

    private void ingestDatasetSeries(Element eowcsDatasetSeriesConfig, boolean validateContent) {

        String msg;
        EODatasetSeries eoDatasetSeries = null;
        try {
            eoDatasetSeries = new EODatasetSeries(eowcsDatasetSeriesConfig, _catalogDir, validateContent);
        } catch (JDOMException e) {
            msg = "ingestCoverageDescription(): CoverageDescription file either did not parse or did not validate. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (IOException e) {
            msg = "ingestCoverageDescription(): Attempting to access CoverageDescription file  generated an IOException. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (ConfigurationException e) {
            msg = "ingestCoverageDescription(): Encountered a configuration error in the configuration file " + _catalogConfigFile + " Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (WcsException e) {
            msg = "ingestCoverageDescription(): When ingesting the CoverageDescription it failed a validation test. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        }

        Vector<EOCoverageDescription> processed = new Vector<>();
        if (eoDatasetSeries != null) {
            String datasetSeriesId = eoDatasetSeries.getId();
            boolean conflict = false;
            for (EOCoverageDescription eocd : eoDatasetSeries.getMembers()) {
                String coverageId = eocd.getCoverageId();
                CoverageDescription cd =
                        _coveragesMap.get(coverageId) == null ? _eoCoveragesMap.get(coverageId) : _coveragesMap.get(coverageId);
                if (cd != null) {
                    StringBuilder sb = new StringBuilder("ingestDatasetSeries() -");
                    sb.append(" SKIPPING new coverage with duplicate coverageID '").append(coverageId);
                    sb.append("' and dataset URL: ").append(eocd.getDapDatasetUrl());
                    sb.append(" The coverageId is already in use and associated with dataset URL: ").append(cd.getDapDatasetUrl());
                    log.error(sb.toString());
                    conflict = true;

                } else {
                    _coveragesMap.put(coverageId, eocd);
                    _eoCoveragesMap.put(coverageId, eocd);
                    processed.add(eocd);
                }
            }
            if (conflict) {
                for (EOCoverageDescription eocd : processed) {
                    _coveragesMap.remove(eocd.getCoverageId());
                    _eoCoveragesMap.remove(eocd.getCoverageId());
                }
                StringBuilder sb = new StringBuilder("ingestDatasetSeries() - ");
                sb.append("CoverageId conflicts were found in the DatasetSeries '").append(datasetSeriesId).append("' ");
                sb.append(" !!SKIPPING!");
                log.error(sb.toString());

            } else {
                _datasetSeriesMap.put(datasetSeriesId, eoDatasetSeries);
                log.info("ingestDatasetSeries() - Ingested EODatasetSeries '{}'", datasetSeriesId);
            }
        }

    }


    private void ingestCoverageDescription(Element wcsCoverageConfig, boolean validateContent) {

        String msg;
        CoverageDescription coverageDescription = null;
        try {
            coverageDescription = new CoverageDescription(wcsCoverageConfig, _catalogDir, validateContent);
        } catch (JDOMException e) {
            msg = "ingestCoverageDescription(): CoverageDescription file either did not parse or did not validate. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (IOException e) {
            msg = "ingestCoverageDescription(): Attempting to access CoverageDescription file  generated an IOException. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (ConfigurationException e) {
            msg = "ingestCoverageDescription(): Encountered a configuration error in the configuration file " + _catalogConfigFile + " Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (WcsException e) {
            msg = "ingestCoverageDescription(): When ingesting the CoverageDescription it failed a validation test. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        }

        if (coverageDescription != null) {
            String coverageId = coverageDescription.getCoverageId();

            CoverageDescription cd = _coveragesMap.get(coverageId);
            if (cd != null) {
                StringBuilder sb = new StringBuilder("ingestCoverageDescription() -");
                sb.append(" SKIPPING new coverage with duplicate coverageID '").append(coverageId);
                sb.append("' and dataset URL: ").append(coverageDescription.getDapDatasetUrl());
                sb.append(" The coverageId is already in use and associated with dataset URL: ").append(cd.getDapDatasetUrl());
                log.error(sb.toString());

            } else {
                _coveragesMap.put(coverageId, coverageDescription);
            }
        }


    }


    private void ingestEOCoverageDescription(Element wcsCoverageConfig, boolean validateContent) {

        String msg;
        EOCoverageDescription eoCoverageDescription = null;
        try {
            eoCoverageDescription = new EOCoverageDescription(wcsCoverageConfig, _catalogDir, validateContent);
        } catch (JDOMException e) {
            msg = "ingestEOCoverageDescription(): CoverageDescription file either did not parse or did not validate. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (IOException e) {
            msg = "ingestEOCoverageDescription(): Attempting to access CoverageDescription file  generated an IOException. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (ConfigurationException e) {
            msg = "ingestEOCoverageDescription(): Encountered a configuration error in the configuration file " + _catalogConfigFile + " Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        } catch (WcsException e) {
            msg = "ingestEOCoverageDescription(): When ingesting the CoverageDescription it failed a validation test. Msg: " +
                    e.getMessage() + "  SKIPPING";
            log.error(msg);
        }

        if (eoCoverageDescription != null) {
            String coverageId = eoCoverageDescription.getCoverageId();

            CoverageDescription cd = _coveragesMap.get(coverageId) == null ? _eoCoveragesMap.get(coverageId) : _coveragesMap.get(coverageId);
            if (cd != null) {
                StringBuilder sb = new StringBuilder("ingestEOCoverageDescription() -");
                sb.append(" SKIPPING new coverage with duplicate coverageID '").append(coverageId);
                sb.append("' and dataset URL: ").append(eoCoverageDescription.getDapDatasetUrl());
                sb.append(" The coverageId is already in use and associated with dataset URL: ").append(cd.getDapDatasetUrl());
                log.error(sb.toString());

            } else {
                _eoCoveragesMap.put(coverageId, eoCoverageDescription);
                _coveragesMap.put(coverageId, eoCoverageDescription);
            }
        }

    }


    @Override
    public boolean hasCoverage(User user, String id) {

        log.debug("Looking for a coverage with ID: " + id);

        return _coveragesMap.containsKey(id);
    }

    @Override
    public boolean hasEoCoverage(String id) {

        log.debug("Looking for a coverage with ID: " + id);

        return _eoCoveragesMap.containsKey(id);
    }


    @Override
    public Element getCoverageDescriptionElement(User user, String id) throws WcsException {

        CoverageDescription coverage = _coveragesMap.get(id);

        if (coverage == null) {
            throw new WcsException("No such coverage.",
                    WcsException.INVALID_PARAMETER_VALUE, "wcs:CoverageId");

        }

        return coverage.getCoverageDescriptionElement();
    }


    @Override
    public CoverageDescription getCoverageDescription(User user, String id) throws WcsException {

        CoverageDescription cd = _coveragesMap.get(id);
        if (cd == null)
            throw new WcsException("No such wcs:Coverage: " + Scrub.fileName(id),
                    WcsException.INVALID_PARAMETER_VALUE, "wcs:CoverageId");


        return cd;
    }


    @Override
    public Element getCoverageSummaryElement(User user, String id) throws WcsException {
        CoverageDescription cd = _coveragesMap.get(id);
        return cd==null?null:cd.getCoverageSummary();
    }

    @Override
    public Collection<Element> getCoverageSummaryElements(User user) throws WcsException {

        TreeMap<String, Element> coverageSummaries = new TreeMap<>();
        Enumeration e = _coveragesMap.elements();
        CoverageDescription cd;
        
        while (e.hasMoreElements()) {
            cd = (CoverageDescription) e.nextElement();
            coverageSummaries.put(cd.getCoverageId(), cd.getCoverageSummary());
        }
        return coverageSummaries.values();
    }

    public Collection<Element> getEOCoverageSummaryElements() throws WcsException {


        TreeMap<String, Element> eoCoverageSummaries = new TreeMap<>();

        Enumeration e = _eoCoveragesMap.elements();

        EOCoverageDescription eoCoverageDescription;

        while (e.hasMoreElements()) {
            eoCoverageDescription = (EOCoverageDescription) e.nextElement();

            eoCoverageSummaries.put(eoCoverageDescription.getCoverageId(), eoCoverageDescription.getCoverageSummary());

        }

        return eoCoverageSummaries.values();
    }

    @Override
    public Collection<Element> getDatasetSeriesSummaryElements() throws InterruptedException, WcsException {

        TreeMap<String, Element> datasetSeriesElements = new TreeMap<>();

        Enumeration e = _datasetSeriesMap.elements();

        EODatasetSeries dss;

        while (e.hasMoreElements()) {
            dss = (EODatasetSeries) e.nextElement();

            String id = dss.getId();
            Element e3 = dss.getDatasetSeriesSummaryElement();
            datasetSeriesElements.put(id, e3);

        }

        return datasetSeriesElements.values();
    }


    @Override
    public long getLastModified() {
        return _lastModified;
    }


    @Override
    public void destroy() {
    }


    @Override
    public void update() {
    }


    @Override
    public EOCoverageDescription getEOCoverageDescription(String id) {
        return _eoCoveragesMap.get(id);
    }

    @Override
    public EODatasetSeries getEODatasetSeries(String id) {
        return _datasetSeriesMap.get(id);
    }

    @XmlElement(name = "WcsCoverage")
    public List<CoverageDescription> getCoverageDescriptionElements() {
        return Collections.list(_coveragesMap.elements());
    }

    public void setCoverageDescriptionElements(ConcurrentHashMap<String, CoverageDescription> covs) {
        this._coveragesMap = covs;
    }

    @XmlElement(name = "EOWcsCoverage")
    public List<EOCoverageDescription> getEoCoverageDescriptionElements() {
        return Collections.list(_eoCoveragesMap.elements());
    }

    public void setEoCoverageDescriptionElements(ConcurrentHashMap<String, EOCoverageDescription> ecovs) {
        this._eoCoveragesMap = ecovs;
    }

    @XmlElement(name = "EODatasetSeries")
    public List<EODatasetSeries> getEoDataSeriesElements() {
        return Collections.list(_datasetSeriesMap.elements());
    }

    public void setEoDataSeriesElements(ConcurrentHashMap<String, EODatasetSeries> dataSeries) {
        this._datasetSeriesMap = dataSeries;
    }

    @Override
    public String getDapDatsetUrl(String coverageID) {
        CoverageDescription cd = _coveragesMap.get(coverageID);
        if (cd == null)
            return null;
        return cd.getDapDatasetUrl();
    }


    //public CredentialsProvider getCredentials() {
    //return _credsProvider;
    //}

    public boolean matches(String coverageId) {
        return coverageId.startsWith(_name);
    }

}

