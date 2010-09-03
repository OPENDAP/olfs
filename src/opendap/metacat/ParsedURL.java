package opendap.metacat;

import java.util.Arrays;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a URL passed to the constructor, parse the URL and provide access
 * to its various parts. This class will break out the machine, path and leaf
 * components of the URL. This also holds a copy of the  entire URL's original
 * text, so that references to instances can get both components and the real
 * thing. 
 * 
 * @author jimg
 *
 */
public class ParsedURL {
	private String machine;
	private String[] components;
	private String theURL;
	
    private static Logger log = LoggerFactory.getLogger(ParsedURL.class);

	public static void main(String[] args) {
		if (args.length < 1)
			return;

		try {
			ParsedURL up = new ParsedURL(args[0]);
			System.out.println("URL: " + args[0]);
			System.out.println(up.getMachine());
			String[] comps = up.getComponents();
			for (String s : comps)
				System.out.println(s);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * A URL is parsed by first removing the protocol and '://' parts, then the
	 * machine part. What remains is the path, where '/' separates the components
	 * until the leaf (file) is found. Within the file we consider any
	 * non-alphanum to be a separator.
	 * 
	 * @param url
	 *            The URL to parse.
	 */
	public ParsedURL(String url) throws Exception {
		theURL = url;
		
		try {
			url = url.substring(url.indexOf("//") + 2);

			machine = url.substring(0, url.indexOf('/'));

			String path = url.substring(url.indexOf('/') + 1, url.lastIndexOf('/'));
			String file = url.substring(url.lastIndexOf('/') + 1);

			String[] path_components = path.split("/");

			String[] file_components = file.split("[^A-Za-z0-9]");
			
			// Break up the parts of a filename further by assuming that a
			// component that either starts or ends with a series of digits
			// might really be separate from a character sequence that the
			// above 'parse' has left attached.
			Vector<String> sub_file_components = new Vector<String>();
			
			for (String comp: file_components) {
				if (comp.matches("[A-za-z]+[0-9]+")) {
					// gobble up the letters, push onto the vector
					String first = new String(), rest = new String();
					int j = 0;
					while (j < comp.length() && Character.isLetter(comp.charAt(j)))
						first += comp.charAt(j++);
					// ... now add the digits
					while(j < comp.length())
						rest += comp.charAt(j++);
					
					sub_file_components.add(first);
					sub_file_components.add(rest);
				}
				else if (comp.matches("[0-9]+[A-Za-z]+")) {
					// gobble up the letters, push onto the vector
					String first = new String(), rest = new String();
					int j = 0;
					while (j < comp.length() && Character.isDigit(comp.charAt(j)))
						first += comp.charAt(j++);
					// ... now add the digits
					while(j < comp.length())
						rest += comp.charAt(j++);
					
					sub_file_components.add(first);
					sub_file_components.add(rest);
				}
				else
					sub_file_components.add(comp);
			}

			file_components = new String[sub_file_components.size()];
			sub_file_components.toArray(file_components);
			
			components = concat(path_components, file_components);
		}
		catch (StringIndexOutOfBoundsException e) {
			log.error("String index out of range: " + e.getLocalizedMessage());
			throw new Exception(e);
		}
	}
	
	public String getMachine() {
		return machine;
	}
	
	public String[] getComponents() {
		return components;
	}
	
	public String getTheURL() {
		return theURL;
	}
	
	/**
	 * Concatenate two arrays of the same type and return the result in an 
	 * array. Requires Java 6
	 * 
	 * @param <T>
	 * @param first The first array
	 * @param second The second array
	 * @return The concatenation of the two arrays
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		final T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	/**
	 * A version of concat that does not require Java 6
	 * @param <T>
	 * @param a
	 * @param b
	 * @return
	 * @see concat
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concat_java5(T[] a, T[] b) {
	    final int alen = a.length;
	    final int blen = b.length;
	    final T[] result = (T[]) java.lang.reflect.Array.
	            newInstance(a.getClass().getComponentType(), alen + blen);
	    System.arraycopy(a, 0, result, 0, alen);
	    System.arraycopy(b, 0, result, alen, blen);
	    return result;
	}

}
