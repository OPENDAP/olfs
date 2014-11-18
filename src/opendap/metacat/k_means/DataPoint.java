package opendap.metacat.k_means;

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
	private double mX, mY, mZ;
	private String mObjName;
	private String mComp;
	private int mCompNum;
	private Cluster mCluster;
	private double mEuDt;

	public DataPoint(double fv[], String name, String comp, int compNum) {
		this(fv[0], fv[1], fv[2], name, comp, compNum);
	}
	
	public DataPoint(int fv[], String name, String comp, int compNum) {
		this((double)fv[0], (double)fv[1], (double)fv[2], name, comp, compNum);
	}
	
	public DataPoint(double x, double y, double z, String name, String comp, int compNum) {
		this.mX = x;
		this.mY = y;
		this.mZ = z;
		this.mObjName = name;
		this.mComp = comp;
		this.mCompNum = compNum;
		this.mCluster = null;
	}

	public void setCluster(Cluster cluster) {
		this.mCluster = cluster;
		calcEuclideanDistance();
	}

	public void calcEuclideanDistance() {

		// called when DP is added to a cluster or when a Centroid is
		// recalculated.
		mEuDt = Math.sqrt(Math.pow((mX - mCluster.getCentroid().getCx()), 2) 
				          + Math.pow((mY - mCluster.getCentroid().getCy()), 2)
				          + Math.pow((mZ - mCluster.getCentroid().getCz()), 2));
	}

	public double testEuclideanDistance(Centroid c) {
		return Math.sqrt(Math.pow((mX - c.getCx()), 2) 
						 + Math.pow((mY - c.getCy()), 2)
						 + Math.pow((mZ - c.getCz()), 2));
	}

	public double getX() {
		return mX;
	}

	public double getY() {
		return mY;
	}

	public double getZ() {
		return mZ;
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

	public String getComp() {
		return mComp;
	}

	public int getCompNum() {
		return mCompNum;
	}

}
