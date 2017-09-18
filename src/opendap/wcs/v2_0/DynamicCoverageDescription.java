package opendap.wcs.v2_0;

import net.opengis.gml.v_3_2_1.*;
import net.opengis.swecommon.v_2_0.DataRecordPropertyType;
import net.opengis.swecommon.v_2_0.DataRecordType;
import net.opengis.wcs.v_2_0.CoverageDescriptionType;
import opendap.dap4.*;
import opendap.namespaces.XML;
import opendap.wcs.srs.SimpleSrs;
import org.jdom.Element;
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
import java.net.URL;
import java.util.*;

import static java.lang.Double.NaN;

/**
 * This class embodies the mapping between a DAP DMR and a WCS Coverage.
 * It's main trick is that it can, utilizing a DMR document as input
 * to it's constructor, output a wc:CoverageDescription document
 * representing the DAP dataset. It also serves the operational
 * role in the service, mapping WCS requests into DAP requests
 * that are used to provide the expected WCS response content.
 *
 * @todo This class needs a thoughtful "serialization" to persist itself
 * so that we don't have to do the dynamic part every single time.
 */
public class DynamicCoverageDescription extends CoverageDescription {
    private Logger _log;
    private Element _myDMR;
    private DynamicService _dynamicService;
    private SimpleSrs _srs;


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
    public DynamicCoverageDescription(Element dmr, DynamicService dynamicService) throws IOException, WcsException {
        this();
        _myDMR = dmr;

        if (dynamicService == null)
            throw new WcsException("There must be a DynamicService associated with the coverage!",
                    WcsException.NO_APPLICABLE_CODE);
        _dynamicService = dynamicService;

        ingestDmr(dmr);

        // If it all went south we just make an empt one as null fighting punt.
        if (_myCD == null) {
            _myCD = new Element("CoverageDescription", WCS.WCS_NS);
            Element coverageId = new Element("CoverageId", WCS.WCS_NS);
            String name = _myDMR.getAttributeValue("name");
            coverageId.setText(name);
            _myCD.addContent(coverageId);
        }


    }

    /**
     * This method uses a DMR to build state into the CoverageDescription
     *
     * @param dmr
     * @throws WcsException
     */
    private void ingestDmr(Element dmr) throws IOException, WcsException {
        _log.debug("ingestDMR() - BEGIN");

        Dataset dataset = buildDataset(dmr);
        _log.debug("Marshalling WCS from DMR at Url: {}", dataset.getUrl());

        CoverageDescriptionType cd = new CoverageDescriptionType();
        cd.setCoverageId(dataset.getName());
        cd.setId(dataset.getName());

        String datasetUrl = dmr.getAttributeValue("base", XML.NS);
        setDapDatasetUrl(new URL(datasetUrl));

        addServiceParameters(cd);

        ingestSrsFromDataset(dataset, _dynamicService.getSrs());
        ingestDomainCoordinates(dataset);
        addBoundedByAndDomainSet(cd, dataset,_srs);
        addRange(cd, dataset, _srs);

        // Last Step, woot.
        // Produce the JDOM object for the CoverageDescription
        _myCD = coverageDescriptionType2JDOM(cd);

        if (_log.isDebugEnabled()) {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            _log.debug("ingestDMR() - END  Built CoverageDescription:\n{}", xmlo.outputString(_myCD));
        }

    }

    /**
     * Uses JAXB to build a Dataset object from the passed DMR.
     *
     * @param dmr The root element of the DMR document to process
     * @return The Dataset object created by JAXB.
     * @throws WcsException When the bad things happen.
     */
    private Dataset buildDataset(Element dmr) throws WcsException {
        try {
            JAXBContext jc = JAXBContext.newInstance(Dataset.class);
            Unmarshaller um = jc.createUnmarshaller();
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            String dmrXml = xmlo.outputString(dmr);
            InputStream is = new ByteArrayInputStream(dmrXml.getBytes("UTF-8"));
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader xsr = factory.createXMLStreamReader(is);
            XMLReaderWithNamespaceInMyPackageDotInfo xr = new XMLReaderWithNamespaceInMyPackageDotInfo(xsr);
            Dataset dataset = (Dataset) um.unmarshal(xr);
            if (dataset == null) {
                String msg = "JAXB failed to produce a Dataset from the DMR.";
                _log.debug(msg);
                throw new WcsException(msg, WcsException.NO_APPLICABLE_CODE);
            }
            return dataset;
        } catch (JAXBException | UnsupportedEncodingException | XMLStreamException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to build Dataset instance from JDOM DMR document.");
            sb.append(" Caught ").append(e.getClass().getName());
            sb.append(" Message  ").append(e.getMessage());
            _log.error(sb.toString());
            throw new WcsException(sb.toString(), WcsException.NO_APPLICABLE_CODE);
        }
    }

    /**
     * Adds wcs:ServiceParameters section to the CoverageDescription
     *
     * @param cd The instance of CoverageDescriptionType to which to add the stuff.
     * @throws WcsException
     */
    private void addServiceParameters(CoverageDescriptionType cd) throws WcsException {
        net.opengis.wcs.v_2_0.ServiceParametersType serviceParameters = new net.opengis.wcs.v_2_0.ServiceParametersType();
        serviceParameters.setCoverageSubtype(
                new QName(
                        "http://www.opengis.net/wcs/2.0",
                        "RectifiedGridCoverage",
                        "wcs")
        );
        serviceParameters.setNativeFormat("application/octet-stream");
        cd.setServiceParameters(serviceParameters);
    }


