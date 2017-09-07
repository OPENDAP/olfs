package opendap.wcs.v2_0;

import opendap.namespaces.XML;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class DynamicCoverageDescription extends CoverageDescription {
    private Logger _log;
    private Element _myDMR;

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
    public DynamicCoverageDescription(Element dmr) throws IOException {
        this();
        _myDMR = dmr;
        // TODO: Get the dataset URL from the DMR top level attribute "xml:base"
        String datasetUrl = dmr.getAttributeValue("base", XML.NS);
        setDapDatasetUrl(new URL(datasetUrl));

        /** - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *  TODO:  Replace this stuff with the output of WcsMarchaller
         */

        WcsMarshaller wcs = new WcsMarshaller(dmr);
        if (wcs._myCD != null) {
            _myCD = wcs._myCD;
        } else {
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
    public DynamicCoverageDescription(String datasetUrl) throws IOException, JDOMException {
        super();
        setDapDatasetUrl(new URL(datasetUrl));

        String dmrUrl = datasetUrl + ".dmr.xml";
        Element dmr = opendap.xml.Util.getDocumentRoot(dmrUrl);

        WcsMarshaller wcs = new WcsMarshaller(dmr);
        _myCD = wcs._myCD;

        ////////////////////////////////////
        //  use DOM to directly set member variables

        //_myFile = cd._myFile.getCanonicalFile();
        //_validateContent = cd._validateContent;
        //_dapGridId.putAll(cd._dapGridId);
        //_domainCoordinates.putAll(dataset.getDimensions());
        //this.fields = dataset.getVars32bitFloats();
        //_initialized = cd._initialized;

    }

    // Sanity Check
    public static void main(String[] args) {

        String testDatasetUrl = "https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4";
        try {
            CoverageDescription cd = new DynamicCoverageDescription(testDatasetUrl);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

}
