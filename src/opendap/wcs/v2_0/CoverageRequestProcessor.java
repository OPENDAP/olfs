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
package opendap.wcs.v2_0;


import opendap.coreServlet.Scrub;
import opendap.wcs.v2_0.http.Attachment;
import opendap.wcs.v2_0.http.MultipartResponse;
import opendap.wcs.v2_0.http.SoapHandler;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Vector;

/**
 * Process GetCoverage requests. Static methods are used to construct a wcs:Coverages
 * response.
 */
public class CoverageRequestProcessor {


    private static Logger log = LoggerFactory.getLogger(CoverageRequestProcessor.class);


    public static String coveragesContentID = "urn:ogc:wcs:1.1:coverages";


    /**
     * @param req             The GetCoverageRequest object built fromt the client request.
     * @param response        HttpServletResponse object that will receive the response content.
     * @param useSoapEnvelope Instructs the server to make the response a SOAP document.
     * @throws WcsException         When a wcs:Coverage response document cannot be
     *                              constructed for the passed request.
     * @throws InterruptedException When the server may need to stop a (possibly length) request
     * @throws IOException          Wen in the disk or the internets are broken.
     */
    public static void sendCoverageResponse(GetCoverageRequest req, HttpServletResponse response, boolean useSoapEnvelope) throws WcsException, InterruptedException, IOException {

        String id = req.getCoverageID();
        boolean b = CatalogWrapper.hasCoverage(id);

        if (!b)
            throw new WcsException("No such wcs:Coverage: " + Scrub.fileName(id),
                    WcsException.INVALID_PARAMETER_VALUE, "wcs:CoverageId");

        // If the mediaType is specified then we know it must be multipart/related because the spec says that's
        // the only acceptable value (orly?) and the GetCoverageRequest class enforces that rule. And since it
        // only has a single value we know it means we have to send the multipart response with the gml:Coverage
        // in the first part and then the binary stuff as specified in the format parameter in the next part.
        if (req.getMediaType() != null) {

            sendMultipartGmlResponse(req, response, useSoapEnvelope);


        } else {
            sendFormatResponse(req, response, useSoapEnvelope);
        }


    }

    public static void sendFormatResponse(GetCoverageRequest req, HttpServletResponse response, boolean useSoapEnvelope) throws WcsException, InterruptedException, IOException {

        log.debug("Sending binary data response...");

        response.setHeader("Content-Disposition", getContentDisposition(req));

        try {
            opendap.wcs.v2_0.http.Util.forwardUrlContent(getDataAccessUrl(req), response, true);
        } catch (URISyntaxException e) {
            throw new WcsException("Internal server error. Server failed to generate a valid server access URI: " + Scrub.fileName(getDataAccessUrl(req)),
                    WcsException.NO_APPLICABLE_CODE, "DataAccessUrl");
        }


    }


    public static void sendMultipartGmlResponse(GetCoverageRequest req, HttpServletResponse response, boolean useSoapEnvelope) throws WcsException, InterruptedException {


        log.debug("Building multi-part Response...");

        String rangePartId = "cid:" + req.getCoverageID();

        Coverage cvg = new Coverage(req.getCoverageID());

        Element coverage = cvg.getCoverageElement(rangePartId, getReturnMimeType(req));

        Document doc = new Document(coverage);

        if (useSoapEnvelope)
            doc = SoapHandler.wrapDocumentInSoapEnvelope(doc);


        MultipartResponse mpr = new MultipartResponse();

        Attachment gmlPart = new Attachment("application/gml+xml; charset=UTF-8", "gml-part", doc);
        mpr.addAttachment(gmlPart);

        Attachment rangePart = new Attachment(getReturnMimeType(req), rangePartId, getDataAccessUrl(req));
        rangePart.setHeader("Content-Disposition", getContentDisposition(req));

        mpr.addAttachment(rangePart);


        try {
            mpr.send(response);
        } catch (Exception e) {
            throw new WcsException("Failed to transmit WCS coverage response.", WcsException.NO_APPLICABLE_CODE);
        }

    }


    public static String getReturnFormat(GetCoverageRequest req) throws WcsException, InterruptedException {
        CoverageDescription coverageDescription = CatalogWrapper.getCoverageDescription(req.getCoverageID());
        String format = req.getFormat();
        if (format == null) {
            format = coverageDescription.getNativeFormat();
        }
        return format;
    }


