/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
// Author:  Patrick West <pwest@hao.ucar.edu>
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


package opendap.ppt ;

class PPTSessionProtocol {
    // Message when the mapper fails to find the proper protocol string
    public static final String PPT_PROTOCOL_UNDEFINED = "PPT_PROTOCOL_UNDEFINED";

    // From client to server
    public static final String PPTCLIENT_TESTING_CONNECTION = "PPTCLIENT_TESTING_CONNECTION";
    public static final String PPTCLIENT_COMPLETE_DATA_TRANSMITION = "PPTCLIENT_COMPLETE_DATA_TRANSMITION";
    public static final String PPTCLIENT_EXIT_NOW = "PPTCLIENT_EXIT_NOW";

    // From server to client

    public static final String PPTSERVER_CONNECTION_OK = "PPTSERVER_CONNECTION_OK";
    public static final String PPTSERVER_COMPLETE_DATA_TRANSMITION = "PPTSERVER_COMPLETE_DATA_TRANSMITION";
    public static final String PPTSERVER_EXIT_NOW = "PPTSERVER_EXIT_NOW";
}

