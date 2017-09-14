package opendap.wcs.srs;

import java.util.concurrent.ConcurrentHashMap;

public class SrsFactory {
    private static ConcurrentHashMap<String, SimpleSrs> _theList;
    static {
        _theList = new ConcurrentHashMap<>();
        Epsg4326 epsg4326 =  new Epsg4326();
        _theList.put(epsg4326.getName(),epsg4326);
    }

    public static SimpleSrs getSrs(String urn){
        SimpleSrs srs = null;
        if(urn!=null)
            srs =  _theList.get(urn);
        return srs;
    }
}
