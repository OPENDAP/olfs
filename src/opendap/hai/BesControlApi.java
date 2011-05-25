package opendap.hai;

import opendap.bes.BES;
import opendap.bes.BESManager;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.Scrub;
import opendap.ppt.OPeNDAPClient;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/15/11
 * Time: 4:53 PM
 * To change this template use File | Settings | File Templates.
 */
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

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String queryString = request.getQueryString();

        log.debug("queryString: "+queryString);

        HashMap<String,String> kvp = processQuery(queryString);

        String status = processBesCommand(kvp);


        PrintWriter output = response.getWriter();

        output.append(StringEscapeUtils.escapeHtml(status));
        output.flush();



    }



    /**
     *
     * @param query
     * @return
     */
    public HashMap<String, String> processQuery(String query){
        HashMap<String, String> kvp = new HashMap<String, String>();


        if(query!=null){
            for(String kvPair : query.split("&")){
                String[] kv = kvPair.split("=");

                String key=null, value=null;


                switch(kv.length){
                    case 2:
                        key = kv[0];
                        value=kv[1];
                        break;
                    case 1:
                        key=kv[0];
                        break;
                    default:
                        break;

                }

                if(key!=null)
                    kvp.put(key,value);
            }
        }



        return kvp;


    }


    /**
     *
     * @param kvp
     * @return
     */
    public String processBesCommand(HashMap<String, String> kvp) {

        StringBuilder sb = new StringBuilder();

        String besCmd = kvp.get("cmd");
        String currentPrefix = kvp.get("prefix");


        if (currentPrefix!=null &&  besCmd != null) {

            BES bes = BESManager.getBES(currentPrefix);

            if (besCmd.equals("Start")) {
                sb.append(bes.start());
            } else if (besCmd.equals("StopNice")) {
                sb.append(bes.stopNice(3000));
            } else if (besCmd.equals("StopNow")) {
                sb.append(bes.stopNow());

            }else  {
                sb.append("Unrecognized BES command: ").append(Scrub.simpleString(besCmd));
            }
        }
        else {
            sb.append("Waiting for you to do something...");
        }


        return sb.toString();


    }


}
