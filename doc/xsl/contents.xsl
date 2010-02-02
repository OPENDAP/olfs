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
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
                >
    <xsl:import href="version.xsl"/>
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>



    <xsl:template match="bes:response">
        <html>
            <xsl:apply-templates />
        </html>
    </xsl:template>



    <xsl:template match="bes:showCatalog">
            <head>
                <link rel='stylesheet' href='/opendap/docs/css/contents.css'
                      type='text/css'/>
                <title>OPeNDAP Hyrax: Contents of <xsl:value-of select="bes:dataset/@name"/></title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="OPeNDAP Logo" src='/opendap/docs/images/logo.gif'/>
                <h1>Contents of
                    <xsl:if test="bes:dataset/@prefix!='/'" >
                        <xsl:if test="bes:dataset/@name='/'" >
                            <xsl:value-of select="bes:dataset/@prefix"/>
                        </xsl:if>
                        <xsl:if test="bes:dataset/@name!='/'" >
                            <xsl:value-of select="bes:dataset/@prefix"/><xsl:value-of select="bes:dataset/@name"/>
                        </xsl:if>
                    </xsl:if>
                    <xsl:if test="bes:dataset/@prefix='/'" >
                        <xsl:value-of select="bes:dataset/@name"/>
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
                                <xsl:if test="bes:dataset/@name!='/'" >
                                    <a href="..">Parent Directory/</a>
                                </xsl:if>
                                <xsl:if test="bes:dataset/@prefix!='/'" >
                                    <xsl:if test="bes:dataset/@name='/'" >
                                        <a href="..">Parent Directory/</a>
                                    </xsl:if>
                                </xsl:if>
                            </td>
                        </tr>
                        <xsl:for-each select="bes:dataset/bes:dataset">

                            <!-- Process a collection. -->
                            <xsl:if test="@node='true'">
                                <tr>
                                    <xsl:call-template name="NodeLinks" />
                                </tr>
                            </xsl:if>

                            <!-- Process a data set -->
                            <xsl:if test="@node='false'">
                                <tr>
                                <xsl:choose>
                                    <xsl:when test="bes:serviceRef">
                                        <xsl:apply-templates />
                                    </xsl:when>

                                    <xsl:otherwise>
                                        <xsl:call-template name="FileServiceLinks" />
                                    </xsl:otherwise>
                                </xsl:choose>
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
                                <a href="/opendap{bes:dataset/@name[.!='/']}/catalog.xml">
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
                <h3>OPeNDAP Hyrax (<xsl:value-of select="$HyraxVersion"/>)

                    <xsl:if test="bes:dataset/@name='/'">
                        <span class="uuid">
                            ServerUUID=e93c3d09-a5d9-49a0-a912-a0ca16430b91-contents
                        </span>
                    </xsl:if>

                    <br/>
                    <a href='/opendap/docs/'>Documentation</a>
                </h3>

            </body>
    </xsl:template>


    <xsl:template name="NodeLinks" >
        <td align="left">
            <a href="{@name}/contents.html">
            <xsl:value-of select="@name"/>/</a>
        </td>

        <td align="center">
            <xsl:value-of select="@lastModified"/>
        </td>

        <td align="right">-</td>
        <xsl:call-template name="NoServiceLinks" />
    </xsl:template>




    <xsl:template match="bes:serviceRef" >
        <xsl:choose>
            <xsl:when test=".='dap'">
                <xsl:call-template name="DapServiceLinks" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="UnkownServiceLinks" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    

    <xsl:template name="DapServiceLinks" >
        <td align="left">
            <b><a href="{../@name}.html">
                <xsl:value-of select="../@name"/>
            </a>
            </b>
        </td>

        <td align="center">
            <xsl:value-of select="../@lastModified" />
        </td>

        <td align="right">
            <xsl:value-of select="../@size"/>
        </td>

        <td align="center">
            <!-- <a href="{../@name}.rdf">rdf</a> -->
            <a href="{../@name}.ddx">ddx</a>
            <a href="{../@name}.dds">dds</a>
            <a href="{../@name}.das">das</a>
            <a href="{../@name}.info">info</a>
            <a href="{../@name}.html">html</a>
            <a href="{../@name}.rdf">rdf</a>
        </td>
    </xsl:template>




    <xsl:template name="UnkownServiceLinks" >
        <td align="left">
            <a href="{../@name}">
                <xsl:value-of select="../@name"/>
            </a>
        </td>
        <td align="center">
            <xsl:value-of
                    select="../@lastmodified" />
        </td>

        <td align="right">
            <xsl:value-of select="../@size"/>
        </td>
        <td align="center">
            Unkown Service Type: <xsl:value-of select="." />
        </td>
    </xsl:template>



    <xsl:template name="FileServiceLinks" >
        <td align="left">
            <a href="{@name}">
                <xsl:value-of select="@name"/>
            </a>
        </td>
        <td align="center">
            <xsl:value-of
                    select="@lastModified" />
        </td>

        <td align="right">
            <xsl:value-of select="@size"/>
        </td>
        <xsl:call-template name="NoServiceLinks" />
    </xsl:template>

    <xsl:template name="NoServiceLinks">
        <td align="center">
            &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;
        </td>
    </xsl:template>




</xsl:stylesheet>