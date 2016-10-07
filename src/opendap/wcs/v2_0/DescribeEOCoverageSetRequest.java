package opendap.wcs.v2_0;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * DescribeEOCoverageSetRequest
 * <p>
 * request = DescribeEOCoverageSet
 * eoId = v1,..,vn where vi = (EOCovergeId | DatasetSeriesId | StichedMosaicId)
 * containment = overlaps | contains
 * count = max_number_of_CoverageDescriptions to return.
 * sections = CoverageDescriptions | DatasetSeriesDescriptions | All
 * <p>
 * subset = (long | lat | phenomenonTime) (low,high)
 * <p>
 * SubsetSpec:  dimension ( interval )
 * dimension:   long | lat | phenomenonTime
 * interval:    low, high
 * low:          point | *
 * high:         point | *
 * point:        number| " token " // " = ASCII 0x42
 * <p>
 * <p>
 * Example:
 * <p>
 * http://www.myserver.org:port/path?
 * service=WCS
 * &version=2.0.1
 * &request=DescribeEOCoverageSet
 * &eoid=C0002
 * &containment=overlaps
 * &subset=long(-71,47)
 * &subset=lat(-66,51)
 * &subset=phenomenonTime("2009-11-06T23:20:52Z","2009-11- 13T23:20:52Z")
 *
 */
public class DescribeEOCoverageSetRequest {


    enum Sections {
        CoverageDescriptions("CoverageDescriptions"),
        DatasetSeriesDescriptions("DatasetSeriesDescriptions"),
        All("All");

        private final String name;