    /**
     * This method will (in the future) examine the DAP Dataset and see if an SRS reference for the Dataset
     * can be located. Lacking such a reference the default SRS will be utilized.
     *
     * @param dataset    The dataset to examine.
     * @param defaultSrs The default SRS.
     */
    public void ingestSrsFromDataset(Dataset dataset, SimpleSrs defaultSrs) {
        // TODO We need to examine the Dataset metadata and see if there is something we can use to determine the SRS
        _srs = new SimpleSrs(defaultSrs);
    }

    /**
     * Examines the passed Dataset object and determines the DomainCoordinates for the coverage. This activity
     * utilizes the DynamicService to determine the domain coordinates and their order.
     * The results are added as state to the object and thus set the stage for later
     * deciding which DAP variables will be fields in the coverage, and later for building functional DAP data requests
     * to service the  WCS GetCoverage  operation for the coverage.
     * <p>
     * The DAP fields in the coverage will have to have the dimensions in the same order as defined
     * the DynamicService instance held by this class.
     *
     * @param dataset
     * @throws WcsException
     */
    private void ingestDomainCoordinates(Dataset dataset) throws WcsException {
        try {
            // Everyone thinks that somehow Time is a "special" coordinate (Oy, still with that) but
            // it's really not, so we handle it like any other coordinate
            // It should be in the list from the DynamicService if there is a time coordinate.
            // FIXME Should this be iterating over the SRS dimension or at leaset checking alignment?

            for (DomainCoordinate defaultCoordinate : _dynamicService.getDomainCoordinates()) {
                DomainCoordinate domainCoordinate = getDomainCoordinate(defaultCoordinate, dataset);
                addDomainCoordinate(domainCoordinate);
            }
        } catch (BadParameterException e) {
            throw new WcsException("Failed to create DomainCoordinate ", WcsException.NO_APPLICABLE_CODE);
        }
    }

    /**
     * The code builds a DomainCoordinate starting with a default. It examines the dataset and if the DomainCoordinate
<<<<<<< Updated upstream
     * can be located then the Dataset version is used to populate the new DomainCoordinate, otherwise the default values
=======
     * can be located, then the Dataset version is used to populate the new DomainCoordinate, otherwise the default values
>>>>>>> Stashed changes
     * are used to construct the new DomainCoordinate
     *
     * @param defaultCoordinate
     * @param dataset
     * @return The new DomainCoordinate.
     * @throws BadParameterException
     * @throws WcsException
     */
    private DomainCoordinate getDomainCoordinate(DomainCoordinate defaultCoordinate, Dataset dataset)
            throws BadParameterException, WcsException {

        DomainCoordinate domainCoordinate;
        String coordinateName = defaultCoordinate.getName();
        Variable coordinateVariable = findVariableWithCfStandardName(dataset, coordinateName);
        if (coordinateVariable != null) {
            String units = coordinateVariable.getAttributeValue("units");
            if (units == null)
                units = defaultCoordinate.getUnits();

            Dimension coordinateDimension = getDomainCoordinateVariableDimension(dataset, coordinateName);

            long size = coordinateDimension.getSizeAsLong();
            if(size<1)
                size = defaultCoordinate.getSize();

            domainCoordinate = new DomainCoordinate(
                    coordinateName,  // This is the WCS coordinate name as defined in the SRS
                    coordinateVariable.getName(), // This is the name of the corresponding DAP variable
                    units,  // The units string, typically deg or the like
                    "",
                    size,
                    defaultCoordinate.getRole());

        } else {
            domainCoordinate = new DomainCoordinate(defaultCoordinate);
        }

        return domainCoordinate;

    }


    /**
     * Returns the Dap 4 variable with CF compliant standard name.  The standard name is actually a
     * value of an attribute named standard_name.  This method will look for this whether or not
     * the underlying DMR Dataset is CF compliant
     *
     * @param dataset       - a JAXB representation of the DMR response
     * @param standard_name
     * @return opendap.dap4.Variable
     * @throws WcsException throw any exception is handling the dataset
     */
    public Variable findVariableWithCfStandardName(Dataset dataset, String standard_name) throws WcsException {

        if (!dataset.usesCfConventions())
            _log.warn("Dataset does not appear conform to the CF convention. YMMV... Dataset: {}", this.getDapDatasetUrl());

        // proceed to look for it anyway, returning null if not found
        for (Variable v : dataset.getVariables()) {
            if (Objects.equals(standard_name, v.getAttributeValue("standard_name"))) {
                _log.debug("Found variable with standard name ", standard_name, v.getName());
                return v;
            }
        }
        return null;
    }

    /**
     * Returns the size of the requested coordinate variable. Since DomainCoordinate may only have a single
     * dimension it get's all exceptiony if they have more.
     *
     * @param dataset
     * @param standard_name The CF standard_name of the coordinate variable;
     * @return
     * @throws WcsException
     */

