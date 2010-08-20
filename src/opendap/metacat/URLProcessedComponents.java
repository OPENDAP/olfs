package opendap.metacat;

import java.util.Enumeration;
import java.util.Vector;

public class URLProcessedComponents {
	public class classes {
		private String lexeme;
		private boolean isPattern;
		
		public boolean isPattern() { return isPattern; }
		public String lexeme() { return lexeme; }
	}

	private Vector<classes> theClasses;
	
	// Testing only...
	
	public static void main(String args[]) {
		if (args.length < 1) return;
		
		// URLComponents comps = new URLComponents(args[0]);
		try {
			URLProcessedComponents classification = new URLProcessedComponents(args[0]);

			System.out.println(args[0]); // print URL

			String classes[] = classification.getLexemes();
			for (String cls : classes)
				System.out.print(cls + " ");
			System.out.println();
			
			ClassificationEnumeration ce = classification.getClassifications();
			while (ce.hasMoreElements()) {
				classes c = ce.nextElement();
				if (c.isPattern())
					System.out.print(c.lexeme() + ": pattern ");
				else
					System.out.print(c.lexeme() + " ");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public URLProcessedComponents(URLComponents url) {
		buildEquivalenceClasses(url);
	}
	
	public URLProcessedComponents(String url) throws Exception {
		buildEquivalenceClasses(new URLComponents(url));
	}
	
	/**
	 * This is the simple code to build the equivalences. 
	 * 
	 * @param url An instance of URLParser
	 */
	private void buildEquivalenceClasses(URLComponents url) {
		String[] comps = url.getComponents();
		
		theClasses = new Vector<classes>();
		
		// classes = new String[comps.length];
		// isPattern = new boolean[comps.length];
		
		//int i = 0;
		for(String comp: comps) {
			classes c = new classes();
			
			// Rule: if comp is all digits, replace each with 'd'
			if (comp.matches("[0-9]+")) {
				int j = 0;
				c.lexeme = ""; //classes[i] = "";
				while (j++ < comp.length())
					c.lexeme += 'd'; //classes[i] += 'd';

				c.isPattern = true; //isPattern[i] = true;
			}
			// if comp is a string of digits followed by chars, replace each
			// digit by a 'd' but keep the literal char data. Allow for a
			// trailing sequence of digits to follow the char data, but treat
			// those as literals.
			else if (comp.matches("[0-9]+[A-Za-z]+[0-9]*")) {
				int j = 0;
				c.lexeme = ""; //classes[i] = "";
				while (j < comp.length() && Character.isDigit(comp.charAt(j))) {
					c.lexeme += 'd'; //classes[i] += 'd';
					++j;
				}
				while(j < comp.length())
					c.lexeme += comp.charAt(j++); // classes[i] += comp.charAt(j++);

				c.isPattern = true; //isPattern[i] = true;
			}
			// If comp is a sequence of chars followed by a sequence of digits,
			// replace the digits by 'd'.
			else if (comp.matches("[A-Za-z]+[0-9]+")) {
				int j = 0;
				c.lexeme = ""; //classes[i] = "";
				while (j < comp.length() && Character.isLetter(comp.charAt(j)))
					c.lexeme += comp.charAt(j++); // classes[i] += comp.charAt(j++);
				while(j < comp.length()) {
					c.lexeme += 'd'; //classes[i] += 'd';
					++j;
				}

				c.isPattern = true; //isPattern[i] = true;
			}
			else {
				c.lexeme = comp; // classes[i] = comp;
				c.isPattern = false; // isPattern[i] = false;
			}
			
			theClasses.add(c);
			
			//++i;
		}
	}
	
	public class ClassificationEnumeration implements Enumeration<classes> {
		private Enumeration<classes> e = theClasses.elements();
		@Override
		public boolean hasMoreElements() {
			return e.hasMoreElements();
		}

		@Override
		public classes nextElement() {
			return e.nextElement();		
		}
	}
	
	public ClassificationEnumeration getClassifications() {
		return new ClassificationEnumeration();
	}
	
	public String[] getLexemes() {
		String[] result = new String[theClasses.size()];
		int i = 0;
		for (classes c: theClasses) 
			result[i++] = c.lexeme;
		return result;
	}
	/*
	public String[] getClassification() {
		return classes;
	}
	*/
}
