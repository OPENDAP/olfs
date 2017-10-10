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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

/**
 * Represents a wcs:Coverage object. A wcs:Coverage object is part of a response to a wcs:GetCoverageRequest
 *
 *
 *
 *
 *
 *
 */
public class Coverage {
    private Logger log;
    private CoverageDescription _myCD;
    private String _requestUrl;

    void init() throws WcsException {
        log = LoggerFactory.getLogger(getClass());

    }

    public Coverage( CoverageDescription cd, String requestUrl) throws WcsException, InterruptedException {
        _myCD = cd;
        if(_myCD==null)
            throw new WcsException("Received a null CoverageDescription",WcsException.NO_APPLICABLE_CODE,"java::opendap.wcs.v_2_0.Coverage");
        _requestUrl = requestUrl;
        init();
    }


    protected CoverageDescription getCoverageDescription(){
        return _myCD;
    }



    public Element getCisCoverageElement(String rangeValuesPartID, String mimeType)throws WcsException{
        return null;
    }


    /**
     *
     * @param rangeValuesPartID  The ID of the multipart document part that carries the range values.
     * @param mimeType  The mimeType of the range values encoding
     * @return An instance of an gmlcov:AbstractCoverage element (Like gmlcov:GridCoverage or gmlcov:RectifiedGridCOverage)
     * @throws WcsException When the bad things happen.
     */
    public  Element getCoverageElement(String rangeValuesPartID, String mimeType) throws WcsException {
        Element coverage;
        String coverageSubtype = _myCD.getCoverageSubtype();
        coverage = new Element(coverageSubtype,WCS.GMLCOV_NS);

        coverage.addNamespaceDeclaration(WCS.WCS_NS);
        coverage.addNamespaceDeclaration(WCS.OWS_NS);
        coverage.addNamespaceDeclaration(WCS.XSI_NS);

        StringBuilder schemaLocation = new StringBuilder();

        schemaLocation
                .append(WCS.WCS_NAMESPACE_STRING).append(" ")
                .append(WCS.WCS_SCHEMA_LOCATION_BASE).append("wcsAll.xsd").append("   ")
                .append(WCS.GML_NAMESPACE_STRING).append(" ")
                .append(WCS.GML_SCHEMA_LOCATION_BASE).append("gml.xsd").append("   ")
                .append(WCS.GMLCOV_NAMESPACE_STRING).append(" ")
                .append(WCS.GMLCOV_SCHEMA_LOCATION_BASE).append("gmlcovAll.xsd").append("   ")
                .append(WCS.SWE_NAMESPACE_STRING).append(" ")
                .append(WCS.SWE_SCHEMA_LOCATION_BASE).append("swe.xsd")
        ;
        coverage.setAttribute("schemaLocation",schemaLocation.toString(),WCS.XSI_NS);

        String gmlId = _myCD.getGmlId();
        if(gmlId!=null)
            coverage.setAttribute("id",gmlId,WCS.GML_NS);

        Vector<Element> abstractFeatureTypeContent = _myCD.getAbstractFeatureTypeContent();
        coverage.addContent(abstractFeatureTypeContent);
        coverage.addContent(_myCD.getDomainSet());
        coverage.addContent(getRangeSet(rangeValuesPartID, mimeType));
        coverage.addContent(_myCD.getRangeType());
        return coverage;
    }







    /**
     *
     *
     *
     *
     <gml:rangeSet>
         <gml:File>
             <gml:rangeParameters
                     xlink:href="cid:200803061600__HFRadar__USEGC__6km__rtv__SIO.nc"
                     xlink:role="http://www.opengis.net/spec/WCS_coverage-encoding_netcdf/req/CF-netCDF"
                     xlink:arcrole="fileReference"/>
             <gml:fileReference>cid:200803061600__HFRadar__USEGC__6km__rtv__SIO.nc</gml:fileReference>
             <gml:fileStructure/>
             <gml:mimeType>application/x-netcdf</gml:mimeType>
         </gml:File>
     </gml:rangeSet>
     *
     *
     * @param rangeValuesPartID  The ID of the multipart document part that carries the range values.
     * @param mimeType  The mimeType of the range values encoding
     * @return Returns the appropriate gml:rangeSet element.
     * @return
     * @throws WcsException
     */
    private Element getRangeSet(String rangeValuesPartID, String mimeType) throws WcsException {

        Element rangeSet = new Element("rangeSet",WCS.GML_NS);
        Element file = new Element("File",WCS.GML_NS);

        String ogcDatasetEncodingUri = OgcDataEncoding.getEncodingUri(mimeType);

        if(ogcDatasetEncodingUri==null){
            String msg = "Internal Service Error: No known OGC data encoding standard for MIME-TYPE: "+mimeType;
            log.error(msg);
            throw new WcsException(msg,WcsException.NO_APPLICABLE_CODE);
        }

        Element rangeParameters = new Element("rangeParameters",WCS.GML_NS);
        rangeParameters.setAttribute("href",rangeValuesPartID,WCS.XLINK_NS);
        rangeParameters.setAttribute("role",ogcDatasetEncodingUri,WCS.XLINK_NS);
        rangeParameters.setAttribute("arcrole","fileReference",WCS.XLINK_NS);
        file.addContent(rangeParameters);

        Element fileReference =  new Element("fileReference",WCS.GML_NS);
        fileReference.setText(rangeValuesPartID);
        file.addContent(fileReference);

        Element fileStructure =  new Element("fileStructure",WCS.GML_NS);
        file.addContent(fileStructure);

        Element mimeTypeElement =  new Element("mimeType",WCS.GML_NS);
        mimeTypeElement.setText(mimeType);

        rangeSet.addContent(file);

        return rangeSet;
    }

    public String getRequestUrl() {
        return _requestUrl;
    }

}
