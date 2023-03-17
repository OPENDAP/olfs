<%@ page import="opendap.bes.dap2Responders.BesApi" %>
<%@ page import="opendap.coreServlet.ReqInfo" %>
<%@ page import="opendap.coreServlet.OPeNDAPException" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="opendap.bes.BadConfigurationException" %>
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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page session="false" %>
<%
    int status = 400;
    String title = "Hyrax - Bad Request ("+status+")";

    String contextPath = request.getContextPath();
    String localUrl = ReqInfo.getLocalUrl(request);
    BesApi besApi = new BesApi();
    String supportEmail;
    try {
        supportEmail = besApi.getSupportEmail(localUrl);
    } catch (BadConfigurationException e) {
        supportEmail=null;
    }

    String message = OPeNDAPException.getAndClearCachedErrorMessage();
    String mailtoHrefAttributeValue = OPeNDAPException.getSupportMailtoLink(request,400,message,supportEmail);

%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <link rel='stylesheet' href='<%= contextPath %>/docs/css/contents.css' type='text/css' />
<title><%=title%></title>
</head>

<body>
<p align="left">&nbsp;</p>

<h1 align="center"><%=title%></h1>
<hr align="left" size="1" noshade="noshade"/>
<table width="100%" border="0">
    <tr>
        <td><img src="<%= contextPath %>/docs/images/BadDapRequest.gif" alt="Bad DAP Request" title="Bad DAP Request"
                 width="323" height="350"/></td>
        <td>
            <p align="left">It appears that you have submitted a Bad Request. </p>
            <% if (message != null) { %>
            <p align="left">The specific error message associated with your request was:</p>
            <blockquote> <p><strong><pre><%= Encode.forHtml(message) %></pre> </strong></p> </blockquote>
            <% } %>

            <p align="left">There may simply be problem with the syntax of your OPeNDAP URL. If you are using server
                side functions in your constraint expression you should double check the syntax of the functions that you are
                attempting to use.</p>

            <p align="left">It may also be that the URL extension did not match any that are known by this server. </p>

            <p align="left">Here is a list of the six URL extensions that are be recognized by all DAP servers:  <br/><br/>
            <span style="margin: 20px;"><strong>dds</strong> - <em>The DAP2 syntactic (structural) metadata response.</em></span><br/>
            <span style="margin: 20px;"><strong>dds</strong> - <em>The DAP2 semantic metadata response.</em></span><br/>
            <span style="margin: 20px;"><strong>dods</strong> - <em>The DAP2 data response.</em></span><br/>
            <span style="margin: 20px;"><strong>info</strong> - <em>The DAP2 HTML dataset information page.</em></span><br/>
            <span style="margin: 20px;"><strong>html</strong> - <em>The DAP2 HTML data request form.</em></span><br/>
            <span style="margin: 20px;"><strong>ascii</strong> - <em>The DAP2 ASCII data response.</em></span><br/>

            </p>
            <p align="left">
                In addition <strong>Hyrax</strong> and other new servers support the following DAP4 URL extensions: <br/><br/>
                <span style="margin: 20px;"><strong>dmr</strong> - <em>The DAP4 dataset metadata response.</em></span><br/>
                <span style="margin: 20px;"><strong>dap</strong> - <em>The DAP4 data response.</em></span><br/>
                <span style="margin: 20px;"><strong>dsr</strong> - <em>The DAP4 dataset services response.</em></span><br/>
                <span style="margin: 20px;"><strong>ddx</strong> - <em>The DAP3.2 dataset metadata response.</em></span><br/>

            </p>
            <% if(supportEmail!=null){ %>
            <p align="left"> If you think that the server is broken (that the URL you submitted should have worked),
                then please contact the OPeNDAP user support coordinator at:
                <a href="<%=mailtoHrefAttributeValue%>"><%= supportEmail %></a>
            </p>
            <% } %>
        </td>
    </tr>
</table>
<hr align="left" size="1" noshade="noshade"/>
<h1 align="center"><%=title%></h1>
</body>
</html>
