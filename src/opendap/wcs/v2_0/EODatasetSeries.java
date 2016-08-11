package opendap.wcs.v2_0;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by ndp on 7/26/16.
 */
public class EODatasetSeries {

    private String myCoverageId;

    private Vector<EOCoverageDescription> members;

    String _catalogDir;

    private Logger log;


    //private long lastModified;

    //private File myFile;

    private boolean _validateContent = false;

    public static final String CONFIG_ELEMENT_NAME = "EODatasetSeries";


    //boolean initialized = false;


    public EODatasetSeries(Element eodsElement, String catalogDir, boolean validateContent) throws WcsException, JDOMException, ConfigurationException, IOException {

        log = LoggerFactory.getLogger(this.getClass());
        _validateContent = validateContent;

        members =  new Vector<>();

        _catalogDir = catalogDir;

        if(!eodsElement.getName().equals(CONFIG_ELEMENT_NAME))
            throw new WcsException("Bad configuration! Unexpected "+eodsElement.getName()+" element. Was expecting "+ CONFIG_ELEMENT_NAME,
                    WcsException.NO_APPLICABLE_CODE,"eowcs:DatasetSeries");


        String s;

        myCoverageId=null;
        s = eodsElement.getAttributeValue("id");
        if(s!=null)
            myCoverageId = s;

        Iterator cvrgList = eodsElement.getChildren(EOCoverageDescription.CONFIG_ELEMENT_NAME,LocalFileCatalog.NS).iterator();

        while(cvrgList.hasNext()){
            Element  eoWcsCoverageElement = (Element) cvrgList.next();
            members.add(new EOCoverageDescription(eoWcsCoverageElement, _catalogDir,_validateContent));
        }
    }






    /**
     *
     * @return Returns the value of the unique wcs:CoverageId associated with this CoverageDescription.
     */
    public String getCoverageId(){
        return myCoverageId;
    }


    public Element getDatasetSeriesSummary() throws WcsException {

        Element datasetSeriesSummary = new Element("DatasetSeriesSummary",WCS.WCSEO_NS);

        //         <wcseo:DatasetSeriesId>someDatasetSeries1</wcseo:DatasetSeriesId>
        Element dsId = new Element("DatasetSeriesId",WCS.WCSEO_NS);
        dsId.setText(getCoverageId());

        datasetSeriesSummary.addContent(dsId);

        BoundingBox seriesBoundingBox = null;

        try {
            for (EOCoverageDescription cd : members) {
                BoundingBox bb = cd.getBoundingBox();
                if (seriesBoundingBox == null) {
                    seriesBoundingBox = new BoundingBox(bb);
                } else {
                    seriesBoundingBox = seriesBoundingBox.union(bb);
                }

            }
        }
        catch (URISyntaxException e) {
            log.error("Failed to get new BoundingBox from copy constructor. Caught URISyntaxException. Message: {}",e.getMessage());
        }

        if(seriesBoundingBox!=null){
            Element wgs84BB = seriesBoundingBox.getWgs84BoundingBoxElement();
            datasetSeriesSummary.addContent(wgs84BB);
            if(seriesBoundingBox.hasTimePeriod()){
                Element timePeriod = seriesBoundingBox.getGmlTimePeriod(getCoverageId()+"_timePeriod");
                if(timePeriod!=null)
                    datasetSeriesSummary.addContent(timePeriod);
            }
        }

        return datasetSeriesSummary;
    }





}
