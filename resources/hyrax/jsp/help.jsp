<!--
/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
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
/////////////////////////////////////////////////////////////////////////////
-->
<%@page session="false" %>
<% String contextPath = request.getContextPath(); %>
<html>
<head>
    <title>OPeNDAP Hyrax</title>
    <link rel='stylesheet' href='<%= contextPath %>/docs/css/contents.css' type='text/css'/>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
</head>

<body>
<a href="http://www.opendap.org"><img src="<%= contextPath %>/docs/images/logo.gif" alt="Logo" width="206" height="93" border="0"/></a>

<h1 align="left">Hyrax Server Help </h1>
<hr size="1" noshade="noshade"/>
<p>To access most of the features of this OPeNDAP server, append one of the following a eleven suffixes to the URL of a dataset: .das,
    .dds, .ddx, .rdf, .dods, .nc, ascii, .xdods, .info, .ver or .help. </p>

<p>Using these suffixes, you can ask this server for:</p>


<table width="50%" border="0">
    <table width="50%" border="0">
    <tr>
        <td width="14%">
            <div align="center">
                <p><em>suffix</em></p>
            </div>
        </td>
        <td width="86%"><p><em>Description</em></p></td>
    </tr>
    </table>


<p><strong>Data Request Form</strong></p>
<table width="50%" border="0">
    <tr>
        <td>
            <div align="center">
                <p><strong>.html</strong></p>
            </div>
        </td>
        <td><p>The html data request form for the specified data set. </p></td>
    </tr>

</table>


<p><strong>Metadata Responses</strong></p>
<table width="50%" border="0">
    <tr>
        <td>
            <div align="center">
                <p><strong>.das</strong></p>
            </div>
        </td>
        <td><p>Dataset Attribute Structure (DAS)</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.dds</strong></p>
            </div>
        </td>
        <td><p>Dataset Descriptor Structure (DDS</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.ddx</strong></p>
            </div>
        </td>
        <td><p>XML version of the DDS/DAS</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.rdf</strong></p>
            </div>
        </td>
        <td><p>An RDF representation of the DDX</p></td>
    </tr>
    <tr></tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.info</strong></p>
            </div>
        </td>
        <td><p>An html document that displays the data set's attributes, types and other information.</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.ver</strong></p>
            </div>
        </td>
        <td><p>An XML document describing the version of the server and its components.</p></td>
    </tr>
</table>


<p><strong>Data Responses</strong></p>
<table width="50%" border="0">

    <tr>
        <td>
            <div align="center">
                <p><strong>.dods</strong></p>
            </div>
        </td>
        <td><p>DAP data object (A constrained DDS populated with data)</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.nc</strong></p>
            </div>
        </td>
        <td><p>A netCDF file containing the DAP data object (dods response).</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.ascii (or .asc)</strong></p>
            </div>
        </td>
        <td><p>A CSV representation of the DAP data object (dods response).</p></td>
    </tr>
    <tr>
        <td>
            <div align="center">
                <p><strong>.xdods</strong></p>
            </div>
        </td>
        <td><p>An XML representation of the DAP data object (dods response). May be size capped.</p></td>
    </tr>
    <tr>
</table>

<p><strong>Help Page</strong></p>
<table width="50%" border="0">


    <tr>
        <td>
            <div align="center">
                <p><strong>help</strong></p>
            </div>
        </td>
        <td><p>help information (this text)</p></td>
    </tr>
</table>
</table>
    
<p>For example, to request the DAS object from the FNOC1 dataset at URI/GSO (a experiments dataset) you would append
    `.das' to the URL: http://test.opendap.org/opendap/data/nc/fnoc1.nc.das.<br/>
</p>

<p><strong>Note</strong>: Many OPeNDAP clients supply these extensions for you so you don't need to append them (for
    example when using interfaces supplied by us or software re-linked with a OPeNDAP client-library). Generally, you
    only need to add these if you are typing a URL directly into a WWW browser.</p>

<p><strong>Note:</strong> If you would like version information for this server but don't know a specific data file or
    data set name, use `/version' for the filename. For example: http://test.opendap.org/opendap/version will
    return the version number for the netCDF server used in the first example.</p>

<p><strong>Suggestion</strong>: If you're typing this URL into a WWW browser and would like information about the
    dataset, use the `.info' extension.</p>

<p>If you'd like to see a data values, use the `.html' extension and submit a query using the customized form.</p>

<p>For more information about the server look <a href="<%= contextPath %>">HERE</a>. </p>

<h3>&nbsp;</h3>
<blockquote>
    <blockquote>
        <p>&nbsp;</p>

        <p>&nbsp;</p>
    </blockquote>
</blockquote>
</body>
</html>
