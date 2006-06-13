/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.coreServlet;

import opendap.dap.DODSException;
import opendap.dap.DAS;
import opendap.dap.Server.ServerDDS;
import opendap.dap.parser.ParseException;

import java.io.PrintWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Provides default implmentations of OPeNDAP responses. Methods in this class typically
 * rely on (wrap calls to) other classaes where the implmentations reside.
 * User: ndp
 * Date: Mar 31, 2006
 * Time: 3:02:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultResponse {


    /**
     * Default handler for OPeNDAP ascii requests. Returns OPeNDAP data in
     * comma delimited ascii columns for ingestion into some not so
     * OPeNDAP enabled application such as MS-Excel.
     * @param pw Where to print the ASCII response.
     * @param rs The ReqState object associated with this client request
     * @param is An input stream containing the .dods data response (i.e. a serialized DataDDS)
     * for the dataset requested by the client.
     * @throws DODSException
     * @throws ParseException
     * @throws IOException
     * @see AsciiResponse
     */
    public static void sendAsciiResponse(PrintWriter pw,
                                         ReqState rs,
                                         InputStream is)
            throws DODSException, ParseException, IOException {

        AsciiResponse.sendASCII(pw, rs, is);

    }


    /**
     * Default handler for OPeNDAP .html requests. Returns an html form
     * and javascript code that allows the user to use their browser
     * to select variables and build constraints for a data request.
     * The DDS and DAS for the data set are used to build the form. The
     * types in opendap.servers.www are integral to the form generation.
     *
     * @param pw Where to print the ASCII response.
     * @param rs The ReqState object associated with this client request
     * @param dds The DDS (with attributes) for which to build the request form.
     * @throws DODSException
     * @throws ParseException
     * @see HtmlResponse
     */
    public static void sendHtmlResponse(PrintWriter pw,
                                        ReqState rs,
                                        ServerDDS dds)
            throws DODSException, ParseException {


        HtmlResponse.sendDataRequestForm(pw, rs, dds);


    }

    /**
     * Default implmentation of the OPeNDAP .info reponse. Writes an html document
     * describing the contents of the servers datasets to passed PrintStream.
     *
     * @param pw  Writes the .info document to this PrintStream.
     * @param rs  The ReqState object for this client request. Used to determine the
     *            location of the INFO directory adn anclillary .info documents.
     * @param dds The DDS to build the response from.
     * @throws DODSException
     * @see InfoResponse
     */
    public static void sendInfoResponse(PrintStream pw,
                                        ReqState rs, ServerDDS dds)
            throws DODSException {

        InfoResponse.sendINFO(pw, rs, dds);

    }




    //@todo Write a default sendDirectoryResponse() - may be unneccessary based on THREDDS work

}