    public Dimension getDomainCoordinateVariableDimension(Dataset dataset, String standard_name) throws WcsException {

        Variable coordinateVariable = findVariableWithCfStandardName(dataset, standard_name);
        if (coordinateVariable == null)
            return null;

        List<Dim> dims = coordinateVariable.getDims();
        if (dims.size() > 1)
            throw new WcsException("Coordinate variable must have a single dimension. dims: {}", dims.size());

        Dim dim = dims.get(0);
        return dataset.getDimension(dim.getName());
    }


    /**
     * This class is a C style structure to hold the longthy
     * parameter list required for building the TimePeriodWithEnvelope
     */
    class BoundedByAndDomainSetParams {
        SimpleSrs srs;
        String coverageID;
        String beginDate;
        String endDate;
        Vector<Double> lowerCorner;
        Vector<Double> upperCorner;
        double origin_lat, origin_lon;
        long latitudeSize;
        double latitudeResolution;
        long longitudeSize;
        double longitudeResolution;

        // This little constructor ensures the collections are never null;
        BoundedByAndDomainSetParams() {
            lowerCorner = new Vector<>();
            upperCorner = new Vector<>();
        }
    }

    /**
     * This is the first step in a two step process:. Here we collect and QC the information
     * needed to build the DomainSet and Envelope. If time information can be found then an
     * EnvelopeWithTimePeriod is built otherwise a simple Envelope is built. The DomainSet
     * is added in either case.
     *
     * @param cd The CoverageDescription to which to add the DomainSet and Envelope content.
     * @param dataset The DAP dataset to query for the information needed.
     */
    private void addBoundedByAndDomainSet(CoverageDescriptionType cd, Dataset dataset, SimpleSrs srs) throws WcsException {

        BoundedByAndDomainSetParams bbadsp = new BoundedByAndDomainSetParams();

        bbadsp.srs = srs;
        bbadsp.coverageID = cd.getCoverageId();

        for (String axisLabel : srs.getAxisLabelsList()) {
            DomainCoordinate dc = getDomainCoordinate(axisLabel);
            if (dc == null)
                throw new WcsException("Failed to locate DomainCoordinate for SRS axis '" +
                        axisLabel + "'", WcsException.NO_APPLICABLE_CODE);
            double min = dc.getMin();
            double max = dc.getMax();
            if (dc.getName().equalsIgnoreCase("latitude")) {
                min = dataset.getValueOfGlobalAttributeWithNameLikeAsDouble("SouthernmostLatitude", min);
                max = dataset.getValueOfGlobalAttributeWithNameLikeAsDouble("NorthernmostLatitude", max);
                bbadsp.origin_lat = min;
                bbadsp.latitudeSize = dc.getSize();
                bbadsp.latitudeResolution = max - min / dc.getSize();

            } else if (dc.getName().equalsIgnoreCase("longitude")) {
                min = dataset.getValueOfGlobalAttributeWithNameLikeAsDouble("EasternmostLongitude", min);
                max = dataset.getValueOfGlobalAttributeWithNameLikeAsDouble("WesternmostLongitude", max);
                bbadsp.origin_lon = min;
                bbadsp.longitudeSize = dc.getSize();
                bbadsp.longitudeResolution = max - min / dc.getSize();
            }
            bbadsp.lowerCorner.add(min);
            bbadsp.upperCorner.add(max);
        }

        // Since time is special in WCS land we have to handle it specially.
        // First we attempt to assign the default time values from the time coordinate
        String date, time;
        DomainCoordinate timeCoordinate = getDomainCoordinate("time");
        if (timeCoordinate != null) {
            String timeUnits = timeCoordinate.getUnits();
            double timeVal = timeCoordinate.getMin();
            Date beginDate = TimeConversion.getTime(timeVal, timeUnits);
            bbadsp.beginDate = TimeConversion.formatDateInGmlTimeFormat(beginDate);

            timeVal = timeCoordinate.getMax();
            Date endDate = TimeConversion.getTime(timeVal, timeUnits);
            bbadsp.endDate = TimeConversion.formatDateInGmlTimeFormat(endDate);
        } else {
            _log.warn("addBoundedByAndDomainSet() - No coordinate for 'time' could be located. A default time period will not be utilized.");
        }

        // Look for obvious start time information in the Dataset metadata.
        date = dataset.getValueOfGlobalAttributeWithNameLike("RangeBeginningDate");
        time = dataset.getValueOfGlobalAttributeWithNameLike("RangeBeginningTime");
        if (date != null && time != null) {
            bbadsp.beginDate = date + "T" + time + "Z";
        } else {
            // TODO uh... not sure how to punt here as this is typically a per coverage/dataset value. Should this come from config? That would flatten the time to a single instance....
        }

        // Look for obvious end time information in the Dataset metadata.
        date = dataset.getValueOfGlobalAttributeWithNameLike("RangeEndingDate");
        time = dataset.getValueOfGlobalAttributeWithNameLike("RangeEndingTime");
        if (date != null && time != null) {
            bbadsp.endDate = date + "T" + time + "Z";
        } else {
            // TODO uh... not sure how to punt here as this is typically a per coverage/dataset value. Should this come from config? That would flatten the time to a single instance....
        }

        addBoundedByAndDomainSet(cd,bbadsp);

    }

