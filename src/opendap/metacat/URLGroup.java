package opendap.metacat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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

	private Set<ParsedURL> urls;
	private URLProcessedComponents processedComponents;
	private Vector<Equivalence> equivalences;
	private Map<String, ParsedURL> orderedURLs;

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
		this.processedComponents = pc;
		this.urls = new HashSet<ParsedURL>();
		this.equivalences = new Vector<Equivalence>();
		this.orderedURLs = new HashMap<String, ParsedURL>();
		
		// Initialize the Vector of Equivalences for this group
		int i = 0;
		Lexemes ce = pc.getLexemes();
		while (ce.hasMoreElements()) {
			Lexeme c = ce.nextElement();
			equivalences.add(new Equivalence(i++, c.getValue(), c.isPattern()));
		}

		add(url);
	}
	
	public void add(ParsedURL url) {
		urls.add(url);
		
		String[] components = url.getComponents();
		
		// For the new URL, increment equivalence class counts
		for (Equivalence e: equivalences) {
			int i = e.getComponentNumber();
			log.debug("Adding 'components[" + new Integer(i).toString() + "]': " + components[i]);
			e.add(components[i]);
			// Add the parsed url object (reference!) to the set of URLs for
			// this equiv using th components[i] as the key. This enables the
			// retrieval of the URLs according to a sort of the equivalence
			// component's values.
		}
	}
	
	public URLProcessedComponents getClassifications() {
		return processedComponents;
	}	
}
