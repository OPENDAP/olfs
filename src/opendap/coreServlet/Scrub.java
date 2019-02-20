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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: ndp
 * Date: Aug 14, 2007
 * Time: 3:28:03 PM
 */
public class Scrub {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(Scrub.class);

    // The C++ coe uses this regex: Regex name("[-0-9A-z_./]+");

    private static final String FILE_NAME_INCLUSION_REGEX = "[-a-zA-Z0-9/.%_ :]*";
    private static final String FILE_NAME_EXCLUSION_REGEX = "[^-a-zA-Z0-9/.%_ :]";
    private static final Pattern FILE_NAME_INCLUSION_PATTERN = Pattern.compile(FILE_NAME_INCLUSION_REGEX);

    public static String fileName(String fileName){
        if(fileName==null)
            return null;
        Matcher m = FILE_NAME_INCLUSION_PATTERN.matcher(fileName);

        log.debug("fileName() - Scrubbing file Name: "+fileName+"   white list pattern: "+ FILE_NAME_INCLUSION_REGEX +"    matches: "+m.matches());
        if(m.matches()){
            return fileName;
        }
        else {
            return fileName.replaceAll(FILE_NAME_EXCLUSION_REGEX,"#");
        }

    }

    // Added the colon (:) to the list of allowed characters, based on 
    // a bug report from NASA/Raytheon. jhrg 5/7/15
    private static final String URL_CONTENT_INCLUSION_REGEX = "[-a-zA-Z0-9/.%_ :]*";
    private static final String URL_CONTENT_EXCLUSION_REGEX = "[^-a-zA-Z0-9/.%_ :]";
    private static final Pattern URL_CONTENT_INCLUSION_PATTERN = Pattern.compile(URL_CONTENT_INCLUSION_REGEX);

    public static String urlContent(String urlContent){

        if(urlContent==null)
            return null;

        Matcher m = URL_CONTENT_INCLUSION_PATTERN.matcher(urlContent);

        log.debug("urlContent() - Scrubbing URL Content: "+urlContent+"   white list pattern: "+ URL_CONTENT_INCLUSION_REGEX +"    matches: "+m.matches());

        if(m.matches()){
            return urlContent;
        }
        else {
            return urlContent.replaceAll(URL_CONTENT_EXCLUSION_REGEX,"#");
        }
    }

    private static final String COMPLETE_URL_INCLUSION_REGEX = "http(s)?://"+ URL_CONTENT_INCLUSION_REGEX;
    private static final String COMPLETE_URL_EXCLUSION_REGEX = "http(s)?://"+ URL_CONTENT_EXCLUSION_REGEX;
    private static final Pattern COMPLETE_URL_INCLUSION_PATTERN = Pattern.compile(COMPLETE_URL_INCLUSION_REGEX);

    public static String completeURL(String url){
        if(url==null)
            return null;
        Matcher m = COMPLETE_URL_INCLUSION_PATTERN.matcher(url);
        log.debug("completeURL() - Scrubbing complete URL: "+url+"   white list pattern: "+ COMPLETE_URL_INCLUSION_REGEX +"    matches: "+m.matches());
        if(m.matches()){
            return url;
        }
        else {
            return url.replaceAll(COMPLETE_URL_EXCLUSION_REGEX,"#");
        }
    }

    private static final String SIMPLE_STRING_INCLUSION_REGEX = "[a-zA-Z0-9_ ]*";
    private static final String SIMPLE_STRING_EXCLUSION_REGEX = "[^a-zA-Z0-9_ ]";
    private static final Pattern SIMPLE_STRING_INCLUSION_PATTERN = Pattern.compile(SIMPLE_STRING_INCLUSION_REGEX);

    public static String simpleString(String s){
        if(s==null)
            return null;
        Matcher m = SIMPLE_STRING_INCLUSION_PATTERN.matcher(s);
        log.debug("simpleString() - Scrubbing simple String: "+s+"   white list pattern: "+ SIMPLE_STRING_INCLUSION_REGEX +"    matches: "+m.matches());
        if(m.matches()){
            return s;
        }
        else {
            return s.replaceAll(SIMPLE_STRING_EXCLUSION_REGEX,"#");
        }
    }

