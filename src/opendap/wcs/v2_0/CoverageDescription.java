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

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

//import org.jdom.Document;

/**
 * An implementation of a wcs:CoverageDescription object. This implementation
 * includes methods that assist in the creation of DAP constraint expressions to
 * retrieve coverage data as NetCDF.
 * 
 * This class ...
 *
 *
 */
@XmlRootElement(name = "CoverageDescription")
public class CoverageDescription {
    public static final String CONFIG_ELEMENT_NAME = "WcsCoverage";
    protected Element _myCD;
    boolean _initialized = false;

    private Logger _log;
    private long _lastModified;
    private File _myFile;
    private boolean _validateContent = false;

    /**
     * Use getDapDatsetUrl() and setDapDatasetUrl() for access
     */
    private URL _dapDatasetUrl;

    /**
     * Use addFieldToDapVarIdAssociation(String, String) add and getDapGridArrayId(String fieldName) to access.
     * The keys in this Map are the wcs:Field names, and the associated entry is the name of the
     * DAP variable that contains the data.
     */
    private HashMap<String, String> _dapGridId;


    /**
     * Use addDomainCoordinate(String, DomainCoordinate) and getDomainCoordinate(String)
     */
    private LinkedHashMap<String, DomainCoordinate> _domainCoordinates;




    public CoverageDescription() {
        _log = LoggerFactory.getLogger(this.getClass());
        _lastModified = System.currentTimeMillis();
        _dapGridId = new HashMap<>();
        _domainCoordinates = new LinkedHashMap<>();
    }

    public CoverageDescription(CoverageDescription cd) throws IOException {
        super();
        _myCD = (Element) cd._myCD.clone();
        _lastModified = cd._lastModified;
        _myFile = cd._myFile.getCanonicalFile();
        _validateContent = cd._validateContent;
        _dapDatasetUrl = new URL(cd._dapDatasetUrl.toString());
        _dapGridId.putAll(cd._dapGridId);
        _domainCoordinates.putAll(cd._domainCoordinates);
        _initialized = cd._initialized;
    }

