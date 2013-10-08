<%@ page import="opendap.aws.glacier.GlacierArchiveManager" %>
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

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% String contextPath = request.getContextPath(); %>
<% String catalogContext = GlacierArchiveManager.theManager().getCatalogServiceContext(); %>

<html>
<head>
    <link rel='stylesheet' href='<%= contextPath %>/docs/css/contents.css' type='text/css'/>
    <title>Glacier Vaults</title>
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
            <div style='font-size:large;'><a href="">Glacier Vaults</a></div>
        </td>
    </tr>
</table>
<h1>Glacier Vaults</h1>
<hr size="1" noshade="noshade"/>

<!-- ****************************************************** -->
<!--                      PAGE BODY                         -->
<!--                                                        -->
<!--                                                        -->

     <dl>


<%  for(String vault: GlacierArchiveManager.theManager().getVaultNames()){   %>
        <dd><a href="<%=catalogContext%>/<%=vault%>/catalog.xml"><%=vault%></a></dd>
<%  } %>

     </dl>



<!-- ****************************************************** -->
<!--                              FOOTER                    -->
<!--                                                        -->
<!--                                                        -->
<hr size="1" noshade="noshade"/>
<table width="100%" border="0">
    <tr>
        <td>
            <div class="small" align="left">
                Glacier Vaults
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
    <a href='<%= contextPath %>/docs'>Documentation</a>
</h3>


</body>
</html>
