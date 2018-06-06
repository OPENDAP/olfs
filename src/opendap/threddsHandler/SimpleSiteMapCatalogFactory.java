package opendap.threddsHandler;

import opendap.PathBuilder;
import opendap.namespaces.THREDDS;
import opendap.namespaces.XLINK;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedHashMap;

public class SimpleSiteMapCatalogFactory {

    private  Logger _log;
    private String _siteMapFileName;
    File _siteMapFile;
    private LinkedHashMap<String, SiteMapNode> _catalogNodes;
    private SiteMapNode _rootNode;

    private String _hyraxServiceBase;



    SimpleSiteMapCatalogFactory(String siteMapFileName, String hyraxServiceBase) throws IOException {
        _log = LoggerFactory.getLogger(this.getClass());
        _siteMapFileName = siteMapFileName;
        _siteMapFile = new File(_siteMapFileName);
        if(!_siteMapFile.canRead()){
            throw new IOException("Unable to locate and read site map file: "+_siteMapFileName);
        }
        _hyraxServiceBase = hyraxServiceBase;
        _catalogNodes = new LinkedHashMap<>();
        _rootNode = new SiteMapNode();
    }


    class SiteMapNode extends SiteMapItem {
        boolean _isRootNode;
        LinkedHashMap<String, SiteMapItem> _children;
        String _indentIncrement = "  ";


        SiteMapNode() {
            super("/",null);
            _isRootNode = true;
            _children = new LinkedHashMap<>();
        }

        SiteMapNode(String name, SiteMapNode parentNode) {
            super(name,parentNode);
            _isRootNode = false;
            _children = new LinkedHashMap<>();
        }

        boolean isNode(){ return true;}

        String getNodeName(){
            StringBuilder sb = new StringBuilder(_name);
            SiteMapNode cNode = _parentNode;
            while(cNode!=null && !cNode._isRootNode){
                sb.insert(0,"/");
                sb.insert(0,cNode._name);
                cNode = cNode._parentNode;
            }
            return sb.toString();
        }

        public String dump(String indent ){
            StringBuilder sb = new StringBuilder(indent).append(getNodeName()).append("\n");
            for(SiteMapItem cn : _children.values()) {
                sb.append(cn.dump(indent+ _indentIncrement));
            }
            return sb.toString();
        }

        public Element getThreddsCatalog(){

            Element catalog = new Element(THREDDS.CATALOG, THREDDS.NS);
            catalog.addNamespaceDeclaration(THREDDS.NS);
            catalog.addNamespaceDeclaration(XLINK.NS);

            catalog.setAttribute("name","Catalog of "+ _name);


            Element service = new Element(THREDDS.SERVICE,THREDDS.NS);
            service.setAttribute("name","dap");
            service.setAttribute("type","OPeNDAP");
            service.setAttribute("base",_hyraxServiceBase);
            catalog.addContent(service);


            service = new Element(THREDDS.SERVICE,THREDDS.NS);
            service.setAttribute("name","file");
            service.setAttribute("type","HTTPServer");
            service.setAttribute("base",_hyraxServiceBase);
            catalog.addContent(service);

            Element topLevelDataset = new Element(THREDDS.DATASET,THREDDS.NS);
            topLevelDataset.setAttribute("name",getNodeName());
            topLevelDataset.setAttribute("ID",_hyraxServiceBase+ getNodeName());
            catalog.addContent(topLevelDataset);

            for(SiteMapItem smi : _children.values()) {
                if(smi.isNode())
                    topLevelDataset.addContent(getCatalogRef(smi));
                else
                    topLevelDataset.addContent(getDataset(smi, _hyraxServiceBase));
            }
            return catalog;
        }
    }


    class SiteMapItem {
        String _name;
        SiteMapNode _parentNode;

        long _size;
        String _lastModified;

        SiteMapItem(String name, SiteMapNode parentNode){
            _name = name;
            _parentNode = parentNode;
        }

        SiteMapItem(String name, SiteMapNode parentNode, long size, String lastModified){
            _name = name;
            _parentNode = parentNode;
            _size = size;
            _lastModified = lastModified;
        }

        boolean isNode(){ return false;}
        String getNodeName(){
            StringBuilder sb = new StringBuilder(_name);
            SiteMapNode cNode = _parentNode;
            while(!cNode._isRootNode){
                sb.insert(0,"/");
                sb.insert(0,cNode._name);
                cNode = cNode._parentNode;
            }
            return sb.toString();
        }
        String dump(String indent){
           return indent + getNodeName()+"\n";
        }

    }

    /**
     * <thredds:catalogRef name="agg" xlink:href="agg/catalog.xml" xlink:title="agg" xlink:type="simple" ID="/opendap/hyrax/data/ncml/agg/"/>
     */
    public static Element getCatalogRef(SiteMapItem smi){
        Element catalogRef = new Element(THREDDS.CATALOG_REF,THREDDS.NS);
        catalogRef.setAttribute("name",smi._name);
        catalogRef.setAttribute("href",smi._name+"/catalog.xml", XLINK.NS);
        catalogRef.setAttribute("title",smi._name, XLINK.NS);
        catalogRef.setAttribute("type","simple", XLINK.NS);
        catalogRef.setAttribute("ID",smi.getNodeName());
        return catalogRef;
    }

