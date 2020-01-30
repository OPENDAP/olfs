/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
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

package opendap.dap;

import opendap.auth.IdFilter;
import opendap.auth.UserProfile;
import opendap.bes.BES;
import opendap.bes.BESManager;
import opendap.bes.BadConfigurationException;
import opendap.coreServlet.ReqInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 6/8/11
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class User {

    private Logger log;
    private UserProfile userProfile;
    private HttpServletRequest request;


    public User(HttpServletRequest req){

        log = LoggerFactory.getLogger(this.getClass());
        request = req;

        HttpSession session = request.getSession(false);
        if(session!=null) {
            userProfile= (UserProfile) session.getAttribute(IdFilter.USER_PROFILE);
            log.debug(userProfile.toString());
        }

    }

    public String getUID(){
        if(userProfile!=null){
            return userProfile.getUID();
        }
        return request.getRemoteUser();
    }

    public String getRelativeUrl(){
        return ReqInfo.getLocalUrl(request);
    }


    public int getMaxResponseSize(){

        if(getUID()==null) {

            BES bes;
            try {
                bes = BESManager.getBES(getRelativeUrl());
            } catch (BadConfigurationException e) {
                return 0;
            }
            return bes.getMaxResponseSize();
        }

        return 0;
    }
}
