/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2019 OPeNDAP, Inc.
 * // Author: Nathan Potter  <ndp@opendap.org>
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
package opendap.dap4;

import ch.qos.logback.classic.Level;
import opendap.logging.LogUtil;
import opendap.namespaces.DAP4;
import opendap.namespaces.DMRPP;
import opendap.xml.Util;
import org.apache.commons.cli.*;
import org.jdom.*;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

public class DmrppJoinExistingAggregator {

    private Logger log;

    public static final String S3_PROTOCOL    = "s3://";
    public static final String FILE_PROTOCOL  = "file://";
    public static final String HTTP_PROTOCOL  = "http://";
    public static final String HTTPS_PROTOCOL = "https://";

    java.net.URL url;

    // The list of dmr++ files that will be used to form the aggregation.
    private ArrayList<String> aggFileList;

    // The list of dmr++ access URLs loaded from
    private ArrayList<String> aggFileNames;

    // The list of dimensions derived from the template dataset
    private Map<String, Element> dimensions;

    // The coordinate vars in the template dataset
    private Map<String,Element> domainCoordinateVars;

    // The template aggregation variables from inside the aggDatasetTemplate document.
    private Map<String,Element> templateVars;

    // The template dataset document
    private Document aggDatasetTemplate;

    // The name of the dimension to aggregate over
    private String aggDimensionName;

    // The file name that holds the list of FQN variable names to be aggregated.
    private String aggVarsFileName;

    // The list of aggregation variable names loaded from aggVarsFileName.
    private ArrayList<String> aggVarNames;

    private boolean trustDatasetUrls;

    Element aggVarDimensionElement;
    Element aggVarElement;
    String aggVarName;

    /**
     *
     * @param coordinateAggVarName The name of the coordinate variable on which the aggregation will be built.
     * @param aggVariablesFileName The name of the file that contains the names of the variables to be aggregated.
     */
    public DmrppJoinExistingAggregator(String coordinateAggVarName, String aggVariablesFileName)
            throws DmrppAggException{
        log = (Logger) LoggerFactory.getLogger(this.getClass());

        aggFileList = new ArrayList<>();
        dimensions = new TreeMap<>();
        domainCoordinateVars = new TreeMap<>();
        templateVars = new TreeMap<>();
        aggDatasetTemplate = null;
        aggVarsFileName = aggVariablesFileName;
        aggVarNames = new ArrayList<>();
        aggFileNames = new ArrayList<>();
        trustDatasetUrls = false;

        if(coordinateAggVarName==null) {
            throw new DmrppAggException("You must specify a coordinate variable on which to build the aggregation.");
        }

        // We know we are aggregating the entire dataset so we want to put
        // the new dimension at the top level. And that means that the FQN will
        // be the dimension name with a leading slash. (But that's not cool
        // for the value of newAggDimensionElement@name.)
        aggDimensionName = coordinateAggVarName.startsWith("/")?"":"/"+coordinateAggVarName;
        aggVarName = (coordinateAggVarName.startsWith("/")?"":"/")+coordinateAggVarName;

    }
    void trustDatasetUrls(boolean v){ trustDatasetUrls = v; }
    boolean trustDatasetUrls(){return trustDatasetUrls; }

    /**
     *
     * @throws DmrppAggException
     */
    public void loadAggVarsList() throws IOException, DmrppAggException {
        loadListFile(aggVarsFileName,aggVarNames);
    }

    /**
     *
     * @throws DmrppAggException
     */
    public void loadAggFilesList(String fileName) throws IOException, DmrppAggException {
        loadListFile(fileName,aggFileNames);
    }

    public ArrayList<String> getAggFileNames(){
        return aggFileNames;
    }

    /**
     *
     * @throws DmrppAggException
     */
    public void loadListFile(String listFileName,  ArrayList<String> list) throws IOException,  DmrppAggException {

        if(listFileName!=null){
            File listFile  = new File(listFileName);
            if(!listFile.exists() || !listFile.isFile() || !listFile.canRead()) {
                throw new DmrppAggException("Unable to read listFile: "+listFileName);
            }
            else {
                log.debug("Ingesting ListFile: {}", listFileName);
                BufferedReader reader=null;
                try{
                    reader = new BufferedReader(new FileReader(listFileName));
                    String value = reader.readLine();
                    while(value!=null){
                        if(!value.isEmpty()) {
                            log.debug("Loading list value: {}", value);
                            list.add(value);
                        }
                        value = reader.readLine();
                    }
                }
                finally {
                    if(reader!=null)
                        reader.close();
                }
                log.debug("Finished ingesting ListFile: {}", listFileName);
            }
        }
    }


