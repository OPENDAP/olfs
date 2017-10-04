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

import opendap.http.Util;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DynamicService {
    private Logger _log;
    private String _prefix;
    private String _longName;
    private String _dapServiceUrlString;
    private SimpleSrs _srs;
    private Vector<DomainCoordinate> _domainCoordinates;
    // private ConcurrentHashMap<String, DomainCoordinate> _dcMap;
    private ConcurrentHashMap<String,FieldDef> _wcsFieldsByDapID;
    private String _pathMatchRegexString;


    static public class FieldDef {
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
        _prefix = null;
        _dapServiceUrlString = null;
        _srs = null;
        _domainCoordinates =  new Vector<>();
        //_dcMap = new ConcurrentHashMap<>();
        _wcsFieldsByDapID = new ConcurrentHashMap<>();
        _pathMatchRegexString = null;
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
        _prefix = config.getAttributeValue("prefix");
        if(_prefix ==null)
            badThingsHappened.add("Failed to locate required attribute 'name' in the DynamicService configuration element!");

        _longName = config.getAttributeValue("name");
        if(_longName==null)
            _longName = _prefix;

        s = config.getAttributeValue("pathMatch");
        if(s!=null){
            if(s.isEmpty()){
                badThingsHappened.add("In the DynamicService element, the 'pathMatch' attribute's " +
                        " value must be a valid regular expression. The empty string is not a valid regular expression," +
                        "at least not in this context. Please fix the empty 'pathMatch' and try again... ");
            }
            else {
                try {
                    Pattern p  = Pattern.compile(s);
                    _log.debug("Compiled pathMatch pattern: {}",p.pattern());
                    _pathMatchRegexString = p.pattern();
                } catch (PatternSyntaxException pse) {
                    badThingsHappened.add("Failed to compile regular expression pattern: '" +
                            s + "  message: " + pse.getMessage());
                }
                _dapServiceUrlString = Util.BES_PROTOCOL;
            }
        }

        s = config.getAttributeValue("href");
        if(s!=null){
            if(_pathMatchRegexString!=null) {
                badThingsHappened.add("A DynamicService element may have either a 'pathMatch' attritbute or an 'href' " +
                    "attribute but not BOTH. Decide, fix, try again...");
            }
            else {

                // The java.net.URL class supports: http, https, ftp, file, and jar
                // We want two things here:  http and https
                if (s.toLowerCase().startsWith(Util.HTTP_PROTOCOL) ||
                        s.toLowerCase().startsWith(Util.HTTPS_PROTOCOL)) {
                    try {
                        _dapServiceUrlString = new URL(s).toString();
                    } catch (MalformedURLException mue) {
                        badThingsHappened.add("Failed to build URL from string '" + s + "' msg: " + mue.getMessage());
                    }
                }
                else {
                    badThingsHappened.add("The DynamicService 'href' attribute references an unsupported protocol." +
                            "Only 'http' and 'https' are supported.");
                }
            }
        }
        else if(_pathMatchRegexString==null){
            badThingsHappened.add("OUCH! The DynamicService element must have either " +
                    "an 'href' attribute or a 'pathMatch' attribute to be correct!");
        }


        s = config.getAttributeValue("srs");
        if(s==null) {
            badThingsHappened.add("DynamicService element is missing required 'srs' attribute whose value should be the URN of the SRS desired, for example 'urn:ogc:def:crs:EPSG::4326'");
        }
        else {
            _srs = SrsFactory.getSrs(s);
            if (_srs == null) {
                badThingsHappened.add("Failed to locate requested SRS '" + s + "' Unable to configure Dynamic service!");
            } else {
                _log.info("WCS-2.0 DynamicService {} has default SRS of {}", _prefix, _srs.getName());
            }
        }

        List<Element> domainCoordinateElements  =  (List<Element>)config.getChildren("DomainCoordinate");
        if(domainCoordinateElements.size()<2) {
            _log.warn("The DynamicService '" + _prefix + "' has " +
                    domainCoordinateElements.size() + " DomainCoordinate elements . " +
                    "This is probably going to break something.");
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
                    "definition '"+ getPrefix()+"' has only "+domainCoordinates.size());

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
                    badThingsHappened.add("Failed to parse the value of the 'min' attribute as a double. min: "+s
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
        //_dcMap.put(dc.getPrefix(),dc);
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

    public void setPrefix(String prefix) { _prefix = prefix; }
    public String getPrefix() { return _prefix; }

    //public void setDapServiceUrl(String dapServiceUrl) throws MalformedURLException {
    //    _dapServiceUrlString = new URL(dapServiceUrl);
    //}
    public String getDapServiceUrlString(){ return _dapServiceUrlString; }


    @Override
    public String toString(){
        /*
    private Logger _log;
    private String _prefix;
    private String _longName;
    private String _dapServiceUrlString;
    private SimpleSrs _srs;
    private Vector<DomainCoordinate> _domainCoordinates;
    private ConcurrentHashMap<String,FieldDef> _wcsFieldsByDapID;
    private String _pathMatchRegexString;
        */
        StringBuilder sb = new StringBuilder();
        sb.append("DynamicService {\n)");
        sb.append("  prefix: ").append(_prefix).append("\n");
        sb.append("  name: ").append(_longName).append("\n");
        sb.append("  dapServiceUrl: ").append(_dapServiceUrlString).append("\n");
        sb.append("  pathMatchRegexString: ").append(_pathMatchRegexString).append("\n");
        sb.append("  srs: ").append(_srs.getName()).append("\n");
        sb.append("  Variable Mappings:\n");
        for(FieldDef field: _wcsFieldsByDapID.values()) {
            sb.append("  ").append(field.name).append("<-->").append(field.dapID).append("\n");
            sb.append("     description: "+field.description).append("\n");
            sb.append("     units: "+field.units).append("\n");
            sb.append("     vmin: "+field.min).append("\n");
            sb.append("     vmax: "+field.max).append("\n");
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


    public static void main(String args[]) {

        String urlStrings[] =
                {
                        "http://wcs.opendap.org:8080/opendap",
                        "file://etc/olfs/",
                        "bes://data/nc/",
                        "wcs://wcs.opendap.org:8080/WCS-2.0/"
                };
        URL url;
        for (String urlString : urlStrings) {
            try {
                url = new URL(urlString);
                System.out.println("urlString: " + urlString + "  URL: " + url.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



    }

    public String getLongName() { return _longName; }

    public String getPathMatchRegexString(){
        return _pathMatchRegexString;
    }

}

