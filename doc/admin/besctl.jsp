<%@ page import="opendap.coreServlet.ServletUtil" %>
<%@ page import="opendap.bes.BES" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.jdom.output.XMLOutputter" %>
<%@ page import="org.jdom.output.Format" %>
<%@ page import="opendap.ppt.OPeNDAPClient" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="opendap.coreServlet.Scrub" %>
<%@ page import="java.net.URL" %>
<%@ page import="opendap.bes.BesConfigurationModule" %>
<%@ page import="java.util.Vector" %>
<%@ page import="opendap.hai.BesControlApi" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<%

    String contextPath = request.getContextPath();

    HashMap<String, String> kvp = BesControlApi.processQuery(request);


    String currentPrefix = kvp.get("prefix");
    if (currentPrefix == null)
        currentPrefix = "/";


    String currentClientId = kvp.get("clientId");


    String currentModuleId =  kvp.get("module");

    BES bes = BESManager.getBES(currentPrefix);

    currentPrefix = bes.getPrefix();
    
    String besCtlApi = contextPath+"/hai/besctl";
    
    
    
    

    StringBuffer status = new StringBuffer();
    status.append(" Waiting for you to do something...");



%>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>BES Controller</title>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/tabs.css' type='text/css'/>
    <script type="text/javascript" src="js/XmlHttpRequest.js"></script>
    <script type="text/javascript" src="js/besctl.js"></script>

</head>

<body>
<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.gif'/></td>
        <td>
            <div style='v-align:center;font-size:large;'>Hyrax Admin Interface</div>
        </td>
    </tr>
</table>
<h1>BES Control</h1>
<ol id="toc">

    <%

        Iterator<BES> i = BESManager.getBES();

        while (i.hasNext()) {
            BES b = i.next();
            String prefix = b.getPrefix();

            out.append("    <li ");
            if (prefix.equals(currentPrefix))
                out.append("class=\"current\"");
            out.append(">");

            out.append("<a href=\"?prefix=")
                    .append(prefix)
                    .append("\">")
                    .append(prefix)
                    .append("</a></li>\n");



            /*
            out.append("<button ")
                    .append("onclick=\"showBes('").append(currentPrefix).append("')")
                    .append("\">")
                    .append(prefix)
                    .append("</button></li>\n");


             */


        }

    %>