        Sections(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Containment {
        OVERLAPS("overlaps"),
        CONTAINS("contains");

        private final String name;

        Containment(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }


    private Logger _log;

    public static final int DEFAULT_COUNT = 100;
    public static final org.jdom.Namespace NS = WCS.WCSEO_NS;

    public static final String KVP_REQUEST = WCS.REQUEST.DESCRIBE_EO_COVERAGE_SET.toString();

    /**
     * eoId = v1,..,vn where vi = (EOCovergeId | DatasetSeriesId | StichedMosaicId)
     */
    private Vector<String> _eoIds;


    /**
     * containment = overlaps | contains
     */
    private Containment _containment;

    /**
     * count = max_number_of_CoverageDescriptions to return
     */
    private int _count;

    /**
     * sections = CoverageDescriptions | DatasetSeriesDescriptions | All
     */
    Vector<Sections> _sections;


    boolean hasSection(Sections s){
        return _sections.contains(s);
    }

    private HashMap<String, DimensionSubset> _dimensionSubsets;

    private TemporalDimensionSubset _temporalSubset;


    /*
    private enum Subsets {
        LONG("long"),
        LAT("lat"),
        PHENOMENON_TIME("phenomenonTime");

        private final String name;

        Subsets(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
  */

    private DescribeEOCoverageSetRequest(){
        _log = LoggerFactory.getLogger(this.getClass());
        _eoIds = new Vector<>();
        _containment = Containment.OVERLAPS;
        _count = DEFAULT_COUNT;
        _sections = new Vector<>();
        _dimensionSubsets =  new HashMap<>();
        _temporalSubset = null;
    }

    /**
     * request
     * eoId
     * containment
     * count
     * sections
     * dimenstionTrim
     *
     * @param descrEoCovSetElem
     */
    public DescribeEOCoverageSetRequest(Element descrEoCovSetElem) throws WcsException {
        this();
        throw new WcsException("XML requests not supported.",
                WcsException.OPERATION_NOT_SUPPORTED,
                "EO-WCS XML/SOAP Request Encoding ");
    }


    /**
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * DescribeEOCoverageSetRequest
     * <p>
     * request = DescribeEOCoverageSet
     * eoId = v1,..,vn where vi = (EOCovergeId | DatasetSeriesId | StichedMosaicId)
     * containment = overlaps | contains
     * count = max_number_of_CoverageDescriptions to return.
     * sections = CoverageDescriptions | DatasetSeriesDescriptions | All
     * <p>
     * subset = (long | lat | phenomenonTime) (low,high)
     * <p>
     * SubsetSpec:  dimension ( interval )
     * dimension:   long | lat | phenomenonTime
     * interval:    low, high
     * low:          point | *
     * high:         point | *
     * point:        number| " token " // " = ASCII 0x42
     * <p>
     * <p>
     * Example:
     * <p>
     * http://www.myserver.org:port/path?
     * service=WCS
     * &version=2.0.1
     * &request=DescribeEOCoverageSet
     * &eoid=C0002
     * &containment=overlaps
     * &subset=long(-71,47)
     * &subset=lat(-66,51)
     * &subset=phenomenonTime("2009-11-06T23:20:52Z","2009-11- 13T23:20:52Z")
     *
     * @param kvp
     */
    public DescribeEOCoverageSetRequest(Map<String, String[]> kvp)
            throws WcsException {

        this();


        String vals[];
        String s;

        vals = kvp.get("request");
        if (vals == null) {
            throw new WcsException("The 'request' query parameter is required for a  " + KVP_REQUEST + " request.", WcsException.MISSING_PARAMETER_VALUE, "request");
        }
        if (!vals[0].equalsIgnoreCase(KVP_REQUEST)) {
            throw new WcsException("The 'request' query parameter must be set to  " +
                    KVP_REQUEST + " because that's what this code is for.",
                    WcsException.INVALID_PARAMETER_VALUE, "request");

        }

        vals = kvp.get("eoid");
        if (vals == null) {
            throw new WcsException("The 'eoId' query parameter is required for a  " + KVP_REQUEST + " request.", WcsException.MISSING_PARAMETER_VALUE, "eoId");
        }
        String ids[] = vals[0].split(",");
        Collections.addAll(_eoIds, ids);
        _log.debug("DescribeEOCoverageSetRequest() - Got {} eoId values.", _eoIds.size());


        vals = kvp.get("containment");
        if (vals != null) {
            if (vals[0].equalsIgnoreCase(Containment.OVERLAPS.toString())) {
                _containment = Containment.OVERLAPS;
            } else if (vals[0].equalsIgnoreCase(Containment.CONTAINS.toString())) {
                _containment = Containment.CONTAINS;
            }

        }
        _log.debug("DescribeEOCoverageSetRequest() - Containment set to: '{}'", _containment);

        vals = kvp.get("count");
        if (vals != null) {
            try {
                _count = Integer.parseInt(vals[0]);
            } catch (NumberFormatException e) {
                throw new WcsException("The 'count' query parameter's value must be a string representation of an integer.", WcsException.INVALID_PARAMETER_VALUE, "count");
            }
        }
        _log.debug("DescribeEOCoverageSetRequest() - Count set to: {} (default: {})", _count, DEFAULT_COUNT);

        vals = kvp.get("sections");
        if (vals != null) {

            String secs[] = vals[0].split(",");
            for (String id : secs) {

                if (id.equalsIgnoreCase(Sections.CoverageDescriptions.toString())) {
                    _sections.add(Sections.CoverageDescriptions);
                } else if (id.equalsIgnoreCase(Sections.DatasetSeriesDescriptions.toString())) {
                    _sections.add(Sections.DatasetSeriesDescriptions);
                } else if (id.equalsIgnoreCase(Sections.All.toString())) {
                    _sections.add(Sections.All);
                } else {
                    _log.warn("DescribeEOCoverageSetRequest() - Encountered unrecognized section name '{}' - SKIPPING.)", id);
                }
            }
        } else {
            _sections.add(Sections.All);
        }
        _log.debug("DescribeEOCoverageSetRequest() - sections has {} elements)", _sections.size());

        vals = kvp.get("subset");
        if (vals != null) {

            for (String subset : vals) {

                DimensionSubset ds = new DimensionSubset(subset);

/**
 * The EOWCS-2.0 spec [OGC 10-140r1] says:
 * "The GetCoverage request is unchanged over WCS Core [OGC 09-110r4], except that for EO Coverages
 * slicing is disallowed as it would leave the EO Metadata undefined."
 *
 * In short: No Slice subsetting because it breaks the metadata description of the object.
 *
 * I think this restriction is a bad choice because it sacrifices data access in order to protect the
 * metadata integrity of the WCS service. Additionally, in the case of servers that are returning complex
 * data objects like NetCDF, DAP2, and DAP4 the response comes, in every case, with syntactic metadata that will always
 * be correct for the returned object, regardless of issues with EO metadata delivered in a CoverageDescription.
 * The NetCDF and DAP4 responses may also contain additional semantic metadata describing the returned object which
 * may or may not be correct. So banning slicing in no way protects the various metadata chains from inaccuracy.
 *
 * And finally: How is a "SlicePoint" different from a "Trim" in which max=min?
 *
 * Therefore, I am not going to enforce the restriction.
 *

                // The EOWCS-2.0 spec says no Slice subsetting because it breaks the metadata description of the object.
                if (ds.isSliceSubset()) {
                    throw new WcsException("A 'low' and 'high' values are required ex: " + ds.getDimensionId() + "(-10,10)",
                            WcsException.INVALID_PARAMETER_VALUE, "subset");
                }
*/

                // Disallow array subsets for this type of request because the entire point is
                // to determine geospatial connection based on coordinate values.

                if(ds.isArraySubset()){
                    String msg = "The submitted subset for dimension '" + ds.getDimensionId() + "' "+
                            "invokes an array index based subset because it submits integer values for " +
                            "the subset bounds. Only value based subsetting is allowed for a " +
                            "DescribeEOCoverageSet request Use floating point values to specify a " +
                            "value based subset. Thanks.";
                    throw new WcsException(msg,WcsException.INVALID_PARAMETER_VALUE,"wcs:DimensionSubset") ;
                }

                if(ds.getDimensionId().toLowerCase().contains("time")){
                    _temporalSubset = new TemporalDimensionSubset(ds);
                }
                else {
                    _dimensionSubsets.put(ds.getDimensionId(), ds);
                }


            }
        }
        _log.debug("DescribeEOCoverageSetRequest() - subsets has {} elements)", _dimensionSubsets.size());
    }

    public String[] getEoIds(){
        String ids[] = new String[_eoIds.size()];
        _eoIds.toArray(ids);
        return ids;
    }

    public int getMaxItemCount(){
        return _count;
    }




    public Containment getContainment(){
        return _containment;
    }


    public HashMap<String, DimensionSubset> getDimensionSubsets(){
        HashMap<String, DimensionSubset> subsets = new HashMap<>(_dimensionSubsets);
        return subsets;
    }

    public TemporalDimensionSubset getTemporalSubset(){
        return new TemporalDimensionSubset(_temporalSubset);
    }


}