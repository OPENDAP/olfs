/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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

package opendap;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

/**
 * Created by ndp on 4/21/15.
 */
public class PathBuilder  {

    private static final String DEFAULT_SYSTEM_SEPARATOR = FileSystems.getDefault().getSeparator();


    private StringBuilder sb;

    public PathBuilder(){
        sb = new StringBuilder();
    }

    public PathBuilder(String s){
        this();
        sb.append(s);
    }

    public PathBuilder(CharSequence cs){
        this();
        sb.append(cs);
    }

    public PathBuilder pathAppend(String s){
        if(s==null || s.length()==0)
            return this;

        while (sb.length()!=0 && s.length() > 0 && (s.startsWith("/") )) {
            s = s.substring(1);
        }
        if (sb.length()==0 || (sb.lastIndexOf("/") == sb.length()-1)) {
            sb.append(s);
        } else {
            sb.append("/").append(s);
        }
        return this;
    }


    public static String pathConcat(String path1, String path2){
        String result;
        if(path1==null || path1.length()==0) {
            result = path2;

        }
        else if(path2==null || path2.length()==0){
            result = path1;
        }
        else {
            while (path2.startsWith("/")) {
                path2 = path2.substring(1);
            }
            if (path1.endsWith("/")) {
                result = path1 + path2;
            } else {
                result = path1 + "/" + path2;
            }
        }
        return result;
    }



    public static String basename(String path){
        String name = path;
        int lastIndexOfSlash = path.lastIndexOf('/');
        if(lastIndexOfSlash > 0) {
            name = path.substring(lastIndexOfSlash);
        }
        while (name.startsWith("/") && name.length() > 1) {
            name = name.substring(1);
        }
        return name;
    }


    public static String normalizePath(String rawPath, boolean leadingSeparator, boolean trailingSeparator) {
        return normalizePath(rawPath, leadingSeparator, trailingSeparator, "/");
    }

    public static String normalizePath(String rawPath, boolean leadingSeparator, boolean trailingSeparator, String separator) {

        if(separator.length()>1)
            throw new IllegalArgumentException("The path separator '"+separator+
                    "' string may only have a single character, not "+separator.length()+".");

        String doubleSeparator = separator+separator;
        String path = rawPath.replace(doubleSeparator,separator);

        if(path.isEmpty())
            path = separator;

        if(path.equals(separator))
            return path;

        if(leadingSeparator){
            if(!path.startsWith(separator)) {
                path = separator + path;
            }
        }
        else {
            if(path.startsWith(separator))
                path = path.substring(1);
        }

        if(trailingSeparator){
            if(!path.endsWith(separator)) {
                path += separator;
            }
        }
        else {
            if(path.endsWith(separator))
                path =  path.substring(0,path.length()-1);
        }

        return path;
    }



    public PathBuilder append(String s){
        sb.append(s);
        return this;
    }

    @Override
    public String toString(){
        return sb.toString();
    }



    public static void main(String[] args) throws URISyntaxException, MalformedURLException {

        Logger log = LoggerFactory.getLogger("MAIN");
        String urlString = "http://test.opendap.org/";
        String getMsg = "Paths.get(more): {}";
        String uriMsg = "Path.toUri(): {}";
        String absoMsg = "Path.isAbsolute(): {}";
        String rootMsg = "Paths.get(\"{}\").getRoot(): {}";

        URL url = new URL(urlString);

        log.info("URL: {}",url);
        log.info("URI: {}",url.toURI());

        Path path = Paths.get("/opendap/","/data/","nc","fnoc1.nc");
        log.info(getMsg,path);
        log.info(uriMsg,path.toUri());
        log.info(absoMsg,path.isAbsolute());
        url = new URL(url,path.toString());
        log.info("new URL(url,path.toString()): {}",url);


        String s = "this/is///a//bogus////path//to/normalize";
        path = Paths.get(s);
        log.info(getMsg,path);
        log.info(uriMsg,path.toUri());
        log.info(absoMsg,path.isAbsolute());

        s = "this/is/\\//a//bogus\\//\\///\\/\\/path//to/normalize";
        path = Paths.get(s);
        log.info(getMsg,path);
        log.info(uriMsg,path.toUri());
        log.info(absoMsg,path.isAbsolute());

        s = "/etc/olfs/olfs.xml";
        path = Paths.get(s);
        log.info(getMsg,path);
        log.info(uriMsg,path.toUri());
        log.info(absoMsg,path.isAbsolute());

        log.info("DEFAULT_SYSTEM_SEPARATOR: {}", DEFAULT_SYSTEM_SEPARATOR);

        s = "/etc/olfs/olfs.xml";
        log.info(rootMsg,s, Paths.get(s).getRoot());

        s = "etc/olfs/olfs.xml";
        log.info(rootMsg,s, Paths.get(s).getRoot());

        for(FileSystemProvider fsp: FileSystemProvider.installedProviders()){
            log.info("FileSystemProvider.getScheme(): {}",fsp.getScheme());
        }

    }



}
