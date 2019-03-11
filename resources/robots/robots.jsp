<%@ page import="opendap.bes.BESSiteMap" %>
<%@ page import="opendap.dap.Request" %>
<%@ page import="opendap.PathBuilder" %>
<%@ page import="opendap.bes.BESError" %>
<%@ page import="opendap.bes.BadConfigurationException" %>
<%@ page import="opendap.ppt.PPTException" %>
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
    String dapServiceContext = "opendap/";
    Request req = new Request(null,request);
    String servicePrefix = req.getWebApplicationUrl();
    String siteMapServicePrefix = PathBuilder.pathConcat(servicePrefix,"siteMap");
    String webapp = req.getWebApplicationUrl();

    String dapService;
    if(getServletConfig().getServletContext().getContextPath().isEmpty()){
        // If we are running in the ROOT context (no contextPath) then we make the assumption that the DAP
        // service is located at the _dapServiceContext as set in the configuration parameter DapServiceContext.
        dapService = PathBuilder.pathConcat(webapp,dapServiceContext);
    }
    else {
        dapService = webapp;
    }

    BESSiteMap besSiteMap;
    try {
        besSiteMap = new BESSiteMap(dapService);

    }
    catch (BESError | BadConfigurationException | PPTException e) {
        e.printStackTrace();
        return;
    }
%>
<%=besSiteMap.getSiteMapEntryForRobotsDotText(siteMapServicePrefix)%>
