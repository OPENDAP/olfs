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

package opendap.metacat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * Hold a collection of groups. This provides a way to serialize/deserialize
 * these objects so that they can be passed between programs.
 * 
 * @author jimg
 *
 */
public class URLGroups implements Iterable<URLGroup> {
	
	private List<URLGroup> groups = null;
	
    private static Logger log = LoggerFactory.getLogger(URLGroups.class);
    private static final String suffix = "_URLGroups.ser";
    
	public URLGroups() throws Exception {
		log.debug("Making a new set of URL Groups.");
		
		groups = new ArrayList<URLGroup>();
	}

	public URLGroups(String groupsName) throws Exception {
		// by default DDXRetriever uses a read-only cache
		log.debug("Restoring URLGroups from: " + groupsName);
		
		restoreState(groupsName);
	}

	public Iterator<URLGroup> iterator() {
		Iterator<URLGroup> igroup = groups.iterator();
		return igroup;
	}

	public int size() {
		return groups.size();
	}
	
	public void add(URLGroup g) {
		
		log.info("New group: " + g.toString());
		groups.add(g);
	}
	
    @SuppressWarnings("unchecked")
	public void restoreState(String groupName) throws Exception {
		FileInputStream fis;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(groupName + suffix);
			ois = new ObjectInputStream(fis);
			
    		groups = (ArrayList<URLGroup>)ois.readObject();
    	}
    	catch (FileNotFoundException e) {
    		log.error("Could not open the URLGroups sserialiaztion - file not found.");
    	}
		catch (ClassNotFoundException e) {
			throw new Exception(
					"URLGroups: "
							+ "Could not find the class when reading the URLGroups list."
							+ e.getMessage());
		}
		catch (ClassCastException e) {
			throw new Exception(
					"URLGroups: "
							+ "Could not cast the persistent store to a URLGroups list"
							+ e.getMessage());
		}
		catch (java.io.IOException e) {
			throw new Exception("URLGroups: " + "Generic Java I/O Exception."
					+ e.getMessage());
		}    	
		finally {
    		if (ois != null)
    			ois.close();
    	}
    }

    public void saveState(String groupsName) throws Exception {
		FileOutputStream fos;
		ObjectOutputStream oos = null;
    	try {
    		fos = new FileOutputStream(groupsName + suffix);
    		oos = new ObjectOutputStream(fos);

    		oos.writeObject(groups);
    	}
    	catch (FileNotFoundException e) {
			throw new Exception(
					"URLGroups: "
							+ "Could not open the Groups file - file not found."
							+ e.getMessage());
    	}
    	catch (SecurityException e) {
			throw new Exception(
					"URLGroups: "
							+ "Could not open the Groups file - permissions violation."
							+ e.getMessage());
    	}	
    	catch (java.io.IOException e) {
			throw new Exception(
					"URLGroups: "
							+ "Generic Java I/O Exception."
							+ e.getMessage());
    	}
    	finally {
    		if (oos != null)
    			oos.close();
    	}
    }
}