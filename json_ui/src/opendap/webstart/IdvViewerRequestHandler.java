/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.webstart;

import opendap.namespaces.DAP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;

import java.io.*;
import java.util.Iterator;

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