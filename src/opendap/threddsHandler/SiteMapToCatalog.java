package opendap.threddsHandler;

import opendap.PathBuilder;
import opendap.gateway.HexAsciiEncoder;
import opendap.namespaces.THREDDS;
import opendap.namespaces.XLINK;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedHashMap;
import org.apache.commons.cli.*;

public class SiteMapToCatalog {

    private  Logger _log;
    private String _siteMapFileName;
    private File _siteMapFile;



    private LinkedHashMap<String, SiteMapNode> _catalogNodes;
    private SiteMapNode _rootNode;

    private String _hyraxServiceBase;
    private String _gatewayServiceBase;
    private String _s3BucketUrl;

    final static String usage  = SiteMapToCatalog.class.getName()+" -s <siteMapFileName> -b <hyraxServiceBase> <outputDirectory> ";

    private static String indentIncrement  = "  ";

    private static String siteMapFileName  = "/tmp/siteMap.txt";
    private static String hyraxServiceBase = "/opendap/hyrax/";
    private static String gatewayServiceBase = "/opendap/gateway/";
    private static String outputDirName = "/tmp/hic_ingest";
    private static String s3BucketUrl  = "https://s3.amazonaws.com/somewhereovertherainbow/";
    // private static String services   = "/Users/ndp/OPeNDAP/hyrax/services.xml";


    public SiteMapToCatalog(String siteMapFileName, String hyraxServiceBase, String gatewayServiceBase, String s3BucketUrl) throws IOException {
        _log = LoggerFactory.getLogger(this.getClass());
        _siteMapFileName = siteMapFileName;
        _siteMapFile = new File(_siteMapFileName);
        if(!_siteMapFile.canRead()){
            throw new IOException("Unable to locate and read site map file: "+_siteMapFileName);
        }
        _hyraxServiceBase = hyraxServiceBase;
        _catalogNodes = new LinkedHashMap<>();
        _rootNode = new SiteMapNode();
        _gatewayServiceBase = gatewayServiceBase;
        _s3BucketUrl = s3BucketUrl;
    }


    public class SiteMapNode extends SiteMapItem {
        boolean _isRootNode;
        LinkedHashMap<String, SiteMapItem> _children;
        String _indentIncrement = "  ";


        SiteMapNode() {
            super("/",null);
            _isRootNode = true;
            _children = new LinkedHashMap<>();
        }

        SiteMapNode(SiteMapNode parentNode, String name) {
            super(name,parentNode);
            _isRootNode = false;
            _children = new LinkedHashMap<>();
        }

        SiteMapNode(SiteMapNode parentNode, String name, long size, String lastModified){
            super(parentNode,name,size,lastModified);
            _isRootNode = false;
            _children = new LinkedHashMap<>();
        }

        @Override
        boolean isNode(){ return true;}

        @Override
        String getFullNodeName(){
            StringBuilder sb = new StringBuilder(_name);
            SiteMapNode cNode = _parentNode;
            while(cNode!=null && !cNode._isRootNode){
                sb.insert(0,"/");
                sb.insert(0,cNode._name);
                cNode = cNode._parentNode;
            }
            return sb.toString();
        }

        @Override
        public String dump(String indent ){
            StringBuilder sb = new StringBuilder(indent).append(getFullNodeName()).append("\n");
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

            service = new Element(THREDDS.SERVICE,THREDDS.NS);
            service.setAttribute("name","arch1");
            service.setAttribute("type","OPeNDAP");
            service.setAttribute("base",_gatewayServiceBase);
            catalog.addContent(service);

            Element topLevelDataset = new Element(THREDDS.DATASET,THREDDS.NS);
            topLevelDataset.setAttribute("name", getFullNodeName());
            topLevelDataset.setAttribute("ID",_hyraxServiceBase+ getFullNodeName());
            catalog.addContent(topLevelDataset);

            for(SiteMapItem smi : _children.values()) {
                if(smi.isNode())
                    topLevelDataset.addContent(getCatalogRef(smi));
                else
                    topLevelDataset.addContent(getDataset(smi, _hyraxServiceBase, _gatewayServiceBase, _s3BucketUrl));
            }
            return catalog;
        }
    }


    public class SiteMapItem {
        String _name;
        SiteMapNode _parentNode;

        long _size;
        String _lastModified;

