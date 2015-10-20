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
package opendap.coreServlet;

import opendap.io.HyraxStringEncoding;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public abstract class HttpResponder {

    private Pattern _requestMatchPattern;
    protected String _systemPath;
    private String pathPrefix;

    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(HttpResponder.class);
    }

    public static final String HttpDatFormatString = "EEE, d MMM yyyy hh:mm:ss z";




    private HttpResponder(){}

    protected HttpResponder(String sysPath, String pathPrefix, String regexPattern){
        super();
        _requestMatchPattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        _systemPath = sysPath;
        this.pathPrefix = pathPrefix;
    }

    public String getRequestMatchRegexString(){
        return _requestMatchPattern.toString();
    }

    public void setRequestMatchRegex(String regexString){
        _requestMatchPattern = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
    }


    public Pattern getRequestSuffixMatchPattern(){
        return _requestMatchPattern;
    }


    public boolean matches(String s){
       Matcher m = getRequestSuffixMatchPattern().matcher(s);
       return m.matches();

    }


    public void setPathPrefix(String prefix){ pathPrefix = prefix ;}

    public String getPathPrefix() { return pathPrefix; }


    public abstract ResourceInfo getResourceInfo(String resourceName) throws Exception;
    public abstract long getLastModified(HttpServletRequest request) throws Exception ;
    public abstract void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;



    public  void respondToHttpPostRequest(HttpServletRequest request, HttpServletResponse response) throws Exception{
        response.getWriter().append("POST is not implemented by this Responder");
    }



    public void sendHttpErrorResponse(int HttpStatus, String errorMessage, String docsService, HttpServletResponse response) throws Exception {
        String errorPageTemplate = _systemPath + "/error/error.html.proto";
        sendHttpErrorResponse( HttpStatus,  errorMessage,  errorPageTemplate,  docsService, response);
    }


    public static void sendHttpErrorResponse(int httpStatus, String errorMessage, String errorPageTemplate, String context,  HttpServletResponse response) throws Exception {

        String template = loadHtmlTemplate(errorPageTemplate, context);

        template = template.replaceAll("<ERROR_MESSAGE />",Scrub.simpleString(errorMessage));

        log.debug("respondToHttpGetRequest(): Sending Error Page ");

        response.setContentType("text/html");
        response.setHeader("Content-Description", "error_page");
        response.setStatus(httpStatus);

        ServletOutputStream sos  = response.getOutputStream();

        sos.println(template);

    }



    public static String loadHtmlTemplate(String htmlTemplateFile, String context) throws Exception {
        String template = readFileAsString(htmlTemplateFile);
        template = template.replaceAll("<CONTEXT />",context);
        return template;
    }





    public static String readFileAsString(String fileName) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(new File(fileName), HyraxStringEncoding.getCharset().name());

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }


    public static String streamToString(InputStream is) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(is, HyraxStringEncoding.getCharset().name());

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }





    public void destroy(){

    }




}
