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


import opendap.bes.BESError;
import opendap.bes.BESManager;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.Scrub;
import opendap.http.Util;
import opendap.ppt.PPTException;
import opendap.wcs.v2_0.formats.WcsResponseFormat;
import opendap.wcs.v2_0.http.Attachment;
import opendap.wcs.v2_0.http.MultipartResponse;
import opendap.wcs.v2_0.http.SoapHandler;
import org.apache.http.client.CredentialsProvider;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;


/**
 * Process GetCoverage requests. Static methods are used to construct a wcs:Coverages
 * response.
 */
public class GetCoverageRequestProcessor {


    private static Logger _log = LoggerFactory.getLogger(GetCoverageRequestProcessor.class);


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
    public static void sendCoverageResponse(
            GetCoverageRequest req,
            HttpServletResponse response,
            boolean useSoapEnvelope
    ) throws WcsException, InterruptedException, IOException, PPTException, BadConfigurationException, BESError {

        String id = req.getCoverageID();
        WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(id);
        boolean b = wcsCatalog.hasCoverage(id);

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
            sendFormatResponse(req, response);
        }


    }

    public static void sendFormatResponse(
            GetCoverageRequest req,
            HttpServletResponse response
    ) throws WcsException, InterruptedException, IOException, PPTException, BadConfigurationException, BESError {
        _log.debug("Sending binary data response...");
        response.setHeader("Content-Disposition", getContentDisposition(req));

        String coverageId = req.getCoverageID();
        WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(coverageId);
        String dapDatasetUrl = wcsCatalog.getDapDatsetUrl(coverageId);

        if (dapDatasetUrl.toLowerCase().startsWith(Util.BES_PROTOCOL)) {
            CoverageDescription coverageDescription = wcsCatalog.getCoverageDescription(coverageId);
            String besDatasetId = dapDatasetUrl.substring(Util.BES_PROTOCOL.length());
            Document besCmd = getBesCmd(req, coverageDescription, wcsCatalog);

            if (!BESManager.isInitialized())
                throw new WcsException("The BESManager has not been configured. Unable to access BES!", WcsException.NO_APPLICABLE_CODE);

            BesApi besApi = new BesApi();
            besApi.besTransaction(besDatasetId, besCmd, response.getOutputStream());
        } else if (dapDatasetUrl.toLowerCase().startsWith(Util.HTTP_PROTOCOL) ||
                dapDatasetUrl.toLowerCase().startsWith((Util.HTTPS_PROTOCOL))) {
            CredentialsProvider authCreds = WcsServiceManager.getCredentialsProvider();
            Util.forwardUrlContent(getDap2DataAccessUrl(req), authCreds, response, true);
        }
    }


    public static void sendMultipartGmlResponse(GetCoverageRequest req, HttpServletResponse response, boolean useSoapEnvelope) throws WcsException, InterruptedException {


        _log.debug("Building multi-part Response...");
        String coverageId = req.getCoverageID();
        WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(coverageId);

        String rangePartId = "cid:" + coverageId;

        CoverageDescription coverageDescription = wcsCatalog.getCoverageDescription(req.getCoverageID());

        /**
         * If this an EO coverage then update its bounding box to reflect the subset.
         */
        if (coverageDescription instanceof EOCoverageDescription) {
            try {
                // Mke a copy so we don't bunk up the original.
                EOCoverageDescription eocd = new EOCoverageDescription((EOCoverageDescription) coverageDescription);
                // Tweak the foot print
                eocd.adjustEOMetadataCoverageFootprint(req);
                // Make it the thing to use...
                coverageDescription = eocd;
            } catch (IOException e) {
                throw new WcsException("sendMultipartGmlResponse() - OUCH!! Failed to create new (malleable) " +
                        "EOCoverageDescription using the copy constructor.",
                        WcsException.NO_APPLICABLE_CODE);
            }
        }

        Coverage coverage = coverageDescription.getCoverage(req.getRequestUrl()); // new Coverage(coverageDescription, req.getRequestUrl());

        Element coverageElement = coverage.getCoverageElement(rangePartId, getReturnMimeType(req));

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        _log.debug(xmlo.outputString(coverageElement));


        Document doc = new Document(coverageElement);

        if (useSoapEnvelope)
            doc = SoapHandler.wrapDocumentInSoapEnvelope(doc);


        MultipartResponse mpr = new MultipartResponse();

        Attachment gmlPart = new Attachment("application/gml+xml; charset=UTF-8", "gml-part", doc);
        mpr.addAttachment(gmlPart);


        Attachment rangePart = null;
        String dapDataAccessUrl = wcsCatalog.getDapDatsetUrl(coverageId);
        if (dapDataAccessUrl.toLowerCase().startsWith(opendap.http.Util.BES_PROTOCOL)) {
            String besDatasetId = dapDataAccessUrl.substring(Util.BES_PROTOCOL.length());
            Document besCmd = getBesCmd(req, coverageDescription, wcsCatalog);
            rangePart = new Attachment(getReturnMimeType(req), rangePartId, besDatasetId, besCmd);
        } else {
            rangePart = new Attachment(getReturnMimeType(req), rangePartId, getDap2DataAccessUrl(req), WcsServiceManager.getCredentialsProvider());

        }


        rangePart.setHeader("Content-Disposition", getContentDisposition(req));

        mpr.addAttachment(rangePart);


        try {
            mpr.send(response);
        } catch (Exception e) {
            StringBuilder msg = new StringBuilder("sendMultipartGmlResponse() - ");
            msg.append("Failed to transmit WCS coverage response.");
            msg.append(" Message: ").append(e.getMessage());
            throw new WcsException(msg.toString(), WcsException.NO_APPLICABLE_CODE);
        }

    }


    public static String getReturnFormat(GetCoverageRequest req) throws WcsException, InterruptedException {
        String format = req.getFormat();
        String id = req.getCoverageID();
        if (format == null) {
            CoverageDescription coverageDescription =
                    WcsServiceManager.getCatalog(id).getCoverageDescription(id);
            format = coverageDescription.getNativeFormat();
        }
        return format;
    }


    /**
     * @param req
     * @return
     * @throws WcsException
     * @throws InterruptedException
     */
    public static String getDap2DataAccessUrl(GetCoverageRequest req) throws WcsException, InterruptedException {

        String format = getReturnFormat(req);
        WcsResponseFormat rFormat = ServerCapabilities.getFormat(format);
        if (rFormat == null) {
            throw new WcsException("Unrecognized response format: " + Scrub.fileName(format),
                    WcsException.INVALID_PARAMETER_VALUE, "format");
        }
        WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(req.getCoverageID());
        String requestURL = wcsCatalog.getDapDatsetUrl(req.getCoverageID());

        StringBuilder dap2DataAccessURL = new StringBuilder(requestURL);
        dap2DataAccessURL.append(".").append(rFormat.dapDataResponseSuffix()).append("?").append(getDap2CE(req));
        return dap2DataAccessURL.toString();
    }


    public static String getContentDisposition(GetCoverageRequest req) throws WcsException, InterruptedException {
        String format = getReturnFormat(req);
        WcsResponseFormat rFormat = ServerCapabilities.getFormat(format);
        if (rFormat == null) {
            throw new WcsException("Unrecognized response format: " + Scrub.fileName(format),
                    WcsException.INVALID_PARAMETER_VALUE, "format");
        }
        StringBuilder contentDisposition = new StringBuilder();
        contentDisposition
                .append(" attachment; filename=\"")
                .append(req.getCoverageID())
                .append(rFormat.dapDataResponseSuffix());

        return contentDisposition.toString();
    }


    public static String getReturnMimeType(GetCoverageRequest req) throws WcsException, InterruptedException {
        String format = getReturnFormat(req);
        WcsResponseFormat rFormat = ServerCapabilities.getFormat(format);
        if (rFormat == null)
            throw new WcsException("Unrecognized response format: " + Scrub.fileName(format),
                    WcsException.INVALID_PARAMETER_VALUE, "format");
        return rFormat.mimeType();
    }


    private static String getDap2CE(GetCoverageRequest req) throws InterruptedException, WcsException {

        String coverageID = req.getCoverageID();

        WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(coverageID);
        CoverageDescription coverageDescription = wcsCatalog.getCoverageDescription(coverageID);
        HashMap<String, DimensionSubset> dimensionSubsets = req.getDimensionSubsets();
        HashMap<DomainCoordinate, DimensionSubset> domCordToDimSubsetMap = new HashMap<>();

        // The user may have provided domain subsets.
        // Let's first just QC the request - We'll make sure that the user is asking for dimension
        // subsets of coordinate dimensions that this field has, and while we do that we will associate
        // every matching DomainCoordinate with the DimensionSubset that it matched.
        LinkedHashMap<String, DomainCoordinate> domainCoordinates = coverageDescription.getDomainCoordinates();
        for (DimensionSubset ds : dimensionSubsets.values()) {
            DomainCoordinate dc = domainCoordinates.get(ds.getDimensionId());
            if (dc == null) {

                //
                // It's likely to happen frequently that the user submits a bad dimension name. So
                // take the time to give an informative error message.
                //
                StringBuilder msg = new StringBuilder();
                msg.append("Bad subsetting request.\n");
                msg.append("A subset was requested for dimension '").append(ds.getDimensionId()).append("'");
                msg.append(" and there is no coordinate dimension of that name in the Coverage ");
                msg.append("'").append(coverageDescription.getCoverageId()).append("'\n");

                msg.append("Valid coordinate dimension names for '").append(coverageDescription.getCoverageId()).append("' ");
                msg.append("are: ");
                for (String dcName : domainCoordinates.keySet()) {
                    msg.append("\n    ").append(dcName);
                }
                msg.append("\n");

                _log.debug(msg.toString());

                throw new WcsException(msg.toString(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:dimension");
            }

            ds.setDomainCoordinate(dc);
            domCordToDimSubsetMap.put(dc, ds);
        }
        //
        // Determines which fields (variables) will be sent back with the response.
        // If none are specified, all are sent.
        //
        Vector<String> requestedFields;
        RangeSubset rangeSubset = req.getRangeSubset();
        if (rangeSubset != null) {
            requestedFields = rangeSubset.getRequestedFields();
        } else {
            requestedFields = new Vector<>();
        }
        if (requestedFields.isEmpty()) {
            // if they didn't ask for a subset of the set of fields, then take them all.
            String fieldNames[] = coverageDescription.getFieldNames();
            requestedFields.addAll(Arrays.asList(fieldNames));
            /*
            Vector<Field> fields  = coverageDescription.getFields();
            for (Field field : fields) {
                String requestedFieldName = field.getName();
                requestedFields.add(requestedFieldName);
            }
            */
        }

        //
        // Is there a Scale request?
        //
        // @TODO - Handle scale requests: ScaleRequest scaleRequest = req.getScaleRequest();

        StringBuilder dap2CE = new StringBuilder();

        //
        // Here we begin building the DAP2 CE
        // For every field (variable) to be transmitted we (may) need server side functional expressions,
        // array subset expressions, etc.
        //
        Vector<String> subsetClauses = new Vector<>();
        for (String fieldId : requestedFields) {
            String dapGridArrayName = coverageDescription.getDapGridArrayId(fieldId);

            if (dimensionSubsets.isEmpty()) {
                // no dimension subsets means take the whole enchilada
                subsetClauses.add(dapGridArrayName);
            } else {

                StringBuilder valueSubsetClause = new StringBuilder();
                boolean arraySubset = false;

                // We need to process the value based subsets by using a call to the grid() ssf
                // The array index subsets, if anyexist, need to be applied to the variable as it is
                // passed into the grid() function and we need to get them in the order of the
                // DomainCoordinate variables
                for (DomainCoordinate domainCoordinate : domainCoordinates.values()) {

                    DimensionSubset dimSub = domCordToDimSubsetMap.get(domainCoordinate);
                    if (dimSub == null) {
                        // No subset on this Coordinate? that's ok, make one that get's the entire dim
                        dimSub = new DimensionSubset(domainCoordinate);
                    }

                    if (dimSub.isValueSubset()) {
                        // A value subset means that the user supplied values of the domain coordinates that specify
                        // the bounds of the subset that they want
                        if (valueSubsetClause.length() > 0) {
                            _log.debug("getDap2CE() - DAP2 CE: {}", dap2CE);
                            valueSubsetClause.append(",");
                        }
                        // Then we tack on the value constraint expression: "low<=dimName<=high"
                        valueSubsetClause.append(dimSub.getDap2GridValueConstraint());
                    } else if (dimSub.isArraySubset()) {
                        // An Array subset means that user indicated (through the use of integer values in
                        // their subset request) that they are wanting to subset by array index.
                        // Because order of the [] array notation in DAP URL's is important, we collect
                        // all of the user provided array constraints here and then literally sort them out below
                        // for inclusion in the response.

                        DomainCoordinate domCoord = domainCoordinates.get(dimSub.getDimensionId());
                        domCoord.setArraySubset(dimSub.getDapArrayIndexConstraint());
                        arraySubset = true;
                    } else {
                        throw new WcsException("Unrecognized dimension subset.", WcsException.NO_APPLICABLE_CODE);
                    }
                }

                // So we've processed all the user requested dimension subsets, now we need to build the inditial
                // array subsetting clause if needed.
                StringBuilder fieldSubsetClause = new StringBuilder();

                // If theres value based subsetting to be done we'll need the grid() ssf
                if (valueSubsetClause.length() > 0) {
                    fieldSubsetClause.append("grid(");
                }
                // then the name of the variable
                fieldSubsetClause.append(dapGridArrayName);

                // Add any inditial array subsets to the variable name
                if (arraySubset) {
                    // We build the subsetting string using the domain coordinates in the order they
                    // appear in the DAP dataset, which is how they MUST occur in the configuration
                    // or this all gets broken.
                    for (DomainCoordinate dc : domainCoordinates.values()) {
                        String clause = dc.getArraySubset();
                        clause = clause == null ? "[*]" : clause;
                        fieldSubsetClause.append(clause);
                    }
                }

                // Add the value subsets (to the grid() syntax)
                if (valueSubsetClause.length() > 0) {
                    fieldSubsetClause.append(",");
                    fieldSubsetClause.append(valueSubsetClause);
                    fieldSubsetClause.append(")");
                }

                // add to the list of clauses.
                subsetClauses.add(fieldSubsetClause.toString());


            } // dimension subsets
        } // fields

        for (String subsetClause : subsetClauses) {
            String comma_as_needed = dap2CE.length() > 0 ? "," : "";
            dap2CE.append(comma_as_needed).append(subsetClause);
        }

        _log.debug("getDap2CE() - DAP2 CE: {}", dap2CE);

        try {
            return URLEncoder.encode(dap2CE.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            _log.error("getDap2CE() - Unable to URLEncoder.encode() DAP CE: '{}'", dap2CE);
            throw new WcsException("Failed URL encode DAP2 CE: " + dap2CE + "'", WcsException.NO_APPLICABLE_CODE);
        }

    }


    /**
     * @param req
     * @return
     * @throws InterruptedException
     * @throws WcsException
     */
    /*
    private static String getDap2CE_OLD(GetCoverageRequest req) throws InterruptedException, WcsException {
        String coverageID = req.getCoverageID();
        WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(coverageID);
        CoverageDescription coverageDescription = wcsCatalog.getCoverageDescription(coverageID);
        Vector<Field> fields = coverageDescription.getFields();
        HashMap<String, DimensionSubset> dimensionSubsets = req.getDimensionSubsets();
        //
        // The user may have provided domain subsets.
        // Let's first just QC the request - We'll make sure that the user is asking for dimension
        // subsets of coordinate dimensions that this field has, and while we do that we will associate
        // every matching DomainCoordinate with the DimensionSubset that it matched.
        LinkedHashMap<String, DomainCoordinate> domainCoordinates = coverageDescription.getDomainCoordinates();
        for(DimensionSubset ds: dimensionSubsets.values()){
            DomainCoordinate dc = domainCoordinates.get(ds.getDimensionId());
            if(dc==null){
                //
                //It's likely to happen frequently that the user submits a bad dimension name. So
                //take the time to give an informative error message.
                //
                StringBuilder msg = new StringBuilder();
                msg.append("Bad subsetting request.\n");
                msg.append("A subset was requested for dimension '").append(ds.getDimensionId()).append("'");
                msg.append(" and there is no coordinate dimension of that name in the Coverage ");
                msg.append("'").append(coverageDescription.getCoverageId()).append("'\n");
                msg.append("Valid coordinate dimension names for '").append(coverageDescription.getCoverageId()).append("' ");
                msg.append("are: ");
                for(String dcName :domainCoordinates.keySet()){
                    msg.append("\n    ").append(dcName);
                }
                msg.append("\n");
                _log.debug(msg.toString());
                throw new WcsException(msg.toString(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:dimension");
            }
            ds.setDomainCoordinate(dc);
        }
        //
        // Determines which fields (variables) will be sent back with the response.
        // If none are specified, all are sent.
        //
        Vector<String> requestedFields;
        RangeSubset rangeSubset =  req.getRangeSubset();
        if(rangeSubset!=null) {
            requestedFields = rangeSubset.getRequestedFields();
            if (requestedFields.isEmpty()) {
                // if they didn't ask for a subset of the set of fields, then take them all.
                for (Field field : fields) {
                    requestedFields.add(field.getName());
                }
            }
        }
        else {
            requestedFields = new Vector<>();
        }

        //
        // Is there a Scale request?
        //
        ScaleRequest sr = req.getScaleRequest();
        StringBuilder dap2CE = new StringBuilder();
        //
        // Here we begin building the DAP2 CE
        // For every field (variable) to be transmitted we (may) need server side functional expressions,
        // array subset expressions, ect.
        //
        //
        Vector<String> gridSubsetClauses = new Vector<>();
        Vector<String> arraySubsetClauses =  new Vector<>();
        for(String fieldId: requestedFields){
            String dapGridArrayName = coverageDescription.getDapGridArrayId(fieldId);
            if(dimensionSubsets.isEmpty()){
                // no dimension subsets means take the whole enchilada
                arraySubsetClauses.add(dapGridArrayName);
            }
            else {
                // So we need to process the value based subsets with a call to grid
                // and the array index subsets with an appended array index subset for that.
                StringBuilder ssfGridSubsetClause = new StringBuilder();
                boolean arraySubset = false;
                // Process each dimension subset term the user has submitted
                for (DimensionSubset dimSub : dimensionSubsets.values()) {
                    if(dimSub.isValueSubset()) {
                        // A value subset means that the user supplied values of the domain coordinates that specify
                        // the bounds of the subset that they want
                        if(ssfGridSubsetClause.length()==0){
                            // the first dimension subset needs the grid ssf function
                            // declaration and the name of the Grid array and a comma
                            // separator.
                            ssfGridSubsetClause.append("grid(").append(dapGridArrayName).append(",");
                        }
                        else {
                            // subsequent dimensions just need the comma separator
                            ssfGridSubsetClause.append(",");
                        }
                        // Then we tack on the value constraint expression: "low<=dimName<=high"
                        ssfGridSubsetClause.append(dimSub.getDap2GridValueConstraint());
                    }
                    else if(dimSub.isArraySubset()) {
                        // An Array subset means that user indicated (through the use of integer values in
                        // their subset request) that they are wanting to subset by array index.
                        // Because order of the [] array notation in DAP URL's is important, we collect
                        // all of the user provided array constraints here and then literally sort them out below
                        // for inclusion in the response.
                        DomainCoordinate domCoord =  domainCoordinates.get(dimSub.getDimensionId());
                        domCoord.setArraySubset(dimSub.getDapArrayIndexConstraint());
                        arraySubset = true;
                    }
                    else {
                        throw new WcsException("Unrecognized dimension subset.",WcsException.NO_APPLICABLE_CODE);
                    }
                }
                // So we've processed all the user requested dimension subsets, now we need to build the inditial
                // array subsetting clause if needed.
                StringBuilder arraySubsetClause = new StringBuilder();
                if(arraySubset){
                    arraySubsetClause.append(dapGridArrayName);
                    // We build the subsetting string using the domain coordinates in the order they
                    // appear in the DAP dataset, which is how they MUST occur in the configuration
                    // or this all gets broken.
                    for(DomainCoordinate dc : domainCoordinates.values()){
                        String clause = dc.getArraySubset();
                        clause = clause==null?"[*]":clause;
                        arraySubsetClause.append(clause);
                    }
                }
                if(ssfGridSubsetClause.length()>0){
                    ssfGridSubsetClause.append(")");
                    //if(arraySubsetClause.length()>0){
                    //    ssfGridSubsetClause.append(",");
                    //}
                    gridSubsetClauses.add(ssfGridSubsetClause.toString());
                }
                if(arraySubsetClause.length()>0){
                    arraySubsetClauses.add(arraySubsetClause.toString());
                    //ssfGridSubsetClause.append(arraySubsetClause);
                }
                //dap2CE.append(ssfGridSubsetClause);
            } // dimension subsets
        } // fields
        for(String gridSubsetClause: gridSubsetClauses){
            String comma_as_needed = dap2CE.length()>0 ? "," : "";
            String possiblyScaledGridSubset = sr.getScaleExpression(gridSubsetClause);
            dap2CE.append(comma_as_needed).append(possiblyScaledGridSubset);
        }

        for(String arraySubsetClause: arraySubsetClauses){
            String comma_as_needed = dap2CE.length()>0 ? "," : "";
            dap2CE.append(comma_as_needed).append(arraySubsetClause);
        }

        try {
            _log.debug("getDap2CE() - DAP2 CE: {}",dap2CE);
            return URLEncoder.encode(dap2CE.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            _log.error("getDap2CE() - Unable to URLEncoder.encode() DAP CE: '{}'",dap2CE);
            throw new WcsException("Failed URL encode DAP2 CE: "+dap2CE+"'",WcsException.NO_APPLICABLE_CODE);
        }
    }

*/
    public static Document getBesCmd(GetCoverageRequest req, CoverageDescription cd, WcsCatalog wcsCatalog) throws WcsException, InterruptedException {

        Document besCmd = null;
        String dap2ce = GetCoverageRequestProcessor.getDap2CE(req);
        String format = GetCoverageRequestProcessor.getReturnFormat(req);
        WcsResponseFormat rFormat = ServerCapabilities.getFormat(format);
        if (rFormat == null)
            throw new WcsException("The requested return format '" + format + "' is not recognized by this service.", WcsException.INVALID_PARAMETER_VALUE, "format");


        String besUrl = wcsCatalog.getDapDatsetUrl(cd.getCoverageId());
        String besDatatsetId = besUrl.substring(Util.BES_PROTOCOL.length());

        if (!BESManager.isInitialized())
            throw new WcsException("The BESManager has not been configured. Unable to access BES!", WcsException.NO_APPLICABLE_CODE);

        BesApi besApi = new BesApi();
        try {

            switch (rFormat.type()) {
                case dap2:
                    besCmd =
                            besApi.getDap2RequestDocumentAsync(
                                    opendap.bes.dap2Responders.BesApi.DAP2_DATA,
                                    besDatatsetId,
                                    dap2ce,
                                    null,
                                    null,
                                    "3.2",
                                    0,
                                    null,
                                    null,
                                    null,
                                    opendap.bes.dap2Responders.BesApi.XML_ERRORS);
                    break;

                case netcdf:
                    besCmd =
                            besApi.getDap2DataAsNetcdf4Request(
                                    besDatatsetId,
                                    dap2ce,
                                    req.getCfHistoryAttribute(),
                                    "3.2",
                                    0);
                    break;

                case geotiff:
                    besCmd =
                            besApi.getDap2DataAsGeoTiffRequest(
                                    besDatatsetId,
                                    dap2ce,
                                    "3.2",
                                    0);
                    break;

                case jpeg2000:
                    besCmd =
                            besApi.getDap2DataAsGmlJpeg2000Request(
                                    besDatatsetId,
                                    dap2ce,
                                    "3.2",
                                    0);
                    break;

                case dap4:
                default:
                    throw new WcsException(("Unsupported format: '" + rFormat.name()) + "' :(",
                            WcsException.INVALID_PARAMETER_VALUE, "format");
            }

        } catch (BadConfigurationException bce) {
            throw new WcsException("Failed to generate a BES command for the GetCoverage request: '" + req.toString()
                    , WcsException.NO_APPLICABLE_CODE);

        }

        return besCmd;


    }


}
