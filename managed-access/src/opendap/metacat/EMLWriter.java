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

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import opendap.metacat.Equivalence.SortedValues;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

public class EMLWriter {
    private static Logger log = LoggerFactory.getLogger(EMLWriter.class);

    /// This is the prefix for all the document ids made up of DDX URLs
    final static String docidScope = "opendap";
    
    /// If metacat needs an explicit schema for our generated EML, use this.
    final static String docidSchema = "/Users/jimg/src/eml-2.10/eml.xsd";
    
    /// These are used to format dates so they are human- and xslt-usable
    final static SimpleDateFormat iso_8601_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    final static SimpleDateFormat infoLogSdf = new SimpleDateFormat("HH:mm:ss");
        
    /// Login credentials for metacat.
    private static String metacatUsername = "uid=jimg,o=unaffiliated,dc=ecoinformatics,dc=org";
    private static String metacatPassword = "p7th0n";
    
    // Increment once for each insert
    private static Integer metacatId = 0;
    // This should change rarely
    private static String metacatRevision = "2";
        
	
	public static void main(String[] args) {

		CommandLineParser parser = new PosixParser();

		Options options = new Options();

		options.addOption("v", "verbose", false, "Write info to stdout");
		options.addOption("V", "very-verbose", false, "Write EML to stdout");
		options.addOption("h", "help", false, "Usage information");
		
		options.addOption("n", "groups-name", true, "URLGroups name prefix");
		options.addOption("o", "output", false, "Write eml files using the groupName/ddxURL.");
		
		options.addOption("d", "ddx-url", true, "Write EML for the dataset found at this (DDX) URL.");
		// options.addOption("d", "dir", true, "Write eml files to this directory.");
		
		options.addOption("m", "metacat-url", true, "Metacat instance");
		
		try {
		    CommandLine line = parser.parse( options, args );

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("EMLWriter [options] [--groups-name <name prefix> | --ddx-url <url>]", options);
				return;
			}

		    boolean verbose = line.hasOption("verbose");
		    boolean veryVerbose = line.hasOption("very-verbose");
		    PrintStream ps = System.out;
		    
		    // Extract options
		    String ddxURL = line.getOptionValue("ddx-url");
		    String groupsName = line.getOptionValue("groups-name");
		    if (groupsName == null && ddxURL == null)
		    	throw new Exception("EML Writter must have either a URLGroups file name or a DDX URL.");
		    if (verbose) {
		    	if (groupsName != null)
		    		ps.println("Groups file: " + groupsName);
		    	else
		    		ps.println("DDX URL: " + ddxURL);
		    }
		    		    
		    boolean useMetacat = false;
		    String metacatUrl = line.getOptionValue("metacat-url");
		    if (metacatUrl != null && !metacatUrl.isEmpty()) {
		    	useMetacat = true;
		    	if (verbose)
		    		ps.println("Metacat URL set to: " + metacatUrl);
		    }
		    
		    boolean output = line.hasOption("output");
		    Integer output_counter = 0;
		    
		    // Build objects
    		DDXRetriever ddxRetriever = new DDXRetriever(true, groupsName);
    		
       		EMLBuilder simpleEmlBuilder = new EMLBuilder(groupsName);
    		EMLBuilder complexEmlBuilder = new EMLBuilder(groupsName, "many_ddx2eml-1.0.xsl");
    		
    		Metacat metacat = null;
    		if (useMetacat) {
    			metacat = ConnectToMetacat(verbose, metacatUrl, metacatUsername, metacatPassword);
    		}
    		    
    		if (ddxURL != null) {
	    		// Use a simple EML builder with the default DDX to EML xslt
	    		if (verbose)
	    			ps.println("Dataset DDX (one-to-one) (" + infoLogSdf.format(new Date()) + "): " + ddxURL);

	    		String ddxDoc = ddxRetriever.getDDXDoc(ddxURL);
	    		
	    		String emlDoc = simpleEmlBuilder.getEML(ddxURL, ddxDoc);

	    		if (veryVerbose)
	    			ps.println("(" + infoLogSdf.format(new Date()) + ") EML: " + emlDoc);

	    		if (output) {
	    			output_counter++;
					String name = ddxURL.substring(ddxURL.lastIndexOf('/')+1) + ".eml";
					if (verbose)
						ps.println("Writing " + name);
	    			FileWriter fw = new FileWriter(name);
	    			fw.write(emlDoc);
	    			fw.close();
	    		}

	    		if (metacat != null)
	    			insertEML(metacat, verbose, ddxURL, emlDoc);
   			
    		}
			else if (groupsName != null) {
				URLGroups groups = new URLGroups();
				groups.restoreState(groupsName);

				for (URLGroup group : groups) {
					// getURLs() returns an instance of URLs, an Iterable object
					// with instances of ParsedURL.
					// For both the one-ddx and many-ddx cases use the first
					// DDX, so grab it here
					String ddxUrl = group.getURLs().get(0).getTheURL();
					if (verbose)
						ps.println("Reading DDX from: " + ddxUrl);
					String ddxDoc = ddxRetriever.getDDXDoc(ddxUrl);
					if (veryVerbose) {
						ps.println("DDX:");
						ps.println(ddxDoc);
					}

					// one URL group?
					if (group.getURLs().size() == 1) {
						// Use a simple EML builder with the default DDX to EML
						// xslt
						if (verbose)
							ps.println("Dataset DDX (one-to-one) (" + infoLogSdf.format(new Date()) + "): " + ddxUrl);

						String emlDoc = simpleEmlBuilder.getEML(ddxUrl, ddxDoc);

						if (veryVerbose)
							ps.println("(" + infoLogSdf.format(new Date()) + ") EML: " + emlDoc);

						if (output) {
							output_counter++;
							String name = groupsName + "_" + output_counter.toString() + ".eml";
							if (verbose)
								ps.println("Writing " + name);
							FileWriter fw = new FileWriter(name);
							fw.write(emlDoc);
							fw.close();
						}

						if (metacat != null)
							insertEML(metacat, verbose, ddxUrl, emlDoc);
					}
					else {
						// Otherwise we have a many-to-one kind of dataset
						Equivalence dateEquiv = group.getDateEquivalence();
						if (dateEquiv != null) {
							// We have a many-to-one dataset with varying dates
							if (verbose)
								ps.println("Dataset DDX (many-to-one, date) (" + infoLogSdf.format(new Date()) + "): " + ddxUrl);

							// Build the parameters to pass into the XSLT
							// processor
							String filename = ddxUrl.substring(ddxUrl.lastIndexOf('/') + 1);

							SortedValues sortedDates = dateEquiv.getSortedValues();
							DateString first = sortedDates.get(0);
							DateString last = sortedDates.get(sortedDates.size() - 1);
							String dateRange = iso_8601_sdf.format(first.getDate()) + " " + iso_8601_sdf.format(last.getDate());

							String urlDateFileTuples = "";
							for (DateString d : sortedDates) {
								String date = iso_8601_sdf.format(d.getDate());
								String url = dateEquiv.getParsedURL(d.getDateString()).getTheURL();
								url = url.substring(0, url.lastIndexOf('.'));
								String file = url.substring(url.lastIndexOf('/') + 1);

								urlDateFileTuples += url + "*" + date + "*" + file + " ";
							}

							String[] params = new String[6];
							params[0] = "filename";
							params[1] = filename;
							params[2] = "date_range";
							params[3] = dateRange;
							params[4] = "url_date_file";
							params[5] = urlDateFileTuples;

							if (verbose)
								ps.println("(" + infoLogSdf.format(new Date()) + ") Built parameters for xslt");

							// Get the EML
							String emlDoc = complexEmlBuilder.getEML(ddxUrl, ddxDoc, params);

							if (veryVerbose)
								ps.println("(" + infoLogSdf.format(new Date()) + ") EML: " + emlDoc);

							if (output) {
								output_counter++;
								String name = groupsName + "_" + output_counter.toString() + ".eml";
								if (verbose)
									ps.println("Writing " + name);
								FileWriter fw = new FileWriter(name);
								fw.write(emlDoc);
								fw.close();
							}

							if (metacat != null)
								insertEML(metacat, verbose, ddxUrl, emlDoc);
						}
						else {
							throw new Exception("EML Writer does not yet know how to process datasets that vary other than by date.");
							// ... with varying parameters other than date
						}
					}
				}
			}
			else {
				throw new Exception("EML Writer requires either a URLGroup .ser file name or a DDX URL.");
			}
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		
		
	}
	
