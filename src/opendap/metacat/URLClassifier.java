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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.DateClassification.DatePart;
import opendap.metacat.Equivalence.SortedValues;
import opendap.metacat.URLGroup.Equivalences;
import opendap.metacat.URLGroup.URLs;

/** 
 * Grovel over a bunch of DDX URLs and group them using equivalence classes.
 * 
 * @author jimg
 *
 */
public class URLClassifier {
	
	private List<URLGroup> groups = null;
	
	private DDXRetriever ddxRetriever;
	
    private static Logger log = LoggerFactory.getLogger(URLClassifier.class);

	public URLClassifier(String cacheName) throws Exception {
		ddxRetriever = new DDXRetriever(true, cacheName);
		groups = new ArrayList<URLGroup>();
	}

	public class URLGroups implements Iterable<URLGroup> {
		@Override
		public Iterator<URLGroup> iterator() {
			return groups.iterator();
		}
	}
	
	public URLGroups getUrlGroups() {
		return new URLGroups();
	}

	public static void main(String args[]) {
		URLClassifier classifier;
		try {
			classifier = new URLClassifier(args[0]);
		}
		catch (Exception e) {
			System.err.println("Could not initialize ddx retriever: " + e.getLocalizedMessage());
			return;
		}
		
		PrintStream ps; 
		try {
			ps = new PrintStream("classifier_" + args[0] + ".txt");
		}
		catch (FileNotFoundException e) {
			System.err.println("Could not open the output file: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}

		ps.println("Classification for: " + args[0]);
		ps.println("Starting classification: " + (new Date()).toString());
		
		try {
			classifier.classifyURLs(ps);
		} 
		catch (Exception e) {
			System.err.println("Could not open the output file: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public int classifyURLs(PrintStream ps) throws Exception {
		int numberOfUrls = assignUrlsToInitialGroups(ddxRetriever.getCache().getLastVisitedKeys());
		
		if (ps != null)
			ps.println("Completed pass 1: " + (new Date()).toString());
		
		lookForDates();
		
		if (ps != null) {
			ps.println("Completed pass 2: " + (new Date()).toString());

			ps.println("Number of URLs processed: " + new Integer(numberOfUrls).toString());

			printClassifications(ps);
			printCompleteClassifications(ps);
		}
		
		return numberOfUrls;
	}
	
	/**
	 * For all of the URLs, assign them to an initial set of URLGroups.
	 * 
	 * @return The number of URLs processed
	 * @throws Exception If the URLComponents object cannot be built
	 */
	public int assignUrlsToInitialGroups(Enumeration<String> ddxURLs) throws Exception {
    	
		// Special case for the first URLGroup (because using 'for' with an
		// iterator fails when the iterator instance is null
    	if (ddxURLs.hasMoreElements()) {
    		String ddxURL = ddxURLs.nextElement();
			ParsedURL parsedUrl = new ParsedURL(ddxURL);
			URLProcessedComponents classification = new URLProcessedComponents(parsedUrl);

    		URLGroup group = new URLGroup(parsedUrl, classification);
    		
    		groups.add(group);
    	}
		
    	int numberOfUrls = 1;
    	
		while (ddxURLs.hasMoreElements()) {
			++numberOfUrls;
			String ddxURL = ddxURLs.nextElement();

			try {
				ParsedURL parsedUrl = new ParsedURL(ddxURL);
				URLProcessedComponents classification = new URLProcessedComponents(parsedUrl);
				String[] classificationLexemes = classification.getLexemeArray();
				// Look for an existing equivalence class for this ddxURL. If
				// one matches, add the parsed URL to it and stop looking for more
				// matches.
				boolean found = false;
				for (URLGroup group : groups) {
					if (Arrays.equals(classificationLexemes, group.getClassifications().getLexemeArray())) {
						group.add(parsedUrl);
						found = true;
						continue;
					}
				}

				// If an equivalence class is not found, add a new one
				if (!found) {
					URLGroup group = new URLGroup(parsedUrl, classification);
					groups.add(group);
				}
			}
			catch (Exception e) {
				log.error("Exception (will continue running): " + e.getLocalizedMessage());
			}
		}
		
		return numberOfUrls;
	}
	
	/**
	 * Look for dates in the equivalence classes that define each group
	 * 
	 * @throws Exception
	 */
	public void lookForDates() throws Exception {

		for (URLGroup group : groups) {
			Equivalences equivs = group.getEquivalences();
			for (Equivalence e: equivs) {
				if (!e.isLitteral()) {
					DateClassification.classifyPotentialDate(e);
				}
			}
		}
	}
	
	private void printClassifications(PrintStream ps, boolean print_urls, boolean print_all_urls, boolean print_histogram) {
		Integer i = 0;
		for(URLGroup group: groups) {
			ps.print(i.toString() + ": ");
			for(String comp: group.getClassifications().getLexemeArray())
				ps.print(comp + " ");
			ps.println();
			++i;
			
			if (print_histogram) {
				Equivalences equivs = group.getEquivalences();
				for (Equivalence e: equivs) {
					String tm =  new Integer(e.getTotalMembers()).toString();
					String dv = new Integer(e.getNumberOfValues()).toString();
					ps.println("\tEquivalence class: "  + e.getPattern() + "; Total members: " + tm + "; Discreet values: " + dv);
					if (e.getNumberDateClassifications() > 0) {
						ps.print("\t\tFound a potentail date:");
						for (DatePart dp: e.getDateClassifications())
							ps.print(" " + dp.toString());		
						ps.println();
					}
				}

				ps.println();
			}

			if (print_urls) {
				// Find the Equivalence with the most date parts; then sort and
				// print
				Equivalence date = group.getDateEquivalence();

				// Either print the sorted URLs or just print them
				if (date != null) {
					SortedValues sc = date.getSortedValues();
					if (print_all_urls) {
						for (DateString comp : sc) {
							ps.println("\t" + date.getParsedURL(comp.getDateString()).getTheURL());
						}
					} 
					else { // Just print the first and last URL
						DateString first = sc.get(0);
						DateString last = sc.get(sc.size() - 1);
						ps.println("\t" + date.getParsedURL(first.getDateString()).getTheURL());
						ps.println("\t" + date.getParsedURL(last.getDateString()).getTheURL());
					}
				}				
				else {
					URLs urls = group.getURLs();
					if (print_all_urls) {
						for (ParsedURL u : urls)
							ps.println("\t" + u.getTheURL());
					}					
					else {
						ParsedURL first = urls.get(0);
						ParsedURL last = urls.get(urls.size() - 1);
						ps.println("\t" + first.getTheURL());
						ps.println("\t" + last.getTheURL());
					}
				}
				ps.println();
			}
		}
	}

	public void printClassifications(PrintStream ps) {
		printClassifications(ps, false, false, false);
	}

	public void printCompleteClassifications(PrintStream ps) {
		printClassifications(ps, true, false, true);
	}
	
	/**
	 * This pass through the groups looks for adjacent Equivalence patterns
	 * that have been identified as parts of dates and combine them. In
	 * particular, it looks for year/month, year/month/day and year/daynum.
	 * 
	 * @note Not used
	 */
	@SuppressWarnings("unused")
	private void mergeAdjacentDates() {
		for (URLGroup group : groups) {
			Equivalences equivs = group.getEquivalences();

			HashMap<DatePart, Equivalence> found = new HashMap<DatePart, Equivalence>();

			// This makes some redundant tests, but that clarifies the state
			// machine a bit. See notes, p83, 8/20/2010
			for (Equivalence e: equivs) {
				if (found.size() == 0) {
					if (e.hasDateClassification(DatePart.year)) {
						found.put(DatePart.year, e);
					}
				}
				else if (found.size() == 1 && found.containsKey(DatePart.year)) {
					if (e.hasDateClassification(DatePart.month)) {
						found.put(DatePart.month, e);
						// At this point we don't know if this is the whole
						// sequence to merge or not. See later state.
					}
					else if (e.hasDateClassification(DatePart.daynum)) {
						found.put(DatePart.daynum, e);
						// Found Year/DayNum
					}
				}
				else if (found.size() == 2 && found.containsKey(DatePart.year) && found.containsKey(DatePart.month)) {
					if (e.hasDateClassification(DatePart.day)) {
						found.put(DatePart.day, e); 
						// At this point found holds a YMD date
					}
					else {
						// At this point found holds a YM date and the current 
						// equivalence is not a day, so we have found YM
					}
				}
				else if (found.size() == 2 && found.containsKey(DatePart.year) && found.containsKey(DatePart.month)) {
					if (e.hasDateClassification(DatePart.day)) {
						found.put(DatePart.day, e); 
						// At this point found holds a YMD date
					}
				}
			}
		}
	}
}