    /**
     * Builds the CoverageDescription object from a lfc:WcsCoverage element that is part of the configuration file used
     * opendap.wcs.v2_0.LocalFileCatalog
     *
     <LocalFileCatalog
         xmlns="http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#"
         xmlns:lfc="http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#"
         validateCatalog="true">
         <WcsCoverage fileName="ECMWF_ERA-40_subset.ncml.xml">

             <DapDatasetUrl>http://test.opendap.org:8080/opendap/ioos/ECMWF_ERA-40_subset.ncml</DapDatasetUrl>

             <field name="tcw">
                 <DapIdOfLatitudeCoordinate>tcw.longitude</DapIdOfLatitudeCoordinate>
                 <DapIdOfLongitudeCoordinate>tcw.latitude</DapIdOfLongitudeCoordinate>
                 <DapIdOfTimeCoordinate>tcw.time</DapIdOfTimeCoordinate>
             </field>

             <field name="tcwv">
                 <DapIdOfLatitudeCoordinate>tcwv.longitude</DapIdOfLatitudeCoordinate>
                 <DapIdOfLongitudeCoordinate>tcwv.latitude</DapIdOfLongitudeCoordinate>
                 <DapIdOfTimeCoordinate>tcwv.time</DapIdOfTimeCoordinate>
             </field>
             <field name="lsp">
                 <DapIdOfLatitudeCoordinate>lsp.longitude</DapIdOfLatitudeCoordinate>
                 <DapIdOfLongitudeCoordinate>lsp.latitude</DapIdOfLongitudeCoordinate>
                 <DapIdOfTimeCoordinate>lsp.time</DapIdOfTimeCoordinate>
             </field>
             <field name="cp">
                 <DapIdOfLatitudeCoordinate>cp.longitude</DapIdOfLatitudeCoordinate>
                 <DapIdOfLongitudeCoordinate>cp.latitude</DapIdOfLongitudeCoordinate>
                 <DapIdOfTimeCoordinate>cp.time</DapIdOfTimeCoordinate>
             </field>
             <field name="msl">
                 <DapIdOfLatitudeCoordinate>msl.longitude</DapIdOfLatitudeCoordinate>
                 <DapIdOfLongitudeCoordinate>msl.latitude</DapIdOfLongitudeCoordinate>
                 <DapIdOfTimeCoordinate>msl.time</DapIdOfTimeCoordinate>
             </field>
             <field name="blh">
                 <DapIdOfLatitudeCoordinate>blh.longitude</DapIdOfLatitudeCoordinate>
                 <DapIdOfLongitudeCoordinate>blh.latitude</DapIdOfLongitudeCoordinate>
                 <DapIdOfTimeCoordinate>blh.time</DapIdOfTimeCoordinate>
             </field>
         </WcsCoverage>

     </LocalFileCatalog>

     * @param wcsCoverageConfig The file containing the wcs:CoverageDescription element.
     * @param catalogDir
     * @param validateContent  Controls the XML parser document validation. A value of true will cause the parser to
     * perform document validation. A value of false will cause the parser to simply parse the document which only
     * checks for that it is well-formed.
     * @throws IOException When the file cannot be read.
     * @throws JDOMException When the file cannot be parsed.
     * @throws ConfigurationException
     * @throws WcsException
     */
    public CoverageDescription(Element wcsCoverageConfig, String catalogDir, boolean validateContent)
            throws IOException, JDOMException, ConfigurationException, WcsException {
        init();

        this._validateContent = validateContent;

        String coverageDescriptionFileName = wcsCoverageConfig.getAttributeValue("fileName");

        String msg;

        if (coverageDescriptionFileName == null) {
            msg = "In the catalog file (" + coverageDescriptionFileName
                    + " a lfc:WcsCoverage element is missing the 'fileName' attribute.";
            _log.error(msg);
            throw new ConfigurationException(msg);
        }

        String fileName = catalogDir == null ? "" : (catalogDir + "/") + coverageDescriptionFileName;
        File coverageDescriptionFile = new File(fileName);

        // Ingest the CoverageDescription File
        _log.debug("Loading coverage description '{}'", coverageDescriptionFile);

        _myCD = ingestCoverageDescription(coverageDescriptionFile);
        _myFile = coverageDescriptionFile;

        qcCoverageDescriptionContent();

        _lastModified = coverageDescriptionFile.lastModified();

        /**
         * Load the DAP Data Access URL through which the coverage data may be accessed.
         * This is REQUIRED.
         */
        Element e = wcsCoverageConfig.getChild("DapDatasetUrl", LocalFileCatalog.NS);
        if (e == null) {
            msg = "ingestCatalog(): In the catalog file (" + coverageDescriptionFileName
                    + " a lfc:WcsCoverage element is missing the 'lfc:DapDatasetUrl' element.";
            _log.error(msg);
            throw new ConfigurationException(msg);
        }
        URL datasetUrl = new URL(e.getTextTrim());
        setDapDatasetUrl(datasetUrl);

        /**
         * Load DAP ID (The Fully Qualified Name) for the variable that holds the domain
         * coordinate values for this field. This is REQUIRED.
         */
        List coordList = wcsCoverageConfig.getChildren("DomainCoordinate", LocalFileCatalog.NS);
        if (coordList.isEmpty()) {
            msg = "ingestCatalog(): The configuration element is missing the required "
                    + "child element(s) 'DomainCoordinate'.";
            _log.error(msg);
            throw new ConfigurationException(msg);
        }
        Iterator coordIt = coordList.iterator();
        while (coordIt.hasNext()) {
            Element domainCoordinateElem = (Element) coordIt.next();
            DomainCoordinate dc = new DomainCoordinate(domainCoordinateElem);
            addDomainCoordinate(dc);
        }

        /**
         * Process each lfc:field element in the coverage's range set..
         */
        for (Object o : wcsCoverageConfig.getChildren("field", LocalFileCatalog.NS)) {
            Element field = (Element) o;

            String fieldName;
            /**
             * Load the field ID for this field in the coverage's range. This is REQUIRED.
             */
            fieldName = field.getAttributeValue("name");
            if (fieldName == null) {
                msg = "ingestCatalog(): In the catalog file " + coverageDescriptionFileName + " a lfc:field element is "
                        + "missing the required attribute 'name' .";
                _log.error(msg);
                throw new ConfigurationException(msg);
            }

            /**
             * Load DAP ID (The Fully Qualified Name) for the Grid variable that holds the
             * range data for this field. This is REQUIRED.
             */
            String dapID = field.getAttributeValue("dapID");
            if (dapID == null) {
                msg = "CoverageDescription(): In in the Catalog Configuration file, the field element" + "named "
                        + fieldName + " is missing the required" + "attribute  'dapID'.";
                _log.error(msg);
                throw new ConfigurationException(msg);
            }
            
            addFieldToDapVarIdAssociation(fieldName, dapID);
        }
    }

