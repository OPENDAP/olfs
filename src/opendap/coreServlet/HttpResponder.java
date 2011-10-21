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
package opendap.coreServlet;

import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 */
public abstract class HttpResponder {

    private Pattern _pattern;
    protected String _systemPath;
    private String pathPrefix;

    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(HttpResponder.class);
    }




    private HttpResponder(){}

    public HttpResponder(String sysPath, String pathPrefix, String regexPattern){
        super();
        _pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        _systemPath = sysPath;
        this.pathPrefix = pathPrefix;
    }

    public Pattern getPattern(){ return _pattern;}
    public void setPattern(String regexPattern){ _pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);}

    public boolean matches(String s){
       return _pattern.matcher(s).matches();

    }


    public void setPathPrefix(String prefix){ pathPrefix = prefix ;}
    public String getPathPrefix() { return pathPrefix; }


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
        Scanner scanner = new Scanner(new File(fileName));

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
        Scanner scanner = new Scanner(is);

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }






}
