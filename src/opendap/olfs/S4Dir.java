/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

import opendap.coreServlet.Debug;
import opendap.coreServlet.ReqInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Iterator;

import org.jdom.Element;

/**
 * Handler for OPeNDAP directory requests. This class is used
 * by OLFS.
 *
 * @author Nathan David Potter
 */
public class S4Dir {


    /**
     * ************************************************************************
     * Handler for OPeNDAP directory requests. Returns an html document
     * with a list of all datasets on this server with links to their
     * DDS, DAS, Information, and HTML responses. Talks to the BES to get the
     * information.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @see opendap.coreServlet.ReqInfo
     */
    public static void sendDIR(HttpServletRequest request,
                               HttpServletResponse response)
            throws Exception {


        if (Debug.isSet("showResponse"))
            System.out.println("sendDIR request = " + request);

        String name;
        String size;
        String lastModified;
        String link;
        String responseLinks;
        PrintWriter pw = new PrintWriter(response.getOutputStream());


        Iterator it;
        Element childDataset;

        String collectionName  = ReqInfo.getCollectionName(request);
        String targetURL = request.getContextPath() + request.getServletPath() + collectionName;


        if(Debug.isSet("S4Dir")) System.out.println("S4Dir - targetURL:                  "+targetURL);
        if(Debug.isSet("S4Dir")) System.out.println("S4Dir - collectionName:        "+collectionName);


        boolean isTopLevel = collectionName.equals("/");




        // Get the catalog for this collection
        Element dataset = BesAPI.showCatalog(collectionName).getRootElement();

        // Compute White Space required for correct formating
        int headerSpace = 0;
        it = dataset.getChildren("dataset").iterator();
        while (it.hasNext()) {
            childDataset = (Element) it.next();
            name = childDataset.getChildTextTrim("name");
            if (headerSpace < name.length())
                headerSpace = name.length();
        }
        headerSpace += 10;


        String baseName; // The last item on the Collection path/name thingy

        // Strip off a trailing / from the basename.
        if (collectionName.endsWith("/"))
            baseName = collectionName.substring(0, collectionName.length() - 1);
        else
            baseName = collectionName;

        // Strip off any prefixed path (made of "/"'s)
        if (baseName.lastIndexOf("/") > 0)
            baseName = baseName.substring(baseName.lastIndexOf("/"), baseName.length());

        if(Debug.isSet("S4Dir")) System.out.println("S4Dir - baseName: "+baseName);


        // Strip basename from the end of the targetURL to make the link to the parent directory
        link = targetURL.substring(0, targetURL.lastIndexOf(baseName))+"/contents.html";

        // Set up the page.
        printHTMLHeader(collectionName, headerSpace, link, pw);




        // Build a line in the page for each child dataset/collection
        it = dataset.getChildren("dataset").iterator();
        while (it.hasNext()) {

            childDataset = (Element) it.next();

            boolean isData = childDataset.getAttributeValue("isData").equalsIgnoreCase("true");

            name = childDataset.getChildTextTrim("name");
            size = childDataset.getChildTextTrim("size");
            lastModified = childDataset.getChild("lastmodified").getChildTextTrim("date") + " " +
                    childDataset.getChild("lastmodified").getChildTextTrim("time");

            // Is it a collection?
            if (childDataset.getAttributeValue("thredds_collection").equalsIgnoreCase("true")) {


                link = targetURL +  name + "/contents.html";

                responseLinks = "        " +
                        " -  " +
                        " -  " +
                        " -  " +
                        " -   " +
                        " -   ";


                name += "/";
                size = "  -";
            } else { /// It must be a dataset

                if(isData)
                    link = targetURL + name + ".html";
                else
                    link = request.getContextPath()+ "/file" + collectionName + name ;

                // Build response links

                responseLinks = "      " +
                        "<a href=\"" + targetURL + name + ".ddx" + "\">ddx</a> " +
                        "<a href=\"" + targetURL + name + ".dds" + "\">dds</a> " +
                        "<a href=\"" + targetURL + name + ".das" + "\">das</a> " +
                        "<a href=\"" + targetURL + name + ".info" + "\">info</a> " +
                        "<a href=\"" + targetURL + name + ".html" + "\">html</a> ";


                size = computeSizeString(size);
            }

            pw.print("<A HREF=\"");
            pw.print(link);
            pw.print("\">" + name + "</a>" + getWhiteSpacePadding(name, headerSpace));
            pw.print(lastModified);
            pw.print("      " + size);

            pw.print(responseLinks);

            pw.print("\n");

        }

        printHTMLFooter(pw,targetURL,isTopLevel);
        pw.flush();


    }




