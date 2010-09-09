package opendap.webstart;

import opendap.bes.BesXmlAPI;
import opendap.namespaces.DAP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;

import java.io.*;
import java.util.Iterator;
import java.util.Scanner;

public class IdvViewerRequestHandler extends JwsHandler {

    private Logger log;
    private String resourcesDir;
    private Element config;

    private String _serviceId = "idv";
    private String _applicationName = "Integrated Data Viewer";
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

    public String getApplicationName(){
        return _applicationName;
    }

    public String getServiceId(){
        return _serviceId;
    }



    public boolean datasetCanBeViewed(Document ddx) {

        Element dataset = ddx.getRootElement();

        Iterator i = dataset.getDescendants(new ElementFilter("Grid", DAP.DAPv32_NS));

        return i.hasNext();
    }



    public String getJnlpForDataset(String datasetUrl) {

        String  jnlp = "";

        try{
           jnlp= readFileAsString(_jnlpFileName);
        }
        catch (IOException e) {
            log.error("Unable to retrieve JNLP file: "+_jnlpFileName);
        }

        log.debug("Got JNLP:\n"+jnlp);



        jnlp = jnlp.replace("{datasetUrl}",datasetUrl);

        log.debug("Tweaked JNLP:\n"+jnlp);


        return jnlp;


    }



}