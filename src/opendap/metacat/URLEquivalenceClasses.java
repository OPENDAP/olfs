package opendap.metacat;

public class URLEquivalenceClasses {
	String[] classes;
	
	// Testing only...
	public static void main(String args[]) {
		if (args.length < 1) return;
		
		// URLComponents comps = new URLComponents(args[0]);
		try {
			URLEquivalenceClasses classification = new URLEquivalenceClasses(args[0]);

			System.out.println(args[0]); // print URL

			String classes[] = classification.getClassification();
			for (String cls : classes)
				System.out.print(cls + " ");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public URLEquivalenceClasses(URLComponents url) {
		buildEquivalenceClasses(url);
	}
	
	public URLEquivalenceClasses(String url) throws Exception {
		buildEquivalenceClasses(new URLComponents(url));
	}
	
	/**
	 * This is the simple code to build the equivalences. 
	 * 
	 * @param url An instance of URLParser
	 */
	private void buildEquivalenceClasses(URLComponents url) {
		String[] comps = url.getComponents();
		
		classes = new String[comps.length];
		
		// classes[0] = url.getMachine();
		int i = 0;
		for(String comp: comps) {
			// Rule: if comp is all digits, replace each with 'd'
			if (comp.matches("[0-9]+")) {
				int j = 0;
				classes[i] = "";
				while (j++ < comp.length())
					classes[i] += 'd';
				++i;
			}
			// if comp is a string of digits followed by chars, replace each
			// digit by a 'd' but keep the literal char data. Allow for a
			// trailing sequence of digits to follow the char data, but treat
			// those as literals.
			else if (comp.matches("[0-9]+[A-Za-z]+[0-9]*")) {
				int j = 0;
				classes[i] = "";
				while (j < comp.length() && Character.isDigit(comp.charAt(j))) {
					classes[i] += 'd';
					++j;
				}
				while(j < comp.length())
					classes[i] += comp.charAt(j++);
				++i;
			}
			// If comp is a sequence of chars followed by a sequence of digits,
			// replace the digits by 'd'.
			else if (comp.matches("[A-Za-z]+[0-9]+")) {
				int j = 0;
				classes[i] = "";
				while (j < comp.length() && Character.isLetter(comp.charAt(j)))
					classes[i] += comp.charAt(j++);
				while(j < comp.length()) {
					classes[i] += 'd';
					++j;
				}
				++i;
			}
			else {
				classes[i++] = comp;
			}
		}
	}
	
	public String[] getClassification() {
		return classes;
	}
}
