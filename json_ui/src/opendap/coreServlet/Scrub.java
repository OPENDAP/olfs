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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Aug 14, 2007
 * Time: 3:28:03 PM
 */
public class Scrub {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(Scrub.class);



    // The C++ coe uses this regex: Regex name("[-0-9A-z_./]+");

    private static String fileNameInclusionRegex = "[-a-zA-Z0-9/.%_ :]*";
    private static String fileNameExclusionRegex = "[^-a-zA-Z0-9/.%_ :]";
    private static Pattern fileNameInclusionPattern = Pattern.compile(fileNameInclusionRegex);

    public static String fileName(String fileName){
        if(fileName==null)
            return null;
        Matcher m = fileNameInclusionPattern.matcher(fileName);

        log.debug("URL() - Scrubbing file Name: "+fileName+"   white list pattern: "+ fileNameInclusionRegex +"    matches: "+m.matches());
        if(m.matches()){
            return fileName;
        }
        else {
            return fileName.replaceAll(fileNameExclusionRegex,"#");
        }

    }




    /*

    // These two regex are missing the "[" and the "]" charcaters. I don't know
    // to make them work with those included in the character class.
    private static String  uriInclusionRegex   = "[-a-zA-Z0-9/:?#@!$&'()*+,;=]*";
    private static String  uriExclusionRegex   = "[^-a-zA-Z0-9/:?#@!$&'()*+,;=]*";
    private static Pattern uriInclusionPattern = Pattern.compile(uriInclusionRegex);
    public static String URI(String uri){
        if(uri==null)
            return null;
        Matcher m = uriInclusionPattern.matcher(uri);

        log.debug("URL() - Scrubbing file Name: "+uri+"   white list pattern: "+ uriInclusionRegex +"    matches: "+m.matches());
        if(m.matches()){
            return uri;
        }
        else {
            return uri.replaceAll(uriExclusionRegex,"#");
        }

    }

            
     */




    private static String htmlContentInclusionRegex = "[-a-zA-Z0-9/.%_ ]*";
    private static String htmlContentExclusionRegex = "[^-a-zA-Z0-9/.%_ ]";
    private static Pattern htmlContentInclusionPattern = Pattern.compile(htmlContentInclusionRegex);

    public static String urlContent(String urlContent){

        if(urlContent==null)
            return null;

        Matcher m = htmlContentInclusionPattern.matcher(urlContent);

        log.debug("URL() - Scrubbing URL Content: "+urlContent+"   white list pattern: "+ htmlContentInclusionRegex +"    matches: "+m.matches());



        if(m.matches()){
            return urlContent;
        }
        else {
            return urlContent.replaceAll(htmlContentExclusionRegex,"#");
        }


    }



    private static String completeURLInclusionRegex = "http://"+ htmlContentInclusionRegex;
    private static String completeURLExclusionRegex = "http://"+ htmlContentExclusionRegex;
    private static Pattern completeURLInclusionPattern = Pattern.compile(completeURLInclusionRegex);

    public static String completeURL(String url){
        if(url==null)
            return null;
        Matcher m = completeURLInclusionPattern.matcher(url);
        log.debug("URL() - Scrubbing URL: "+url+"   white list pattern: "+ completeURLInclusionRegex +"    matches: "+m.matches());
        if(m.matches()){
            return url;
        }
        else {
            return url.replaceAll(completeURLExclusionRegex,"#");
        }
    }






    private static String simpleStringInclusionRegex = "[a-zA-Z0-9_ ]*";
    private static String simpleStringExclusionRegex = "[^a-zA-Z0-9_ ]";
    private static Pattern simpleStringInclusionPattern = Pattern.compile(simpleStringInclusionRegex);

    public static String simpleString(String s){
        if(s==null)
            return null;
        Matcher m = simpleStringInclusionPattern.matcher(s);
        log.debug("URL() - Scrubbing String: "+s+"   white list pattern: "+ simpleStringInclusionRegex +"    matches: "+m.matches());
        if(m.matches()){
            return s;
        }
        else {
            return s.replaceAll(simpleStringExclusionRegex,"#");
        }
    }





    private static String integerStringInclusionRegex = "[0-9]*";
    private static String integerStringExclusionRegex = "[^0-9]";
    private static Pattern integerStringInclusionPattern = Pattern.compile(integerStringInclusionRegex);

    public static String integerString(String s){
        if(s==null)
            return null;
        Matcher m = integerStringInclusionPattern.matcher(s);
        log.debug("URL() - Scrubbing String: "+s+"   white list pattern: "+ integerStringInclusionRegex +"    matches: "+m.matches());
        if(m.matches()){
            return s;
        }
        else {
            return s.replaceAll(integerStringExclusionRegex,"#");
        }
    }









    private static String simpleQueryStringInclusionRegex = "[a-zA-Z0-9_ =\\.]*";
    private static String simpleQueryStringExclusionRegex = "[^a-zA-Z0-9_ =\\.]";
    private static Pattern simpleQueryStringInclusionPattern = Pattern.compile(simpleStringInclusionRegex);

    public static String simpleQueryString(String s){
        if(s==null)
            return null;
        Matcher m = simpleQueryStringInclusionPattern.matcher(s);
        log.debug("URL() - Scrubbing String: "+s+"   white list pattern: "+ simpleQueryStringInclusionRegex +"    matches: "+m.matches());
        if(m.matches()){
            return s;
        }
        else {
            return s.replaceAll(simpleQueryStringExclusionRegex,"#");
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

        boolean value =  s1.equals(s2);

        if(expected){
            String msg = value?"PASS (Strings Match):  ":"FAIL (Strings DO NOT Match):  ";
            msg+= "s1=\""+s1+"\";   s2: "+s2;
            //System.out.println(msg);
            assertTrue(msg,value);
        }
        else{
            String msg = value?"FAIL (Strings Match):  ":"PASS (Strings DO NOT Match):  ";
            msg+= "s1=\""+s1+"\";   s2: "+s2;
            //System.out.println(msg);
            assertFalse(msg,value);
        }



    }












}