    /**
     * Adds BoundedBy and DomainSet objects to the CoverageDescription.
     * The BoundedBy is built conditionally based on the presence of time bounds. If the time
     * bounds are missing or incomplete  a gml:Envelope will be included, otherwise a
     * gml:EnvelopeWithTimePeriod will be included in the BoundedBy
     * @param cd An instance of CoverageDescriptionType to which to add stuff
     * @param params  An initialized instance BoundedByAndDomainSetParams, no bad values man.
     * @throws WcsException When the bad things happen.
     */
    private void addBoundedByAndDomainSet(CoverageDescriptionType cd, BoundedByAndDomainSetParams params) throws WcsException {
        // Construct the BoundedBy element - a wrapper for the more informative Envelope
        // and EnvelopeWithTimePeriod content
        ObjectFactory gmlFactory = new ObjectFactory();
        BoundingShapeType boundedBy;
        if (params.beginDate == null || params.endDate == null) {
            _log.warn("Failed to determine time period information. Need a fall back..");
            boundedBy = getBoundedByWithEnvelope(params);
        }
        else {
            boundedBy = getBoundedByWithEnvelopeWithTimePeriod(params);
        }
        // Add BoundedBy to the coverage
        cd.setBoundedBy(boundedBy);

        // Get the Domain Set
        DomainSetType domainSet = getDomainSet(params);

        // Add the DomainSet to the coverages.
        cd.setDomainSet(gmlFactory.createDomainSet(domainSet));
    }

    /**
     *
     * @param params An instance of BoundedByAndDomainSetParams containing
     *               the information needed to build the gml:Envelope.
     * @return  An instance of BoundingShapeType (aka BoundedBy) containing a gml:Envelope
     */
    private BoundingShapeType getBoundedByWithEnvelope(BoundedByAndDomainSetParams params){

        EnvelopeType envelope = new EnvelopeType();

        SimpleSrs srs = params.srs;

        envelope.setAxisLabels(srs.getAxisLabelsList());
        envelope.setSrsName(srs.getName());

        envelope.setSrsName(srs.getName());
        envelope.setAxisLabels(srs.getAxisLabelsList());
        envelope.setUomLabels(srs.getUomLabelsList());
        envelope.setSrsDimension(BigInteger.valueOf(srs.getSrsDimension()));

        DirectPositionType envelopeLowerCorner = new DirectPositionType();
        envelopeLowerCorner.setValue(params.lowerCorner);
        envelope.setLowerCorner(envelopeLowerCorner);

        DirectPositionType envelopeUpperCorner = new DirectPositionType();
        envelopeUpperCorner.setValue(params.upperCorner);
        envelope.setUpperCorner(envelopeUpperCorner);

        BoundingShapeType bs = new BoundingShapeType();
        ObjectFactory gmlFactory = new ObjectFactory();
        bs.setEnvelope(gmlFactory.createEnvelope(envelope));

        return bs;
    }

    /**
     * Build a gml:EnvelopeWithTimePeriod using the passed parameter structure.
     *
     * @param params An instance of BoundedByAndDomainSetParams containing the information
     *               needed to build the gml:EnvelopeWithTimePeriod.
     * @return  An instance of BoundingShapeType (aka BoundedBy) containing a gml:EnvelopeWithTimePeriod
     */
    private BoundingShapeType getBoundedByWithEnvelopeWithTimePeriod(BoundedByAndDomainSetParams params) throws WcsException {

        // compute the envelope from dataset
        EnvelopeWithTimePeriod etp = new EnvelopeWithTimePeriod();
        etp.addLowerCornerCoordinateValues(params.lowerCorner);
        etp.addUpperCornerCoordinateValues(params.upperCorner);
        etp.setBeginTimePosition(params.beginDate);
        etp.setEndTimePosition(params.endDate);

        _log.debug(etp.toString());

        net.opengis.gml.v_3_2_1.EnvelopeWithTimePeriodType envelope = etp.getEnvelope(params.srs);
        net.opengis.gml.v_3_2_1.BoundingShapeType bs = new net.opengis.gml.v_3_2_1.BoundingShapeType();
        net.opengis.gml.v_3_2_1.ObjectFactory gmlFactory = new net.opengis.gml.v_3_2_1.ObjectFactory();
        bs.setEnvelope(gmlFactory.createEnvelopeWithTimePeriod(envelope));

        ////////////////////////////////////////////////////////////////////////////////////////

        return bs;
    }

