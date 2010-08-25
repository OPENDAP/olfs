package opendap.metacat;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
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
	
	private DDXRetriever ddxSource;
	
    private static Logger log = LoggerFactory.getLogger(URLClassifier.class);

	public URLClassifier(String cacheName) throws Exception {
		ddxSource = new DDXRetriever(true, cacheName);
		groups = new LinkedList<URLGroup>();
	}

	public static void main(String args[]) {
		URLClassifier c;
		try {
			c = new URLClassifier(args[0]);
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
			int numberOfUrls = c.assignUrlsToInitialGroups();
			
			ps.println("Completed pass 1: " + (new Date()).toString());
			
			c.lookForDates();
			
			ps.println("Completed pass 2: " + (new Date()).toString());
			
			ps.println("Number of URLs processed: " + new Integer(numberOfUrls).toString());
			c.printClassifications(ps);
			c.printCompleteClassifications(ps);
			
			
		} 
		catch (Exception e) {
			System.err.println("Could not open the output file: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * For all of the URLs, assign them to an initial set of URLGroups.
	 * 
	 * @return The number of URLs processed
	 * @throws Exception If the URLComponents object cannot be built
	 */
	public int assignUrlsToInitialGroups() throws Exception {

    	Enumeration<String> ddxURLs = ddxSource.getCache().getLastVisitedKeys();
    	
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
	
	/**
	 * This pass through the groups looks for adjacent Equivalence patterns
	 * that have been identified as parts of dates and combine them. In
	 * particular, it looks for year/month, year/month/day and year/daynum.
	 * The Equivalences for the group in question are modified.
	 */
	/*
	private void mergeAdjacentDates() {
		for (URLGroup group : groups) {

			Equivalence current = null, previous = null;
			Equivalences es = group.getEquivalences();
			if (es.iterator().hasNext())
				current = es.iterator().next();
			while (es.iterator().hasNext()) {
				previous = current;
				current = es.iterator().next();
				
				if (previous.getDateClassification().size() > 0
					&& current.getDateClassification().size() > 0) {
					// merge adjacent date equivalences
				}
			}
			
			
		}
	}
	*/
	/*
	private void mergeAdjacentDates_no() {
		for (URLGroup group : groups) {
			Equivalences equivs = group.getEquivalences();

			HashMap<DatePart, Equivalence> found = new HashMap<DatePart, Equivalence>();

			// This makes some redundant tests, but that clarifies the sate
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
					}
					else if (e.hasDateClassification(DatePart.daynum)) {
						found.put(DatePart.daynum, e);
					}
				}
				else if (found.size() == 2 && found.containsKey(DatePart.year) && found.containsKey(DatePart.month)) {
					if (e.hasDateClassification(DatePart.day)) {
						found.put(DatePart.day, e); // At this point found holds a YMD date
					}
					else {
						// At this point found holds a YM date and the current 
						// equivalence is not a day, so we have found two adjacent 
						// date nodes to merge
					}
				}
				else if (found.size() == 2 && found.containsKey(DatePart.year) && found.containsKey(DatePart.month)) {
					if (e.hasDateClassification(DatePart.day)) {
						found.put(DatePart.day, e); // At this point found holds a YMD date
					}
				}
			}
		}
	}
	*/
	
	private void printClassifications(PrintStream ps, boolean print_all_data, boolean print_histogram) {
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
			/*
			if (print_all_data) {
				URLs urls = group.getURLs();
				for (ParsedURL u: urls)
					ps.println("\t" + u.getTheURL());
				ps.println();
			}
			*/
			if (print_all_data) {
				// Find the Equivalence with the most date parts; then sort and
				// print
				Equivalences equivs = group.getEquivalences();
				Equivalence date = null;
				int maxNumDateParts = 0;
				for (Equivalence e : equivs) {
					if (e.getNumberDateClassifications() > maxNumDateParts) {
						maxNumDateParts = e.getNumberDateClassifications();
						date = e;
					}
				}

				if (date != null) {
					SortedValues sc = date.getSortedValues();
					for (DateString comp : sc) {
						log.debug("DateString: " + comp.getDate());
						ParsedURL p = date.getParsedURL(comp.getDate());
						log.debug("ParsedURL: " + p);
						ps.println("\t" + date.getParsedURL(comp.getDate()).getTheURL());
					}
					ps.println();
				}
			}
		}
	}

	public void printClassifications(PrintStream ps) {
		printClassifications(ps, false, false);
	}

	public void printCompleteClassifications(PrintStream ps) {
		printClassifications(ps, true, true);
	}

}
