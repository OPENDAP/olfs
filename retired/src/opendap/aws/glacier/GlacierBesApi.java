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

import opendap.aws.AwsUtil;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.RequestCache;
import opendap.dap.Dap2Error;
import opendap.dap4.Dap4Error;
import opendap.dap4.QueryParameters;
import opendap.ppt.PPTException;
import org.jdom.JDOMException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 9/25/13
 * Time: 12:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierBesApi extends BesApi {



    public String getBesDataSourceID(String relativeUrl, boolean checkWithBes){

        String id = super.getBesDataSourceID(relativeUrl,checkWithBes);

        return AwsUtil.encodeKeyForFileSystemName(id);
    }

    public String getBesDataSourceID(String relativeUrl, Pattern matchPattern, boolean checkWithBes){

        while(relativeUrl.startsWith("/") && relativeUrl.length()>1)
            relativeUrl = relativeUrl.substring(1);


        String id = super.getBesDataSourceID(relativeUrl,matchPattern,checkWithBes);
        if(id!=null)
            id = AwsUtil.encodeKeyForFileSystemName(id);
        return id;


    }


    @Override
    public void writeDDS(String dataSource,
                            String constraintExpression,
                            String xdap_accept,
                            OutputStream os)
            throws BadConfigurationException, BESError, IOException, PPTException {


        dataSource = AwsUtil.decodeFileSystemNameForKey(dataSource);

        GlacierArchive grec;
        try {
            grec = GlacierManager.theManager().getArchiveRecord(dataSource);
        } catch (JDOMException e) {
            throw new IOException("Unable to parse Glacier Record object. msg: "+e.getMessage(), e);
        }

        if(grec!=null){
            String dds = grec.getDDS();

            if(dds!=null){
                os.write(dds.getBytes());
                return;
            }
            else {
                String errMsg = "ERROR: The Glacier Archive Record for resource "+dataSource+" is missing cached DDS metadata.";
                throw new GlacierStateException(errMsg);
            }

        }
        else {
            noSuchResource(dataSource, os);
        }



    }

    @Override
    public void writeDAS(String dataSource, String constraintExpression, String xdap_accept, OutputStream os) throws BadConfigurationException, BESError, IOException, PPTException {


        dataSource = AwsUtil.decodeFileSystemNameForKey(dataSource);

        GlacierArchive grec;
        try {
            grec = GlacierManager.theManager().getArchiveRecord(dataSource);
        } catch (JDOMException e) {
            throw new IOException("Unable to parse Glacier Record object. msg: "+e.getMessage(), e);
        }

        if(grec!=null){
            String das = grec.getDAS();

            if(das!=null){
                os.write(das.getBytes());
            }
            else {
                String errMsg = "ERROR: The Glacier Archive Record for resource "+dataSource+" is missing cached DAS metadata.";
                throw new GlacierStateException(errMsg);
            }

        }
        else {
            noSuchResource(dataSource, os);
        }

    }

    public static final String XML_BASE_TAG = "#XML_BASE#";


    @Override
    public void writeDDX(String dataSource,
                         String constraintExpression,
                         String xdap_accept,
                         String xml_base,
                         OutputStream os)
            throws BadConfigurationException, BESError, IOException, PPTException {


        dataSource = AwsUtil.decodeFileSystemNameForKey(dataSource);

        GlacierArchive grec;
        try {
            grec = GlacierManager.theManager().getArchiveRecord(dataSource);
        } catch (JDOMException e) {
            throw new IOException("Unable to parse Glacier Record object. msg: "+e.getMessage(), e);
        }

        if(grec!=null){
            String ddx = grec.getDDX();

            if(ddx!=null){

                if(ddx.contains(XML_BASE_TAG))
                    ddx = ddx.replace(XML_BASE_TAG,xml_base);
                os.write(ddx.getBytes());
            }
            else {
                String errMsg = "ERROR: The Glacier Archive Record for resource "+dataSource+" is missing cached DDX metadata.";
                sendDap4Error(errMsg,HttpServletResponse.SC_INTERNAL_SERVER_ERROR,os);
            }

        }
        else {
            String errMsg = "ERROR: No such resource:  "+dataSource;
            sendDap4Error(errMsg,HttpServletResponse.SC_NOT_FOUND,os);
        }



    }

    public void sendDap4Error(String msg, int status, OutputStream err) throws IOException {
        Dap4Error error  = new Dap4Error();
        error.setMessage(msg);
        error.setContext("Glacier Service");
        error.setHttpStatusCode(status);
        err.write(error.toString().getBytes());
    }



    @Override
     public void writeDMR(String dataSource, QueryParameters qp,  String xml_base, OutputStream os) throws BadConfigurationException, BESError, IOException, PPTException {


         dataSource = AwsUtil.decodeFileSystemNameForKey(dataSource);

         GlacierArchive grec;
         try {
             grec = GlacierManager.theManager().getArchiveRecord(dataSource);
         } catch (JDOMException e) {
             throw new IOException("Unable to parse Glacier Record object. msg: "+e.getMessage(), e);
         }

         if(grec!=null){
             String ddx = grec.getDDX();

             if(ddx!=null){

                 if(ddx.contains(XML_BASE_TAG))
                     ddx = ddx.replace(XML_BASE_TAG,xml_base);

                 os.write(ddx.getBytes());
             }
             else {
                 String errMsg = "ERROR: The Glacier Archive Record for resource "+dataSource+" is missing cached DMR metadata.";
                 sendDap4Error(errMsg,HttpServletResponse.SC_INTERNAL_SERVER_ERROR,os);
             }

         }
         else {
             String errMsg = "ERROR: No such resource:  "+dataSource;
             sendDap4Error(errMsg,HttpServletResponse.SC_NOT_FOUND,os);
         }

     }


    @Override
    public void  writeDap2DataRequestForm(String dataSource, String xdap_accept, String url, OutputStream os) throws BadConfigurationException, BESError, IOException, PPTException {



        dataSource = AwsUtil.decodeFileSystemNameForKey(dataSource);

        GlacierArchive grec;
        try {
            grec = GlacierManager.theManager().getArchiveRecord(dataSource);
        } catch (JDOMException e) {
            throw new IOException("Unable to parse Glacier Record object. msg: "+e.getMessage(), e);
        }

        if(grec!=null){
            String ddx = grec.getDDX();

            if(ddx!=null){

                os.write("<html><h1>Need an XSLT to make the DDX/DMR into the HTML form.</h1></html>".getBytes());
            }
            else {
                String errMsg = "ERROR: The Glacier Archive Record for resource "+dataSource+" is missing cached DDX metadata.";
                Dap2Error dap2Error = new Dap2Error(Dap2Error.UNDEFINED_ERROR,errMsg);
                dap2Error.print(os);
                // err.write(errMsg.getBytes());
            }

        }
        else {
            noSuchResource(dataSource,os);
        }

    }

    private void noSuchResource(String missingResource, OutputStream err) {
        String errMsg = "ERROR: No such resource: "+missingResource;
        Dap2Error dap2Error = new Dap2Error(Dap2Error.NO_SUCH_FILE,errMsg);
        dap2Error.print(err);
        // err.write(errMsg.getBytes());

    }

}
