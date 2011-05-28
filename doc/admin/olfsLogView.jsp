<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
  ~ //
  ~ //
  ~ // Copyright (c) $year OPeNDAP, Inc.
  ~ // Author: Nathan David Potter  <ndp@opendap.org>
  ~ //
  ~ // This library is free software; you can redistribute it and/or
  ~ // modify it under the terms of the GNU Lesser General Public
  ~ // License as published by the Free Software Foundation; either
  ~ // version 2.1 of the License, or (at your option) any later version.
  ~ //
  ~ // This library is distributed in the hope that it will be useful,
  ~ // but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  ~ // Lesser General Public License for more details.
  ~ //
  ~ // You should have received a copy of the GNU Lesser General Public
  ~ // License along with this library; if not, write to the Free Software
  ~ // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  ~ //
  ~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
  ~ /////////////////////////////////////////////////////////////////////////////
  -->
<html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% String contextPath = request.getContextPath(); %>
<head>
    <link rel='stylesheet' href='<%=contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%=contextPath%>/docs/css/besctl.css' type='text/css'/>
    <script type="text/javascript" src="js/XmlHttpRequest.js"></script>
    <script type="text/javascript" src="js/logTail.js"></script>
    <title>OLFS Log Viewer</title>
</head>
<body>


<!-- ****************************************************** -->
<!--                      PAGE BANNER                       -->
<!--                                                        -->
<!--                                                        -->
<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.gif'/></td>
        <td>
            <div style='v-align:center;font-size:large;'><a href=".">Hyrax Admin Interface</a></div>
        </td>
    </tr>
</table>
<hr size="1" noshade="noshade"/>

<!-- ****************************************************** -->
<!--                      PAGE BODY                         -->
<!--                                                        -->
<!--                                                        -->


<div id="controls" class="loggingControls">
    <div>
        <button onclick="getLog('<%=contextPath%>/hai/olfsLog','500');">Start</button>
        <button onclick="stopTail();">Stop</button>
        <button onclick="clearLogWindow();">Clear</button>
    </div>
</div>


<div id="resize">
    <div id="log" class="LogWindow" />
</div>
<div id="message" class="statusDisplay">
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
                <a href="<%=contextPath%>/docs/admin/index.html">Hyrax Admin Interface</a>
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
    <a href='<%=contextPath%>/docs/'>Documentation</a>
</h3>


</body>
</html>