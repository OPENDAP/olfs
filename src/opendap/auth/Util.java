/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2022 OPeNDAP, Inc.
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

package opendap.auth;

import opendap.PathBuilder;
import opendap.coreServlet.ReqInfo;
import opendap.io.HyraxStringEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Map;

import static opendap.auth.IdFilter.USER_PROFILE;

/**
 * Created by ndp on 9/24/14.
 */
public class Util {


    /**
     * Utility method used to submit an HTTP request.
     *
     * This method will submit a GET request unless 'data' is non-null,
     * in which case it will be considered a POST request.
     *
     */
    public static String submitHttpRequest( String url, Map<String, String> headers, String data )
        throws IOException
    {
        Logger log = LoggerFactory.getLogger("opendap.auth.Util");
        StringBuilder result = new StringBuilder();
        HttpURLConnection connection = null;

        try
        {
            // Create a connection and build the request
            connection = (HttpURLConnection) (new URL(url)).openConnection();

            connection.setUseCaches(false);
            connection.setDoInput(true);

            for(Map.Entry<String, String> header: headers.entrySet()){
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // If data is provided, then convert it to a POST request.
            if( data != null )
            {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(data);
                out.flush();
                out.close();
            }

            int http_status = connection.getResponseCode();

            try {
                // Here we try to get the response body even if it is an error
                // because the server may ave sent back something useful in
                // addition to the status.
                InputStream is = connection.getInputStream();
                // Extract the body of the response so we can return it.
                // We always want this even if the http status is an error.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(is,HyraxStringEncoding.getCharset()));

                String line;
                while( (line = in.readLine()) != null ) result.append(line);
                in.close();
                is.close();
            }
            catch(IOException e){
                log.error("Caught {} message: {}",e.getClass().getName(),e.getMessage());
            }

            // Check the response to the request. We consider anything other than
            // 200 (OK) as an error, though it may be useful to be able to return
            // this value to the caller and let the caller decide.
            if( http_status != 200 )
            {
                StringBuilder msg = new StringBuilder();
                msg.append("HTTP request failed. status: ").append(http_status);
                msg.append(" url: ").append(url);
                msg.append(" message: ").append(result.toString());
                throw new IOException(msg.toString());
            }

        }
        finally
        {
            if( connection != null ) connection.disconnect();
        }

        return result.toString();
    }


    /**
     * Makes a json-like string of the request headers...
     * @param request the HttpServletRequest to explore and to stringify
     * @param log The log object to which to log the HttpServletRequest info
     */
    static void debugHttpRequest(HttpServletRequest request, Logger log) throws IOException{

        if(log.isDebugEnabled()) {
            log.debug("HttpServletRequest.getMethod(): {}",request.getMethod());
            log.debug("HttpServletRequest.getRequestURL(): {}",request.getRequestURL());
            log.debug("HttpServletRequest.getRequestURI(): {}",request.getRequestURI());
            log.debug("HttpServletRequest.getServerName(): {}",request.getServerName());
            log.debug("HttpServletRequest.getServerPort(): {}",request.getServerPort());
            log.debug("HttpServletRequest.getQueryString(): {}",request.getQueryString());
            Enumeration<String> h = request.getHeaderNames();
            while (h.hasMoreElements()) {
                String name = h.nextElement();
                Enumeration<String> v = request.getHeaders(name);
                while (v.hasMoreElements()) {
                    String value = v.nextElement();
                    log.debug("{}: {}", name, value);
                }
            }
            log.debug("ReqInfo.getConstraintExpression(): {}",ReqInfo.getConstraintExpression(request));
            log.debug("ReqInfo.getLocalUrl(): {}",ReqInfo.getLocalUrl(request));
        }
    }

    /**
     * Here we make sure that request is really for something that the user would like to return to before we cache
     * the URL. Pretty much we are trying to el,inate page componets like java script, xsl, images, css, etc.
     * @param session
     * @param requestUrl
     * @param requestURI
     * @param contextPath
     */
    static void cacheRequestUrlAsNeeded(HttpSession session, String requestUrl, String requestURI, String contextPath){
        Logger log = LoggerFactory.getLogger("opendap.auth.Util");

        String docsPath = PathBuilder.pathConcat(contextPath,"docs");
        String xslPath = PathBuilder.pathConcat(contextPath,"xsl");
        String jsPath = PathBuilder.pathConcat(contextPath,"js");
        String webStartPath = PathBuilder.pathConcat(contextPath,"WebStart");

        log.debug("requestURI:  {}",requestURI);
        log.debug("requestUrl:  {}",requestUrl);
        log.debug("contextPath: {}",contextPath);

        if(requestURI.startsWith(docsPath) ||
                requestURI.startsWith(xslPath) ||
                requestURI.startsWith(jsPath)  ||
                requestURI.startsWith(webStartPath) ||
                requestURI.equalsIgnoreCase("favicon.ico")
                ){
            log.debug("Not caching request url: {}",requestUrl);
            return;
        }
        if(log.isDebugEnabled()){
            String msg ="Caching request URL as session Attribute with key '"+ IdFilter.RETURN_TO_URL+"' ";
            msg += "and value: " + requestUrl;
            msg += " (session: "+session.getId()+")";
            log.debug(msg);
        }
        session.setAttribute(IdFilter.RETURN_TO_URL,requestUrl);
        log.debug("Sanity check session.getAttribute("+ IdFilter.RETURN_TO_URL+") returns {} (session: {})",session.getAttribute(IdFilter.RETURN_TO_URL), session.getId());
    }

    public static String getUID(HttpServletRequest req){
        HttpSession session  = req.getSession(false);
        String uid = null;
        if(session!=null){
            UserProfile up = (UserProfile) session.getAttribute(USER_PROFILE);
            if(up!=null){
                uid = up.getUID();
            }
        }
        if (uid!=null && uid.isEmpty()) {
            String remoteUser = req.getRemoteUser();
            if (remoteUser == null) {
                Principal userPrinciple = req.getUserPrincipal();
                if (userPrinciple != null) {
                    uid = userPrinciple.getName();
                }
            }
            else {
                uid = remoteUser;
            }
        }
        return uid;
    }
}
