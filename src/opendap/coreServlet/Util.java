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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some handy servlet sniffing utility methods.
 */
public class Util {

    /**
     * ************************************************************************
     *
     * written.
     */
    public static String htmlSystemProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("<title>System Properties</title>\n");
        sb.append("<hr />\n");
        sb.append("<body><h2>System Properties</h2>\n");
        sb.append("<h3>Date: ").append(new Date()).append("</h3>\n");

        Properties sysp = System.getProperties();
        Enumeration<?> e = sysp.propertyNames();

        sb.append("<ul>\n");
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = System.getProperty(name);
            sb.append("<li>" + name + ": " + value + "</li>\n");
        }
        sb.append("</ul>\n");
        sb.append("<hr />\n");
        sb.append("<h3>Runtime Info:</h3>\n");
        sb.append("<pre>\n");
        sb.append(getMemoryReport());
        sb.append("<hr />\n");
        sb.append("</pre>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }


    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically availableInChunk,
     * used only for debugging
     *
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @param pw       The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public static void sendSystemProperties(HttpServletResponse response, PrintWriter pw) {
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");
        response.setStatus(HttpServletResponse.SC_OK);
        pw.println(htmlSystemProperties());
    }


    public static void printMemoryReport(PrintWriter pw){

        pw.print(getMemoryReport());

    }

    public static String getMemoryReport(){
        String msg;
        Runtime rt = Runtime.getRuntime();
        msg =  "Memory Usage:\n"    ;
        msg += " JVM Max Memory:   " + (rt.maxMemory() / 1024.0) / 1000 + " MB (JVM Maximum Allowable Heap)\n";
        msg += " JVM Total Memory: " + (rt.totalMemory() / 1024.0) / 1000 + " MB (JVM Heap size)\n";
        msg += " JVM Free Memory:  " + (rt.freeMemory() / 1024.0) / 1000 + " MB (Unused part of heap)\n";
        msg += " JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024.0) / 1000. + " MB (Currently active memory)\n";
        return msg;

    }

    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically availableInChunk,
     * used only for debugging
     *
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @param ps       The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public static void sendSystemProperties(HttpServletResponse response, PrintStream ps) {
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");
        response.setStatus(HttpServletResponse.SC_OK);
        ps.println(htmlSystemProperties());
        ps.flush();

    }


    public static String urlInfo(URL url){
        String msg = "\n";
        msg += "URL: "+url.toString()+"\n";
        msg += "  protocol:      "+url.getProtocol()+"\n";
        msg += "  host:          "+url.getHost()+"\n";
        msg += "  port:          "+url.getPort()+"\n";
        msg += "  default port:  "+url.getDefaultPort()+"\n";
        msg += "  path:          "+url.getPath()+"\n";
        msg += "  query:         "+url.getQuery()+"\n";
        msg += "  file:          "+url.getFile()+"\n";
        msg += "  ref:           "+url.getRef()+"\n";
        msg += "  user info:     "+url.getUserInfo()+"\n";
        //msg += "  hash code:     "+url.hashCode()+"\n";  // Commented out because hasCode() method can block for Domain Name resolution

        try {
            msg += "  URI:           "+url.toURI().toASCIIString()+"\n";
        } catch (URISyntaxException e) {
            msg += "  URI:            error: Could not express the URL as URI because: "+e.getMessage()+"\n";
        }
        return msg;
    }

    public static String uriInfo(URI uri){

        String msg = "\n";
        msg += "URI: "+uri.toString()+"\n";
        msg += "  Authority:              "+uri.getAuthority()+"\n";
        msg += "  Host:                   "+uri.getHost()+"\n";
        msg += "  Port:                   "+uri.getPort()+"\n";
        msg += "  Path:                   "+uri.getPath()+"\n";
        msg += "  Query:                  "+uri.getQuery()+"\n";
        msg += "  hashCode:               "+uri.hashCode()+"\n";
        msg += "  Fragment:               "+uri.getFragment()+"\n";
        msg += "  RawAuthority:           "+uri.getRawAuthority()+"\n";
        msg += "  RawFragment:            "+uri.getRawFragment()+"\n";
        msg += "  RawPath:                "+uri.getRawPath()+"\n";
        msg += "  RawQuery:               "+uri.getRawQuery()+"\n";
        msg += "  RawSchemeSpecificPart:  "+uri.getRawSchemeSpecificPart()+"\n";
        msg += "  RawUSerInfo:            "+uri.getRawUserInfo()+"\n";
        msg += "  Scheme:                 "+uri.getScheme()+"\n";
        msg += "  SchemeSpecificPart:     "+uri.getSchemeSpecificPart()+"\n";
        msg += "  UserInfo:               "+uri.getUserInfo()+"\n";
        msg += "  isAbsoulte:             "+uri.isAbsolute()+"\n";
        msg += "  isOpaque:               "+uri.isOpaque()+"\n";
        msg += "  ASCIIString:            "+uri.toASCIIString()+"\n";

        try {
            msg += "  URL:                    "+uri.toURL()+"\n";
        } catch (Exception e) {
            msg += "  URL:                    uri.toURL() FAILED msg="+e.getMessage();
        }

        return msg;
    }


    public static String checkRegex(Matcher m, boolean isMatched){
        StringBuilder s = new StringBuilder();
        s.append("\n---------------------------------------------------\n");
        s.append("checkRegex():\n");
        s.append("  Matcher.find():        ").append(isMatched).append("\n");
        s.append("  Matcher.hitEnd():      ").append(m.hitEnd()).append("\n");
        s.append("  Matcher.requireEnd():  ").append(m.requireEnd()).append("\n");
        s.append("  Matcher.regionStart(): ").append(m.regionStart()).append("\n");
        s.append("  Matcher.regionEnd():   ").append(m.regionEnd()).append("\n");
        if(isMatched){
            s.append("  Matcher.group():       ").append(m.group()).append("\n");
            s.append("  Matcher.start():       ").append(m.start()).append("\n");
            s.append("  Matcher.end():         ").append(m.end()).append("\n");
        }
        s.append("\n");
        return s.toString();
    }



    public static String dropSuffixFrom(String s, Pattern suffixPattern){
        Logger log =  LoggerFactory.getLogger(Util.class);
        log.debug("dropSuffixFrom() - regex: '{}'   inputString: '{}'",suffixPattern.pattern(),s);
        String result = s;

        Matcher suffixMatcher = suffixPattern.matcher(s);
        boolean suffixMatched = false;
        while(!suffixMatcher.hitEnd()){
            suffixMatched = suffixMatcher.find();
            log.debug("{}", Util.checkRegex(suffixMatcher, suffixMatched));
        }
        if(suffixMatched){
            int start =  suffixMatcher.start();
            result =  s.substring(0, start);
        }
        log.debug("dropSuffixFrom() - returning '{}'",result);
        return result;
    }


    public static boolean matchesSuffixPattern(String s, Pattern suffixPattern){
        Matcher suffixMatcher = suffixPattern.matcher(s);
        boolean suffixMatched = false;
        while(!suffixMatcher.hitEnd()){
            suffixMatched = suffixMatcher.find();
            LoggerFactory.getLogger(Util.class).debug("{}", Util.checkRegex(suffixMatcher, suffixMatched));
        }
        return suffixMatched;
    }



}