        SiteMapItem(String name, SiteMapNode parentNode){
            _name = name;
            _parentNode = parentNode;
        }

        SiteMapItem(SiteMapNode parentNode, String name, long size, String lastModified){
            _name = name;
            _parentNode = parentNode;
            _size = size;
            _lastModified = lastModified;
        }

        boolean isNode(){ return false;}
        String getFullNodeName(){
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
           return indent + getFullNodeName()+"\n";
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
        catalogRef.setAttribute("ID",smi.getFullNodeName());
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
    public static Element getDataset(SiteMapItem smi, String hyraxServicePrefix, String gatewayServicePath, String s3BucketUrl){
        Element dataset = new Element(THREDDS.DATASET,THREDDS.NS);
        dataset.setAttribute("name",smi._name);
        dataset.setAttribute("ID", PathBuilder.pathConcat(hyraxServicePrefix,smi.getFullNodeName()));

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
        access.setAttribute(THREDDS.URL_PATH,smi.getFullNodeName());
        dataset.addContent(access);

        access = new Element(THREDDS.ACCESS,THREDDS.NS);
        access.setAttribute(THREDDS.SERVICE_NAME,"file");
        access.setAttribute(THREDDS.URL_PATH,smi.getFullNodeName());
        dataset.addContent(access);

        access = new Element(THREDDS.ACCESS,THREDDS.NS);
        access.setAttribute(THREDDS.SERVICE_NAME,"arch1");

        String url = PathBuilder.pathConcat(s3BucketUrl,smi.getFullNodeName());
        String gatewayRequestBase = HexAsciiEncoder.stringToHex(url);
        PathBuilder.pathConcat(gatewayServicePath,gatewayRequestBase);
        access.setAttribute(THREDDS.URL_PATH,gatewayRequestBase);
        dataset.addContent(access);

        return dataset;
    }


    public static String getHtmlCatalogFromNode(SiteMapNode smNode, String hyraxServicePrefix, String gatewayServicePath, String s3BucketUrl) throws IOException {

        StringBuilder cat = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        cat.append("<html>\n");
        cat.append("<head>\n");
        cat.append("<link rel=\"stylesheet\" href=\"/opendap/docs/css/contents.css\" type=\"text/css\"/>\n");
        cat.append("<title>OPeNDAP Hyrax: Contents of ").append(smNode.getFullNodeName()).append("</title>\n");
        cat.append("</head>\n");
        cat.append("<body>\n");
        cat.append("<img alt=\"OPeNDAP Logo\" src=\"/opendap/docs/images/logo.png\"/>\n");
        cat.append("<h1>\n");
        cat.append("Contents of ").append(smNode.getFullNodeName()).append("<br/>\n");
        cat.append("</h1>\n");
        if (smNode._parentNode != null) {
            cat.append("<span class=\"small_bold\" style=\"font-family:Courier New;text-align: left;\"><a href=\"../index.html\">Parent Dir</a></span>\n");
        }
        else {
            cat.append("<span class=\"small_bold\" style=\"font-family:Courier New; text-align: left;\"><a href=\".\">Eject</a></span>\n");
        }
        String hr = "<hr size=\"1\" width=\"100%\" noshade=\"noshade\"/>";
        cat.append("<pre>\n");
        cat.append("<table>\n");
        cat.append("<tr>\n");
        cat.append("<th align=\"center\">Name").append(hr).append("</th>\n");
        cat.append("<th align=\"center\">Date").append(hr).append("</th>\n");
        cat.append("<th align=\"center\">Size (bytes)").append(hr).append("</th>\n");
        cat.append("<th align=\"center\">Data Access").append(hr).append("</th>\n");
        cat.append("</tr>\n");


        for(SiteMapItem smi : smNode._children.values()){
            if(smi.isNode()){
                cat.append(getCatalogLink((SiteMapNode)smi,indentIncrement));
            }
            else {
                String s3ObjectUrl  = PathBuilder.pathConcat(s3BucketUrl,smi.getFullNodeName());
                String a1DatasetUrl = PathBuilder.pathConcat(gatewayServicePath,HexAsciiEncoder.stringToHex(s3ObjectUrl));
                String a2DatasetUrl = PathBuilder.pathConcat(hyraxServicePrefix,smi.getFullNodeName());
                String a0DatasetUrl = a2DatasetUrl.replace("Arch-2","Arch-0");

                if(a2DatasetUrl.equals(a0DatasetUrl))
                    a0DatasetUrl = null;

                cat.append(getDatasetHtmlRow(smi,a0DatasetUrl, a1DatasetUrl,a2DatasetUrl,s3ObjectUrl,indentIncrement));
            }
        }
        cat.append("</table>\n");
        cat.append("</pre>\n");
        cat.append("<hr size=\"1\" noshade=\"noshade\"/>\n");

        cat.append("<h3>OPeNDAP Hyrax-Protobeast</h3>");
        cat.append("</body>\n");
        cat.append("</html>\n");
        return cat.toString();
    }

    public static String getCatalogLink(SiteMapNode smNode, String indent){
        StringBuilder catLink = new StringBuilder();

        catLink.append(indent).append("<tr>\n");
        catLink.append(indent).append(indentIncrement);
        catLink.append("<td style=\"text-align: center; font-weight: bold; padding-bottom: 15px; padding-left: 10px;\">");
        catLink.append("<a href=\"").append(smNode._name).append("/index.html\">").append(smNode._name).append("</a>");
        catLink.append("</td>\n");

        catLink.append("<td style=\"text-align: center; padding-bottom: 15px; padding-left: 10px;\">").append(smNode._lastModified).append("</td>\n");
        catLink.append("<td style=\"text-align: right; padding-bottom: 15px; padding-left: 10px;\">").append(smNode._size).append("</td>\n");
        catLink.append("<td style=\"text-align: center; padding-bottom: 15px; padding-left: 10px;\"> --- </td>\n");
        catLink.append(indent).append("</tr>\n");

        return catLink.toString();
    }

    public static String getDatasetHtmlRow(SiteMapItem smi, String a0DatasetUrl, String a1DatasetUrl, String a2DatasetUrl, String s3ObjectUrl, String indent) throws IOException {

        if(smi.isNode())
            throw new IOException("Passed SiteMapItem "+smi.getFullNodeName()+" is a node (a.k.a. a catalog) and not a dataset.");

        String myIndent =  indent+indentIncrement;
        StringBuilder datasetRow = new StringBuilder();

        datasetRow.append(indent).append("<tr>\n");

        datasetRow.append(myIndent);
        datasetRow.append("<td style=\"text-align: left;vertical-align: top; padding-top: 3px; padding-left: 10px; font-weight: bold;\">").append(smi._name);
        datasetRow.append(myIndent);
        datasetRow.append("<td style=\"text-align: center;vertical-align: top; padding-top: 3px; padding-left: 10px;\">").append(smi._lastModified).append("</td>\n");
        datasetRow.append(indent);
        datasetRow.append("<td style=\"text-align: right;vertical-align: top; padding-top: 3px; padding-left: 10px;\">").append(smi._size).append("</td>\n");

        datasetRow.append(myIndent);
        datasetRow.append("<td style=\"padding-left: 10px;vertical-align: top; padding-bottom: 5px;\">\n");

        if(a0DatasetUrl!=null) {
            datasetRow.append(myIndent).append(indentIncrement);
            datasetRow.append("<div ><span class=\"small_bold\">(Arch-0)</span>\n");
            datasetRow.append(getDapLinks(a0DatasetUrl, myIndent + indentIncrement + indentIncrement));
            datasetRow.append(myIndent).append(indentIncrement);
            datasetRow.append("</div>\n");
        }

        datasetRow.append(myIndent).append(indentIncrement);
        datasetRow.append("<div ><span class=\"small_bold\">(<a href=\"").append(s3ObjectUrl).append("\">Arch-1</a>)</span>\n");
        datasetRow.append(myIndent).append(indentIncrement).append(indentIncrement);
        datasetRow.append("<!-- S3 URL: ").append(s3ObjectUrl).append(" -->\n");
        datasetRow.append(getDapLinks(a1DatasetUrl,myIndent+indentIncrement+indentIncrement));
        datasetRow.append(myIndent).append(indentIncrement);
        datasetRow.append("</div>\n");

        datasetRow.append(myIndent).append(indentIncrement);
        datasetRow.append("<div ><span class=\"small_bold\">(Arch-2)</span>\n");
        datasetRow.append(getDapLinks(a2DatasetUrl,myIndent+indentIncrement+indentIncrement));
        datasetRow.append(myIndent).append(indentIncrement);
        datasetRow.append("</div>\n");

        datasetRow.append(myIndent).append(indentIncrement);
        // datasetRow.append("<hr size=\"1\" noshade=\"noshade\"/>\n");

        datasetRow.append(myIndent).append("</td>\n");

        datasetRow.append(indent).append("</tr>\n");

        return datasetRow.toString();

    }


    private static String getDapLinks(String datasetUrl, String indent){
        StringBuilder links =  new StringBuilder();
        links.append(indent).append("<span>");
        links.append("<a href=\"").append(datasetUrl).append(".html\" >dap2_form</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".dds\" >dds</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".das\" >das</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".dods\">dods</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".ascii\">ascii</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".ifh\">ifh</a> |");
        links.append("</span>\n");
        links.append(indent).append("<span>");
        links.append("<a href=\"").append(datasetUrl).append(".dmr.html\">dap4_form</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".dmr.xml\">dmr</a> ");
        links.append("<a href=\"").append(datasetUrl).append(".dap\">dap</a> ");
        links.append("</span>\n");

        return links.toString();
    }


    public void ingestSiteMap() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(_siteMapFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                _log.debug("ingestSiteMap() Processing Line: {}",line);
                // process the line.
                process_line(line);
            }
        }

    }


