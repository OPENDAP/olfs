package opendap.wcs.v1_1_2;

import org.jdom.Document;
import org.jdom.Element;

/**
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 11:39:08 PM
 * To change this template use File | Settings | File Templates.
 */
public interface WcsResponder {
        Document getCapabilities(Element getCapabilitiesRequest, String serviceUrl) throws WcsException;
        Document describeCoverage(Element describeCoverageRequest) throws WcsException;
        Document getCoverage(Element getCoverageRequest, String urlBase) throws WcsException;
}
