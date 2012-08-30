<!--
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
-->
<%@ page import="opendap.bes.BES" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="opendap.bes.BesConfigurationModule" %>
<%@ page import="opendap.hai.Util" %>
<%@ page import="opendap.ppt.OPeNDAPClient" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Vector" %>
<%@ page import="opendap.bes.BesAdminFail" %>
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
    //System.out.println("bes('"+currentPrefix+"').isAdminConfigured(): "+bes.isAdminPortConfigured());
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
    <script type="text/javascript" src="../js/XmlHttpRequest.js"></script>
    <script type="text/javascript" src="../js/besctl.js"></script>

</head>

<body>
<div style='float: right;vertical-align:middle;font-size:small;'><a style="color: green;" href="logout.jsp">logout</a>
</div>
<div style="clear: both;"></div>

<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.gif'/></td>
        <td>
            <div style='v-align:center;font-size:large;'><a href="<%= contextPath%>/admin/index.html">Hyrax Admin Interface</a></div>
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

<%
    if (!bes.isAdminPortConfigured()) {

        status = new StringBuffer();
        status.append("OLFS CONFIGURATION ERROR!");
%>

<div class="medium_bold"><%=status%></div>
<br/>
<div class="medium">
    The OLFS configuration did not declare a port number for the administration interface
    of the BES associated with prefix '<%= currentPrefix%>'.
    <br/>
    <br/>
    Please:
    <ul>
        <li>Edit the olfs.xml file and make sure that the administration port for
            the BES with prefix '<%= currentPrefix%>' is uncommented and set to the correct value.</li>
        <li>Restart Tomcat.</li>
        <li>Reload this page.</li>
    </ul>
</div>

<%
    } else if (!bes.checkBesAdminConnection()) {
        status = new StringBuffer();
        status.append("There was an error communicating with the BES!");
%>

<div class="medium_bold"><%=status%></div>
<br/>
<div class="medium">
    There are several possible sources of this issue:.
    <br/>
    <br/>
    <ul>
        <li>The OLFS is not configured with the correct BES admin port. Check the OLFS configuration (olfs.xml)</li>
        <li>The BES.DaemonPort for this BES may not be enabled. Check the BES configuration (bes.conf)</li>
        <li>The BES may not be running.</li>
    </ul>
</div>



<%

    }
    else {
%>


<div id="besControls1">


    <button style="border: 0; background-color: transparent;"
            onclick="startBes('<%=currentPrefix%>','<%=besCtlApi%>');">
        <img alt="Start" src="<%=contextPath%>/docs/images/startButton.png" border='0' height='40px'>
    </button>


    <button style="border: 0; background-color: transparent;"
            onclick="stopBesNicely('<%=currentPrefix%>','<%=besCtlApi%>');">
        <img alt="StopNice" src="<%=contextPath%>/docs/images/stopNiceButton.png" border='0' height='40px'>
    </button>


    <button style="border: 0; background-color: transparent;"
            onclick="stopBesNow('<%=currentPrefix%>','<%=besCtlApi%>');"
            >
        <img alt="StopNow" src="<%=contextPath%>/docs/images/stopNowButton.png" border='0' height='40px'>
    </button>


</div>


<div class="small"
     style="
            border: 1px solid rgb(150, 150, 150);
            padding-left: 5px;
            padding-right: 5px;
            padding-top: 5px;
            padding-bottom: 5px;
            width: 300px;">

    <div class="small_bold" style="padding-bottom: 4px;">OLFS Configuration</div>
    bes prefix: <strong><%=bes.getPrefix()%>
</strong><br/>
    hostname: <strong><%=bes.getHost()%>:<%=bes.getPort()%>
</strong><br/>
    max client connections: <strong><%=bes.getMaxClients()%>
</strong><br/>
    current client connections: <strong><%=bes.getBesClientCount()%>
</strong><br/>
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
    <div id="besClientDetail" class='content'>

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

        Vector<BesConfigurationModule> configurationModules =  null;;
        BesConfigurationModule currentModule = null;
        boolean besFail = false;
        try {
            configurationModules = bes.getConfigurationModules();

        } catch (BesAdminFail besAdminFail) {

            out.append("<strong>").append(besAdminFail.getMessage()).append("</strong>");
            besFail = true;

        }

        if(configurationModules != null){
%>

<div class='small'>
    <ol id="modules">
        <%
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
                                background: rgba(255, 0, 0, 0.03);
                                "
                        id="besConfiguration"
                        name="besConfiguration"
                        rows="20"
                        cols="80">
                    <%=currentModule.getConfig()%>
                </textarea>
            </p>
            <input type="reset"/>
        </form>

        <button onclick="setBesConfig('<%=currentModule.getName()%>','<%=currentPrefix%>','<%=besCtlApi%>');">
            Save <%=currentModule.getShortName()%> module configuration
        </button>

        <%

            } else if (!besFail) {

                out.append("<strong>Select a configuration module to configure.</strong>");
            }


        %>

    </div>
</div>
<%
    }
%>

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

        <button onclick="startBesLogTailing('<%=besCtlApi%>','<%=currentPrefix%>');">Start</button>
        <button onclick="stopBesLogTailing();">Stop</button>
        <button onclick="clearBesLogWindow();">Clear</button>
        &nbsp;&nbsp;Lines To Show:
        <select id="logLines">
            <option>10</option>
            <option >50</option>
            <option>100</option>
            <option>500</option>
            <option selected="">1000</option>
            <option>5000</option>
            <option>all</option>
        </select>
    </div>

    <div style="float: right;">

        <%
            String logConfigUrl = "besLogConfig.jsp?prefix=" + currentPrefix;
        %>

        <button onclick='launchBesLoggingConfigurationWindow("<%=logConfigUrl%>" ,"BES Logging Configuration","width=200,height=525")'>
            Configuration
        </button>


        <div style="clear: both;"></div>


    </div>


    <div id="log" class="LogWindow"></div>

    <%
        }
    %>


</div>

</div>


<%
    }
%>


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
    <a href='<%= contextPath%>/docs'>Documentation</a>
</h3>
</body>
</html>