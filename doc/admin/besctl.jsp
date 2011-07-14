<%@ page import="opendap.bes.BES" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="opendap.bes.BesConfigurationModule" %>
<%@ page import="opendap.hai.Util" %>
<%@ page import="opendap.ppt.OPeNDAPClient" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Vector" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<%

    String contextPath = request.getContextPath();

    HashMap<String, String> kvp = Util.processQuery(request);


    String currentPrefix = kvp.get("prefix");
    if (currentPrefix == null)
        currentPrefix = "/";


    String currentClientId = kvp.get("clientId");

    String currentBesTask = kvp.get("task");
    if (currentBesTask == null)
        currentBesTask = "";


    String currentModuleId = kvp.get("module");

    BES bes = BESManager.getBES(currentPrefix);

    currentPrefix = bes.getPrefix();

    String besCtlApi = contextPath + "/hai/besctl";


    StringBuffer status = new StringBuffer();
    status.append(" OK ");

    /*
    System.out.println("contextPath: "+contextPath);
    System.out.println("currentPrefix: "+currentPrefix);
    System.out.println("currentClientId: "+currentClientId);
    System.out.println("currentBesTask: "+currentBesTask);
    System.out.println("currentModuleId: "+currentModuleId);
    System.out.println("besCtlApi: "+besCtlApi);
    System.out.println("status: "+status);
    */

%>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>BES Controller</title>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/tabs.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/besctl.css' type='text/css'/>
    <script type="text/javascript" src="js/XmlHttpRequest.js"></script>
    <script type="text/javascript" src="js/besctl.js"></script>

</head>

<body>
<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.gif'/></td>
        <td>
            <div style='v-align:center;font-size:large;'><a href=".">Hyrax Admin Interface</a></div>
        </td>
    </tr>
</table>
<h1>BES Management</h1>
<hr size="1" noshade="noshade"/>
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

        }

    %>
</ol>
<div id="besDetail" class="content">
    <div class="small" style="float: left;">

        bes prefix: <strong><%=bes.getPrefix()%></strong><br/>
        hostname: <strong><%=bes.getHost()%>:<%=bes.getPort()%></strong><br/>
        max client connections: <strong><%=bes.getMaxClients()%></strong><br/>
        current client connections: <strong><%=bes.getBesClientCount()%></strong><br/>
    </div>

    <div id="besControls"style="float: right;">


        <button style="border: 0; background-color: transparent;"
                onclick="start('<%=currentPrefix%>','<%=besCtlApi%>');">
            <img alt="Start" src="<%=contextPath%>/docs/images/startButton.png" border='0' height='40px'>
        </button>


        <button style="border: 0; background-color: transparent;"
                onclick="stopNice('<%=currentPrefix%>','<%=besCtlApi%>');">
            <img alt="StopNice" src="<%=contextPath%>/docs/images/stopNiceButton.png" border='0' height='40px'>
        </button>


        <button style="border: 0; background-color: transparent;"
                onclick="stopNow('<%=currentPrefix%>','<%=besCtlApi%>');"
                >
            <img alt="StopNow" src="<%=contextPath%>/docs/images/stopNowButton.png" border='0' height='40px'>
        </button>


    </div>
    <div style="clear: both;"> </div>

</div>

<br/>

<div class='medium'>
    <ol id="besTasks">
        <%
            out.append("    <li ");
            if (currentBesTask.equals("config")) {
                out.append("class=\"current\"");
            }
            out.append(">");


            out.append("<a href=\"?prefix=")
                    .append(currentPrefix)
                    .append("&task=config")
                    .append("\">")
                    .append("Configuration")
                    .append("</a></li>\n");

            out.append("    <li ");
            if (currentBesTask.equals("logging")) {
                out.append("class=\"current\"");
            }
            out.append(">");

            out.append("<a href=\"?prefix=")
                    .append(currentPrefix)
                    .append("&task=logging")
                    .append("\">")
                    .append("Logging")
                    .append("</a></li>\n");

            out.append("    <li ");
            if (currentBesTask.equals("olfsConnections")) {
                out.append("class=\"current\"");
            }
            out.append(">");

            out.append("<a href=\"?prefix=")
                    .append(currentPrefix)
                    .append("&task=olfsConnections")
                    .append("\">")
                    .append("OLFS Connections")
                    .append("</a></li>\n");




        %>

    </ol>

