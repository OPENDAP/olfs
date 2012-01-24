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
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
                >
    <xsl:import href="version.xsl" />

    <xsl:param name="dapService" />
    <xsl:param name="docsService" />
    <xsl:param name="webStartService" />
    <xsl:param name="allowDirectDataSourceAccess" />

    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes' />

    <xsl:variable name="besPrefix">
        <xsl:choose>
            <xsl:when test="/bes:response/bes:showCatalog/bes:dataset/@prefix!='/'">
                <xsl:value-of select="concat(/bes:response/bes:showCatalog/bes:dataset/@prefix,'/')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="/bes:response/bes:showCatalog/bes:dataset/@prefix"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>


    <xsl:template match="bes:response">
        <html>
            <xsl:apply-templates />
        </html>
    </xsl:template>



    <xsl:template match="bes:showCatalog">
            <head>
                <link rel='stylesheet' href='{$docsService}/css/contents.css'
                      type='text/css'/>
                <title>OPeNDAP Hyrax: Contents of <xsl:value-of select="bes:dataset/@name"/></title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="OPeNDAP Logo" src='{$docsService}/images/logo.gif'/>
                <h1>Contents of
                    <xsl:choose>
                        <xsl:when test="bes:dataset/@name='/'" >
                            <xsl:value-of select="/bes:response/bes:showCatalog/bes:dataset/@prefix"/>
                        </xsl:when>
                        <xsl:otherwise >
                            <xsl:value-of select="$besPrefix"/><xsl:value-of select="bes:dataset/@name"/>
                        </xsl:otherwise>
                    </xsl:choose>
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
                            <th align="center">DAP Response Links</th>
                            <th align="center">Webstart</th>
                        </tr>
                        <tr>
                            <td>
                                <xsl:if test="bes:dataset/@name!='/'" >
                                    <a href="..">Parent Directory/</a>
                                </xsl:if>
                                <xsl:if test="$besPrefix!='/'" >
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
                                THREDDS Catalog <a href="catalog.xml">XML</a>
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
                    <a href='{$docsService}/'>Documentation</a>
                </h3>

            </body>
    </xsl:template>


    <xsl:template name="NodeLinks" >
        <td align="left">
            <a href="{@name}/contents.html">
            <xsl:value-of select="@name"/>/</a>
        </td>

        <td align="center" nowrap="nowrap">
            <xsl:value-of select="@lastModified"/>
        </td>

        <td align="right">-</td>
        <xsl:call-template name="NoDapServiceLinks" />
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

        <td align="center" nowrap="nowrap">
            <xsl:value-of select="../@lastModified" />
        </td>

        <td align="right">
            <xsl:value-of select="../@size"/>
        </td>


        <td align="center">
            <table>
                <tr>
                    <td> <a href="{../@name}.ddx">ddx</a>&NBSP;</td>
                    <td> <a href="{../@name}.dds">dds</a>&NBSP;</td>
                    <td> <a href="{../@name}.das">das</a>&NBSP;</td>
                    <td> <a href="{../@name}.info">info</a>&NBSP;</td>
                    <td> <a href="{../@name}.html">html</a>&NBSP;</td>
                    <td> <a href="{../@name}.rdf">rdf</a>&NBSP;</td>
                    <xsl:if test="$allowDirectDataSourceAccess='true'">
                        <td> <a href="{../@name}">file</a>&NBSP;</td>
                    </xsl:if>
                </tr>
            </table>
        </td>
        <xsl:call-template name="WebStartLinks"/>
    </xsl:template>




    <xsl:template name="WebStartLinks" >
        <xsl:variable name="datasetID">
            <xsl:choose>
                <xsl:when test="../../@name='/'">
                   <xsl:value-of select="$besPrefix"/><xsl:value-of select="../@name" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$besPrefix"/><xsl:value-of select="../../@name" />/<xsl:value-of select="../@name" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>



        <td align="center">
              <a href="{$webStartService}/viewers?dapService={$dapService}&#38;datasetID={$datasetID}">viewers</a>
        </td>
        
    </xsl:template>




    <xsl:template name="UnkownServiceLinks" >
        <td align="left">
            <a href="{../@name}">
                <xsl:value-of select="../@name"/>
            </a>
        </td>
        <td align="center" nowrap="nowrap">
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
        <td align="center" nowrap="nowrap">
            <xsl:value-of
                    select="@lastModified" />
        </td>

        <td align="right">
            <xsl:value-of select="@size"/>
        </td>
        <xsl:call-template name="NoDapServiceLinks" />
    </xsl:template>

    <xsl:template name="NoDapServiceLinks">
        <td  align="center">
            <table >
                <tr>
                    <td>&NBSP;-&NBSP;&NBSP;</td>
                    <td>&NBSP;-&NBSP;&NBSP;</td>
                    <td>&NBSP;-&NBSP;&NBSP;</td>
                    <td>&NBSP;-&NBSP;&NBSP;&NBSP;</td>
                    <td>&NBSP;-&NBSP;&NBSP;&NBSP;</td>
                    <td>&NBSP;-&NBSP;&NBSP;</td>
                    <xsl:if test="$allowDirectDataSourceAccess='true'">
                        <td>&NBSP;-&NBSP;&NBSP;&NBSP;</td>
                    </xsl:if>
                </tr>
            </table>
        </td>
    </xsl:template>




</xsl:stylesheet>