</ol>
<div id="besDetail"class="content">
    bes prefix: <strong><%=bes.getPrefix()%></strong><br/>
    hostname: <strong><%=bes.getHost()%>:<%=bes.getPort()%></strong><br/>


    max client connections: <strong><%=bes.getMaxClients()%></strong><br/>
    current client connections: <strong><%=bes.getBesClientCount()%></strong><br/>

    <br/>

    <div class='small'>
        <ol id="toc2">
            <%

                Enumeration<OPeNDAPClient> clients;
                OPeNDAPClient currentClient = null;
                clients = bes.getClients();
                while (clients.hasMoreElements()) {
                    OPeNDAPClient client = clients.nextElement();

                    out.append("    <li ");
                    if (client.getID().equals(currentClientId)) {
                        out.append("class=\"current\"");
                        currentClient = client;
                    }
                    out.append(">");


                    out.append("<a href=\"?prefix=")
                            .append(currentPrefix)
                            .append("&clientId=")
                            .append(client.getID())
                            .append("\">")
                            .append(client.getID())
                            .append("</a></li>\n");
                }
            %>

        </ol>

    </div>

    <div class='medium'>
        <div id="clientDetail" class='content'>

            <%
                if (currentClient != null) {

                    out.append("client id: <strong>")
                            .append(currentClient.getID())
                            .append("</strong><br />\n")
                            .append("commands executed: <strong>")
                            .append(Integer.toString(currentClient.getCommandCount()))
                            .append("</strong><br />\n")
                            .append("is running: <strong>")
                            .append(currentClient.isRunning() ? "true" : "false")
                            .append("</strong><br />\n");

                } else if (bes.getBesClientCount() == 0) {

                    out.append("<div class='small'>No active connections to BES '").append(currentPrefix).append("'</div>");

                } else {
                    out.append("<strong>Select a client to inspect.</strong>");
                }


            %>

        </div>
    </div>
    <br/>







    <div  id="besControls">


        <button style="border: 0; background-color: transparent;"
                onclick="start('<%=currentPrefix%>','<%=besCtlApi%>');">
            <img alt="Start" src="<%=contextPath%>/docs/images/startButton.png"  border='0' height='40px'>
        </button>


        <button style="border: 0; background-color: transparent;"
                onclick="stopNice('<%=currentPrefix%>','<%=besCtlApi%>');">
            <img alt="StopNice" src="<%=contextPath%>/docs/images/stopNiceButton.png"  border='0' height='40px'>
        </button>


        <button style="border: 0; background-color: transparent;"
                onclick="stopNow('<%=currentPrefix%>','<%=besCtlApi%>');"
                >
            <img alt="StopNow" src="<%=contextPath%>/docs/images/stopNowButton.png"  border='0' height='40px'>
        </button>



    </div>




    <h3>Configuration Management For BES <%=currentPrefix%></h3>

    <div class='small'>
        <ol id="modules">
            <%

                Vector<BesConfigurationModule> configurationModules;
                BesConfigurationModule currentModule = null;
                configurationModules = bes.getConfigurationModules();
                for(BesConfigurationModule module : configurationModules){

                    out.append("    <li ");


                    if (module.getName().equals(currentModuleId)) {
                        out.append("class=\"current\"");
                        currentModule = module;
                    }
                    out.append(">");


                    out.append("<a href=\"?prefix=")
                            .append(currentPrefix)
                            .append("&module=")
                            .append(module.getName())
                            .append("\">")
                            .append(module.getShortName())
                            .append("</a></li>\n");
                }
            %>

        </ol>

    </div>
    <div class='medium'>
        <div id="currentModule" class='content'>

            <%
                if (currentModule != null) {

                    out.append("module name: <strong>")
                            .append(currentModule.getName())
                            .append("</strong><br />\n");

            %>
            <form action="<%=besCtlApi%>?prefix=<%=currentPrefix%>&module=<%=currentModule.getName()%>&cmd=setConfig" method="post">
                <p>
                    <textarea style="font-family:courier;"   id="CONFIGURATION" name="CONFIGURATION" rows="20" cols="80"><%=currentModule.getConfig()%></textarea>
                </p>
                <input type="reset"/>
            </form>

            <button  onclick="setConfig('<%=currentModule.getName()%>','<%=currentPrefix%>','<%=besCtlApi%>');">Save <%=currentModule.getShortName()%> module configuration</button>

            <%

                } else {

                    out.append("<strong>Select a configurationModule to configure.</strong>");
                }


            %>

        </div>
    </div>
    <br/>


    <hr/>
    <div class="tiny_black_fw">Status

    <div id='status' class='status'><pre><%=StringEscapeUtils.escapeHtml(status.toString())%></pre></div>

    </div>


    <!--
    <div class='medium'>Version Document</div>
    <div class='small'>
        <pre>
            <%
            /*
                try {
                    out.append(StringEscapeUtils.escapeHtml(xmlo.outputString(bes.getVersionDocument())));
                } catch (Exception e) {
                    out.append("<p><strong>Unable to produce BES Version document.</strong></p>")
                            .append("<p>Error Message:<p>")
                            .append(e.getMessage())
                            .append("</p></p>");
                }
                */

            %>
        </pre>
    </div>
    -->

</div>

<table width="100%" border="0">
    <tr>
        <td>
            <div class="small" align="left">
                Hyrax Administration Prototype
            </div>
        </td>
        <td>
            <div class="small" align="right">
                Hyrax development sponsored by
                <a href='http://www.nsf.gov/'>NSF</a>
                ,
                <a href='http://www.nasa.gov/'>NASA</a>
                , and
                <a href='http://www.noaa.gov/'>NOAA</a>
            </div>
        </td>
    </tr>
</table>
<h3>OPeNDAP Hyrax

    <br/>
    <a href='<%= contextPath%>/docs/'>Documentation</a>
</h3>
</body>
</html>