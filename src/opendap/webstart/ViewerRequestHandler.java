package opendap.webstart;

import javax.servlet.http.HttpServletRequest;

interface ViewerRequestHandler {

    public void init(String resourcesDirectory);
    
    public boolean datasetCanBeViewed(String datasetRequestUrl);

    public String getJnlpForDataset(String datasetRequestUrl);


}