/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Hyrax" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2011 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
package opendap.hai;

import opendap.bes.BES;
import opendap.bes.BESManager;
import opendap.bes.BesAdminFail;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.Scrub;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BesControlApi extends HttpResponder {



    private Logger log;

    private static String defaultRegex = ".*\\/besctl";








    public void init() {
        log = LoggerFactory.getLogger(getClass());
        log.debug("Initializing BES Controller.");


    }


    public BesControlApi(String sysPath) {
        super(sysPath, null, defaultRegex);
        init();
    }

    public BesControlApi(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        init();
    }

    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        controlApi(request,response,false);


    }

    @Override
    public void respondToHttpPostRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        controlApi(request,response,true);



    }


    private void controlApi(HttpServletRequest request, HttpServletResponse response, boolean isPost) throws IOException {



        StringBuilder sb = new StringBuilder();

        Enumeration headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = (String) headers.nextElement();
            String headerValue = request.getHeader(headerName);
            sb.append("    ").append(headerName).append(" = '").append(headerValue).append("'\n");
        }

        log.debug("\nHTTP HEADERS:\n{}",sb);

        //log.debug("\nBODY:\n{}",getRequestBodyAsString(request));

        HashMap<String,String> kvp = Util.processQuery(request);

        String status = null;
        try {
            status = processBesCommand(kvp, isPost);
        }
        catch (BesAdminFail baf){
            status = baf.getMessage();

        }
        PrintWriter output = response.getWriter();

        //@todo work this out to not escape everything.
        //output.append(StringEscapeUtils.escapeHtml(status));

        //String s = processStatus(status);


        output.append(status);


        output.flush();

    }


    public String processStatus(String status){

        StringBuilder s = new StringBuilder();
        SAXBuilder sb = new SAXBuilder(false);
        ByteArrayInputStream bais = new ByteArrayInputStream(status.getBytes());


        try {
            Document besResponseDoc = sb.build(bais);
            Element besResponse = besResponseDoc.getRootElement();

            String errorResponse = processBesErrors(besResponse);

            if(errorResponse.length()==0){
                Element ok = besResponse.getChild("OK",opendap.namespaces.BES.BES_ADMIN_NS);
                if(ok!=null){
                    s.append("OK");
                }
                else {
                    s.append("ERROR! Unrecognized BES response.");
                }
            }
            else {
                s.append(errorResponse);
            }


        } catch (JDOMException e) {
            s.append("Failed to parse BES response! Msg: ").append(e.getMessage());
            log.error(s.toString());
        } catch (IOException e) {
            s.append("Failed to ingest BES response! Msg: ").append(e.getMessage());
            log.error(s.toString());
        }


        return s.toString();

    }



    public String besLogTailResponse(String logResponse){

        StringBuilder s = new StringBuilder();
        SAXBuilder sb = new SAXBuilder(false);
        ByteArrayInputStream bais = new ByteArrayInputStream(logResponse.getBytes());


        try {
            Document besResponseDoc = sb.build(bais);
            Element besResponse = besResponseDoc.getRootElement();


            String errorResponse = processBesErrors(besResponse);

            if(errorResponse.length()==0){
                Element besLog = besResponse.getChild("BesLog",opendap.namespaces.BES.BES_ADMIN_NS);
                if(besLog!=null){
                    s.append(besLog.getText());
                }
                else {
                    s.append("ERROR! Unrecognized BES response.");
                }
            }
            else {
                s.append(errorResponse);
            }




        } catch (JDOMException e) {
            s.append("Failed to parse BES response! Msg: ").append(e.getMessage());
            log.error(s.toString());
        } catch (IOException e) {
            s.append("Failed to ingest BES response! Msg: ").append(e.getMessage());
            log.error(s.toString());
        }


        return s.toString();
    }


    private String processBesErrors(Element topElem){
        StringBuilder s = new StringBuilder();

        List errors = topElem.getChildren("BESError", opendap.namespaces.BES.BES_ADMIN_NS);

        if(!errors.isEmpty()) {
            for(Object o: errors){
                Element error = (Element) o;
                Element msgElem = error.getChild("Message",opendap.namespaces.BES.BES_ADMIN_NS);
                Element typeElem = error.getChild("Type",opendap.namespaces.BES.BES_ADMIN_NS);

                String msg = "BES ERROR Message Is Missing";
                if(msgElem!=null)
                    msg = msgElem.getTextNormalize();

                String type = "BES ERROR Type Is Missing";
                if(typeElem!=null)
                    type = typeElem.getTextNormalize();


                s.append("Error[").append(type).append("]: ").append(msg).append("\n");
            }
        }


        return s.toString();

    }


    private enum besCmds {
        cmd, prefix,
        Start, StopNice, StopNow,
        getConfig, module, setConfig, CONFIGURATION,
        getLog, lines, getLoggerState, setLoggerState, logger, state, setLoggerStates, enable, disable, on, off
    }




    /**
     *
     * @param kvp
     * @return
     */
    public String processBesCommand(HashMap<String, String> kvp, boolean isPost) throws BesAdminFail {

        StringBuilder sb = new StringBuilder();
        StringBuilder status = new StringBuilder();
        String module, loggerName, loggerState;

        String besCmd = kvp.get("cmd");
        String currentPrefix = kvp.get("prefix");


        if (currentPrefix!=null &&  besCmd != null) {

            BES bes = BESManager.getBES(currentPrefix);

            if(bes!=null){

                switch(besCmds.valueOf(besCmd)){

                    case Start:
                        sb.append(processStatus(bes.start()));
                        break;

                    case StopNice:
                        sb.append(processStatus(bes.stopNice(3000)));
                        break;

                    case StopNow:
                        sb.append(processStatus(bes.stopNow()));
                        break;

                    case getConfig:
                        module = kvp.get(besCmds.module.toString());
                        /*
                        sb.append("You issued a getConfig command");
                        if(module!=null)
                            sb.append(" for module '").append(module).append("'.\n");
                        else
                            sb.append(".\n");
                         */
                        status.append(bes.getConfiguration(module));
                        sb.append(status);
                        break;


                    case setConfig:
                        String submittedConfiguration  = kvp.get(besCmds.CONFIGURATION.toString());
                        if(isPost && submittedConfiguration!=null ){

                            module = kvp.get(besCmds.module.toString());

                            /*
                            sb.append("You issued a setConfig command");
                            if(module!=null)
                                sb.append(" for module '").append(module).append("'.\n");
                            else
                                sb.append(".\n");

                            sb.append("Your Configuration: \n");
                            sb.append(submittedConfiguration);
                             */

                            status.append(bes.setConfiguration(module, submittedConfiguration));
                            sb.append(processStatus(status.toString()));

                        }
                        else {
                            sb.append("In order to use the setConfig command you MUST supply a configuration via HTTP POST content.\n");
                        }
                        break;


                    case getLog:
                        String logLines = kvp.get("lines");
                        String logContent =  bes.getLog(logLines);
                        logContent = besLogTailResponse(logContent);

                        logContent = StringEscapeUtils.escapeXml(logContent);

                        sb.append(logContent);
                        break;


                    case getLoggerState:
                        loggerName = getValidLoggerName(bes, kvp.get(besCmds.logger.toString()));

                        status.append(bes.getLoggerState(loggerName));

                        sb.append(status);
                        break;


                    case setLoggerState:
                        loggerName = getValidLoggerName(bes, kvp.get(besCmds.logger.toString()));

                        if(loggerName != null){
                            loggerState = getValidLoggerState(kvp.get(besCmds.state.toString()));

                            status = new StringBuilder();

                            status.append(bes.setLoggerState(loggerName,loggerState)).append("\n");

                            sb.append(status);

                        }
                        else {
                            sb.append("User requested an unknown logger.");
                        }


                        break;


                    case setLoggerStates:
                        String enabledLoggers = kvp.get(besCmds.enable.toString());

                        String disabledLoggers = kvp.get(besCmds.disable.toString());


                        status = new StringBuilder();
                        for (String enabledLoggerName : enabledLoggers.split(",")) {

                            loggerName = getValidLoggerName(bes, enabledLoggerName);

                            if (loggerName != null)
                                status.append(bes.setLoggerState(loggerName, besCmds.on.toString())).append("\n");
                        }

                        for (String disabledLoggerName : disabledLoggers.split(",")) {

                            loggerName = getValidLoggerName(bes, disabledLoggerName);

                            if (loggerName != null)
                                status.append(bes.setLoggerState(loggerName, besCmds.off.toString())).append("\n");

                        }

                        sb.append(status);
                        break;


                    default:
                        sb.append(" Unrecognized BES command: ").append(Scrub.simpleString(besCmd));
                        break;


                }


            }
            else {
                String cleanPrefix = Scrub.fileName(currentPrefix);
                sb.append("There is no BES available for prefix '").append(cleanPrefix).append("'");
                log.error("OUCH!! The BESManager failed to return a BES for the prefix '{}'. " +
                        "This should never happen! " +
                        "The BES with prefix '/' should always be available and it handle all unmapped prefix " +
                        "values.",cleanPrefix);
            }

        }
        else {

            sb.append(" Waiting for you to do something...");
        }


        return sb.toString();


    }




    private String getValidLoggerName(BES bes, String loggerName) throws BesAdminFail {

        TreeMap<String,BES.BesLogger> validLoggers = bes.getBesLoggers();
        if(validLoggers.containsKey(loggerName)){
            BES.BesLogger besLogger = validLoggers.get(loggerName);
            return besLogger.getName();
        }

        log.debug("User requested unknown BES logger: '{}'", loggerName);

        return null;

    }

    private String getValidLoggerState(String loggerState) throws BesAdminFail {

        if(!loggerState.equals(besCmds.on.toString()))
            loggerState = besCmds.off.toString();

        return besCmds.on.toString();

    }



}
