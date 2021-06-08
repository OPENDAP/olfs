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
package opendap.ppt;

import opendap.io.ChunkProtocol;

public class PPTSessionProtocol implements ChunkProtocol {
    // Message when the mapper fails to find the proper protocol string
    public static final String PPT_PROTOCOL_UNDEFINED = "PPT_PROTOCOL_UNDEFINED";

    public static final String PPT_CLIENT_TESTING_CONNECTION = "PPTCLIENT_TESTING_CONNECTION";

    public static final String PPT_SERVER_CONNECTION_OK = "PPTSERVER_CONNECTION_OK";

    public static final String PPT_COMPLETE_DATA_TRANSMISSION = "PPT_COMPLETE_DATA_TRANSMITION";

    public static final String PPT_EXIT_NOW = "PPT_EXIT_NOW";



    public static final String PPTCLIENT_REQUEST_AUTHPORT = "PPTCLIENT_REQUEST_AUTHPORT";


    public String serverProtocolUndefined(){
        return PPT_PROTOCOL_UNDEFINED;
    }
    public String clientTestingConnection(){
        return PPT_CLIENT_TESTING_CONNECTION;

    }
    public String serverConnectionOk(){
        return PPT_SERVER_CONNECTION_OK;

    }
    public String clientCompleteDataTransmission(){
        return PPT_COMPLETE_DATA_TRANSMISSION;

    }
    public String clientExitingNow(){
        return PPT_EXIT_NOW;

    }

}
