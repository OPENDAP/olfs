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

import org.jdom.Element;

import java.util.Collection;
import java.util.Vector;

public class DummyCatalog implements WcsCatalog {

    @Override
    public void init(Element config, String cacheDir, String resourcePath) throws Exception {

    }

    @Override
    public boolean hasCoverage(String coverageId) throws InterruptedException {
        return false;
    }

    @Override
    public CoverageDescription getCoverageDescription(String coverageId) throws InterruptedException, WcsException {
        return null;
    }

    @Override
    public Element getCoverageDescriptionElement(String coverageId) throws InterruptedException, WcsException {
        return null;
    }

    @Override
    public Element getCoverageSummaryElement(String coverageId) throws InterruptedException, WcsException {
        return null;
    }

    @Override
    public Collection<Element> getCoverageSummaryElements() throws InterruptedException, WcsException {
        return new Vector<>();
    }

    @Override
    public Collection<Element> getDatasetSeriesSummaryElements() throws InterruptedException, WcsException {
        return new Vector<>();
    }

    @Override
    public String getDapDatsetUrl(String coverageID) throws InterruptedException {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void update() throws Exception {

    }

    @Override
    public EOCoverageDescription getEOCoverageDescription(String id) throws WcsException {
        return null;
    }

    @Override
    public EODatasetSeries getEODatasetSeries(String id) throws WcsException {
        return null;
    }

    @Override
    public boolean hasEoCoverage(String id) {
        return false;
    }

    @Override
    public boolean matches(String coverageId) {
        return false;
    }
}
