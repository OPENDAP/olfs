package opendap.wcs.v2_0;

import opendap.dap4.Dim;
import opendap.wcs.srs.SimpleSrs;
import opendap.wcs.srs.SrsFactory;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicService {
    private Logger _log;
    private String _name;
    private URL _dapServiceUrl;
    private SimpleSrs _srs;
    private Vector<DomainCoordinate> _domainCoordinates;
    private ConcurrentHashMap<String, DomainCoordinate> _dcMap;
    private ConcurrentHashMap<String,FieldDef> _wcsFieldsByDapID;


    public class FieldDef {
        String name;
        String dapID;
        String description;
        String units;
        double min;
        double max;
    }

    public DynamicService(){
        super();
        _log = LoggerFactory.getLogger(this.getClass());
        _name = null;
        _dapServiceUrl = null;
        _srs = null;
        _domainCoordinates =  new Vector<>();
        _dcMap = new ConcurrentHashMap<>();
        _wcsFieldsByDapID = new ConcurrentHashMap<>();
    }

    /**
     * In which we examine the passed element and set state, or throw an Exception is the config is crummy.
     * @param config
     * @throws ConfigurationException
     */
    public DynamicService(Element config) throws ConfigurationException {
        this();

        Vector<String> badThingsHappened = new Vector<>();

        String s;
        _name = config.getAttributeValue("name");
        if(_name==null)
            badThingsHappened.add("Failed to locate required attribute 'name' in the DynamicService configuration element!");

        s = config.getAttributeValue("href");
        if(s==null) {
            badThingsHappened.add("Failed to locate required attribute 'href' in the DynamicService configuration element!");
        }
        else {
            try {
                _dapServiceUrl = new URL(s);
            }
            catch(MalformedURLException mue){
                badThingsHappened.add("Failed to build URL from string '"+s+"' msg: "+ mue.getMessage());
            }
        }

        s = config.getAttributeValue("srs");
        if(s==null)
            badThingsHappened.add("DynamicService element is missing required 'srs' attribute whose value should be the URN of the SRS desired, for example 'urn:ogc:def:crs:EPSG::4326'");

        _srs = SrsFactory.getSrs(s);
        if(_srs==null) {
            badThingsHappened.add("Failed to locate requested SRS '" + s + "' Unable to configure Dynamic service!");
        }
        else {
            _log.info("WCS-2.0 DynamicService {} has default SRS of {}", _name, _srs.getName());
        }

        List<Element> domainCoordinateElements  =  (List<Element>)config.getChildren("DomainCoordinate");
        if(domainCoordinateElements.size()<2) {
            _log.warn("The DynamicService '" + _name + "' has " + domainCoordinateElements.size() + " DomainCoordinate elements . This is probably going to break something.");
        }

        for(Element dcElement: domainCoordinateElements){
            DomainCoordinate dc = new DomainCoordinate(dcElement);
            addDomainCoordinate(dc);
        }
        // Now we QC the DomainCoordinates and the SRS to make sure they are compatible.
        // And since there may be more DomainCoordinates defined than there SRS dimension
        // We require reverse iterators.
        Vector<DomainCoordinate> domainCoordinates = getDomainCoordinates();
        ListIterator<DomainCoordinate> domainCoordRevIter = domainCoordinates.listIterator(domainCoordinates.size());

        List<String> srsAxisLabels = _srs.getAxisLabelsList();
        ListIterator<String> axisLabelRevIter = srsAxisLabels.listIterator(srsAxisLabels.size());

        if(domainCoordinates.size() < srsAxisLabels.size())
            badThingsHappened.add("OUCH! There must be at least as many DomainCoordinates as t" +
                    "he SRS has dimensions. srs has "+srsAxisLabels.size()+" and the DynamicService " +
                    "definition '"+getName()+"' has only "+domainCoordinates.size());

        while(axisLabelRevIter.hasPrevious() && domainCoordRevIter.hasPrevious()){
            String axisLabel = axisLabelRevIter.previous();
            DomainCoordinate domainCoord  =domainCoordRevIter.previous();

            if(!axisLabel.equalsIgnoreCase(domainCoord.getName())){
                StringBuilder troubles = new StringBuilder();
                troubles.append("The DynamicService must define DomainCoordinates for each axis in the SRS. ");
                troubles.append("They must appear in the DynamicService definition in the order they appear in the SRS, ");
                troubles.append("and their names must match as well.");
                troubles.append("We could not locate a DomainCoordinate in the correct position ");
                troubles.append("whose name matches the SRS axis ");
                troubles.append("label '").append(axisLabel).append("'\n ");
                badThingsHappened.add(troubles.toString());
            }
        }

        List<Element> fields = (List<Element>) config.getChildren("field");
        if(fields.isEmpty()){
            _log.warn("The configuration fails to associate wcs:Field elements with DAP variables.");
        }
        for(Element fieldElement : fields){
            boolean borked = false;
            int errInsertPoint = badThingsHappened.size();
            FieldDef field = new FieldDef();
            field.name = fieldElement.getAttributeValue("name");
            if(field.name==null){
                badThingsHappened.add("Each field element must have a 'name' attribute.");
                borked = true;
            }
            field.dapID = fieldElement.getAttributeValue("dapID");
            if(field.name==null){
                badThingsHappened.add("Each field element must have a 'dapID' attribute.");
                borked = true;
            }
            field.description = fieldElement.getAttributeValue("description");
            if(field.description ==null){
                badThingsHappened.add("Each field element must have a 'description' attribute.");
                borked = true;
            }
            field.units = fieldElement.getAttributeValue("units");
            if(field.units == null){
                badThingsHappened.add("Each field element must have a 'units' attribute.");
                borked = true;
            }

            s = fieldElement.getAttributeValue("min");
            if(s == null){
                badThingsHappened.add("Each field element must have a 'min' attribute.");
                borked = true;
            }
            else {
                try {
                    field.min = Double.parseDouble(s);
                } catch (NumberFormatException nue) {
                    badThingsHappened.add("Failed to parse the value of the 'vmin' attribute as a double. vmin: "+s
                            +" msg: "+nue.getMessage());
                    borked = true;
                }
            }


            s = fieldElement.getAttributeValue("max");
            if(s==null){
                badThingsHappened.add("Each field element must have a 'max' attribute.");
                borked = true;
            }
            else {
                try {
                    field.max = Double.parseDouble(s);
                } catch (NumberFormatException nue) {
                    badThingsHappened.add("Failed to parse the value of the 'max' attribute as a double. vmin: "+s
                            +" msg: "+nue.getMessage());
                    borked = true;
                }
            }
            if(borked){
                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                String badFieldHeader =  "Bad FieldDef: "+ xmlo.outputString(fieldElement);
                badThingsHappened.insertElementAt(badFieldHeader,errInsertPoint);
            }
            else {
                _wcsFieldsByDapID.put(field.dapID, field);
            }
        }
        if(!badThingsHappened.isEmpty()){
            StringBuilder sb = new StringBuilder();
            sb.append("Dynamic Configuration Failed To Ingest!\n");
            for(String msg: badThingsHappened){
                sb.append(msg).append("\n");
                _log.error("DynamicService() - {}",msg);
            }
            throw new ConfigurationException(sb.toString());
        }

    }

    public void addDomainCoordinate(DomainCoordinate dc){
        _domainCoordinates.add(dc);
        _dcMap.put(dc.getName(),dc);
    }



    /*
    public DomainCoordinate getDomainCoordinate(String coordinateName){
        return _dcMap.get(coordinateName);
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
    */


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
        sb.append("  Variable Mappings:\n");
        for(FieldDef field: _wcsFieldsByDapID.values()) {
            sb.append("  ").append(field.name).append("<-->").append(field.dapID).append("\n");
            sb.append("     description: "+field.description).append("\n");
            sb.append("     units: "+field.units).append("\n");
            sb.append("     vmin: "+field.min).append("\n");
            sb.append("     vmzx: "+field.max).append("\n");
            sb.append("\n");
        }

        sb.append("  Coordinate Order: ");
        for(DomainCoordinate dc : _domainCoordinates)
            sb.append(dc.getName()).append(" ");
        sb.append("\n");

        return sb.toString();

    }
    public FieldDef getFieldDefFromDapId(String dapId){
        return _wcsFieldsByDapID.get(dapId);
    }

}

