package opendap.wcs.v2_0;

import org.jdom.Element;

/**
 * Created by ndp on 8/5/16.
 */
public class EOCoverage extends Coverage {

    /**
     *
     * @param eoCoverageDescription
     * @param requestUrl
     * @throws WcsException
     * @throws InterruptedException
     */
    public EOCoverage(EOCoverageDescription eoCoverageDescription, String requestUrl) throws WcsException, InterruptedException {
        super(eoCoverageDescription, requestUrl);
    }


    /**
     * Add the EO linage information to the passed CoverageDescription element
     * using the KVP representation of the request
     *
     * @param requestURL KVP WCS request
     * @return
     */
    private void addLineage(Element eoMetadata, String requestURL) throws WcsException {

        if (eoMetadata == null)
            return; // Give up on the linage thing...

        Element lineageElement = new Element("lineage", WCS.WCSEO_NS);
        Element refGetCoverageElement = new Element("referenceGetCoverage", WCS.WCSEO_NS);
        Element owsReferenceElement = new Element("Reference", WCS.OWS_NS);

        owsReferenceElement.setAttribute("href", requestURL, WCS.XLINK_NS);

        refGetCoverageElement.addContent(owsReferenceElement);

        lineageElement.addContent(refGetCoverageElement);
        eoMetadata.addContent(lineageElement);
    }

    public EOCoverageDescription getEOCoverageDescription() throws WcsException{
        CoverageDescription cd = getCoverageDescription();
        if(!(cd instanceof EOCoverageDescription))
            throw new WcsException("Bad things happened. Found a instance of CoverageDescription " +
                    "Expected an instance of EOCoverageDescription.",WcsException.NO_APPLICABLE_CODE);

        return (EOCoverageDescription) cd;

    }


    /**
     * This overridden version adds the EOMetadata and linage information if the EOMetadata section is successfully
     * located.
     * @param rangeValuesPartID  The ID of the multipart document part that carries the range values.
     * @param mimeType  The mimeType of the range values encoding
     * @return
     * @throws WcsException
     */
    @Override
    public Element getCoverageElement(String rangeValuesPartID, String mimeType) throws WcsException {

        Element ce = super.getCoverageElement(rangeValuesPartID, mimeType);

        EOCoverageDescription eocd = getEOCoverageDescription();

        Element eoMetadata = eocd.getEOMetadata();
        String requestUrl = getRequestUrl();
        if (requestUrl != null) {
            addLineage(eoMetadata, requestUrl);
        }
        addEOMetadata(ce,eoMetadata);

        return ce;

    }


    private void addEOMetadata(Element coverage, Element eoMetadata) throws WcsException {


        Element metadata =  coverage.getChild("metadata",WCS.GMLCOV_NS);
        if(metadata == null) {
            metadata = new Element("metadata",WCS.GMLCOV_NS);
            coverage.addContent(metadata);
        }


        Element extension =  metadata.getChild("Extension",WCS.GMLCOV_NS);
        if(extension == null){
            extension = new Element("Extension",WCS.GMLCOV_NS);
            coverage.addContent(extension);
        }

        Element eomd =  extension.getChild("EOMetadata",WCS.WCSEO_NS);
        if(eomd != null) {
            throw new WcsException("Bad server state. Found EOMetadata object when one was not expected.",WcsException.NO_APPLICABLE_CODE);

        }
        extension.addContent(eoMetadata);





    }



}
