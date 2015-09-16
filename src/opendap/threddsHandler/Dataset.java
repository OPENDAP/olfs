/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.threddsHandler;

import opendap.bes.BadConfigurationException;
import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.List;
import java.util.Vector;

/**
 * Created by ndp on 4/14/15.
 */
public class Dataset {


    private String  _name;

    protected Element _sourceDatasetElement;


    public Dataset(Element dataset) throws BadConfigurationException {

        if(dataset==null)
            throw new BadConfigurationException("Dataset() Passed element may not be null in constructor.");

        _name = dataset.getAttributeValue("name");
        if(_name  ==null)
            throw new BadConfigurationException("Dataset() - The 'dataset' element must have a 'name' attribute.");

        _sourceDatasetElement = dataset;


    }


    public Vector<Element> getAccess(){
        List access = _sourceDatasetElement.getChildren(THREDDS.ACCESS, THREDDS.NS);
        Vector<Element> accessElements = new Vector<>();
        for(Object a : access)
            accessElements.add((Element) ((Element)a).clone());
        return accessElements;
    }

    public Vector<Element> getMetadata(){
        List meta = _sourceDatasetElement.getChildren(THREDDS.METADATA, THREDDS.NS);
        Vector<Element> metaElements = new Vector<>();
        for(Object metadata : meta)
            metaElements.add((Element) ((Element)metadata).clone());

        return metaElements;
    }






    public String getName(){
        return _name;
    }

    public String getAlias(){
        return  _sourceDatasetElement.getAttributeValue("alias");
    }


    public String getAuthority(){
        return  _sourceDatasetElement.getAttributeValue("authority");
    }


    public String getCollectionType(){
        return  _sourceDatasetElement.getAttributeValue("collectionType");
    }


    public String getDataType(){
        return  _sourceDatasetElement.getAttributeValue("dataType");
    }


    public String getHarvest(){
        return  _sourceDatasetElement.getAttributeValue("harvest");
    }


    public String getID(){
        return  _sourceDatasetElement.getAttributeValue("ID");
    }


    public String getRestrictAccess(){
        return  _sourceDatasetElement.getAttributeValue("restrictAccess");
    }


    public String getServiceName(){
        return  _sourceDatasetElement.getAttributeValue("serviceName");
    }

    public String getUrlPath(){
        return  _sourceDatasetElement.getAttributeValue("urlPath");
    }




}
