/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
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
package opendap.wcs;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.util.List;
import java.util.Date;
import java.io.IOException;

/**
 * User: ndp
 * Date: Mar 12, 2008
 * Time: 3:39:45 PM
 */
public class WcsCrDs implements CrawlableDataset, Comparable {
    public Object getConfigObject() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CrawlableDataset getParentDataset() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean exists() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isCollection() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CrawlableDataset getDescendant(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List listDatasets() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List listDatasets(CrawlableDatasetFilter crawlableDatasetFilter) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long length() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date lastModified() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
