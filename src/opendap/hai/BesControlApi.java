package opendap.hai;

import opendap.bes.BES;
import opendap.bes.BESManager;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.Scrub;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

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


        HashMap<String,String> kvp = processQuery(request);

        String status = processBesCommand(kvp, request, isPost);

        PrintWriter output = response.getWriter();

        output.append(StringEscapeUtils.escapeHtml(status));
        output.flush();

    }


    public HashMap<String, String> processQuery(HttpServletRequest request){
        HashMap<String, String> kvp = new HashMap<String, String>();

        StringBuilder sb = new StringBuilder();
        Map<java.lang.String,java.lang.String[]> params = request.getParameterMap();
        for(String name: params.keySet()){
            sb.append(name).append(" = ");
            String[] values = params.get(name);
            if(values.length>1){
                log.warn("Multiple values found for besctl parameter '{}'. Will use the last one found.", name);
            }
            for(String value: values){
                sb.append("'").append(value).append("' ");
                kvp.put(name,value);
            }
            sb.append("\n");
        }

        log.debug("Parameters:\n{}",sb);



        return kvp;


    }


    /**
     *
     * @param kvp
     * @return
     */
    public String processBesCommand(HashMap<String, String> kvp, HttpServletRequest request, boolean isPost) {

        StringBuilder sb = new StringBuilder();

        String besCmd = kvp.get("cmd");
        String currentPrefix = kvp.get("prefix");


        if (currentPrefix!=null &&  besCmd != null) {

            BES bes = BESManager.getBES(currentPrefix);

            if (besCmd.equals("Start")) {
                sb.append(bes.start());
            }
            else if (besCmd.equals("StopNice")) {
                sb.append(bes.stopNice(3000));
            }
            else if (besCmd.equals("StopNow")) {
                sb.append(bes.stopNow());
            }
            else if (besCmd.equals("getConfig")) {
                String module = kvp.get("module");

                sb.append("You issued a getConfig command");
                if(module!=null)
                    sb.append(" for module '").append(module).append("'.\n");
                else
                    sb.append(".\n");

                String status = bes.getConfiguration(module);
                sb.append(status);
            }
            else if (besCmd.equals("setConfig")) {
                String submittedConfiguration  = kvp.get("CONFIGURATION");
                if(isPost && submittedConfiguration!=null ){

                    String module = kvp.get("module");

                    sb.append("You issued a setConfig command");
                    if(module!=null)
                        sb.append(" for module '").append(module).append("'.\n");
                    else
                        sb.append(".\n");

                    sb.append("Your Configuration: \n");
                    sb.append(submittedConfiguration);

                    String status = bes.setConfiguration(module, submittedConfiguration);
                    sb.append(status);

                }
                else {
                    sb.append("In order to use the setConfig command you MUST supply a configuration via HTTP POST content");
                }
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


    private String getRequestBodyAsString(HttpServletRequest req) {


        StringBuilder sb = new StringBuilder();
        try {
            int contentLength = req.getContentLength();
            log.debug("HttpServletRequest.getContentLength(): {}",contentLength);
            BufferedReader buff = req.getReader();
            char[] buf = new char[4 * 1024]; // 4 KB char buffer
            int len;
            while ((len = buff.read(buf, 0, buf.length)) != -1) {
                sb.append(buf,0,len);
            }
        } catch (IOException e) {
            log.error("Failed to read HTTP request body.");
        }


        return sb.toString();

    }


}
