package opendap.wcs.v2_0;

import org.jdom.Document;
import org.jdom.Element;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by ndp on 9/8/16.
 */
public class DescribeEOCoverageSetRequestProcessor {

    private static void addEoWcsNameSpaces(Element e){

        e.addNamespaceDeclaration(WCS.GML_NS);
        e.addNamespaceDeclaration(WCS.SWE_NS);
        e.addNamespaceDeclaration(WCS.GMLCOV_NS);
        e.addNamespaceDeclaration(WCS.XSI_NS);
        e.addNamespaceDeclaration(WCS.WCSEO_NS);

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

        int numberMatched = 0;
        int numberReturned = 0;

        Vector<String> unprocessedIds = new Vector<>();

        HashMap<String,EOCoverageDescription> resultCDs = null;
        HashMap<String,EODatasetSeries > resultDSs = null;

        if(req.hasSection(DescribeEOCoverageSetRequest.Sections.CoverageDescriptions) |
                req.hasSection(DescribeEOCoverageSetRequest.Sections.All)) {
            for (String id : req.getEoIds()) {
                EOCoverageDescription eoCoverageDescription = CatalogWrapper.getEOCoverageDescription(id);
                if (eoCoverageDescription != null) {

                    //@TODO Evaluate subset here!!

                    numberMatched++;
                    if (resultCDs == null) {
                        resultCDs = new HashMap<>();
                    }
                    if (numberReturned < req.getMaxItemCount()) {
                        resultCDs.put(id,eoCoverageDescription);
                        numberReturned++;
                    }
                } else {
                    unprocessedIds.add(id);
                }
            }
        }

        Vector<String> remaingIds = unprocessedIds;
        unprocessedIds = new Vector<>();


        if(req.hasSection(DescribeEOCoverageSetRequest.Sections.DatasetSeriesDescriptions) |
                req.hasSection(DescribeEOCoverageSetRequest.Sections.All)) {

            resultDSs  = new HashMap<>();

            for(String eoId: remaingIds){
                EODatasetSeries eoDatasetSeries = CatalogWrapper.getEODatasetSeries(eoId);
                if(eoDatasetSeries!=null ) {

                    //@TODO Evaluate subset here!!



                    numberMatched++;

                    if( numberReturned < req.getMaxItemCount()) {
                        resultDSs.put(eoId,eoDatasetSeries);
                        numberReturned++;
                    }

                    for(EOCoverageDescription eoCoverageDescription: eoDatasetSeries.getMembers()){
                        if (resultCDs == null) {
                            resultCDs = new HashMap<>();
                        }

                        if( numberReturned < req.getMaxItemCount()) {
                            resultCDs.put(eoCoverageDescription.getCoverageId(),eoCoverageDescription);
                            numberReturned++;
                        }

                    }




                }
                else {
                    unprocessedIds.add(eoId);
                }

            }

        }

        if(!unprocessedIds.isEmpty()){

            StringBuilder sb = new StringBuilder();

            sb.append("Only IDs associated with EOCoverages or DatasetSeries may be submitted to ");
            sb.append("DescribeEOCoverageSet request. The the submitted coverage identifiers '");
            for(String id:unprocessedIds){
                sb.append(unprocessedIds.indexOf(id)>0?",":"").append(id);
            }
            sb.append("' is/are not associated with a recognized EOCoverage or DatsetSeries.");

            throw new WcsException(sb.toString(),
                    WcsException.INVALID_PARAMETER_VALUE,"wcseo:DescribeEOCoverageSetRequest");

        }





        if(resultCDs!=null) {
            Element eoCoverageDescriptions = new Element("CoverageDescriptions", WCS.WCS_NS);
            eoCoverageSetDescription.addContent(eoCoverageDescriptions);
            for (String eoId : resultCDs.keySet()) {
                EOCoverageDescription eoCoverageDescription = resultCDs.get(eoId);
                eoCoverageDescriptions.addContent(eoCoverageDescription.getCoverageDescriptionElement());

            }
        }

        if(resultDSs!=null) {
            Element eoDatasetSeriesDescriptions = new Element("DatasetSeriesDescriptions", WCS.WCSEO_NS);
            eoCoverageSetDescription.addContent(eoDatasetSeriesDescriptions);
            for (String eoId : resultDSs.keySet()) {
                EODatasetSeries eoDatasetSeries = resultDSs.get(eoId);
                eoDatasetSeriesDescriptions.addContent(eoDatasetSeries.getDatasetSeriesDescriptionElement());
            }
        }



        eoCoverageSetDescription.setAttribute("numberMatched",Integer.toString(numberMatched));

        eoCoverageSetDescription.setAttribute("numberReturned",Integer.toString(numberReturned));




        return new Document(eoCoverageSetDescription);
    }



}
