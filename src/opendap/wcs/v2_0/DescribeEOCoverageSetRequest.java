package opendap.wcs.v2_0;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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

    public enum Containment {
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


    /**
     * count = max_number_of_CoverageDescriptions to return
     */
    private int _count;

    /**
     * sections = CoverageDescriptions | DatasetSeriesDescriptions | All
     */
    Vector<Sections> _sections;

    public enum Sections {
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

    boolean hasSection(Sections s){
        return _sections.contains(s);
    }

    /**
     * subset = long | lat | phenomenonTime (low,high)
     * <p>
     * SubsetSpec:  dimension ( interval )
     * dimension:   long | lat | phenomenonTime
     * interval:    low, high
     * low:          point | *
     * high:         point | *
     * point:        number| " token " // " = ASCII 0x42
     */
    HashMap<Subsets, DimensionSubset> _subsets;

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


        _log = LoggerFactory.getLogger(this.getClass());

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

        _eoIds = null;
        vals = kvp.get("eoid");
        if (vals == null) {
            throw new WcsException("The 'eoId' query parameter is required for a  " + KVP_REQUEST + " request.", WcsException.MISSING_PARAMETER_VALUE, "eoId");
        }
        _eoIds = new Vector<>();
        String ids[] = vals[0].split(",");
        for (String id : ids) {
            _eoIds.add(id);
        }
        _log.debug("DescribeEOCoverageSetRequest() - Got {} eoId values.", _eoIds.size());


        _containment = Containment.OVERLAPS; // Default.
        vals = kvp.get("containment");
        if (vals != null) {
            if (vals[0].equalsIgnoreCase(Containment.OVERLAPS.toString())) {
                _containment = Containment.OVERLAPS;
            } else if (vals[0].equalsIgnoreCase(Containment.CONTAINS.toString())) {
                _containment = Containment.CONTAINS;
            }

        }
        _log.debug("DescribeEOCoverageSetRequest() - Containment set to: '{}'", _containment);

        _count = DEFAULT_COUNT;
        vals = kvp.get("count");
        if (vals != null) {
            try {
                _count = Integer.parseInt(vals[0]);
            } catch (NumberFormatException e) {
                throw new WcsException("The 'count' query parameter's value must be a string representation of an integer.", WcsException.INVALID_PARAMETER_VALUE, "count");
            }
        }
        _log.debug("DescribeEOCoverageSetRequest() - Count set to: {} (default: {})", _count, DEFAULT_COUNT);


        _sections = new Vector<>();
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


        _subsets = new HashMap<>();
        vals = kvp.get("subset");
        if (vals != null) {

            for (String subset : vals) {
                DimensionSubset ds = new DimensionSubset(subset);
                if (ds.isSliceSubset()) {
                    throw new WcsException("A 'low' and 'high' values are required ex: " + ds.getDimensionId() + "(-10,10)",
                            WcsException.INVALID_PARAMETER_VALUE, "subset");
                }

                if (ds.getDimensionId().equalsIgnoreCase(Subsets.LAT.toString())) {
                    _subsets.put(Subsets.LAT, ds);
                } else if (ds.getDimensionId().equalsIgnoreCase(Subsets.LONG.toString())) {
                    _subsets.put(Subsets.LONG, ds);
                } else if (ds.getDimensionId().equalsIgnoreCase(Subsets.PHENOMENON_TIME.toString())) {
                    _subsets.put(Subsets.PHENOMENON_TIME, ds);
                }
                else {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Use of the 'subset' parameter with a ").append(DescribeEOCoverageSetRequest.KVP_REQUEST);
                    msg.append(" is restricted to the coordinate dimensions ").append(Subsets.LONG);
                    msg.append(",").append(Subsets.LAT).append(", and ").append(Subsets.PHENOMENON_TIME);
                    throw new WcsException("msg",WcsException.INVALID_PARAMETER_VALUE, "subset");
                }

            }
        }
        _log.debug("DescribeEOCoverageSetRequest() - subsets has {} elements)", _subsets.size());
    }

    public String[] getEoIds(){
        String ids[] = new String[_eoIds.size()];
        _eoIds.toArray(ids);
        return ids;
    }

    public int getMaxItemCount(){
        return _count;
    }

    public BoundingBox getSubsetBoundingBox(){

        //BoundingBox bb = new BoundingBox();

        return null;

    }

    public Containment getContainment(){
        return _containment;
    }



}