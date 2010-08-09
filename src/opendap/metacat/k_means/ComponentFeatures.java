package opendap.metacat.k_means;

/**
 * Given a String/pattern that is one component of a URL, calculate the 'features' that
 * describe that component. Currently, the features are the number of digits,
 * the number of characters and the number of distinct character or digit
 * groups. Eacho os these is recorded as an integer and the tuple of those
 * three integers makes up a feature vector.
 * 
 * @author jimg
 *
 */
public class ComponentFeatures {
	
	private int digits = 0;
	private int letters = 0;
	private int groups = 0;
	
	private static enum KIND {
		DIGIT {
			public String toString() {
				return "DIGIT";
			}
		},
		LETTER {
			public String toString() {
				return "LETTER";
			}
		},
		NOTHING {
			public String toString() {
				return "NOTHING";
			}
		}
	}
	
	public ComponentFeatures(String pattern) {
		// At first we have seen nothing, and the first thing is the first group
		KIND k = KIND.NOTHING;
		groups++;
		for (int i = 0; i < pattern.length(); ++i) {
			if (Character.isDigit(pattern.charAt(i))) {
				// if the last thing seen was a letter, this is a new group
				if (k == KIND.LETTER)
					groups++;
				k = KIND.DIGIT;
				digits++;
			}
			else {	
				// if the current character is not a digit, it must be a
				// letter since all the other ASCII characters are separators
				if (k == KIND.DIGIT)
					groups++;
				k = KIND.LETTER;
				letters++;
			}
		}
	}

	public int[] getFeatureVector() {
		int fv[] = new int[3];
		fv[0] = digits;
		fv[1] = letters;
		fv[2] = groups;
		return fv;
	}

	public double[] getNormalizedFeatureVector() {
		double fv[] = new double[3];
		double size = digits + letters;
		fv[0] = digits/size;
		fv[1] = letters/size;
		fv[2] = groups/size;
		return fv;
	}
}
