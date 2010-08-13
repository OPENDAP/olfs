package opendap.metacat;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.URLGroup.Equivalence;
import opendap.metacat.URLGroup.EquivalenceEnumeration;
import opendap.metacat.URLGroup.URLEnumeration;

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
			System.err.println("Could not initial ddx retriever: " + e.getLocalizedMessage());
			return;
		}
		
		PrintStream ps; 
		try {
			ps = new PrintStream("classifier_" + args[0] + ".txt");
		}
		catch (FileNotFoundException e) {
			log.error("Could not open the output file: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}

		ps.println("Classification for: " + args[0]);
		ps.println("Starting classification: " + (new Date()).toString());
		
		try {
			int numberOfUrls = c.pass1();
			ps.println("Completed classification: " + (new Date()).toString());
			ps.println("Number of URLs processed: " + new Integer(numberOfUrls).toString())
			;
			c.printClassifications(ps);
			c.printCompleteClassifications(ps);
		} 
		catch (Exception e) {
			log.error("Could not open the output file: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	public int pass1() throws Exception {

    	Enumeration<String> ddxURLs = ddxSource.getCache().getLastVisitedKeys();
    	
    	// Special case for the first URL (because using 'for' with an iterator
    	// fails when the iterator instance is null
    	if (ddxURLs.hasMoreElements()) {
    		String ddxURL = ddxURLs.nextElement();
			URLComponents componets = new URLComponents(ddxURL);
			URLClassification classification = new URLClassification(componets);

    		URLGroup group = new URLGroup(ddxURL, componets, classification);
    		
    		groups.add(group);
    	}
		
    	int numberOfUrls = 1;
    	
		while (ddxURLs.hasMoreElements()) {
			++numberOfUrls;
			String ddxURL = ddxURLs.nextElement();
			log.debug("Processing URL: " + ddxURL);
			try {
				URLComponents componets = new URLComponents(ddxURL);
				URLClassification classification = new URLClassification(componets);

				// Look for an existing equivalence class for this ddxURL. If
				// one matches, add the ddxURL to it and stop looking for more
				// matches.
				boolean found = false;
				for (URLGroup group : groups) {
					if (Arrays.equals(classification.getClassification(), group.getClassification())) {
						group.add(ddxURL, componets);
						found = true;
						continue;
					}
				}

				// If an equivalence class is not found, add a new one
				if (!found) {
					URLGroup group = new URLGroup(ddxURL, componets, classification);
					// TODO: Is it more efficient to prepend than append?
					groups.add(group);
				}
			}
			catch (Exception e) {
				log.error("Exception (will continue running): " + e.getLocalizedMessage());
			}
		}
		
		return numberOfUrls;
	}
	
	private void printClassifications(PrintStream ps, boolean print_all_data, boolean print_histogram) {
		Integer i = 0;
		for(URLGroup group: groups) {
			ps.print(i.toString() + ": ");
			for(String comp: group.getClassification())
				ps.print(comp + " ");
			ps.println();
			++i;
			
			if (print_histogram) {
				EquivalenceEnumeration ee = group.getEquivalences();
				while(ee.hasMoreElements()) {
					Equivalence e = ee.nextElement();
					String tm =  new Integer(e.getTotalMembers()).toString();
					String dv = new Integer(e.getSize()).toString();
					ps.println("\tEquivalence class: "  + e.getComponentSpecifier() + "; Total members: " + tm + "; Discreet values: " + dv);
				}
				
				ps.println();
			}
			
			if (print_all_data) {
				URLEnumeration urls = group.getURLs();
				while(urls.hasMoreElements())
					ps.println("\t" + urls.nextElement());
					
				ps.println();
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
