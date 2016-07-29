package opendap.wcs.v2_0;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ndp on 7/29/16.
 */
public class EOCoverageDescription extends CoverageDescription {

    private Logger log;

    public EOCoverageDescription(Element eowcsCoverageConfig, String catalogDir, boolean validateContent) throws IOException, JDOMException, ConfigurationException, WcsException {
        super(eowcsCoverageConfig,catalogDir,validateContent);
        log = LoggerFactory.getLogger(this.getClass());

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
    public BoundingBox getBoundingBox() throws WcsException {
        BoundingBox bb = super.getBoundingBox();

        if(!bb.hasTimePeriod()){
            // No time period? Check the  EO metadata section for that...

            Element metadata =  myCD.getChild("metadata",WCS.GMLCOV_NS);
            if(metadata == null)
                return bb; // Give up on the time thing...

            Element extension =  metadata.getChild("Extension",WCS.GMLCOV_NS);
            if(extension == null)
                return bb; // Give up on the time thing...

            Element eoMetadata =  extension.getChild("EOMetadata",WCS.WCSEO_NS);
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



}
