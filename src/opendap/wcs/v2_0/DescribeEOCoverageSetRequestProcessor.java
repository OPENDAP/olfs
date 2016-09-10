package opendap.wcs.v2_0;

import org.jdom.Document;
import org.jdom.Element;

/**
 * Created by ndp on 9/8/16.
 */
public class DescribeEOCoverageSetRequestProcessor {

    private static void addEoWcsNameSpaces(Element e){

        e.addNamespaceDeclaration(WCS.GML_NS);
        e.addNamespaceDeclaration(WCS.SWE_NS);
        e.addNamespaceDeclaration(WCS.GMLCOV_NS);
        e.addNamespaceDeclaration(WCS.XSI_NS);

        StringBuilder schemaLocationValue = new StringBuilder();

        schemaLocationValue.append(WCS.WCS_NAMESPACE_STRING).append(" ").append(WCS.WCS_SCHEMA_LOCATION_BASE+"wcsAll.xsd ");
        schemaLocationValue.append(WCS.GML_NAMESPACE_STRING).append(" ").append(WCS.GML_SCHEMA_LOCATION_BASE+"gml.xsd ");
        schemaLocationValue.append(WCS.SWE_NAMESPACE_STRING).append(" ").append(WCS.SWE_SCHEMA_LOCATION_BASE+"swe.xsd ");
        schemaLocationValue.append(WCS.GMLCOV_NAMESPACE_STRING).append(" ").append(WCS.GMLCOV_SCHEMA_LOCATION_BASE+"gmlcovAll.xsd ");

        e.setAttribute("schemaLocation",schemaLocationValue.toString(),WCS.XSI_NS);

    }


    /**
     * <wcseo:EOCoverageSetDescription numberMatched="2" numberReturned="2">
     * @param req
     * @return
     * @throws InterruptedException
     * @throws WcsException
     */
    public static Document processDescribeEOCoverageSetRequest(DescribeEOCoverageSetRequest req)
            throws InterruptedException, WcsException {

        Element eoCoverageSetDescription = new Element("EOCoverageSetDescription",WCS.WCSEO_NS);

        addEoWcsNameSpaces(eoCoverageSetDescription);

        int numberReturned = 0;
        int numberMatched = 0;

        if(req.hasSection(DescribeEOCoverageSetRequest.Sections.CoverageDescriptions) |
                req.hasSection(DescribeEOCoverageSetRequest.Sections.All)) {

            Element eoCoverageDescriptions = null;

            for(String id: req.getEoIds()){
                EOCoverageDescription eoCoverageDescription = CatalogWrapper.getEOCoverageDescription(id);

                if(eoCoverageDescription!=null) {

                    //@TODO Evaluate subset here!!

                    numberMatched++;

                    if(eoCoverageDescriptions==null) {
                        eoCoverageDescriptions = new Element("CoverageDescriptions", WCS.WCS_NS);
                        eoCoverageSetDescription.addContent(eoCoverageDescriptions);
                    }


                    if(numberReturned < req.getMaxItemCount()) {
                        eoCoverageDescriptions.addContent(eoCoverageDescription.getCoverageDescriptionElement());
                        numberReturned++;
                    }
                }
            }
        }


        if(req.hasSection(DescribeEOCoverageSetRequest.Sections.DatasetSeriesDescriptions) |
                req.hasSection(DescribeEOCoverageSetRequest.Sections.All)) {

            Element eoDatasetSeriesDescriptions = null;

            for(String id: req.getEoIds()){
                EODatasetSeries eoDatasetSeries = CatalogWrapper.getEODatasetSeries(id);
                if(eoDatasetSeries!=null ) {

                    //@TODO Evaluate subset here!!

                    numberMatched++;
                    if(eoDatasetSeriesDescriptions==null) {
                        eoDatasetSeriesDescriptions = new Element("DatasetSeriesDescriptions", WCS.WCSEO_NS);
                        eoCoverageSetDescription.addContent(eoDatasetSeriesDescriptions);
                    }

                    if( numberReturned < req.getMaxItemCount()) {
                        eoDatasetSeriesDescriptions.addContent(eoDatasetSeries.getDatasetSeriesDescriptionElement());
                        numberReturned++;
                    }
                }

            }

        }
        eoCoverageSetDescription.setAttribute("numberMatched",Integer.toString(numberMatched));

        eoCoverageSetDescription.setAttribute("numberReturned",Integer.toString(numberReturned));




        return new Document(eoCoverageSetDescription);
    }



}
