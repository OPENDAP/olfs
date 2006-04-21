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

package opendap.soap;

import org.jdom.Element;
import org.jdom.Text;

import javax.servlet.http.HttpServletRequest;
import java.rmi.server.UID;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Apr 11, 2006
 * Time: 12:04:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoapUtils {


    public static  Element getDDXRequestElement(String path, String constraintExpression){
        Element req = new Element("Request");

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString());

        Element cmd = new Element("GetDDX");

        Element dataset = new Element("DataSet");
        Element dpath = new Element("path");
        Element ce = new Element("ConstraintExpression");

        dpath.addContent(new Text(path));
        ce.addContent(new Text(constraintExpression));

        dataset.addContent(dpath);
        cmd.addContent(dataset);
        dataset.addContent(ce);

        req.addContent(cmd);

        return req;

    }

    public static Element getCatalogRequestElement(String path){
        Element req = new Element("Request");

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString());

        Element cmd = new Element("GetTHREDDSCatalog");

        Element dpath = new Element("path");

        dpath.addContent(new Text(path));

        cmd.addContent(dpath);

        req.addContent(cmd);

        return req;

    }


    public static Element getMultiPartTestResponse(HttpServletRequest srvReq, Element reqElement) {

        Element respElement = new Element("Response");
        respElement.setAttribute("reqID",reqElement.getAttributeValue("reqID"));
        
        return respElement;

    }
}
