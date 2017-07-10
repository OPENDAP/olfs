package opendap.wcs.v2_0;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This child of CoverageDescription  adds functionality specific to the EO profile.
 *
 */
public class EOCoverageDescription extends CoverageDescription {

    private Logger _log;

    public static final String CONFIG_ELEMENT_NAME = "EOWcsCoverage";

   public EOCoverageDescription() {
    	
    }

    public EOCoverageDescription(EOCoverageDescription eocd) throws IOException {
        super(eocd);
        _log =  LoggerFactory.getLogger(this.getClass());

    }

    public EOCoverageDescription(Element eowcsCoverageConfig, String catalogDir, boolean validateContent) throws IOException, JDOMException, ConfigurationException, WcsException {
        super(eowcsCoverageConfig,catalogDir,validateContent);
        _log = LoggerFactory.getLogger(this.getClass());

    }

    /**
     *       <gmlcov:metadata>
     <gmlcov:Extension>
     <wcseo:EOMetadata>
     <eop:EarthObservation gml:id="eop_someEOCoverage1">
     <om:phenomenonTime>
     <gml:TimePeriod gml:id="tp_someEOCoverage1">
     <gml:beginPosition>2008-03-13T10:00:06.000</gml:beginPosition>
     <gml:endPosition>2008-03-13T10:20:26.000</gml:endPosition>
     </gml:TimePeriod>
     </om:phenomenonTime>

     *
     * @return Returns the BoundingBox defined by the gml:boundedBy element, if such an element is present.
     * Returns null if no gml:boundedBy element is found.
     * @throws WcsException When bad things happen.
     */
    @Override
    public NewBoundingBox getBoundingBox() throws WcsException {
        NewBoundingBox bb = super.getBoundingBox();

        if(!bb.hasTimePeriod()){
            // No time period? Check the  EO metadata section for that...

            Element eoMetadata =  getEOMetadata(_myCD);
            if(eoMetadata == null)
                return bb; // Give up on the time thing...

            Element earthObservation =  eoMetadata.getChild("EarthObservation",WCS.EOP_NS);
            if(earthObservation == null)
                return bb; // Give up on the time thing...


            Element phenomTime =  earthObservation.getChild("phenomenonTime",WCS.OM_NS);
            if(phenomTime == null)
                return bb; // Give up on the time thing...


            Element timePeriodEnvelope =  phenomTime.getChild("TimePeriod",WCS.GML_NS);
            if(timePeriodEnvelope == null)
                return bb; // Give up on the time thing...

            bb.ingestTimePeriod(timePeriodEnvelope);

        }


        return bb;
    }

    public void adjustEOMetadataCoverageFootprint(GetCoverageRequest req) throws WcsException {

        NewBoundingBox cvrgBB = getBoundingBox();
        NewBoundingBox regBB = WCS.getSubsetBoundingBox(req.getDimensionSubsets(),req.getTemporalSubset(),cvrgBB);

        String newPositionListValue = regBB.getEOFootprintPositionListValue();
        Element eoFootprintPositionList = getEOFootprintPositionList(_myCD);

        if(eoFootprintPositionList==null)
            throw new WcsException("adjustEOMetadataCoverageFootprint() - Unable to locate EO Footprint content.",WcsException.NO_APPLICABLE_CODE);

        eoFootprintPositionList.setText(newPositionListValue);





    }



    /*
        <wcs:CoverageDescription>
            <gmlcov:metadata>
                <gmlcov:Extension>
     */

    public  Element getEOMetadata(){

        Element eoMetadata =  getEOMetadata(_myCD);

        return (Element)eoMetadata.clone();
    }

    protected  Element getEOMetadata(Element coverageDescriptionElement){

        Element metadata =  coverageDescriptionElement.getChild("metadata",WCS.GMLCOV_NS);
        if(metadata == null)
            return null; // Give up on the EO Metadata thing...

        Element extension =  metadata.getChild("Extension",WCS.GMLCOV_NS);
        if(extension == null)
            return null; // Give up on the EO Metadata thing...

        Element eoMetadata =  extension.getChild("EOMetadata",WCS.WCSEO_NS);
        if(eoMetadata == null)
            return null; // Give up on the EO Metadata thing...


        return eoMetadata;
    }
    /*

         <wcseo:EOMetadata>
            <eop:EarthObservation>
                <om:featureOfInterest> <!-- 0..1 -->
                    <eop:Footprint gml:id="footprint_MODIS_AQUA_L3_CHLA_DAILY_4KM_R_000_nc4_min_eo">
                        <eop:multiExtentOf>
                            <gml:MultiSurface gml:id="multisurface_MODIS_AQUA_L3_CHLA_DAILY_4KM_R_000_nc4_min_eo" srsName="EPSG:4326">
                                <gml:surfaceMember>
                                    <gml:Polygon gml:id="polygon_MODIS_AQUA_L3_CHLA_DAILY_4KM_R_000_nc4_min_eo">
                                        <gml:exterior>
                                            <gml:LinearRing>
                                                <gml:posList>-90 -180 90 -180 90 180 -90 180 -90 -180</gml:posList>

    */
    public Element getEOFootprintPositionList(Element coverageDescriptionElement) {

        Element eoMetadata = getEOMetadata(coverageDescriptionElement);

        Element earthObservation =  eoMetadata.getChild("EarthObservation",WCS.EOP_NS);
        if(earthObservation == null)
            return null; // Give up on the footprint thing...


        Element featureOfInterest =  earthObservation.getChild("featureOfInterest",WCS.OM_NS);
        if(featureOfInterest == null)
            return null; // Give up on the footprint thing...


        Element footprint =  featureOfInterest.getChild("Footprint",WCS.EOP_NS);
        if(footprint == null)
            return null; // Give up on the footprint thing...

        Element multiExtentOf =  footprint.getChild("multiExtentOf",WCS.EOP_NS);
        if(multiExtentOf == null)
            return null; // Give up on the footprint thing...


        Element multiSurface =  multiExtentOf.getChild("MultiSurface",WCS.GML_NS);
        if(multiSurface == null)
            return null; // Give up on the footprint thing...

        Element surfaceMember =  multiSurface.getChild("surfaceMember",WCS.GML_NS);
        if(surfaceMember == null)
            return null; // Give up on the footprint thing...

        Element polygon =  surfaceMember.getChild("Polygon",WCS.GML_NS);
        if(polygon == null)
            return null; // Give up on the footprint thing...

        Element exterior =  polygon.getChild("exterior",WCS.GML_NS);
        if(exterior == null)
            return null; // Give up on the footprint thing...


        Element linearRing =  exterior.getChild("LinearRing",WCS.GML_NS);
        if(linearRing == null)
            return null; // Give up on the footprint thing...


        Element positionList =  linearRing.getChild("posList",WCS.GML_NS);
        if(positionList == null)
            return null; // Give up on the footprint thing...

        return positionList;
    }



    @Override
    public Coverage getCoverage(String requestUrl) throws WcsException, InterruptedException {

        EOCoverage coverage = new EOCoverage(this,requestUrl);

        return coverage;


    }


}
