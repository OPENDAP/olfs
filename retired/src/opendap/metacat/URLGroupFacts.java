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
package opendap.metacat;

public class URLGroupFacts {
	
	// These are the facts the class stores about a group
	private String datasetRoot;
	private int serverRootPosition;
	private boolean isTimeSeries = false;
	private boolean isMultiFile = false;
	private String firstDate;
	private String lastDate;
	private boolean isDatasetRootUnique = false;
	
	// These facts must be set
	private String firstDDXURL;
	private String firstDDXDoc;
	
	// These are used to maintain consistency
	// private boolean isTimeSeriesSet = false;
	private boolean isDatasetRootSet = false;
	private boolean isServerRootPositionSet = false;
	// private boolean isMultiFileSet = false;
	private boolean isFirstDateSet = false;
	private boolean isLastDateSet = false;
	// private boolean isDatasetRootUniqueSet = false;
	
	public URLGroupFacts(String firstDDXURL, String firstDDXDoc) {
		this.firstDDXURL = firstDDXURL;
		this.firstDDXDoc = firstDDXDoc;
	}
	public String getFirstDDXURL() {
		return firstDDXURL;
	}
	public String getFirstDDXDoc() {
		return firstDDXDoc;
	}

	public boolean getIsDatasetRootUnique() {
		return isDatasetRootUnique;
	}
	public void setIsDatasetRootUnique(boolean v) {
		isDatasetRootUnique = v;
		// isDatasetRootUniqueSet = true;
	}

	public String getFirstDate() throws Exception {
		if (!isFirstDateSet)
			throw new Exception("FirstDate fact not set");
		
		return firstDate;
	}
	public void setFirstDate(String fd) {
		firstDate = fd;
		isFirstDateSet = true;
	}
	
	public String getLastDate() throws Exception {
		if (!isLastDateSet)
			throw new Exception("LastDate fact not set");
		
		return lastDate;
	}
	public void setLastDate(String ld) {
		lastDate = ld;
		isLastDateSet = true;
	}
	

	public boolean getIsMultiFile()  {
		/* if (!isMultiFileSet)
			throw new Exception("IsMultiFile fact not set");*/
		
		return isMultiFile;
	}
	public void setIsMultiFile(boolean imf) {
		isMultiFile = imf;
		//isMultiFileSet = true;
	}
	
	/**
	 * The pathname relative to the OPeNDAP server's root directory to the
	 * start of the directory tree that holds this dataset.
	 * @return The pathname
	 * @throws Exception
	 */
	public String getDatasetRoot() throws Exception {
		if (!isDatasetRootSet)
			throw new Exception("DatasetRoot fact not set");
		
		return datasetRoot;
	}
	public void setDatasetRoot(String dsr) {
		datasetRoot = dsr;
		isDatasetRootSet = true;
	}
	
	/**
	 * The position in the URL where the 'mechanics' end and the pathnames 
	 * start. In a URL like http://machine/servlet/data, this value is the 
	 * location of 'd' in 'data'. This value is useful in extracting pathnames
	 * from a collection of URLs and storing it here saves having to recompute
	 * it thousands of times. 
	 * @return
	 * @throws Exception
	 */
	public int  getServerRootPosition() throws Exception {
		if (!isServerRootPositionSet)
			throw new Exception("ServerRoot fact not set");
		
		return serverRootPosition;
	}
	public void setServerRootPosition(int sr) {
		serverRootPosition = sr;
		isServerRootPositionSet = true;
	}
	
	public boolean getIsTimeSeries() /*throws Exception*/ {
		/*if (!isTimeSeriesSet)
			throw new Exception("IsTimeSeries fact not set");*/
		
		return isTimeSeries;
	}
	public void setIsTimeSeries(boolean its) {
		isTimeSeries = its;
		//isTimeSeriesSet = true;
	}
	
}
