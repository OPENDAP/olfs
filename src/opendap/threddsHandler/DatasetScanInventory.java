package opendap.threddsHandler;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltTransformer;
import opendap.namespaces.THREDDS;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jun 28, 2010
 * Time: 12:53:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatasetScanInventory {  


    private static String transform =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                    "<!DOCTYPE xsl:stylesheet []>\n" +
                    "<xsl:stylesheet version=\"2.0\"\n" +
                    "                xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                    "                xmlns:fn=\"http://www.w3.org/2005/02/xpath-functions\"\n" +
                    "                xmlns:wcs=\"http://www.opengis.net/wcs\"\n" +
                    "                xmlns:gml=\"http://www.opengis.net/gml\"\n" +
                    "                xmlns:thredds=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"\n" +
                    "                xmlns:ncml=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\"\n" +
                    "                xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
                    "\n" +
                    "                >\n" +
                    "    <xsl:param name=\"remoteHost\" />\n" +
                    "    <xsl:param name=\"remoteRelativeURL\" />\n" +
                    "    <xsl:param name=\"remoteCatalog\" />\n" +
                    "    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>\n" +
                    "\n" +
                    "    <xsl:key name=\"service-by-name\" match=\"//thredds:service\" use=\"@name\"/>\n" +
                    "\n" +
                    "    <xsl:template match=\"@* | node()\">\n" +
                    "            <xsl:apply-templates />\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:catalog\">\n" +
                    "        <catalogIngest>\n" +
                    "            <xsl:apply-templates />\n" +
                    "        </catalogIngest>\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:datasetScan\">\n" +
                    "\n" +
                    "        <xsl:comment>########## datasetScanIngest generated from thredds:datasetScan ##########</xsl:comment>\n" +
                    "\n" +
                    "        <xsl:variable name=\"serviceName\" select=\"thredds:metadata/thredds:serviceName\"/>\n" +
                    "\n" +
                    "        <xsl:variable name=\"datasetScanLocation\">\n" +
                    "            <xsl:choose>\n" +
                    "                <xsl:when test=\"substring(@location,string-length(@location))='/'\">\n" +
                    "                    <xsl:value-of select=\"@location\"/>\n" +
                    "                </xsl:when>\n" +
                    "                <xsl:otherwise>\n" +
                    "                    <xsl:value-of select=\"concat(@location,'/')\"/>\n" +
                    "                </xsl:otherwise>\n" +
                    "\n" +
                    "            </xsl:choose>\n" +
                    "        </xsl:variable>\n" +
                    "\n" +
                    "        <xsl:variable name=\"datasetScanName\" select=\"@name\"/>\n" +
                    "\n" +
                    "        <xsl:variable name=\"serviceElement\" select=\"key('service-by-name', $serviceName)\"/>\n" +
                    "\n" +
                    "        <xsl:variable name=\"dapServices\"\n" +
                    "                      select=\"$serviceElement[@serviceType='OPENDAP'] | $serviceElement/thredds:service[@serviceType='OPENDAP'] \"/>\n" +
                    "\n" +
                    "        <datasetScanIngest name=\"{$datasetScanName}\">\n" +
                    "\n" +
                    "            <xsl:for-each select=\"$dapServices\">\n" +
                    "\n" +
                    "                <xsl:variable name=\"base\" select=\"@base\"/>\n" +
                    "\n" +
                    "                <xsl:variable name=\"lastCharOfBase\" select=\"substring($base,string-length($base))\"/>\n" +
                    "\n" +
                    "                <xsl:variable name=\"catalogPath\">\n" +
                    "                    <xsl:choose>\n" +
                    "\n" +
                    "                        <xsl:when test=\"$lastCharOfBase='/' and starts-with($datasetScanLocation,'/')\">\n" +
                    "                            <xsl:variable name=\"location\"\n" +
                    "                                          select=\"substring($datasetScanLocation,2,string-length($datasetScanLocation))\"/>\n" +
                    "                            <xsl:variable name=\"targetURL\" select=\"concat($base,$location)\"/>\n" +
                    "                            <xsl:value-of select=\"$targetURL\"/>\n" +
                    "                        </xsl:when>\n" +
                    "\n" +
                    "                        <xsl:when test=\"$lastCharOfBase!='/' and not(starts-with($datasetScanLocation,'/'))\">\n" +
                    "                            <xsl:variable name=\"targetURL\" select=\"concat($base,'/',$datasetScanLocation)\"/>\n" +
                    "                            <xsl:value-of select=\"$targetURL\"/>\n" +
                    "                        </xsl:when>\n" +
                    "\n" +
                    "                        <xsl:otherwise>\n" +
                    "                            <xsl:variable name=\"targetURL\" select=\"concat($base,$datasetScanLocation)\"/>\n" +
                    "                            <xsl:value-of select=\"$targetURL\"/>\n" +
                    "                        </xsl:otherwise>\n" +
                    "\n" +
                    "                    </xsl:choose>\n" +
                    "\n" +
                    "                </xsl:variable>\n" +
                    "\n" +
                    "                <catalogPath>\n" +
                    "                    <xsl:value-of select=\"$catalogPath\"/>\n" +
                    "                </catalogPath>\n" +
                    "\n" +
                    "            </xsl:for-each>\n" +
                    "\n" +
                    "            <xsl:copy-of select=\"thredds:metadata[@inherited='true']\"/>\n" +
                    "\n" +
                    "        </datasetScanIngest>\n" +
                    "\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "\n" +
                    "</xsl:stylesheet>\n" +
                    "";



    private static Logger log = org.slf4j.LoggerFactory.getLogger(DatasetScanInventory.class);
    private static Transformer _dsIngestTransformer;
    static {
        try {
            ByteArrayInputStream  is = new   ByteArrayInputStream(transform.getBytes());
            _dsIngestTransformer = new Transformer(new StreamSource(is));
        } catch (SaxonApiException e) {
            log.error("FAILED to build transform! Msg: "+e.getMessage());
        }
    }




    private static ConcurrentHashMap<String,Element> _inheritedMetadata = new ConcurrentHashMap<String,Element>();

    private static ReentrantLock _inventoryLock = new ReentrantLock();

    public static void ingestDatasetScan(Source catalog) throws SaxonApiException, IOException, JDOMException {

        try {
            //_inventoryLock.lock();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();


            SAXBuilder sb = new SAXBuilder();
            Document dsIngest;
            Element cp, md;
            Iterator datasetScanIngests, catalogPaths;
            String catalogPath;

            _dsIngestTransformer.transform(catalog, baos);
            dsIngest = sb.build(new ByteArrayInputStream(baos.toByteArray()));

            datasetScanIngests = dsIngest.getDescendants(new ElementFilter("datasetScanIngest"));

            Vector<Element> ingests = new Vector<Element>();
            while (datasetScanIngests.hasNext()) {
                ingests.add((Element)datasetScanIngests.next());
            }




            for (Element dsi : ingests) {
                log.debug("Processing datasetScan '" + dsi.getAttributeValue("name") + "'");

                md = dsi.getChild("metadata", THREDDS.NS);
                if (md != null) {
                    md.detach();
                    log.debug("Found inherited metadata.");

                    Iterator i  = md.getChildren("serviceName",THREDDS.NS).iterator();
                    Vector<Element> serviceNameElements =  new Vector<Element>();
                    while(i.hasNext()){
                        serviceNameElements.add((Element)i.next());
                    }

                    for(Element serviceName: serviceNameElements){
                        serviceName.detach();
                        log.debug("Removed Element <thredds:serviceName>"+serviceName.getTextTrim()+"</thredds:serviceName> from inherited metadata.");
                    }

                    catalogPaths = dsi.getDescendants(new ElementFilter("catalogPath"));
                    while (catalogPaths.hasNext()) {
                        cp = (Element) catalogPaths.next();
                        catalogPath = cp.getTextTrim();
                        _inheritedMetadata.put(catalogPath, md);
                    }
                }

            }
        }
        finally {
            //_inventoryLock.unlock();
        }
    }


    public static boolean hasInheritedMetadata(String catalogPath){

       Enumeration<String> paths = _inheritedMetadata.keys();
       String path;

       while(paths.hasMoreElements()){
           path = paths.nextElement();
           if(catalogPath.startsWith(path)){
               return true;
           }
       }

       return false;
    }



    public static Vector<Element> getInheritedMetadata(String catalogPath){


        // Find all of the applicable inherited metadata.
        Enumeration<String> paths = _inheritedMetadata.keys();
        String path;
        Vector<Element> metadataElements = new Vector<Element>();
        while(paths.hasMoreElements()){
            path = paths.nextElement();
            if(catalogPath.startsWith(path)){
                metadataElements.add((Element)_inheritedMetadata.get(path).clone());
            }
        }

        return metadataElements;


    }


}
