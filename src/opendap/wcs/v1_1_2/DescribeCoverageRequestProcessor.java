package opendap.wcs.v1_1_2;

import org.jdom.Document;
import org.jdom.Element;
import opendap.coreServlet.Scrub;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 12, 2010
 * Time: 2:02:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class DescribeCoverageRequestProcessor {

    public static Document processDescribeCoveragesRequest(DescribeCoverageRequest req) throws WcsException {


        Element coverageDescriptions = new Element("CoverageDescriptions",WCS.WCS_NS);
        CoverageDescription cd;

        for(String id: req.getIds()){
            cd = CatalogWrapper.getCoverageDescription(id);
            if(cd==null)
                throw new WcsException("No such wcs:Coverage: "+ Scrub.fileName(id),
                        WcsException.INVALID_PARAMETER_VALUE,"wcs:Identifier");

            coverageDescriptions.addContent(cd.getElement());
        }

        return new Document(coverageDescriptions);

    }

}
