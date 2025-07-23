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
<% String contextPath = request.getContextPath(); %>
<html>
<head>
    <title>OPeNDAP Hyrax</title>
    <link rel='stylesheet' href='<%= contextPath %>/docs/css/contents.css' type='text/css'/>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
</head>

<body>
<a href="https://www.opendap.org"><img class="banner_logo_img" alt="OPeNDAP Logo" src="<%= contextPath %>/docs/images/logo.svg"/></a>

<h1 align="left">Hyrax Server Help </h1>
<hr size="1" noshade="noshade"/>
The Hyrax server supports both the DAP2 and DAP4 data access protocols.
<h2>DAP4 API</h2>
<p>
    The DAP4 API supports data types, such as <code>Int64</code> & <code>Group</code>, that were not addressed in the
    DAP2 specification.</p>
<p>
    The DAP4 request API is made of two primary request interfaces, metadata and data.
</p>
<p>
    DAP4 Metadata Access URLs utilize a primary URL extension suffix of <code>.dmr</code> which, when used alone,
    returns the DAP4 DMR document encoded as XML. Additional suffixes may be added after <code>.dmr</code> to indicate
    that a different encoding, such as RDF (<code>.dmr.rdf</code>) or HTML (<code>.dmr.html</code>) is desired.
    Note that DAP4 also supports HTML 1.1 client/server content negotiation. Which means that when a client application,
    such as a web browser, asks for the <code>.dmr</code> response and also uses the <code>Accept</code> request header to
    indicate that the client will accept anything, but that it would prefer HTML to anything else, then the service will
    send the HTML encoded version of the DMR response. If you want to see the XML encoded DMR in your browser then use
    the <code>.dmr.xml</code> metadata request suffix.
</p>
<p>
    DAP4 Data Access URLs utilize a primary URL extension suffix of <code>.dap</code> which, when used alone, will
    cause the service to return the requested data in the form of the canonical DAP4 data response. Additional suffixes
    may be added after <code>.dap</code> to indicate that a different encoding, such as NetCDF-4 (<code>.dap.nc4</code>)
    or CSV (<code>.dap.csv</code>) is desired.
</p>
<table width="50%" border="0">
    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p><strong>DAP4 Data Request Form</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>.dmr.html</strong></p></div></td>
        <td><p>The html data request form for the specified data set. </p></td>
    </tr>

    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p>&nbsp;</p><p><strong>DAP4 Metadata Responses</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>.dmr</strong></p></div></td>
        <td><p>Dataset Metadata Response (DMR) using HTTP 1.1 client/server content negotiation.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dmr.xml</strong></p></div></td>
        <td><p>Dataset Metadata Response (DMR) encoded as XML</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dmr.html</strong></p></div></td>
        <td><p>Dataset Metadata Response (DMR) encoded as HTML - aka The DAP4 Data Request Form for the dataset.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dmr.rdf</strong></p></div></td>
        <td><p>Dataset Metadata Response (DMR) encoded as RDF</p></td>
    </tr>


    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p>&nbsp;</p><p><strong>DAP4 Data Responses</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>.dap</strong></p></div></td>
        <td><p>Returns the canonical DAP4 Data Response (A constrained DMR followed by the matching serialized binary
            data.)</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dap.nc4</strong></p></div></td>
        <td><p>The DAP4 Data Response encoded as a netCDF-4 file.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dap.nc3</strong></p></div></td>
        <td><p>The DAP4 Data Response encoded as a netCDF-3 file.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dap.csv</strong></p></div></td>
        <td><p>The DAP4 Data Response encoded as a CSV file.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dap.xml</strong></p></div></td>
        <td><p>The DAP4 Data Response encoded as an XML file.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dap.covjson</strong></p></div></td>
        <td><p>The DAP4 Data Response encoded as a CoverageJSON document.</p></td>
    </tr>
</table>
<p>&nbsp;</p>
<h2>DAP2 API</h2>
<p>
    The DAP4 request API is made of two primary request interfaces, metadata and data. There are two primary catagories
    of metadata in DAP2: syntactic and sematic. Syntactic metadata is the metadata that describes the structure and
    types of the dataset variables. This includes things like atomic and "constructor" types and the names one uses to
    refer to the variables in the dataset. The syntactic metadata is returned in a DDS document. The semantic metedata
    is all the metadata that is not part of the syntactic metadata.  The semantic metadata document is called the
    DAS.
</p>

<p>
    To access most of the DAP2 features of this OPeNDAP server, append one of the following a eleven suffixes to the
    URL of a dataset: .das (DAS), .dds (DDS), .ddx, .rdf, .dods (DAP2 Data Response), .nc, ascii, .xdods, .info,
    .ver or .help.
</p>

<p>Using these suffixes, you can ask this server for metadata and data responses for a particular dataset.</p>

<table width="50%" border="0">
    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p><strong>DAP2 Data Request Form</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>.html</strong></p></div></td>
        <td><p>The html data request form for the specified data set. </p></td>
    </tr>

    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p>&nbsp;</p><p><strong>DAP2 Metadata Responses</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>.das</strong></p></div></td>
        <td><p>Dataset Attribute Structure (DAS)</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.dds</strong></p></div></td>
        <td><p>Dataset Descriptor Structure (DDS</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.ddx</strong></p></div></td>
        <td><p>XML version of the DDS/DAS</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.rdf</strong></p></div></td>
        <td><p>An RDF representation of the DDX</p></td>
    </tr>
    <tr></tr>
    <tr>
        <td><div align="center"><p><strong>.info</strong></p></div></td>
        <td><p>An html document that displays the data set's attributes, types and other information.</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.ver</strong></p></div></td>
        <td><p>An XML document describing the version of the server and its components.</p></td>
    </tr>

    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p>&nbsp;</p><p><strong>DAP2 Data Responses</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>.dods</strong></p></div></td>
        <td><p>DAP data object (A constrained DDS populated with data)</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.nc</strong></p></div></td>
        <td><p>A netCDF-3 file containing the DAP data object (dods response).</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.ascii (or .asc)</strong></p></div></td>
        <td><p>A CSV representation of the DAP data object (dods response).</p></td>
    </tr>
    <tr>
        <td><div align="center"><p><strong>.xdods</strong></p></div></td>
        <td><p>An XML representation of the DAP data object (dods response). May be size capped.</p></td>
    </tr>

    <!-- ------------------------------------------------------------------------------ !-->
    <tr><td colspan="2"><p>&nbsp;</p><p><strong>Help Page</strong></p></td></tr>
    <tr>
        <td><div align="center"><p><strong>help</strong></p></div></td>
        <td><p>help information (this text)</p></td>
    </tr>
</table>

<hr/>

<p>For example, to request the DAP2 DAS object from the FNOC1 dataset at URI/GSO (a experiments dataset) you would append
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
