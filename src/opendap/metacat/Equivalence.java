package opendap.metacat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.DateClassification.DatePart;

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
 * @see URLProcessedComponents
 * @author jimg
 * 
 */
public class Equivalence {
	
    private static Logger log = LoggerFactory.getLogger(Equivalence.class);

	int componentNumber;	// Which of the URL's components
	String componentValue; // This is the string used to encode the pattern (eg 'dddd')
	boolean isPattern;
	int totalMembers;		// Total number of data points
	Map<String, Integer> componentOccurrences; // Occurrences of a given string
	Set<DatePart> dateClassification;
	
	public Equivalence(int n, String s, boolean p) {
		componentOccurrences = new HashMap<String, Integer>();
		dateClassification = new HashSet<DatePart>();
		componentNumber = n;
		componentValue = s;
		isPattern = p;
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
			log.debug("Adding new value (" + comp + ") for equiv class " + componentValue);
			componentOccurrences.put(comp, 1);
		}	
	}
	
	public int getComponentNumber() {
		return componentNumber;
	}
	
	public String getComponentValue() {
		return componentValue;
	}

	public boolean isPattern() {
		return isPattern;
	}
	
	/**
	 * How many URLs contributed to this class? Should be the same as
	 * URLGroup's totalMembers.
	 * @return
	 */
	public int getTotalMembers() {
		return totalMembers;
	}
	
	/**
	 * How many times does the value 'comp' show up? For a pattern, this
	 * can be any number between 1 and getTotalMembers(). If getIsPattern()
	 * is false, ths must be equal to getTotalMembers().
	 * 
	 * @param comp
	 * @return
	 */
	public int getOccurrences(String comp) {
		return componentOccurrences.get(comp);
	}
	
	/**
	 * How many discreet values exist for this equivalence class?
	 * @return
	 */
	public int getNumberOfValues() {
		return componentOccurrences.size();
	}

	public class Components implements Iterable<String> {
		private Iterator<String> comps = componentOccurrences.keySet().iterator();

		@Override
		public Iterator<String> iterator() {
			return comps;
		}
	}

	public Components getComponents() {
		return new Components();
	}
	
	public void addDateClassification(DatePart dp) {
		dateClassification.add(dp);
	}
	
	public boolean hasDateClassification(DatePart dp) {
		return dateClassification.contains(dp);
	}
	
	public Set<DatePart> getDateClassification() {
		return dateClassification;
	}
}

