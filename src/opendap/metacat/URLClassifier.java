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
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
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
	
	private URLGroups groups = null;
	
	private DDXRetriever ddxRetriever;
	
    private static Logger log = LoggerFactory.getLogger(URLClassifier.class);

	public URLClassifier(String cacheName, boolean readOnly) throws Exception {
		// by default DDXRetriever uses a read-only cache
		log.debug("Making DDXRetriever using cache name: " + cacheName);
		
		ddxRetriever = new DDXRetriever(cacheName);
		
		if (readOnly)
			groups = new URLGroups(cacheName); // This reads from .ser file
		else
			groups = new URLGroups();
	}
	
	public static void main(String args[]) {
		URLClassifier classifier;

		CommandLineParser parser = new PosixParser();

		Options options = new Options();

		options.addOption("v", "verbose", false, "Be verbose");
		options.addOption("h", "help", false, "Usage information");
		
		options.addOption("c", "cache-name", true, "Cache name prefixes; read DDX URLs from cache files with this name prefix.");
		options.addOption("r", "read-only", false, "Just read an already-writen URLGroups file and print its contents.");
		options.addOption("o", "output", true, "Write files using this name as the prefix.");
		
		boolean verbose;
		String cacheName;
		String output;
		
		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("url_classifier [options] --cache-name <name prefix>", options);
				return;
			}
		    
			verbose = line.hasOption("verbose");
			cacheName = line.getOptionValue("cache-name");
			output = line.getOptionValue("output");

			if (cacheName == null || cacheName.isEmpty())
				throw new Exception("--cache-name must be given");
			if (output == null || output.isEmpty())
				output = cacheName;

			boolean readOnly = line.hasOption("read-only");
			if (readOnly)
				verbose = true;
			
			classifier = new URLClassifier(cacheName, readOnly);

			PrintStream ps = null;
			if (verbose) {
				ps = new PrintStream("classifier_" + output + ".txt");
				ps.println("Classification for: " + output);
				ps.println("Starting classification: " + (new Date()).toString());
			}

			if (!readOnly)
				classifier.classifyURLs(ps);

			if (ps != null) {
				classifier.printClassifications(ps);
				classifier.printCompleteClassifications(ps);
			}
			
			if (!readOnly)
				classifier.groups.saveState(output);
		}
		catch (FileNotFoundException e) {
			System.err.println("File error: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public int classifyURLs(PrintStream ps) throws Exception {
		if (ps != null)
			ps.println("Starting pass 1: " + (new Date()).toString());
		
		int numberOfUrls = assignUrlsToInitialGroups(ddxRetriever.getCachedDDXURLs());
		
		if (ps != null)
			ps.println("Completed pass 1 (" + numberOfUrls + " URLs): " + (new Date()).toString());
		
		lookForDates();
		
		if (ps != null) {
			ps.println("Completed pass 2: " + (new Date()).toString());

			ps.println("Number of URLs processed: " + new Integer(numberOfUrls).toString());
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

    		log.debug("Processing URL: " + ddxURL);

    		ParsedURL parsedUrl = new ParsedURL(ddxURL);
			URLProcessedComponents classification = new URLProcessedComponents(parsedUrl);

    		URLGroup group = new URLGroup(parsedUrl, classification);
    		
    		groups.add(group);
    	}
		
    	int numberOfUrls = 1;
    	
		while (ddxURLs.hasMoreElements()) {
			++numberOfUrls;
			String ddxURL = ddxURLs.nextElement();

    		log.debug("Processing URL: " + ddxURL);
    		if (numberOfUrls % 10000 == 0) {
    			log.info("Time " + (new Date()).toString());
    			log.info("Processed " + numberOfUrls);
    			log.info("Currently there are " + groups.size() + " groups");
    		}
    		
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
						break;
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
						ps.print("\t\tFound a potential date:");
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

	/**
	 * Print simple information about a classification. Foreach group, print
	 * the path components used to form the equivalence classes.
	 * @param ps
	 */
	public void printClassifications(PrintStream ps) {
		printClassifications(ps, false, false, false);
	}

	/**
	 * Print more information about each classification. This sorts the URLs
	 * in the group and prints the first and last ones. It also prints 
	 * histogram information for each equivalence used to form the group.
	 * @param ps
	 */
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
