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
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 10/2/13
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class BesMetadataExtractor {


    private static boolean inititialized = false;

    private static String _besPrefix;

    public static enum DAP2 {
        DDS, DAS, DDX
    }


    public static void  init(String besPrefix){

        if(inititialized)
            return;

        while(besPrefix.endsWith("/"))
            besPrefix = besPrefix.substring(0,besPrefix.lastIndexOf('/'));

        _besPrefix = besPrefix;

        RequestCache.openThreadCache();

        Element besConfig = DapServlet.getDefaultBesManagerConfig();

        try {
            BESManager besManager = new BESManager();
            besManager.init(null,besConfig);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void destroy() {
        RequestCache.closeThreadCache();

    }




    public static void extractMetadata(GlacierRecord gar, File tmpDir, File datasetFile) throws BadConfigurationException, IOException {





        String besStandAlone = _besPrefix + "/bin/besstandalone";
        String besConfig     = _besPrefix + "/etc/bes/s3Bes.conf";
        File   bescmd        = new File(tmpDir,"bes.cmd");


        Element metadata;


        metadata = new Element("DDS", GlacierRecord.GlacierRecordNameSpace);
        metadata.setAttribute("type","dds");

        gar.addMetaDataElement(metadata);

        StringBuilder sysCmd = new StringBuilder();

        String result;

        mkCommand(bescmd,datasetFile,DAP2.DDS);
        sysCmd.append(besStandAlone)
                .append(" -c ").append(besConfig)
                .append(" -i ").append(bescmd.getAbsolutePath())
                ;//.append(" -f ").append(metadataResult);

        result = runSysCommand(sysCmd.toString());

        metadata.setText(result);


        metadata = new Element("DAS", GlacierRecord.GlacierRecordNameSpace);
        metadata.setAttribute("type","das");
        gar.addMetaDataElement(metadata);

        sysCmd = new StringBuilder();

        mkCommand(bescmd,datasetFile,DAP2.DAS);
        sysCmd.append(besStandAlone)
                .append(" -c ").append(besConfig)
                .append(" -i ").append(bescmd.getAbsolutePath())
                ;//.append(" -f ").append(metadataResult);

        result = runSysCommand(sysCmd.toString());
        metadata.setText(result);


        metadata = new Element("DDX", GlacierRecord.GlacierRecordNameSpace);
        metadata.setAttribute("type","ddx");
        gar.addMetaDataElement(metadata);


        sysCmd = new StringBuilder();

        mkCommand(bescmd, datasetFile, DAP2.DDX);
        sysCmd.append(besStandAlone)
                .append(" -c ").append(besConfig)
                .append(" -i ").append(bescmd.getAbsolutePath())
                ;//.append(" -f ").append(metadataResult);

        result = runSysCommand(sysCmd.toString());

        metadata.setText(result);

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

        if(!cmdFile.exists())
            cmdFile.createNewFile();


        String resourceID  = datasetFile.getName();

        Document bescmd = null;
        switch(responseType){
            case DAS:
                bescmd = besApi.getDASRequest(resourceID, "", "3.2");
                break;

            case DDS:
                bescmd = besApi.getDDSRequest(resourceID, "", "3.2");
                break;

            case DDX:
                bescmd = besApi.getDDXRequest(resourceID, "", "3.2","http://xmlbase");
                break;

            default:
                throw new BadConfigurationException("Unsupported command type.");

        }

        FileOutputStream os = new FileOutputStream(cmdFile);

        xmlo.output(bescmd,os);


    }



    public static void main(String[] args)  {


        File targetDir =  new File("/Users/ndp/scratch/s3Test/foo-s3cmd.nodc.noaa.gov");

        File datasetFile = new File("/Users/ndp/scratch/s3Test/foo-s3cmd.nodc.noaa.gov/#2Fgdr#2Fcycle097#2FJA2_GPN_2PdP097_093_20110222_131017_20110222_140630.nc");

        String besPrefix = "/Users/ndp/hyrax/trunk";

        init(besPrefix);


        try {
            GlacierRecord grec = new GlacierRecord("vname","resourceId","archiveId");

            BesMetadataExtractor.extractMetadata(grec,targetDir, datasetFile);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


            xmlo.output(grec.getArchiveRecordDocument(),System.out);



        } catch (BadConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        destroy();
    }

}
