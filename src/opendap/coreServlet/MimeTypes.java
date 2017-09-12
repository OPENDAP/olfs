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

import opendap.bes.dap4Responders.MediaType;

import java.util.TreeMap;

/**
 * Maps file extensions (suffixes) to commonly known MIME types.
 * User: ndp
 * Date: Oct 5, 2006
 * Time: 4:28:08 PM
 */
public class MimeTypes {

    private static TreeMap<String, String[]> typeMap = new TreeMap<>();


    /**
     * MIME Type to file extension mapping taken from: http://www.huw.id.au/code/fileTypeIDs.html
     */
    static {

        // Plain Text
        typeMap.put("txt",   new String[] {"text", "plain"});
        typeMap.put("text",  new String[] {"text", "plain"});

        // Human Readable Rich Text
        typeMap.put("rtf",   new String[] {"text", "rtf"});

        typeMap.put("html",  new String[] {"text", "html"});
        typeMap.put("htm",   new String[] {"text", "html"});
        typeMap.put("xhtml", new String[] {"text", "xhtml"});
        typeMap.put("xhtm",  new String[] {"text", "xhtml"});
        typeMap.put("shtml", new String[] {"text", "xhtml"});
        typeMap.put("shtm",  new String[] {"text", "xhtml"});

        typeMap.put("xml",   new String[] {"text", "xml"});
        typeMap.put("jsp",   new String[] {"text", "jsp"});
        typeMap.put("css",   new String[] {"text", "css"});


        // Images
        typeMap.put("jpeg",  new String[] {"image", "jpeg"});
        typeMap.put("jpg",   new String[] {"image", "jpeg"});
        typeMap.put("jfif",  new String[] {"image", "jpeg"});
        typeMap.put("jpe",   new String[] {"image", "jpeg"});
        typeMap.put("jp2",   new String[] {"image", "jp2"});

        typeMap.put("gif",   new String[] {"image", "gif"});

        typeMap.put("png",   new String[] {"image", "png"});

        typeMap.put("tiff",  new String[] {"image", "tiff"});
        typeMap.put("tif",   new String[] {"image", "tiff"});

        typeMap.put("bmp",   new String[] {"image", "bmp"});

        typeMap.put("psd",   new String[] {"image", "psd"});


        // Video
        typeMap.put("mov",   new String[] {"video", "quicktime"});
        typeMap.put("qt",    new String[] {"video", "quicktime"});

        typeMap.put("avi",   new String[] {"video", "avi"});

        typeMap.put("mpeg",  new String[] {"video", "mpeg"});
        typeMap.put("mpg",   new String[] {"video", "mpeg"});
        typeMap.put("mpe",   new String[] {"video", "mpeg"});

        typeMap.put("mp4",   new String[] {"video", "mp4"});
        typeMap.put("m4v",   new String[] {"video", "mp4v"});


        // Audio
        typeMap.put("mp3",   new String[] {"audio", "mp3"});

        typeMap.put("m4a",   new String[] {"audio", "mp4"});
        typeMap.put("aac",   new String[] {"audio", "mp4"});

        typeMap.put("aiff",  new String[] {"audio", "x-aiff"});
        typeMap.put("aif",   new String[] {"audio", "x-aiff"});
        typeMap.put("aifc",  new String[] {"audio", "x-aiff"});

        typeMap.put("wav",   new String[] {"audio", "wave"});

        typeMap.put("midi",  new String[] {"audio", "midi"});
        typeMap.put("mid",   new String[] {"audio", "midi"});


        // PDF and Postscript
        typeMap.put("pdf",   new String[] {"application", "pdf"});
        typeMap.put("ps",    new String[] {"application", "postscript"});
        typeMap.put("eps",   new String[] {"application", "postscript"});
        typeMap.put("ai",    new String[] {"application", "illustrator"});


        // Windows Media
        typeMap.put("asx",    new String[] {"video", "x-ms-asf"});
        typeMap.put("asf",    new String[] {"video", "x-ms-asf"});
        typeMap.put("wma",    new String[] {"video", "x-ms-wma"});
        typeMap.put("wax",    new String[] {"video", "x-ms-wax"});
        typeMap.put("wmv",    new String[] {"video", "x-ms-wmv"});
        typeMap.put("wvx",    new String[] {"video", "x-ms-wvx"});
        typeMap.put("wm",     new String[] {"video", "x-ms-wm"});
        typeMap.put("wmx",    new String[] {"video", "x-ms-wmx"});
        typeMap.put("wmz",    new String[] {"video", "x-ms-wmz"});
        typeMap.put("wmd",    new String[] {"video", "x-ms-wmd"});


        // Microsoft Office
        typeMap.put("doc",   new String[] {"application", "msword"});
        typeMap.put("xls",   new String[] {"application", "vnd.ms-excel"});
        typeMap.put("ppt",   new String[] {"application", "vnd.ms-powerpoint"});


        // OpenDocument
        typeMap.put("odb",   new String[] {"application", "vnd.oasis.opendocument.database"});
        typeMap.put("odc",   new String[] {"application", "vnd.oasis.opendocument.chart"});
        typeMap.put("odf",   new String[] {"application", "vnd.oasis.opendocument.formula"});
        typeMap.put("odg",   new String[] {"application", "vnd.oasis.opendocument.graphics"});
        typeMap.put("otg",   new String[] {"application", "vnd.oasis.opendocument.graphics-template"});

        typeMap.put("odi",   new String[] {"application", "vnd.oasis.opendocument.image"});
        typeMap.put("odm",   new String[] {"application", "vnd.oasis.opendocument.text-master"});
        typeMap.put("odp",   new String[] {"application", "vnd.oasis.opendocument.presentation"});
        typeMap.put("otp",   new String[] {"application", "vnd.oasis.opendocument.presentation-template"});

        typeMap.put("ods",   new String[] {"application", "vnd.oasis.opendocument.spreadsheet"});
        typeMap.put("ots",   new String[] {"application", "vnd.oasis.opendocument.spreadsheet-template"});

        typeMap.put("odt",   new String[] {"application", "vnd.oasis.opendocument.text"});
        typeMap.put("ott",   new String[] {"application", "vnd.oasis.opendocument.text-template"});
        typeMap.put("oth",   new String[] {"application", "vnd.oasis.opendocument.text-web"});


        // StarOffice/OpenOffice.org/NeoOffice
        typeMap.put("sxc",   new String[] {"application", "vnd.sun.xml.calc"});
        typeMap.put("stc",   new String[] {"application", "vnd.sun.xml.calc-template"});

        typeMap.put("sxd",   new String[] {"application", "vnd.sun.xml.draw"});
        typeMap.put("std",   new String[] {"application", "vnd.sun.xml.draw-template"});

        typeMap.put("sxg",   new String[] {"application", "vnd.sun.xml.writer.global"});

        typeMap.put("sxi",   new String[] {"application", "vnd.sun.xml.impress"});
        typeMap.put("sti",   new String[] {"application", "vnd.sun.xml.impress-template"});

        typeMap.put("sxm",   new String[] {"application", "vnd.sun.xml.math"});

        typeMap.put("sxw",   new String[] {"application", "vnd.sun.xml.writer"});
        typeMap.put("stw",   new String[] {"application", "vnd.sun.xml.writer-template"});


        // Java
        typeMap.put("class", new String[] {"application", "java"});
        typeMap.put("java",  new String[] {"application", "x-java-source"});
        typeMap.put("jar",   new String[] {"application", "java"});


        // Miscellaneous

        typeMap.put("torrent", new String[] {"application", "x-bittorrent"});
        typeMap.put("m3u",     new String[] {"audio", "mpegurl"});
        typeMap.put("pls",     new String[] {"audio", "scpls"});
        typeMap.put("djvu",    new String[] {"image", "x-djvu"});
        typeMap.put("djv",     new String[] {"image", "x-djvu"});
        typeMap.put("vcf",     new String[] {"text", "x-vcard"});
        typeMap.put("vcs",     new String[] {"text", "x-vcalander"});
        typeMap.put("ics",     new String[] {"text", "calander"});


        // OPeNDAP DAP2

        typeMap.put("das",     new String[] {"text", "x-dods_das"});
        typeMap.put("dds",     new String[] {"text", "x-dods_dds"});
        typeMap.put("dods",    new String[] {"application","octet-stream"});
        typeMap.put("xdods",   new String[] {"text","xml"});


        typeMap.put("jnlp",    new String[] {"application","x-java-jnlp-file"});


        typeMap.put("hdf",     new String[] {"application","x-hdf"});
        typeMap.put("hdf4",    new String[] {"application","x-hdf"});

        typeMap.put("h5",      new String[] {"application","x-hdf5"});
        typeMap.put("hdf5",    new String[] {"application","x-hdf5"});

        typeMap.put("nc",      new String[] {"application","x-netcdf"});
        typeMap.put("nc4",     new String[] {"application","x-netcdf;ver=4"});

        typeMap.put("ncml",      new String[] {"text","xml"});



    }

    /**
     *
     * @param fileExtension The file type extension (suffix) to be mapped to a MIME type
     * @return The MIME type associated with the passed file extension. Null if no mapping can be found.
     */
    public static String getMimeType(String fileExtension){
        String mimeTypeString = null;
        String type[] =  typeMap.get(fileExtension.toLowerCase());
        if(type!=null){
            mimeTypeString = type[0] + "/" + type[1] ;

        }
        return mimeTypeString;
    }

    public static MediaType getMediaType(String fileExtension){

        MediaType mediaType = null;
        String type[] =  typeMap.get(fileExtension.toLowerCase());
        if(type!=null){
            mediaType = new MediaType(type[0], type[1], fileExtension);
        }

         return mediaType;
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