    /**
     * <thredds:dataset name="coads_climatology.ncml" ID="/opendap/hyrax/data/ncml/coads_climatology.ncml">
     *   <thredds:dataSize units="bytes">1048</thredds:dataSize>
     *   <thredds:date type="modified">2011-12-21T18:35:45</thredds:date>
     *   <thredds:access serviceName="dap" urlPath="/data/ncml/coads_climatology.ncml"/>
     *   <thredds:access serviceName="file" urlPath="/data/ncml/coads_climatology.ncml"/>
     * </thredds:dataset>
     * @param smi
     * @return
     */
    public static Element getDataset(SiteMapItem smi, String hyraxServicePrefix){
        Element dataset = new Element(THREDDS.DATASET,THREDDS.NS);
        dataset.setAttribute("name",smi._name);
        dataset.setAttribute("ID", PathBuilder.pathConcat(hyraxServicePrefix,smi.getNodeName()));

        Element dataSize = new Element(THREDDS.DATASIZE,THREDDS.NS);
        dataSize.setAttribute("units","bytes");
        dataSize.setText(""+smi._size);
        dataset.addContent(dataSize);

        Element date = new Element(THREDDS.DATE,THREDDS.NS);
        date.setAttribute("type","modified");
        date.setText(smi._lastModified);
        dataset.addContent(date);

        Element access = new Element(THREDDS.ACCESS,THREDDS.NS);
        access.setAttribute(THREDDS.SERVICE_NAME,"dap");
        access.setAttribute(THREDDS.URL_PATH,smi.getNodeName());
        dataset.addContent(access);

        access = new Element(THREDDS.ACCESS,THREDDS.NS);
        access.setAttribute(THREDDS.SERVICE_NAME,"file");
        access.setAttribute(THREDDS.URL_PATH,smi.getNodeName());
        dataset.addContent(access);



        return dataset;
    }





    public void ingestSiteMap() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(_siteMapFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                _log.debug("ingestSiteMap() Processing: {}",line);
                // process the line.
                process_line(line);
            }
        }

    }

    public  void writeCatalog(String outputDir) throws IOException {

        // Check to be sure we can write
        File topDir = new File(outputDir);

        if(topDir.exists()) {

            if (!topDir.canWrite()) {
                throw new IOException(this.getClass().getName() + ".writeCatalog() ERROR! Unable to write to target Catalog directory `" + outputDir + "'");
            }
        }
        else {
            if(!topDir.mkdirs()){
                throw new IOException(this.getClass().getName() + ".writeCatalog() ERROR! Unable to create to target Catalog directory `" + outputDir + "'");
            }
        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element rootCatalog =  _rootNode.getThreddsCatalog();

        File rootCatFile = new File(topDir,"catalog.xml");
        _log.debug("writeCatalog() Writing root catalog file '{}'",rootCatFile.getAbsolutePath());
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(rootCatFile));
        xmlo.output(rootCatalog,os);
        _log.debug("writeCatalog() Root Catalog: \n{}",xmlo.outputString(rootCatalog));


        for(String nodeKey: _catalogNodes.keySet()){
            //_log.debug("writeCatalog() ################################################################################");
            _log.debug("writeCatalog() catalog node: {}",nodeKey);

            SiteMapNode smn = _catalogNodes.get(nodeKey);
            File dir = new File(topDir,nodeKey);
            _log.debug("writeCatalog() Creating directory '{}'",dir.getAbsolutePath());
            dir.mkdirs();

            File catalog = new File(dir,"catalog.xml");
            _log.debug("writeCatalog() Writing catalog file '{}'",catalog.getAbsolutePath());

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(catalog));
            Element threddsCatalog = smn.getThreddsCatalog();
            xmlo.output(threddsCatalog,bos);

            _log.debug("writeCatalog() Catalog: \n{}",xmlo.outputString(threddsCatalog));
        }


    }


    private void process_line(String line) throws IOException {

        String fields[] = line.split("(?=\\s)");


        if(fields.length < 3)
            throw new IOException("SiteMap file appears to be corrupt! The current line does not have enough fields. line: "+line);

        String lastModified = fields[0];
        String s = fields[1].substring(1);
        long size = Long.parseLong(s);

        int start = line.indexOf(s);
        int url_begin = start + s.length() + 1;

        String url=line.substring(url_begin);

        String nodes[] = url.split("/");
        int lastNodeIndex= nodes.length - 1;
        int nodeIndex=0;
        StringBuilder nodeName = new StringBuilder();
        SiteMapNode currentNode = _rootNode;
        for(String node:nodes){
            if(nodeName.length()>0)
                nodeName.append("/");
            nodeName.append(node);

            _log.debug("process_url() Processing node: {} nodeName: {}",node, nodeName);
            if(nodeIndex!=lastNodeIndex){
                SiteMapNode cNode = _catalogNodes.get(nodeName);
                if(cNode!=null){
                    currentNode = cNode;
                }
                else {
                    SiteMapNode thisNode = new SiteMapNode(node, currentNode);
                    currentNode._children.put(node,thisNode);
                    _catalogNodes.put(nodeName.toString(),thisNode);
                    currentNode = thisNode;
                }
            }
            else {
                SiteMapItem dataset = currentNode._children.get(node);
                if(dataset==null){
                    dataset = new SiteMapItem(node,currentNode, size, lastModified);
                }
                currentNode._children.put(node,dataset);
            }
            nodeIndex++;
        }

    }


    public static void main(String args[]) throws Exception {

        String siteMapFileName  = "/Users/ndp/OPeNDAP/hyrax/bes/bin/siteMap.txt";
        String services         = "/Users/ndp/OPeNDAP/hyrax/services.xml";
        String hyraxServiceBase = "/opendap/hyrax/";

        SimpleSiteMapCatalogFactory ssmcFactory =  new SimpleSiteMapCatalogFactory(siteMapFileName, hyraxServiceBase);

        ssmcFactory.ingestSiteMap();

        System.out.println("################################################################################");
        System.out.println(ssmcFactory._rootNode.dump(""));

        ssmcFactory.writeCatalog("/tmp/catalog");
    }


}
