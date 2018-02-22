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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.DateClassification.DatePart;
import opendap.metacat.URLProcessedComponents.Lexeme;

/**
 * Store information about a particular 'equivalence class'. Each URLGroup
 * will likely have several of these. Once all the URLs are processed, we
 * will want to look at how individual patterns are distributed to see if
 * the equivalence class should be split or not. The likely criteria for the
 * splitting a class is that all of the instances fall into a very small
 * number of distinct values with zero outliers. At least that's the idea.
 * 
 * @note When a URL is parsed, it is broken into 'path components' which are
 *       literally the parts between the '/' characters, and writing the
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
public class Equivalence implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private static Logger log = LoggerFactory.getLogger(Equivalence.class);

	private int patternPosition;	// Which of the URL's components
	private String componentValue; // This is the string used to encode the pattern (eg 'dddd')
	private boolean litteral;
	private int totalMembers;		// Total number of data points
	private List<DatePart> dateClassification;
	
	private Map<String, Integer> valueOccurrences; // Occurrences of a given string
	private Map<String, ParsedURL> valueURLs; // Mapping between values for this equivalence and specific URLs
	
	/**
	 * Make a new Equivalence. This does not add a URL to the Equivalence, it
	 * only initializes the 'equivalence class.'
	 * @param n Which parsed component of the URL does this Equivalence correspond to
	 * @param l What is the component value - and is it a pattern?
	 */
	public Equivalence(int n, Lexeme l) {
		dateClassification = new ArrayList<DatePart>();
		
		valueOccurrences = new HashMap<String, Integer>();
		// This is the mapping of values that matched a pattern and the URLs
		// they came from. Once the values are ordered, it's easy to get the
		// URLs in the same order.
		valueURLs = new HashMap<String, ParsedURL>();
		
		patternPosition = n;
		componentValue = l.getValue();
		litteral = !l.isPattern();
		totalMembers = 0;
	}
	
	/** 
	 * Add a new component literal and its source URL to this equivalence 
	 * class.
	 * @param u The Parsed URL
	 */
	public void add(ParsedURL u) {
		++totalMembers;
		
		String comp = u.getComponents()[patternPosition];
		
		// Record the number of occurrences of this particular value for the 
		// Equivalence.
		
		// If comp is in there already, increment its count
		if (valueOccurrences.containsKey(comp)) {
			log.debug("cache hit for " + comp);
			valueOccurrences.put(comp, valueOccurrences.get(comp) + 1);
		}
		else { // Add the comp and set count to one
			log.debug("Adding new value (" + comp + ") for equiv class " + componentValue);
			valueOccurrences.put(comp, 1);
		}
		
		// ... And record the source URL for the particular value if this is
		// a pattern
		if (!litteral)
			valueURLs.put(comp, u);
	}
	
	public int getPatternPosition() {
		return patternPosition;
	}
	
	public String getPattern() {
		return componentValue;
	}

	public boolean isLitteral() {
		return litteral;
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
	 * is false, this must be equal to getTotalMembers().
	 * 
	 * @param comp
	 * @return
	 */
	public int getOccurrences(String comp) {
		return valueOccurrences.get(comp);
	}
	
	/**
	 * How many discreet values exist for this equivalence class?
	 * @return
	 */
	public int getNumberOfValues() {
		return valueOccurrences.size();
	}

	public class Values implements Iterable<String> {
		private Iterator<String> vals = valueOccurrences.keySet().iterator();

		@Override
		public Iterator<String> iterator() {
			return vals;
		}
	}

	public Values getValues() {
		return new Values();
	}
	
	public class SortedValues implements Iterable<DateString> {
		private ArrayList<DateString> sortedKeys;
		private Iterator<DateString> sortedKeysIter;
		
		public SortedValues() {
			// sortedKeys = new ArrayList<DateString>(valueOccurrences.keySet(), dateClassification);
			sortedKeys = new ArrayList<DateString>(valueOccurrences.size());
			for (String d: valueOccurrences.keySet()) 
				sortedKeys.add(new DateString(d, dateClassification));
			
			Collections.sort(sortedKeys);
			sortedKeysIter = sortedKeys.iterator();
		}
		
		@Override
		public Iterator<DateString> iterator() {
			return sortedKeysIter;
		}
		
		public DateString get(int index) {
			return sortedKeys.get(index);
		}
		
		public int size() {
			return sortedKeys.size();
		}

	}
	
	public SortedValues getSortedValues() {
		return new SortedValues();
	}

	public ParsedURL getParsedURL(String comp) {
		return valueURLs.get(comp);
	}

	public void addDateClassification(DatePart dp) {
		dateClassification.add(dp);
	}
	
	public boolean hasDateClassification(DatePart dp) {
		return dateClassification.contains(dp);
	}

	public int getNumberDateClassifications() {
		return dateClassification.size();
	}
	
	public class DateClassifications implements Iterable<DatePart> {
		private Iterator<DatePart> dateParts = dateClassification.iterator();

		@Override
		public Iterator<DatePart> iterator() {
			return dateParts;
		}
		
	}
	
	public DateClassifications getDateClassifications() {
		return new DateClassifications();
	}
}