    /**
     *
     * @param params An instance of BoundedByAndDomainSetParams containing the information needed to build the
     *              components of the DomainSet.
     * @return  An instance of DomainSetType (aka DomainSet) based on the passed params.
     */
    public DomainSetType getDomainSet(BoundedByAndDomainSetParams params){
        // Grid Envelope
        ObjectFactory gmlFactory = new ObjectFactory();
        net.opengis.gml.v_3_2_1.GridEnvelopeType gridEnvelope = gmlFactory.createGridEnvelopeType();

        SimpleSrs srs = params.srs;

        ////////////////////////////////////////////////////////////
        // Crucial member variable state setting...
        // Note: The index values for the arrays are 0 based and so the upper index is
        // one less than the size.
        List<BigInteger> upper = Arrays.asList(
                BigInteger.valueOf(params.latitudeSize - 1),
                BigInteger.valueOf(params.longitudeSize - 1));
        List<BigInteger> lower = Arrays.asList(BigInteger.ZERO, BigInteger.ZERO);
        gridEnvelope.withHigh(upper).withLow(lower);
        ////////////////////////////////////////////////////////////

        // Create the limits, set the envelope on them.
        GridLimitsType gridLimits = gmlFactory.createGridLimitsType();
        gridLimits.withGridEnvelope(gridEnvelope);

        // Make the RectifiedGridType instance
        DomainSetType domainSet = new net.opengis.gml.v_3_2_1.DomainSetType();
        RectifiedGridType rectifiedGrid = new net.opengis.gml.v_3_2_1.RectifiedGridType();
        rectifiedGrid.setId("RectifiedGrid-" + params.coverageID);
        rectifiedGrid.setDimension(BigInteger.valueOf(srs.getSrsDimension()));
        rectifiedGrid.setAxisLabels(srs.getAxisLabelsList());
        rectifiedGrid.setLimits(gridLimits);

        // Create the Origin.
        DirectPositionType position = gmlFactory.createDirectPositionType();
        position.withValue(params.origin_lat, params.origin_lon);
        PointType point = gmlFactory.createPointType();
        point.withPos(position);
        point.setId("GridOrigin-" + params.coverageID);
        point.setSrsName(srs.getName());
        PointPropertyType origin = gmlFactory.createPointPropertyType();
        origin.withPoint(point);
        rectifiedGrid.setOrigin(origin);
        // Create the offset vectors .
        List<VectorType> offsetList = new ArrayList<VectorType>();
        VectorType offset1 = gmlFactory.createVectorType();

        offset1.withValue(params.latitudeResolution, 0.0);
        offset1.setSrsName(srs.getName());
        offsetList.add(offset1);
        VectorType offset2 = gmlFactory.createVectorType();

        offset2.withValue(0.0, params.longitudeResolution);
        offset2.setSrsName(srs.getName());
        offsetList.add(offset2);
        rectifiedGrid.setOffsetVector(offsetList);

        domainSet.setAbstractGeometry(gmlFactory.createRectifiedGrid(rectifiedGrid));

        return domainSet;
    }



    /**
<<<<<<< Updated upstream
     * This method examines the variables in the Dataset to determine which variables are members of the range (i.e.
     * fields. It determines which variables can be used as fields in the coverage and then adds them to the
     * CoverageDescription
=======
     * Examine all of the variables in the dataset and determine which ones make up the Range
     * variables (i.e., the fields). Add the list of range variables to the CoverageDescription
     * passed in at the first argument.
>>>>>>> Stashed changes
     *
     * @param cd
     * @param dataset
     */
    private void addRange(CoverageDescriptionType cd, Dataset dataset, SimpleSrs srs) throws WcsException {
        DataRecordPropertyType rangeType = new DataRecordPropertyType();
        DataRecordType dataRecord = new DataRecordType();
        List<DataRecordType.Field> fieldList = new ArrayList<>();

        for (Variable var : dataset.getVariables()) {

            boolean varFitsCoverageSrs = variableDimensionsAreCompatibleWithSrs(dataset, var, srs);
            _log.debug("The variable dimensions{}match the SRS.", varFitsCoverageSrs ? " " : " DO NOT ");

            boolean dimsMatchDataset = compareVariableDimensionsWithDataSet(var,dataset);

            if (varFitsCoverageSrs && dimsMatchDataset ) {
                DynamicService.FieldDef defaultFieldDef = _dynamicService.getFieldDefFromDapId(var.getName());

                if (defaultFieldDef == null)
                    _log.warn("addRange() - No defaults WCS mapping was located for DAP variable '" + var.getName() + "'");

                DataRecordType.Field sweField = buildSweFieldFromDapVar(var, defaultFieldDef);
                if (sweField != null)
                    fieldList.add(sweField);
                else
                    _log.warn("Failed to convert DAP variable '{}' to an swe:Field object. SKIPPING", var.getName());

            }
        }
        if (fieldList.isEmpty())
            throw new WcsException("Failed to generate swe:Field elements from DAP variables. There Is No Coverage Here.", WcsException.NO_APPLICABLE_CODE);

        dataRecord.setField(fieldList);
        rangeType.setDataRecord(dataRecord);
        cd.setRangeType(rangeType);

    }


