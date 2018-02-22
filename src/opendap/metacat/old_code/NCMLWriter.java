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

package opendap.metacat.old_code;

import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

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
		    Integer output_counter = 0;
		    
		    // Build objects
    		DDXRetriever ddxRetriever = new DDXRetriever(true, groupsName);
    		
    		NCMLBuilder multifileNCMLBuilder = new NCMLBuilder(groupsName, "many_ddx2ncml-1.0.xsl");
    		
		    URLGroups groups = new URLGroups();
		    groups.restoreState(groupsName);
		    
		    for (URLGroup group: groups) {

		    	// Only build NCML for multifile datasets
		    	if (group.getURLs().size() > 1) {
		    		
		    		// getURLs() returns an instance of URLs, an Iterable object with instances of ParsedURL.
			    	// Get the first DDX to use as a template 
		    		String ddxUrl = group.getURLs().get(0).getTheURL();
		    		String ddxDoc = ddxRetriever.getDDXDoc(ddxUrl);

		    		Equivalence dateEquiv = group.getDateEquivalence();
		    		if (dateEquiv != null) {
		    			// We have a many-to-one dataset with varying dates
			    		if (verbose)
			    			ps.println("Dataset DDX (many-to-one, date) (" + infoLogSdf.format(new Date()) + "): " + ddxUrl);

		    			SortedValues sortedDates = dateEquiv.getSortedValues();
						DateString first = sortedDates.get(0);
						DateString last = sortedDates.get(sortedDates.size() - 1);
						String dateRange = iso_8601_sdf.format(first.getDate()) + " " + iso_8601_sdf.format(last.getDate());
						
						int serverNameEndPosition = findServerNameEnd(ddxUrl);
						int dataRootPosition = findDataRoot(group);
						int datasetScanPosition = findDatasetScan(group);
						
						String datasetScan = ddxUrl.substring(serverNameEndPosition + dataRootPosition, serverNameEndPosition + datasetScanPosition);
						log.debug("DDX: " + ddxUrl + "; dataset scan: " + datasetScan);
						
						String urlDateFileTuples = buildURLDateFileTuples(dateEquiv, sortedDates, serverNameEndPosition, dataRootPosition);
						
						String[] params = new String[4];
						params[0] = "date_range";
						params[1] = dateRange;
						params[2] = "url_date_file";
						params[3] = urlDateFileTuples;
						
						if (verbose)
							ps.println("(" + infoLogSdf.format(new Date()) + ") Built parameters for xslt");
						
						// build the NCML
						String ncmlDoc = multifileNCMLBuilder.getNCML(ddxUrl, ddxDoc, params);
			    		
			    		if (veryVerbose)
			    			ps.println("(" + infoLogSdf.format(new Date()) + ") NCML: " + ncmlDoc);
			    		
			    		if (output) {
			    			output_counter++;
			    			FileWriter fw = new FileWriter(groupsName + "_" + output_counter.toString() + ".ncml");
			    			fw.write(ncmlDoc);
			    			fw.close();
			    		}
		    		}
		    		else {
		    			// ... with varying parameters other than date
		    		}
		    	}
		    }
		  
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
	}

	private static String buildURLDateFileTuples(Equivalence dateEquiv, SortedValues sortedDates, int serverNameEndPosition, int dataRootPosition) {
		String urlDateFileTuples = "";
		for (DateString d : sortedDates) {
			String date = iso_8601_sdf.format(d.getDate());
			String url =  dateEquiv.getParsedURL(d.getDateString()).getTheURL();
			url = url.substring(0, url.lastIndexOf('.'));
			
			// Add the two offsets to get the filename
			String file = url.substring(serverNameEndPosition + dataRootPosition);
			
			urlDateFileTuples += url + "*" + date + "*" + file + " ";
		}
		return urlDateFileTuples;
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
	private static int findDataRoot(URLGroup group) {
		int count = 1;	// Assume every URL has '/' following the machine name

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
	private static int findDatasetScan(URLGroup group) throws Exception {
		int count = 1;	// Assume every URL has '/' following the machine name
		
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