	private static Metacat ConnectToMetacat(boolean verbose, String metacatURL, String user, String pw) throws Exception {
		if (verbose)
			log.info("Building metacat cache connection: " + metacatURL + "(" + user + ", " + pw + ").");
		try {
			Metacat metacat = MetacatFactory.createMetacatConnection(metacatURL);

			String response = metacat.login(user, pw);

			if (verbose) {
			log.info("login(): response=" + response);
			log.info("login(): Session ID=" + metacat.getSessionId());
			}
			
			String id = metacat.getLastDocid(docidScope);
			// if not null, use the value, else use the initial of 0
			if (!id.isEmpty()) {
				String[] parts = id.split(".");
				metacatId = new Integer(parts[1]);
				if (verbose) {
					log.info("getLastDocid(): =" + id);
					log.info("\t...setting metacatId to: " + metacatId);
				}
			}
			
			return metacat;
		}
		catch (MetacatAuthException mae) {
			throw new Exception("Metacat authorization failed:\n" + mae.getMessage());
		}
		catch (MetacatInaccessibleException mie) {
			throw new Exception("Metacat inaccessible:\n" + mie.getMessage());
		}
		catch (MetacatException mie) {
			throw new Exception("Metacat error:\n" + mie.getMessage());
		}
	}
	
