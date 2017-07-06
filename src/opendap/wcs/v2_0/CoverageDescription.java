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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.xml.bind.annotation.*;

/**
 * An implementation of a wcs:CoverageDescription object. This implementation includes methods that assist in the
 * creation of DAP constraint expressions to retrieve coverage data as NetCDF.
 *
 */
public class CoverageDescription {

    private Element myCD;

    private Logger log;


    private long lastModified;

    private File myFile;

    private boolean validateContent = false;


    private URL dapDatasetUrl;
    private HashMap<String,String> _dapGridId;


    private LinkedHashMap<String, DomainCoordinate> _domainCoordinates;
    private List<Field> fields;

	boolean initialized = false;
    

    public CoverageDescription()
    {
    	
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

     *
     *
     *
     *
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
    public CoverageDescription(Element wcsCoverageConfig, String catalogDir, boolean validateContent) throws IOException, JDOMException, ConfigurationException, WcsException {
        init();


        this.validateContent = validateContent;

        String coverageDescriptionFileName =  wcsCoverageConfig.getAttributeValue("fileName");

        String msg;

        if(coverageDescriptionFileName==null){
            msg = "ingestCatalog(): In the catalog file ("+coverageDescriptionFileName+" a lfc:WcsCoverage element is missing the 'fileName' attribute.";
            log.error(msg);
            throw new ConfigurationException(msg);
        }

        String fileName = catalogDir==null?"":(catalogDir + "/") + coverageDescriptionFileName;
        File coverageDescriptionFile = new File(fileName);

        // Ingest the CoverageDescription File
        log.debug("ingestCatalog(): Loading coverage description '{}'",coverageDescriptionFile);

        myCD = ingestCoverageDescription(coverageDescriptionFile);
        myFile = coverageDescriptionFile;

        qcCoverageDescriptionContent();

        lastModified = coverageDescriptionFile.lastModified();


        /**
         * Load the DAP Data Access URL through which the coverage data may be accessed.
         * This is REQUIRED.
         */
        Element e = wcsCoverageConfig.getChild("DapDatasetUrl",LocalFileCatalog.NS);
        if(e==null){
            msg = "ingestCatalog(): In the catalog file ("+coverageDescriptionFileName+" a lfc:WcsCoverage element is missing the 'lfc:DapDatasetUrl' element.";
            log.error(msg);
            throw new ConfigurationException(msg);
        }
        URL datasetUrl = new URL(e.getTextTrim());
        setDapDatasetUrl(datasetUrl);



        /**
         * Load DAP ID (The Fully Qualified Name) for the variable that holds the Latitude coordinate values
         * for this field.
         * This is REQUIRED.
         */
        List coordList = wcsCoverageConfig.getChildren("DomainCoordinate",LocalFileCatalog.NS);
        if(coordList.isEmpty()){
            msg = "ingestCatalog(): The configuration element is missing the required " +
                    "child element(s) 'DomainCoordinate'.";
            log.error(msg);
            throw new ConfigurationException(msg);
        }
        Iterator coordIt = coordList.iterator();
        while(coordIt.hasNext()){
            Element domainCoordinateElem =  (Element)coordIt.next();
            DomainCoordinate dc = new DomainCoordinate(domainCoordinateElem);
            _domainCoordinates.put(dc.getName(),dc);
        }


        /**
         * Process each lfc:field element in the coverage's range set..
         */
        for (Object o : wcsCoverageConfig.getChildren("field", LocalFileCatalog.NS)) {
            Element field = (Element) o;


            String fieldName;
            String dapGridId;


            /**
             * Load the field ID for this field in the coverage's range.
             * This is REQUIRED.
             */
            fieldName = field.getAttributeValue("name");
            if(fieldName==null){
                msg = "ingestCatalog(): In the catalog file "+coverageDescriptionFileName+" a lfc:field element is " +
                        "missing the required attribute 'name' .";
                log.error(msg);
                throw new ConfigurationException(msg);
            }


            /**
             * Load DAP ID (The Fully Qualified Name) for the Grid variable that holds the range data for this field.
             * This is REQUIRED.
             */

            String dapID = field.getAttributeValue("dapID");
            if(dapID==null){
                msg = "CoverageDescription(): In in the Catalog Configuration file, the field element"  +
                        "named "+fieldName+" is missing the required" +
                        "attribute  'dapID'.";
                log.error(msg);
                throw new ConfigurationException(msg);
            }
            setDapGridArrayId(fieldName,dapID);




        }


    }




    private void init(){

        if(initialized) return;

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _dapGridId = new HashMap<String,String>();
        _domainCoordinates = new LinkedHashMap<String, DomainCoordinate>();
        initialized = true;
    }



    private Element parseXmlDoc(InputStream is, boolean validateContent) throws JDOMException, IOException {
        // get a validating jdom parser to parse and validate the XML document.
        SAXBuilder validatingParser = new SAXBuilder("org.apache.xerces.parsers.SAXParser",validateContent);
        validatingParser.setFeature("http://apache.org/xml/features/validation/schema",validateContent);

        Document cdDoc = validatingParser.build(is);

        Element cd = cdDoc.getRootElement();

        cd.detach();

        return cd;

    }


    private Element ingestCoverageDescription(File cdFile) throws IOException, JDOMException, WcsException {
        String msg;

        if(!Util.isReadableFile(cdFile)){
            msg = "Problem with file: "+ cdFile.getName();
            log.error(msg);
            throw new IOException(msg);
        }

        log.info("Ingesting wcs:CoverageDescription from '{}'. XML Validation is {}.",cdFile,validateContent?"ON":"OFF");

        Element cd = parseXmlDoc(new FileInputStream(cdFile),validateContent);

        myFile = cdFile;

        lastModified = cdFile.lastModified();

        return cd;

    }

    private void qcCoverageDescriptionContent() throws WcsException {

        StringBuilder msg = new StringBuilder();

        if (getNativeFormat() == null) {
            msg.append("wcs:CoverageDescription is missing required component. " +
                    "wcs:CoverageDescription/wcs:ServiceParameters/wcs:nativeFormat");
            throw new WcsException(msg.toString(),WcsException.MISSING_PARAMETER_VALUE,"wcs:nativeFormat");

        }


    }


    /**
     * Returns the last modified time of the Coverage.
     * @return Returns the last modified time of the Coverage.
     *
     */
    public long lastModified() {

        try {
            if (myFile != null && lastModified < myFile.lastModified()) {
                myCD = ingestCoverageDescription(myFile);
            }
        }
        catch (Exception e) {

            String msg ="Failed to update CoverageDescription from file ";

            if(myFile != null){
                msg += myFile.getAbsoluteFile();
            }
            log.error(msg);
        }

        return lastModified;

    }


    /**
     * Checks the Coverage to see if it's range contains a particular field.
     * @param fieldID The value of the wcs:Identifier for the wcs:Field in question.
     * @return True if the Field is present, false otherwise.
     */
    public boolean hasField(String fieldID){
        boolean foundIt = false;

        Element range =  myCD.getChild("rangeType",WCS.GMLCOV_NS);

        if(range == null) return foundIt;

        Element dataRecord =  myCD.getChild("DataRecord",WCS.SWE_NS);

        if(dataRecord==null)  return foundIt;


        Element field;
        String id;
        Iterator i = dataRecord.getChildren("field",WCS.SWE_NS).iterator();

        while(i.hasNext()){
            field = (Element)i.next();
            id = field.getAttributeValue("name");
            if(id!=null && fieldID.equals(id))
                foundIt = true;

        }

        return foundIt;
    }






    /**
     *
     * @return Returns the value of the unique wcs:CoverageId associated with this CoverageDescription.
     */
    public String getCoverageId(){
        Element coverageId =  myCD.getChild("CoverageId",WCS.WCS_NS);
        return coverageId.getText();
    }

    /**
     *
     * @return Returns the unique wcs:Identifier associated with this CoverageDescription.
     */
    public Element getCoverageIdElement(){
        return (Element) myCD.getChild("CoverageId",WCS.WCS_NS).clone();
    }

    /**
     *
     * @return Returns the BoundingBox defined by the gml:boundedBy element, if such an element is present.
     * Returns null if no gml:boundedBy element is found.
     * @throws WcsException When bad things happen.
     */
    public BoundingBox getBoundingBox() throws WcsException {
        Element boundedBy =  myCD.getChild("boundedBy",WCS.GML_NS);
        if(boundedBy!=null)
            return new BoundingBox(boundedBy);
        return null;
    }


    public boolean hasBoundedBy(){
        Element boundedBy =  myCD.getChild("boundedBy",WCS.GML_NS);
        if(boundedBy!=null)
            return true;
        return false;

    }

    /**
     *
     * @return Returns the gml:boundedBy element associated with this CoverageDescription. Will
     * return null if no gml:boundedBy element is present.
     * @throws WcsException When bad things happen.
     */
    public Element getBoundedByElement() throws WcsException {
        Element boundedBy =  myCD.getChild("boundedBy",WCS.GML_NS);
        if(boundedBy!=null)
            return  (Element) boundedBy.clone();
        return null;
    }




    /**
     *
     * @return Returns the wcs:SupportedCRS elements associated with this CoverageDescription.
     */
    public List getSupportedCrsElements(){
        return  cloneElementList(myCD.getChildren("SupportedCRS",WCS.WCS_NS));
    }



    /**
     *
     * @return Returns the wcs:nativeFormat element associated with this CoverageDescription.
     */
    public Element getNativeFormatElement(){

        Element serviceParameters = myCD.getChild("ServiceParameters",WCS.WCS_NS);
        if(serviceParameters==null)
            return null;

        Element nativeFormat = serviceParameters.getChild("nativeFormat",WCS.WCS_NS);
        if(nativeFormat==null)
            return null;

        return  (Element) nativeFormat.clone();
    }


    public String getNativeFormat(){

        Element nativeFormatElement = getNativeFormatElement();
        if(nativeFormatElement==null)
            return null;

        return nativeFormatElement.getTextTrim();
    }


    public String getCoverageSubtype(){
        Element serviceParameters = myCD.getChild("ServiceParameters",WCS.WCS_NS);
        if(serviceParameters==null)
            return null;

        Element coverageSubtype = serviceParameters.getChild("CoverageSubtype",WCS.WCS_NS);
        if(coverageSubtype==null)
            return null;

        return  coverageSubtype.getTextTrim();

    }


    public Element getCoverageSubtypeElement(){
        Element serviceParameters = myCD.getChild("ServiceParameters",WCS.WCS_NS);
        if(serviceParameters==null)
            return null;

        Element coverageSubtype = serviceParameters.getChild("CoverageSubtype",WCS.WCS_NS);
        if(coverageSubtype==null)
            return null;

        return  (Element) coverageSubtype.clone();
    }

    public Element getCoverageSubtypeFamilyTree(){
        Element serviceParameters = myCD.getChild("ServiceParameters",WCS.WCS_NS);
        if(serviceParameters==null)
            return null;

        Element coverageSubtypeParent = serviceParameters.getChild("CoverageSubtypeParent",WCS.WCS_NS);
        if(coverageSubtypeParent==null)
            return null;

        return  (Element) coverageSubtypeParent.clone();
    }



    public boolean hasCoverageSubtypeFamilyTree(){
        Element serviceParameters = myCD.getChild("ServiceParameters",WCS.WCS_NS);
        if(serviceParameters==null)
            return false;

        Element coverageSubtypeParent = serviceParameters.getChild("CoverageSubtypeParent",WCS.WCS_NS);
        if(coverageSubtypeParent==null)
            return false;
        return true;

    }



    private List<Element> cloneElementList(List list){
        ArrayList<Element> newList = new ArrayList<Element>();

        Iterator i = list.iterator();
        Element e;

        while(i.hasNext()){
            e = (Element)i.next();
            newList.add((Element)e.clone());
        }

        return newList;
    }


    /**
     *
     *
     *
     <complexType name="CoverageSummaryType">
        <complexContent>
            <extension base="ows:DescriptionType">
                <sequence>
                    <element ref="ows:WGS84BoundingBox" minOccurs="0" maxOccurs="unbounded"/>
                        <!--
                        This item above is put at this position (and not next to ows:BoundingBox)
                        to avoid a problem of "ambiguity" (thanks, Joan Maso!):
                        ows:WGS84BoundingBox is on the same substitution group as ows:BoundingBox
                        so when instantiating ows:WGS84BoundingBox you do not know if its a WGS84BoundingBox
                        or an ows:BoundingBox substituded by a ows:WGS84BoundingBox.
                        Because order is relevant and the wcs:CoverageId separates both this ambiguity gets resolved.
                        -->
                    <element ref="wcs:CoverageId"/>
                    <element ref="wcs:CoverageSubtype"/>
                    <element ref="wcs:CoverageSubtypeParent" minOccurs="0"/>
                    <element ref="ows:BoundingBox" minOccurs="0" maxOccurs="unbounded"/>
                    <element ref="ows:Metadata" minOccurs="0" maxOccurs="unbounded"/>
                </sequence>
            </extension>
        </complexContent>
     </complexType>

     * @return Returns the wcs:CoverageSummary element that represents this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public Element getCoverageSummary() throws WcsException {

        Element covSum = new Element("CoverageSummary",WCS.WCS_NS);

        Element coverageID = getCoverageIdElement();
        covSum.addContent(coverageID);

        Element coverageSubType = getCoverageSubtypeElement();
        covSum.addContent(coverageSubType);

        if(hasCoverageSubtypeFamilyTree()){
            Element coverageSubtypeParent = getCoverageSubtypeFamilyTree();
            if(coverageSubtypeParent!=null)
                covSum.addContent(coverageSubtypeParent);
        }

        if(hasBoundedBy()){
            BoundingBox boundingBox = getBoundingBox();
            Element bb = boundingBox.getOwsBoundingBoxElement();
            covSum.addContent(bb);
        }

        //@todo Add metadata records if available.


        return covSum;
    }


    /**
     *
     * @return Returns the wcs:CoverageDescription element that represents this CoverageDescription.
     */
    public Element getElement(){
        return (Element) myCD.clone();
    }


    /**
     * Gets the DAP local ID for the DAP Grid variable data array  that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the DAP local ID for the DAP Grid variable data array  that is associated by the wcs:Identifier
     */
    public String getDapGridArrayId(String fieldID){
        return _dapGridId.get(fieldID);
    }

    /**
     * Sets the DAP local ID for the Grid variable data array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  dapGridId The DAP Variable ID of the Grid variable data array that is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setDapGridArrayId(String fieldID, String dapGridId){
        _dapGridId.put(fieldID,dapGridId);
    }

    /**
     * Gets the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @return  Returns the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     */
    @XmlTransient
    public LinkedHashMap<String,DomainCoordinate> getDomainCoordinatesLinkedHashMap() {


        LinkedHashMap<String,DomainCoordinate> dc = new LinkedHashMap<String,DomainCoordinate>();
        for(DomainCoordinate mydc: _domainCoordinates.values()){
            dc.put(mydc.getName(), new DomainCoordinate(mydc));
        }
        return dc;


    }
    @XmlElement(name="DomainCoordinate")
    public List<DomainCoordinate> getDomainCoordinates()
    {
    	return new ArrayList(getDomainCoordinatesLinkedHashMap().values());
    }

    public void setDomainCoordinatesLinkedHashMap(LinkedHashMap<String, DomainCoordinate> dc)
    {
      this._domainCoordinates = dc;	
    }
    
    /**
     * Gets the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @return  Returns the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     */
    public DomainCoordinate getDomainCoordinate(String coordinateName) {


        DomainCoordinate dc = _domainCoordinates.get(coordinateName);

        if(dc==null)
            return null;
        return new DomainCoordinate(dc);

    }






    public void setDapDatasetUrl(URL datasetUrl) throws MalformedURLException {

        dapDatasetUrl = new URL(datasetUrl.toExternalForm());
    }

    @XmlElement(name = "DapDatasetUrl")
    public URL getDapDatasetUrl()  {

        return dapDatasetUrl;
    }


    public String getGmlId(){

        return myCD.getAttributeValue("id",WCS.GML_NS);

    }

    public Vector<Element> getAbstractGmlTypeContent(){

        Element e;
        Vector<Element> abstractGmlTypeContent = new Vector<Element>();

        for (Object o : myCD.getChildren("metaDataProperty", WCS.GML_NS)) {
            e = (Element) o;
            abstractGmlTypeContent.add((Element) e.clone());
        }

        e = myCD.getChild("description",WCS.GML_NS);
        if(e!=null)
            abstractGmlTypeContent.add((Element) e.clone());

        e = myCD.getChild("descriptionReference",WCS.GML_NS);
        if(e!=null)
            abstractGmlTypeContent.add((Element) e.clone());


        e = myCD.getChild("identifier",WCS.GML_NS);
        if(e!=null)
            abstractGmlTypeContent.add((Element) e.clone());


        for (Object o : myCD.getChildren("name", WCS.GML_NS)) {
            e = (Element) o;
            abstractGmlTypeContent.add((Element) e.clone());
        }

        return abstractGmlTypeContent;


    }

    public Vector<Element> getAbstractFeatureTypeContent(){

        Element e;
        Vector<Element> abstractFeatureTypeContent = getAbstractGmlTypeContent();

        e = myCD.getChild("boundedBy",WCS.GML_NS);
        if(e!=null)
            abstractFeatureTypeContent.add((Element) e.clone());

        e = myCD.getChild("location",WCS.GML_NS);
        if(e!=null)
            abstractFeatureTypeContent.add((Element) e.clone());

        return abstractFeatureTypeContent;

    }

    public Element getDomainSet() throws WcsException {

        Element domainSet;

        domainSet = myCD.getChild("domainSet",WCS.GML_NS);
        if(domainSet!=null)
            return (Element) domainSet.clone();

        throw new WcsException("wcs:CoverageDescription is missing a gml:domainSet: ",
                WcsException.MISSING_PARAMETER_VALUE,"gml:domainSet");
    }

    public Element getRangeType() throws WcsException {

        Element rangeType;

        rangeType = myCD.getChild("rangeType",WCS.GMLCOV_NS);
        if(rangeType!=null)
            return (Element) rangeType.clone();

        throw new WcsException("wcs:CoverageDescription is missing a gmlcov:rangeType: ",
                WcsException.MISSING_PARAMETER_VALUE,"gmlcov:rangeType");
    }

    @XmlElement(name="field")
    public List<Field> getFieldsList()
    {
    	//System.out.println("Yaay...JAXB...Came into getFieldsList");
    	if (this.fields == null || this.fields.isEmpty()) 
    	{
    		try
    		{
    			fields.addAll(getFields());
    		}
    		catch  (Exception e)
    		{
    			
    		}
    	}
    	
    	return this.fields;
    }
    
    
    public Vector<Field> getFields() throws WcsException {
     
    	
    	//if (this.fields != null || !this.fields.isEmpty()) return new Vector<Field> (this.fields);
    	
    	
        Element rangeType;

        rangeType = myCD.getChild("rangeType",WCS.GMLCOV_NS);
        if(rangeType==null)
            throw new WcsException("wcs:CoverageDescription is missing a gmlcov:rangeType: ",
                WcsException.MISSING_PARAMETER_VALUE,"gmlcov:rangeType");

        Vector<Field> fields = new Vector<Field>();

        ElementFilter filter = new ElementFilter("field",WCS.SWE_NS);
        Iterator i = rangeType.getDescendants(filter);
        while(i.hasNext()){
            Element fieldElement = (Element) i.next();

            Field field = new Field(fieldElement);
            fields.add(field);

        }

        return fields;
    }

    public void setFields(List<Field> fields) {
		this.fields = fields;
	}

    
    @XmlAttribute(name = "fileName")
	public File getMyFile() {
		return myFile;
	}


	public void setMyFile(File myFile) {
		this.myFile = myFile;
	}
	
	



}
