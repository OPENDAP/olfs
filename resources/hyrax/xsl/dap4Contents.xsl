<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2016 OPeNDAP, Inc.
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
<!DOCTYPE stylesheet []>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
>
    <xsl:import href="version.xsl" />

    <xsl:param name="dapService" />
    <xsl:param name="docsService" />
    <xsl:param name="viewersService" />
    <xsl:param name="collectionURL" />
    <xsl:param name="catalogPublisherJsonLD" />
    <xsl:param name="supportLink"/>

    <xsl:param name="allowDirectDataSourceAccess" />
    <xsl:param name="datasetUrlResponseType"/>

    <xsl:param name="userId" />
    <xsl:param name="loginLink" />
    <xsl:param name="logoutLink" />

    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes' />

    <!--***********************************************
   -
   - FUNCTION: bes:path_concat()
   -
   - Concatenates the string members of the passed
   - sequence parameter using a default delimiter
   - of slash "/".  This is a brute force method
   - that first makes the concatenation and then
   - uses a regexto replace any multiple occurance
   - of the delimiter with a single occurance
   -
   - TODO - Have slash be the default and add a second param for supplying other delimiters.
   -
   -
 -->
    <xsl:function name="bes:path_concat">
        <xsl:param name="sseq"/>
        <xsl:value-of select="replace(replace(string-join($sseq,'/'),'[/]+','/'),'[/]+$','')" />
    </xsl:function>


    <xsl:variable name="besPrefix">
        <xsl:choose>
            <xsl:when test="/bes:response/bes:showNode/@prefix!='/'">
                <xsl:value-of select="concat(/bes:response/bes:showNode/@prefix,'/')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="/bes:response/bes:showNode/@prefix" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>


    <xsl:template match="bes:response">
        <html>
            <xsl:apply-templates />
        </html>
    </xsl:template>



    <xsl:template match="bes:showNode">

        <xsl:variable name="catalogName">
            <xsl:choose>
                <xsl:when test="bes:node/@name='/'" >
                    <xsl:value-of select="/bes:response/bes:showNode/@prefix"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:if test="$besPrefix!='/'"><xsl:value-of select="$besPrefix"/></xsl:if>
                    <xsl:value-of select="(bes:node | bes:item)/@name"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <head>
            <link rel='stylesheet' href='{$docsService}/css/contents.css'
                  type='text/css'/>
            <title>OPeNDAP Hyrax: Contents of <xsl:value-of select="$catalogName"/></title>
        </head>
        <body>

            <!-- ****************************************************** -->
            <!--                      LOGIN UI                          -->
            <!--                                                        -->
            <!--                                                        -->
            <xsl:choose>
                <xsl:when test="$userId">

                    <div style='float: right;vertical-align:middle;font-size:small;'>
                        <xsl:choose>
                            <xsl:when test="$loginLink">
                                <b><a href="{$loginLink}"><xsl:value-of select="$userId"/></a></b> <br/>
                            </xsl:when>
                            <xsl:otherwise >
                                <b><xsl:value-of select="$userId"/></b><br/>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:if test="$logoutLink"><a style="color: green;" href="{$logoutLink}">logout</a></xsl:if>
                    </div>


                </xsl:when>
                <xsl:otherwise>

                    <xsl:if test="$loginLink">
                        <div style='float: right;vertical-align:middle;font-size:small;'>
                            <a style="color: green;" href="{$loginLink}">login</a>
                        </div>
                    </xsl:if>

                </xsl:otherwise>
            </xsl:choose>

            <!-- ****************************************************** -->
            <!--                      PAGE BANNER                       -->
            <!--                                                        -->
            <!--                                                        -->

            <img class="banner_logo_img" alt="OPeNDAP Logo" src='{$docsService}/images/logo.svg'/>
            <h1>Contents of
                <xsl:value-of select="$catalogName"/>
            </h1>
            <hr size="1" noshade="noshade"/>

            <!-- ****************************************************** -->
            <!--                       PAGE BODY                        -->
            <!--                                                        -->
            <!--                                                        -->
            <pre>
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
                        <th align="center">DAP4 Response Links</th>
                        <th align="center">DAP2 Response Links</th>
                        <th align="center">Dataset Viewers</th>
                    </tr>
                    <tr>
                        <td>
                            <xsl:if test="$besPrefix!='/'" >
                                <xsl:if test="bes:dataset/@name='/'" >
                                    <a href="..">Parent Directory/</a>
                                </xsl:if>
                            </xsl:if>
                        </td>
                        <td /><td /><td /><td /><td />
                    </tr>
                    <xsl:for-each select="bes:node/bes:item">

                        <!-- Process a collection. -->
                        <xsl:if test="@type='node'">
                            <tr>
                                <xsl:call-template name="NodeLinks" />
                            </tr>
                        </xsl:if>

                        <!-- Process a data set -->
                        <xsl:if test="@type='leaf'">
                            <tr itemprop="dataset" itemscope="" itemtype="http://schema.org/Dataset">
                                <xsl:choose>
                                    <xsl:when test="@isData='true'">
                                        <xsl:call-template name="DapServiceLinks" />
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
                <div>
                    <a href="{$docsService}/">Documentation</a>
                    <span class="small" style="font-weight: normal; display: inline; float: right; padding-right: 10px;">
                        <a href="{$supportLink}">Questions? Contact Support</a>
                    </span>
                </div>
            </h3>
            <xsl:call-template name="json-ld-DataCatalog"/>
        </body>
    </xsl:template>


    <xsl:template name="NodeLinks" >
        <td align="left">
            <!-- a href="{@name}/contents.html" -->
            <!-- Added 'encode-for-uri()' to fix an issue where URL components that contain
            	 colon characters were confusing browsers. Colons were added to the set of
            	 URL chars not scrubbed at the request of NASA/Raytheon. The equivalent edit
            	 was made in several places below. jhrg 5/7/15 -->
            <xsl:element name="a">
                <xsl:if test="bes:description">
                    <xsl:attribute name="title" select="bes:description"/>
                </xsl:if>
                <xsl:attribute name="href">
                    <xsl:value-of select="encode-for-uri(@name)"/>/contents.html
                </xsl:attribute>
                <xsl:value-of select="@name"/>/
            </xsl:element>
        </td>

        <td align="center" nowrap="nowrap">
            <xsl:value-of select="@lastModified"/>
        </td>

        <td align="center">-</td>
        <xsl:call-template name="NoDapServiceLinks" />
    </xsl:template>


    <xsl:template name="DapServiceLinks" >
        <!-- == == == == == == == == == == == == == == == == == == == == == -->
        <!-- DAP4 Data Request Form -->
        <xsl:variable name="dap_service_url">
            <xsl:choose>
                <xsl:when test="@dap_url" ><xsl:value-of select="@dap_url"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="encode-for-uri(@name)"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:comment>Using dap_service_url: <xsl:value-of select="$dap_service_url"/></xsl:comment>

        <xsl:variable name="description">
            <xsl:choose>
                <xsl:when test="bes:description" ><xsl:value-of select="bes:description"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="encode-for-uri(@name)"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:comment>Using description: <xsl:value-of select="description"/></xsl:comment>

        <td>
            <b>
                <xsl:element name="a">
                    <xsl:attribute name="href"><xsl:value-of select="$dap_service_url"/>.dmr.html</xsl:attribute>
                    <xsl:if test="$description">
                        <xsl:attribute name="title"><xsl:value-of select="$description"/></xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="@name"/>
                </xsl:element>
            </b>
        </td>
        <!-- == == == == == == == == == == == == == == == == == == == == == -->
        <!-- Last Modified Time -->
        <td align="center" nowrap="nowrap">
            <time itemprop="dateModified" datetime="{@lastModified}">
                <xsl:value-of select="@lastModified"/>
            </time>
        </td>
        <!-- == == == == == == == == == == == == == == == == == == == == == -->
        <!-- Size -->
        <td align="right">
            <xsl:value-of select="@size"/>
        </td>
        <!-- == == == == == == == == == == == == == == == == == == == == == -->
        <!-- DAP4 Service Links -->
        <td align="center" >
            <table class="response_links">
                <tr>
                    <td itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{$dap_service_url}.dmr.xml"/>
                        <meta itemprop="encodingFormat" content="application/vnd.opendap.dataset-metadata+xml"/>
                        <a title="DAP4 Metadata Response (.dmr.xml)"
                           itemprop="contentUrl" href="{$dap_service_url}.dmr.xml">dmr</a>
                    </td>
                    <td itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{$dap_service_url}.dap"/>
                        <meta itemprop="encodingFormat" content="application/vnd.opendap.data"/>
                        <a title="DAP4 Data Response (.dap)"
                           itemprop="contentUrl" href="{$dap_service_url}.dap">dap</a>
                    </td>
                    <td itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{$dap_service_url}.dmr.html"/>
                        <meta itemprop="encodingFormat" content="text/html"/>
                        <a title="DAP4 Data Request Form (.dmr.html)"
                           itemprop="contentUrl" href="{$dap_service_url}.dmr.html">html</a>
                    </td>
                    <td itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{$dap_service_url}.dmr.rdf"/>
                        <meta itemprop="encodingFormat" content="application/rdf+xml"/>
                        <a title="DAP4 Metadata Response encoded as RDF (.dmr.rdf)"
                           itemprop="contentUrl" href="{$dap_service_url}.dmr.rdf">rdf</a>
                    </td>

                    <xsl:if test="$allowDirectDataSourceAccess='true'">
                        <td itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                            <meta itemprop="name" content="{@name}"/>
                            <meta itemprop="contentSize" content="{@size}"/>
                            <xsl:element name="a">
                                <xsl:attribute name="itemprop">contentUrl</xsl:attribute>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="$dap_service_url"/>
                                    <xsl:if test="$datasetUrlResponseType!='download'">.file</xsl:if>
                                </xsl:attribute>
                                <xsl:text>file</xsl:text>
                            </xsl:element>
                        </td>
                    </xsl:if>
                </tr>
            </table>
        </td>
        <!-- == == == == == == == == == == == == == == == == == == == == == -->
        <!-- DAP2 Service Links -->
        <td align="center">
            <table class="response_links">
                <tr >
                    <td align="center" itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{@name}.dds"/>
                        <meta itemprop="encodingFormat" content="text/plain"/>
                        <a title="DAP2 Syntactic (structural) Metadata Response (.dds)"
                           itemprop="contentUrl" href="{encode-for-uri(@name)}.dds">dds</a>
                    </td>
                    <td align="center" itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{@name}.das"/>
                        <meta itemprop="encodingFormat"  content="text/plain"/>
                        <a title="DAP2 Semantic Metadata Response (.das)"
                           itemprop="contentUrl" href="{encode-for-uri(@name)}.das">das</a>
                    </td>
                    <td align="center" itemprop="distribution" itemscope="" itemtype="https://schema.org/DataDownload">
                        <meta itemprop="name" content="{@name}.info"/>
                        <meta itemprop="encodingFormat" content="text/html"/>
                        <a title="DAP2 Summary Information HTML response (.info)"
                           itemprop="contentUrl" href="{encode-for-uri(@name)}.info">info</a>
                    </td>
                </tr>
            </table>
        </td>
        <xsl:call-template name="DatasetViewers"/>
    </xsl:template>




    <xsl:template name="DatasetViewers" >

        <xsl:if test="not(@dap_url)">
            <xsl:variable name="datasetID">
                <xsl:choose>
                    <xsl:when test="../../@name='/'">
                        <xsl:value-of select="bes:path_concat(($besPrefix,@name))" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="bes:path_concat(($besPrefix, ../@name,@name))" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <td align="center">
                <table class="response_links">
                    <td align="center">
                        <xsl:comment>viewersService: <xsl:value-of select="$viewersService"/></xsl:comment>
                        <a href="{$viewersService}/viewers?dapService={$dapService}&#38;datasetID={$datasetID}">viewers</a>
                    </td>
                </table>
            </td>

        </xsl:if>

    </xsl:template>




    <xsl:template name="UnknownServiceLinks" >
        <td align="left">
            <a href="{encode-for-uri(../@name)}">
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
            <a href="{encode-for-uri(@name)}">
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
            <table class="response_links">
                <tr>
                    <td align="center">-</td>
                    <td align="center">-</td>
                    <td align="center">-</td>
                    <td align="center">-</td>
                </tr>
            </table>
        </td>
        <td align="center">
            <table class="response_links">
                <tr>
                    <td align="center">-</td>
                    <td align="center">-</td>
                    <td align="center">-</td>
                </tr>
            </table>
        </td>
        <td align="center">
            <table class="response_links">
                <tr>
                    <td align="center">-</td>
                </tr>
            </table>
        </td>
    </xsl:template>



    <xsl:template name="json-ld-DataCatalog">
        <xsl:element name="script" >
            <xsl:attribute name="type">application/ld+json</xsl:attribute>
            {
            "@context": "http://schema.org",
            "@type": "DataCatalog",
            "name": "Hyrax Data Server (OPeNDAP)",
            "url": "https://www.opendap.org",
            <xsl:value-of select="$catalogPublisherJsonLD"/>,
            "fileFormat": [
            "application/geo+json",
            "application/json",
            "text/csv"
            ],
            "isAccessibleForFree": "True",
            "dataset": [ <xsl:for-each select="bes:node/bes:item[@isData='true']">
            {
            "@type": "Dataset",
            "name": "<xsl:value-of select="@name"/>",
            "sameAs": "<xsl:value-of select="$collectionURL"/><xsl:value-of select="@name"/>.html"
            }<xsl:if test="position()!=last()" >,</xsl:if>
        </xsl:for-each>
            ]
            }
        </xsl:element>
    </xsl:template>


</xsl:stylesheet>