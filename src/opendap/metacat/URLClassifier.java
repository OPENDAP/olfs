package opendap.metacat;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.URLGroup.DatePart;
import opendap.metacat.URLGroup.Equivalence;
import opendap.metacat.URLGroup.EquivalenceEnumeration;
import opendap.metacat.URLGroup.URLEnumeration;
import opendap.metacat.URLGroup.Equivalence.ComponentsEnumeration;

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
			int numberOfUrls = c.pass1();
			
			ps.println("Completed pass 1: " + (new Date()).toString());
			
			c.pass2();
			
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
	public int pass1() throws Exception {

    	Enumeration<String> ddxURLs = ddxSource.getCache().getLastVisitedKeys();
    	
		// Special case for the first URLGroup (because using 'for' with an
		// iterator fails when the iterator instance is null
    	if (ddxURLs.hasMoreElements()) {
    		String ddxURL = ddxURLs.nextElement();
			URLComponents componets = new URLComponents(ddxURL);
			URLProcessedComponents classification = new URLProcessedComponents(componets);

    		URLGroup group = new URLGroup(ddxURL, componets, classification);
    		
    		groups.add(group);
    	}
		
    	int numberOfUrls = 1;
    	
		while (ddxURLs.hasMoreElements()) {
			++numberOfUrls;
			String ddxURL = ddxURLs.nextElement();

			try {
				URLComponents componets = new URLComponents(ddxURL);
				URLProcessedComponents classification = new URLProcessedComponents(componets);
				String[] classificationLexemes = classification.getLexemes();
				// Look for an existing equivalence class for this ddxURL. If
				// one matches, add the ddxURL to it and stop looking for more
				// matches.
				boolean found = false;
				for (URLGroup group : groups) {
					if (Arrays.equals(classificationLexemes, group.getClassifications().getLexemes())) {
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
	
	/**
	 * Look for dates in the equivalence classes that define each group
	 * 
	 * @throws Exception
	 */
	public void pass2() throws Exception {

		for (URLGroup group : groups) {
			EquivalenceEnumeration equivs = group.getEquivalences();
			while (equivs.hasMoreElements()) {
				Equivalence e = equivs.nextElement();
				if (e.getIsPattern()) {
					classifyPotentialDate(e);
				}
			}

		}
	}
	
	private void classifyPotentialDate(Equivalence e) {
		if (isYear(e))
			e.addDateClassification(DatePart.year);
		else if (isMonth(e))
			e.addDateClassification(DatePart.month);
		else if (isMonth2(e))
			e.addDateClassification(DatePart.month1);
		else if (isDay(e))
			e.addDateClassification(DatePart.day);
		else if (isDayNum(e))
			e.addDateClassification(DatePart.daynum);
		else if (isYearMonthDay(e)) {
			e.addDateClassification(DatePart.year);
			e.addDateClassification(DatePart.month);
			e.addDateClassification(DatePart.day);
		}
		else if (isYearDayNumTime(e)) {
			e.addDateClassification(DatePart.year);
			e.addDateClassification(DatePart.daynum);
			e.addDateClassification(DatePart.hours);
			e.addDateClassification(DatePart.minutes);
			e.addDateClassification(DatePart.seconds);
		}
		else if (isYearDayNum(e)) {
			e.addDateClassification(DatePart.year);
			e.addDateClassification(DatePart.daynum);
		}
		else if (isYearDayNum2Time(e)) {
			e.addDateClassification(DatePart.year2);
			e.addDateClassification(DatePart.daynum);
			e.addDateClassification(DatePart.hours);
			e.addDateClassification(DatePart.minutes);
			e.addDateClassification(DatePart.seconds);
		}
		else if (isYearDayNum2(e)) {
			e.addDateClassification(DatePart.year2);
			e.addDateClassification(DatePart.daynum);
		}
		else if (isYearMonth(e)) {
			e.addDateClassification(DatePart.year);
			e.addDateClassification(DatePart.month);
		}
	}
	
	final int minimumYear = 1970;
	final int minimumDayNum = 0;
	final int maximumDayNum = 366;
	final int minimumMonth = 0;
	final int maximumMonth = 12;
	final int minimumDay = 0;
	final int maximumDay = 31;
	final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
	final int maximumHour = 23;
	final int maximumMinute = 59;
	final int maximumSecond = 59;
	
	private boolean isYear(Equivalence e) {
		log.debug("Is this a year? (" + e.getComponentSpecifier() + ")");
		// If the pattern isn't right, fail without looking at values
		if (!e.getComponentSpecifier().equals("dddd"))
			return false;

		// If there are any values outside the allowed range, fail

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			log.debug("Is '" + sv +"' a valid year?");
			if (!isValidYear(sv))
				return false;
		}

		// If the pattern is OK and the values are all OK, return true
		return true;
	}

	private boolean isDayNum(Equivalence e) {
		// The pattern must match and there are only a certain number of
		// discrete values possible, bounded by the number of days in the year.
		// This is not testing the values, just that the number of values
		// found is sane.
		if (!e.getComponentSpecifier().equals("ddd") || !(e.getSize() == 366 || e.getSize() == 365))
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			if (!isValidDayNum(sv))
				return false;
		}

		return true;
	}

	private boolean isDay(Equivalence e) {
		if (!e.getComponentSpecifier().equals("dd") || e.getSize() != 31)
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			if (!isValidDay(sv))
				return false;
		}
		
		return true;
	}

	private boolean isMonth(Equivalence e) {
		if (!e.getComponentSpecifier().equals("dd") || e.getSize() != 12)
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			if (!isValidMonth(sv))
				return false;
		}
		
		return true;
	}

	// Should this be here? This should be used in context when the preceding
	// Equivalence is a year.
	private boolean isMonth2(Equivalence e) {
		// There's no point in test the number of discrete values since 0-9 are
		// all that's possible and all are valid when a single digit is used
		// for Jan to Sept.
		if (!e.getComponentSpecifier().equals("d") || e.getSize() != 10)
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			if (!isValidMonth(sv))
				return false;
		}
		
		return true;
	}
	
	private boolean isYearMonthDay(Equivalence e) {
		if (!e.getComponentSpecifier().equals("dddddddd")) // Eight digits
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			String ysv = sv.substring(0, 4);
			if (!isValidYear(ysv))
				return false;
			String msv = sv.substring(4, 6);
			if (!isValidMonth(msv))
				return false;
			String dsv = sv.substring(6);
			if (!isValidDay(dsv))
				return false;
		}
		
		return true;
	}

	private boolean isYearMonth(Equivalence e) {
		if (!e.getComponentSpecifier().equals("dddddd")) // Six digits
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			String ysv = sv.substring(0, 4);
			if (!isValidYear(ysv))
				return false;
			String msv = sv.substring(4);
			if (!isValidMonth(msv))
				return false;
		}
		
		return true;
	}

	private boolean isYearDayNum(Equivalence e) {
		if (!e.getComponentSpecifier().equals("ddddddd")) // 7 digits
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			String ysv = sv.substring(0, 4);
			if (!isValidYear(ysv))
				return false;
			String dnsv = sv.substring(4, 7);
			if (!isValidDayNum(dnsv))
				return false;
		}
		
		return true;
	}
	
	private boolean isYearDayNumTime(Equivalence e) {
		if (!e.getComponentSpecifier().equals("ddddddddddddd")) // 7 digits followed by 6 digits
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			String ysv = sv.substring(0, 4);
			if (!isValidYear(ysv))
				return false;
			String dnsv = sv.substring(4, 7);
			if (!isValidDayNum(dnsv))
				return false;
			String hsv = sv.substring(7, 9);
			if (!isValidHour(hsv))
				return false;
			String msv = sv.substring(9, 11);
			if (!isValidMinute(msv))
				return false;
			String ssv = sv.substring(11);
			if (!isValidSecond(ssv))
				return false;			
		}
		
		return true;
	}

	private boolean isYearDayNum2(Equivalence e) {
		if (!e.getComponentSpecifier().equals("ddddddddddd")) // 5 digits (two for year) followed by 6 digits
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			String ysv = sv.substring(0, 2);
			if (!isValidYear2(ysv))
				return false;
			String dnsv = sv.substring(2, 4);
			if (!isValidDayNum(dnsv))
				return false;
		}
		
		return true;
	}

	private boolean isYearDayNum2Time(Equivalence e) {
		if (!e.getComponentSpecifier().equals("ddddddddddd")) // 5 digits (two for year) followed by 6 digits
			return false;

		ComponentsEnumeration c = e.getComponents();
		while (c.hasMoreElements()) {
			String sv = c.nextElement();
			String ysv = sv.substring(0, 2);
			if (!isValidYear2(ysv))
				return false;
			String dnsv = sv.substring(2, 5);
			if (!isValidDayNum(dnsv))
				return false;
			String hsv = sv.substring(5, 7);
			if (!isValidHour(hsv))
				return false;
			String msv = sv.substring(7, 9);
			if (!isValidMinute(msv))
				return false;
			String ssv = sv.substring(9);
			if (!isValidSecond(ssv))
				return false;
		}
		
		return true;
	}

	private boolean isValidDay(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < minimumDay || value > maximumDay);
	}

	private boolean isValidDayNum(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < minimumDayNum || value > maximumDayNum);
	}

	private boolean isValidMonth(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < minimumMonth || value > maximumMonth);
	}

	private boolean isValidYear(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < minimumYear || value > currentYear);
	}

	private boolean isValidYear2(String sv) {
		int value = new Integer(sv).intValue();
		return !(value > (currentYear/100) && value < (minimumYear/100));
	}

	private boolean isValidHour(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < 0|| value > maximumHour);
	}

	private boolean isValidMinute(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < 0 || value > maximumMinute);
	}

	private boolean isValidSecond(String sv) {
		int value = new Integer(sv).intValue();
		return !(value < 0 || value > maximumSecond);
	}

	private void printClassifications(PrintStream ps, boolean print_all_data, boolean print_histogram) {
		Integer i = 0;
		for(URLGroup group: groups) {
			ps.print(i.toString() + ": ");
			for(String comp: group.getClassifications().getLexemes())
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
					if (e.getDateClassification().size() > 0) {
						ps.print("\t\tFound a potentail date:");
						for (DatePart dp: e.getDateClassification())
							ps.print(" " + dp.toString());		
						ps.println();
					}
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
