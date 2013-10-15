/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
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

package opendap.aws.glacier;

import opendap.bes.BESManager;
import opendap.bes.BadConfigurationException;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.RequestCache;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 10/2/13
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class BesMetadataExtractor {



    private File _besPrefix;
    private File _besConf;
    private File _tmpDir;

    private String _dataFileNames[];

    private boolean _verbose = false;


    private File _datasetFile;

    public static enum DAP2 {
        DDS, DAS, DDX
    }


    public BesMetadataExtractor(File besPrefix, File besConf ){

        this();

        _besPrefix = besPrefix;
        _besConf = besConf;
        _tmpDir = new File("/tmp");

        _dataFileNames = null;

        Element besConfig = DapServlet.getDefaultBesManagerConfig();
        try {
            BESManager besManager = new BESManager();
            besManager.init(null,besConfig);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    private  BesMetadataExtractor() {

        _besPrefix     = null;
        _besConf       = null;
        _tmpDir        = null;
        _dataFileNames = null;

        _verbose = false;


    }

    public BesMetadataExtractor(String [] args )throws ParseException{

        this();

        processCommandline(args);

        Element besConfig = DapServlet.getDefaultBesManagerConfig();
        try {


            BESManager besManager = new BESManager();
            besManager.init(null,besConfig);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args)  {

        try {

            BesMetadataExtractor bme = new BesMetadataExtractor(args);

            for(String dataFileName : bme._dataFileNames){

                File datasetFile = new File(dataFileName);

                GlacierArchive grec = new GlacierArchive("vaultName",dataFileName,"archiveId");


                bme.extractMetadata(grec, datasetFile);
                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                xmlo.output(grec.getArchiveRecordDocument(),System.out);
            }



        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private boolean processCommandline(String[] args) throws ParseException {

        String s;
        CommandLineParser parser = new PosixParser();

        Options options = new Options();


        options.addOption("h", "help", false, "Help message.");
        options.addOption("v", "verbose", false, "Makes more output...");


        //
        // options.addOption("d", "dataset-file", true, "The dataset file from which to extract metadata.");

        options.addOption("c", "bes-conf", true, "A BES configuration which defines the " +
                "BES.Catalog.catalog.RootDirectory as the " +
                "parent directory of the dataset file.");

        options.addOption("p", "bes-prefix", true, "The location (aka 'prefix') of the BES installation");

        options.addOption("t", "temp-dir", true, "An (existing) directory to which the program can write temporary files.");


        String usage = this.getClass().getSimpleName() + "-d datasetFile -n datasetFile -c besConfigurationFile " +
                "-p besPrefix [-t tempDir] FILE1 [FILE2 FILE3 ...]";


        CommandLine line =   parser.parse(options, args);


        StringBuilder errorMessage = new StringBuilder();

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return false;
        }

        _verbose = line.hasOption("verbose");


        s = line.getOptionValue("bes-prefix");
        if(s==null){
            errorMessage.append("Missing Parameter - You must provide the location (aka 'prefix') of the BES " +
                    "installation with the  --bes-prefix option.\n");
        }
        _besPrefix = new File(s);


        s = line.getOptionValue("bes-conf");
        if(s!=null){
            _besConf = new File(s);
        }
        else {
            _besConf = new File(_besPrefix,"etc/bes/bes.conf");
        }



        s = line.getOptionValue("temp-dir");
        if(s!=null){
            setTmpDir(s);
        }
        else {
            setTmpDir("/tmp");
        }


        _dataFileNames = line.getArgs();

        if(_dataFileNames.length ==0){
            errorMessage.append("Missing Parameter - You must provide (after all of the other command line parameters) " +
                    "one or more datafile names that are specified" +
                    " as their path relative to the BES.Catalog.catalog.RootDirectory \n");

        }




        if(errorMessage.length()!=0){

            System.err.println(errorMessage);

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);

            return false;
        }

        return true;

    }





    public void setTmpDir(String dirName){
        _tmpDir = new File(dirName);
    }




    public Element getDDSGlacierRecordMetadataElement(File datasetFile) throws BadConfigurationException, IOException {

        StringBuilder sysCmd;
        String result;
        File besStandAlone = new File(_besPrefix, "/bin/besstandalone");
        File   bescmd      = new File(_tmpDir,"bes.cmd");

        Element dds = new Element(GlacierArchive.DDS, GlacierArchive.GlacierRecordNameSpace);
        sysCmd = new StringBuilder();

        mkCommand(bescmd,datasetFile,DAP2.DDS);
        sysCmd.append(besStandAlone)
                .append(" -c ").append(_besConf.getAbsolutePath())
                .append(" -i ").append(bescmd.getAbsolutePath())
                ;//.append(" -f ").append(metadataResult);

        result = runSysCommand(sysCmd.toString());
        if(_verbose)
            System.out.println("\n\n"+result);
        dds.setText(result);

        return dds;
    }


    public Element getDASGlacierRecordMetadataElement(File datasetFile) throws BadConfigurationException, IOException {

        StringBuilder sysCmd;
        String result;
        File besStandAlone = new File(_besPrefix, "/bin/besstandalone");
        File   bescmd      = new File(_tmpDir,"bes.cmd");


        Element das = new Element(GlacierArchive.DAS, GlacierArchive.GlacierRecordNameSpace);
        sysCmd = new StringBuilder();

        mkCommand(bescmd,datasetFile,DAP2.DAS);
        sysCmd.append(besStandAlone)
                .append(" -c ").append(_besConf.getAbsolutePath())
                .append(" -i ").append(bescmd.getAbsolutePath())
                ;//.append(" -f ").append(metadataResult);

        result = runSysCommand(sysCmd.toString());
        if(_verbose)
            System.out.println("\n\n"+result);
//        das.setText(result);

        return das;
    }



    public Element getDDXGlacierRecordMetadataElement(File datasetFile) throws BadConfigurationException, IOException {

        StringBuilder sysCmd;
        String result;
        File besStandAlone = new File(_besPrefix, "/bin/besstandalone");
        File   bescmd      = new File(_tmpDir,"bes.cmd");


        Element ddx = new Element(GlacierArchive.DDX, GlacierArchive.GlacierRecordNameSpace);

        sysCmd = new StringBuilder();

        mkCommand(bescmd, datasetFile, DAP2.DDX);
        sysCmd.append(besStandAlone)
                .append(" -c ").append(_besConf.getAbsolutePath())
                .append(" -i ").append(bescmd.getAbsolutePath())
                ;//.append(" -f ").append(metadataResult);

        result = runSysCommand(sysCmd.toString());

        if(_verbose)
            System.out.println("\n\n"+result);
        ddx.setText(result);

        return ddx;
    }


    public  void extractMetadata(GlacierArchive gar, File datasetFile) throws BadConfigurationException, IOException {


        RequestCache.openThreadCache();
        try {

            // -------------------------------------------
            // ------------- Retrieve DDX ----------------
            Element ddx = getDDXGlacierRecordMetadataElement(datasetFile);
            gar.addMetaDataElement(GlacierArchive.DDX,ddx);


            // -------------------------------------------
            // ------------- Retrieve DDS ----------------

            Element dds = getDDSGlacierRecordMetadataElement(datasetFile);
            gar.addMetaDataElement(GlacierArchive.DDS, dds);


            // -------------------------------------------
            // ------------- Retrieve DAS ----------------
            Element das = getDASGlacierRecordMetadataElement(datasetFile);
            gar.addMetaDataElement(GlacierArchive.DAS,das);



            }
        finally {
            RequestCache.closeThreadCache();

        }

    }


    private static String runSysCommand(String sysCmd) throws IOException{

        Process p = null;

        try {
            System.out.println("COMMAND: "+sysCmd);

            Runtime rt = Runtime.getRuntime();
            p = rt.exec(sysCmd.toString());
            InputStream in = p.getInputStream();
            OutputStream out = p.getOutputStream();
            InputStream err = p.getErrorStream();


            CharArrayWriter caw = new CharArrayWriter();

            IOUtils.copy(in,caw);
            IOUtils.copy(err,System.err);

            return caw.toString();
        }
        finally {
            if(p!=null)
                p.destroy();
        }


    }




    private static void mkCommand(File cmdFile, File datasetFile, DAP2 responseType) throws BadConfigurationException, IOException {



        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        BesApi besApi = new BesApi();


        String resourceID  = datasetFile.getPath();

        Document bescmd = null;
        switch(responseType){
            case DAS:
                bescmd = besApi.getDASRequest(resourceID, "", "3.2");
                break;

            case DDS:
                bescmd = besApi.getDDSRequest(resourceID, "", "3.2");
                break;

            case DDX:
                bescmd = besApi.getDDXRequest(resourceID, "", "3.2","#XML_BASE#");
                break;

            default:
                throw new BadConfigurationException("Unsupported command type.");

        }

        FileOutputStream os = new FileOutputStream(cmdFile);

        xmlo.output(bescmd,os);


    }




}
