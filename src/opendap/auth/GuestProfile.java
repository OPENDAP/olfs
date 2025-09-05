/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2018 OPeNDAP, Inc.
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

package opendap.auth;


import java.io.Serializable;

/**
 * Created by ndp on 9/24/14.
 */
public class GuestProfile extends UserProfile implements Serializable {


    public GuestProfile(){
        super();
        setUID("GUEST");
        setEdlProfileAttribute("first_name", "Guest");
        setEdlProfileAttribute("last_name", "Profile");
        setEdlProfileAttribute("email_address", "guest_profile@opendap.org");
        setEdlProfileAttribute("country", "Erewhon");
        setEdlProfileAttribute("affiliation", "unknown");

        addGroup("guest");
        addRole("guest");
    }

}
