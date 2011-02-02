/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
package opendap.wcs.v1_1_2.http;

import opendap.wcs.v1_1_2.WCS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.net.URLDecoder;

import opendap.coreServlet.DispatchServlet;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 9:00:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class PostHandler extends XmlRequestHandler {


    public PostHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }

    public void init(DispatchServlet servlet, Element config) throws Exception {
        super.init(servlet,config);
    }



    public void handleWcsRequest(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, IOException {

        String dataAccessBase = Util.getServiceUrl(request);
        String serviceUrl = Util.getServiceUrlString(request,_prefix);
        BufferedReader  sis = request.getReader();
        ServletOutputStream os = response.getOutputStream();

        String encoding = request.getCharacterEncoding();
        if(encoding==null)
            encoding = "UTF-8";


        String sb = "";
        String reqDoc = "";
        int length;
        while(sb!= null){
            sb = sis.readLine();
            if(sb != null){

                length =  sb.length() + reqDoc.length();
                if( length > WCS.MAX_REQUEST_LENGTH)
                    throw new IOException("Post Body too long. Try again with something smaller.");
                reqDoc += sb;
            }
        }
        if(reqDoc!=null){
            reqDoc = URLDecoder.decode(reqDoc,encoding);

            ByteArrayInputStream baos = new ByteArrayInputStream(reqDoc.getBytes());

            response.setContentType("text/xml");

            Document wcsResponse = getWcsResponse(serviceUrl,this,baos);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            xmlo.output(wcsResponse,os);


        }

    }


}