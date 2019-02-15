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

package opendap.dap;

import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class DapResponder extends HttpResponder  {

    private static String matchAnythingRegex = ".*";

    protected String requestSuffix;

    private Logger log;

    private String _serviceRoleId = null;
    private String _serviceTitle = null;
    private String _serviceDescription = null;
    private String _serviceDescriptionLink = null;


    public DapResponder(String sysPath, String requestSuffix) {
        this(sysPath,null,requestSuffix);
    }

    public DapResponder(String sysPath, String pathPrefix, String requestSuffix) {
        super(sysPath, pathPrefix, matchAnythingRegex);

        log = LoggerFactory.getLogger(this.getClass());

        setRequestSuffix(requestSuffix);
    }


    public void setRequestSuffix(String reqSuffix){

        requestSuffix = reqSuffix;

        String requestSuffixRegex = reqSuffix;

        if(!reqSuffix.endsWith("?"))
            requestSuffixRegex += "?";

        if(reqSuffix.startsWith("."))
            requestSuffixRegex = "\\"+reqSuffix;

        String requestMatchRegex = matchAnythingRegex + requestSuffixRegex;

        setRequestMatchRegex(requestMatchRegex);

    }


    public String getRequestSuffix(){
        return requestSuffix;
    }



    public String getXmlBase(HttpServletRequest req){
        String requestUrl = ReqInfo.getRequestUrlPath(req);
        String xmlBase = removeRequestSuffixFromString(requestUrl);
        log.debug("@xml:base='{}'",xmlBase);
        return xmlBase;
    }


    public String removeRequestSuffixFromString(String requestString){

        Pattern pattern = getRequestSuffixMatchPattern();

        String trimmedRequestString = Util.dropSuffixFrom(requestString,pattern);

        log.debug("removeRequestSuffixFromString() - trimmed: {}",trimmedRequestString);

         return trimmedRequestString;
    }



    public String getServiceRoleId(){
        return _serviceRoleId;
    }
    public String getServiceTitle(){
        return _serviceTitle;
    }
    public String getServiceDescription(){
        return _serviceDescription;
    }
    public String getServiceDescriptionLink(){
        return _serviceDescriptionLink;
    }

    protected void setServiceRoleId(String serviceRoleId){
        _serviceRoleId = serviceRoleId;
    }
    protected void setServiceTitle(String serviceTitle){
        _serviceTitle = serviceTitle;
    }
    protected void setServiceDescription(String serviceDescription){
        _serviceDescription = serviceDescription;
    }
    protected void setServiceDescriptionLink(String serviceDescriptionLink){
        _serviceDescriptionLink =  serviceDescriptionLink;
    }



    public abstract boolean isMetadataResponder();
    public abstract boolean isDataResponder();




}
