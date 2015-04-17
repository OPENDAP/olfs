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

    protected Element _sourceDataset;


    public Dataset(Element dataset) throws BadConfigurationException {

        if(dataset==null)
            throw new BadConfigurationException("Dataset() Passed element may not be null in constructor.");

        _name = dataset.getAttributeValue("name");
        if(_name  ==null)
            throw new BadConfigurationException("Dataset() - The 'dataset' element must have a 'name' attribute.");

        _sourceDataset = dataset;


    }


    public Vector<Element> getAccess(){
        List access = _sourceDataset.getChildren(THREDDS.ACCESS, THREDDS.NS);
        Vector<Element> accessElements = new Vector<>();
        for(Object a : access)
            accessElements.add((Element) ((Element)a).clone());
        return accessElements;
    }

    public Vector<Element> getMetadata(){
        List meta = _sourceDataset.getChildren(THREDDS.METADATA, THREDDS.NS);
        Vector<Element> metaElements = new Vector<>();
        for(Object metadata : meta)
            metaElements.add((Element) ((Element)metadata).clone());

        return metaElements;
    }






    public String getName(){
        return _name;
    }

    public String getAlias(){
        return  _sourceDataset.getAttributeValue("alias");
    }


    public String getAuthority(){
        return  _sourceDataset.getAttributeValue("authority");
    }


    public String getCollectionType(){
        return  _sourceDataset.getAttributeValue("collectionType");
    }


    public String getDataType(){
        return  _sourceDataset.getAttributeValue("dataType");
    }


    public String getHarvest(){
        return  _sourceDataset.getAttributeValue("harvest");
    }


    public String getID(){
        return  _sourceDataset.getAttributeValue("ID");
    }


    public String getRestrictAccess(){
        return  _sourceDataset.getAttributeValue("restrictAccess");
    }


    public String getServiceName(){
        return  _sourceDataset.getAttributeValue("serviceName");
    }

    public String getUrlPath(){
        return  _sourceDataset.getAttributeValue("urlPath");
    }




}