    private File qcTargetDir(String dirName) throws IOException {
        File dir = new File(dirName);
        if(dir.exists()) {

            if (!dir.canWrite()) {
                throw new IOException(this.getClass().getName() + ".writeCatalogTree() ERROR! Unable to write to target Catalog directory `" + dirName + "'");
            }
        }
        else {
            if(!dir.mkdirs()){
                throw new IOException(this.getClass().getName() + ".writeCatalogTree() ERROR! Unable to create to target Catalog directory `" + dirName + "'");
            }
        }
        return dir;
    }

    public  void writeCatalogTree(String catalogDirName) throws IOException {

        File catalogDir = qcTargetDir(catalogDirName);

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element rootCatalog =  _rootNode.getThreddsCatalog();

        File rootCatFile = new File(catalogDir,"catalog.xml");
        _log.debug("writeCatalogTree() Writing root catalog file '{}'",rootCatFile.getAbsolutePath());
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(rootCatFile));
        xmlo.output(rootCatalog,os);
        _log.debug("writeCatalogTree() Root Catalog: \n{}",xmlo.outputString(rootCatalog));


        for(String nodeKey: _catalogNodes.keySet()){
            //_log.debug("writeCatalogTree() ################################################################################");
            _log.debug("writeCatalogTree() catalog node: {}",nodeKey);

            SiteMapNode smn = _catalogNodes.get(nodeKey);
            File dir = new File(catalogDir,nodeKey);
            _log.debug("writeCatalogTree() Creating directory '{}'",dir.getAbsolutePath());
            dir.mkdirs();

            File catalog = new File(dir,"catalog.xml");
            _log.debug("writeCatalogTree() Writing catalog file '{}'",catalog.getAbsolutePath());

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(catalog));
            Element threddsCatalog = smn.getThreddsCatalog();
            xmlo.output(threddsCatalog,bos);

