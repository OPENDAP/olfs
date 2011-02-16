package opendap.threddsHandler;

import net.sf.saxon.s9api.SaxonApiException;
import opendap.xml.Transformer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/7/11
 * Time: 5:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class NcmlManager {
    /**
     * This transform takes a THREDDS catalog as input and creates an XML file that contains elements representing
     * each thredds:datasetScan element that contains metadata elements whose 'inherited' attribute is true. A
     * metadataRootPath which represents where in the BES directory hierarchy the metadata is to be injected, is
     * computed for each applicable service and added  to the response.
     */
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
                    "    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>\n" +
                    "\n" +
                    "    <xsl:key name=\"service-by-name\" match=\"//thredds:service\" use=\"@name\"/>\n" +
                    "\n" +
                    "    <xsl:template match=\"@* | node()\">\n" +
                    "            <xsl:apply-templates />\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:service\">\n" +
                    "        <xsl:copy-of select=\".\"/>\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:catalog\">\n" +
                    "        <catalogIngest>\n" +
                    "            <xsl:apply-templates />\n" +
                    "        </catalogIngest>\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:datasetScan[thredds:metadata/@inherited='true']\">\n" +
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
                    "                <xsl:variable name=\"metadataRootPath\">\n" +
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
                    "                <metadataRootPath>\n" +
                    "                    <xsl:value-of select=\"$metadataRootPath\"/>\n" +
                    "                </metadataRootPath>\n" +
                    "\n" +
                    "            </xsl:for-each>\n" +
                    "\n" +
                    //"            <xsl:copy-of select=\"$serviceElement\"/>\n" +
                    "            <xsl:copy-of select=\"thredds:metadata[@inherited='true']\"/>\n" +
                    "\n" +
                    "        </datasetScanIngest>\n" +
                    "\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "\n" +
                    "</xsl:stylesheet>\n" +
                    "";


    private static Logger log = org.slf4j.LoggerFactory.getLogger(InheritedMetadataManager.class);
    private static Transformer _dsIngestTransformer;

    static {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(transform.getBytes());
            _dsIngestTransformer = new Transformer(new StreamSource(is));
        } catch (SaxonApiException e) {
            log.error("FAILED to build transform! Msg: " + e.getMessage());
        }
    }

    /**
     * Lock for thread safe operation.
     */
    private static ReentrantReadWriteLock _inventoryLock = new ReentrantReadWriteLock();


    /**
     * This ConcurrentHashMap<String,String[]> represents: ConcurrentHashMap<String catalogKey ,String[] metadataRootPathsFromThisCatalog>
     */
    private static ConcurrentHashMap<String, String[]> _catalog2MetadataMap = new ConcurrentHashMap<String, String[]>();

    /**
     * This  ConcurrentHashMap<String,HashMap<String,Vector<Element>>>  represents
     * ConcurrentHashMap<String metadataRootPath,HashMap<String catalogKey,Vector<Element metadataElement>>>
     */
    private static ConcurrentHashMap<String, HashMap<String, Vector<Element>>> _inheritedMetadata = new ConcurrentHashMap<String, HashMap<String, Vector<Element>>>();

    /**
     * This ConcurrentHashMap<String,HashMap<String,Element>> represents:
     * ConcurrentHashMap<String metadataRootPath,HashMap<String catalogKey,Element inheritedService>>
     */
    private static ConcurrentHashMap<String, HashMap<String, Vector<Element>>> _inheritedServices = new ConcurrentHashMap<String, HashMap<String, Vector<Element>>>();




    public static void ingestNcml(Catalog catalog) throws SaxonApiException, IOException, JDOMException {







    }


}
