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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class URLProcessedComponents implements Serializable {
	private static final long serialVersionUID = 1L;

	public class Lexeme implements Serializable {
		private static final long serialVersionUID = 1L;
		private String value;
		private boolean pattern;
		
		public Lexeme() {
			value = "";
			pattern = false;
		}
		public boolean isPattern() { return pattern; }
		public String getValue() { return value; }
	}

	private Vector<Lexeme> theClasses;
	
    List<String> fileExtensions = Collections.unmodifiableList(Arrays.asList("bz2", "gz", "Z", "nc", "hdf", "HDF", "h5"));
	
	// Testing only...
	
	public static void main(String args[]) {
		if (args.length < 1) return;
		
		try {
			URLProcessedComponents pc = new URLProcessedComponents(args[0]);

			System.out.println(args[0]); // print URL

			String classes[] = pc.getLexemeArray();
			for (String cls : classes)
				System.out.print(cls + " ");
			System.out.println();
			
			Lexemes ce = pc.getLexemes();
			while (ce.hasMoreElements()) {
				Lexeme c = ce.nextElement();
				if (c.isPattern())
					System.out.print(c.getValue() + ": pattern ");
				else
					System.out.print(c.getValue() + " ");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public URLProcessedComponents(String url) throws Exception {
		this(new ParsedURL(url));
	}
	
	public URLProcessedComponents(ParsedURL url) {
		buildEquivalenceClasses(url);
	}
	
	/**
	 * This is the simple code to build the equivalences. 
	 * 
	 * @param url An instance of URLParser
	 */
	private void buildEquivalenceClasses(ParsedURL url) {
		String[] comps = url.getComponents();
		
		theClasses = new Vector<Lexeme>();
		
		Lexeme previousLexeme = null;
		for(String comp: comps) {
			Lexeme c = new Lexeme();
			
			// Rule: if comp is all digits, replace each with 'd'
			if (comp.matches("[0-9]+")) {
				int j = 0;
				while (j++ < comp.length())
					c.value += 'd';
				// Hack: if the previous lexeme was 'dddd' and this one is 'd'
				// make it 'dd' because it's likely we have a degenerate case
				// where months are represented using both one and two digit
				// values.
				if (previousLexeme != null && previousLexeme.value.equals("dddd") && c.value.equals("d"))
					c.value += 'd';
					
				c.pattern = true;
			}
			else if (fileExtensions.contains(comp)) {
				c.value = comp;
				c.pattern = false;
			}
			else if (comp.matches("[0-9]{2,}[A-Za-z]{3}[0-9]{2,}")) {
				int j = 0;
				while (j < comp.length()) {
					 if (Character.isDigit(comp.charAt(j)))
						 c.value += 'd';
					 else
						 c.value += 'c';
					++j;
				}

				c.pattern = true;
				
			}
			// if comp is a string of digits followed by chars, replace each
			// digit by a 'd' but keep the literal char data. Allow for a
			// trailing sequence of digits to follow the char data, but treat
			// those as literals. Note that there are plenty of cases where
			// a single digit starts out a literal so require at least two 
			// digits at the front.
			else if (comp.matches("[0-9][0-9]+[A-Za-z]+[0-9]*")) {
				int j = 0;
				while (j < comp.length() && Character.isDigit(comp.charAt(j))) {
					c.value += 'd';
					++j;
				}
				while(j < comp.length())
					c.value += comp.charAt(j++);

				c.pattern = true;
			}
			// If comp is a single char, replace the char by 'c'.
			else if (comp.matches("[A-Za-z]") && comp.length() == 1) {
				c.value += 'c';
				c.pattern = true;
			}
			// If comp is a sequence of chars followed by a sequence of digits,
			// replace the digits by 'd'. 
			// Added optional digit prefix. jhrg 12/22/2010
			else if (comp.matches("[0-9]*[A-Za-z]+[0-9]+")) {
				int j = 0;
				while (j < comp.length() && Character.isLetter(comp.charAt(j)))
					c.value += comp.charAt(j++);
				while(j < comp.length()) {
					c.value += 'd';
					++j;
				}

				c.pattern = true;
			}
			else {
				c.value = comp;
				c.pattern = false;
			}
			
			theClasses.add(c);
			previousLexeme = c;
		}
	}
	
	public class Lexemes implements Enumeration<Lexeme> {
		private Enumeration<Lexeme> e = theClasses.elements();
		@Override
		public boolean hasMoreElements() {
			return e.hasMoreElements();
		}

		@Override
		public Lexeme nextElement() {
			return e.nextElement();		
		}
	}
	
	public Lexemes getLexemes() {
		return new Lexemes();
	}
	
	public String[] getLexemeArray() {
		String[] result = new String[theClasses.size()];
		int i = 0;
		for (Lexeme c: theClasses) 
			result[i++] = c.value;
		return result;
	}
}
