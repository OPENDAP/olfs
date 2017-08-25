package opendap.wcs.v2_0;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class DynamicCoverageDescription extends CoverageDescription {
    private Logger _log;
    private Element _myDMR;

    public DynamicCoverageDescription()  {
        super();
        _log = LoggerFactory.getLogger(getClass());
        _myDMR = null;
    }

    public DynamicCoverageDescription(Element dmr, String datasetUrl) throws IOException {
        this();
        _myDMR = dmr;
        setDapDatasetUrl(new URL(datasetUrl));
        String name = _myDMR.getAttributeValue("name");

        _myCD =  new Element("CoverageDescription",WCS.WCS_NS);
        Element coverageId =  new Element("CoverageId",WCS.WCS_NS);
        coverageId.setText(name);
        _myCD.addContent(coverageId);


    }




}
