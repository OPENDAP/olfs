/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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


package opendap.olfs;

import opendap.dap.DODSException;
import opendap.ppt.PPTException;
import opendap.util.Debug;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.jdom.JDOMException;
import org.jdom.Element;

/**
 * Default handler for OPeNDAP directory requests. This class is used
 * by OLFS. This code exists as a seperate class in order to alleviate
 * code bloat in the OLFS class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class S4Dir {



    /**
     * ************************************************************************
     * Default handler for OPeNDAP directory requests. Returns an html document
     * with a list of all datasets on this server with links to their
     * DDS, DAS, Information, and HTML responses.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @param rs       The request state object for this client request.
     * @see ReqState
     */
    public static void sendDIR(HttpServletRequest request,
                               HttpServletResponse response,
                               ReqState rs)
            throws DODSException, PPTException, JDOMException, BESException, IOException {

        if (Debug.isSet("showResponse")) System.out.println("sendDIR request = " + request);

        //String ddxCacheDir = rs.getDDXCache();
        //String ddsCacheDir = rs.getDDSCache();

        String name, collectionName;
        String size;
        String lastModified;
        String link;
        String responseLinks;


        Iterator it;
        Element childDataset;

        String requestURL = rs.getRequestURL();


        // clean up the url
        if(requestURL.endsWith("/"))
            requestURL = requestURL.substring(0,requestURL.length()-1);

        PrintWriter pw = new PrintWriter(response.getOutputStream());


        // Make shure the dataset name is not null
        if(rs.getDataset() == null)
            name = "/";
        else
            name = rs.getDataset();


        // Get the catalog for this collection
        Element dataset = BesAPI.showCatalog(name).getRootElement();




        // Compute White Space required for correct formating
        int headerSpace=0;
        it = dataset.getChildren("dataset").iterator();
        while(it.hasNext()){
            childDataset = (Element) it.next();
            name = childDataset.getChildTextTrim("name");
            if(headerSpace < name.length())
                headerSpace = name.length();
        }
        headerSpace += 10;




        // get the name of the collection
        name = dataset.getChildTextTrim("name");

        // Figure out what the link to the parent directory looks like.
//        if (name.endsWith("/"))
//            collectionName = name.substring(0, name.length() - 1);
//        else
            collectionName = name;

        String baseName;
        if(collectionName.endsWith("/"))
                baseName = collectionName.substring(0,collectionName.length()-1);
        else
            baseName = collectionName;

        if(baseName.lastIndexOf("/") > 0)
            baseName = baseName.substring(baseName.lastIndexOf("/"),baseName.length());



        // Strip off the basename to make the link
        link = requestURL.substring(0, requestURL.lastIndexOf(baseName));

        // Set up the page.
        printHTMLHeader(collectionName, headerSpace, link, pw);


        // Build a line in the page for each child dataset/collection
        it = dataset.getChildren("dataset").iterator();
        while(it.hasNext()){

            childDataset = (Element) it.next();

            name = childDataset.getChildTextTrim("name");
            size = childDataset.getChildTextTrim("size");
            lastModified = childDataset.getChild("lastmodified").getChildTextTrim("date") + " " +
                    childDataset.getChild("lastmodified").getChildTextTrim("time");

            // Is it a collection?
            if(childDataset.getAttributeValue("thredds_collection").equalsIgnoreCase("true")){


                link = requestURL+"/"+name+"/";

                responseLinks = "        " +
                        " -  "+
                        " -  "+
                        " -  "+
                        " -   "+
                        " -   ";


                name += "/";
                size = " -";
            }
            else { /// It must be a dataset
                link = requestURL+"/"+name+".html";

                // Build response links

                responseLinks = "      " +
                        "<a href=\"" + requestURL + "/" + name + ".ddx"  + "\">ddx</a> "+
                        "<a href=\"" + requestURL + "/" + name + ".dds"  + "\">dds</a> "+
                        "<a href=\"" + requestURL + "/" + name + ".das"  + "\">das</a> "+
                        "<a href=\"" + requestURL + "/" + name + ".info" + "\">info</a> "+
                        "<a href=\"" + requestURL + "/" + name + ".html" + "\">html</a> ";


                size = computeSizeString(size);
            }

            pw.print("<A HREF=\"");
            pw.print(link);
            pw.print("\">" + name + "</a>" + getWhiteSpacePadding(name,headerSpace));
            pw.print(lastModified);
            pw.print("      " + size);

            pw.print(responseLinks);

            pw.print("\n");

        }

        printHTMLFooter(pw);
        pw.flush();


    }

    private static void printHTMLHeader(String collectionName, int headerSpace, String parentLink, PrintWriter pw) {


        pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
        pw.println("<html>");
        pw.println("    <head>");
        pw.println("        <title>OPeNDAP Server4:  Index of " + collectionName + "</title>");
        pw.println("    </head>");
        pw.println("    <body>");
        pw.println("        <h1>OPeNDAP Server4:  Index of " + collectionName + "</h1>");
        pw.println("        <pre>");

        // original line with images
        //pw.println("<img src=\"/icons/blank.gif\" alt=\"Icon \" /> <A HREF=\"?C=N;O=D\">Name</a>                      <A HREF=\"?C=M;O=A\">Last modified</a>      <A HREF=\"?C=S;O=A\">Size</a>  <A HREF=\"?C=D;O=A\">Description</a><hr /><img src=\"/icons/back.gif\" alt=\"[DIR]\" /> <A HREF=\"http://test.opendap.org/opendap-3.5/nph-dods/data/\">Parent Directory</a>                               -   ");

        // Original line, sans images
        //pw.println("<A HREF=\"?C=N;O=D\">Name</a>"+getWhiteSpacePadding("Name",headerSpace)+"<A HREF=\"?C=M;O=A\">Last modified</a>            <A HREF=\"?C=S;O=A\">Size</a>        <A HREF=\"?C=D;O=A\">Description</a>");

        //No Images, No sorting links.
        pw.println("Name"+getWhiteSpacePadding("Name",headerSpace)+"Last modified            Size        Response Links");
        pw.println("<hr />");
        //pw.println("<img src=\"/icons/back.gif\" alt=\"[DIR]\" /> <A HREF=\"http://test.opendap.org/opendap-3.5/nph-dods/data/\">Parent Directory</a>                               -   ");
        pw.println("<A HREF=\"" + parentLink + "\">Parent Directory</a>"+getWhiteSpacePadding("Parent Directory",headerSpace+26)+"-");


    }

    private static void printHTMLFooter(PrintWriter pw) {
        /*
        <hr /></pre>
        <address>Apache/2.0.46 (Red Hat) Server at test.opendap.org Port 80</address>
        </body></html>
        */
        pw.println("<hr /></pre>");
        //pw.println("<address>Apache/2.0.46 (Red Hat) Server at test.opendap.org Port 80</address>");
        pw.println("</body></html>");

    }

    private static String getWhiteSpacePadding(String stringToPad, int desiredSize){

        String result = "";

        //System.out.println("stringToPad.length(): "+stringToPad.length()+"    desiredSize: "+desiredSize);


        if(stringToPad.length() >= desiredSize)
            return(result);


        for(int i=0; i<(desiredSize - stringToPad.length());i++)
            result += " ";

        //System.out.println("result.length(): "+result.length()+"    desiredSize: "+desiredSize);

        return result;
    }




    private static String computeSizeString(String size){

        int sz = Integer.parseInt(size);
        String result;

        int mag = 0;

        while(sz >= 1024){
            sz /= 1024;
            mag++;
        }
        switch(mag){
            case 0:
                result = sz + " ";
                break;
            case 1:
                result = sz+"K";
                break;
            case 2:
                result = sz+"M";
                break;
            case 3:
                result = sz+"G";
                break;
            case 4:
                result = sz+"P";
                break;
            default:
                result = "Way to big!";
                break;
        }

        result = getWhiteSpacePadding(result,4) +result;
        return result;

    }


}





