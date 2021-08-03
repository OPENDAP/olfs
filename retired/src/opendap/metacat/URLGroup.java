/////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2010 OPeNDAP, Inc.
// Author: James Gallagher  <jgallagher@opendap.org>
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import opendap.metacat.URLProcessedComponents.Lexemes;
import opendap.metacat.URLProcessedComponents.Lexeme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * A URLGroup is a set of URLs each with the same number of components where
 * each corresponding component of all the URLs either matches literally or
 * is a member of an equivalence class. Since the number of components match,
 * we can keep track of the variable in values for those components that fall
 * into an equivalence class. Once all the URLs have been processed, the 
 * histograms built for for those equivalence classes can be used to alter
 * the grouping.
 * 
 * @author jimg
 *
 */
public class URLGroup implements Serializable {
	private static final long serialVersionUID = 1L;

	private static Logger log = LoggerFactory.getLogger(URLGroup.class);

	private List<ParsedURL> urls;
	private URLProcessedComponents processedComponents;
	private Vector<Equivalence> equivalences;
	
	// These fields store information about the group that can be useful when
	// building NCML files.
	
	// on second though... these don't really belong here.
	
	/*
	/// Guess at the part of the URL that follows the servlet/context/cgi 
	private String serverDataRoot = null;
	/// What is the root directory of this dataset (essentially NCML's datasetScan)
	private String datasetRoot = null;
	/// Is this a time-series of files?
	private boolean isTimeSeries = false;
	*/
	public class URLs implements Iterable<ParsedURL> {
		private Iterator<ParsedURL> i = urls.iterator();

		@Override
		public Iterator<ParsedURL> iterator() {
			return i;
		}

		public ParsedURL get(int j) {
			return urls.get(j);
		}

		public int size() {
			return urls.size();
		}
	}
	/*
	public String getServerDataRoot() {
		if (serverDataRoot == null) {
			
		}
		return serverDataRoot;
	}
	
	int serverNameEndPosition = findServerNameEnd(ddxUrl);
	int dataRootPosition = findDataRoot(group);
	int datasetScanPosition = findDatasetScan(group);
	*/
	
	public String toString() {
		StringBuilder s = new StringBuilder(2048);
		s.append("Number of URLs: ");
		s.append(urls.size());
		s.append("; ");
		
		s.append("First URL: ");
		s.append(urls.get(0).getTheURL());
		s.append("\n");
		
		Equivalences equivs = getEquivalences();
		for (Equivalence e : equivs) {
			s.append(e.getPattern());
			s.append(" ");
		}

		return s.toString();
	}
	
	/**
	 * Get an instance of the Iteration over the URLs in this group.
	 * 
	 * @return URLs: An iterator over the URLs in the group.
	 */
	public URLs getURLs() {
		return new URLs();
	}
	
	/**
	 * How many URLs are in this group?
	 * 
	 * @return The number of URL in the group.
	 */
	public int getNumberOfUrls() {
		return urls.size();
	}

	/**
	 * An Enumeration of the equivalence classes that help define this group.
	 * @author jimg
	 *
	 */
	public class Equivalences implements Iterable<Equivalence> {
		Iterator<Equivalence> i = equivalences.iterator();

		@Override
		public Iterator<Equivalence> iterator() {
			return i;
		}
	}

	/**
	 * Use this to access the Equivalences that help define this group.
	 * @return
	 */
	public Equivalences getEquivalences() {
		return new Equivalences();
	}

	/**
	 * Build a new URLGroup and initialize it with a single URL.
	 * 
	 * @param url The parsed  URL
	 * @param pc Classifications for the parsed components
	 * @throws Exception
	 */
	public URLGroup(ParsedURL url, URLProcessedComponents pc) {
		// By definition, each URL in a group has the same URLEquivalenceClasses
		processedComponents = pc;
		urls = new ArrayList<ParsedURL>();
		equivalences = new Vector<Equivalence>();
		
		// Initialize the Vector of Equivalences for this group
		int i = 0;
		Lexemes lexemes = pc.getLexemes();
		while (lexemes.hasMoreElements()) {
			Lexeme lexeme = lexemes.nextElement();
			equivalences.add(new Equivalence(i++, lexeme));
		}

		add(url);
	}
	
	public void add(ParsedURL url) {
		// First record the URL as being part of the Group
		urls.add(url);
		
		// Increment equivalence class counts and add this URL to all 
		// appropriate equivalences.
		for (Equivalence e: equivalences) {
			e.add(url);
		}
	}
	
	/**
	 * Search for the Equivalence that has the largest number of DatePart 
	 * instances and return it. This can be used to provide an ordering for
	 * the URLs in the URLGroup.
	 * 
	 * @return An Equivalence for the component with the most date information.
	 * Return null if no Equivalence has date information.
	 */
	public Equivalence getDateEquivalence() {
		Equivalences equivs = getEquivalences();
		Equivalence date = null;
		int maxNumDateParts = 0;
		for (Equivalence e : equivs) {
			if (e.getNumberDateClassifications() > maxNumDateParts) {
				maxNumDateParts = e.getNumberDateClassifications();
				date = e;
			}
		}

		return date;
	}

	public URLProcessedComponents getClassifications() {
		return processedComponents;
	}	
}
