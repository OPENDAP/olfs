package opendap.metacat;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class URLGroup {

	private String[] classification;
	private Set<String> urls;
	
	public URLGroup(String url) throws Exception {
		URLClassification c = new URLClassification(url);
		classification = c.getClassification();
		urls = new HashSet<String>();
		urls.add(url);
	}
	
	public String[] getClassification() {
		return classification;
	}
	
	public void add(String url) {
		urls.add(url);
	}
	
	public class URLEnumeration implements Enumeration<String> {
		private Iterator<String> i = urls.iterator();
		
		@Override
		public boolean hasMoreElements() {
			return i.hasNext();
		}

		@Override
		public String nextElement() {
			return i.next();
		}
	}
	
	public URLEnumeration getURLs() {
		return new URLEnumeration();
	}
}
