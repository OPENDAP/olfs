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
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
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

    private static final Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(Util.class);
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
        SAXBuilder sb = new SAXBuilder();
        return sb.build(f);
    }


    public static Document getDocument(URL url)throws IOException, JDOMException{
        SAXBuilder sb = new SAXBuilder();
        return sb.build(url);
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
    public static  Element  getDocumentRoot(String docUrlString, CredentialsProvider credsProvider)
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
     */
    public static Document  getDocument(String docUrlString, CredentialsProvider credsProvider) throws IOException, JDOMException {

        log.debug("getDocument() - URL: {}", docUrlString);
        Document doc = null;

        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        HttpGet httpGet = new HttpGet(docUrlString);
        CloseableHttpResponse resp = httpclient.execute(httpGet);
        try {
            log.debug("HTTP STATUS: {}", resp.getStatusLine());
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
    public static Element  getDocumentRoot(String filename)throws IOException, JDOMException {
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

        log.debug("getDocument() - Retrieving: {}",filename);

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
        if(log.isDebugEnabled()) {
            log.debug("getDocument() - Loaded XML Document: \n{}", xmlo.outputString(doc));
        }
        return doc;
    }


    public static final String NCNAME_REGEX_STRING = "^[^\\.\\-\\d][\\w-\\._\\d]*$";
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

    /**
     * Returns a "safe" javax.xml.stream.XMLInputFactory.
     * Because parsing "untrusted" XML document with external entities and DTD's enabled can can allow attackers to
     * request pretty much any file from the host file system we disable these prior to returning the factory.
     *
     * @return An javax.xml.stream.XMLInputFactory with external entities and DTD's disabled.
     */
    public static XMLInputFactory getXmlInputFactory(){
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return factory;
    }

    /**
     * Returns a "safe" javax.xml.parsers.DocumentBuilder.
     * Because parsing "untrusted" XML document with external entities and DTD's enabled can can allow attackers to
     * request pretty much any file from the host file system we disable these prior to returning the factory.
     *
     * @return An javax.xml.parsers.DocumentBuilder with external entities and DTD's disabled.
     */
    public static DocumentBuilder getDocumentBuilder(){
        DocumentBuilder db = null;
        String FEATURE = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
            // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
            FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            dbf.setFeature(FEATURE, true);

            // If you can't completely disable DTDs, then at least do the following:
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            // JDK7+ - http://xml.org/sax/features/external-general-entities
            FEATURE = "http://xml.org/sax/features/external-general-entities";
            dbf.setFeature(FEATURE, false);

            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
            // JDK7+ - http://xml.org/sax/features/external-parameter-entities
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            dbf.setFeature(FEATURE, false);

            // Disable external DTDs as well
            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dbf.setFeature(FEATURE, false);

            FEATURE = null;
            // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(true);

            db = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            // This should catch a failed setFeature feature
            String msg = "Failed to get DocumentBuilder!";
            if(FEATURE!=null){
                msg +=  "The feature '" + FEATURE + "' is probably not supported by the XML processor.";
            }
            msg += " Caught ParserConfigurationException Message: " + e.getMessage();
            log.error(msg);
        }

        return db;
    }

    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger(Util.class);

        String[]  ncNameConversionTests = {
                "jhbwf", "2ljhb", "kbwv::(&^", "kbwv::(&^bartmight","238kbwv::(&^bartmight" };

        for(String testStr: ncNameConversionTests)
            log.info("{} --> {}",testStr, convertToNCNAME(testStr));

    }


}
