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

	//private Set<ParsedURL> urls;
	private Set<String> urls;
	private URLEquivalenceClasses classification;
	private Vector<Equivalence> equivalences;

    /**
     * Use this to access the URLs in the group.
     */
	public class URLEnumeration implements Enumeration<String> {
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
	
	/**
	 * Get an instance of the Enumeration of URLs in this group. There is no
	 * ordering to the URLs.
	 * 
	 * @return URLEnumeration: An enumeration of the URLs in the group.
	 */
	public URLEnumeration getURLs() {
		return new URLEnumeration();
	}

	/**
	 * Store information about a particular 'equivalence class'. Each URLGroup
	 * will likely have several of these. Once all the URLs are processed, we
	 * will want to look at how individual patterns are distributed to see if
	 * the equivalence class should be split or not. The likely criteria for the
	 * splitting a class is that all of the instances fall into a very small
	 * number of distinct values with zero outliers. At least that's the idea.
	 * 
	 * @note When a URL is parsed, it is broken into 'path components' which are
	 *       literally the parts between the '/' characters, and witing the
	 *       filename, any non alphanum character. For any path component that
	 *       contains digits, the code makes an Equivalence. The equivalence
	 *       classes preserve the morphology of the component; effectively they
	 *       'wild card' digits in various cases using a simple set of rules.
	 *       All the URLs in a URLGroup have either the same exact value or the
	 *       same Equivalence class for corresponding path components.
	 * 
	 * @see URLEquivalenceClasses
	 * @author jimg
	 * 
	 */
	public class Equivalence {
		int componentNumber;	// Which of the URL's components
		String componentSpecifier; // This is the string used to encode the pattern (eg 'dddd')
		int totalMembers;		// Total number of data points
		Map<String, Integer> componentOccurrences; // Occurrences of a given string
		
		public Equivalence(int n, String s) {
			componentOccurrences = new HashMap<String, Integer>();
			componentNumber = n;
			componentSpecifier = s;
			totalMembers = 0;
		}
		
		public void add(String comp) {
			++totalMembers;
			
			// If comp is in there already, increment its count
			if (componentOccurrences.containsKey(comp)) {
				log.debug("cache hit for " + comp);
				componentOccurrences.put(comp, componentOccurrences.get(comp) + 1);
			}
			else { // Add the comp and set count to one
				log.debug("Adding new value (" + comp + ") for equiv class " + componentSpecifier);
				componentOccurrences.put(comp, 1);
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
			return componentOccurrences.get(comp);
		}
		
		public int getSize() {
			return componentOccurrences.size();
		}
		
		public class ComponentsEnumeration implements Enumeration<String> {
			private Iterator<String> comps = componentOccurrences.keySet().iterator();

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
	
	/**
	 * An Enumeration of the equivalence classes that help define this group.
	 * @author jimg
	 *
	 */
	public class EquivalenceEnumeration implements Enumeration<Equivalence> {
		Iterator<Equivalence> i = equivalences.iterator();

		@Override
		public boolean hasMoreElements() {
			return i.hasNext();
		}

		@Override
		public Equivalence nextElement() {
			return i.next();
		}
	}

	/**
	 * Use this to access the Equivalences that help define this group.
	 * @return
	 */
	public EquivalenceEnumeration getEquivalences() {
		return new EquivalenceEnumeration();
	}

	/**
	 * Build a new URLGroup and initialize it with a single URL.
	 * 
	 * @param url The URL, a String
	 * @param comps The parsed components of the URL
	 * @param uc Classifications for the parsed components
	 * @throws Exception
	 */
	public URLGroup(String url, URLComponents comps, URLEquivalenceClasses uc) {
		// By definition, each URL in a group has the same URLEquivalenceClasses
		this.classification = uc;
		this.urls = new HashSet<String>();
		this.equivalences = new Vector<Equivalence>();
		
		// Initialize the Vector of Equivalences for this group
		int i = 0;
		for (String c: uc.getClassification()) {
			equivalences.add(new Equivalence(i++, c));
		}
		
		add(url, comps);
	}
	
	public void add(String url, URLComponents comps) {
		urls.add(url);
		
		String[] components = comps.getComponents();
		
		// For the new URL, increment equivalence class counts
		for (Equivalence e: equivalences) {
			int i = e.getComponentNumber();
			log.debug("Adding 'components[" + new Integer(i).toString() + "]': " + components[i]);
			e.add(components[i]);
		}
	}
	
	public String[] getClassification() {
		return classification.getClassification();
	}	
}
