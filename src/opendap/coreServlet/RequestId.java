package opendap.coreServlet;

import java.util.UUID;

/**
 * Provides utility methods that perform "analysis" of the user request and return important componet strings
 * for the OPeNDAP servlet.
 * <p>
 * The dataSourceName is the local URL path of the request, minus any requestSuffixRegex detected. So, if the request is
 * for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset minus the
 * requestSuffixRegex. If the request is for a collection, then the dataSourceName is the complete local path.
 * <p><b>Examples:</b>
 * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds?lat,lon,time&lat>72.0<br/>
 * Then the:</li>
 * <ul>
 * <li> RequestURL = http://opendap.org:8080/opendap/nc/fnoc1.nc </li>
 * <li> CollectionName = /opendap/nc/ </li>
 * <li> DataSetName = fnoc1.nc </li>
 * <li> DataSourceName = /opendap/nc/fnoc1.nc </li>
 * <li> RequestSuffix = dds </li>
 * <li> ConstraintExpression = lat,lon,time&lat>72.0 </li>
 * </ul>
 *
 * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
 * Then the:</li>
 * <ul>
 * <li> RequestURL = http://opendap.org:8080/opendap/nc/ </li>
 * <li> CollectionName = /opendap/nc/ </li>
 * <li> DataSetName = null </li>
 * <li> DataSourceName = /opendap/nc/ </li>
 * <li> RequestSuffix = "" </li>
 * <li> ConstraintExpression = "" </li>
 * </ul>
 * </ul>
 *
 * @author Nathan Potter
 */

public class RequestId {
    private String id;
    private UUID uuid;

    public RequestId() {
        id = "";
        uuid = UUID.randomUUID();
    }
    public RequestId(String id) {
        this();
        this.id = id;
    }
    public void id(String id){ this.id = id;}

    public String id() {return id;}
    public UUID uuid() {return uuid;}

    public String getCombined(){ return id + "-" + uuid.toString(); }
}
