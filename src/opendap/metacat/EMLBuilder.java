/////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2010 OPeNDAP, Inc.
// Author: James Gallagher <jgallagher@opendap.org>
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

package opendap.metacat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import opendap.xml.Transformer;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the task of getting an EML given a DDX document. It can
 * test the returned document to see if it is well-formed and it can cache the
 * document.
 * 
 * @author jimg
 * 
 */
public class EMLBuilder {

	final static String ddx2emlDefault = "ddx2eml-3.2.xsl";
	
	private static Logger log = LoggerFactory.getLogger(EMLBuilder.class);

	// The EMLCache that holds both the DDXs LMT and the EML XML/text
	private ResponseCachePostgres EMLCache = null;

    // This is the transformer that takes the DDX and returns EML
    private Transformer transformer;

    private Date date = new Date();

	public EMLBuilder() throws Exception {
		this(false, "", ddx2emlDefault);
	}

	public EMLBuilder(String namePrefix) throws Exception{
		this(true, namePrefix, ddx2emlDefault);
	}
	
	public EMLBuilder(String namePrefix, String xslt) throws Exception{
		this(true, namePrefix, xslt);
	}
	
	public EMLBuilder(boolean useCache, String namePrefix, String xslt) throws Exception{

		try {
			transformer = new Transformer(xslt);
		}
		catch (SaxonApiException e) {
			log.error("Could not build the XSL transformer object: ", e);
			throw new Exception(e);
		}
		
		// The first parameter to EMLCache() restores the cache from its
		// persistent form and will cause the cache to be written when
		// the DDXCache instance is collected.
		if (useCache)
			EMLCache = new ResponseCachePostgres(namePrefix + "EML", "eml_responses");
	}
	
	/**
	 * Simple method to test if the EML will parse. Generally there's no need to
	 * call this but it'll be useful when developing the crawler.
	 * 
	 * @note This method must be called by client code; it is not used by any of
	 *       the methods here.
	 * 
	 * @param ddxString
	 *            The EML to test
	 * @return true if the EML parses, false if the SAX parser throws an
	 *         exception
	 */
	public boolean isWellFormedEML(String emlString) {
		try {
			org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
			@SuppressWarnings("unused")
			org.jdom.Document emlDoc = sb.build(new ByteArrayInputStream(emlString.getBytes()));
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Build and cache an EML document using the given DDX document. Use the 
	 * DDX's URL as the key for the cache entry. If caching is not on, ignore
	 * the DDX URL and don't use the cache.
	 * 
	 * @param ddxUrl Use this as the key when caching the EML
	 * @param ddxString Build EML from this document
	 * @return The EML document
	 * @throws Exception 
	 */
	public String getEML(String ddxUrl, String ddxString) throws Exception {

		String params[] = new String[4];
		
		// '.' finds the start of the '.ddx. extension
		String dataset_url = ddxUrl.substring(0, ddxUrl.lastIndexOf('.'));
		// '/' + 1 finds the start of the filename
		String filename = dataset_url.substring(ddxUrl.lastIndexOf('/') + 1);
		
		// Build the params
		params[0] = "filename";
		params[1] = filename;
		params[2] = "dataset_url";
		params[3] = dataset_url;
		
		return getEML(ddxUrl, ddxString, params);
	}
	
	/**
	 * This version takes a varying number of parameters. 
	 * 
	 * @param ddxUrl Use this as the key when caching the EML
	 * @param ddxString Build EML from this document
	 * @param params Array element pairs: name1, value1, name2, value2, ...
	 * @return The EML document
	 * @throws Exception
	 */
	public String getEML(String ddxUrl, String ddxString, String[] params) throws Exception {
		// Get the EML document
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XdmNode ddxXdm = null;
		
		try {
			ddxXdm = transformer.build(new StreamSource(new ByteArrayInputStream(ddxString.getBytes("UTF-8"))));
							
			// Set parameters
			for (int i = 0; i < params.length; i += 2) {
				log.debug("Setting parameter named: " + params[i]);
				transformer.setParameter(params[i], params[i+1]);
			}
			
			transformer.transform(ddxXdm, os);
		} 
		catch (Exception e) {
			log.error("While trying to transform the DDX: " + ddxString);
			log.error("I got the following error: " + e.getMessage());
			return "";
		}
		finally {
			// Clear parameters
			for (int i = 0; i < params.length; i += 2) {
				transformer.clearParameter(params[i]);
			}
		}
		
		String eml = os.toString();
		
		if (EMLCache != null) {
			EMLCache.setLastVisited(ddxUrl, date.getTime());
			EMLCache.setCachedResponse(ddxUrl, eml);
		}
		
		return eml;
	}
    
	/**
	 * Return the EML document generated using the DDX from the given DDX URL.
	 * This method reads from the EML cache.
	 * 
	 * @param DDXURL The DDX URL is the key used to reference the EML document.
	 * @return The EML in a String.
	 * @throws Exception Thrown if caching is not on.
	 */
	public String getCachedEMLDoc(String DDXURL) throws Exception {
		if (EMLCache == null)
			throw new Exception("Caching is off but I was asked to read from the cache.");
		return EMLCache.getCachedResponse(DDXURL);
	}

	/**
	 * Save the EML cache.
	 * 
	 * @throws Exception Thrown if caching is not on.
	 */
	public void saveEMLCache() throws Exception {
		if (EMLCache == null)
			throw new Exception("Caching is off but I was asked to save the cache.");
		EMLCache.saveState();
	}
}
