package opendap.threddsHandler;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.BadConfigurationException;
import opendap.namespaces.THREDDS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

/**
 * Created by ndp on 4/13/15.
 */
public class DatasetScan  extends Dataset implements Catalog{



    private Element _addProxies;



    private Element _addTimeCoverage;


    public DatasetScan(Element datasetScan)throws BadConfigurationException {
        super(datasetScan);

    }

    public String getPath(){
        return _sourceDataset.getAttributeValue("path");
    }

    public String getLocation(){
        return _sourceDataset.getAttributeValue("location");
    }

    public Element getNamer(){
        return _sourceDataset.getChild("namer", THREDDS.NS);
    }


    public Element getFilter(){
        return _sourceDataset.getChild("filter", THREDDS.NS);
    }

    public Element getSort(){
        return _sourceDataset.getChild("sort", THREDDS.NS);
    }

    public Element getAddProxies(){
        return _sourceDataset.getChild("addProxies", THREDDS.NS);
    }

    public Element getAddTimeCoverage(){
        return _sourceDataset.getChild("addTimeCoverage", THREDDS.NS);
    }




    @Override
    public void destroy() {

    }

    @Override
    public boolean usesMemoryCache() {
        return false;
    }

    @Override
    public boolean needsRefresh() {
        return false;
    }

    @Override
    public void writeCatalogXML(OutputStream os) throws Exception {

    }

    @Override
    public void writeRawCatalogXML(OutputStream os) throws Exception {

    }

    @Override
    public Document getCatalogDocument() throws IOException, JDOMException, SaxonApiException {
        return null;
    }

    @Override
    public Document getRawCatalogDocument() throws IOException, JDOMException, SaxonApiException {
        return null;
    }

    @Override
    public XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {
        return null;
    }

    @Override
    public XdmNode getRawCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getCatalogKey() {
        return null;
    }

    @Override
    public String getPathPrefix() {
        return null;
    }

    @Override
    public String getUrlPrefix() {
        return null;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getIngestTransformFilename() {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }
}
