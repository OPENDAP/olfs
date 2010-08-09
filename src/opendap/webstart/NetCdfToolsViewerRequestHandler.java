package opendap.webstart;

import org.jdom.Element;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 27, 2010
 * Time: 9:54:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class NetCdfToolsViewerRequestHandler extends JwsHandler {

    private Logger log;
    private String resourcesDir;
    private Element config;

    private String HTTP = "http://";

    private String _serviceId = "netcdfToolsUI";
    private String _jnlpFileName = _serviceId+".jnlp";


    public void init(Element config, String resourcesDirectory) {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        resourcesDir = resourcesDirectory;
        this.config = config;
        _jnlpFileName = resourcesDirectory+"/"+ _serviceId+".jnlp";

        File f = new File(_jnlpFileName);

        if(!f.exists()){
            log.error("Missing JNLP file: "+_jnlpFileName);
        }

    }



    public boolean datasetCanBeViewed(String serviceId, String query) {
        log.debug("Checking request. serviceId:"+serviceId+"   query: "+query);
        if(_serviceId.equalsIgnoreCase(serviceId))
            return true;
        else
            return false;
    }

    public String getViewerLinkHtml(String context, String datasetURI) {

        return "<a href='" + context + "/webstart/netcdfToolsUI.jnlp?url=" + datasetURI + "'>IDV</a>";
    }


    public String getJnlpForDataset(String query) {

        String queryStart = "dataset=";

        String datasetUrl = "";
        if(query.startsWith(queryStart)){
            datasetUrl = query.substring(queryStart.length(),query.length());
        }



        String  jnlp = "";

        try{
           jnlp= readFileAsString(_jnlpFileName);
        }
        catch (IOException e) {
            log.error("Unable to retrieve JNLP file: "+_jnlpFileName);
        }

        log.debug("Got JNLP:\n"+jnlp);


        String catalogUrl = "";

        if(datasetUrl.contains("/"))
            catalogUrl = datasetUrl.substring(0,datasetUrl.lastIndexOf("/")+1);

        catalogUrl += "catalog.xml";

        log.debug("catalogUrl: "+catalogUrl);

        String datasetId = "";

        if(datasetUrl.startsWith(HTTP)){
            datasetId = datasetUrl.substring(HTTP.length(),datasetUrl.length());
            datasetId = datasetId.substring(datasetId.indexOf("/"),datasetId.length());
        }

        log.debug("datasetId: "+datasetId);


        jnlp = jnlp.replace("{datasetUrl}",datasetUrl);

        log.debug("Tweaked JNLP:\n"+jnlp);


        return jnlp;


    }


}
