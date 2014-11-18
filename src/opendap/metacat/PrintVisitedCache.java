package opendap.metacat;

import java.util.Enumeration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * Print the keys (URLs) and Last Modified Times (which are actually the access
 * times) for a given 'visited' cache.
 * 
 * @author jimg
 *
 */
public class PrintVisitedCache {

	private ResponseCachePostgres cache = null;
	
	public PrintVisitedCache(String basename, String tablename) throws Exception {
		cache = new ResponseCachePostgres(true, basename, tablename);
	}
	/**
	 * Given the name of the visited cache (e.g., test.opendap.or_THREDDS) and
	 * the name of the Postgres database table (e.g., thredds_resoponses), print
	 * the URLs and the stored LMT. We need the Postgres table name because of
	 * the way ResponseCachePostgres works.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		options.addOption("h", "help", false, "Usage information");
		
		options.addOption("d", "ddx-urls", false, "Print all of the cached DDX URLs.");
		options.addOption("t", "thredds-urls", false, "Print all of the cached THREDDS URLs.");
		options.addOption("n", "cache-name", true, "Use this as the cache base name.");

		try {
			CommandLine line = parser.parse( options, args );
			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("print_cached_urls [options] --cache-name <name prefix>", options);
				return;
			}
		    
			String cacheName = line.getOptionValue("cache-name");
			if (line.hasOption("ddx-urls")) {
				PrintVisitedCache printer = new PrintVisitedCache(cacheName + "_DDX", "ddx_responses");
				printer.printURLs();
			}
			if (line.hasOption("thredds-urls")) {
				PrintVisitedCache printer = new PrintVisitedCache(cacheName + "_THREDDS", "thredds_responses");
				printer.printURLs();
			}
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getLocalizedMessage());
		}
	}

	private void printURLs() {
		int i = 0;
		Enumeration<String> keys = cache.getLastVisitedKeys();
		while (keys.hasMoreElements()) {
			++i;
			String url = keys.nextElement();
			System.out.println(url +  ": " + cache.getLastVisited(url));
		}
		System.out.println("Found " + i + " keys.");
	}
}