    private void init() {
        if (_initialized)
            return;

        _initialized = true;
    }

    private Element parseXmlDoc(InputStream is, boolean validateContent) throws JDOMException, IOException {
        // get a validating jdom parser to parse and validate the XML document.
        SAXBuilder validatingParser = new SAXBuilder("org.apache.xerces.parsers.SAXParser", validateContent);
        validatingParser.setFeature("http://apache.org/xml/features/validation/schema", validateContent);

        org.jdom.Document cdDoc = validatingParser.build(is);

        Element cd = cdDoc.getRootElement();

        cd.detach();

        return cd;
    }

    private Element ingestCoverageDescription(File cdFile) throws IOException, JDOMException, WcsException {
        String msg;

        if (!Util.isReadableFile(cdFile)) {
            msg = "Problem with file: " + cdFile.getName();
            _log.error(msg);
            throw new IOException(msg);
        }

        _log.info("Ingesting wcs:CoverageDescription from '{}'. XML Validation is {}.", cdFile,
                _validateContent ? "ON" : "OFF");

        Element cd = parseXmlDoc(new FileInputStream(cdFile), _validateContent);

        _myFile = cdFile;

        _lastModified = cdFile.lastModified();

        return cd;
    }

    private void qcCoverageDescriptionContent() throws WcsException {

        StringBuilder msg = new StringBuilder();

        if (getNativeFormat() == null) {
            msg.append("wcs:CoverageDescription is missing required component. "
                    + "wcs:CoverageDescription/wcs:ServiceParameters/wcs:nativeFormat");
            throw new WcsException(msg.toString(), WcsException.MISSING_PARAMETER_VALUE, "wcs:nativeFormat");
        }
    }

    /**
     * Returns the last modified time of the Coverage.
     * 
     * @return Returns the last modified time of the Coverage.
     *
     */
    public long lastModified() {
        try {
            if (_myFile != null && _lastModified < _myFile.lastModified()) {
                _myCD = ingestCoverageDescription(_myFile);
            }
        } catch (Exception e) {

            String msg = "Failed to update CoverageDescription from file ";

            if (_myFile != null) {
                msg += _myFile.getAbsoluteFile();
            }
            _log.error(msg);
        }

        return _lastModified;
    }

    /**
     * Checks the Coverage to see if it's range contains a particular field.
     * 
     * @param fieldID
     *            The value of the wcs:Identifier for the wcs:Field in question.
     * @return True if the Field is present, false otherwise.
     */
    public boolean hasField(String fieldID) {
        boolean foundIt = false;

        Element range = _myCD.getChild("rangeType", WCS.GMLCOV_NS);

        if (range == null)
            return foundIt;

        Element dataRecord = _myCD.getChild("DataRecord", WCS.SWE_NS);

        if (dataRecord == null)
            return foundIt;

        Element field;
        String id;
        Iterator i = dataRecord.getChildren("field", WCS.SWE_NS).iterator();

        while (i.hasNext()) {
            field = (Element) i.next();
            id = field.getAttributeValue("name");
            if (id != null && fieldID.equals(id))
                foundIt = true;
        }

        return foundIt;
    }

    /**
     *
     * @return Returns the value of the unique wcs:CoverageId associated with this
     *         CoverageDescription.
     */
    public String getCoverageId() {
        Element coverageId = _myCD.getChild("CoverageId", WCS.WCS_NS);
        return coverageId.getText();
    }

    /**
     *
     * @return Returns the unique wcs:Identifier associated with this
     *         CoverageDescription.
     */
    public Element getCoverageIdElement() {
        return (Element) _myCD.getChild("CoverageId", WCS.WCS_NS).clone();
    }

    /**
     *
     * @return Returns the BoundingBox defined by the gml:boundedBy element, if such
     *         an element is present. Returns null if no gml:boundedBy element is
     *         found.
     * @throws WcsException
     *             When bad things happen.
     */
    public NewBoundingBox getBoundingBox() throws WcsException {
        Element boundedBy = _myCD.getChild("boundedBy", WCS.GML_NS);
        if (boundedBy != null)
            return new NewBoundingBox(boundedBy);
        return null;
    }