    public static String getDataAccessUrl(GetCoverageRequest req) throws WcsException, InterruptedException {
        String dataAccessURL;

        String format = getReturnFormat(req);
        if (format.contains("netcdf")) {
            dataAccessURL = getNetcdfDataAccessURL(req);
        } else if (format.equals("image/geotiff")) {
            dataAccessURL = getGeoTiffDataAccessURL(req);
        } else if (format.equals("application/octet-stream")) {
            dataAccessURL = getDapDataAccessURL(req);
        } else {
            throw new WcsException("Unrecognized response format: " + Scrub.fileName(format),
                    WcsException.INVALID_PARAMETER_VALUE, "wcs:Ouput/@format");
        }

        return dataAccessURL;
    }


    public static String getContentDisposition(GetCoverageRequest req) throws WcsException, InterruptedException {
        String contentDisposition;

        String format = getReturnFormat(req);
        if (format.contains("netcdf")) {
            contentDisposition = " attachment; filename=\"" + req.getCoverageID() + ".nc\"";
        } else if (format.equals("application/octet-stream")) {
            contentDisposition = " attachment; filename=\"" + req.getCoverageID() + ".dods\"";
        } else {
            throw new WcsException("Unrecognized response format: " + Scrub.fileName(format),
                    WcsException.INVALID_PARAMETER_VALUE, "wcs:Ouput/@format");
        }

        return contentDisposition;
    }


    public static String getReturnMimeType(GetCoverageRequest req) throws WcsException, InterruptedException {
        String mime_type;

        String format = getReturnFormat(req);
        if (format.contains("netcdf")) {
            mime_type = "application/x-netcdf";
        } else if (format.equals("application/octet-stream")) {
            mime_type = "application/octet-stream";
        } else {
            throw new WcsException("Unrecognized response format: " + Scrub.fileName(format),
                    WcsException.INVALID_PARAMETER_VALUE, "wcs:Ouput/@format");
        }

        return mime_type;
    }


    public static String getGeoTiffDataAccessURL(GetCoverageRequest req) throws InterruptedException, WcsException {

        String requestURL = CatalogWrapper.getDataAccessUrl(req.getCoverageID());

        requestURL += ".geotiff" + "?" + getDapCE(req);

        return requestURL;
    }

    public static String getDapDataAccessURL(GetCoverageRequest req) throws InterruptedException, WcsException {

        String requestURL = CatalogWrapper.getDataAccessUrl(req.getCoverageID());

        requestURL += ".dods" + "?" + getDapCE(req);

        return requestURL;
    }


    public static String getNetcdfDataAccessURL(GetCoverageRequest req) throws InterruptedException, WcsException {

        String requestURL = CatalogWrapper.getDataAccessUrl(req.getCoverageID());

        requestURL += ".nc" + "?" + getDapCE(req);

        return requestURL;
    }


    public static String getMetadataAccessURL(GetCoverageRequest req) throws InterruptedException, WcsException {

        String requestURL = CatalogWrapper.getDataAccessUrl(req.getCoverageID());

        requestURL += ".ddx";
        //requestURL +=  "/" + req.getCoverageID() + ".ddx"+"?"+getDapCE(req);

        return requestURL;
    }


    private static String getDapCE(GetCoverageRequest req) throws InterruptedException, WcsException {

        StringBuilder dapCE = new StringBuilder();

        String coverageID = req.getCoverageID();


        CoverageDescription coverage = CatalogWrapper.getCoverageDescription(coverageID);


        Vector<Field> fields = coverage.getFields();
        DimensionSubset[] dimensionSubsets = req.getDimensionSubsets();

        for (Field field : fields) {


            if(dapCE.length()>0)
                dapCE.append(",");

            dapCE.append("grid(").append(field.getName()).append(",");

            for (DimensionSubset dimSub : dimensionSubsets) {

                StringBuilder subsetClause = new StringBuilder();

                switch (dimSub.getType()) {

                    case TRIM:
                        subsetClause
                                .append(dimSub.getTrimLow())
                                .append("<=")
                                .append(dimSub.getDimensionId())
                                .append("<=")
                                .append(dimSub.getTrimHigh());

                        break;

                    case SLICEPOINT:
                        subsetClause
                                .append(dimSub.getDimensionId())
                                .append("=")
                                .append(dimSub.getSlicePoint());


                        break;

                    default:
                        throw new WcsException("Unknown Subset Type!", WcsException.INVALID_PARAMETER_VALUE, "subset");

                }

                if (dapCE.length() > 0 && subsetClause.length() > 0)
                    dapCE.append(",");

                dapCE.append(subsetClause.toString());
            }

            dapCE.append(")");


        }


        return dapCE.toString();
    }


}
