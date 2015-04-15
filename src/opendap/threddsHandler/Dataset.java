package opendap.threddsHandler;

import opendap.bes.BadConfigurationException;
import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.List;

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


    public Element[] getAccess(){
        List<Element> access = _sourceDataset.getChildren("access", THREDDS.NS);
        Element accessElements[] = new Element[access.size()];
        return access.toArray(accessElements);
    }

    public Element[] getMetadata(){
        List<Element> meta = _sourceDataset.getChildren("metadata", THREDDS.NS);
        Element metaElements[] = new Element[meta.size()];
        return meta.toArray(metaElements);
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
