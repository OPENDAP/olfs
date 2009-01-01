<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
<!DOCTYPE stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>



    <xsl:template match="showCatalog">
        <html>
            <xsl:apply-templates />
        </html>
    </xsl:template>



    <xsl:template match="response">
            <head>
                <link rel='stylesheet' href='/opendap/docs/css/contents.css'
                      type='text/css'/>
                <title>OPeNDAP Hyrax: Contents of <xsl:value-of select="dataset/name"/></title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="OPeNDAP Logo" src='/opendap/docs/images/logo.gif'/>
                <h1>Contents of
                    <xsl:if test="dataset/@prefix!='/'" >
                        <xsl:if test="dataset/name='/'" >
                            <xsl:value-of select="dataset/@prefix"/>
                        </xsl:if>
                        <xsl:if test="dataset/name!='/'" >
                            <xsl:value-of select="dataset/@prefix"/><xsl:value-of select="dataset/name"/>
                        </xsl:if>
                    </xsl:if>
                    <xsl:if test="dataset/@prefix='/'" >
                        <xsl:value-of select="dataset/name"/>
                    </xsl:if>
                </h1>
                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <pre>
                    <table border="0" width="100%">
                        <tr>
                            <th align="left">Name</th>
                            <th align="center">Last Modified</th>
                            <th align="center">Size</th>
                            <th align="center">Response Links</th>
                        </tr>
                        <tr>
                            <td>
                                <xsl:if test="/dataset/name!='/'" >
                                    <a href="..">Parent Directory/</a>
                                </xsl:if>
                                <xsl:if test="/dataset/@prefix!='/'" >
                                    <xsl:if test="/dataset/name='/'" >
                                        <a href="..">Parent Directory/</a>
                                    </xsl:if>
                                </xsl:if>
                            </td>
                        </tr>
                        <xsl:for-each select="dataset/dataset">

                            <!-- Process a collection. -->
                            <xsl:if test="@thredds_collection='true'">
                                <tr>
                                    <td align="left">
                                        <a href="{name}/contents.html">
                                        <xsl:value-of select="name"/>/</a>
                                    </td>

                                    <td align="center">
                                        <xsl:value-of
                                                select="lastmodified/date"/>
                                        <xsl:text disable-output-escaping="yes">
                                            &amp;nbsp;
                                        </xsl:text>
                                        <xsl:value-of
                                                select="lastmodified/time"/>
                                    </td>

                                    <td align="right">-</td>
                                    <td align="center">
                                        &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;
                                    </td>
                                </tr>
                            </xsl:if>

                            <!-- Process a data set -->
                            <xsl:if test="@thredds_collection='false'">
                                <tr>
                                    <xsl:if test="@isData='false'">
                                        <td align="left">
                                            <a href="{name}">
                                                <xsl:value-of select="name"/>
                                            </a>
                                        </td>
                                    </xsl:if>
                                    <xsl:if test="@isData='true'">
                                        <td align="left">
                                            <a href="{name}.html">
                                                <xsl:value-of select="name"/>
                                            </a>
                                        </td>
                                    </xsl:if>
                                    <td align="center">
                                        <xsl:value-of
                                                select="lastmodified/date"/>
                                        <xsl:text disable-output-escaping="yes">
                                            &amp;nbsp;
                                        </xsl:text>
                                        <xsl:value-of
                                                select="lastmodified/time"/>
                                    </td>

                                    <td align="right">
                                        <xsl:value-of select="size"/>
                                    </td>
                                    <td align="center">
                                        <xsl:if test="@isData='true'">
                                            <a href="{name}.ddx">ddx</a>
                                            <a href="{name}.dds">dds</a>
                                            <a href="{name}.das">das</a>
                                            <a href="{name}.info">info</a>
                                            <a href="{name}.html">html</a>
                                        </xsl:if>
                                        <xsl:if test="@isData='false'">
                                            &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;
                                        </xsl:if>
                                    </td>
                                </tr>
                            </xsl:if>


                        </xsl:for-each>
                    </table>
                </pre>
                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <div class="small" align="left">
                                THREDDS Catalog
                                <a href="/opendap{/showCatalog/response/dataset/name[.!='/']}/catalog.xml">
                                    XML
                                </a>
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
                <h3>OPeNDAP Hyrax (###HYRAX_VERSION###)

                    <xsl:if test="/dataset/name='/'">
                        <xsl:text disable-output-escaping="yes">&amp;nbsp;
                        </xsl:text>
                        <span class="uuid">
                            ServerUUID=e93c3d09-a5d9-49a0-a912-a0ca16430b91-contents
                        </span>
                    </xsl:if>

                    <br/>
                    <a href='/opendap/docs/'>Documentation</a>
                </h3>

            </body>
    </xsl:template>


</xsl:stylesheet>