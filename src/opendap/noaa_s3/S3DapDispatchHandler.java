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

package opendap.noaa_s3;

import opendap.bes.BesDapDispatcher;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.FileAccess;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ServletUtil;
import opendap.coreServlet.Util;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/21/13
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3DapDispatchHandler extends BesDapDispatcher {

    private Logger log;

    private S3BesApi _besApi;

    private static final String _defaultPrefix = "s3/";
    private String _prefix;

    private boolean _initialized ;

    private FileAccess fileResponder;

    public S3DapDispatchHandler() {
        this(_defaultPrefix);
    }

    public S3DapDispatchHandler(String prefix) {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;
        _besApi = null;
        _prefix = prefix;


    }


    @Override
    public void init(HttpServlet servlet, Element config) throws Exception {

        if(_initialized)
            return;

        ingestPrefix(config);
        _besApi = new S3BesApi(_prefix);


        init(servlet, config, _besApi);

        String systemPath = ServletUtil.getSystemPath(servlet, "");


        // Non data resources need a special fileResponder.
        fileResponder = new FileAccess(systemPath, null, "", _besApi);
        fileResponder.clearAltResponders();
        fileResponder.setCombinedRequestSuffixRegex(fileResponder.buildRequestMatchingRegex());
        fileResponder.setAllowDirectDataSourceAccess(true);

        _initialized = true;
    }




    @Override
    public long getLastModified(HttpServletRequest req) {

        String relativeURL = ReqInfo.getLocalUrl(req);

        log.debug("relativeURL: {}",relativeURL);
        long lmt = new Date().getTime();

        for (HttpResponder r : getResponders()) {
            log.debug("Checking responder: {} (pathPrefix: {})",r.getClass().getSimpleName(), r.getPathPrefix());

           if(r instanceof Dap4Responder) {
               Dap4Responder d4r = (Dap4Responder)r;
               Pattern suffixPattern = Pattern.compile(d4r.getCombinedRequestSuffixRegex(), Pattern.CASE_INSENSITIVE);

               if(Util.matchesSuffixPattern(relativeURL, suffixPattern)) {

                   log.info("The relative URL: {}  matches the pattern: \"{}\"",
                           relativeURL, r.getRequestMatchRegexString());

                   String resourceId = d4r.removeRequestSuffixFromString(relativeURL);
                   S3IndexedFile s3if = S3CatalogManager.theManager().getIndexedFile(resourceId);

                   if(s3if!=null){
                       lmt = s3if.getLastModified();
                       log.debug("Returning: {}",lmt);
                       return lmt;
                   }
               }
           }
        }
        log.debug("Returning: {}",lmt);
        return lmt;
    }





    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception  {


        String relativeURL = ReqInfo.getLocalUrl(request);


        log.debug("relativeURL:    "+relativeURL);



        while(relativeURL.startsWith("/") && relativeURL.length()>1)
            relativeURL = relativeURL.substring(1,relativeURL.length());


        //boolean itsJustThePrefixWithoutTheSlash = _prefix.substring(0,_prefix.lastIndexOf("/")).equals(relativeURL);
        //boolean itsJustThePrefix = _prefix.equals(relativeURL);

        if (relativeURL != null) {
            if (sendResponse) {
                log.info("Sending DAP Response for S3 holding.");

                if(!super.requestDispatch(request,response, true)  && !response.isCommitted()){


                    try {
                        fileResponder.sendNormativeRepresentation(request,response);
                    } catch (Exception e) {
                        return false;
                    }

                }
                else
                    log.info("Sent DAP Response for S3 holding.");
            }
        }

        return true;
    }







    private void ingestPrefix(Element config) throws Exception {


        if (config != null) {

            String msg;

            Element e = config.getChild("prefix");
            if (e != null)
                _prefix = e.getTextTrim();

            if (_prefix.equals("/")) {
                msg = "Bad Configuration. The <Handler> " +
                        "element that declares " + this.getClass().getName() +
                        " MUST provide 1 <prefix>  " +
                        "child element whose value may not be equal to \"/\"";
                log.error(msg);
                throw new Exception(msg);
            }


            if (!_prefix.endsWith("/"))
                _prefix += "/";

            if (_prefix.startsWith("/"))
                _prefix = _prefix.substring(1);

        }
        log.info("Using prefix=" + _prefix);

    }




}
