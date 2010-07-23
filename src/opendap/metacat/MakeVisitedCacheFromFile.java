package opendap.metacat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Goofy utility class to make a ConcurrentHashMap, serialized, that can be
 * used as a 'Visited' cache by the ResponseCachePostgres class. Useful if
 * there are a bunch of URLs in the PG database and you want to read them out
 * using methods from the cache class.
 * 
 * @author jimg
 *
 */
public class MakeVisitedCacheFromFile {
	
	private String filename = "ddx_urls.2.txt";
	private String cacheBaseName = "data.nodc.noaa.govDDX";
	final static String VisitedName = "Visited.save";
	
	private ConcurrentHashMap<String, Long> responsesVisited;
	
	public static void main(String args[]) {
		MakeVisitedCacheFromFile maker = new MakeVisitedCacheFromFile();
		
		maker.responsesVisited = new ConcurrentHashMap<String, Long>();
		
		try {
			// Open the file
			FileInputStream fstream = new FileInputStream(maker.filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String url;
			Date date = new Date();
			// Read File Line By Line
			while ((url = br.readLine()) != null) {
				maker.responsesVisited.put(url, date.getTime());
				// Print the content on the console
				//System.out.println(url);
			}
			// Close the input stream
			in.close();
			
			maker.saveVisitedState();
		}
		catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
    private void saveVisitedState() throws Exception {
		FileOutputStream fos;
		ObjectOutputStream oos = null;
    	try {
    		fos = new FileOutputStream(cacheBaseName + VisitedName);
    		oos = new ObjectOutputStream(fos);

    		oos.writeObject(responsesVisited);
    	}
    	catch (FileNotFoundException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Could not open the Responses Visited cache - file not found."
							+ e.getMessage());
    	}
    	catch (SecurityException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Could not open the Responses Visited cache - permissions violation."
							+ e.getMessage());
    	}	
    	catch (java.io.IOException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Generic Java I/O Exception."
							+ e.getMessage());
    	}
    	finally {
    		if (oos != null)
    			oos.close();
    	}
    }  
}
