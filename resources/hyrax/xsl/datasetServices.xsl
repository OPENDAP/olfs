<?xml version="1.0" encoding="ISO-8859-1"?>
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
<!DOCTYPE stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:ds="http://xml.opendap.org/ns/DAP/4.0/dataset-services#"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xlink="http://www.w3.org/1999/xlink"
        >
    <xsl:import href="version.xsl" />

    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:variable name="docsService">/opendap/docs</xsl:variable>





    <xsl:template match="ds:DatasetServices">
        <html>
            <head>
                <link rel='stylesheet' href='{$docsService}/css/contents.css'
                      type='text/css'/>
                <title>OPeNDAP Hyrax: Dataset Service Description for <xsl:value-of select="@xml:base"/></title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="OPeNDAP Logo" src='{$docsService}/images/logo.gif'/>
                <h1>
                    Dataset Services Response
                    <div class="medium">
                       Dataset Resource ID: <xsl:value-of select="@xml:base"/>
                    </div>
                </h1>
                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <pre>
                    <table border="0">
                        <tr>
                            <th align="left">Service Name
                                <div class="small" style="margin-left: 10px;">Available Representations</div>
                                <hr size="1" noshade="noshade"/>
                            </th>
                            <th align="center"> </th>
                            <th align="left">Description
                                <div class="small" style="margin-left: 10px;color: lightGrey;">Resource Role ID</div>
                                <hr size="1" noshade="noshade"/>
                            </th>
                        </tr>
                        <xsl:apply-templates select="ds:Service"/>
                    </table>
                </pre>

                <hr size="1" noshade="noshade"/>
                <div class="medium_bold">Server Side Functions Available For This Dataset</div>

                <pre>
                    <xsl:apply-templates select="ServerSideFunctions"/>
                </pre>

                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                        </td>
                        <td>
                            <div class="small" text-align="right">
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

                    <br/>
                    <a href='{$docsService}/'>Documentation</a>
                </h3>

            </body>
        </html>
    </xsl:template>


    <xsl:template match="ds:Service">

        <tr>
            <td>
                <xsl:value-of select="@title"/>
                <xsl:apply-templates select="ds:link" />
            </td>
            <td> </td>
            <td>
                <xsl:if test="ds:Description/@href!=''">
                    <xsl:value-of select="ds:Description"/>
                    <xsl:if test="ds:Description/@href!=''">
                        (<a href="{ds:Description/@href}">more</a>)
                    </xsl:if>
                    <br/>
                </xsl:if>
                    <span class="small" style="margin-left: 10px;color: lightGrey;"><xsl:value-of select="@role"/></span>
            </td>
        </tr>

    </xsl:template>


    <xsl:template match="ds:link">

        <div class="small" style="margin-left: 10px;"><a title="{@description}" href="{@href}"><xsl:value-of select="@type"/></a></div>

    </xsl:template>



    <xsl:template match="ServerSideFunctions">

        <table border="0">
            <tr>
                <th align="left">Function Name</th>
                <th align="center"></th>
                <th align="left">Description</th>
            </tr>
            <xsl:apply-templates select="Function"/>
        </table>

    </xsl:template>



    <xsl:template match="Function">

        <tr>
            <td><xsl:value-of select="@name"/></td>
            <td> </td>
          <td><xsl:value-of select="Description"/> (<a href="{@xlink:href}">more</a>)</td>
        </tr>

    </xsl:template>






</xsl:stylesheet>