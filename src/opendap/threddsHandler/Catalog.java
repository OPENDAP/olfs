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
package opendap.threddsHandler;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.jdom.Document;
import org.jdom.JDOMException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * User: ndp
 * Date: Dec 30, 2008
 * Time: 9:01:30 AM
 */
public interface Catalog {
    void destroy();

    boolean usesMemoryCache();

    boolean needsRefresh();

    void writeCatalogXML(OutputStream os) throws Exception;
    void writeRawCatalogXML(OutputStream os) throws Exception;

    Document getCatalogDocument() throws IOException, JDOMException, SaxonApiException;
    Document getRawCatalogDocument() throws IOException, JDOMException, SaxonApiException;

    XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException;
    XdmNode getRawCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException;

    String getName();

    String getCatalogKey();

    String getPathPrefix();

    String getUrlPrefix();

    String getFileName();

    String getIngestTransformFilename();

    long getLastModified();
}