    public boolean hasBoundedBy() {
        Element boundedBy = _myCD.getChild("boundedBy", WCS.GML_NS);
        if (boundedBy != null)
            return true;
        return false;
    }

    /**
     *
     * @return Returns the gml:boundedBy element associated with this
     *         CoverageDescription. Will return null if no gml:boundedBy element is
     *         present.
     * @throws WcsException
     *             When bad things happen.
     */
    public Element getBoundedByElement() throws WcsException {
        Element boundedBy = _myCD.getChild("boundedBy", WCS.GML_NS);
        if (boundedBy != null)
            return (Element) boundedBy.clone();
        return null;
    }

    /**
     *
     * @return Returns the wcs:SupportedCRS elements associated with this
     *         CoverageDescription.
     */
    public List getSupportedCrsElements() {
        return cloneElementList(_myCD.getChildren("SupportedCRS", WCS.WCS_NS));
    }

    /**
     *
     * @return Returns the wcs:nativeFormat element associated with this
     *         CoverageDescription.
     */
    public Element getNativeFormatElement() {

        Element serviceParameters = _myCD.getChild("ServiceParameters", WCS.WCS_NS);
        if (serviceParameters == null)
            return null;

        Element nativeFormat = serviceParameters.getChild("nativeFormat", WCS.WCS_NS);
        if (nativeFormat == null)
            return null;

        return (Element) nativeFormat.clone();
    }

    public String getNativeFormat() {

        Element nativeFormatElement = getNativeFormatElement();
        if (nativeFormatElement == null)
            return null;

        return nativeFormatElement.getTextTrim();
    }

    public String getCoverageSubtype() {
        Element serviceParameters = _myCD.getChild("ServiceParameters", WCS.WCS_NS);
        if (serviceParameters == null)
            return null;

        Element coverageSubtype = serviceParameters.getChild("CoverageSubtype", WCS.WCS_NS);
        if (coverageSubtype == null)
            return null;

        return coverageSubtype.getTextTrim();
    }

    public Element getCoverageSubtypeElement() {
        Element serviceParameters = _myCD.getChild("ServiceParameters", WCS.WCS_NS);
        if (serviceParameters == null)
            return null;

        Element coverageSubtype = serviceParameters.getChild("CoverageSubtype", WCS.WCS_NS);
        if (coverageSubtype == null)
            return null;

        return (Element) coverageSubtype.clone();
    }

    public Element getCoverageSubtypeFamilyTree() {
        Element serviceParameters = _myCD.getChild("ServiceParameters", WCS.WCS_NS);
        if (serviceParameters == null)
            return null;

        Element coverageSubtypeParent = serviceParameters.getChild("CoverageSubtypeParent", WCS.WCS_NS);
        if (coverageSubtypeParent == null)
            return null;

        return (Element) coverageSubtypeParent.clone();
    }

    public boolean hasCoverageSubtypeFamilyTree() {
        Element serviceParameters = _myCD.getChild("ServiceParameters", WCS.WCS_NS);
        if (serviceParameters == null)
            return false;

        Element coverageSubtypeParent = serviceParameters.getChild("CoverageSubtypeParent", WCS.WCS_NS);
        if (coverageSubtypeParent == null)
            return false;
        return true;
    }

    private List<Element> cloneElementList(List list) {
        ArrayList<Element> newList = new ArrayList<Element>();

        Iterator i = list.iterator();
        Element e;

        while (i.hasNext()) {
            e = (Element) i.next();
            newList.add((Element) e.clone());
        }

        return newList;
    }

