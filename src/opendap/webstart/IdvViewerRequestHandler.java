package opendap.webstart;

import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

public class IdvViewerRequestHandler implements ViewerRequestHandler {

    private String resourcesDir;
    private Logger log;

    public void init(String resourcesDirectory) {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        resourcesDir = resourcesDirectory;

    }


    public boolean datasetCanBeViewed(String datasetRequestUrl) {
        return true;
    }

    public String getViewerLinkHtml(String datasetRequestUrl) {
        String dataURI = "";

        return "<a href='" + datasetRequestUrl + "/view/idv.jnlp?url=" + dataURI.toString() + "'>Integrated Data Viewer (IDV) (webstart)</a>";
    }


    public String getJnlpForDataset(String datasetRequestUrl) {
        return null;

    }


}