</div>

<div class='medium'>
    <div id="besTaskDetail" class='content'>

        <%
            /**  #####################################################################
             *
             *         OLFS CONNECTIONS PANEL
             *
             *
             *
             */
            if (currentBesTask.equals("olfsConnections")) {

        %>

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
                                .append("&task=")
                                .append(currentBesTask)
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

                        out.append("<div class='small'>This OLFS is holding no open connections to BES '").append(currentPrefix).append("'</div>");

                    } else {
                        out.append("<strong>Select a client to inspect.</strong>");
                    }


                %>

            </div>
        </div>
        <%
            /**  #####################################################################
             *
             *         CONFIG PANEL
             *
             *
             *
             */
        } else if (currentBesTask.equals("config")) {

        %>

        <div class='small'>
            <ol id="modules">
                <%

                    Vector<BesConfigurationModule> configurationModules;
                    BesConfigurationModule currentModule = null;
                    configurationModules = bes.getConfigurationModules();
                    for (BesConfigurationModule module : configurationModules) {

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
                                .append("&task=")
                                .append(currentBesTask)
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

                        out.append("<strong>")
                                .append(currentModule.getName())
                                .append("</strong><br />\n");

                %>
                <form action="<%=besCtlApi%>?prefix=<%=currentPrefix%>&module=<%=currentModule.getName()%>&cmd=setConfig"
                      method="post">
                    <p>
                        <textarea
                                style="
                                font-family:courier;
                                margin-left: 5px;
                                margin-right: 5px;
                                max-width: 99%;
                                width: 99%;
                                "
                                id="CONFIGURATION"
                                name="CONFIGURATION"
                                rows="20"
                                cols="80">
                            <%=currentModule.getConfig()%>
                        </textarea>
                    </p>
                    <input type="reset"/>
                </form>

                <button onclick="setConfig('<%=currentModule.getName()%>','<%=currentPrefix%>','<%=besCtlApi%>');">
                    Save <%=currentModule.getShortName()%> module configuration
                </button>

                <%

                    } else {

                        out.append("<strong>Select a configuration module to configure.</strong>");
                    }


                %>

            </div>
        </div>
        <%

        /**  #####################################################################
         *
         *         LOGGING PANEL
         *
         *
         *
         */
        } else if (currentBesTask.equals("logging")) {
        %>




            <div class="small">

                <div style="float: left;">

                    <button onclick="getBesLog('<%=besCtlApi%>','<%=currentPrefix%>','10');">Start</button>
                    <button onclick="stopTailing();">Stop</button>
                    <button onclick="clearLogWindow();">Clear</button>
                    &nbsp;&nbsp;Lines To Show:
                    <select id="logLines">
                        <option>10</option>
                        <option selected="">50</option>
                        <option>100</option>
                        <option >500</option>
                        <option>1000</option>
                        <option>5000</option>
                        <option>all</option>
                    </select>
                </div>

                <div style="float: right;">

                    <%
                        String logConfigUrl =  "besLogConfig.jsp?prefix="+currentPrefix;
                    %>

                    <button onclick='window.open("<%=logConfigUrl%>" ,"BES Logging Configuration","width=200,height=525")'>Configuration</button>


                <div style="clear: both;"> </div>


            </div>


        <div id="log" class="LogWindow"></div>

        <%
            }
        %>


    </div>

</div>

<br/>


<hr/>
<div class="tiny_black_fw"><strong>Status</strong>

    <div id='status' class='statusDisplay'>
        <pre><%=status%></pre>
    </div>

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