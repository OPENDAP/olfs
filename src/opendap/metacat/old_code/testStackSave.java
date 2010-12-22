package opendap.metacat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Stack;

public class testStackSave {
	public static void main(String[] args) {
		Stack<String> s = new Stack<String>();
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AMSRE/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/ATS_NR_2P/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR16_G/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR16_L/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR17_G/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR17_L/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR18_G/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR18_L/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRR19_L/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/AVHRRMTA_G/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/GOES11/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/GOES12/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/MODIS_A/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/MODIS_T/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/NAR16_SST/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/NAR17_SST/catalog.xml");
		s.push("http://data.nodc.noaa.gov/opendap/hyrax/ghrsst/L2P/NAR18_SST/catalog.xml");
		
		System.err.println("Top of the stack: " + s.peek());
		
		try {
			saveState(s);

			Stack<String> t = new Stack<String>();

			t = restoreState();
			System.err.println("Top of the restored stack: " + t.peek());
			Enumeration<String> ts = t.elements();
			while (ts.hasMoreElements())
				System.err.println("t: " + ts.nextElement());
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
		}

	}
	public static void saveState(Stack<String> p) throws Exception {
		FileOutputStream fos;
		ObjectOutputStream oos = null;
    	try {
    		fos = new FileOutputStream("testStack");
    		oos = new ObjectOutputStream(fos);

    		oos.writeObject(p);
    	}
    	catch (Exception e) {
			throw new Exception("testStack.saveState: ", e);
    	}
	}
	
	@SuppressWarnings("unchecked")
	private static Stack<String> restoreState() throws Exception {
		FileInputStream fis;
		ObjectInputStream ois = null;
		Stack<String> p = null;
		try {
			fis = new FileInputStream("testStack");
			ois = new ObjectInputStream(fis);
			
    		p = (Stack<String>)ois.readObject();
    	}
    	catch (FileNotFoundException e) {
    		System.err.println("Could not open the Responses Visited cache - file not found.");
    	}
		catch (ClassNotFoundException e) {
			throw new Exception(
					"testStack.saveState: "
							+ "Could not find the class when reading the Stack<String> object."
							+ e.getMessage());
		}
		catch (ClassCastException e) {
			throw new Exception(
					"testStack.saveState: "
							+ "Could not cast the persistent store to a Stack<String> object."
							+ e.getMessage());
		}
		catch (java.io.IOException e) {
			throw new Exception("testStack.saveState: " + "Generic Java I/O Exception."
					+ e.getMessage());
		}    	
		finally {
    		if (ois != null)
    			ois.close();
    	}
		return p;
    }
}

