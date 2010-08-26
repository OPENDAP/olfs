package opendap.metacat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
public class URLGroup {

    private static Logger log = LoggerFactory.getLogger(URLGroup.class);

	private List<ParsedURL> urls;
	private URLProcessedComponents processedComponents;
	private Vector<Equivalence> equivalences;

	public class URLs implements Iterable<ParsedURL> {
		private Iterator<ParsedURL> i = urls.iterator();

		@Override
		public Iterator<ParsedURL> iterator() {
			return i;
		}
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
		Lexemes ce = pc.getLexemes();
		while (ce.hasMoreElements()) {
			Lexeme c = ce.nextElement();
			equivalences.add(new Equivalence(i++, c));
		}

		add(url);
	}
	
	public void add(ParsedURL url) {
		// First record the URL as being part of the Group
		urls.add(url);
		
		// Increment equivalence class counts
		for (Equivalence e: equivalences) {
			// Because the ParsedURL has both the full and parsed components of
			// the URL and because the Equivalence already knows whch part of
			// the URL it is associated with, we can pass only the ParsedURL
			// object.
			e.add(url);
		}
	}
	
	public URLProcessedComponents getClassifications() {
		return processedComponents;
	}	
}
