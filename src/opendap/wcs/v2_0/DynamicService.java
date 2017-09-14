package opendap.wcs.v2_0;

import opendap.wcs.srs.SimpleSrs;
import opendap.wcs.srs.SrsFactory;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicService {
    Logger _log;
    private String _name;
    private URL _dapServiceUrl;
    private SimpleSrs _srs;
    private Vector<DomainCoordinate> _domainCoordinates;
    private DomainCoordinate _time;
    private DomainCoordinate _latitude;
    private DomainCoordinate _longitude;
    private ConcurrentHashMap<String,String> _wcsFieldsToDapVar;

    public DynamicService(){
        _log = LoggerFactory.getLogger(this.getClass());
        _name = null;
        _dapServiceUrl = null;
        _time = null;
        _latitude = null;
        _longitude = null;
        _domainCoordinates =  new Vector<>();
        _srs = null;
        _wcsFieldsToDapVar = new ConcurrentHashMap<>();
    }

    public DynamicService(Element config) throws BadParameterException{

        Element e;
        String s=null;

        _name = config.getAttributeValue("name");
        if(_name==null)
            throw new BadParameterException("Failed to locate required attribute 'name' in the DynamicService configuration element!");

        s = config.getAttributeValue("href");
        if(s==null)
            throw new BadParameterException("Failed to locate required attribute 'href' in the DynamicService configuration element!");
        try {
            _dapServiceUrl = new URL(s);
        }
        catch(MalformedURLException mue){
            throw new BadParameterException("Failed to build URL from string '"+s+"' ");
        }



        e = config.getChild("srs");
        if(e==null)
            throw new BadParameterException("DynamicService element is missing required 'srs' child element whose value should be the URN of the SRS desired, for example 'urn:ogc:def:crs:EPSG::4326'");

        String srsName = config.getAttributeValue("srs");
        _srs = SrsFactory.getSrs(srsName);
        if(_srs!=null)
            throw new BadParameterException("Failed to locate requested SRS '" + srsName + "' Unable to configure Dynamic service!");
        _log.info("WCS-2.0 DynamicService {} has default SRS of {}",_name, _srs.getName());


        for(Object o : config.getChildren("field")){
            Element fieldElement =  (Element)o;
            String fieldName = fieldElement.getAttributeValue("name");
            String dapId = fieldElement.getAttributeValue("dapID");
            if(fieldName==null || dapId==null){
                throw new BadParameterException("Each field element must have both a 'name' and a 'dapID' attribute.");
            }
            _wcsFieldsToDapVar.put(fieldName,dapId);
        }








    }








    public void setTimeCoordinate(DomainCoordinate time){
        if(_time!=null){
            _domainCoordinates.remove(_time);
        }
        _time = time;
        _domainCoordinates.add(time);
    }

    public DomainCoordinate getTimeCoordinate(){ return _time; }

    public void setLatitudeCoordinate(DomainCoordinate latitude){
        if(_latitude!=null){
            _domainCoordinates.remove(_latitude);
        }
        _latitude = latitude;
        _domainCoordinates.add(latitude);
    }
    public DomainCoordinate getLatitudeCoordinate(){
        return _latitude; }

    public void setLongitudeCoordinate(DomainCoordinate longitude){
        if(_longitude!=null){
            _domainCoordinates.remove(_longitude);
        }
        _longitude = longitude;
        _domainCoordinates.add(longitude);
    }

    public void setSrs(SimpleSrs srs){ _srs = srs; }

    public SimpleSrs getSrs() {
        return _srs;
    }

    public Vector<DomainCoordinate> getDomainCoordinates(){
        return _domainCoordinates;
    }

    public void setName(String name) { _name = name; }
    public String getName() { return  _name; }

    public void setDapServiceUrl(String dapServiceUrl) throws MalformedURLException {
        _dapServiceUrl = new URL(dapServiceUrl);
    }
    public URL getDapServiceUrl(){ return _dapServiceUrl; }


    @Override
    public String toString(){
        /*
        Logger _log;
        private String _name;
        private URL _dapServiceUrl;
        private SimpleSrs _srs;
        private Vector<DomainCoordinate> _domainCoordinates;
        private DomainCoordinate _time;
        private DomainCoordinate _latitude;
        private DomainCoordinate _longitude;
        */
        StringBuilder sb = new StringBuilder();
        sb.append("DynamicService {\n)");
        sb.append("  name: ").append(_name).append("\n");
        sb.append("  dapServiceUrl: ").append(_dapServiceUrl).append("\n");
        sb.append("  srs: ").append(_srs.getName()).append("\n");
        sb.append("  time: ").append(_time).append("\n");
        sb.append("  latitude: ").append(_latitude).append("\n");
        sb.append("  longitude: ").append(_longitude).append("\n");
        sb.append("  Variable Mappings: ").append(_longitude).append("\n");
        for(String wcsField: _wcsFieldsToDapVar.keySet() )
            sb.append("    ").append(wcsField).append("<-->").append(_wcsFieldsToDapVar.get(wcsField)).append("\n");

        sb.append("  Coordinate Order: ");
        for(DomainCoordinate dc : _domainCoordinates)
            sb.append(dc.getName()).append(" ");
        sb.append("\n");

        return sb.toString();

    }
}

