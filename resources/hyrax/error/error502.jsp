<%@ page import="opendap.bes.dap2Responders.BesApi" %>
<%@ page import="opendap.coreServlet.ReqInfo" %>
<%@ page import="opendap.coreServlet.OPeNDAPException" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2013 OPeNDAP, Inc.
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
  ~ // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
  ~ //
  ~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
  ~ /////////////////////////////////////////////////////////////////////////////
  --%>

<%@page session="false" %>
<%
    String contextPath = request.getContextPath();
    String localUrl = ReqInfo.getLocalUrl(request);

    BesApi besApi = new BesApi();
    String supportEmail = besApi.getSupportEmail(localUrl);

    String message = OPeNDAPException.getAndClearCachedErrorMessage();
    message = Encode.forHtml(message);
    String mailtoHrefAttributeValue = OPeNDAPException.getSupportMailtoLink(request,502,message,supportEmail);
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="stylesheet" href="<%= contextPath %>/docs/css/contents.css" type="text/css"/>

    <title>Hyrax: ERROR</title>
    <style type="text/css">
        <!--
        .style1 {
            font-size: 24px;
            font-weight: bold;
        }

        -->
    </style>
</head>


<body>
<p>&nbsp;</p>

<h1 align="center">Hyrax Error - Bad Gateway (502)</h1>
<hr noshade="noshade" size="1"/>
<table border="0" width="100%">
    <tbody>
    <tr>
        <td>
            <img style="width: 320px; height: 426px;"
                 src="<%= contextPath %>/docs/images/BadGateway.png"
                 alt="Error 502 - Bad Gateway"
                 title="Bad Gateway"
                    />
        </td>
        <td><p class="style1" align="center">Oops!</p>

            <p align="left">Drat! I was unable to act as a gateway to the requested remote resource..</p>
            <% if (message != null) { %>
            <p align="left">The specific error message associated with your request was:</p>
            <blockquote><p><strong><%= message %>
            </strong></p></blockquote>
            <% } %>

            <p align="left">If
                you think that the server is broken (that the URL you submitted should
                have worked), then please contact the OPeNDAP user support coordinator
                at: <a href="<%= mailtoHrefAttributeValue %>"><%= supportEmail %>
                </a></p></td>
    </tr>
    </tbody>
</table>
<hr noshade="noshade" size="1"/>
<h1 align="center">Hyrax Error - Bad Gateway (502)</h1>
</body>
</html>