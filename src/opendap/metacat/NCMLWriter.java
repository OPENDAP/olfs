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

import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import opendap.metacat.Equivalence.SortedValues;
import opendap.metacat.URLGroup.Equivalences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCMLWriter {
    private static Logger log = LoggerFactory.getLogger(NCMLWriter.class);

	private static final HashSet<String> likelyServerNames = new HashSet<String>();
	
	private static Map<URLGroup, URLGroupFacts> factBase = null;
	
	private static NCMLBuilder multifileNCMLBuilder = null;
	
    /// These are used to format dates so they are human- and xslt-usable
    final static SimpleDateFormat iso_8601_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    final static SimpleDateFormat infoLogSdf = new SimpleDateFormat("HH:mm:ss");        
	
	public static void main(String[] args) {
		// There has to be a better way...
		likelyServerNames.add("opendap");
		likelyServerNames.add("hyrax");
		likelyServerNames.add("dap");
		likelyServerNames.add("data");
		
		CommandLineParser parser = new PosixParser();

		Options options = new Options();

		options.addOption("v", "verbose", false, "Write info to stdout");
		options.addOption("V", "very-verbose", false, "Write NCML to stdout");
		options.addOption("h", "help", false, "Usage information");
		
		options.addOption("n", "groups-name", true, "URLGroups name prefix");
		options.addOption("o", "output", false, "Write NCML files using the groupName name and a counter.");
		// options.addOption("d", "dir", true, "Write NCML files to this directory.");
		
		try {
		    CommandLine line = parser.parse( options, args );

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("NCMLWriter [options] --groups-name <name prefix>", options);
				return;
			}

		    boolean verbose = line.hasOption("verbose");
		    boolean veryVerbose = line.hasOption("very-verbose");
		    PrintStream ps = System.out;
		    
		    // Extract options
		    String groupsName = line.getOptionValue("groups-name");
		    if (groupsName == null || groupsName.isEmpty())
		    	throw new Exception("The calssifier must have a URLGroups file name.");
		    if (verbose)
		    	ps.println("Groups file: " + groupsName);
		    		    		    
		    boolean output = line.hasOption("output");
		    
		    // Build objects
    		DDXRetriever ddxRetriever = new DDXRetriever(true, groupsName);
    		
    		multifileNCMLBuilder = new NCMLBuilder(groupsName, "many_ddx2ncml-1.0.xsl");
    		
			if (verbose)
				ps.println("(" + infoLogSdf.format(new Date()) + ") Reading groups");

		    URLGroups groups = new URLGroups();
		    groups.restoreState(groupsName);
		    
		    factBase = new HashMap<URLGroup, URLGroupFacts>();
		    
			if (verbose)
				ps.println("(" + infoLogSdf.format(new Date()) + ") Begining analysis of groups");

		    // initialize the fact base.
		    Integer count = 0;
		    for (URLGroup group: groups) {
		    	String ddxURL = group.getURLs().get(0).getTheURL();
		    	factBase.put(group, new URLGroupFacts(ddxURL, ddxRetriever.getDDXDoc(ddxURL)));
		    	count++;
		    }
		    
		    // Look at each group and decide if it can be aggregated.
		    /*
		    for (URLGroup group: groups) {
		    	
		    }
		    */
		    
		    if (verbose)
		    	ps.println("(" + infoLogSdf.format(new Date()) + ") " + count.toString() + " groups.");
		    
		    // Find multifile groups
		    count = 0;
		    for (URLGroup group: groups) {
		    	if (group.getURLs().size() > 1) {
		    		factBase.get(group).setIsMultiFile(true);
		    		count++;
		    	}
		    }
		    	
		    if (verbose)
		    	ps.println("(" + infoLogSdf.format(new Date()) + ") " + count.toString() + " multifile groups.");
		    
		    // Find multifile groups with a date equivalence
		    count = 0;
		    for (URLGroup group: groups) {
		    	if (factBase.get(group).getIsMultiFile() && group.getDateEquivalence() != null) {
		    		factBase.get(group).setIsTimeSeries(true);
		    		
		    		SortedValues sortedDates = group.getDateEquivalence().getSortedValues();
	    			  
					DateString first = sortedDates.get(0);
					DateString last = sortedDates.get(sortedDates.size() - 1);
					
					factBase.get(group).setFirstDate(iso_8601_sdf.format(first.getDate()));
					factBase.get(group).setLastDate(iso_8601_sdf.format(last.getDate()));
					
					count++;
		    	}
		    }
		    
		    if (verbose)
		    	ps.println("(" + infoLogSdf.format(new Date()) + ") " + count.toString() + " multifile groups with date equivalence classes.");
		    
		    // For groups that have multiple files, find various pathnames
			for (URLGroup group : groups) {
				String DDXURL = factBase.get(group).getFirstDDXURL();
				int serverNameEndPosition = findServerNameEnd(DDXURL);
				log.debug("serverNameEndPosition: " + serverNameEndPosition);

				int serverRootPosition = findServerRootPosition(group, serverNameEndPosition);
				log.debug("serverRootPosition: " + serverRootPosition);

				factBase.get(group).setServerRootPosition(serverRootPosition);

				int dataRootPosition = findDataRootPosition(group, serverNameEndPosition);
				log.debug("dataRootPosition: " + dataRootPosition);

				log.debug("DDX (length: " + DDXURL.length() + "): " + DDXURL);
				int len = DDXURL.length();
				// The monkey shines here guard against the case where a single
				// file dataset's dataRootPosition is past the en of the URL.
				String dataRoot = DDXURL.substring(serverRootPosition, (dataRootPosition > len) ? len : dataRootPosition);
				log.debug("DDX: " + DDXURL + "; dataset scan: " + dataRoot);

				factBase.get(group).setDatasetRoot(dataRoot);
			}

		    // Check for things that are unique, like dataRoot - for each group
		    // is its value of dataRoot Unique?
			
			// Put all the dataRoot paths in a list
		    List<String> dataRootList= new ArrayList<String>();;
		    for (URLGroup group: groups) {
		    	dataRootList.add(factBase.get(group).getDatasetRoot());
		    }
		    
		    // ... now test to see if the first and last occurrence are the same
		    for (URLGroup group: groups) {
		    	String dr = factBase.get(group).getDatasetRoot();
		    	if (dataRootList.indexOf(dr) == dataRootList.lastIndexOf(dr))
		    		factBase.get(group).setIsDatasetRootUnique(true);
		    	else
		    		factBase.get(group).setIsDatasetRootUnique(false);
		    }
		    
		    // now build some NCML
			if (verbose)
				ps.println("(" + infoLogSdf.format(new Date()) + ") Building NCML");

		    Integer output_counter = 0;
		    for (URLGroup group: groups) {
		    	String ncmlDoc = null;
		    	
		    	if (factBase.get(group).getIsTimeSeries()) {
		    		if (verbose)
		    			ps.println("(" + infoLogSdf.format(new Date()) + ") Start building NCML");
		    		
		    		ncmlDoc = buildExplicitNCMLForTimeSeries(group);
			    	
		    		if (verbose)
		    			ps.println("(" + infoLogSdf.format(new Date()) + ") End building NCML");

		    		if (veryVerbose)
						ps.println("(" + infoLogSdf.format(new Date()) + ") NCML: " + ncmlDoc);
		    	}
				
				if (output && ncmlDoc != null && !ncmlDoc.isEmpty()) {
					output_counter++;
					FileWriter fw;
					String dr = factBase.get(group).getDatasetRoot();
					dr = dr.replace('/', '_');
					if (dr.charAt(dr.length()-1) == '_')
						dr = dr.substring(0, dr.length()-1);
					if (factBase.get(group).getIsDatasetRootUnique())
						fw = new FileWriter(dr + ".ncml");
					else
						fw = new FileWriter(dr + "_" + output_counter.toString() + ".ncml");
					
					fw.write(ncmlDoc);
					fw.close();
				}
				
				// delete document just written 
				// ncmlDoc = null;
		    }
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
	}

	private static String buildExplicitNCMLForTimeSeries(URLGroup group) throws Exception {
		
		String urlDateFileTuples = buildURLDateFileTuples(group.getDateEquivalence(), factBase.get(group).getServerRootPosition());
		
		String[] params = new String[4];
		params[0] = "date_range";
		params[1] = factBase.get(group).getFirstDate() + " " + factBase.get(group).getLastDate();
		params[2] = "url_date_file";
		params[3] = urlDateFileTuples;
		
		// build the NCML
		return multifileNCMLBuilder.getNCML(factBase.get(group).getFirstDDXURL(), factBase.get(group).getFirstDDXDoc(), params);
		
	}
	
	private static String buildURLDateFileTuples(Equivalence dateEquiv, int dataRootPosition) {
		SortedValues sortedDates = dateEquiv.getSortedValues();
		StringBuilder urlDateFileTuples = new StringBuilder("");
		for (DateString d : sortedDates) {
			String date = iso_8601_sdf.format(d.getDate());
			String url =  dateEquiv.getParsedURL(d.getDateString()).getTheURL();
			url = url.substring(0, url.lastIndexOf('.'));
			
			// Add the two offsets to get the filename
			String file = url.substring(dataRootPosition);
			
			//urlDateFileTuples += url + "*" + date + "*" + file + " ";
			urlDateFileTuples.append(url);
			urlDateFileTuples.append("*");
			urlDateFileTuples.append(date);
			urlDateFileTuples.append("*");
			urlDateFileTuples.append(file);
			urlDateFileTuples.append(" ");
		}
		return urlDateFileTuples.toString();
	}

	/**
	 * Guess at the place in the list of equivalence classes where the server's
	 * name ends and the DataRoot starts. Pure hackery... use the offset 
	 * returned by this function with substring to cut away the unwanted part
	 * of the URL.
	 * 
	 * @note Here's how it works:
	 * http://machine/tomcat/servlet/data/nc/fnoc1.nc
	 * ^             ^              ^
	 * machine       tomcat         Data Root
	 * This code returns the distance between 'tomcat' and 'Data Root'
	 * 
	 * @note Uses a canned set of values to determine where the tomcat 
	 * context and stuff end and the data root starts. 
	 * 
	 * @return Position offset from the end of 'http://machine.name/' to '/' 
	 * that marks the start of the DataRoot directory.
	 */
	private static int findServerRootPosition(URLGroup group, int serverNameEndPosition) {
		int count = serverNameEndPosition + 1;	// Assume every URL has '/' following the machine name

		Equivalences equivs = group.getEquivalences();
		for (Equivalence e: equivs) {
			log.debug("In findDataRoot; e.getPattern(): " + e.getPattern() 
					+ "; e.isLitteral(): " + e.isLitteral() 
					+ "; likelyServerNames.contains(e.getPattern()): " + likelyServerNames.contains(e.getPattern()));

			if (e.isLitteral() && likelyServerNames.contains(e.getPattern()))
				count += e.getPattern().length() + 1; // +1 includes the tailing '/' for this component in the URL
			else
				return count;
		}
		
		return count;		
	}
	
	/**
	 * Guess at the place in the list of equivalence classes where the group's
	 * Dataset scan directory starts. Pure hackery... use the offset 
	 * returned by this function with substring to cut away the unwanted part
	 * of the URL.
	 * 
	 * @note Uses the idea that the once the groups' URLs' components start
	 * to change those components are part of the dataset. Thus if a path is
	 * formed of those components that don't change that is the path leading
	 * up to the dataset and can serve as the dataset scan path. This is
	 * useful both to make the ncml datasetScan element but also to make a
	 * readable filename and title for the NCML itself since those directory
	 * names often have meaning.
	 * 
	 * @return Position offset from the end of 'http://machine.name/' to '/' 
	 * that marks the start of the DataRoot directory.
	 */
	private static int findDataRootPosition(URLGroup group, int serverNameEndPosition) throws Exception {
		int count = serverNameEndPosition + 1;	// Assume every URL has '/' following the machine name
		
		Equivalences equivs = group.getEquivalences();
		for (Equivalence e: equivs) {
			if (e.getNumberOfValues() != 1)
				return count;
			else
				count += e.getPattern().length() + 1;
		}
		
		return count;
	}
	
	/**
	 * Where in the string that holds the URL does the machine name end? 
	 * 
	 * @param url
	 * @return
	 */
	private static int findServerNameEnd(String url) {
		return url.indexOf('/', url.indexOf("//") + 2);
	}
	
	/**
	 * Look at the list of equivalence classes and find the last one in the list
	 * that has only one value. Use this as the root of the group within the
	 * server's file system.
	 * 
	 * @note Assume that the equivalences correspond to slash-separated 
	 * pathname components. If the equivalences are the same all the way to
	 * the file level, then this must not be a multifile dataset. So we test
	 * for that right away and throw an exception.  
	 * 
	 * @note Not used now, but this could be combined with the scan element
	 * to make much smaller NCML files.
	 * 
	 * @param group
	 * @return 
	 */
	@SuppressWarnings("unused")
	private int findCommonDirectory(URLGroup group) throws Exception {
		if (group.getURLs().size() == 1)
			throw new Exception("A URLGroup with only one instance was passed to findCommonDirectory");
		
		Equivalence previous_e = null;
		int previous_members = 0;
		Equivalences equivs = group.getEquivalences();
		for (Equivalence e: equivs) {
			previous_e = e;
			if (previous_members != 0 && e.getTotalMembers() != previous_members)
				throw new Exception("findCommonDirectory expected that all equivalence classes within a group would have the same number of member elements");
			previous_members = e.getTotalMembers();
			
			if (e.getNumberOfValues() != 1)
				return previous_e.getPatternPosition();
		}
		
		return previous_e.getPatternPosition();
	}
}