    /**
     * @param var
     * @param dataset
     * @return
     * @author uday
     */
    private boolean compareVariableDimensionsWithDataSet(Variable var, Dataset dataset) {
        boolean flag = true;

        List<Dim> vdims = var.getDims();
        List<Dimension> dimensions = dataset.getDimensions();

        if (vdims == null || dimensions == null || vdims.isEmpty() || dimensions.isEmpty()) {
            return false;
        } else if (vdims.size() == dimensions.size()) {
            _log.debug("Examining dimension of Variable " + var.getName() + " which has same number of dimensions as Dataset, " + vdims.size());
            for (Dim dim : var.getDims()) {
                boolean found = false;
                String dimName = dim.getName();
                if (dimName.charAt(0) == '/') dimName = dimName.substring(1);
                _log.debug("Look at " + var.getName() + " dimension " + dimName + ", assume it is not in dataset to begin with");
                for (Dimension dimension : dataset.getDimensions()) {
                    _log.debug("comparing variable dimension " + dimName + " with Dataset dimension name " + dimension.getName());
                    // probably need a better test
                    if (dimName.equalsIgnoreCase(dimension.getName())) found = true;
                }
                if (found) {
                    _log.debug("Dimension " + dimName + " found in Dataset");
                    continue;
                } else {
                    _log.debug("Dimension " + dimName + " NOT found in DataSet");
                    flag = false;
                    break;
                }
            }
        } else {
            flag = false;
            _log.debug("OOPS! Variable '" + var.getName() + "' has " + vdims.size() + " dimensions, while the parent Dataset has " + dimensions.size());
        }

        if (flag) {
            _log.debug("All dimensions in DAP variable '" + var.getName() + "' match the Dataset dimensions, so it will be included in WCS coverage ");
        } else {
            _log.debug("All dimensions in DAP variable '" + var.getName() + "' did NOT match Dataset dimensions, so it will be NOT included in WCS coverage ");
        }

        return flag;
    }

