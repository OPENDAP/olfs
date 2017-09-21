/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0;

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

        coverageDescriptions.addNamespaceDeclaration(WCS.GML_NS);
        coverageDescriptions.addNamespaceDeclaration(WCS.SWE_NS);
        coverageDescriptions.addNamespaceDeclaration(WCS.GMLCOV_NS);
        coverageDescriptions.addNamespaceDeclaration(WCS.XSI_NS);

        StringBuilder schemaLocationValue = new StringBuilder();

        schemaLocationValue.append(WCS.WCS_NAMESPACE_STRING).append(" ").append(WCS.WCS_SCHEMA_LOCATION_BASE+"wcsAll.xsd ");
        schemaLocationValue.append(WCS.GML_NAMESPACE_STRING).append(" ").append(WCS.GML_SCHEMA_LOCATION_BASE+"gml.xsd ");
        schemaLocationValue.append(WCS.SWE_NAMESPACE_STRING).append(" ").append(WCS.SWE_SCHEMA_LOCATION_BASE+"swe.xsd ");
        schemaLocationValue.append(WCS.GMLCOV_NAMESPACE_STRING).append(" ").append(WCS.GMLCOV_SCHEMA_LOCATION_BASE+"gmlcovAll.xsd ");

        coverageDescriptions.setAttribute("schemaLocation",schemaLocationValue.toString(),WCS.XSI_NS);

        CoverageDescription cd;

        String ids[] = req.getIds();
        if(ids!=null && ids.length>0){
            for(String id: ids){
                cd = CatalogWrapper.getCoverageDescription(id);
                coverageDescriptions.addContent(cd.getCoverageDescriptionElement());
            }
        }
        return new Document(coverageDescriptions);

    }

}
