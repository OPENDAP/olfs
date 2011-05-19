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


        showBesctlPage(request, response, kvp);



    }




    /**
     *
     * @param req
     * @param resp
     * @param kvp
     * @throws ServletException
     * @throws IOException
     */
    private void showBesctlPage(HttpServletRequest req, HttpServletResponse resp, HashMap<String,String> kvp)
            throws ServletException, IOException {



        String currentPrefix = kvp.get("prefix");
        String currentClient = kvp.get("clientId");

        if(currentPrefix==null)
            currentPrefix = "/";


        String status = processBesCommand(kvp);


        //log.debug("Sending logging info");
        resp.setContentType("text/html");
        PrintWriter output = resp.getWriter();

        String localRef = req.getContextPath();
        output.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n")
                .append("        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n")
                .append("    <title>BES Control</title>\n")
                .append("    <link rel='stylesheet' href='/opendap/docs/css/contents.css' type='text/css'/>\n")
                .append("    <link rel='stylesheet' href='/opendap/docs/css/tabs.css' type='text/css'/>\n")
                .append("</head>\n")
                .append("<body>");


        output.append("<table width='95%'>");

        output.append("<tr><td><img alt=\"OPeNDAP Logo\" src='/opendap/docs/images/logo.gif'/></td>")
                .append("<td><div style='v-align:center;h-align:center;font-size:large;'>Hyrax Admin Interface</div></td></tr>");

        output.append("</table>");

        output.append("<h1>BES Control</h1>");



        output.append("<ol id=\"toc\">\n");

        Iterator<BES> i = BESManager.getBES();

        while(i.hasNext()) {
            BES bes = i.next();
            String prefix = bes.getPrefix();

            output.append("    <li ");
            if(prefix.equals(currentPrefix))
                output.append("class=\"current\"");
            output.append(">");

            output.append("<a href=\"?prefix=")
                    .append(prefix)
                    .append("\">")
                    .append(prefix)
                    .append("</a></li>\n");
        }
        output.append("</ol>");




        output.append(besctlContent(currentPrefix,currentClient,status));




        output.append("<table width=\"100%\" border=\"0\">\n")
                .append("    <tr>\n")
                .append("        <td>\n")
                .append("            <div class=\"small\" align=\"left\">\n")
                .append("                Hyrax Administration Prototype\n")
                .append("            </div>\n").append("        </td>\n")
                .append("        <td>\n")
                .append("            <div class=\"small\" align=\"right\">\n")
                .append("                Hyrax development sponsored by\n")
                .append("                <a href='http://www.nsf.gov/'>NSF</a>\n")
                .append("                ,\n")
                .append("                <a href='http://www.nasa.gov/'>NASA</a>\n")
                .append("                , and\n")
                .append("                <a href='http://www.noaa.gov/'>NOAA</a>\n")
                .append("            </div>\n").append("        </td>\n")
                .append("    </tr>\n")
                .append("</table>");

        output.append("<h3>OPeNDAP Hyrax\n")
                .append("\n")
                .append("    <br/>\n")
                .append("    <a href='/opendap/docs/'>Documentation</a>\n")
                .append("</h3>");

        output.flush();
        output.close();
    }















    /**
     *
     * @param prefix
     * @param status
     * @return
     */
    public String besctlContent(String prefix, String currentClientId, String status){
        StringBuffer sb = new StringBuffer();

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        BES bes = BESManager.getBES(prefix);

        sb.append("<div class=\"content\">\n")

                .append("<strong>BES</strong><br />")


                .append("prefix: <strong>")
                .append(prefix)
                .append("</strong>")
                .append("<br />\n")
                .append("hostname: <strong>")
                .append(bes.getHost()) .append(":").append(bes.getPort())
                .append("</strong>")
                .append("<br />\n");










        sb.append("max client connections: <strong>")
                .append(bes.getMaxClients())
                .append("</strong><br />\n")
                .append("current client connections: <strong>")
                .append(bes.getBesClientCount())
                .append("</strong><br />\n")
                .append("<br />\n");




        Enumeration<OPeNDAPClient> clients;

        sb.append("<div class='small'>");

        sb.append("<ol id=\"toc\">\n");

        OPeNDAPClient currentClient = null;
        clients = bes.getClients();
        while(clients.hasMoreElements()){
            OPeNDAPClient client = clients.nextElement();

            sb.append("    <li ");
            if(client.getID().equals(currentClientId)){
                sb.append("class=\"current\"");
                currentClient = client;
            }
            sb.append(">");

            sb.append("<a href=\"?prefix=")
                    .append(prefix)
                    .append("&clientId=")
                    .append(client.getID())
                    .append("\">")
                    .append(client.getID())
                    .append("</a></li>\n");
        }
        sb.append("</ol>");

        sb.append("</div>");

        sb.append("<div class='content'><div class='medium'>\n");

        if(currentClient!=null){

            sb.append("client id: <strong>")
                .append(currentClient.getID())
                .append("</strong><br />\n")
                .append("commands executed: <strong>")
                .append(currentClient.getCommandCount())
                .append("</strong><br />\n")
                .append("is running: <strong>")
                .append(currentClient.isRunning())
                .append("</strong><br />\n");

        }
        else {

            sb.append("<strong>Select a client to inspect.</strong>");

        }



        sb.append("</div>");
        sb.append("</div>");





        sb.append("<br />\n");



        sb.append("<a href=\"?prefix=").append(prefix)
                .append("&clientId=").append(currentClientId)
                .append("&cmd=Start")
                .append("\">")
                .append("<img src='/opendap/docs/images/startButton.png' border='0' height='40px' /></a>")

                .append("<a href=\"?prefix=").append(prefix)
                .append("&clientId=").append(currentClientId)
                .append("&cmd=StopNice")
                .append("\">")
                .append("<img src='/opendap/docs/images/stopNiceButton.png' border='0'  height='40px' /></a>")

                .append("<a href=\"?prefix=").append(prefix)
                .append("&clientId=").append(currentClientId)
                .append("&cmd=StopNow")
                .append("\">")
                .append("<img src='/opendap/docs/images/stopNowButton.png'  border='0' height='40px' /></a>");





        sb.append("<div class='status'>")
                .append(StringEscapeUtils.escapeHtml(status))
                .append("</div>");






        sb.append("<hr />");

        try {
            sb.append("<div class='medium'>")
                .append("Version Document")
                .append("</div>");

            sb.append("<div class='small'><pre>")
                .append(StringEscapeUtils.escapeHtml(xmlo.outputString(bes.getVersionDocument())))
                .append("</pre></div>");

        } catch (Exception e) {
            sb.append("<p><strong>Unable to produce BES Version document.</strong></p>")
            .append("<p>Error Message:<p>")
            .append(e.getMessage())
            .append("</p></p>");
        }




        sb.append("</div>");

        return sb.toString();


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

        StringBuffer sb = new StringBuffer();

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