    /**
     * Compares the dataset Dimensions of the DAP Variable _var_ with
     * the associated SRS DomainCoordinate's DAP variable. In particular
<<<<<<< Updated upstream
     * comparsiom is made by working in reverse order through the dimensions of the SRS
     * and the varable. If all of the dimensions have matched when the SRS runs out of DomainCoordinates
=======
     * comparison made by working in reverse order through the dimensions of the SRS
     * and the variable. If all of the dimensions have matched when the SRS runs of DomainCoordinates
>>>>>>> Stashed changes
     * then it's a positive result.
     *
     * @note This particular trick, using reverse iteration on the SRS dimensions, is
     * used because the SRS dimensions only need to match the _last_ dimensions of the
     * variable.
     *
     * @param dataset The Dataset that's being turned into a coverage.
     * @param dapVar The variable to be evaluated.
     * @param srs The SRS which to compare the variable against.
     * @return True when the DAP variable is dimensionally SRS conformant.
     */
    private boolean variableDimensionsAreCompatibleWithSrs(Dataset dataset, Variable dapVar, SimpleSrs srs) {

        if (dataset == null || dapVar == null || srs == null)
            return false;

        boolean result = true;
        Vector<String> weveGotIssuesMan = new Vector<>();
        List<Dim> dapVarDims = dapVar.getDims();
        ListIterator<Dim> dapVarDimIter = dapVarDims.listIterator(dapVarDims.size());

        List<String> srsAxisLabels = srs.getAxisLabelsList();
        ListIterator<String> axisLabelIter = srsAxisLabels.listIterator(srsAxisLabels.size());

        if (dapVarDims.size() < srsAxisLabels.size())
            weveGotIssuesMan.add("OUCH! SRS has more dimensions (" + srs.getSrsDimension() + " " +
                    "than the variable " + dapVar.getName() + " which has " + dapVarDims.size() + " dimensions.");

        // Check to see that the DAP variable dapVar the same crucial dimensions as the Coverage SRS
        // Since we know that this all hinges on  the inner most (last) dimensions matching because
        // in DAP land that's how we can get multidimensional arrays into WCS, by focusing on the
        // last dims. Anyway, thus, we iterate backwards across the SRS DomainCoordinates and the
        // the DAP variable's dimensions. The DomainCoordinate's dapVar is collected and it's Dim
        // reference (there should only be one because in the WCS and DAP models 'dimensions' are
        // one dimensional items). We verify that the domainCoordDapVarDim  references the
        // same Dataset Dimension  as the variable dapVar does in the dimension being assessed.
        //
        // Additional tests are performed that need reviewed and possibly dropped:
        //
        // -  Compare the name of the  domainCoordDapVar to the name of the dapVarDatasetDimension
        //    (Bad assumption? I don't believe they are always the same.)
        // - Compare the size of the domainCoordinate and dapVarDatasetDimension
        //   (Redundant? See comment below)
        //
        //
        while (axisLabelIter.hasPrevious() && dapVarDimIter.hasPrevious()) {

            // The next dimension reference from the DAP variable
            Dim dapVarDim = dapVarDimIter.previous();

            // Get the next coordinate/axis name from the SRS
            String axisLabel = axisLabelIter.previous();

            // Find the DomainCoordinate associated with this axis
            DomainCoordinate domainCoordinate = getDomainCoordinate(axisLabel);
            
            // Find the DAP Variable referenced by the DomainCoordinate
            Variable domainCoordinateVariable = dataset.getVariable(domainCoordinate.getDapID());

            //////////////////////////////////////////////////////////////////////////
            // TEST
            // Compare the variable's Dim for this dimension with
            // the DomainCoordinate's DAP Variable shared Dimension ref.
            List<Dim> dims = domainCoordinateVariable.getDims();
            Dim domainCoordVarDim = dims.get(0);   // TODO Someday there may be multidimensional DomainCoordinates.
            if (domainCoordVarDim.getName().equals(dapVarDim.getName())) {
                _log.debug("woot. The domainCoordinate DAP variable references the same dimension name as the variable.");
            }
            else {
                weveGotIssuesMan.add("OUCH - The domainCoordinate DAP variable DOES NOT " +
                        "have the same dimension name as the variable.");
            }
            //////////////////////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////////////////////
            // TEST
            // Check to see if the domainCoordinate is associate with the DAP variable with the same name as the
            // Dimension we grabbed from the Dataset.
            // FIXME Is this a bogus test? I think maybe so because the name of the "dimension" may not be the name of the Array that holds the data for the dimension.
            // Get the Dimension instance referenced by the Dim from the Dataset
            Dimension dapVarDatasetDimension = dataset.getDimension(dapVarDim.getName());

            if (domainCoordinateVariable.getName().equals(dapVarDatasetDimension.getName())) {
                _log.debug("woot. domainCoordinate.getDapID() matches dimension.getName()");
            } else {
                weveGotIssuesMan.add("OUCH! SRS domainCoordinate dapId '" + domainCoordinate.getDapID() + "' " +
                        "DOES NOT match the dimension name '" + dapVarDatasetDimension.getName() + "'");
            }
            //////////////////////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////////////////////
            // TEST
            // Compare the size of the domainCoordinate for this dimension with the
            // size of the Dataset Dimension referenced by the variables Dim for
            // this dimension.
            // FIXME This next test maybe redundant since we have already confirmed that the current dapVarDim and the domainCoordVarDim  both reference the Dataset Dimension
            if (domainCoordinate.getSize() == dapVarDatasetDimension.getSizeAsLong()) {
                _log.debug("woot.  domainCoordinate.size() matches dimension.size()");
            } else {
                weveGotIssuesMan.add("OUCH! SRS domainCoordinate size '" + domainCoordinate.getSize() + "' " +
                        "DOES NOT match the dimension size '" + dapVarDatasetDimension.getSize() + "'");
            }
            //////////////////////////////////////////////////////////////////////////
        }
        if (!weveGotIssuesMan.isEmpty()) {
            _log.error("variableDimensionsAreCompatibleWithSrs() - You've got issues man.\n");
            for (String msg : weveGotIssuesMan)
                _log.error("OUCH! {}", msg);
            result = false;
        }

        if (dapVarDimIter.hasPrevious())
            _log.debug("the dapVar '{}' has more dimensions than the SRS", dapVar.getName());


        return result;

        /*

        //List<DomainCoordinate> domainCoordinates = getDomainCoordinatesAsList();
        //Vector<Variable> dapVars = dataset.getVariables();
        //List<Dimension> datasetDimensions =  dataset.getDimensions();


        for(Variable dapVar: dapVars){
            StringBuilder sb = new StringBuilder("variableDimensionsAreCompatibleWithSrs() - ");
            sb.append("dapVar name: '").append(dapVar.getName()).append("'");
            int i=0;
            for(Dim dim : dapVar.getDims()){
                sb.append(" dim[").append(i++).append("]: '").append(dim.getName()).append("' ").append(dapVar.getDims().size()==var.getDims().size()?"DIMENSION MATCH":"");
            }
            _log.debug(sb.toString());
        }

        int i=0;
        for(Dim dim: dapVarDims) {
            Dimension datasetDimension  = datasetDimensions.get(i);
            DomainCoordinate domainCoordinate = domainCoordinates.get(i);
            StringBuilder sb = new StringBuilder("variableDimensionsAreCompatibleWithSrs() - ");
            sb.append(" dim: '").append(dim.getName()).append("' ");
            sb.append(" datasetDimension: ").append(datasetDimension.getName()).append("[").append(datasetDimension.getSize()).append("]");
            sb.append(" domainCoordinate: ").append(domainCoordinate.getName()).append("[").append(domainCoordinate.getSize()).append("]");
            _log.debug(sb.toString());
            i++;
        }

        */
    }

