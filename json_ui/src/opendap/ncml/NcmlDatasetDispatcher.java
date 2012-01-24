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

import opendap.bes.BESDataSource;
import opendap.coreServlet.*;
import opendap.bes.dapResponders.*;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * IsoDispatchHandler for ISO responses from Hyrax
 */
public class NcmlDatasetDispatcher extends DapDispatcher {



    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;

    private String systemPath;
    private Element _config;
    private Vector<HttpResponder> responders;


    public NcmlDatasetDispatcher(){
        log = LoggerFactory.getLogger(getClass());
        responders = new Vector<HttpResponder>();

    }


    @Override
    public void init(HttpServlet servlet,Element config) throws Exception {

        NcmlDatasetBesApi besApi = new NcmlDatasetBesApi();

        init(servlet, config ,besApi);

    }


    @Override
    public DataSourceInfo getDataSourceInfo(String dataSourceName) throws Exception {
        return new NcmlDatasetInfo(dataSourceName);
    }









}