    /**
     * <complexType name="CoverageSummaryType"> <complexContent>
     * <extension base="ows:DescriptionType"> <sequence>
     * <element ref="ows:WGS84BoundingBox" minOccurs="0" maxOccurs="unbounded"/>
     * <!-- This item above is put at this position (and not next to
     * ows:BoundingBox) to avoid a problem of "ambiguity" (thanks, Joan Maso!):
     * ows:WGS84BoundingBox is on the same substitution group as ows:BoundingBox so
     * when instantiating ows:WGS84BoundingBox you do not know if its a
     * WGS84BoundingBox or an ows:BoundingBox substituded by a ows:WGS84BoundingBox.
     * Because order is relevant and the wcs:CoverageId separates both this
     * ambiguity gets resolved. --> <element ref="wcs:CoverageId"/>
     * <element ref="wcs:CoverageSubtype"/>
     * <element ref="wcs:CoverageSubtypeParent" minOccurs="0"/>
     * <element ref="ows:BoundingBox" minOccurs="0" maxOccurs="unbounded"/>
     * <element ref="ows:Metadata" minOccurs="0" maxOccurs="unbounded"/> </sequence>
     * </extension> </complexContent> </complexType>
     * 
     * @return Returns the wcs:CoverageSummary element that represents this
     *         CoverageDescription.
     * @throws WcsException
     *             When bad things happen.
     */
    public Element getCoverageSummary() throws WcsException {

        Element covSum = new Element("CoverageSummary", WCS.WCS_NS);

        Element coverageID = getCoverageIdElement();
        covSum.addContent(coverageID);

        Element coverageSubType = getCoverageSubtypeElement();
        covSum.addContent(coverageSubType);

        if (hasCoverageSubtypeFamilyTree()) {
            Element coverageSubtypeParent = getCoverageSubtypeFamilyTree();
            if (coverageSubtypeParent != null)
                covSum.addContent(coverageSubtypeParent);
        }

        if (hasBoundedBy()) {
            NewBoundingBox boundingBox = getBoundingBox();
            Element bb = boundingBox.getOwsBoundingBoxElement();
            covSum.addContent(bb);
        }

        // @todo Add metadata records if available.

        return covSum;
    }

    /**
     *
     * @return Returns the wcs:CoverageDescription element that represents this
     *         CoverageDescription.
     */
    public Element getCoverageDescriptionElement() {
        return (Element) _myCD.clone();
    }

    /**
     * @param requestURL
     *            The request URL for the current request to be used in constructing
     *            lineage content.
     * @return Returns the wcs:CoverageDescription element that represents this
     *         CoverageDescription.
     */
    // public Element getCoverageDescriptionElement(String requestURL){
    // Element cdElement = (Element) _myCD.clone();
    // addLineage(cdElement, requestURL);
    // return cdElement;
    // }

    /**
     * Regular WCS-2.0 doesn't know about lineage, but EO WCS does. So this is a do
     * nothing call in the general case and it adds the lineage to the EO case.
     * Woot...
     *
     * @param requestURL
     */
    public void addLineage(Element coverageDescription, String requestURL) {
    }

    /**
     * Gets the DAP local ID for the DAP Grid variable data array that is associated
     * by the wcs:Identifier for the wcs:Field
     * 
     * @param fieldID
     *            The value of the wcs:Identifier associated with the wcs:Field in
     *            question.
     * @return Returns the DAP local ID for the DAP Grid variable data array that is
     *         associated by the wcs:Identifier
     */
    public String getDapGridArrayId(String fieldID) {
        return _dapGridId.get(fieldID);
    }

    /**
     * Sets the DAP local ID for the Grid variable data array that is associated by
     * the wcs:Identifier for the wcs:Field
     * 
     * @param fieldID
     *            The value of the wcs:Identifier associated with the wcs:Field in
     *            question.
     * @param dapGridId
     *            The DAP Variable ID of the Grid variable data array that is
     *            associated with the wcs:Field's wcs:Identifier represented by
     *            filedID.
     */
    public void addFieldToDapVarIdAssociation(String fieldID, String dapGridId) {
        _dapGridId.put(fieldID, dapGridId);
    }


    /**
     * Gets the DAP local ID for the Latitude coordinate map array that is
     * associated by the wcs:Identifier for the wcs:Field
     * 
     * @return Returns the DAP local ID for the Latitude coordinate map array that
     *         is associated by the wcs:Identifier
     */
    public LinkedHashMap<String, DomainCoordinate> getDomainCoordinates() {

        LinkedHashMap<String, DomainCoordinate> dc = new LinkedHashMap<String, DomainCoordinate>();
        for (DomainCoordinate mydc : _domainCoordinates.values()) {
            dc.put(mydc.getName(), new DomainCoordinate(mydc));
        }
        return dc;

    }


    /**
     * Adds a named DomainCoordinate to the Coverage.
     * NOTE: The order that coordinates are added is preserved and MATTERS.
     * 
     * @param dc
     */
    public void addDomainCoordinate(DomainCoordinate dc){
        _domainCoordinates.put(dc.getName(),dc);
    }

