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
package opendap.wcs.v1_1_2;

import opendap.coreServlet.Scrub;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 12, 2010
 * Time: 2:02:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class DescribeCoverageRequestProcessor {

    public static Document processDescribeCoveragesRequest(DescribeCoverageRequest req)  throws InterruptedException, WcsException {


        Element coverageDescriptions = new Element("CoverageDescriptions",WCS.WCS_NS);
        CoverageDescription cd;

        for(String id: req.getIds()){
            cd = CatalogWrapper.getCoverageDescription(id);
            if(cd==null)
                throw new WcsException("No such wcs:Coverage: "+ Scrub.fileName(id),
                        WcsException.INVALID_PARAMETER_VALUE,"wcs:Identifier");

            coverageDescriptions.addContent(cd.getElement());
        }

        return new Document(coverageDescriptions);

    }

}
