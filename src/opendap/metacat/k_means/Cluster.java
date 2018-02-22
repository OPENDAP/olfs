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
package opendap.metacat.k_means;

/*-----------------Cluster.java----------------*/

//package org.c4s.algorithm.cluster;

import java.util.Vector;

/**
 * This class represents a Cluster in a Cluster Analysis Instance. A Cluster is
 * associated with one and only one JCA Instance. A Cluster is related to more
 * than one DataPoints and one centroid.
 * 
 * @author Shyam Sivaraman
 * @version 1.1
 * @see DataPoint
 * @see Centroid
 */

class Cluster {
	private String mName;
	private Centroid mCentroid;
	private double mSumSqr;
	private Vector<DataPoint> mDataPoints;

	public Cluster(String name) {
		this.mName = name;
		this.mCentroid = null; // will be set by calling setCentroid()
		mDataPoints = new Vector<DataPoint>();
	}

	public void setCentroid(Centroid c) {
		mCentroid = c;
	}

	public Centroid getCentroid() {
		return mCentroid;
	}

	public void addDataPoint(DataPoint dp) { // called from CAInstance
		dp.setCluster(this); // initiates a inner call to
								// calcEuclideanDistance() in DP.
		this.mDataPoints.addElement(dp);
		calcSumOfSquares();
	}

	public void removeDataPoint(DataPoint dp) {
		this.mDataPoints.removeElement(dp);
		calcSumOfSquares();
	}

	public int getNumDataPoints() {
		return this.mDataPoints.size();
	}

	public DataPoint getDataPoint(int pos) {
		return (DataPoint) this.mDataPoints.elementAt(pos);
	}

	public void calcSumOfSquares() { // called from Centroid
		int size = this.mDataPoints.size();
		double temp = 0;
		for (int i = 0; i < size; i++) {
			temp = temp + ((DataPoint) this.mDataPoints.elementAt(i)).getCurrentEuDt();
		}
		this.mSumSqr = temp;
	}

	public double getSumSqr() {
		return this.mSumSqr;
	}

	public String getName() {
		return this.mName;
	}

	public Vector<DataPoint> getDataPoints() {
		return this.mDataPoints;
	}

}