    /**
     * Generates a swe:Field from Dap4 variable.
     *
     * @param var The DAP4 Variable from which to produce a field.
     * @return The DataRecord.Field built from var, or returns null if the process failed.
     */
    private net.opengis.swecommon.v_2_0.DataRecordType.Field buildSweFieldFromDapVar(Variable var, DynamicService.FieldDef fieldDef) {

        net.opengis.swecommon.v_2_0.DataRecordType.Field field =
                new net.opengis.swecommon.v_2_0.DataRecordType.Field();

        String sweMeasureDefinition = "urn:ogc:def:dataType:OGC:1.1:measure";
        double min = NaN;
        double max = NaN;
        String s;
        Vector<String> errors = new Vector<>();

        // Makes sure that the field name is an NCNAME...
        String  fieldNcName = var.getName();
        if(!opendap.xml.Util.isNCNAME(var.getName())) {
            // it's not an NCNAME. Is there default supplied?
            fieldNcName = fieldDef.name;
            if(fieldNcName == null)  {
                // nope, we'll punt...
                fieldNcName = opendap.xml.Util.convertToNCNAME(var.getName());
            }
        }
        field.setName(fieldNcName);

        String description = var.getAttributeValue("long_name");
        if (description == null) {
            if (fieldDef != null)
                description = fieldDef.description;
            else
                errors.add("Failed to locate DAP Attribute 'long_name', no default value available.");
        }
        String units = var.getAttributeValue("units");
        if (units == null) {
            if (fieldDef != null)
                units = fieldDef.units;
            else
                errors.add("Failed to locate DAP Attribute 'units', no default value available.");
        }

        s = var.getAttributeValue("vmin");
        if (s != null) {
            try {
                min = Double.parseDouble(s);
            } catch (NumberFormatException nfe) {
                if (fieldDef != null)
                    min = fieldDef.min;
                else
                    errors.add("Failed to parse the value of DAP Attribute 'vmin' as a Double. msg: " + nfe.getMessage());
            }
        } else {
            if (fieldDef != null)
                min = fieldDef.min;
            else
                errors.add("Failed to locate DAP Attribute 'vmin', no default value available.");
        }


        s = var.getAttributeValue("vmax");
        if (s != null) {
            try {
                max = Double.parseDouble(s);
            } catch (NumberFormatException nfe) {
                if (fieldDef != null)
                    max = fieldDef.max;
                else
                    errors.add("Failed to parse the value of DAP Attribute 'vmax' as a Double. msg: " + nfe.getMessage());
            }
        } else {
            if (fieldDef != null)
                max = fieldDef.max;
            else
                errors.add("Failed to locate DAP Attribute 'vmax', no default value available.");

        }

        if (!errors.isEmpty()) {
            String s1 = "Failed to map DAP variable '" + var.getName() + "' to swe:Field. SKIPPING!\n";
            _log.error(s1);
            for (String msg : errors) {
                _log.error(msg);
            }
            return null;
        }

        net.opengis.swecommon.v_2_0.QuantityType quantity = new net.opengis.swecommon.v_2_0.QuantityType();
        quantity.setDefinition(sweMeasureDefinition);
        quantity.setDescription(description);

        net.opengis.swecommon.v_2_0.UnitReference uom = new net.opengis.swecommon.v_2_0.UnitReference();
        uom.setCode(units);
        quantity.setUom(uom);

        net.opengis.swecommon.v_2_0.AllowedValuesPropertyType allowedValues = new net.opengis.swecommon.v_2_0.AllowedValuesPropertyType();
        net.opengis.swecommon.v_2_0.AllowedValuesType allowed = new net.opengis.swecommon.v_2_0.AllowedValuesType();
        List<Double> allowedInterval = Arrays.asList(min, max);
        List<JAXBElement<List<Double>>> coordinates = new Vector<>();
        net.opengis.swecommon.v_2_0.ObjectFactory sweFactory = new net.opengis.swecommon.v_2_0.ObjectFactory();
        coordinates.add(sweFactory.createAllowedValuesTypeInterval(allowedInterval));
        allowed.setInterval(coordinates);
        allowedValues.setAllowedValues(allowed);
        quantity.setConstraint(allowedValues);

        field.setAbstractDataComponent(sweFactory.createAbstractDataComponent(quantity));

        /////////////////////////////////////////////////////////////
        // Crucial member variable state setting...
        this.addFieldToDapVarIdAssociation(field.getName(), var.getName());
        /////////////////////////////////////////////////////////////

        return field;
    }

    /**
     * Converts the JAXB generated CoverageDescriptionType to a JDOM representation of the CoverageDescription
     *
     * @param cd The CoverageDescriptionType instance to process
     * @return The JDOM representation of the CoverageDescription
     * @throws WcsException
     */
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
            String msg = "Failed to get marshall CoverageDescription! JAXBException Message: " + e.getMessage();
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
        return cdElement;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Out friend main() runs a sanity check using a DMR obtained from test.opendap.org
     *
     * @param args Ignored...
     */
    public static void main(String[] args) {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String testDmrUrl = "https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml";

        testDmrUrl = "http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml";
        try {
            Element dmrElement =
                    opendap.xml.Util.getDocumentRoot(testDmrUrl, opendap.http.Util.getNetRCCredentialsProvider());

            SimpleSrs defaultSrs = new SimpleSrs("urn:ogc:def:crs:EPSG::4326", "latitude longitude", "deg deg", 2);
            DynamicService ds = new DynamicService();
            ds.setSrs(defaultSrs);

            String s = "time";
            DomainCoordinate dc = new DomainCoordinate(s, s, "minutes since 1980-01-01 00:30:00", "", 1, s);
            dc.setMin(690);
            dc.setMax(9330);
            ds.setTimeCoordinate(dc);

            s = "latitude";
            dc = new DomainCoordinate(s, s, "deg", "", 361, s);
            dc.setMin(-90.0);
            dc.setMax(90.0);
            ds.setLatitudeCoordinate(dc);

            s = "longitude";
            dc = new DomainCoordinate(s, s, "deg", "", 576, s);
            dc.setMin(-180.0);
            dc.setMax(179.625);
            ds.setLongitudeCoordinate(dc);

            CoverageDescription cd = new DynamicCoverageDescription(dmrElement, ds);

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




}
