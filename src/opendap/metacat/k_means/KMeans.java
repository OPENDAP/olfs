package opendap.metacat.k_means;

import java.util.Enumeration;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.DDXRetriever;
import opendap.metacat.URLComponents;

/**
 * Created by IntelliJ IDEA. User: shyam.s Date: Apr 18, 2004 Time: 4:26:06 PM
 */
public class KMeans {
    private static Logger log = LoggerFactory.getLogger(KMeans.class);

	public static void main(String args[]) {
		KMeans km = new KMeans();
		
		String cacheName = args[0];
		
		try {
			DDXRetriever ddxSource = new DDXRetriever(true, cacheName);
			//Build the datapoints for a particular crawl
			Vector<DataPoint> dataPoints = km.buildDataPoints(ddxSource);
		}
		catch (Exception e) {
			System.err.println("Exception: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
	
		/*
		dataPoints.add(new DataPoint(22, 21, "p53"));
		dataPoints.add(new DataPoint(19, 20, "bcl2"));
		dataPoints.add(new DataPoint(18, 22, "fas"));
		dataPoints.add(new DataPoint(1, 3, "amylase"));
		dataPoints.add(new DataPoint(3, 2, "maltase"));
		*/
		/*
		JCA jca = new JCA(2, 1000, dataPoints);
		jca.startAnalysis();

		Vector<DataPoint> v[] = jca.getClusterOutput();
		for (int i = 0; i < v.length; i++) {
			Vector<DataPoint> tempV = v[i];
			System.out.println("----------- Cluster " + i + " ---------");
			Iterator<DataPoint> iter = tempV.iterator();
			while (iter.hasNext()) {
				DataPoint dpTemp = (DataPoint) iter.next();
				System.out.println(dpTemp.getObjName() + "[" + dpTemp.getX() + "," + dpTemp.getY() + "]");
			}
		}
		*/
	}
	
	/**
	 * Build a Vector<DataPoint> instance with all of the URL Components as
	 * data points.
	 * 
	 * @return The DataPoint Vector
	 * @param catalogRoot
	 */
	public Vector<DataPoint> buildDataPoints(DDXRetriever ddxSource) {
		Vector<DataPoint> dataPoints = new Vector<DataPoint>();
		Enumeration<String> ddxURLs = ddxSource.getCache().getLastVisitedKeys();
    	
    	// Special case for teh first URL (because using 'for' with an iterator
    	// fails when the iterator instance is null
    	while (ddxURLs.hasMoreElements()) {
    		String ddxURL = ddxURLs.nextElement();
    		URLComponents urlComps = null;
    		try {
    			urlComps = new URLComponents(ddxURL);
			}
			catch (Exception e) {
				log.error("URLComponets: " + e.getMessage());
				continue;
			}

			String[] components = urlComps.getComponents();
			for (String component : components) {
				ComponentFeatures cf = new ComponentFeatures(component);
				int fv[] =  cf.getFeatureVector();
				System.out.println("Component: " + component + ", Features: " + new Integer(fv[0]).toString() + ", " + new Integer(fv[1]).toString() + ", " + new Integer(fv[2]).toString());
				double dv[] =  cf.getNormalizedFeatureVector();
				System.out.println("Component: " + component + ", Features: " + new Double(dv[0]).toString() + ", " + new Double(dv[1]).toString() + ", " + new Double(dv[2]).toString());
				
				//dataPoints.add(new DataPoint(cf.getFeatureVector(), ddxURL, i++));
			}
    	}

		dataPoints.add(new DataPoint(22, 21, "p53"));
		
		return dataPoints;
	}
}