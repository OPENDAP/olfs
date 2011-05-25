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
    <script type="text/javascript" src="js/ajax.js"></script>
    <script type="text/javascript" src="js/ajax-logtail.js"></script>
    <title>OLFS Log Viewer</title>
</head>
<body>


<style type="text/css">
    .resiziableLogWindow {
        background: rgba(255, 0, 0, 0.03);
        font-size: 8pt;
        border: 1px solid black;
        height: 125px;
        margin-left: 25px;
        margin-right: 25px;
        margin-bottom: 25px;
        overflow: auto;
        resize: vertical;
        min-width: 500px;
        min-height: 75px;
        padding-left: 15px;
        padding-right: 15px;
        padding-top: 10px;
        padding-bottom: 20px;

    }
</style>


<!-- ****************************************************** -->
<!--                      PAGE BANNER                       -->
<!--                                                        -->
<!--                                                        -->

<img alt="OPeNDAP Logo" src='<%=contextPath%>/docs/images/logo.gif'/>

<h1>Hyrax Admin Interface: OLFS Log Viewer</h1>
<hr size="1" noshade="noshade"/>

<!-- ****************************************************** -->
<!--                      PAGE BODY                         -->
<!--                                                        -->
<!--                                                        -->


<div id="controls"
     style="
        border:solid 1px #dddddd;
        margin-left:25px;
        margin-right: 25px;
        font-size:14px;
        font-family:san-serif,tahoma,arial;
        padding-left:15px;
        padding-right:15px;
        padding-top:0px;
        padding-bottom:0px;
        margin-top:25px;
        margin-bottom:10px;
        text-align:left;
        "

        >
    <div>
        <button onclick="getLog('<%=contextPath%>/hai/olfsLog');">Start</button>
        <button onclick="stopTail();">Stop</button>
        <button onclick="clearLogWindow();">Clear</button>
    </div>
</div>


<div id="resize">
    <div id="log"
         class="resiziableLogWindow"
            />
</div>
<div id="message"
     style="
        border:solid 1px #dddddd;
        margin-left:25px;
        margin-right: 25px;
        font-size: 11px;
        font-family:san-serif,tahoma,arial;
        padding-left:15px;
        padding-right:15px;
        padding-top:0px;
        padding-bottom:0px;
        margin-top:25px;
        margin-bottom:10px;
        text-align:left;
        "

        >
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