    /**
     *
     * @param dmrpp_urls
     * @throws MalformedURLException
     * @throws DmrppAggException
     */
    public void ingestDmrppList(ArrayList<String> dmrpp_urls) throws IOException, DmrppAggException {

        for(String dmrppUrl: dmrpp_urls){
            String targetUrl;
            if (dmrppUrl.startsWith(HTTP_PROTOCOL) ||
                    dmrppUrl.startsWith(HTTPS_PROTOCOL) ||
                    dmrppUrl.startsWith(S3_PROTOCOL)) {
                // It's http or s3 protocol, we use it as is.
                targetUrl = dmrppUrl;
            }
            else {
                if (dmrppUrl.contains(".."))
                    throw new DmrppAggException("Upward traversal paths (containing: \"..\") are not allowed.");

                if (!dmrppUrl.startsWith(FILE_PROTOCOL)) {
                    // It's not http(s), s3, or file protocol,
                    // we'll make it a file URL.
                    if (dmrppUrl.startsWith("/")) {
                        // It's a fully qualifed path so all we need
                        // do is tack on the file protocol part.
                        dmrppUrl = FILE_PROTOCOL + dmrppUrl;
                    } else {
                        // It's a relative URL so we interpret it in terms of the
                        // current working directory.
                        String cwd = System.getProperty("user.dir") + "/";
                        dmrppUrl = FILE_PROTOCOL + cwd + dmrppUrl;
                    }
                }
                targetUrl = dmrppUrl;
            }
            aggFileList.add(targetUrl);
        }

            /*
        if(dmrpp_urls.size()>1) {
        }
        else if(dmrpp_urls.size()==1){
            File arg0 = new File(dmrpp_urls.iterator().next());
            if(!arg0.exists())
                throw new DmrppAggException("Unable to locate file: "+arg0);

            if(arg0.isDirectory()) {
                File[] contents = arg0.listFiles();
                for (File f : contents) {
                    aggFileList.add(f.toURI().toURL());
                }
            }
            else if(arg0.isFile()){

                HashSet<String> aggFileNames = new HashSet<>();
                loadListFile(arg0.getAbsolutePath(),aggFileNames);
                for(String aggFileName:aggFileNames){
                    URL aggFileURL;
                    if(aggFileName.startsWith(HTTP_PROTOCOL) ||
                            aggFileName.startsWith(HTTPS_PROTOCOL) ||
                            aggFileName.startsWith(FILE_PROTOCOL)
                            ){
                        aggFileURL = new URL(aggFileName);
                    }
                    else {
                        File aggFile = new File(aggFileName);
                        if (!aggFile.exists() || !aggFile.isFile() || !aggFile.canRead()) {
                            throw new DmrppAggException("Unable to read aggFile: " + aggFile);
                        }
                        aggFileURL = aggFile.toURI().toURL();
                    }
                    log.debug("Adding aggFile: {}", aggFileURL);
                    aggFileList.add(aggFileURL);
                }
            }
            else {
                throw new DmrppAggException("The named file is neither a file or a dir: "+arg0);
            }
        }
        else {
            throw new DmrppAggException("This business won't work if you don't provide some dmr++ files my friend.");
        }
        */
    }

    /**
     * Aggregates the identified variables from the list of dmr++ files.
     * @return
     * @throws DmrppAggException
     * @throws DmrppAggException
     * @throws JDOMException
     */
    public Document aggregate() throws IOException, DmrppAggException, JDOMException {

        if(aggFileList.isEmpty())
            throw new DmrppAggException("No files were specified, unable to perform aggregation.");


        // Use the first file on the list as the template dataset
        String templateDmrppUrl = aggFileList.remove(0);
        Document aggDataset = Util.getDocument(templateDmrppUrl);
        ingestTemplateDataset(aggDataset);

        aggVarDimensionElement.getAttributeValue(DAP4.SIZE);
        int chunkIndex = 1;
        for(String url:aggFileList){
            log.info("Processing dmr++ dataset document: {}",url);
            aggDataset = Util.getDocument(url);
            ingestAggDataset(aggDataset, chunkIndex);
            chunkIndex++;
            aggVarDimensionElement.setAttribute(DAP4.SIZE, chunkIndex+"");
            log.info("Processing completed for: {}",url);
        }

        if(log.isDebugEnabled()) {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            for (String aggVarName : templateVars.keySet()) {
                Element aggVarElement = templateVars.get(aggVarName);
                log.debug("aggVar:\n{}", xmlo.outputString(aggVarElement));
            }
        }
        return aggDatasetTemplate;
    }

