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

import java.util.TreeMap;

/**
 * Maps file extensions (suffixes) to commonly known MIME types.
 * User: ndp
 * Date: Oct 5, 2006
 * Time: 4:28:08 PM
 */
public class MimeTypes {

    private static TreeMap<String, String> typeMap = new TreeMap<String, String>();


    /**
     * MIME Type to file extension mapping taken from: http://www.huw.id.au/code/fileTypeIDs.html
     */
    static {

        // Plain Text
        typeMap.put("txt", "text/plain");
        typeMap.put("text","text/plain");

        // Human Readable Rich Text
        typeMap.put("rtf",   "text/rtf");

        typeMap.put("html",  "text/html");
        typeMap.put("htm",   "text/html");
        typeMap.put("xhtml", "text/xhtml");
        typeMap.put("xhtm",  "text/xhtml");
        typeMap.put("shtml", "text/xhtml");
        typeMap.put("shtm",  "text/xhtml");

        typeMap.put("xml",   "text/xml");
        typeMap.put("jsp",   "text/jsp");
        typeMap.put("css",   "text/css");


        // Images
        typeMap.put("jpeg",  "image/jpeg");
        typeMap.put("jpg",   "image/jpeg");
        typeMap.put("jfif",  "image/jpeg");
        typeMap.put("jpe",   "image/jpeg");

        typeMap.put("gif",   "image/gif");

        typeMap.put("png",   "image/png");

        typeMap.put("tiff",  "image/tiff");
        typeMap.put("tif",   "image/tiff");

        typeMap.put("bmp",   "image/bmp");

        typeMap.put("psd",   "image/psd");


        // Video
        typeMap.put("mov",   "video/quicktime");
        typeMap.put("qt",    "video/quicktime");

        typeMap.put("avi",   "video/avi");

        typeMap.put("mpeg",  "video/mpeg");
        typeMap.put("mpg",   "video/mpeg");
        typeMap.put("mpe",   "video/mpeg");

        typeMap.put("mp4",   "video/mp4");
        typeMap.put("m4v",   "video/mp4v");


        // Audio
        typeMap.put("mp3",   "audio/mp3");

        typeMap.put("m4a",   "audio/mp4");
        typeMap.put("aac",   "audio/mp4");

        typeMap.put("aiff",  "audio/x-aiff");
        typeMap.put("aif",   "audio/x-aiff");
        typeMap.put("aifc",  "audio/x-aiff");

        typeMap.put("wav",  "audio/wave");

        typeMap.put("midi",  "audio/midi");
        typeMap.put("mid",   "audio/midi");


        // PDF and Postscript
        typeMap.put("pdf",   "application/pdf");
        typeMap.put("ps",    "application/postscript");
        typeMap.put("eps",   "application/postscript");
        typeMap.put("ai",    "application/illustrator");


        // Windows Media
        typeMap.put("asx",    "video/x-ms-asf");
        typeMap.put("asf",    "video/x-ms-asf");
        typeMap.put("wma",    "video/x-ms-wma");
        typeMap.put("wax",    "video/x-ms-wax");
        typeMap.put("wmv",    "video/x-ms-wmv");
        typeMap.put("wvx",    "video/x-ms-wvx");
        typeMap.put("wm",     "video/x-ms-wm");
        typeMap.put("wmx",    "video/x-ms-wmx");
        typeMap.put("wmz",    "video/x-ms-wmz");
        typeMap.put("wmd",    "video/x-ms-wmd");


        // Microsoft Office
        typeMap.put("doc",   "application/msword");
        typeMap.put("xls",   "application/vnd.ms-excel");
        typeMap.put("ppt",   "application/vnd.ms-powerpoint");


        // OpenDocument
        typeMap.put("odb",   "application/vnd.oasis.opendocument.database");
        typeMap.put("odc",   "application/vnd.oasis.opendocument.chart");
        typeMap.put("odf",   "application/vnd.oasis.opendocument.formula");
        typeMap.put("odg",   "application/vnd.oasis.opendocument.graphics");
        typeMap.put("otg",   "application/vnd.oasis.opendocument.graphics-template");

        typeMap.put("odi",   "application/vnd.oasis.opendocument.image");
        typeMap.put("odm",   "application/vnd.oasis.opendocument.text-master");
        typeMap.put("odp",   "application/vnd.oasis.opendocument.presentation");
        typeMap.put("otp",   "application/vnd.oasis.opendocument.presentation-template");

        typeMap.put("ods",   "application/vnd.oasis.opendocument.spreadsheet");
        typeMap.put("ots",   "application/vnd.oasis.opendocument.spreadsheet-template");

        typeMap.put("odt",   "application/vnd.oasis.opendocument.text");
        typeMap.put("ott",   "application/vnd.oasis.opendocument.text-template");
        typeMap.put("oth",   "application/vnd.oasis.opendocument.text-web");


        // StarOffice/OpenOffice.org/NeoOffice
        typeMap.put("sxc",   "application/vnd.sun.xml.calc");
        typeMap.put("stc",   "application/vnd.sun.xml.calc-template");

        typeMap.put("sxd",   "application/vnd.sun.xml.draw");
        typeMap.put("std",   "application/vnd.sun.xml.draw-template");

        typeMap.put("sxg",   "application/vnd.sun.xml.writer.global");

        typeMap.put("sxi",   "application/vnd.sun.xml.impress");
        typeMap.put("sti",   "application/vnd.sun.xml.impress-template");

        typeMap.put("sxm",   "application/vnd.sun.xml.math");

        typeMap.put("sxw",   "application/vnd.sun.xml.writer");
        typeMap.put("stw",   "application/vnd.sun.xml.writer-template");


        // Java
        typeMap.put("class", "application/java");
        typeMap.put("java",  "application/x-java-source");
        typeMap.put("jar",   "application/java");


        // Miscellaneous

        typeMap.put("torrent", "application/x-bittorrent");
        typeMap.put("m3u",     "audio/mpegurl");
        typeMap.put("pls",     "audio/scpls");
        typeMap.put("djvu",    "image/x-djvu");
        typeMap.put("djv",     "image/x-djvu");
        typeMap.put("vcf",     "text/x-vcard");
        typeMap.put("vcs",     "text/x-vcalander");
        typeMap.put("ics",     "text/calander");


        // OPeNDAP DAP2

        typeMap.put("das",     "text/x-dods_das");
        typeMap.put("dds",     "text/x-dods_dds");
        typeMap.put("dods",    "application/octet-stream");
        typeMap.put("xdods",   "text/xml");


        typeMap.put("jnlp",    "application/x-java-jnlp-file");


        typeMap.put("hdf",     "application/x-hdf");
        typeMap.put("hdf4",    "application/x-hdf");

        typeMap.put("h5",      "application/x-hdf5");
        typeMap.put("hdf5",    "application/x-hdf5");

        typeMap.put("nc",      "application/x-netcdf");






    }

    /**
     *
     * @param fileExtension The file type extension (suffix) to be mapped to a MIME type
     * @return The MIME type associated with the passed file extension. Null if no mapping can be found.
     */
    public static String getMimeType(String fileExtension){
        return typeMap.get(fileExtension.toLowerCase());
    }


    public static String getMimeTypeFromFileName(String fileName){

        int index = fileName.lastIndexOf(".");
        String mimeType = null;

        if(index!=-1){
            String extension = fileName.substring(index+1,fileName.length());
            mimeType =  getMimeType(extension);

        }
        //System.out.println("MIME Type: "+mimeType);

        return mimeType==null?"unknown":mimeType;


    }





}
