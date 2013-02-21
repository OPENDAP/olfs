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
package opendap.ncml;

import opendap.bes.dapResponders.DapDispatcher;
import opendap.coreServlet.DataSourceInfo;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.ReqInfo;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * IsoDispatchHandler for ISO responses from Hyrax
 */
public class NcmlDatasetDispatcher extends DapDispatcher {



    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;

    private String systemPath;
    private Element _config;


    public NcmlDatasetDispatcher(){
        //super();
        log = LoggerFactory.getLogger(getClass());
    }


    @Override
    public void init(HttpServlet servlet,Element config) throws Exception {

        NcmlDatasetBesApi besApi = new NcmlDatasetBesApi();

        ingestConfig(config);

        init(servlet, config ,besApi);

    }


    private void ingestConfig(Element config){

        _config = config;
        Element PreloadNcmlIntoBes = _config.getChild("PreloadNcmlIntoBes");
        if(PreloadNcmlIntoBes!=null){
            String value = PreloadNcmlIntoBes.getTextNormalize();
            if(value.equalsIgnoreCase("true"))
                NcmlManager.setPreloadBes(true);

            log.debug("Pre-Load Catalog NcML into BES: ",NcmlManager.preloadBes());

        }
    }


    @Override
    public DataSourceInfo getDataSourceInfo(String dataSourceName) throws Exception {
        return new NcmlDatasetInfo(dataSourceName);
    }

    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);



        log.debug("The client requested this resource: {}",relativeUrl);

        for (HttpResponder r : getResponders()) {
            log.debug("Checking responder: "+ r.getClass().getSimpleName()+ " (pathPrefix: "+r.getPathPrefix()+")");

            String candidateDataSourceId = getBesApi().getBesDataSourceID(relativeUrl,r.getRequestSuffixMatchPattern(),false);

            if (NcmlManager.isNcmlDataset(candidateDataSourceId)){
                log.info("The candidateDataSourceId: \"{}\" if an NcmlDataset.", candidateDataSourceId);

                if(r.matches(relativeUrl)) {

                    log.info("The relative URL: " + relativeUrl + " matches " +
                            "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                    if (sendResponse)
                        r.respondToHttpGetRequest(request, response);

                    return true;
                }
            }
        }


        return false;

    }







}
