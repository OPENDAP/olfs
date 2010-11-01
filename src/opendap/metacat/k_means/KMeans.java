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

package opendap.metacat.k_means;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.DDXRetriever;
import opendap.metacat.ParsedURL;

public class KMeans {
    private static Logger log = LoggerFactory.getLogger(KMeans.class);

	public static void main(String args[]) {
		KMeans km = new KMeans();
		
		String cacheName = args[0];
		
		try {
			DDXRetriever ddxSource = new DDXRetriever(true, cacheName);
			//Build the data points for a particular crawl
			Vector<DataPoint> dataPoints = km.buildDataPoints(ddxSource);
			
			JCA jca = new JCA(5, 1000, dataPoints);
			jca.startAnalysis();

			Vector<DataPoint> v[] = jca.getClusterOutput();
			for (int i = 0; i < v.length; i++) {
				Vector<DataPoint> tempV = v[i];
				log.debug("----------- Cluster " + i + " ---------");
				Iterator<DataPoint> iter = tempV.iterator();
				while (iter.hasNext()) {
					DataPoint dpTemp = (DataPoint) iter.next();
					log.debug(dpTemp.getObjName() + ":" + dpTemp.getCompNum() + ": " + dpTemp.getComp()
							+ " [" + dpTemp.getX() + "," + dpTemp.getY() + "," + dpTemp.getZ() + "]");
				}
			}
		}
		catch (Exception e) {
			System.err.println("Exception: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
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
    		ParsedURL urlComps = null;
    		try {
    			urlComps = new ParsedURL(ddxURL);
			}
			catch (Exception e) {
				log.error("URLComponets: " + e.getMessage());
				continue;
			}
			
			int i = 0;
			String[] components = urlComps.getComponents();
			for (String component : components) {
				ComponentFeatures cf = new ComponentFeatures(component);

				//int fv[] =  cf.getFeatureVector();
				// log.debug("Component: " + component + ", Features: " + new Integer(fv[0]).toString() + ", " + new Integer(fv[1]).toString() + ", " + new Integer(fv[2]).toString());
				// double dv[] =  cf.getNormalizedFeatureVector();
				// log.debug("Component: " + component + ", Features: " + new Double(dv[0]).toString() + ", " + new Double(dv[1]).toString() + ", " + new Double(dv[2]).toString());
				
				dataPoints.add(new DataPoint(cf.getFeatureVector(), ddxURL, component, i++));
			}
    	}
    	
		return dataPoints;
	}
}
