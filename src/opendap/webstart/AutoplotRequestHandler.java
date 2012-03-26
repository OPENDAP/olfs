/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2012 OPeNDAP, Inc.
// Author: Bernard Harris
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

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.slf4j.Logger;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * This class implements a JwsHandler for the
 * <a href="http://autoplot.org/">Autoplot</a>data viewer.
 *
 */
public class AutoplotRequestHandler 
    extends JwsHandler {

    private Logger log;
    private Element config;

    private String _serviceId = "Autoplot";
    private String _applicationName = "Autoplot";


    public void init(Element config, String resourcesDirectory) {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        this.config = config;
    }


    public String getApplicationName(){
        return _applicationName;
    }


    public String getServiceId(){
        return _serviceId;
    }


    /**
     * Autoplot supported DISPLAY_TYPE values.  That is, data with any
     * of the DISPLAY_TYPE values in this set can be visualized by the
     * autoplot application.
     */
    private static final Set<String> SUPPORTED_DISPLAY_TYPES;

    static {
        SUPPORTED_DISPLAY_TYPES = new HashSet<String>();

        SUPPORTED_DISPLAY_TYPES.add("\"time_series\"");
        SUPPORTED_DISPLAY_TYPES.add("\"spectrogram\"");
        SUPPORTED_DISPLAY_TYPES.add("\"image\"");
        SUPPORTED_DISPLAY_TYPES.add("\"stack_plot\"");
    }


    /**
     * Determines if Autoplot can display the dataset described by the
     * given DDX document.
     *
     * @param ddx a document describing a dataset.
     * @return true if Autoplot can display the specified dataset or
     *     false if it cannot.
     */
    public boolean datasetCanBeViewed(Document ddx) {

        Element dataset = ddx.getRootElement();
                                       // root ddx element
        Iterator attrElements = 
            dataset.getDescendants(
                new ElementFilter("Attribute", DAP.DAPv32_NS));
                                       // <Attribute> element iterator

        while (attrElements.hasNext()) {

            Element attrElement = (Element)attrElements.next();
                                       // an <Attribute> element
            Attribute nameAttr = attrElement.getAttribute("name");
                                       // name attribute of <Attribute>
                                       // element
            if (nameAttr != null && 
                nameAttr.getValue().equalsIgnoreCase("DISPLAY_TYPE")) {

                Attribute typeAttr = attrElement.getAttribute("type");
                                       // type attribute of <Attribute>
                                       // element
                if (typeAttr != null && 
                    typeAttr.getValue().equalsIgnoreCase("String")) {

                    String value = 
                        attrElement.getChildTextTrim("value", 
                            DAP.DAPv32_NS);
                                       // value of <value> element
                    if (value != null &&
                        SUPPORTED_DISPLAY_TYPES.contains(value)) {

                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * Produces a string containing a JNLP that will launch Autoplot
     * to display the given dataset.
     *
     * @param datasetUrl URL to the dataset that is to be displayed.
     * @return a string containing a JNLP that will launch Autoplot
     *     to display the given dataset.
     */
    public String getJnlpForDataset(String datasetUrl) {

        try {

            URL jnlpUrl = 
                new URL("http://autoplot.org/autoplot.jnlp?uri=" + 
                    datasetUrl + ".dds");
                                       // url for a service that can 
                                       // produce an autoplot jnlp to
                                       // display the given dataset
            BufferedReader jnlpReader = 
                new BufferedReader(
                    new InputStreamReader(jnlpUrl.openStream()));
                                       // reader to get the jnlp
            StringBuilder jnlpStringBuilder = new StringBuilder(4 * 1024);
                                       // StringBuilder into which the
                                       // jnlp is read
            int c;                     // a character used to read the
                                       // jnlp 
            while ((c = jnlpReader.read()) != -1) {

                jnlpStringBuilder.append((char)c);
            }

            return jnlpStringBuilder.toString();
        }
        catch (MalformedURLException e) {

            log.error("Malformed Autoplot JNLP URL: " + e.getMessage());
        }
        catch (IOException e) {

            log.error("IOException while retrieving Autoplot JNLP URL: " + 
                e.getMessage());
        }

        return "";
    }


}
