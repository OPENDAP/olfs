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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the task of getting an NCML given a DDX document. It can
 * test the returned document to see if it is well-formed and it can cache the
 * document. This is based on the DDX to NCML builder but, unlike that code,
 * this only handles the case where many DDX documents comprise a single
 * dataset.
 * 
 * @author jimg
 * 
 */
public class NCMLBuilder {

	final static String ddx2ncmlDefault = "many_ddx2ncml-1.0.xsl";
	
	private static Logger log = LoggerFactory.getLogger(NCMLBuilder.class);

	// The NCMLCache that holds both the DDXs LMT and the NCML XML/text
	private ResponseCachePostgres cache = null;

    // This is the transformer that takes the DDX and returns NCML
    private Transformer transformer;

    private Date date = new Date();

	public NCMLBuilder(String namePrefix) throws Exception{
		this(namePrefix, ddx2ncmlDefault);
	}
	
	public NCMLBuilder(String namePrefix, String xslt) throws Exception {
		try {
			transformer = new Transformer(xslt);
		}
		catch (SaxonApiException e) {
			log.error("Could not build the XSL transformer object: ", e);
			throw new Exception(e);
		}
		
		cache = new ResponseCachePostgres(namePrefix + "_NCML", "ncml_responses");
	}
	
	/**
	 * Simple method to test if the NCML will parse. Generally there's no need to
	 * call this but it'll be useful when developing the crawler.
	 * 
	 * @note This method must be called by client code; it is not used by any of
	 *       the methods here.
	 * 
	 * @param ddxString
	 *            The NCML to test
	 * @return true if the NCML parses, false if the SAX parser throws an
	 *         exception
	 */
	public boolean isWellFormedNCML(String ncmlString) {
		try {
			org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
			@SuppressWarnings("unused")
			org.jdom.Document ncmlDoc = sb.build(new ByteArrayInputStream(ncmlString.getBytes()));
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Build and cache an NCML document using the given DDX document. Use the 
	 * DDX's URL as the key for the cache entry. If caching is not on, ignore
	 * the DDX URL and don't use the cache.
	 * 
	 * @param ddxUrl Use this as the key when caching the NCML
	 * @param ddxString Build NCML from this document
	 * @return The NCML document
	 * @throws Exception 
	 */
	public String getNCML(String ddxUrl, String ddxString) throws Exception {

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
		
		return getNCML(ddxUrl, ddxString, params);
	}
	
	/**
	 * This version takes a varying number of parameters. 
	 * 
	 * @param ddxUrl Use this as the key when caching the NCML
	 * @param ddxString Build NCML from this document
	 * @param params Array element pairs: name1, value1, name2, value2, ...
	 * @return The NCML document
	 * @throws Exception
	 */
	public String getNCML(String ddxUrl, String ddxString, String[] params) throws Exception {
		// Get the NCML document
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
		
		String ncml = os.toString();
		
		if (cache != null) {
			cache.setLastVisited(ddxUrl, date.getTime());
			cache.setCachedResponse(ddxUrl, ncml);
		}
		
		return ncml;
	}
    
	/**
	 * Return the NCML document generated using the DDX from the given DDX URL.
	 * This method reads from the NCML cache.
	 * 
	 * @param DDXURL The DDX URL is the key used to reference the NCML document.
	 * @return The NCML in a String.
	 * @throws Exception Thrown if caching is not on.
	 */
	public String getCachedNCMLDoc(String DDXURL) throws Exception {
		if (cache == null)
			throw new Exception("Caching is off but I was asked to read from the cache.");
		return cache.getCachedResponse(DDXURL);
	}

	/**
	 * Save the NCML cache.
	 * 
	 * @throws Exception Thrown if caching is not on.
	 */
	public void saveNCMLCache() throws Exception {
		if (cache == null)
			throw new Exception("Caching is off but I was asked to save the cache.");
		cache.saveState();
	}
}
