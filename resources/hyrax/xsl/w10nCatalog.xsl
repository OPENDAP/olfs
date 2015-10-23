<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2015 OPeNDAP, Inc.
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

        <xsl:variable name="catalogName">
            <xsl:choose>
                <xsl:when test="bes:dataset/@name='/'" >
                    <xsl:value-of select="/bes:response/bes:showCatalog/bes:dataset/@prefix"/>
                </xsl:when>
                <xsl:otherwise >
                    <xsl:value-of select="$besPrefix"/><xsl:value-of select="bes:dataset/@name"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

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

                <table width="100%" >
                    <tr>
                        <td width="206px"><img alt="OPeNDAP Logo" src='{$docsService}/images/logo.gif'/></td>
                        <td align="center" class="xxlarge">w10n Service</td>
                    </tr>
                </table>

                <h1><span class="small">meta for
                    <xsl:choose>
                        <xsl:when test="bes:dataset/@node='false'">leaf:</xsl:when>
                        <xsl:otherwise>node:</xsl:otherwise>
                    </xsl:choose>
                    </span> <xsl:value-of select="$catalogName"/></h1>
                <table width="100%">
                    <tr>
                        <td class="small">
                            <xsl:if test="bes:dataset/@name!='/'" >
                                <span class="small" style="font-weight: bold;"><a href="..">Parent Node</a></span>
                            </xsl:if>
                        </td>
                        <td align="right">
                            <span class="small">
                                <span style="font-weight: bold;">
                                    META:
                                </span>
                                <span style="padding-left:2px;">
                                    <a href=".?output=json">json</a>
                                </span>
                                <span style="padding-left:2px;">
                                    <a href=".?output=html">html</a>
                                </span>
                            </span>

                        </td>
                    </tr>
                </table>

                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->

                <pre>

                    <xsl:choose>

                        <xsl:when test="bes:dataset/@node='false'">
                            <xsl:call-template name="ProcessCatalogLeaf">
                                <xsl:with-param name="fullName" select="$catalogName" />
                                <xsl:with-param name="dataset" select="bes:dataset"/>
                            </xsl:call-template>
                        </xsl:when>

                        <xsl:otherwise>
                            <table border="0" width="100%" itemscope="" itemtype="http://schema.org/DataCatalog">
                                <caption style="display:none">
                                    <a itemprop="url" href="#">
                                        <span itemprop="name">
                                            <xsl:value-of select="$catalogName"/>
                                        </span>
                                    </a>
                                </caption>
                                <tr>
                                    <th align="left">Name</th>
                                    <th align="center">Last Modified</th>
                                    <th align="center">Size</th>
                                </tr>

                                <xsl:call-template name="ProcessCatalogNode" />

                            </table>


                        </xsl:otherwise>
                    </xsl:choose>


                </pre>
                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <div class="small" align="right">
                    Hyrax development sponsored by
                    <a href='http://www.nsf.gov/'>NSF</a>
                    ,
                    <a href='http://www.nasa.gov/'>NASA</a>
                    , and
                    <a href='http://www.noaa.gov/'>NOAA</a>
                </div>

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

    <xsl:template name="ProcessCatalogLeaf"  >
        <xsl:param name="fullName"/>
        <xsl:param name="dataset"/>

        <table>
            <tr>
                <td class="medium" style="padding-right: 50px;"><xsl:value-of select="$dataset/@name" /></td>
                <td class="medium_bold"><a href="{$dapService}/{$fullName}"> Download </a></td>
            </tr>
            <tr>
                <td class="medium">Size (bytes)</td>
                <td class="medium"><xsl:value-of select="$dataset/@size" /></td>
            </tr>
            <tr>
                <td class="medium">Last Modified Time</td>
                <td class="medium"><xsl:value-of select="$dataset/@lastModified" /></td>
            </tr>
        </table>

    </xsl:template>


    <xsl:template name="ProcessCatalogNode" >
        <xsl:for-each select="bes:dataset/bes:dataset">

            <!-- Process a collection. -->
            <xsl:if test="@node='true'">
                <tr>
                    <xsl:call-template name="NodeLinks" />
                </tr>
            </xsl:if>

            <!-- Process a data set -->
            <xsl:if test="@node='false'">
                <tr itemprop="dataset" itemscope="" itemtype="http://schema.org/Dataset">
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

    </xsl:template>

    <xsl:template name="NodeLinks" >
        <td align="left">
            <a href="{@name}/"><xsl:value-of select="@name"/>/</a>
        </td>

        <td align="center" nowrap="nowrap">
            <xsl:value-of select="@lastModified"/>
        </td>
        <td align="right">- - -</td>

    </xsl:template>




    <xsl:template match="bes:serviceRef" >
        <xsl:choose>
            <xsl:when test=".='dap'">
                <xsl:call-template name="DapServiceLinks" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="UnknownServiceLinks" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



    <xsl:template name="DapServiceLinks" >
        <td align="left">
            <b><a href="{../@name}/">
                <span itemprop="name"><xsl:value-of select="../@name"/>/</span>
            </a>
            </b>
            <!--
            <span class="small"
                  align="center"
                  itemprop="distribution"
                  itemscope=""
                  itemtype="http://schema.org/DataDownload">
                <meta itemprop="name" content="{../@name}/?output=json" />
                <meta itemprop="encodingFormat" content="application/json" />
                &NBSP;&NBSP;(<a itemprop="contentUrl" href="{../@name}/?output=json">json</a>)
            </span>
            -->
        </td>

        <td align="center" nowrap="nowrap">
            <time itemprop="dateModified" datetime="{../@lastModified}">
                <xsl:value-of select="../@lastModified" />
            </time>
        </td>

        <td align="right">
            <xsl:value-of select="../@size"/>
        </td>


    </xsl:template>





    <xsl:template name="UnknownServiceLinks" >
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
            Unknown Service Type: <xsl:value-of select="." />
        </td>
    </xsl:template>



    <xsl:template name="FileServiceLinks" >
        <td align="left">
            <a href="{@name}/">
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
    </xsl:template>





</xsl:stylesheet>