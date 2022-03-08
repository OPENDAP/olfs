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

/*----------------DataPoint.java----------------*/

//package org.c4s.algorithm.cluster;

/**
 * This class represents a candidate for Cluster analysis. A candidate must have
 * a name and two independent variables on the basis of which it is to be
 * clustered. A Data Point must have two variables and a name. A Vector of Data
 * Point objects is fed into the constructor of the JCA class. JCA and DataPoint
 * are the only classes which may be available from other packages.
 * 
 * @author Shyam Sivaraman
 * @version 1.0
 * @see JCA
 * @see Cluster
 */

public class DataPoint {
	private double mX, mY;
	private String mObjName;
	private Cluster mCluster;
	private double mEuDt;

	public DataPoint(double x, double y, String name) {
		this.mX = x;
		this.mY = y;
		this.mObjName = name;
		this.mCluster = null;
	}

	public void setCluster(Cluster cluster) {
		this.mCluster = cluster;
		calcEuclideanDistance();
	}

	public void calcEuclideanDistance() {

		// called when DP is added to a cluster or when a Centroid is
		// recalculated.
		mEuDt = Math.sqrt(Math.pow((mX - mCluster.getCentroid().getCx()), 2) + Math.pow((mY - mCluster.getCentroid().getCy()), 2));
	}

	public double testEuclideanDistance(Centroid c) {
		return Math.sqrt(Math.pow((mX - c.getCx()), 2) + Math.pow((mY - c.getCy()), 2));
	}

	public double getX() {
		return mX;
	}

	public double getY() {
		return mY;
	}

	public Cluster getCluster() {
		return mCluster;
	}

	public double getCurrentEuDt() {
		return mEuDt;
	}

	public String getObjName() {
		return mObjName;
	}

}
