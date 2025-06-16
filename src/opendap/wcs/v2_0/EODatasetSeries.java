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

import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * Created by ndp on 7/26/16.
 */
@XmlRootElement(name = "EODatasetSeries")
public class EODatasetSeries {

    private String _id;

    private Vector<EOCoverageDescription> members;

    String _catalogDir;

    private Logger log;


    //private long lastModified;

    //private File myFile;

    private boolean _validateContent = false;

    public static final String CONFIG_ELEMENT_NAME = "EODatasetSeries";


    //boolean _initialized = false;

    public EODatasetSeries(){}

    public EODatasetSeries(Element eodsElement, String catalogDir, boolean validateContent)
            throws WcsException, JDOMException, ConfigurationException, IOException {

        log = LoggerFactory.getLogger(this.getClass());
        _validateContent = validateContent;

        members =  new Vector<>();

        _catalogDir = catalogDir;

        if(!eodsElement.getName().equals(CONFIG_ELEMENT_NAME))
            throw new WcsException("Bad configuration! Unexpected "+eodsElement.getName()+
                    " element. Was expecting "+ CONFIG_ELEMENT_NAME,
                    WcsException.NO_APPLICABLE_CODE,"eowcs:DatasetSeries");


        String s;

        _id =null;
        s = eodsElement.getAttributeValue("id");
        if(s!=null)
            _id = s;

        Iterator cvrgList = eodsElement.getChildren(EOCoverageDescription.CONFIG_ELEMENT_NAME,LocalFileCatalog.NS).iterator();

        while(cvrgList.hasNext()){
            Element  eoWcsCoverageElement = (Element) cvrgList.next();
            members.add(new EOCoverageDescription(eoWcsCoverageElement, _catalogDir,_validateContent));
        }
    }


    Vector<EOCoverageDescription> getMembers(){
        return new Vector<>(members);
    }





    /**
     *
     * @return Returns the value of the unique wcs:CoverageId associated with this CoverageDescription.
     */
    @XmlAttribute
    public String getId(){
        return _id;
    }

    public void setId(String myId){
    	this._id = myId;
    }

    public NewBoundingBox getBoundingBox() throws WcsException {
        NewBoundingBox seriesBoundingBox = null;

        for (EOCoverageDescription cd : members) {
            NewBoundingBox bb = cd.getBoundingBox();
            if (seriesBoundingBox == null) {
                try {
                    seriesBoundingBox = new NewBoundingBox(bb);
                }
                catch (URISyntaxException e) {
                    log.error("Failed to get new BoundingBox from copy constructor. Caught URISyntaxException. Message: {}",e.getMessage());
                }

            } else {
                seriesBoundingBox = seriesBoundingBox.union(bb);
            }

        }
        return seriesBoundingBox;

    }


    public Element getDatasetSeriesSummaryElement() throws WcsException {
        Element datasetSeriesSummary = new Element("DatasetSeriesSummary",WCS.WCSEO_NS);
        addDatasetSeriesContent(datasetSeriesSummary);
        return datasetSeriesSummary;

    }

    public void addDatasetSeriesContent(Element e) throws WcsException {


        //         <wcseo:DatasetSeriesId>someDatasetSeries1</wcseo:DatasetSeriesId>
        Element dsId = new Element("DatasetSeriesId",WCS.WCSEO_NS);
        dsId.setText(getId());

        e.addContent(dsId);

        NewBoundingBox seriesBoundingBox = getBoundingBox();

        if(seriesBoundingBox!=null){
            Element wgs84BB = seriesBoundingBox.getWgs84BoundingBoxElement();
            e.addContent(wgs84BB);
            if(seriesBoundingBox.hasTimePeriod()){
                Element timePeriod = seriesBoundingBox.getGmlTimePeriod(getId()+"_timePeriod");
                if(timePeriod!=null)
                    e.addContent(timePeriod);
            }
        }
    }

    public Element getDatasetSeriesDescriptionElement() throws WcsException {

        Element datasetSeriesDescription = new Element("DatasetSeriesDescription",WCS.WCSEO_NS);


        addDatasetSeriesContent(datasetSeriesDescription);


        return datasetSeriesDescription;

    }

    
    @XmlElement(name = "EOWcsCoverage")
    public List<EOCoverageDescription> getEoCoverageDescriptionElements() {
    	if (members == null || members.isEmpty()) return Collections.<EOCoverageDescription>emptyList();
    	return Collections.list(members.elements());
    }
  
    public void setEoCoverageDescriptionElements(Vector<EOCoverageDescription> ecovs)
    {
      this.members = ecovs;	
    }

}
