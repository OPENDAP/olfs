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
package opendap.xml;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * User: ndp
 * Date: Apr 1, 2009
 * Time: 2:47:01 PM
 */
public class Util {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Util.class);


    public static Element loadXML(String fname)throws IOException, JDOMException{
        File f = new File(fname);
        return getDocumentRoot(f);
    }

    public static Element getDocumentRoot(File f)throws IOException, JDOMException {

        Document cdDoc = getDocument(f);

        Element root = cdDoc.getRootElement();
        root.detach();

        return root;
    }
    public static Document getDocument(File f)throws IOException, JDOMException{

        String msg;


        if(!f.exists()){
            msg = "Cannot find file: "+ f.getAbsoluteFile();
            log.error(msg);
            throw new IOException(msg);
        }

        if(!f.canRead()){
            msg = "Cannot read file: "+ f.getAbsoluteFile();
            log.error(msg);
            throw new IOException(msg);
        }
        if(!f.isFile()){
            msg = "The file " + f.getAbsoluteFile() +" is not actually a file.";
            log.error(msg);
            throw new IOException(msg);
        }

        SAXBuilder sb = new SAXBuilder();
        return sb.build(f);
    }

    public static Document getDocument(InputStream f)throws IOException, JDOMException{

        String msg;
        SAXBuilder sb = new SAXBuilder();
        return sb.build(f);
    }

    public static Element  getDocumentRoot(String docUrlString)throws MalformedURLException, IOException, JDOMException {

        Element docRoot = null;

        Document doc = getDocument(docUrlString);
        if(doc!=null){
            docRoot = doc.getRootElement();
        }
        return docRoot;

    }

    public static Document getDocument(String docUrlString) throws MalformedURLException, IOException, JDOMException {

        Document doc = null;

        SAXBuilder sb = new SAXBuilder();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat() );


        log.debug("Retrieving XML Document: "+docUrlString);

        if(docUrlString.startsWith("http://") || docUrlString.startsWith("https://")){
            URL docUrl = new URL(docUrlString);
            log.debug("Document URL INFO: \n"+getUrlInfo(docUrl));
            doc = sb.build(docUrl);

        }
        else {
            String fname =  docUrlString;
            if (docUrlString.startsWith("file:")){

                fname =  docUrlString.substring(5,docUrlString.length());

                if(fname.startsWith("/")){
                    while(fname.startsWith("/")){
                        fname =  fname.substring(1);
                    }
                    fname = "/" + fname;
                }
            }
            File f = new File(fname);

            doc = getDocument(f);

        }


        log.debug("Loaded XML Document: \n"+xmlo.outputString(doc));

        return doc;

    }


    private static String getUrlInfo(URL url){
        String info = "URL:\n";

        info += "    getHost():         " + url.getHost() + "\n";
        info += "    getAuthority():    " + url.getAuthority() + "\n";
        info += "    getFile():         " + url.getFile() + "\n";
        info += "    getSystemPath():         " + url.getPath() + "\n";
        info += "    getDefaultPort():  " + url.getDefaultPort() + "\n";
        info += "    getPort():         " + url.getPort() + "\n";
        info += "    getProtocol():     " + url.getProtocol() + "\n";
        info += "    getQuery():        " + url.getQuery() + "\n";
        info += "    getRef():          " + url.getRef() + "\n";
        info += "    getUserInfo():     " + url.getUserInfo() + "\n";

        return info;
    }


}
