package opendap.wcs.v2_0;

import net.opengis.gml.v_3_2_1.*;
import net.opengis.wcs.v_2_0.CoverageDescriptionType;
import opendap.dap4.*;
import opendap.namespaces.XML;
import opendap.threddsHandler.ThreddsCatalogUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class DynamicCoverageDescription extends CoverageDescription {
    private Logger _log;
    private Element _myDMR;
    private SimpleSrs _defaultSrs;


    public DynamicCoverageDescription() {
        super();
        _log = LoggerFactory.getLogger(getClass());
        _myDMR = null;
    }

    /**
     * Primary constructor for this class
     *
     * @param dmr
     * @throws IOException
     */
    public DynamicCoverageDescription(Element dmr, SimpleSrs defaultSrs) throws IOException, WcsException {
        this();
        _myDMR = dmr;

        if(defaultSrs==null)
            throw new WcsException("There must be a default SRS for the coverage!",WcsException.NO_APPLICABLE_CODE);
        _defaultSrs = defaultSrs;

        // TODO: Get the dataset URL from the DMR top level attribute "xml:base"
        String datasetUrl = dmr.getAttributeValue("base", XML.NS);
        setDapDatasetUrl(new URL(datasetUrl));

        /** - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *  TODO:  Replace this stuff with the output of WcsMarchaller
         */

        ingestDmr(dmr);

        if (_myCD == null) {
            _myCD = new Element("CoverageDescription", WCS.WCS_NS);
            Element coverageId = new Element("CoverageId", WCS.WCS_NS);
            String name = _myDMR.getAttributeValue("name");
            coverageId.setText(name);
            _myCD.addContent(coverageId);
        }

        /** - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

        //TODO: All this stuff needs reviewed?
        ////////////////////////////////////
        //  use DOM to directly set member variables

        //_myFile = cd._myFile.getCanonicalFile();
        //_validateContent = cd._validateContent;
        //_dapGridId.putAll(cd._dapGridId);
        //_domainCoordinates.putAll(dataset.getDimensions());
        //this.fields = dataset.getVars32bitFloats();
        //_initialized = cd._initialized;

        /**
         * TODO: Populate the parent class's (CoverageDescription) internal objects including: _myCD, _gridIds, _domainCoordinates, and _fields from WcsMarchaller
         * The parent class may need additional setter/getters or protected variables
         * in order to fufill this.
         */

    }

    /**
     * Abandon this version for the version that takes the DMR as a JDOM element (see above)
     * <p>
     * From dap4dmr branch version of CoverageDescription
     *
     * @param datasetUrl
     * @throws IOException
     */
    @Deprecated
    public DynamicCoverageDescription(String datasetUrl) throws IOException, JDOMException, WcsException {
        super();
        setDapDatasetUrl(new URL(datasetUrl));

        String dmrUrl = datasetUrl + ".dmr.xml";
        Element dmr = opendap.xml.Util.getDocumentRoot(dmrUrl);

        ingestDmr(dmr);

        ////////////////////////////////////
        //  use DOM to directly set member variables

        //_myFile = cd._myFile.getCanonicalFile();
        //_validateContent = cd._validateContent;
        //_dapGridId.putAll(cd._dapGridId);
        //_domainCoordinates.putAll(dataset.getDimensions());
        //this.fields = dataset.getVars32bitFloats();
        //_initialized = cd._initialized;

    }

    private void ingestDmr(Element dmr) throws WcsException {

        /////////////////////////////////////////////////
        // Use OLFS method to fetch the DMR
        //
        // For this to work, against a NASA server
        // then you will need to have valid
        // Earthdata Login credentials available in the
        // local filesystem like so:
        // In Unix ~/.netrc should have
        // machine urs.earthdata.nasa.gov
        // login userName
        // password password
        //
        // Then login to earthdata and add the respective
        // dataset to user profile

        // Object wrapper to DMR XML

        JAXBContext jc = null;
        Dataset dataset = null;
        try {
            jc = JAXBContext.newInstance(Dataset.class);
            Unmarshaller um = jc.createUnmarshaller();
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            String dmrXml = xmlo.outputString(dmr);
            InputStream is = new ByteArrayInputStream(dmrXml.getBytes("UTF-8"));
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader xsr = factory.createXMLStreamReader(is);
            XMLReaderWithNamespaceInMyPackageDotInfo xr = new XMLReaderWithNamespaceInMyPackageDotInfo(xsr);
            dataset = (Dataset) um.unmarshal(xr);
        } catch (JAXBException | UnsupportedEncodingException | XMLStreamException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to build Dataset instance from JDOM DMR document.");
            sb.append(" Caught ").append(e.getClass().getName());
            sb.append(" Message  ").append(e.getMessage());
            _log.error(sb.toString());
            throw new WcsException(sb.toString(), WcsException.NO_APPLICABLE_CODE);
        }

        /////////////////////////////////////////////////////////////////////////
        // interpret contents of the dataset (DMR) to generate WCS per OGC below.

        // First Loop through the variables to glean "knowledge"

        // FIXME Test the null condition above in the try/catch block and exit there
        // with an exception the WCS service code can catch and process. Doing that
        // will also reduce one level of nesting down here. jhrg 9/7/17
        if (dataset == null) {
            _log.debug("DMR dataset....NULL; bye-bye");
            throw new WcsException("Failed to build Dataset instance from DMR.", WcsException.NO_APPLICABLE_CODE);
        }
        //////////////////////////////////////////////////////////
        // this else block extends all the way to almost
        // end of program with brace commented with }
        // end if (dataset==null)

        // FIXME If you need this, put it in a method and call it when the logger is
        // set to the DEBUG level. ... Sanity check, too. jhrg 9/7/17

        _log.debug("Marshalling WCS from DMR at Url: {}", dataset.getUrl());


        CoverageDescriptionType cd = new CoverageDescriptionType();
        URL datasetUrl = null;
        try {
            datasetUrl = new URL(dmr.getAttributeValue("base", XML.NS));
        } catch (MalformedURLException e) {
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }

        ingestDomainCoordinates(dataset);

        DomainCoordinate lat = getDomainCoordinate("latitude");
        DomainCoordinate lon = getDomainCoordinate("longitude");

        // compute the envelope from dataset
        EnvelopeWithTimePeriod envelopeWithTimePeriod = new EnvelopeWithTimePeriod();

        envelopeWithTimePeriod.setNorthernmostLatitude(dataset.getValueOfGlobalAttributeWithNameLike("NorthernmostLatitude"));
        envelopeWithTimePeriod.setSouthernmostLatitude(dataset.getValueOfGlobalAttributeWithNameLike("SouthernmostLatitude"));
        envelopeWithTimePeriod.setEasternmostLongitude(dataset.getValueOfGlobalAttributeWithNameLike("EasternmostLongitude"));
        envelopeWithTimePeriod.setWesternmostLongitude(dataset.getValueOfGlobalAttributeWithNameLike("WesternmostLongitude"));

        envelopeWithTimePeriod.setRangeBeginningDate(dataset.getValueOfGlobalAttributeWithNameLike("RangeBeginningDate"));
        envelopeWithTimePeriod.setRangeBeginningTime(dataset.getValueOfGlobalAttributeWithNameLike("RangeBeginningTime"));
        envelopeWithTimePeriod.setRangeEndingDate(dataset.getValueOfGlobalAttributeWithNameLike("RangeEndingDate"));
        envelopeWithTimePeriod.setRangeEndingTime(dataset.getValueOfGlobalAttributeWithNameLike("RangeEndingTime"));

        _log.debug(envelopeWithTimePeriod.toString());

        net.opengis.gml.v_3_2_1.EnvelopeWithTimePeriodType envelope = envelopeWithTimePeriod.getEnvelope(_defaultSrs);

        net.opengis.gml.v_3_2_1.BoundingShapeType bs = new net.opengis.gml.v_3_2_1.BoundingShapeType();
        net.opengis.gml.v_3_2_1.ObjectFactory gmlFactory = new net.opengis.gml.v_3_2_1.ObjectFactory();
        bs.setEnvelope(gmlFactory.createEnvelopeWithTimePeriod(envelope));

        // Grid Envelope
        net.opengis.gml.v_3_2_1.GridEnvelopeType gridEnvelope = gmlFactory.createGridEnvelopeType();

        ////////////////////////////////////////////////////////////
        // Crucial member variable state setting...
        // Note: The index values for the arrays are 0 based and so the upper index is
        // one less than the size.
        List<BigInteger> upper = Arrays.asList(BigInteger.valueOf(lat.getSize()-1), BigInteger.valueOf(lon.getSize()-1));
        List<BigInteger> lower = Arrays.asList(BigInteger.ZERO, BigInteger.ZERO);
        gridEnvelope.withHigh(upper).withLow(lower);
        ////////////////////////////////////////////////////////////

        // Create the limits, set the envelope on them.
        GridLimitsType gridLimits = gmlFactory.createGridLimitsType();
        gridLimits.withGridEnvelope(gridEnvelope);

        net.opengis.gml.v_3_2_1.DomainSetType domainSet = new net.opengis.gml.v_3_2_1.DomainSetType();
        net.opengis.gml.v_3_2_1.RectifiedGridType rectifiedGrid = new net.opengis.gml.v_3_2_1.RectifiedGridType();
        rectifiedGrid.setDimension(new BigInteger(this.getDomainCoordinates().size()+""));
        rectifiedGrid.setId(dataset.getCoverageId());

        //Create the grid envelope for the limits
        rectifiedGrid.setLimits(gridLimits);

        List<String> axisLabels = _defaultSrs.getAxisLabelsList();
        rectifiedGrid.setAxisLabels(axisLabels);

        // Create the Origin.
        DirectPositionType position = gmlFactory.createDirectPositionType();
        position.withValue(Double.valueOf(envelopeWithTimePeriod.getSouthernmostLatitude()),
                           Double.valueOf(envelopeWithTimePeriod.getWesternmostLongitude()));
        PointType point = gmlFactory.createPointType();
        point.withPos(position);
        point.setId("GridOrigin-" + dataset.getCoverageId());
        point.setSrsName(_defaultSrs.getName());
        PointPropertyType origin = gmlFactory.createPointPropertyType();
        origin.withPoint(point);
        rectifiedGrid.setOrigin(origin);

        // Create the offset vector.
        List<VectorType> offsetList = new ArrayList<VectorType>();
        VectorType offset1 = gmlFactory.createVectorType();
        offset1.withValue(dataset.getLatitudeResolution(), 0.0);
        offset1.setSrsName(_defaultSrs.getName());
        offsetList.add(offset1);
        VectorType offset2 = gmlFactory.createVectorType();
        offset2.withValue(0.0, dataset.getLongitudeResolution());
        offset2.setSrsName(_defaultSrs.getName());
        offsetList.add(offset2);
        rectifiedGrid.setOffsetVector(offsetList);

        domainSet.setAbstractGeometry(gmlFactory.createRectifiedGrid(rectifiedGrid));
        cd.setDomainSet(gmlFactory.createDomainSet(domainSet));
        cd.setBoundedBy(bs);


        net.opengis.swecommon.v_2_0.DataRecordPropertyType rangeType = new  net.opengis.swecommon.v_2_0.DataRecordPropertyType();
        net.opengis.swecommon.v_2_0.DataRecordType dataRecord = new net.opengis.swecommon.v_2_0.DataRecordType();
        List<net.opengis.swecommon.v_2_0.DataRecordType.Field> fieldList = new ArrayList<net.opengis.swecommon.v_2_0.DataRecordType.Field>();


        for(Variable var : dataset.getVariables()){
          if (compareVariableDimensionsWithDataSet(var, dataset)) {
            fieldList.add(getField(var));
          }
        }

        dataRecord.setField(fieldList);
        rangeType.setDataRecord(dataRecord);
        cd.setRangeType(rangeType);


        hardwireTheCdAndDcdForTesting(dataset.getCoverageId(), datasetUrl, cd);
    }



    //FIXME The contents of this loop should be refactored to use a loop similar but not exactly like the psudo-code one in the comment below.
    void ingestDomainCoordinates(Dataset dataset) throws WcsException {
        /*
        for(String dimName:_defaultSrs.getAxisLabelsList()){
            Variable coord = dataset.getVariable(dimName);
            List<Dim> dims = coord.getDims();
            if(dims.size()>1)
                throw new WcsException("",WcsException.NO_APPLICABLE_CODE);
            // Figure out dimension size

            DomainCoordinate dc = new DomainCoordinate(dimName,coord.getName(),coord.getAttributeValue("units"),"",)

        }
        */


        Variable time = dataset.getVariable("time");
        Variable latitude  = dataset.getVariable("lat");
        Variable longitude  = dataset.getVariable("lon");

        DomainCoordinate lat, lon, tim;
        try {

            tim = new DomainCoordinate(time.getAttributeValue("long_name"),
                    time.getAttributeValue("standard_name"),
                    time.getAttributeValue("units"),
                    "",
                    dataset.getSizeOfDimensionWithNameLike("time"));

            lat = new DomainCoordinate(latitude.getAttributeValue("long_name"),
                    latitude.getAttributeValue("standard_name"),
                    latitude.getAttributeValue("units"),
                    "",
                    dataset.getSizeOfDimensionWithNameLike("lat"));

            lon = new DomainCoordinate(longitude.getAttributeValue("long_name"),
                    longitude.getAttributeValue("standard_name"),
                    longitude.getAttributeValue("units"),
                    "",
                    dataset.getSizeOfDimensionWithNameLike("lon"));
        } catch (BadParameterException e) {
            // This shouldn't happen based on the stuff above...
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }
        ////////////////////////////////////////////////////////////
        // Crucial member variable state setting...
        this.addDomainCoordinate(tim);
        this.addDomainCoordinate(lat);
        this.addDomainCoordinate(lon);
        /////////////////////////////////////////////////////////////


    }


    public Element coverageDescriptionType2JDOM(CoverageDescriptionType cd) throws WcsException {

        // Boiler plate JAXB marshaling of Coverage Description object into JDOM

        ////////////////////////////////////////////////////////
        // Since this was generated from third-party XML schema
        // need to bootstrap the JAXBContext
        // from the package name of the generated model
        // or the ObjectFactory class
        // (i.e. just have to know the package: net.opengis.wcs.v_2_0)

        // Required: First, bootstrap context with known WCS package name

        Marshaller jaxbMarshaller;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("net.opengis.wcs.v_2_0");
            jaxbMarshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            String msg = "Failed to get JAXB Marshaller! JAXBException Message: " + e.getMessage();
            _log.error(msg);
            throw new WcsException(msg, WcsException.NO_APPLICABLE_CODE);
        }

        try {

            // optional:  output "pretty printed"
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // optional: this is a list of the schema definitions.
            jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://www.opengis.net/wcs/2.0 http://schemas.opengis.net/wcs/2.0/wcsAll.xsd " +
                            "http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd " +
                            "http://www.opengis.net/gmlcov/1.0 http://schemas.opengis.net/gmlcov/1.0/gmlcovAll.xsd " +
                            "http://www.opengis.net/swe/2.0 http://schemas.opengis.net/sweCommon/2.0/swe.xsd");

            // optional:  capture namespaces per MyMapper, instead of ns2, ns8 etc
            //jaxbMarshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new MyNamespaceMapper());
            jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespaceMapper());

        } catch (PropertyException e) {
            _log.warn("NON-FATAL ISSUE WARNING: Another JAXB impl (not the reference implementation) is being used" +
                    "...namespace prefixes like wcs, gml will not show up...instead you will ns2, ns8 etc. Message" + e.getMessage());
        }

        //////////////////////////////////////////////////////////////////////////////////////
        // per https://stackoverflow.com/questions/819720/no-xmlrootelement-generated-by-jaxb
        // method#1:  need to wrap CoverageDescription as JAXB element
        // marshal coverage description into console (more specifically, System.out)
        //jaxbMarshaller.marshal(new JAXBElement(new QName("http://www.opengis.net/wcs/2.0", "wcs"), CoverageDescriptionType.class, cd), System.out);

        // TODO: marshal this into the OLFS JDOM object representation of CoverageDescription...more directly

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            String msg = "Failed to get DocumentBuilder! ParserConfigurationException Message: " + e.getMessage();
            _log.error(msg);
            throw new WcsException(msg, WcsException.NO_APPLICABLE_CODE);
        }
        Document doc = db.newDocument();

        //////////////////////////////////////////////////////////////////////////////////////////
        // per https://stackoverflow.com/questions/819720/no-xmlrootelement-generated-by-jaxb/
        // method#2: wrap WCS Coverage Description as JAXB Element using Object Factory
        // marshal coverage description into a org.w3c.dom.Document...first

        // ... and then convert the resultant org.w3c.dom.Document to JDOM (1.1.3) ..which is what OLFS runs on
        // (for JDOM 2, the Builder would be org.jdom2.input.DOMBuilder)
        net.opengis.wcs.v_2_0.ObjectFactory wcsObjFactory = new net.opengis.wcs.v_2_0.ObjectFactory();
        try {
            jaxbMarshaller.marshal(wcsObjFactory.createCoverageDescription(cd), doc);
        } catch (JAXBException e) {
            String msg = "Failed to get marshall COverageDescription! JAXBException Message: " + e.getMessage();
            _log.error(msg);
            throw new WcsException(msg, WcsException.NO_APPLICABLE_CODE);
        }
        org.jdom.input.DOMBuilder jdb = new org.jdom.input.DOMBuilder();
        org.jdom.Document jdoc = jdb.build(doc);

        // gotcha!  This is what integrates into OLFS (mostly).
        // The rest of CoverageDescription object can be derive from whatever has been captured so far
        // or from _myCD (TODO).

        Element cdElement = jdoc.getRootElement();
        cdElement.detach();

        // couple of quick sanity checks
        _log.debug(cdElement.toString());
        Element coverageId = cdElement.getChild("CoverageId", WCS.WCS_NS);
        _log.debug(coverageId.getText());
        return  cdElement;
    }

 

    private boolean compareVariableDimensionsWithDataSet(Variable var, Dataset dataset)
    {
    	boolean flag = true;
    	
    	List<Dim> vdims = var.getDims();
    	List<Dimension> dimensions = dataset.getDimensions();
    	
    	if (vdims == null || dimensions == null || vdims.isEmpty() || dimensions.isEmpty())
    	{
    	  return false;
    	}
    	else if (vdims.size() == dimensions.size())
    	{
    	  _log.debug("Examining dimension of Variable " + var.getName() + " which has same number of dimensions as Dataset, " + vdims.size());
    	  for (Dim dim : var.getDims()) {
          boolean found = false;
          String dimName = dim.getName();
          if (dimName.charAt(0) == '/') dimName = dimName.substring(1);
          _log.debug("Look at " + var.getName() + " dimension " + dimName + ", assume it is not in dataset to begin with");
    	    for (Dimension dimension : dataset.getDimensions()) {
    	      _log.debug("comparing variable dimension " + dimName + " with Dataset dimension name " + dimension.getName() );

    	      // probably need a better test
    	      if (dimName.equalsIgnoreCase(dimension.getName())) found = true;
    	      
    	     
    	     if (found) {
    	       _log.debug("Dimension " + dimName + " found in Dataset");
    	       break;
    	     }
    	   }
        }
    	}
    	else
    	{
    	  flag = false;
    	  _log.debug("Variable " + var.getName() + " has " + vdims.size() + " dimensions, while Dataset has " + dimensions.size());
    	}
    	
    	if (flag)
    	{
    	  _log.debug("All dimensions in Variable " + var.getName() + " match DataSet, so it will be included in WCS coverage ");
    	}
    	else
    	{
    	  _log.debug("All dimensions in Variable " + var.getName() + " did NOT match DataSet, so it will be NOT included in WCS coverage ");
    	}
    	
    	return flag;
    }

   /**
    * generates a DataRecord.Field from Dap4 variable
    */
    private net.opengis.swecommon.v_2_0.DataRecordType.Field getField(Variable var)
    {
    	net.opengis.swecommon.v_2_0.DataRecordType.Field field =
    			new net.opengis.swecommon.v_2_0.DataRecordType.Field();

      field.setName(var.getName());
      
      net.opengis.swecommon.v_2_0.QuantityType quantity = new net.opengis.swecommon.v_2_0.QuantityType();
      quantity.setDefinition("urn:ogc:def:dataType:OGC:1.1:measure");
      quantity.setDescription(var.getAttributeValue("long_name"));

      net.opengis.swecommon.v_2_0.UnitReference uom = new net.opengis.swecommon.v_2_0.UnitReference();
      uom.setCode(var.getAttributeValue("units"));
      quantity.setUom(uom);

      net.opengis.swecommon.v_2_0.AllowedValuesPropertyType allowedValues = new net.opengis.swecommon.v_2_0.AllowedValuesPropertyType();
      net.opengis.swecommon.v_2_0.AllowedValuesType allowed = new net.opengis.swecommon.v_2_0.AllowedValuesType();
      List<Double> allowedInterval = Arrays.asList(Double.valueOf(var.getAttributeValue("vmin")),
              Double.valueOf(var.getAttributeValue("vmax")));
      List<JAXBElement<List<Double>>> coordinates = new Vector<JAXBElement<List<Double>>>();
      net.opengis.swecommon.v_2_0.ObjectFactory sweFactory = new net.opengis.swecommon.v_2_0.ObjectFactory();
      coordinates.add(sweFactory.createAllowedValuesTypeInterval(allowedInterval));
      allowed.setInterval(coordinates);
      allowedValues.setAllowedValues(allowed);
      quantity.setConstraint(allowedValues);

      field.setAbstractDataComponent(sweFactory.createAbstractDataComponent(quantity)); 
      
      /////////////////////////////////////////////////////////////
      // Crucial member variable state setting...
      this.addFieldToDapVarIdAssociation(var.getName(),var.getName());
      /////////////////////////////////////////////////////////////


    	return field;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Out friend main() runs a sanity check using a DMR obtained from test.opendap.org
     * @param args Ignored...
     */
    public static void main(String[] args) {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String testDmrUrl = "https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml";

        testDmrUrl = "http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml";
        try {
            ThreddsCatalogUtil tcc = new ThreddsCatalogUtil();
            org.jdom.Document dmrDoc = tcc.getDocument(testDmrUrl);
            Element dmrElement = dmrDoc.getRootElement();
            dmrElement.detach();

            SimpleSrs defaultSrs = new SimpleSrs("urn:ogc:def:crs:EPSG::4326","latitude longitude","deg deg",2);
            CoverageDescription cd = new DynamicCoverageDescription(dmrElement,defaultSrs);

            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            System.out.println("RESULT: " + cd.toString());
            xmlo.output(cd.getCoverageDescriptionElement(), System.out);
            System.out.println("");
            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            xmlo.output(cd.getCoverageSummary(), System.out);
            System.out.println("");
            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    
    private void hardwireTheCdAndDcdForTesting( String id,
    		                                    URL datasetURl,
    		                                    CoverageDescriptionType cd) throws WcsException {
        cd.setCoverageId(id);
        cd.setId(id);

        ////////////////////////////////////////////////////////////
        // Crucial member variable state setting...
        this.setDapDatasetUrl(datasetURl);
        ////////////////////////////////////////////////////////////


        net.opengis.wcs.v_2_0.ServiceParametersType serviceParameters = new net.opengis.wcs.v_2_0.ServiceParametersType();
        net.opengis.wcs.v_2_0.ObjectFactory wcsFactory = new net.opengis.wcs.v_2_0.ObjectFactory();
        serviceParameters
                .setCoverageSubtype(new QName("http://www.opengis.net/wcs/2.0", "RectifiedGridCoverage", "wcs"));
        serviceParameters.setNativeFormat("application/octet-stream");

        cd.setServiceParameters(serviceParameters);

        _myCD = coverageDescriptionType2JDOM(cd);

    }

}
