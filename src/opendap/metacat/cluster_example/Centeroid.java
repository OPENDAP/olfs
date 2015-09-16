/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
 * // Author: James Gallagher  <jgallagher@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.metacat.cluster_example;

/*---------------Centroid.java-----------------*/

//package org.c4s.algorithm.cluster;

/**
 * This class represents the Centroid for a Cluster. The initial centroid is
 * calculated using a equation which divides the sample space for each dimension
 * into equal parts depending upon the value of k.
 * 
 * @author Shyam Sivaraman
 * @version 1.0
 * @see Cluster
 */

class Centroid {
	private double mCx, mCy;
	private Cluster mCluster;

	public Centroid(double cx, double cy) {
		this.mCx = cx;
		this.mCy = cy;
	}

	public void calcCentroid() { // only called by CAInstance
		int numDP = mCluster.getNumDataPoints();
		double tempX = 0, tempY = 0;
		int i;
		// caluclating the new Centroid
		for (i = 0; i < numDP; i++) {
			tempX = tempX + mCluster.getDataPoint(i).getX();
			// total for x
			tempY = tempY + mCluster.getDataPoint(i).getY();
			// total for y
		}
		this.mCx = tempX / numDP;
		this.mCy = tempY / numDP;
		// calculating the new Euclidean Distance for each Data Point
		tempX = 0;
		tempY = 0;
		for (i = 0; i < numDP; i++) {
			mCluster.getDataPoint(i).calcEuclideanDistance();
		}
		// calculate the new Sum of Squares for the Cluster
		mCluster.calcSumOfSquares();
	}

	public void setCluster(Cluster c) {
		this.mCluster = c;
	}

	public double getCx() {
		return mCx;
	}

	public double getCy() {
		return mCy;
	}

	public Cluster getCluster() {
		return mCluster;
	}

}