    /**
     * Gets the DAP local ID for the Latitude coordinate map array that is
     * associated by the wcs:Identifier for the wcs:Field
     * 
     * @return Returns the DAP local ID for the Latitude coordinate map array that
     *         is associated by the wcs:Identifier
     */
    public DomainCoordinate getDomainCoordinate(String coordinateName) {

        DomainCoordinate dc = _domainCoordinates.get(coordinateName);

        if (dc == null)
            return null;
        return new DomainCoordinate(dc);
    }

    public void setDapDatasetUrl(URL datasetUrl) throws WcsException {

        try {
            _dapDatasetUrl = new URL(datasetUrl.toExternalForm());
        } catch (MalformedURLException e) {
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }
    }

    public URL getDapDatasetUrl() {

        return _dapDatasetUrl;
    }

    public String getGmlId() {

        return _myCD.getAttributeValue("id", WCS.GML_NS);
    }

    public Vector<Element> getAbstractGmlTypeContent() {

        Element e;
        Vector<Element> abstractGmlTypeContent = new Vector<Element>();

        for (Object o : _myCD.getChildren("metaDataProperty", WCS.GML_NS)) {
            e = (Element) o;
            abstractGmlTypeContent.add((Element) e.clone());
        }

        e = _myCD.getChild("description", WCS.GML_NS);
        if (e != null)
            abstractGmlTypeContent.add((Element) e.clone());

        e = _myCD.getChild("descriptionReference", WCS.GML_NS);
        if (e != null)
            abstractGmlTypeContent.add((Element) e.clone());

        e = _myCD.getChild("identifier", WCS.GML_NS);
        if (e != null)
            abstractGmlTypeContent.add((Element) e.clone());

        for (Object o : _myCD.getChildren("name", WCS.GML_NS)) {
            e = (Element) o;
            abstractGmlTypeContent.add((Element) e.clone());
        }

        return abstractGmlTypeContent;
    }

    public Vector<Element> getAbstractFeatureTypeContent() {

        Element e;
        Vector<Element> abstractFeatureTypeContent = getAbstractGmlTypeContent();

        e = _myCD.getChild("boundedBy", WCS.GML_NS);
        if (e != null)
            abstractFeatureTypeContent.add((Element) e.clone());

        e = _myCD.getChild("location", WCS.GML_NS);
        if (e != null)
            abstractFeatureTypeContent.add((Element) e.clone());

        return abstractFeatureTypeContent;
    }

    public Element getDomainSet() throws WcsException {

        Element domainSet;

        domainSet = _myCD.getChild("domainSet", WCS.GML_NS);
        if (domainSet != null)
            return (Element) domainSet.clone();

        throw new WcsException("wcs:CoverageDescription is missing a gml:domainSet: ",
                WcsException.MISSING_PARAMETER_VALUE, "gml:domainSet");
    }

    public Element getRangeType() throws WcsException {

        Element rangeType;

        rangeType = _myCD.getChild("rangeType", WCS.GMLCOV_NS);
        if (rangeType != null)
            return (Element) rangeType.clone();

        throw new WcsException("wcs:CoverageDescription is missing a gmlcov:rangeType: ",
                WcsException.MISSING_PARAMETER_VALUE, "gmlcov:rangeType");
    }

    public Vector<Field> getFields() throws WcsException {

        Element rangeType;

        rangeType = _myCD.getChild("rangeType",WCS.GMLCOV_NS);
        if(rangeType==null)
            throw new WcsException("wcs:CoverageDescription is missing a gmlcov:rangeType: ",
                WcsException.MISSING_PARAMETER_VALUE,"gmlcov:rangeType");

        Vector<Field> fields = new Vector<>();

        ElementFilter filter = new ElementFilter("field",WCS.SWE_NS);
        Iterator i = rangeType.getDescendants(filter);
        while(i.hasNext()){
            Element fieldElement = (Element) i.next();
            Field field = new Field(fieldElement);
            fields.add(field);
        }

        return fields;
    }



    public Coverage getCoverage(String requestUrl) throws WcsException, InterruptedException {

        Coverage coverage = new Coverage(this, requestUrl);

        return coverage;
    }

    @XmlAttribute(name = "fileName")
    public File getMyFile() {
        return _myFile;
    }

    public void setMyFile(File myFile) {
        this._myFile = myFile;
    }

    @XmlElement(name = "DomainCoordinate")
    public List<DomainCoordinate> getDomainCoordinatesAsList() {
        return new ArrayList(getDomainCoordinates().values());
    }

}
