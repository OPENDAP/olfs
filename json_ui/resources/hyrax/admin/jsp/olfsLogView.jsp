<!--
~ /////////////////////////////////////////////////////////////////////////////
~ // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
~ //
~ //
~ // Copyright (c) $year OPeNDAP, Inc.
~ // Author: Nathan David Potter <ndp@opendap.org>
~ //
~ // This library is free software; you can redistribute it and/or
~ // modify it under the terms of the GNU Lesser General Public
~ // License as published by the Free Software Foundation; either
~ // version 2.1 of the License, or (at your option) any later version.
~ //
~ // This library is distributed in the hope that it will be useful,
~ // but WITHOUT ANY WARRANTY; without even the implied warranty of
~ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
~ // Lesser General Public License for more details.
~ //
~ // You should have received a copy of the GNU Lesser General Public
~ // License along with this library; if not, write to the Free Software
~ // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
~ //
~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
~ /////////////////////////////////////////////////////////////////////////////
-->
<%@ page import="ch.qos.logback.classic.LoggerContext" %>
<%@ page import="opendap.bes.BES" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="opendap.hai.Util" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="java.util.HashMap" %>
<html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%

    String contextPath = request.getContextPath();

    HashMap<String, String> kvp = Util.processQuery(request);


    String currentPrefix = kvp.get("prefix");
    if (currentPrefix == null)
        currentPrefix = "/";


    BES bes = BESManager.getBES(currentPrefix);

    currentPrefix = bes.getPrefix();

    String olfsCtlApi = contextPath+"/hai/olfsctl";


    StringBuilder status = new StringBuilder();
    status.append(" OK ");



%>
<head>
    <link rel='stylesheet' href='<%=contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%=contextPath%>/docs/css/besctl.css' type='text/css'/>
    <script type="text/javascript" src="../js/XmlHttpRequest.js"></script>
    <script type="text/javascript" src="../js/olfsctl.js"></script>
    <title>OLFS Log Viewer</title>
</head>
<body>

<div style='float: right;vertical-align:middle;font-size:small;'><a style="color: green;" href="logout.jsp">logout</a></div>
<div style="clear: both;"> </div>

<!-- ****************************************************** -->
<!--                      PAGE BANNER                       -->
<!--                                                        -->
<!--                                                        -->
<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%=contextPath%>/docs/images/logo.gif'/></td>
        <td>
            <div style='v-align:center;font-size:large;'><a href="<%= contextPath%>/admin/index.html">Hyrax Admin Interface</a></div>
        </td>
    </tr>
</table>
<h1>OLFS Log Viewer</h1>
<hr size="1" noshade="noshade"/>

<!-- ****************************************************** -->
<!--                      PAGE BODY                         -->
<!--                                                        -->
<!--                                                        -->



<div id="olfsLoggingControls" class="loggingControls">
    <div class="small">
        <div style="float: left;">
            <button onclick="startOlfsLogTailing('<%=olfsCtlApi%>');">Start</button>
            <button onclick="stopOlfsLogTailing();">Stop</button>
            <button onclick="clearOlfsLogWindow();">Clear</button>
            &nbsp;&nbsp;Lines To Show:
            <select id="olfsLogLines">
                <option>10</option>
                <option>50</option>
                <option>100</option>
                <option selected="">500</option>
                <option>1000</option>
                <option>5000</option>
                <option>all</option>
            </select>
        </div>

        <div style="float: right;">

            <button onclick="setOlfsLogLevel('<%=olfsCtlApi%>');">Set Log Level</button>
            <select id="olfsLoggerName" onchange="updateOlfsLogLevelSelection('<%=olfsCtlApi%>')">
            <%
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                for (ch.qos.logback.classic.Logger logger : lc.getLoggerList()) {
                    out.append("<option>").append(logger.getName()).append("</option>");
                }
            %>
            </select>

            <select id="olfsLoggerLevel">
                <option>all</option>
                <option>debug</option>
                <option>info</option>
                <option>warn</option>
                <option selected="">error</option>
                <option>off</option>
            </select>
        </div>

        <div style="clear: both;"> </div>

    </div>
</div>


<div id="olfsLogDisplay" class="LogWindow"></div>

<div id="status" class="statusDisplay">
    This is the OLFS Log Viewer. To begin viewing the OLFS log, click the Start button.
</div>




<!-- ****************************************************** -->
<!--                              FOOTER                    -->
<!--                                                        -->
<!--                                                        -->
<hr size="1" noshade="noshade"/>
<table width="100%" border="0">
    <tr>
        <td>
            <div class="small" align="left">
                <a href="<%=contextPath%>/docs/admin">Hyrax Admin Interface</a>
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

<!-- ****************************************************** -->
<!--         HERE IS THE HYRAX VERSION NUMBER               -->
<!--                                                        -->
<h3>OPeNDAP Hyrax
    <br/>
    <a href='<%=contextPath%>/docs'>Documentation</a>
</h3>


</body>
</html>