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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
-->
<%@ page import="opendap.bes.BES" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="opendap.hai.Util" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.TreeMap" %>
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



    String currentBesTask = kvp.get("task");
    if (currentBesTask == null)
        currentBesTask = "";


    BES bes = BESManager.getBES(currentPrefix);

    currentPrefix = bes.getPrefix();

    String besCtlApi = contextPath + "/hai/besctl";


    StringBuffer status = new StringBuffer();
    status.append(" OK ");


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
<div style='float: right;vertical-align:middle;font-size:small;'><a style="color: green;" href="logout.jsp">logout</a></div>
<div style="clear: both;"> </div>
<h1>Bes Logging Configuration</h1>

<div id="besLoggingConfig" class="content">


            <div class="small">

                <div style=" margin-left: 40px; margin-right: 5px;">
                    <form name="loggerSelect" action="<%=besCtlApi%>?prefix=<%=currentPrefix%>&cmd=setLoggers"
                          method="get">

                    <%
                        TreeMap<String, BES.BesLogger> besLoggers = null;
                        try {
                            besLoggers = bes.getBesLoggers();
                            for (BES.BesLogger logger : besLoggers.values()) {
                                out.append("<input type='checkbox' name='lSelection' value='").append(logger.getName()).append("'");

                                if (logger.getIsEnabled()) {
                                    out.append(" checked='checked' ");
                                }
                                out.append(" />").append(logger.getName()).append("<br/>\n");

                            }
                        } catch (BesAdminFail besAdminFail) {

                            out.append("<strong>").append(besAdminFail.getMessage()).append("</strong>");
                            status = new StringBuffer();
                            status.append(besAdminFail.getMessage());
                            //besAdminFail.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }

                    %>

                    </form>
                </div>
            </div>

        <button onclick="commitBesLoggingChanges('<%=besCtlApi%>','<%=bes.getPrefix()%>',document.loggerSelect.lSelection);">Commit</button>

        <button onclick="self.close()">Cancel</button>


</div>


<br/>
<div class="tiny_black_fw"><strong>Status</strong>

    <div id='status' class='statusDisplay'>
        <pre><%=status%></pre>
    </div>

</div>


</body>
</html>