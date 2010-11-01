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

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the entry point for constructing Cluster Analysis objects. Each
 * instance of JCA object is associated with one or more clusters, and a Vector
 * of DataPoint objects. The JCA and DataPoint classes are the only classes
 * available from other packages.
 * 
 * @see DataPoint
 **/

public class JCA {
	private Cluster[] clusters;
	private int miter;
	private Vector<DataPoint> mDataPoints = new Vector<DataPoint>();
	private double mSWCSS;
    private static Logger log = LoggerFactory.getLogger(JCA.class);

	public JCA(int k, int iter, Vector<DataPoint> dataPoints) {
		clusters = new Cluster[k];
		for (int i = 0; i < k; i++) {
			clusters[i] = new Cluster("Cluster" + i);
		}
		this.miter = iter;
		this.mDataPoints = dataPoints;
	}

	private void calcSWCSS() {
		double temp = 0;
		for (int i = 0; i < clusters.length; i++) {
			temp = temp + clusters[i].getSumSqr();
		}
		mSWCSS = temp;
	}

	public void startAnalysis() {
		// set Starting centroid positions - Start of Step 1
		setInitialCentroids();
		int n = 0;
		// assign DataPoint to clusters. this distributes the data points to
		// clusters without making any initial guess at a likely best choice.
		loop1: while (true) {
			for (int l = 0; l < clusters.length; l++) {
				clusters[l].addDataPoint(mDataPoints.elementAt(n));
				n++;
				if (n >= mDataPoints.size())
					break loop1;
			}
		}

		log.debug("Data points initially assigned to clusters: " + n);
		
		// calculate E for all the clusters
		calcSWCSS();
		log.debug("Initial SWCSS value: " + getSWCSS());
		
		// recalculate Cluster centroids - Start of Step 2
		for (int i = 0; i < clusters.length; i++) {
			clusters[i].getCentroid().calcCentroid();
		}

		// recalculate E for all the clusters
		calcSWCSS();
		log.debug("Second SWCSS value: " + getSWCSS());

		for (int i = 0; i < miter; i++) {
			// enter the loop for cluster 1
			for (int j = 0; j < clusters.length; j++) {
				for (int k = 0; k < clusters[j].getNumDataPoints(); k++) {

					// pick the first element of the first cluster
					// get the current Euclidean distance
					double tempEuDt = clusters[j].getDataPoint(k).getCurrentEuDt();
					Cluster tempCluster = null;
					boolean matchFoundFlag = false;

					// call testEuclidean distance for all clusters
					for (int l = 0; l < clusters.length; l++) {

						// if testEuclidean < currentEuclidean then
						if (tempEuDt > clusters[j].getDataPoint(k).testEuclideanDistance(clusters[l].getCentroid())) {
							tempEuDt = clusters[j].getDataPoint(k).testEuclideanDistance(clusters[l].getCentroid());
							tempCluster = clusters[l];
							matchFoundFlag = true;
						}
						// if statement - Check whether the Last EuDt is >
						// Present EuDt

					}
					// for variable 'l' - Looping between different Clusters for
					// matching a Data Point.
					// add DataPoint to the cluster and calcSWCSS

					if (matchFoundFlag) {
						tempCluster.addDataPoint(clusters[j].getDataPoint(k));
						clusters[j].removeDataPoint(clusters[j].getDataPoint(k));
						for (int m = 0; m < clusters.length; m++) {
							clusters[m].getCentroid().calcCentroid();
						}

						// for variable 'm' - Recalculating centroids for all
						// Clusters

						calcSWCSS();
					}

					// if statement - A Data Point is eligible for transfer
					// between Clusters.
				}
				// for variable 'k' - Looping through all Data Points of the
				// current Cluster.
			}// for variable 'j' - Looping through all the Clusters.
		}// for variable 'i' - Number of iterations.
	}

	public Vector<DataPoint>[] getClusterOutput() {
		Vector<DataPoint> v[] = new Vector[clusters.length];
		for (int i = 0; i < clusters.length; i++) {
			v[i] = clusters[i].getDataPoints();
		}
		return v;
	}

	private void setInitialCentroids() {
		// kn = (round((max-min)/k)*n)+min where n is from 0 to (k-1).
		double cx = 0, cy = 0, cz = 0;
		for (int n = 1; n <= clusters.length; n++) {
			cx = (((getMaxXValue() - getMinXValue()) / (clusters.length + 1)) * n) + getMinXValue();
			cy = (((getMaxYValue() - getMinYValue()) / (clusters.length + 1)) * n) + getMinYValue();
			cz = (((getMaxZValue() - getMinZValue()) / (clusters.length + 1)) * n) + getMinZValue();
			Centroid c1 = new Centroid(cx, cy, cz);
			clusters[n - 1].setCentroid(c1);
			c1.setCluster(clusters[n - 1]);
		}
	}

	private double getMaxXValue() {
		double temp;
		temp = mDataPoints.elementAt(0).getX();
		for (int i = 0; i < mDataPoints.size(); i++) {
			DataPoint dp = mDataPoints.elementAt(i);
			temp = (dp.getX() > temp) ? dp.getX() : temp;
		}
		return temp;
	}

	private double getMinXValue() {
		double temp = 0;
		temp = mDataPoints.elementAt(0).getX();
		for (int i = 0; i < mDataPoints.size(); i++) {
			DataPoint dp = mDataPoints.elementAt(i);
			temp = (dp.getX() < temp) ? dp.getX() : temp;
		}
		return temp;
	}

	private double getMaxYValue() {
		double temp = 0;
		temp = mDataPoints.elementAt(0).getY();
		for (int i = 0; i < mDataPoints.size(); i++) {
			DataPoint dp = mDataPoints.elementAt(i);
			temp = (dp.getY() > temp) ? dp.getY() : temp;
		}
		return temp;
	}

	private double getMinYValue() {
		double temp = 0;
		temp = mDataPoints.elementAt(0).getY();
		for (int i = 0; i < mDataPoints.size(); i++) {
			DataPoint dp = mDataPoints.elementAt(i);
			temp = (dp.getY() < temp) ? dp.getY() : temp;
		}
		return temp;
	}

	private double getMaxZValue() {
		double temp = 0;
		temp = mDataPoints.elementAt(0).getZ();
		for (int i = 0; i < mDataPoints.size(); i++) {
			DataPoint dp = mDataPoints.elementAt(i);
			temp = (dp.getZ() > temp) ? dp.getZ() : temp;
		}
		return temp;
	}

	private double getMinZValue() {
		double temp = 0;
		temp = mDataPoints.elementAt(0).getZ();
		for (int i = 0; i < mDataPoints.size(); i++) {
			DataPoint dp = mDataPoints.elementAt(i);
			temp = (dp.getZ() < temp) ? dp.getZ() : temp;
		}
		return temp;
	}

	public int getKValue() {
		return clusters.length;
	}

	public int getIterations() {
		return miter;
	}

	public int getTotalDataPoints() {
		return mDataPoints.size();
	}

	public double getSWCSS() {
		return mSWCSS;
	}

	public Cluster getCluster(int pos) {
		return clusters[pos];
	}
}