    private static final String INTEGER_STRING_INCLUSION_REGEX = "[0-9]*";
    private static final String INTEGER_STRING_EXCLUSION_REGEX = "[^0-9]";
    private static final Pattern INTEGER_STRING_INCLUSION_PATTERN = Pattern.compile(INTEGER_STRING_INCLUSION_REGEX);

    public static String integerString(String s){
        if(s==null)
            return null;
        Matcher m = INTEGER_STRING_INCLUSION_PATTERN.matcher(s);
        log.debug("integerString() - Scrubbing integer String: "+s+"   white list pattern: "+ INTEGER_STRING_INCLUSION_REGEX +"    matches: "+m.matches());
        if(m.matches()){
            return s;
        }
        else {
            return s.replaceAll(INTEGER_STRING_EXCLUSION_REGEX,"#");
        }
    }

    private static final String SIMPLE_QUERY_STRING_INCLUSION_REGEX = "[a-zA-Z0-9_ =\\.]*";
    private static final String SIMPLE_QUERY_STRING_EXCLUSION_REGEX = "[^a-zA-Z0-9_ =\\.]";
    private static final Pattern SIMPLE_QUERY_STRING_INCLUSION_PATTERN = Pattern.compile(SIMPLE_STRING_INCLUSION_REGEX);

    public static String simpleQueryString(String s){
        if(s==null)
            return null;
        Matcher m = SIMPLE_QUERY_STRING_INCLUSION_PATTERN.matcher(s);
        log.debug("simpleQueryString() - Scrubbing simple query String: "+s+"   white list pattern: "+ SIMPLE_QUERY_STRING_INCLUSION_REGEX +"    matches: "+m.matches());
        if(m.matches()){
            return s;
        }
        else {
            return s.replaceAll(SIMPLE_QUERY_STRING_EXCLUSION_REGEX,"#");
        }
    }





    public static void main(String[] args){
        org.junit.runner.JUnitCore.main("opendap.coreServlet.Scrub");
    }

    @org.junit.Test public void test() {

        checkURLContent("This <> should suck.", false);

        checkURLContent("this IsGood", true);

        checkCompleteURL("http://.../OAK_00168/microstar227926-20080205.nc",
                true);

        checkCompleteURL("http://dods.jpl.nasa.gov/opendap/" +
                "sea_surface_temperature/" +
                "avhrr/pathfinder/data_v4.1/best_sst/monthly/ascending/09km/" +
                "1987/198704h09ma-gdm.hdf.Z.dds",
                true);

        checkCompleteURL("http://dods.jpl.nasa.gov/opendap/ocean_wind/ssmi/" +
                "msfc_pathfinder/level3/data/pentad/contents.html",
                true);

        checkCompleteURL("http://cdaweb.gsfc.nasa.gov/cgi-bin/opendap/" +
                "nph-dods/" +
                "istp_public/data/isis1/ott/1975/184/" +
                "i1_av_ott_1975184014529_v01.cdf.html",
                true);

        checkCompleteURL("http://ingrid.ldeo.columbia.edu/SOURCES/.OORT/" +
                "dods.dds",
                true);

        checkCompleteURL("http://dods.mbari.org/cgi-bin/nph-nc/data/oasis/" +
                "netcdf/" +
                "dailyAveragedM2.nc.html",
                true);

        checkCompleteURL("http://dataportal.ucar.edu:9191/dods/" +
                        "ASSIMtF_COLAMOM3/Tot/" +
                        "current.dds",
                true);

    }

    private static void checkCompleteURL(String s, boolean expected){

        simpleCheck(s,completeURL(s),expected);

    }

    private static void checkURLContent(String s, boolean expected){

        simpleCheck(s,urlContent(s),expected);

    }

    private static void simpleCheck(String s1, String s2, boolean expected){
        boolean value;

        if(expected){
            if(s1 == null)
                value = s2==null;
            else
                value =  s1.equals(s2);

            String msg = value?"PASS (Strings Match):  ":"FAIL (Strings DO NOT Match):  ";
            msg+= "s1=\""+s1+"\";   s2: "+s2;
            assertTrue(msg,value);
        }
        else{
            if(s1 == null)
                value = s2!=null;
            else
                value =  s1.equals(s2);

            String msg = value?"FAIL (Strings Match):  ":"PASS (Strings DO NOT Match):  ";
            msg+= "s1=\""+s1+"\";   s2: "+s2;
            assertFalse(msg,value);
        }

    }

}
