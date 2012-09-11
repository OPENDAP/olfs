/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2012 OPeNDAP, Inc.
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
package opendap.dap;

import opendap.bes.BESManager;
import opendap.coreServlet.ReqInfo;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 6/8/11
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class User {


    String userName;
    String dataSource;


    public User(HttpServletRequest request){

        userName = request.getRemoteUser();
        String relativeUrl = ReqInfo.getLocalUrl(request);
        dataSource = ReqInfo.getBesDataSourceID(relativeUrl);

    }


    public int getMaxResponseSize(){


        if(userName==null) {
            return BESManager.getBES(dataSource).getMaxResponseSize();
        }

        return 0;
    }
}
