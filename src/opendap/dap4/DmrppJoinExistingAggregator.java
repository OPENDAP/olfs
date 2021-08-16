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
import opendap.bes.BadConfigurationException;
import opendap.namespaces.DAP4;
import opendap.namespaces.DMRPP;
import opendap.xml.Util;
import org.apache.commons.cli.*;
import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class DmrppJoinExistingAggregator {

    private Logger log;


    public static final String FILE_PROTOCOL  = "file://";
    public static final String HTTP_PROTOCOL  = "http://";
    public static final String HTTPS_PROTOCOL = "https://";

    private ArrayList<URL> aggFileList;
    private Map<String, Element> dimensions;
    private Map<String,Element> coordinateVars;
    private Map<String,Element> aggVarTemplates;

    private Document aggDatasetTemplate;
    private String newAggDimensionName;
    private Element newAggDimensionElement;
    private String aggVarsFileName;
    private Set<String> aggVarNames;
    private String aggDataFileName;
    private Set<String> aggFileNames;

    /**
     *
     * @param dimName
     * @param aggVariablesFileName
     */
    public DmrppJoinExistingAggregator(String dimName, String aggVariablesFileName){
        log = (Logger) LoggerFactory.getLogger(this.getClass());

        aggFileList = new ArrayList<>();
        dimensions = new TreeMap<>();
        coordinateVars = new TreeMap<>();
        aggVarTemplates = new TreeMap<>();
        aggDatasetTemplate = null;
        aggVarsFileName = aggVariablesFileName;
        aggVarNames = new HashSet<>();
        aggFileNames = new LinkedHashSet<>();

        if(dimName==null) {
            dimName="myNewDim";
        }
        newAggDimensionElement = new Element(DAP4.DIMENSION,DAP4.NS);
        newAggDimensionElement.setAttribute(DAP4.NAME,dimName);
        newAggDimensionElement.setAttribute(DAP4.SIZE,"0");

        // We know we are aggregating the entire dataset so we want to put
        // the new dimension at the top level. And that means that the FQN will
        // be the dimension name with a leading slash. (But that's not cool
        // for the value of newAggDimensionElement@name.)
        newAggDimensionName = dimName.startsWith("/")?"":"/"+dimName;

    }

    /**
     *
     * @throws IOException
     */
    public void loadAggVarsList() throws IOException {
        loadListFile(aggVarsFileName,aggVarNames);
    }

    /**
     *
     * @throws IOException
     */
    public void loadAggFilesList(String args) throws IOException {
        loadListFile(args,aggFileNames);
    }

    public String[] getAggFileNames(){
        return aggFileNames.toArray(new String[aggFileNames.size()]);
    }

    /**
     *
     * @throws IOException
     */
    public void loadListFile(String listFileName,  Set<String> list) throws IOException {

        if(listFileName!=null){
            File listFile  = new File(listFileName);
            if(!listFile.exists() || !listFile.isFile() || !listFile.canRead()) {
                throw new IOException("Unable to read listFile: "+listFileName);
            }
            else {
                BufferedReader reader=null;
                try{
                    reader = new BufferedReader(new FileReader(listFileName));
                    String value = reader.readLine();
                    while(value!=null){
                        if(!value.isEmpty()) {
                            log.debug("Loading value: {}", value);
                            list.add(value);
                        }
                        value = reader.readLine();
                    }
                }
                finally {
                    if(reader!=null)
                        reader.close();
                }
            }
        }
    }


    /**
     *
     * @param args
     * @throws MalformedURLException
     * @throws BadConfigurationException
     */
    public void loadDmrppList(String[] args) throws IOException, BadConfigurationException {

        if(args.length>1) {
            for (String s : args) {
                if (s.startsWith(HTTP_PROTOCOL) || s.startsWith(HTTPS_PROTOCOL)) {
                    URL remoteDmrpp = new URL(s);
                    aggFileList.add(remoteDmrpp);
                } else {

                    if (s.contains(".."))
                        throw new BadConfigurationException("Upward traversal paths (containing: \"..\") are not allowed.");

                    if (!s.startsWith(FILE_PROTOCOL)) {
                        if (s.startsWith("/")) {
                            s = FILE_PROTOCOL + s;
                        } else {
                            String cwd = System.getProperty("user.dir") + "/";
                            s = FILE_PROTOCOL + cwd + s;
                        }
                    }
                    URL localDmrpp = new URL(s);
                    aggFileList.add(localDmrpp);
                }
            }
        }
        else if(args.length==1){
            File arg0 = new File(args[0]);
            if(!arg0.exists())
                throw new BadConfigurationException("Unable to locate file: "+arg0);

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
                            throw new IOException("Unable to read aggFile: " + aggFile);
                        }
                        aggFileURL = aggFile.toURI().toURL();
                    }
                    log.debug("Adding aggFile: {}", aggFileURL);
                    aggFileList.add(aggFileURL);
                }
            }
            else {
                throw new BadConfigurationException("The named file is neither a file or a dir: "+arg0);
            }
        }
        else {
            throw new BadConfigurationException("This business won't work if you don't provide some dmr++ files my friend.");
        }
    }

    /**
     *
     * @return
     * @throws BadConfigurationException
     * @throws IOException
     * @throws JDOMException
     */
    public Document aggregate() throws BadConfigurationException, IOException, JDOMException {

        if(aggFileList.isEmpty())
            throw new BadConfigurationException("No files were specified, unable to perform aggregation.");


        URL templateDmrppUrl = aggFileList.remove(0);
        Document aggDataset = Util.getDocument(templateDmrppUrl);
        ingestTemplateDataset(aggDataset);

        int chunkIndex = 1;
        for(URL url:aggFileList){
            aggDataset = Util.getDocument(url);
            ingestAggDataset(aggDataset,chunkIndex);
            chunkIndex++;
            if(newAggDimensionElement!=null) {
                newAggDimensionElement.setAttribute(DAP4.SIZE, chunkIndex+"");
            }
        }

        if(log.isDebugEnabled()) {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            for (String aggVarName : aggVarTemplates.keySet()) {
                Element aggVarElement = aggVarTemplates.get(aggVarName);
                log.debug("aggVar:\n{}", xmlo.outputString(aggVarElement));
            }
        }
        return aggDatasetTemplate;
    }

    /**
     *
     * @param datasetDoc
     * @param chunkIndex
     * @throws IOException
     */
    public void ingestAggDataset(Document datasetDoc, int chunkIndex) throws IOException {

        Element datasetElement = datasetDoc.getRootElement();
        String s = datasetElement.getAttributeValue(DMRPP.HREF,DMRPP.NS);
        URL dataURL = null;
        if(s!=null)
            dataURL = new URL(s);

        aggContainer(datasetElement, dataURL, chunkIndex);
    }


    /**
     *
     * @param container
     * @param chunkIndex
     * @throws IOException
     */
    private void aggContainer(Element container,  URL dataURL , int chunkIndex ) throws IOException {

        @SuppressWarnings("unchecked")
        List<Element> topKids = container.getChildren();
        for(Element kid:topKids){
            if (kid.getName().equals(DAP4.GROUP)){
                aggContainer(kid, dataURL, chunkIndex);
            }
            else if(kid.getName().equals(DAP4.DIMENSION)){
                String dimName = getFQN(kid);
                Element templateDimension = dimensions.get(dimName);
                if(templateDimension==null){
                    throw new IOException("OUCH! Encountered non templated Dimension declaration for '"+dimName+"'");
                }
                String dSize = kid.getAttributeValue(DAP4.SIZE);
                String tSize = templateDimension.getAttributeValue(DAP4.SIZE);
                if(!dSize.equals(tSize)){
                    throw new IOException("OUCH! Agg dimension ("+dimName+") size ("+dSize+")" +
                            " does not match template dimension size ("+tSize+")");
                }
            }
            else if (kid.getName().equals(DAP4.STRUCTURE)){
                aggContainer(kid, dataURL, chunkIndex);
            }
            else if (kid.getName().equals(DAP4.SEQUENCE)){
                aggContainer(kid, dataURL, chunkIndex);
            }
            else if (kid.getName().equals(DAP4.INT8)   |
                    kid.getName().equals(DAP4.UINT8)   |
                    kid.getName().equals(DAP4.BYTE)    |
                    kid.getName().equals(DAP4.CHAR)    |
                    kid.getName().equals(DAP4.INT16)   |
                    kid.getName().equals(DAP4.UINT16)  |
                    kid.getName().equals(DAP4.INT32)   |
                    kid.getName().equals(DAP4.UINT32)  |
                    kid.getName().equals(DAP4.INT64)   |
                    kid.getName().equals(DAP4.UINT64)  |
                    kid.getName().equals(DAP4.FLOAT32) |
                    kid.getName().equals(DAP4.FLOAT64) |
                    kid.getName().equals(DAP4.STRING)  |
                    kid.getName().equals(DAP4.D_URI)   |
                    kid.getName().equals(DAP4.OPAQUE)
                    ){

                String aggVarName = getFQN(kid);

                if(aggVarName.equals("/time") || !coordinateVars.containsKey(aggVarName)) {

                    Element templateVar = aggVarTemplates.get(aggVarName);

                    if (templateVar == null) {
                        log.warn("Unable to locate aggVarTemplate '{}' SKIPPING.", aggVarName);
                    } else {
                        @SuppressWarnings("unchecked")
                        List<Element> vDims = kid.getChildren(DAP4.DIM, DAP4.NS);
                        @SuppressWarnings("unchecked")
                        List<Element> tDims = templateVar.getChildren(DAP4.DIM, DAP4.NS);
                        if (tDims.size() != vDims.size()) {
                            throw new IOException("The template variable does not have the same number of Dimensions " +
                                    "as the aggregation variable. (name: " + aggVarName + ")");
                        } else {

                            Element vChunksElement = kid.getChild(DMRPP.CHUNKS, DMRPP.NS);
                            if (vChunksElement == null) {
                                throw new IOException("The aggregation variable '" + aggVarName + "' has no chunks!");
                            }

                            Element vChunkDimSizesElement = vChunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);
                            if (vChunkDimSizesElement == null) {
                                throw new IOException("The aggregation variable '" + aggVarName + "' has no dmrpp:" +
                                        DMRPP.CHUNK_DIMENSION_SIZES + " element!");
                            }
                            List<Integer> vChunkDimSizes = parseChunkDimensionSizes(vChunkDimSizesElement);

                            Element tChunksElement = templateVar.getChild(DMRPP.CHUNKS, DMRPP.NS);
                            if (tChunksElement == null) {
                                throw new IOException("The template variable '" + aggVarName + "' has no chunks!");
                            }
                            Element tChunkDimSizesElement = tChunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);
                            if (tChunkDimSizesElement == null) {
                                throw new IOException("The template variable '" + aggVarName + "' has no dmrpp:" +
                                        DMRPP.CHUNK_DIMENSION_SIZES + " element!");
                            }
                            List<Integer> tChunkDimSizes = parseChunkDimensionSizes(tChunkDimSizesElement);

                            String msg = "The template variable chunk dimension sizes (" +
                                    tChunkDimSizesElement.getTextTrim() + ") are not compatible " +
                                    "with the aggregation variable chunk dimension sizes (" +
                                    vChunkDimSizesElement.getTextTrim() + ") (name: " + aggVarName + ")";

                            if (vChunkDimSizes.size() != tChunkDimSizes.size()) {
                                throw new IOException(msg);
                            }
                            int vSize, tSize;
                            for (int i = 0; i < tChunkDimSizes.size(); i++) {
                                vSize = vChunkDimSizes.get(i);
                                tSize = tChunkDimSizes.get(i);
                                if (vSize != tSize) {
                                    throw new IOException(msg);
                                }
                            }

                            // Add the new dimension to the CHUNK_POSITION_IN_ARRAY
                            @SuppressWarnings("unchecked")
                            List<Element> vChunkElements = vChunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);

                            for (Element vChunkElement : vChunkElements) {
                                String chunkPositionInArray = vChunkElement.getAttributeValue(DMRPP.CHUNK_POSITION_IN_ARRAY);
                                if (!aggVarName.equals("/time")) {
                                    chunkPositionInArray = chunkPositionInArray.replaceFirst("\\[0,", "[" + chunkIndex + ",");
                                }
                                else {
                                    chunkPositionInArray = chunkPositionInArray.replaceFirst("\\[0", "[" + chunkIndex);
                                }
                                Element chunk = (Element) vChunkElement.clone();
                                chunk.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY,chunkPositionInArray);

                                String href = chunk.getAttributeValue(DMRPP.HREF);
                                if(href==null)
                                    chunk.setAttribute(DMRPP.HREF,dataURL.toString());

                                tChunksElement.addContent(chunk);
                            }

                        }
                    }
                }
            }
            else if (kid.getName().equals(DAP4.ENUMERATION)){
                log.warn("Skipping {} name: {}",DAP4.ENUMERATION,kid.getAttributeValue(DAP4.NAME));
            }
            else if (kid.getName().equals(DAP4.ENUM)){
                log.warn("Skipping {} name: {}",DAP4.ENUM,kid.getAttributeValue(DAP4.NAME));
            }

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
    public void pruneAggTree() throws IOException {

        if(aggVarNames.isEmpty())
            return;

        Map<String,Element> dropList = new HashMap<>();
        HashMap<String,List<String>> outerDims = new HashMap<>();


        for(Map.Entry<String,Element> entry: aggVarTemplates.entrySet()){
            log.debug("aggVar - name: {} type: {}",entry.getKey(), entry.getValue().getName());

            String varFQN = entry.getKey();
            Element varElement = entry.getValue();

            Element outerDim = varElement.getChild(DAP4.DIM,DAP4.NS);
            if(outerDim==null)
                throw new IOException("OUCH! The aggVarTemplate variable "+varFQN+" has no "+DAP4.DIM+" elements.");

            String outerDimName = outerDim.getAttributeValue(DAP4.NAME);

            List<String> matchingVars = outerDims.get(outerDimName);
            if(matchingVars==null) {
                matchingVars = new ArrayList<>();
                outerDims.put(outerDimName,matchingVars);
            }
            matchingVars.add(outerDimName);

            if(!aggVarNames.contains(varFQN) && !coordinateVars.containsKey(varFQN)){
                dropList.put(varFQN,varElement);
            }
        }
        for(Map.Entry<String,Element> entry: dropList.entrySet()) {
            String name = entry.getKey();
            Element varElement = entry.getValue();
            aggVarTemplates.remove(name);
            varElement.detach();
        }

    }

    /**
     *
     * @param templateDoc
     * @throws IOException
     */
    public void ingestTemplateDataset(Document templateDoc) throws IOException {

        aggDatasetTemplate = templateDoc;
        Element dataset = templateDoc.getRootElement();

        String s = dataset.getAttributeValue(DMRPP.HREF,DMRPP.NS);
        URL dataURL = null;
        if(s!=null)
            dataURL = new URL(s);
        ingestContainerTemplate(dataset, dataURL);
        locateCoordinates();
        pruneAggTree();

        if(newAggDimensionElement!=null) {
            newAggDimensionElement.setAttribute(DAP4.SIZE, "1");
        }

        if(log.isDebugEnabled()) {
            for (Map.Entry<String, Element> entry : dimensions.entrySet()) {
                log.debug("Dimension: {}", entry.getKey(), entry.getValue().getName());
            }
            for (Map.Entry<String, Element> entry : coordinateVars.entrySet()) {
                log.debug("coordinateVar: {}", entry.getKey());
            }
            for (Map.Entry<String, Element> entry : aggVarTemplates.entrySet()) {
                log.debug("aggVar - name: {} type: {}", entry.getKey(), entry.getValue().getName());
            }
        }

    }

    /**
     *
     */
    public void locateCoordinates(){
        for(Map.Entry<String,Element> entry : dimensions.entrySet()){
            String dimName = getFQN(entry.getValue());

            Element matchVar = aggVarTemplates.get(dimName);

            if(matchVar!=null && !dimName.equals("/time")) {
                // So it's a coordinate variable, we add it to the
                // coordinate variable list.
                coordinateVars.put(dimName,matchVar);

                // And we drop it from the aggregation variable list because
                // we are not going to aggregate coordinates variables.
                aggVarTemplates.remove(dimName);
            }
        }
    }

    /**
     *
     * @param container
     * @throws IOException
     */
    private void ingestContainerTemplate(Element container, URL dataURL ) throws IOException {

        @SuppressWarnings("unchecked")
        List<Element> topKids = container.getChildren();
        for(Element kid:topKids){
            String varFQN = getFQN(kid);

            if (kid.getName().equals(DAP4.GROUP)){
                ingestContainerTemplate(kid,dataURL);
            }
            else if(kid.getName().equals(DAP4.DIMENSION)){
                dimensions.put(varFQN,kid);
            }
            else if (kid.getName().equals(DAP4.STRUCTURE)){
                ingestContainerTemplate(kid,dataURL);
            }
            else if (kid.getName().equals(DAP4.SEQUENCE)){
                ingestContainerTemplate(kid,dataURL);
            }
            else if (kid.getName().equals(DAP4.INT8)   |
                    kid.getName().equals(DAP4.UINT8)   |
                    kid.getName().equals(DAP4.BYTE)    |
                    kid.getName().equals(DAP4.CHAR)    |
                    kid.getName().equals(DAP4.INT16)   |
                    kid.getName().equals(DAP4.UINT16)  |
                    kid.getName().equals(DAP4.INT32)   |
                    kid.getName().equals(DAP4.UINT32)  |
                    kid.getName().equals(DAP4.INT64)   |
                    kid.getName().equals(DAP4.UINT64)  |
                    kid.getName().equals(DAP4.FLOAT32) |
                    kid.getName().equals(DAP4.FLOAT64) |
                    kid.getName().equals(DAP4.STRING)  |
                    kid.getName().equals(DAP4.D_URI)   |
                    kid.getName().equals(DAP4.OPAQUE)
                    ){


                @SuppressWarnings("unchecked")
                List<Element> dims = kid.getChildren(DAP4.DIM,DAP4.NS);
                boolean isArray = dims.size()>0;

                if(isArray){
                    @SuppressWarnings("unchecked")
                    Element chunksElement = kid.getChild(DMRPP.CHUNKS, DMRPP.NS);
                    if (chunksElement != null) {
                        Element tChunkDimSizesElement = chunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS);
                        if (tChunkDimSizesElement == null) {
                            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                            log.error(xmlo.outputString(chunksElement));

                            throw new IOException("The template variable '" + varFQN + "' has no dmrpp:" +
                                    DMRPP.CHUNK_DIMENSION_SIZES + " element!");
                        }

                        // this works to change
                        // if (varFQN.equals("/time"))
                        //    chunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES, DMRPP.NS).setText("2");

                        // And add the new dimension to the CHUNK_POSITION_IN_ARRAY attribute in each chunk.
                        @SuppressWarnings("unchecked")
                        List<Element> chunkElements = chunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);
                        for (Element chunkElement : chunkElements) {
                            String chunkPositionInArray = chunkElement.getAttributeValue(DMRPP.CHUNK_POSITION_IN_ARRAY);
                            chunkElement.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY, chunkPositionInArray.replaceFirst("\\[0,", "[0,"));

                            String href = chunkElement.getAttributeValue(DMRPP.HREF);
                            if (href == null)
                                chunkElement.setAttribute(DMRPP.HREF, dataURL.toString());
                        }

                        aggVarTemplates.put(varFQN, kid);
                    }
                }
            }
            else if (kid.getName().equals(DAP4.ENUMERATION)){
                log.warn("Skipping {} name: {}",DAP4.ENUMERATION,kid.getAttributeValue(DAP4.NAME));
            }
            else if (kid.getName().equals(DAP4.ENUM)){
                log.warn("Skipping {} name: {}",DAP4.ENUM,kid.getAttributeValue(DAP4.NAME));
            }

        }
    }




    /**
     *
     * @param var
     * @return
     */
    public String getFQN(Element var){

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
        Level debugLevel = Level.OFF;
        String aggVarsFile = null;

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
                debugLevel = Level.ALL;
            }

            //---------------------------
            // Output File
            if (cmd.hasOption("o")) {
                outfile = cmd.getOptionValue("o");
            }

            //---------------------------
            // aggregation files
            if (cmd.hasOption("a")) {
                args = cmd.getOptionValues("a");
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

            //----------------------------------------------------------------------
            //----------------------------------------------------------------------

            DmrppJoinExistingAggregator dAgg = new DmrppJoinExistingAggregator(joinNewDimName, aggVarsFile);
            dAgg.log.setLevel(debugLevel);
            dAgg.loadAggFilesList(args[0]);
            dAgg.loadDmrppList(dAgg.getAggFileNames());
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
        options.addOption("d", "debug",   false, "Turn on debug output.");
        options.addOption("o", "output",  true, "File into which the output will be written.");
        options.addOption("a", "aggFile", true, "One or more files to add.");
        options.addOption("n", "dimName", true, "joinNew dimension name");
        options.addOption("v", "variablesFile", true, "A file containing a list of the " +
                "names of the variables to be aggregated.");
        return options;
    }


}
