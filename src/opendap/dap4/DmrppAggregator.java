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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class DmrppAggregator {

    private Logger log;

    public static final String FILE_PROTOCOL = "file://";

    private ArrayList<URL> aggFileList;
    private Map<String, Element> dimensions;
    private Map<String,Element> coordinateVars;
    private Map<String,Element> aggVarTemplates;
    private Map<String,String> aggVarChunkDimSizes;

    private Document aggDatasetTemplate;
    private String newAggDimensionName;
    private Element newAggDimensionElement;

    /**
     *
     */
    public DmrppAggregator(String dimName){
        log = (Logger) LoggerFactory.getLogger(this.getClass());

        aggFileList = new ArrayList<>();
        dimensions = new TreeMap<>();
        coordinateVars = new TreeMap<>();
        aggVarTemplates = new TreeMap<>();
        aggVarChunkDimSizes = new TreeMap<>();
        aggDatasetTemplate = null;

        if(dimName!=null){

            newAggDimensionElement = new Element(DAP4.DIMENSION,DAP4.NS);
            newAggDimensionElement.setAttribute(DAP4.NAME,dimName);
            newAggDimensionElement.setAttribute(DAP4.SIZE,"0");

            // We know we are aggregating the entire dataset so we want to put
            // the new dimension at the top level. And that means that the FQN will
            // be the dimension name with a leading slash. (But that's not cool
            // for the element name.
            newAggDimensionName = dimName.startsWith("/")?"":"/"+dimName;
        }
    }

    /**
     *
     * @param args
     * @throws MalformedURLException
     * @throws BadConfigurationException
     */
    public void loadDmrppList(String[] args) throws MalformedURLException, BadConfigurationException {

        if(args.length>1) {
            for (String s : args) {
                if (s.startsWith("http://") || s.startsWith("https://")) {
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
        else {
            File aggDir = new File(args[0]);
            if(!aggDir.isDirectory()) {
                throw new BadConfigurationException("A single parameter must be a directory name that contains the files to agg.");
            }
            File[] contents = aggDir.listFiles();
            for(File f:contents){
                aggFileList.add(f.toURI().toURL());
            }
        }
    }

    /**
     *
     * @return
     * @throws BadConfigurationException
     * @throws IOException
     * @throws JDOMException
     */
    public Document agg() throws BadConfigurationException, IOException, JDOMException {

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

        for(String aggVarName:aggVarTemplates.keySet()){
            Element aggVarElement = aggVarTemplates.get(aggVarName);

            // Update Chunk dimension size for this variable.
            aggVarChunkDimSizes.get(aggVarName);
            Element chunks = aggVarElement.getChild(DMRPP.CHUNKS,DMRPP.NS);
            if(chunks==null)
                throw new BadConfigurationException("Variable "+aggVarName+" is missing the dmrpp:chunks element.");
            Element chunkDimSizes = chunks.getChild(DMRPP.CHUNK_DIMENSION_SIZES,DMRPP.NS);
            chunkDimSizes.setText(aggVarChunkDimSizes.get(aggVarName));

            // Now add the Dim reference to the joinNew Dimension.
            if(newAggDimensionElement!=null){
                Element newDim = new Element(DAP4.DIM,DAP4.NS);
                newDim.setAttribute(DAP4.NAME,newAggDimensionName);
                aggVarElement.addContent(0,newDim);
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

                if(!coordinateVars.containsKey(aggVarName)) {

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


                            if (isJoinNew()) {
                                // Add the new dimension to the CHUNK_POSITION_IN_ARRAY
                                @SuppressWarnings("unchecked")
                                List<Element> vChunkElements = vChunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);

                                for (Element vChunkElement : vChunkElements) {
                                    String chunkPositionInArray = vChunkElement.getAttributeValue(DMRPP.CHUNK_POSITION_IN_ARRAY);
                                    chunkPositionInArray = chunkPositionInArray.replaceFirst("\\[", "[" + chunkIndex + ",");
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
     *
     * @param templateDoc
     * @throws IOException
     */
    public void ingestTemplateDataset(Document templateDoc) throws IOException {

        aggDatasetTemplate = templateDoc;
        Element dataset = templateDoc.getRootElement();
        if(newAggDimensionElement!=null) {
            dataset.addContent(0,newAggDimensionElement);
        }

        String s = dataset.getAttributeValue(DMRPP.HREF,DMRPP.NS);
        URL dataURL = null;
        if(s!=null)
            dataURL = new URL(s);
        ingestContainerTemplate(dataset, dataURL);
        locateCoordinates();



        for(Map.Entry<String,Element> entry : dimensions.entrySet()){
            log.debug("Dimension: {}",entry.getKey(), entry.getValue().getName());
        }

        for(Map.Entry<String,Element> entry: coordinateVars.entrySet()){
            log.debug("coordinateVar: {}",entry.getKey());
        }

        for(Map.Entry<String,Element> entry: aggVarTemplates.entrySet()){
            log.debug("aggVar - name: {} type: {}",entry.getKey(), entry.getValue().getName());
        }

        if(newAggDimensionElement!=null) {
            newAggDimensionElement.setAttribute(DAP4.SIZE, "1");
        }

    }

    /**
     *
     */
    public void locateCoordinates(){
        for(Map.Entry<String,Element> entry : dimensions.entrySet()){
            String dimName = getFQN(entry.getValue());

            Element matchVar = aggVarTemplates.get(dimName);

            if(matchVar!=null) {
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
            if (kid.getName().equals(DAP4.GROUP)){
                ingestContainerTemplate(kid,dataURL);
            }
            else if(kid.getName().equals(DAP4.DIMENSION)){
                dimensions.put(getFQN(kid),kid);
            }
            else if (kid.getName().equals(DAP4.STRUCTURE)){
                ingestContainerTemplate(kid,dataURL);
            }
            else if (kid.getName().equals(DAP4.SEQUENCE)){
                ingestContainerTemplate(kid,dataURL);
            }
            else if (kid.getName().equals(DAP4.INT8)   |
                    kid.getName().equals(DAP4.UINT8)   |
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
                    Element chunksElement = kid.getChild(DMRPP.CHUNKS,DMRPP.NS);
                    if(chunksElement != null){
                        String aggVarName = getFQN(kid);
                        Element tChunkDimSizesElement = chunksElement.getChild(DMRPP.CHUNK_DIMENSION_SIZES,DMRPP.NS);
                        if (tChunkDimSizesElement == null) {
                            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                            log.error(xmlo.outputString(chunksElement));

                            throw new IOException("The template variable '" + aggVarName + "' has no dmrpp:"+
                                    DMRPP.CHUNK_DIMENSION_SIZES+" element!");
                        }
                        if(isJoinNew()) {
                            // If it's a joinNew we add the new dimension to the CHUNK_DIMENSION_SIZES
                            aggVarChunkDimSizes.put(aggVarName,"1 "+tChunkDimSizesElement.getTextTrim());

                            // And add the new dimension to the CHUNK_POSITION_IN_ARRAY attribute in each chunk.
                            @SuppressWarnings("unchecked")
                            List<Element> chunkElements = chunksElement.getChildren(DMRPP.CHUNK, DMRPP.NS);
                            for (Element chunkElement : chunkElements) {
                                String chunkPositionInArray = chunkElement.getAttributeValue(DMRPP.CHUNK_POSITION_IN_ARRAY);
                                chunkElement.setAttribute(DMRPP.CHUNK_POSITION_IN_ARRAY,chunkPositionInArray.replaceFirst("\\[", "[0,"));

                                String href = chunkElement.getAttributeValue(DMRPP.HREF);
                                if(href==null)
                                    chunkElement.setAttribute(DMRPP.HREF,dataURL.toString());
                            }
                        }
                        aggVarTemplates.put(aggVarName,kid);
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
     * @return
     */
    public boolean isJoinNew(){
        return true;
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
    public static void main(String[] args) throws Exception {

        String outfile = null;

        Options options = createCmdLineOptions();

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        //---------------------------
        // Command File
        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(120);
            formatter.printHelp( "DmrppAggregator", options );
            return;
        }

        //---------------------------
        // Output File
        if (cmd.hasOption("o")) {
            outfile = cmd.getOptionValue("o");
        }

        if (cmd.hasOption("a")) {
            args = cmd.getOptionValues("a");
        }

        String joinNewDimName = "z";
        if (cmd.hasOption("d")) {
            joinNewDimName = cmd.getOptionValue("d");
        }

        DmrppAggregator dAgg = new DmrppAggregator(joinNewDimName);
        dAgg.log.setLevel(Level.OFF);

        dAgg.loadDmrppList(args);
        Document aggDataset = dAgg.agg();


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        if(outfile!=null){
            try (FileOutputStream fos = new FileOutputStream(outfile)) {
                xmlo.output(aggDataset, fos);
            }
        }
        else {
            System.out.println(xmlo.outputString(aggDataset));
        }
    }

    private static Options createCmdLineOptions(){

        Options options = new Options();

        options.addOption("o", "output",  true, "File into which the output will be written.");
        options.addOption("h", "help",    false, "Print this usage statement.");
        options.addOption("a", "aggFile", true, "One or more files to add.");
        options.addOption("t", "aggType", true, "joinNew | joinExisting");
        options.addOption("d", "dimName", true, "joinNew dimension name");


        return options;

    }


}
