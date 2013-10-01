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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * User: ndp
 * Date: Aug 14, 2008
 * Time: 1:15:27 PM
 */
public class WcsExceptionReport {

    private static final Namespace _nameSpace = WCS.OWS_NS;
    private static final String _schemaLocation = WCS.OWS_NAMESPACE_STRING + "  " +WCS.OWS_SCHEMA_LOCATION_BASE+"owsExceptionReport.xsd";




    private final Document report;

    public WcsExceptionReport(){
        Element root = new Element("WcsExceptionReport", _nameSpace);
        root.addNamespaceDeclaration(WCS.XSI_NS);
        root.setAttribute("schemaLocation", _schemaLocation,WCS.XSI_NS);

        report = new Document();
        report.setRootElement(root);
    }

    public WcsExceptionReport(WcsException exp){
        this();
        addException(exp);
    }

    public void addException(WcsException exp){
        report.getRootElement().addContent(exp.getExceptionElement());
    }

    public void serialize(OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(report, os);
    }

    public String toString() {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        return xmlo.outputString(report);
    }

    public Document getReport(){
        return report;
    }


}
