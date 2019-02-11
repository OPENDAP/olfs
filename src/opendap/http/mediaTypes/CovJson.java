/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Author: Riley Rimer 
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
package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

public class CovJson extends MediaType {

    public static final String NAME = "covjson";

    public static final String PRIMARY_TYPE = "application";
    public static final String SUB_TYPE = "prs.coverage+json";


    public CovJson(){
        this("."+ NAME);
        setName(NAME);

    }

    public CovJson(String typeMatchString){
        super(PRIMARY_TYPE,SUB_TYPE, typeMatchString);
    }

}
