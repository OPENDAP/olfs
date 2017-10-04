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

import org.apache.http.HttpEntity;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Apr 1, 2009
 * Time: 2:47:01 PM
 */
public class Util {

    private static final Logger _log;
    static {
        _log = org.slf4j.LoggerFactory.getLogger(Util.class);
    }

    public static Element getDocumentRoot(File f)throws IOException, JDOMException {
        Element root = null;
        Document cdDoc = getDocument(f);
        if(cdDoc!=null){
            root = cdDoc.getRootElement();
            root.detach();
        }
        return root;
    }

    public static Document getDocument(File f)throws IOException, JDOMException{

        String msg;


        if(!f.exists()){
            msg = "Cannot find file: "+ f.getAbsoluteFile();
            _log.error(msg);
            throw new IOException(msg);
        }

        if(!f.canRead()){
            msg = "Cannot read file: "+ f.getAbsoluteFile();
            _log.error(msg);
            throw new IOException(msg);
        }
        if(!f.isFile()){
            msg = "The file " + f.getAbsoluteFile() +" is not actually a file.";
            _log.error(msg);
            throw new IOException(msg);
        }

        SAXBuilder sb = new SAXBuilder();
        return sb.build(f);
    }

    public static Document getDocument(InputStream f)throws IOException, JDOMException{
        SAXBuilder sb = new SAXBuilder();
        return sb.build(f);
    }


    /**
     * Opens, parses, and returns the root JDOM Element of the XML docuument
     * located at the supplied URL.
     * The provided credentials will be consulted if an HTTP authentication challenge
     * is encountered.
     * @param docUrlString  The URL of the document to parse.
     * @param credsProvider  The authentication credentials to use when encountering an
     *                       HTTP authentication challenge
     * @return The root JDOM of the Document produced by parsing the content retrieved from
     *          docUrlString
     * @throws IOException
     * @throws JDOMException
     */
    static public Element  getDocumentRoot(String docUrlString, CredentialsProvider credsProvider)
            throws IOException, JDOMException {
        Element docRoot = null;
        Document doc = getDocument(docUrlString,credsProvider);
        if(doc!=null){
            docRoot = doc.getRootElement();
            docRoot.detach();
        }
        return docRoot;
    }


    /**
     * Opens, parses, and returns the XML docuument
     * located at the supplied URL.
     * The provided credentials will be consulted if an HTTP authentication challenge
     * is encountered.
     * @param docUrlString  The URL of the document to parse.
     * @param credsProvider  The authentication credentials to use when encountering an
     *                       HTTP authentication challenge
     * @return The JDOM Document produced by parsing the content retrieved from
     *         docUrlString
     * @throws IOException
     * @throws JDOMException
     */     static public Document  getDocument(String docUrlString, CredentialsProvider credsProvider) throws IOException, JDOMException {

        _log.debug("getDocument() - URL: {}",docUrlString);
        Document doc = null;

        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        HttpGet httpGet = new HttpGet(docUrlString);
        CloseableHttpResponse resp = httpclient.execute(httpGet);
        try {
            _log.debug("HTTP STATUS: {}",resp.getStatusLine());
            HttpEntity entity1 = resp.getEntity();
            doc = opendap.xml.Util.getDocument(entity1.getContent());
            EntityUtils.consume(entity1);
        } finally {
            resp.close();
        }

        return doc;

    }


    /**
     * Opens, parses, and returns the root JDOM Element of the XML docuument
     * located at the supplied filename path.
     * @param filename The file to open.
     * @return The root element of the document generated by SAX parsing the content of filenam
     * @throws MalformedURLException
     * @throws IOException
     * @throws JDOMException
     */
    public static Element  getDocumentRoot(String filename)throws MalformedURLException, IOException, JDOMException {
        Element docRoot = null;
        Document doc = getDocument(filename);
        if(doc!=null){
            docRoot = doc.getRootElement();
            docRoot.detach();
        }
        return docRoot;
    }


    /**
     * Opens, parses, and returns  the JDOM Docuument
     * located at the supplied filename path.
     * @param filename The file to open.
     * @return The JDOM Document generated by SAX parsing the content of filenam
     * @throws MalformedURLException
     * @throws IOException
     * @throws JDOMException
     */
    public static Document getDocument(String filename) throws IOException, JDOMException {

        _log.debug("getDocument() - Retrieving: "+filename);

        Document doc;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat() );
        String fname =  filename;
        if (filename.toLowerCase().startsWith("file:")){
            fname =  filename.substring(5,filename.length());
            if(fname.startsWith("/")){
                while(fname.startsWith("/")){
                    fname =  fname.substring(1);
                }
                fname = "/" + fname;
            }
        }
        File f = new File(fname);
        doc = getDocument(f);
        _log.debug("getDocument() - Loaded XML Document: \n"+xmlo.outputString(doc));
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

    public static String NCNAME_REGEX_STRING = "^[^\\.\\-\\d][\\w-\\._\\d]*$";
    public static boolean isNCNAME(String s){
        Pattern pattern = Pattern.compile(NCNAME_REGEX_STRING);
        return pattern.matcher(s).matches();
    }

    /**
     *  Convert the passed string to an NCNAME by replacing any disallowed character
     *  with n under score ("_") character. If needed this could expanded to a simple underscore
     *  escaping scheme, because, why not...
     * @param s
     * @return
     */
    public static String convertToNCNAME(String s){
        /*
        char[] allowedChars = {
                '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

        char[] disallowedFirstChars = { '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
        */

        char[] disallowedChars = {
                ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '/', ':',
                ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '`', '{', '|', '}', '~'} ;

        // Grind out a char by char replacement.
        for(char badChar: disallowedChars){
            s = s.replace(badChar,'_');
        }
        // Clean up first char - easy regex check
        String badFirstRegexString = "^[-\\.\\d].*$";
        if(Pattern.compile(badFirstRegexString).matcher(s).matches())
            s =  "_"+s.substring(1);
        return s;
    }


    public static void main(String[] args) {
        String[]  ncNameConversionTests = {
                "jhbwf", "2ljhb", "kbwv::(&^", "kbwv::(&^bartmight","238kbwv::(&^bartmight" };

        for(String testStr: ncNameConversionTests)
            System.out.println(testStr+" --> "+convertToNCNAME(testStr));

    }


}