            _log.debug("writeCatalogTree() Catalog: \n{}",xmlo.outputString(threddsCatalog));
        }


    }

    public  void writeDataTree(String targetDataDirName, boolean addHtmlCatalog) throws IOException {
        _log.debug("BUILDING NULL DATA TREE");
        File targetDataDir = qcTargetDir(targetDataDirName);
        writeDataTreeNode(targetDataDir,_rootNode, addHtmlCatalog);

    }
    public  void writeDataTreeNode(File targetDir, SiteMapNode dataTreeNode, boolean addHtmlCatalog) throws IOException {

        _log.debug("Processing node: {}",dataTreeNode.getFullNodeName());
        if(addHtmlCatalog){
            File catalogFile = new File(targetDir,"index.html");
            BufferedWriter bw = new BufferedWriter(new FileWriter(catalogFile));
            String catalogContent = getHtmlCatalogFromNode(dataTreeNode,_hyraxServiceBase,_gatewayServiceBase,_s3BucketUrl);
            bw.write(catalogContent);
            bw.close();
        }

        for(SiteMapItem smi :dataTreeNode._children.values()){
            if(smi instanceof SiteMapNode){
                File nodeDir = new File(targetDir,smi._name);
                nodeDir.mkdirs();
                writeDataTreeNode(nodeDir, (SiteMapNode)smi, addHtmlCatalog);
            }
            else {
                // it's a leaf!
                File dataset = new File(targetDir,smi._name);
                _log.debug("Processing dataset leaf: {}",dataset);
                if(!dataset.createNewFile()){
                    _log.debug("Dataset file {} already exists.",dataset);
                }
            }
        }

    }


    public void writeCombined(String outputDirName, boolean addHtmlCatalog) throws IOException {

        // Check to be sure we can write
        String catalogDirName = PathBuilder.pathConcat(outputDirName,"catalog_tree");
        String dataDirName = PathBuilder.pathConcat(outputDirName,"data_tree");

        writeCatalogTree(catalogDirName);
        writeDataTree(dataDirName, addHtmlCatalog);
    }




    private static Options options =  null;

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
        StringBuilder fullNodeName = new StringBuilder();
        SiteMapNode currentNode = _rootNode;
        for(String nodeStr:nodes){
            if(fullNodeName.length()>0)
                fullNodeName.append("/");
            fullNodeName.append(nodeStr);

            _log.debug("process_url() Processing node: '{}'   nodeName: '{}'",nodeStr, fullNodeName);
            if(nodeIndex!=lastNodeIndex){
                _log.debug("process_url() This is a node. '{}'", fullNodeName);
                SiteMapNode cNode = _catalogNodes.get(fullNodeName.toString());
                if(cNode!=null){
                    currentNode = cNode;
                }
                else {
                    _log.debug("process_url() Making new node. '{}'",nodeStr, fullNodeName);
                    SiteMapNode thisNode = new SiteMapNode(currentNode, nodeStr, size, lastModified);
                    currentNode._children.put(nodeStr,thisNode);
                    _catalogNodes.put(fullNodeName.toString(),thisNode);
                    currentNode = thisNode;
                }
            }
            else {
                _log.debug("process_url() This is last node, is leaf. '{}'",nodeStr, fullNodeName);
                SiteMapItem dataset = currentNode._children.get(nodeStr);
                if(dataset==null){
                    dataset = new SiteMapItem(currentNode, nodeStr, size, lastModified);
                }
                currentNode._children.put(nodeStr,dataset);
            }
            nodeIndex++;
        }

    }


    private static CommandLine parseCommandline(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        options = new Options();
        options.addOption("b", "hyraxServiceBase", true, "The hyrax service base. [default: '/opendap/hyrax/']");
        options.addOption("o", "outputDirName", true, "The to which to write the catalog. [default: '/tmp/hic_ingest']");
        options.addOption("s", "siteMapFileName", true, "Name of the file into which to write the site map. [default: 'siteMap.txt']");
        options.addOption("h", "help", false, "Usage information.");
        options.addOption("v", "verbose", false, "Verbose mode [Always On].");

        CommandLine line = parser.parse(options, args);
        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return null;
        }
        return line;
    }


    private  static boolean processCommandline(String[] args) throws Exception {

        CommandLine line = parseCommandline(args);

        if(line==null)
            return false;

        StringBuilder errorMessage = new StringBuilder();
        String s;

        // boolean verbose = line.hasOption("verbose");

        s = line.getOptionValue("siteMapFileName");
        if(s!=null){
            siteMapFileName = s;
        }

        s = line.getOptionValue("hyraxServiceBase");
        if(s!=null){
            hyraxServiceBase = s;
        }

        s = line.getOptionValue("outputDirName");
        if(s!=null){
            outputDirName = s;
        }

        s = line.getOptionValue("s3BucketUrl");
        if(s!=null){
            s3BucketUrl = s;
        }

        if(errorMessage.length()!=0){
            System.err.println(errorMessage);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return false;
        }


        return true;

    }


    public static void main(String args[]) throws Exception {


        processCommandline(args);

        SiteMapToCatalog ssmcFactory =  new SiteMapToCatalog(siteMapFileName, hyraxServiceBase, gatewayServiceBase, s3BucketUrl);

        ssmcFactory.ingestSiteMap();

        System.out.println("################################################################################");
        System.out.println(ssmcFactory._rootNode.dump(""));

        ssmcFactory.writeCombined(outputDirName, true);
    }


}
