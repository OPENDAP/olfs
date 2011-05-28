package opendap.hai;

import opendap.bes.BES;
import opendap.bes.BESManager;
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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

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


    private void controlApi(HttpServletRequest request, HttpServletResponse response, boolean isPost)throws Exception {



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

        String status = processBesCommand(kvp, isPost);

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

            s.append(processBesErrors(besResponse));


            Element ok = besResponse.getChild("OK",opendap.namespaces.BES.BES_ADMIN_NS);
            if(ok!=null){
                s.append("OK");
            }
            else {
                s.append("ERROR! Unrecognized BES response.");
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



    public String processLogResponse(String logResponse){

        StringBuilder s = new StringBuilder();
        SAXBuilder sb = new SAXBuilder(false);
        ByteArrayInputStream bais = new ByteArrayInputStream(logResponse.getBytes());


        try {
            Document besResponseDoc = sb.build(bais);
            Element besResponse = besResponseDoc.getRootElement();


            s.append(processBesErrors(besResponse));


            Element besLog = besResponse.getChild("BesLog",opendap.namespaces.BES.BES_ADMIN_NS);
            if(besLog!=null){
                s.append(besLog.getText());
            }
            else {
                s.append("ERROR! Unrecognized BES response.");
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

    /**
     *
     * @param kvp
     * @return
     */
    public String processBesCommand(HashMap<String, String> kvp, boolean isPost) {

        StringBuilder sb = new StringBuilder();

        String besCmd = kvp.get("cmd");
        String currentPrefix = kvp.get("prefix");


        if (currentPrefix!=null &&  besCmd != null) {

            BES bes = BESManager.getBES(currentPrefix);

            if (besCmd.equals("Start")) {
                sb.append(processStatus(bes.start()));
            }
            else if (besCmd.equals("StopNice")) {
                sb.append(processStatus(bes.stopNice(3000)));
            }
            else if (besCmd.equals("StopNow")) {
                sb.append(processStatus(bes.stopNow()));
            }


            else if (besCmd.equals("getConfig")) {
                String module = kvp.get("module");


                /*
                sb.append("You issued a getConfig command");
                if(module!=null)
                    sb.append(" for module '").append(module).append("'.\n");
                else
                    sb.append(".\n");
                 */

                String status = bes.getConfiguration(module);
                sb.append(status);
            }


            else if (besCmd.equals("setConfig")) {
                String submittedConfiguration  = kvp.get("CONFIGURATION");
                if(isPost && submittedConfiguration!=null ){

                    String module = kvp.get("module");

                    /*
                    sb.append("You issued a setConfig command");
                    if(module!=null)
                        sb.append(" for module '").append(module).append("'.\n");
                    else
                        sb.append(".\n");

                    sb.append("Your Configuration: \n");
                    sb.append(submittedConfiguration);
                     */

                    String status = bes.setConfiguration(module, submittedConfiguration);
                    sb.append(processStatus(status));

                }
                else {
                    sb.append("In order to use the setConfig command you MUST supply a configuration via HTTP POST content.\n");
                }
            }
            else if (besCmd.equals("getLog")) {
                String lines = kvp.get("lines");
                String log =  bes.getLog(lines);
                log = processLogResponse(log);

                log = StringEscapeUtils.escapeXml(log);

                sb.append(log);
            }
            else  {
                sb.append(" Unrecognized BES command: ").append(Scrub.simpleString(besCmd));
            }
        }
        else {

            sb.append(" Waiting for you to do something...");
        }


        return sb.toString();


    }



}
