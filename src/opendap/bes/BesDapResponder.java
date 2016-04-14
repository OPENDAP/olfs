/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes;

import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ResourceInfo;
import opendap.dap.DapResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 11/28/11
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BesDapResponder extends DapResponder {



    private Logger log;

    private BesApi _besApi;


    public BesDapResponder(String sysPath, String requestSuffixRegex, BesApi besApi) {
        this(sysPath,null,requestSuffixRegex, besApi);
    }

    public BesDapResponder(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix);

        log = LoggerFactory.getLogger(BesDapResponder.class);

        _besApi = besApi;

    }



    @Override
    public long getLastModified(HttpServletRequest request) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = getBesApi().getBesDataSourceID(relativeUrl, false);

        ResourceInfo ri = getResourceInfo(dataSource);
        return ri.lastModified();

    }




    public ResourceInfo getResourceInfo(String resourceName) throws Exception {
        return new BESResource(resourceName, getBesApi());
    }




/**
     *
     * @param relativeUrl
     * @return
     */
    @Override
    public boolean matches(String relativeUrl) {

        return matches(relativeUrl,false);

    }


    /**
     *
     * @param relativeUrl
     * @param checkWithBes
     * @return
     *
     */
    public boolean matches(String relativeUrl, boolean checkWithBes) {

        String besDataSourceId = getBesApi().getBesDataSourceID(relativeUrl, getRequestSuffixMatchPattern(), checkWithBes);

        if(besDataSourceId!=null)
            return true;


        return false;

    }

    //public abstract boolean needsBesToMatch();
    //public abstract boolean needsBesToRespond();


    public BesApi getBesApi(){
        return _besApi;
    }

    public void setBesApi(BesApi besApi){
        _besApi = besApi;
    }



}