    private static void printHTMLHeader(String collectionName, int headerSpace, String parentLink, PrintWriter pw) {


        pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                   "<html xmlns=\"http://www.w3.org/1999/xhtml\">" );
        pw.println("    <head>");
        pw.println("        <title>OPeNDAP Server4:  Contents of " + collectionName + "</title>");
        pw.println("<STYLE>\n" +
                    "<!--\n" +
                    "H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#527CC1;font-size:22px;}\n" +
                    "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#527CC1;font-size:16px;}\n" +
                    "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#527CC1;font-size:14px;}\n" +
                    "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;}\n" +
                    "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#527CC1;}\n" +
                    "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}\n" +
                    "A {color : black;}\n" +
                    "A.name {color : black;}\n" +
                    "HR {color : #525D76;}\n" +
                    "-->\n" +
                    "</STYLE>\n" +
                    "<style type=\"text/css\">" +
                    "<!--\n" +
                    ".uuid {font-size: 9px;color:#4A74B9}\n" +
                    "-->\n" +
                    "</style>" +
                    "<style type=\"text/css\">" +
                    "<!--\n" +
                    ".small {font-size: 10px;}\n" +
                    "-->\n" +
                    "</style>"


        );

        pw.println("    </head>");
        pw.println("    <body>");
        pw.println("        <img src='/opendap/docs/images/logo.gif' />");

        pw.println("        <h1>Contents of " + collectionName + "</h1>");
        pw.println("        <hr size=\"1\" noshade=\"noshade\">");
        pw.println("        <pre>");

        // original line with images
        //pw.println("<img src=\"/icons/blank.gif\" alt=\"Icon \" /> <A HREF=\"?C=N;O=D\">Name</a>                      <A HREF=\"?C=M;O=A\">Last modified</a>      <A HREF=\"?C=S;O=A\">Size</a>  <A HREF=\"?C=D;O=A\">Description</a><hr /><img src=\"/icons/back.gif\" alt=\"[DIR]\" /> <A HREF=\"http://experiments.opendap.org/opendap-3.5/nph-dods/data/\">Parent Directory</a>                               -   ");

        // Original line, sans images
        //pw.println("<A HREF=\"?C=N;O=D\">Name</a>"+getWhiteSpacePadding("Name",headerSpace)+"<A HREF=\"?C=M;O=A\">Last modified</a>            <A HREF=\"?C=S;O=A\">Size</a>        <A HREF=\"?C=D;O=A\">Description</a>");

        //No Images, No sorting links.
        pw.println("Name" + getWhiteSpacePadding("Name", headerSpace) + "Last modified             Size        Response Links");
        pw.println("<hr size=\"1\" noshade=\"noshade\">");
        //pw.println("<img src=\"/icons/back.gif\" alt=\"[DIR]\" /> <A HREF=\"http://experiments.opendap.org/opendap-3.5/nph-dods/data/\">Parent Directory</a>                               -   ");

        if (!collectionName.equals("/"))
            pw.println("<A HREF=\"" + parentLink + "\">Parent Directory</a>" + getWhiteSpacePadding("Parent Directory", headerSpace + 27) + "-");


    }

    private static void printHTMLFooter(PrintWriter pw, String targetURL, boolean isTopLevel) {
        pw.println("        </pre> ");
        pw.println("        <hr size=\"1\" noshade=\"noshade\">");
        pw.println("        <span class=\"small\">THREDDS Catalog " +
                "<a href='"+targetURL+"catalog.html'>HTML</a> " +
                "<a href='"+targetURL+"catalog.xml'>XML</a></span>\n");
        pw.println("        <h3>" +
                   "            OPeNDAP Server4 ("+Version.getVersionString()+")");

        if(isTopLevel)
            pw.println("            <span class=\"uuid\">ServerUUID="+Version.getServerUUID()+"-contents</span>\n");


        pw.println("            <br />\n" +
                   "            <a href='/opendap/docs/'> Documentation</a>\n");


        pw.println("        </h3>\n");

        pw.println("</body></html>");

    }

    private static String getWhiteSpacePadding(String stringToPad, int desiredSize) {

        String result = "";

        //System.out.println("stringToPad.length(): "+stringToPad.length()+"    desiredSize: "+desiredSize);


        if (stringToPad.length() >= desiredSize)
            return (result);


        for (int i = 0; i < (desiredSize - stringToPad.length()); i++)
            result += " ";

        //System.out.println("result.length(): "+result.length()+"    desiredSize: "+desiredSize);

        return result;
    }


    private static String computeSizeString(String size) {

        int sz = Integer.parseInt(size);
        String result;

        int mag = 0;

        while (sz >= 1024) {
            sz /= 1024;
            mag++;
        }
        switch (mag) {
            case 0:
                result = sz + " ";
                break;
            case 1:
                result = sz + "K";
                break;
            case 2:
                result = sz + "M";
                break;
            case 3:
                result = sz + "G";
                break;
            case 4:
                result = sz + "P";
                break;
            default:
                result = "Way to big!";
                break;
        }

        result = getWhiteSpacePadding(result, 5) + result;
        return result;

    }


}





