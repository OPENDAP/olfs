package opendap.wcs.v2_0;

import opendap.wcs.srs.SimpleSrs;
import opendap.wcs.srs.SrsFactory;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicService {
    private Logger _log;
    private String _name;
    private URL _dapServiceUrl;
    private SimpleSrs _srs;
    private Vector<DomainCoordinate> _domainCoordinates;
    private DomainCoordinate _time;
    private DomainCoordinate _latitude;
    private DomainCoordinate _longitude;
    private ConcurrentHashMap<String,String> _wcsFieldsToDapVar;

    public DynamicService(){
        super();
        _log = LoggerFactory.getLogger(this.getClass());
        _name = null;
        _dapServiceUrl = null;
        _time = null;
        _latitude = null;
        _longitude = null;
        _srs = null;
        _domainCoordinates =  new Vector<>();
        _wcsFieldsToDapVar = new ConcurrentHashMap<>();
    }

    public DynamicService(Element config) throws ConfigurationException {
        this();

        String s;
        _name = config.getAttributeValue("name");
        if(_name==null)
            throw new ConfigurationException("Failed to locate required attribute 'name' in the DynamicService configuration element!");

        s = config.getAttributeValue("href");
        if(s==null)
            throw new ConfigurationException("Failed to locate required attribute 'href' in the DynamicService configuration element!");
        try {
            _dapServiceUrl = new URL(s);
        }
        catch(MalformedURLException mue){
            throw new ConfigurationException("Failed to build URL from string '"+s+"' msg: "+ mue.getMessage());
        }

        
        s = config.getAttributeValue("srs");
        if(s==null)
            throw new ConfigurationException("DynamicService element is missing required 'srs' attribute whose value should be the URN of the SRS desired, for example 'urn:ogc:def:crs:EPSG::4326'");

        _srs = SrsFactory.getSrs(s);
        if(_srs==null)
            throw new ConfigurationException("Failed to locate requested SRS '" + s + "' Unable to configure Dynamic service!");
        _log.info("WCS-2.0 DynamicService {} has default SRS of {}",_name, _srs.getName());

        List<Element> domainCoordinateElements  =  (List<Element>)config.getChildren("DomainCoordinate");
        if(domainCoordinateElements.size()<2)
            _log.warn("The DynamicService '"+_name+"' has "+domainCoordinateElements.size()+" DomainCoordinate elements . This is probably going to break something.");
        for(Element dcElement: domainCoordinateElements){
            DomainCoordinate dc = new DomainCoordinate(dcElement);
            _domainCoordinates.add(dc);
            String role = dc.getRole();
            if(role!=null){
                if(role.equalsIgnoreCase("latitude"))
                    _latitude = dc;
                else if(role.equalsIgnoreCase("longitude"))
                    _longitude = dc;
                else if(role.equalsIgnoreCase("time"))
                    _time = dc;
            }
        }

        List<Element> variableMappings = (List<Element>) config.getChildren("field");
        if(variableMappings.isEmpty()){
            _log.warn("The configuration fails to associate wcs:Field elements with DAP variables.");
        }
        for(Element fieldElement : variableMappings){
            String fieldName = fieldElement.getAttributeValue("name");
            String dapId = fieldElement.getAttributeValue("dapID");
            if(fieldName==null || dapId==null){
                throw new ConfigurationException("Each field element must have both a 'name' and a 'dapID' attribute.");
            }
            _wcsFieldsToDapVar.put(fieldName,dapId);
        }
    }

    private void orderPreservingCoordinateReplace(DomainCoordinate newCoordinate, DomainCoordinate oldCoordinate){

        if(oldCoordinate==null){
            _domainCoordinates.add(newCoordinate);
            return;
        }

        if(_domainCoordinates.contains(oldCoordinate)){
            int index = _domainCoordinates.indexOf(oldCoordinate);
            if(index>=0){
                _domainCoordinates.insertElementAt(newCoordinate,index);
            }
            else {
                _domainCoordinates.add(newCoordinate);
            }
            _domainCoordinates.remove(oldCoordinate);
        }
        else{
            _domainCoordinates.add(newCoordinate);
        }

    }

    public void setTimeCoordinate(DomainCoordinate time){
        orderPreservingCoordinateReplace(time,_time);
        _time = time;
    }
    public DomainCoordinate getTimeCoordinate(){ return _time; }

    public void setLatitudeCoordinate(DomainCoordinate latitude){
        orderPreservingCoordinateReplace(latitude,_latitude);
        _latitude = latitude;
    }
    public DomainCoordinate getLatitudeCoordinate(){ return _latitude; }

    public void setLongitudeCoordinate(DomainCoordinate longitude){
        orderPreservingCoordinateReplace(longitude,_longitude);
        _longitude = longitude;
    }
    public DomainCoordinate getLongitudeCoordinate(){ return _longitude; }


    public void setSrs(SimpleSrs srs){ _srs = srs; }

    public SimpleSrs getSrs() {
        return _srs;
    }

    /**
     * Returns an ordered Vector of the domain coordinates where the last meber is the inner most dimension
     * of the data
     * @return
     */
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

