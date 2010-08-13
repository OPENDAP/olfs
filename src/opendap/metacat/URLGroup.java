package opendap.metacat;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.nceas.utilities.Log;

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

	/**
	 * This holds a URL and its parsed components. Really just a convenience
	 * because I don't want the to run the parser more than once. If we make 
	 * the parsing smarter, this could be more important...
	 * @author jimg
	 *
	 */
    /*
    private class ParsedURL {
		private String URL;
		private URLComponents comps;
		
		public ParsedURL(String u, URLComponents c) {
			URL = u;
			comps = c;
		}
		
		public String getURL() {
			return URL;
		}
		
		public URLComponents getComponents() {
			return comps;
		}
	}
	*/
	public class URLEnumeration implements Enumeration<String> {
		//private Iterator<ParsedURL> i = urls.iterator();
		private Iterator<String> i = urls.iterator();
		
		@Override
		public boolean hasMoreElements() {
			return i.hasNext();
		}

		@Override
		public String nextElement() {
			return i.next(); //.getURL();
		}
	}
	
	public URLEnumeration getURLs() {
		return new URLEnumeration();
	}

	/**
	 * Store information about a particular 'equivalence class'. Each URLGroup
	 * will likely have several of these. Once all the URLs are processed, we 
	 * will want to look at how individual patterns are distributed to see if
	 * the equivalence class should be split or not. The likely criteria for
	 * the splitting a class is that all of the instances fall into a very
	 * small number of distinct values with zero outliers. At least that's the
	 * idea.
	 * @author jimg
	 *
	 */
	public class Equivalence {
		int componentNumber;	// Which of the URL's components
		String componentSpecifier;
		int totalMembers;		// Total number of data points
		Map<String, Integer> componentValues; // Occurrences of a given string
		
		public Equivalence(int n, String s) {
			componentValues = new HashMap<String, Integer>();
			componentNumber = n;
			componentSpecifier = s;
			totalMembers = 0;
		}
		
		public void add(String comp) {
			++totalMembers;
			
			// If comp is in there already, increment its count
			if (componentValues.containsKey(comp)) {
				log.debug("cache hit for " + comp);
				componentValues.put(comp, componentValues.get(comp) + 1);
			}
			else { // Add the comp and set count to one
				log.debug("Adding new value (" + comp + ") for equiv class " + componentSpecifier);
				componentValues.put(comp, 1);
			}	
		}
		
		public int getComponentNumber() {
			return componentNumber;
		}
		
		public String getComponentSpecifier() {
			return componentSpecifier;
		}
		
		public int getTotalMembers() {
			return totalMembers;
		}
		
		public int getOccurrences(String comp) {
			return componentValues.get(comp);
		}
		
		public int getSize() {
			return componentValues.size();
		}
		
		public class ComponentsEnumeration implements Enumeration<String> {
			private Iterator<String> comps = componentValues.keySet().iterator();

			@Override
			public boolean hasMoreElements() {
				return comps.hasNext();
			}

			@Override
			public String nextElement() {
				return comps.next();
			}
		}
	}
	
	public class EquivalenceEnumeration implements Enumeration<Equivalence> {
		Iterator<Equivalence> i = EquivalenceClasses.iterator();

		@Override
		public boolean hasMoreElements() {
			return i.hasNext();
		}

		@Override
		public Equivalence nextElement() {
			return i.next();
		}
	}

	public EquivalenceEnumeration getEquivalences() {
		return new EquivalenceEnumeration();
	}

	//private Set<ParsedURL> urls;
	private Set<String> urls;
	private URLClassification classification;
	private Vector<Equivalence> EquivalenceClasses;
	
	/**
	 * Build a new URLGroup and initialize it with a single URL.
	 * 
	 * @param url The URL, a String
	 * @param comps The parsed components of the URL
	 * @param uc Classifications for the parsed components
	 * @throws Exception
	 */
	public URLGroup(String url, URLComponents comps, URLClassification uc) {
		// By definition, each URL in a group has the same URLClassification
		this.classification = uc;
		//this.urls = new HashSet<ParsedURL>();
		this.urls = new HashSet<String>();
		this.EquivalenceClasses = new Vector<Equivalence>();
		
		// Initialize the Vector of Equivalences for this group
		int i = 0;
		for (String c: uc.getClassification()) {
			EquivalenceClasses.add(new Equivalence(i++, c));
		}
		
		add(url, comps);
	}
	
	public void add(String url, URLComponents comps) {
		// urls.add(new ParsedURL(url, comps));
		urls.add(url);
		
		String[] components = comps.getComponents();
		
		// For the new URL, increment equivalence class counts
		for (Equivalence e: EquivalenceClasses) {
			int i = e.getComponentNumber();
			log.debug("Adding 'components[" + new Integer(i).toString() + "]': " + components[i]);
			e.add(components[i]);
		}
	}
	
	public String[] getClassification() {
		return classification.getClassification();
	}
		
}
