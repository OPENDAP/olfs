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
<%@ page import="opendap.noaa_s3.S3CatalogManager" %>
<%@ page import="java.util.Enumeration" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% String contextPath = request.getContextPath(); %>
<% String catalogServiceContextPath = S3CatalogManager.theManager().getCatalogServiceContext(); %>

<html>







<head>
    <link rel='stylesheet' href='<%= contextPath %>/docs/css/contents.css' type='text/css'/>
    <title>S3 Bucket List</title>
</head>

<!-- ****************************************************** -->
<!--                      PAGE BANNER                       -->
<!--                                                        -->
<!--                                                        -->

<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.png'/></td>
        <td>
            <div style='font-size:large;font-style: italic'><a href="">Experimental DAP Service</a></div>
        </td>
    </tr>
</table>
<h1>S3 DAP Service</h1>
<div class="medium_italic">Bucket List</div>
<hr size="1" noshade="noshade"/>

<!-- ****************************************************** -->
<!--                      PAGE BODY                         -->
<!--                                                        -->
<!--                                                        -->



<%

    Enumeration<String> e = S3CatalogManager.theManager().getBucketContexts();

    if(e.hasMoreElements()) {
        out.append("<dl>");

        while(e.hasMoreElements()){
            String bucketContext = e.nextElement();
            String bucketName    = S3CatalogManager.theManager().getBucketName(bucketContext);
            out.append("<dd>");
            out.append("<a href=\"").append(catalogServiceContextPath).append(bucketContext).append("/catalog.xml\">").append(bucketName).append("</a>");
            out.append("</dd>");

        }
        out.append("</dl>");
    }
    else {
        out.append("</dl>");
%>
        <strong>No Buckets Found.</strong>

<%

    }



%>








<!-- ****************************************************** -->
<!--                              FOOTER                    -->
<!--                                                        -->
<!--                                                        -->
<hr size="1" noshade="noshade"/>
<table width="100%" border="0">
    <tr>
        <td>
            <div class="small" align="left">
                <a href="<%= contextPath %>/docs/admin">Hyrax Admin Interface </a>
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