    /**
     *
     * @param datasetDoc
     * @param chunkIndex
     * @throws DmrppAggException
     */
    public void ingestAggDataset(Document datasetDoc, int chunkIndex) throws DmrppAggException, IOException {

        Element datasetElement = datasetDoc.getRootElement();
        String s = datasetElement.getAttributeValue(DMRPP.HREF,DMRPP.NS);
        String dataAccessURL = null;
        if(s!=null)
            dataAccessURL = s;

        aggContainer(datasetElement, dataAccessURL, chunkIndex);
    }


    /**
     *
     * @param container
     * @param chunkIndex
     * @throws DmrppAggException
     */
    private void aggContainer(Element container,  String dataURL , int chunkIndex ) throws DmrppAggException {

        @SuppressWarnings("unchecked")
        List<Element> dapObjects = container.getChildren();
        for(Element dapObject:dapObjects){
            String aggVarName = getFQN(dapObject);
            log.debug("Begin processing {}",aggVarName);
            if (dapObject.getName().equals(DAP4.GROUP)){
                aggContainer(dapObject, dataURL, chunkIndex);
            }
            else if(dapObject.getName().equals(DAP4.DIMENSION)){
                String dimName = getFQN(dapObject);
                Element templateDimension = dimensions.get(dimName);
                if(templateDimension==null){
                    throw new DmrppAggException("OUCH! Encountered non templated Dimension declaration for '"+dimName+"'");
                }
            }
            else if (dapObject.getName().equals(DAP4.STRUCTURE)){
                aggContainer(dapObject, dataURL, chunkIndex);
            }
            else if (dapObject.getName().equals(DAP4.SEQUENCE)){
                aggContainer(dapObject, dataURL, chunkIndex);
            }
            else if (dapObject.getName().equals(DAP4.INT8)   |
                    dapObject.getName().equals(DAP4.UINT8)   |
                    dapObject.getName().equals(DAP4.BYTE)    |
                    dapObject.getName().equals(DAP4.CHAR)    |
                    dapObject.getName().equals(DAP4.INT16)   |
                    dapObject.getName().equals(DAP4.UINT16)  |
                    dapObject.getName().equals(DAP4.INT32)   |
                    dapObject.getName().equals(DAP4.UINT32)  |
                    dapObject.getName().equals(DAP4.INT64)   |
                    dapObject.getName().equals(DAP4.UINT64)  |
                    dapObject.getName().equals(DAP4.FLOAT32) |
                    dapObject.getName().equals(DAP4.FLOAT64) |
                    dapObject.getName().equals(DAP4.STRING)  |
                    dapObject.getName().equals(DAP4.D_URI)   |
                    dapObject.getName().equals(DAP4.OPAQUE)
                    ){

                if(aggVarName.equals(aggDimensionName) || !domainCoordinateVars.containsKey(aggVarName)) {

                    Element templateVar = templateVars.get(aggVarName);

                    if (templateVar == null) {
                        log.info("Skipping variable '{}', no matching aggVarTemplate.", aggVarName);
                    } else {
                        @SuppressWarnings("unchecked")
                        List<Element> vDims = dapObject.getChildren(DAP4.DIM, DAP4.NS);
                        @SuppressWarnings("unchecked")
                        List<Element> tDims = templateVar.getChildren(DAP4.DIM, DAP4.NS);
                        if (tDims.size() != vDims.size()) {
                            throw new DmrppAggException("The template variable does not have the same number of Dimensions " +
                                    "as the aggregation variable. (name: " + aggVarName + ")");
                        } else {

                            Element vChunksElement = dapObject.getChild(DMRPP.CHUNKS, DMRPP.NS);
                            if (vChunksElement == null) {
                                throw new DmrppAggException("The aggregation variable '" + aggVarName + "' has no chunks!");
                            }

                            Element vChunkDimSizesElement = vChunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);
                            if (vChunkDimSizesElement == null) {
                                vChunkDimSizesElement = hack_simple_var_chunks(aggVarName,vChunksElement);
                            }
                            List<Integer> vChunkDimSizes = parseChunkDimensionSizes(vChunkDimSizesElement);

                            Element tChunksElement = templateVar.getChild(DMRPP.CHUNKS, DMRPP.NS);
                            if (tChunksElement == null) {
                                throw new DmrppAggException("The template variable '" + aggVarName + "' has no chunks!");
                            }
                            Element tChunkDimSizesElement = tChunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);

                            List<Integer> tChunkDimSizes = parseChunkDimensionSizes(tChunkDimSizesElement);

                            String msg = "The template variable chunk dimension sizes (" +
                                    tChunkDimSizesElement.getTextTrim() + ") are not compatible " +
                                    "with the aggregation variable chunk dimension sizes (" +
                                    vChunkDimSizesElement.getTextTrim() + ") (name: " + aggVarName + ")";

                            if (vChunkDimSizes.size() != tChunkDimSizes.size()) {
                                throw new DmrppAggException(msg);
                            }
                            int vSize, tSize;
                            for (int i = 0; i < tChunkDimSizes.size(); i++) {
                                vSize = vChunkDimSizes.get(i);
                                tSize = tChunkDimSizes.get(i);
                                if (vSize != tSize) {
                                    throw new DmrppAggException(msg);
                                }
                            }

                            @SuppressWarnings("unchecked")
                            List<Element> vChunkElements = vChunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);
                            for (Element vChunkElement : vChunkElements) {
                                String chunkPositionInArray = vChunkElement.getAttributeValue(DMRPP.CHUNK_POSITION_IN_ARRAY);
                                if(vChunkDimSizes.size()>1) {
                                    chunkPositionInArray = chunkPositionInArray.replaceFirst("\\[0,", "[" + chunkIndex + ",");
                                }
                                else {
                                    chunkPositionInArray = chunkPositionInArray.replaceFirst("\\[0", "[" + chunkIndex);
                                }
                                Element chunk = (Element) vChunkElement.clone();
                                chunk.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY,chunkPositionInArray);

                                String href = chunk.getAttributeValue(DMRPP.HREF,DMRPP.NS);
                                if(href==null) {
                                    chunk.setAttribute(DMRPP.HREF, dataURL,DMRPP.NS);
                                }
                                log.info("./Chunk@dmrpp:href is set to current chunk to: {}",chunk.getAttributeValue(DMRPP.HREF,DMRPP.NS));

                                if(trustDatasetUrls())
                                    chunk.setAttribute(DMRPP.TRUST,Boolean.toString(trustDatasetUrls()),DMRPP.NS);

                                tChunksElement.addContent(chunk);
                            }
                            log.info("Added {} chunks to {}",vChunkElements.size(), aggVarName);
                        }
                    }
                }
            }
            else if (dapObject.getName().equals(DAP4.ENUMERATION)){
                log.info("Skipping {} name: {}",DAP4.ENUMERATION,dapObject.getAttributeValue(DAP4.NAME));
            }
            else if (dapObject.getName().equals(DAP4.ENUM)){
                log.info("Skipping {} name: {}",DAP4.ENUM,dapObject.getAttributeValue(DAP4.NAME));
            }
            log.debug("Finished processing {}",aggVarName);
        }
    }


    /**
     *
     * @param cdsElement
     * @return
     */
    public static ArrayList<Integer> parseChunkDimensionSizes(Element cdsElement){
        ArrayList<Integer> cds = new ArrayList<>();

        String[] values = cdsElement.getTextTrim().split(" ");
        for(String s : values){
            cds.add(Integer.valueOf(s));
        }
        return cds;
    }

    /**
     * If the user has specified a subset of the variables to be aggregated this
     * will "prune" the non aggregated variables from the result document. It checks
     * to be sure NOT to drop coordinate dimension variables (which are not
     * aggregated but need to be kept because, well... coordinate vars, right?
     */
    public void pruneAggTree() throws DmrppAggException {

        log.debug("pruneAggTree() - BEGIN");

        if(aggVarNames.isEmpty())
            return;

        Map<String,Element> dropList = new HashMap<>();
        HashMap<String,List<String>> outerDims = new HashMap<>();


        for(Map.Entry<String,Element> entry: templateVars.entrySet()){
            log.debug("Processing Template Variable name: {} type: {}",entry.getKey(), entry.getValue().getName());

            String varFQN = entry.getKey();
            Element varElement = entry.getValue();

            // The outer most Dim is the first Dim in the document order.
            Element outerDim = varElement.getChild(DAP4.DIM,DAP4.NS);
            if(outerDim==null)
                throw new DmrppAggException("OUCH! The aggVarTemplate variable "+varFQN+" has no "+DAP4.DIM+" elements.");

            String outerDimName = outerDim.getAttributeValue(DAP4.NAME);

            List<String> matchingVars = outerDims.get(outerDimName);
            if(matchingVars==null) {
                matchingVars = new ArrayList<>();
                outerDims.put(outerDimName,matchingVars);
            }
            matchingVars.add(outerDimName);

            if(!aggVarNames.contains(varFQN) && !domainCoordinateVars.containsKey(varFQN)){
                log.debug("Adding variable {} to drop list",varFQN);
                dropList.put(varFQN,varElement);
            }
        }
        for(Map.Entry<String,Element> entry: dropList.entrySet()) {
            String varFQN = entry.getKey();
            Element varElement = entry.getValue();
            log.debug("Removing variable {} from templateVars",varFQN);
            templateVars.remove(varFQN);
            log.debug("Detaching variable element {} from parent document.",varFQN);
            varElement.detach();
        }

    }

    /**
     *
     * @param templateDoc
     * @throws DmrppAggException
     */
    public void ingestTemplateDataset(Document templateDoc) throws IOException, DmrppAggException {

        // Stash the template dataset document
        aggDatasetTemplate = templateDoc;
        // Grab the root element, which must be a dap4:Dataset
        Element dataset = templateDoc.getRootElement();
        String rootName = dataset.getName();
        String rootNamespaceURI = dataset.getNamespace().getURI();
        if(!rootName.equals(DAP4.DATASET) || !rootNamespaceURI.equals(DAP4.NS.getURI())) {
            StringBuilder sb = new StringBuilder();
            sb.append("Input files must be valid DAP4 DMR documents.");
            sb.append("The root element '").append(rootName).append("' in the namespace '");
            sb.append(rootNamespaceURI).append("' ").append("is not a valid for a DAP4 DMR.");
            throw new DmrppAggException(sb.toString());
        }
        // Check the top-level URL for the dataset.
        // This implementation only support top level dmrpp:href URLs and not individual chunk URLs
        String dataURL = null;
        String s = dataset.getAttributeValue(DMRPP.HREF,DMRPP.NS);
        // There might not be a Dataset level URL if all the Chunks already have them, so null is OK, but if
        // it's not null then grab the dataURL.
        if(s!=null){
            dataURL =s;

            if(trustDatasetUrls()) {
                // Mark the Dataset to trust the URL.
                dataset.setAttribute(DMRPP.TRUST, Boolean.toString(trustDatasetUrls()), DMRPP.NS);
            }
        }

        // Since a Dataset is in essence a "container" or, in effect, the "root" group
        // we can just throw it into the recursive ingestContainerTemplate()
        ingestContainerTemplate(dataset, dataURL);

        ingestDomainCoordinateVars();

        pruneAggTree();

        if(aggVarElement == null){
            throw new DmrppAggException("Unable to locate the aggregation coordinate variable named: "+aggVarName);
        }

        @SuppressWarnings("unchecked")
        Iterator<Element> dims = aggVarElement.getDescendants(new ElementFilter(DAP4.DIMENSION,DAP4.NS));
        if(dims.hasNext()){
            while(dims.hasNext()){
                Element dim = dims.next();
                String dimFQN = getFQN(dim);
                log.info("Found dap4:Dimension: ", dimFQN);
            }
            aggVarDimensionElement.setAttribute(DAP4.SIZE, "1");
        }
        else {
            dims = aggVarElement.getDescendants(new ElementFilter(DAP4.DIM,DAP4.NS));
            while(dims.hasNext()){
                Element dim = dims.next();
                String dimName = dim.getAttributeValue(DAP4.NAME);
                String dimSize = dim.getAttributeValue(DAP4.SIZE);

                if(dimName==null){
                    // Handle anonymous dimensions
                }
                else {
                    log.info("Found <dap4:Dim name={} />",dimName);
                    aggVarDimensionElement = dimensions.get(dimName);
                    dimName = aggVarDimensionElement.getAttributeValue(DAP4.NAME);
                    log.info("Found <dap4:Dimension name={} />",dimName);
                }
            }
        }


        if(log.isDebugEnabled()) {
            log.debug("Ingested Template Dataset Summary:");
            for (Map.Entry<String, Element> entry : dimensions.entrySet()) {
                log.debug("    Dimension: '{}'", entry.getKey());
            }
            for (Map.Entry<String, Element> entry : domainCoordinateVars.entrySet()) {
                log.debug("    coordinateVar: '{}'", entry.getKey());
            }
            for (Map.Entry<String, Element> entry : templateVars.entrySet()) {
                log.debug("    aggVarTemplates: name: '{}' type: '{}'", entry.getKey(), entry.getValue().getName());
            }
        }

    }

    /**
     * This method locates every domain coordinate variable,  which for now are those whose name exactly
     * matches an associated variable. In the process it locates the aggregation variable element.
     * The domain coordinate variables are removed from the list of aggregatable template variables
     * and keeps them in a separate list since, except for the aggregation variable (which must be a
     * domain coordinate variable), we will not be aggregating the domain coordinate variables.
     *
     */
    public void ingestDomainCoordinateVars(){
        for(Map.Entry<String,Element> entry : dimensions.entrySet()){
            String dimName = getFQN(entry.getValue());
            log.info("Locating domain coordinate for dimension: {}",dimName);

            Element matchingVarible = templateVars.get(dimName);

            if(matchingVarible!=null) {
                if(!dimName.equals(aggVarName)){
                    // This domain coordinate variable is not the aggregation variable,
                    // we don't want to aggregate it.
                    log.info("Adding domain coordinate variable: {} to domainCoordinateVars",dimName);
                    // Add the matchingVarible the domain coordinate variable list.
                    domainCoordinateVars.put(dimName,matchingVarible);

                    // And we drop it from the template variable list because
                    // we are not going to aggregate domain coordinate variables other than the aggregation variable.
                    log.info("Removing domain coordinate variable: {} from templateVars",dimName);
                    templateVars.remove(dimName);
                }
                else {
                    // If the dimension name matches the aggVarName then it's the AggVar!
                    aggVarElement = matchingVarible;
                    log.info("Found aggregation variable element: {}",aggVarName);
                    if(log.isDebugEnabled()){
                        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                        log.debug("Aggregation Variable Element:\n{}",xmlo.outputString(aggVarElement));
                    }
                }
            }

        }
    }

    /**
     *
     * @param varFQN
     * @param chunksElement
     * @return
     * @throws DmrppAggException
     */
    private Element hack_simple_var_chunks(String varFQN, Element chunksElement) throws DmrppAggException
    {
        // A missing chunkDimensionSizes element is bad, but may be recoverable is it's a variable
        // of one dimension with a single chunk... Let's check that out.
        List<Element> chunkElements = chunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);
        if (chunkElements.size() != 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("The template variable '").append(varFQN);
            sb.append("' has more than one chunk, and it's missing a child ");
            sb.append(DMRPP.CHUNK_DIMENSION_SIZES).append(" element.");
            sb.append("Unable to include this variable in the aggregation");
            throw new DmrppAggException(sb.toString());
        }

        // Now we hack the variable by adding chunkDimensionSize and chunkPositionInArray
        // appropriately
        Element chunkDimSizesElement = new Element(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);
        chunkDimSizesElement.setText("1");
        chunksElement.addContent(0, chunkDimSizesElement);

        Element theChunk = chunkElements.get(0);
        theChunk.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY, "[0]");

        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Added ").append(DMRPP.CHUNK_DIMENSION_SIZES).append(" to the ");
            sb.append(DMRPP.CHUNKS).append(" the variable ").append(varFQN);
            log.info(sb.toString());
        }
        return chunkDimSizesElement;
    }

    /**
     *
     * @param container
     * @throws DmrppAggException
     */
    private void ingestContainerTemplate(Element container, String dataURL ) throws DmrppAggException {

        @SuppressWarnings("unchecked")
        List<Element> childElements = container.getChildren();
        for(Element childElement:childElements){
            String childFQN = getFQN(childElement);

            if (childElement.getName().equals(DAP4.GROUP)){
                // Groups are containers, recurse...
                ingestContainerTemplate(childElement,dataURL);
            }
            else if(childElement.getName().equals(DAP4.DIMENSION)){
                // Collect all of the Dimension definitions.
                dimensions.put(childFQN,childElement);
            }
            else if (childElement.getName().equals(DAP4.STRUCTURE)){
                // Structures are containers, recurse...
                ingestContainerTemplate(childElement,dataURL);
            }
            else if (childElement.getName().equals(DAP4.SEQUENCE)){
                // Sequences are containers, recurse...
                ingestContainerTemplate(childElement,dataURL);
            }
            else if (childElement.getName().equals(DAP4.INT8)   |
                    childElement.getName().equals(DAP4.UINT8)   |
                    childElement.getName().equals(DAP4.BYTE)    |
                    childElement.getName().equals(DAP4.CHAR)    |
                    childElement.getName().equals(DAP4.INT16)   |
                    childElement.getName().equals(DAP4.UINT16)  |
                    childElement.getName().equals(DAP4.INT32)   |
                    childElement.getName().equals(DAP4.UINT32)  |
                    childElement.getName().equals(DAP4.INT64)   |
                    childElement.getName().equals(DAP4.UINT64)  |
                    childElement.getName().equals(DAP4.FLOAT32) |
                    childElement.getName().equals(DAP4.FLOAT64) |
                    childElement.getName().equals(DAP4.STRING)  |
                    childElement.getName().equals(DAP4.D_URI)   |
                    childElement.getName().equals(DAP4.OPAQUE)
                    ){
                // So it's an atomic type - maybe even an array!

                @SuppressWarnings("unchecked")
                List<Element> dims = childElement.getChildren(DAP4.DIM,DAP4.NS);
                boolean isArray = dims.size()>0;

                if(isArray){
                    @SuppressWarnings("unchecked")
                    Element chunksElement = childElement.getChild(DMRPP.CHUNKS, DMRPP.NS);
                    if (chunksElement != null) {
                        Element tChunkDimSizesElement = chunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);
                        @SuppressWarnings("unchecked")
                        List<Element> chunkElements = chunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);

                        if (tChunkDimSizesElement == null) {
                            // This is a simple hack one dimensional variables with a single chunk.
                            tChunkDimSizesElement = hack_simple_var_chunks(childFQN, chunksElement);
                            //chunksElement.addContent(0, tChunkDimSizesElement);
                        }
                        for (Element chunkElement : chunkElements) {
                            String chunkPositionInArray = chunkElement.getAttributeValue(DMRPP.CHUNK_POSITION_IN_ARRAY);
                            if (chunkPositionInArray != null) {
                                // Took this out, looks like a no-op
                                // chunkElement.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY, chunkPositionInArray.replaceFirst("\\[0,", "[0,"));
                                String href = chunkElement.getAttributeValue(DMRPP.HREF);
                                if (href == null) {
                                    if (dataURL == null) {
                                        String sb = "Unable to aggregate dmr++ document. " +
                                                "Both the Dataset and the Chunk are missing a " + DMRPP.HREF + " attribute.";
                                        throw new DmrppAggException(sb);
                                    }
                                    chunkElement.setAttribute(DMRPP.HREF, dataURL.toString());
                                }
                            }
                            else if(chunkElements.size()==1){
                                chunkElement.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY, "[0]");
                            }
                            else {
                                StringBuilder sb = new StringBuilder();
                                sb.append("ERROR: Found multiple chunk element in the variable ");
                                sb.append(childFQN).append(", but one or more chunk elements are missing the ");
                                sb.append(DMRPP.CHUNK_POSITION_IN_ARRAY).append(" attribute.");
                                throw new DmrppAggException(sb.toString());
                            }
                            if (trustDatasetUrls) {
                                chunkElement.setAttribute(DMRPP.TRUST, "true", DMRPP.NS);
                            }
                        }
                        templateVars.put(childFQN, childElement);
                    }
                }
                else {
                    log.warn("The variable {} is not a Array variable, SKIPPING.",childFQN);
                }
            }
            else if (childElement.getName().equals(DAP4.ENUMERATION)){
                log.warn("SKIPPING {} name: {}",DAP4.ENUMERATION,childFQN);
            }
            else if (childElement.getName().equals(DAP4.ENUM)){
                log.warn("SKIPPING {} name: {}",DAP4.ENUM,childFQN);
            }

        }
    }




    /** Computes a DAP4 FQN for the passed Element.
     * @param var
     * @return
     */
    public String getFQN(Element var){

        if(var.getName().equals(DAP4.DIM) && var.getNamespace()==DAP4.NS){
            String name = var.getAttributeValue(DAP4.NAME);
            if(name==null || name.isEmpty()){
                return ""; // maybe null??
            }
            return name;
        }

        ArrayList<String> name_fields = new ArrayList<>();
        name_fields.add(var.getAttributeValue(DAP4.NAME));

        Element parentElement = var.getParentElement();

        while(parentElement!=null && !parentElement.getName().equals(DAP4.DATASET)){
            name_fields.add(parentElement.getAttributeValue(DAP4.NAME));
            parentElement = parentElement.getParentElement();
        }

        StringBuilder fqn = new StringBuilder();
        for (ListIterator<String> iterator = name_fields.listIterator(name_fields.size()); iterator.hasPrevious();) {
            String field = iterator.previous();
            fqn.append("/").append(field);
        }

        return fqn.toString();

    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)  {

        String outfile = null;
        String joinNewDimName = null;
        String debugLevel;
        Class introSpec = DmrppJoinExistingAggregator.class;
        String loggerName = introSpec.getName();
        String aggVarsFile = null;
        String aggFilesListFileName = null;
        boolean trustUrls = false;

        Logger log = (Logger) LoggerFactory.getLogger(introSpec);
        log.setLevel(Level.ERROR);
        /*
        Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders();
        while (it.hasNext()) {
            Appender<ILoggingEvent> app = it.next();
            System.out.println( app.getName() );
        }
        */


        try {
            //----------------------------------------------------------------------
            Options options = createCmdLineOptions();
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            //---------------------------
            // Usage/Help
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(120);
                formatter.printHelp( "DmrppAggregator", options );
                return;
            }
            //---------------------------
            // Debug control
            if (cmd.hasOption("d")) {
                debugLevel = cmd.getOptionValue("d");
                LogUtil.setLogLevel(loggerName,debugLevel);
            }

            //---------------------------
            // Output File
            if (cmd.hasOption("o")) {
                outfile = cmd.getOptionValue("o");
            }

            //---------------------------
            // aggregation files
            if (cmd.hasOption("a")) {
                aggFilesListFileName = cmd.getOptionValue("a");
            }

            //---------------------------
            // Name of the joinNew Dimension
            if (cmd.hasOption("n")) {
                joinNewDimName = cmd.getOptionValue("n");
            }

            //---------------------------
            // Name of the variables file
            if (cmd.hasOption("v")) {
                aggVarsFile = cmd.getOptionValue("v");
            }

            //---------------------------
            // Mark the URLs as trusted.
            if (cmd.hasOption("t")) {
                trustUrls = true;
            }

            //----------------------------------------------------------------------
            //----------------------------------------------------------------------

            DmrppJoinExistingAggregator dAgg = new DmrppJoinExistingAggregator(joinNewDimName, aggVarsFile);
            dAgg.trustDatasetUrls(trustUrls);
            dAgg.loadAggFilesList(aggFilesListFileName);
            dAgg.ingestDmrppList(dAgg.getAggFileNames());
            dAgg.loadAggVarsList();

            Document aggDatasetDoc = dAgg.aggregate();

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            if(outfile!=null){
                try (FileOutputStream fos = new FileOutputStream(outfile)) {
                    xmlo.output(aggDatasetDoc, fos);
                }
            }
            else {
                System.out.println(xmlo.outputString(aggDatasetDoc));
            }
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
    }

    /**
     *
     * @return
     */
    private static Options createCmdLineOptions(){

        Options options = new Options();
        options.addOption("h", "help",    false, "Print this usage statement.");
        options.addOption("d", "debug",   true, "Turn on debug output. The argument " +
                "is one of: all, error, warn, info, debug, off");
        options.addOption("o", "output",  true, "File into which the output will " +
                "be written.");
        options.addOption("a", "aggFiles", true, "A file containing a list the dmr++ " +
                "files to aggregate.");
        options.addOption("n", "dimName", true, "joinNew dimension name");
        options.addOption("v", "variablesFile", true, "A file containing a list of " +
                "the names of the variables to be aggregated.");
        options.addOption("t", "trust", false, "If used will tag as trusted all the " +
                "URLs processed from the dmr++ file.");
        return options;
    }


}
