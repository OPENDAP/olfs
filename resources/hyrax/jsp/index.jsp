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
<html>
<% String contextPath = request.getContextPath(); %>
<head>
    <title>OPeNDAP Hyrax</title>
    <link rel='stylesheet' href='<%= contextPath %>/docs/css/contents.css' type='text/css'/>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
</head>

<body>
<a href="http://www.opendap.org"><img src="<%= contextPath %>/docs/images/logo.png"/></a>

<h1 align="left">Hyrax</h1>
<hr size="1" noshade="noshade"/>
<p>Greetings,</p>

<p>This is the <a href="http://www.opendap.org"><strong>OPeNDAP</strong></a><strong> 4 Data Server</strong>, also known
    as <strong>Hyrax</strong>.<br/>
    <br/>
    Hyrax is a new data server which combines the efforts at UCAR/HAO to build a high performance DAP-compliant data
    server for the Earth System Grid II
    project with existing software developed by OPeNDAP. The server is intended to be a replacement for the existing 3.x
    servers which OPeNDAP is distributing.</p>

<p>The new server uses the Java servlet mechanism to hand off requests from a general web daemon to DAP format-specific
    software. This results in higher performance for small requests. The servlet front end, which we call the
    <strong>O</strong>PeNDAP <strong>L</strong>ightweight <strong>F</strong>ront end <strong>S</strong>erver (OLFS)
    looks at each request and formulates a query to a second server (which may or may not on the same machine as the
    OLFS) called the <strong>B</strong>ack <strong>E</strong>nd <strong>S</strong>erver (BES). The BES is the
    high-performance server software from HAO. It handles reading data from the data stores and returning DAP-compliant
    responses to the OLFS. In turn, the OLFS may pass these response back to the requestor with little or no
    modification or it may use them to build more complex responses. The nature of the Inter Process Communication (IPC)
    between the OLFS and BES is such that they should both be on the same machine or be able to communicate over a very
    high bandwidth channel.</p>

<p> Both the OLFS and the BES require installation and configuration before they can be run. </p>

<h3>Find Your Hyrax Server Here...</h3>

<p>If Tomcat is running and Hyrax is installed you could: </p>
<ul>
    <li>
        <a href="<%= contextPath %>/hyrax/"><strong>Look here for Hyrax default page</strong></a>
        <br/>
        <br/>
    </li>
    <li>
        <a href="<%= contextPath %>/hyrax/contents.html"><strong>Look here for the top level OPeNDAP
        directory </strong></a>
        <br/>
        <br/>
    </li>
    <li>
        <a href="<%= contextPath %>/hyrax/catalog.xml"><strong>Top Level THREDDS catalog.xml is here. </strong></a>
        <br />
        <br />
    </li>
    <li><a href="<%= contextPath %>/admin/index.html"><strong>Hyrax Admin Interface (beta) is here.</strong></a>
        <span class="medium">(Requires SSL support in Tomcat.
        See the <a href="http://docs.opendap.org/index.php/Hyrax_-_Administrators_Interface"> HAI instructions</a>
        for more information.)</span>
    </li>
</ul>

<p>&nbsp;</p>
<h3>Documentation</h3>

<p>OPeNDAP user and developer documentation is now located at the <a href="http://docs.opendap.org">OPeNDAP
    Documentation Site</a></p>

<p>There you will find:</p>
<dl>
    <dt><a href="https://opendap.github.io/hyrax_guide/Master_Hyrax_Guide.html">Hyrax Documentation </a></dt>
    <dd>
        <ul>
            <li><a href="https://opendap.github.io/hyrax_guide/Master_Hyrax_Guide.html#Download_and_Install_Hyrax">Installation instructions</a></li>
            <li><a href="https://opendap.github.io/hyrax_guide/Master_Hyrax_Guide.html#Hyrax_Configuration">Configuration information</a></li>
            <li><a href="http://docs.opendap.org/index.php/Hyrax_-_Logging_Configuration">Logging configuration information</a>
            </li>
            <li><a href="https://opendap.github.io/hyrax_guide/Master_Hyrax_Guide.html#apache-integration">Apache integration instructions <br/>
                <br/>
            </a></li>
            <li>
                Source Code and Release Notes<br/>
                <span class="small_italic">Hyrax is composed of 3 major components each of which has its own
                GitHub project. In each project's GitHub page you can find the source code for the project, releases,
                release notes, and DOIs for the releases.</span>
                <ul>
                    <li><a href="https://github.com/OPENDAP/bes/">BES Project</a><br/></li>
                    <li><a href="https://github.com/OPENDAP/olfs/">OLFS Project</a><br/></li>
                    <li><a href="https://github.com/OPENDAP/libdap4/">Libdap Project</a><br/><br/></li>
                </ul>
            </li>
            <li><a href="https://docs.opendap.org/index.php?title=Developer_Info">Instructions for developers.</a><br/></li>
        </ul>
    </dd>
</dl>

<h3>THREDDS Support </h3>
<p>Hyrax supports THREDDS catalogs, look <a href="https://opendap.github.io/hyrax_guide/Master_Hyrax_Guide.html#THREDDS-config">here
    for configuration details</a>. </p>

<h3>Thanks!</h3>

<p>We hope we hope you find this software useful, and we welcome
    your questions and comments. </p>

<p>To Contact Us:</p>

<p> Technical Support: <a href="mailto:support@opendap.org">support@opendap.org</a></p>

<p>&nbsp;</p>
<p>Hyrax Development: </p>
<blockquote>
    <p>danh &lt;AT&gt; opendap &lt;DOT&gt; org </p>
    <p>jgallagher &lt;AT&gt; opendap &lt;DOT&gt; org </p>
    <p>kneumiller &lt;AT&gt; opendap &lt;DOT&gt; org </p>
    <p>ndp &lt;AT&gt; opendap &lt;DOT&gt; org </p>
    <p>slloyd &lt;AT&gt; opendap &lt;DOT&gt; org </p>
</blockquote>
<p>&nbsp;</p>
<br/>

<h1>Sponsorship</h1>

<p> OPeNDAP Hyrax development is sponsored by:</p>
<blockquote>
    <blockquote>
        <p><a href="http://www.nsf.gov"><img src="<%= contextPath %>/docs/images/nsf-logo.png" alt="NSF" width="95" height="95" border="0"
                                             align="middle" longdesc="http://www.nsf.gov"/></a> <span class="style8"><a
                href="http://www.nsf.gov">National Science Foundation</a></span></p>

        <p><a href="http://www.nasa.gov"><img src="<%= contextPath %>/docs/images/nasa-logo.jpg" alt="NASA" width="97" height="80" border="0"
                                              align="middle" longdesc="http://www.nasa.gov"/><span class="style8">National Aeronautics and Space Administration</span></a>
        </p>

        <p><a href="http://www.noaa.gov"><img src="<%= contextPath %>/docs/images/noaa-logo.jpg" alt="NOAA" width="90" height="90" border="0"
                                              align="middle" longdesc="http://www.noaa.gov"/></a> <span
                class="style8"><a href="http://www.noaa.gov">National Oceanic and Atmospheric Administration</a></span>
        </p>
    </blockquote>
</blockquote>
<hr size="1" noshade="noshade"/>
<h3>&nbsp;</h3>
<blockquote>
    <blockquote>
        <p>&nbsp;</p>

        <p>&nbsp;</p>
    </blockquote>
</blockquote>
</body>
</html>
