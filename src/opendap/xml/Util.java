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
package opendap.xml;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

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

        if(docUrlString.startsWith("http://")){
            URL catalogURL = new URL(docUrlString);
            log.debug("Document URL INFO: \n"+getUrlInfo(catalogURL));
            doc = sb.build(catalogURL);

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