	/**
	 * Return a metacat document id. A metacat document id must be of the form
	 * <string>.<string>.<digit> Where the dots are literal and <string> must
	 * not contain any dots. Furthermore, only the last two parts are used when
	 * accessing the document id; the first <string> is ignored. This method
	 * returns a document id by combining the value of the class' docidScope
	 * with an integer and a '1'. Each call to the function returns a different
	 * document id (the integer that makes up the second component is
	 * incremented).
	 * 
	 * @return A document id string suitable for use with metacat
	 */
    private static String getDocid() {
    	++metacatId;
    	return docidScope + "." + metacatId.toString() + "." + metacatRevision;
    }
    
    /**
     * Insert the EML document into Metacat. This assumes that the EML document
     * is indexed using a DDX URL. 
     * 
     * @param emlString
     */
	private static void insertEML(Metacat metacat, boolean verbose, String ddxUrl, String emlString) throws Exception {

		String docid = getDocid();
		if (verbose)
			log.info("Storing " + ddxUrl + "(docid:" + docid + ") in metacat.");

		try {
			String response = metacat.insert(docid, new StringReader(emlString), null);
			
			if (verbose)
				log.info("Metacat's response to the adding '" + docid + "': " + response);
		}
		catch (FileNotFoundException e) {
			log.error("Could not open the file: " + docidSchema);
			throw new Exception("FileNotFoundException: " + e.getMessage());
		}
		catch (InsufficientKarmaException e) {
			log.error("Error storing the response: Insufficent rights for the operation: " + e.getMessage());
			throw new Exception("InsufficientKarmaException: " + e.getMessage());
		}
		catch (MetacatInaccessibleException e) {
			log.error("Error storing the response: Error reading the xml document: " + e.getMessage());
			throw new Exception("MetacatInaccessibleException: " + e.getMessage());
		}
		// These errors are not fatal
		catch (MetacatException e) {
			log.error("Error storing the response [" + ddxUrl + "]: " + e.getMessage());
			log.error("The EML document: [" + emlString + "]");
		}
		catch (IOException e) {
			log.error("Error storing the response: Unknown error [" + ddxUrl + "]: " + e.getMessage());
		}
	}
}
