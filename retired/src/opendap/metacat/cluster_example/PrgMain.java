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

/*-----------------PrgMain.java---------------*/

//import org.c4s.algorithm.cluster.DataPoint;
//import org.c4s.algorithm.cluster.JCA;
import java.util.Vector;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: shyam.s Date: Apr 18, 2004 Time: 4:26:06 PM
 */
public class PrgMain {
	public static void main(String args[]) {
		Vector<DataPoint> dataPoints = new Vector<DataPoint>();
		dataPoints.add(new DataPoint(22, 21, "p53"));
		dataPoints.add(new DataPoint(19, 20, "bcl2"));
		dataPoints.add(new DataPoint(18, 22, "fas"));
		dataPoints.add(new DataPoint(1, 3, "amylase"));
		dataPoints.add(new DataPoint(3, 2, "maltase"));

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
	}
}
