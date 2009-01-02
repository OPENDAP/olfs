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
<!DOCTYPE xsl:stylesheet [
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"

                >
    <xsl:param name="remoteHost" />
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

    <xsl:template match="thredds:catalog">
        <html>
            <head>
                <link rel='stylesheet' href='/opendap/docs/css/contents.css'
                      type='text/css'/>
                <title><xsl:value-of select="@name"/></title>

            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="Logo" src='/opendap/docs/images/logo.gif'/>
                <h1>
                    <xsl:value-of select="@name"/>
                    <div class="small" align="left">
                        <xsl:if test="thredds:service">
                            <br/>services:
                            <table>
                                <xsl:apply-templates select="thredds:service" mode="banner">
                                    <xsl:with-param name="indent"> </xsl:with-param>
                                </xsl:apply-templates>
                            </table>
                            <br/>
                        </xsl:if>

                    </div>
                </h1>
                <h3>Remote Host: <xsl:value-of select="$remoteHost" /></h3>

                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <pre>


                    <table border="0" width="100%">
                        <tr>
                            <th align="left">Dataset</th>
                            <th align="center">Size</th>
                            <th align="center">Last Modified</th>
                        </tr>


                        <xsl:apply-templates />


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
                                <SCRIPT LANGUAGE="JavaScript">
                                    <xsl:comment >
                                    {
                                        catalog = location.href.replace('.html','.xml');
                                        document.write('&lt;a href="' + catalog +'"&gt;');
                                        document.write('XML');
                                        document.write('&lt;/a&gt;');
                                    }
                                    </xsl:comment>
                                </SCRIPT>
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
                <!--                                                        -->
                <h3>OPeNDAP Hyrax <font class="small">(THREDDS Catalog Service)</font>

                    <br/>
                    <a href='/opendap/docs/'>Documentation</a>
                </h3>


            </body>
        </html>


    </xsl:template>



    <!--***********************************************
       -
       -
       -
       -
       -
       -
       -
     -->
    <xsl:template match="thredds:catalogRef">
        <xsl:param name="indent" />
        
        <xsl:if test="false()">
            <tr>
                <td align="left" >
    
                    <!-- If the href ends in .xml, change it to .html
                         so the link in the presentation points to
                         another HTML page. -->
                    <xsl:if test="substring(./@xlink:href,string-length(./@xlink:href) - 3)='.xml'">
                        <a href="{concat(substring(./@xlink:href,1,string-length(./@xlink:href) - 4),'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
                    </xsl:if>
    
                    <!-- Since it doesn't end in .xml we don't know how to promote it, so leave it be. -->
                    <xsl:if test="not(substring(./@xlink:href,string-length(./@xlink:href) - 3))">
                        <a href="{./@xlink:href}" ><xsl:value-of select="./@xlink:title"/> /</a>
                    </xsl:if>
    
                </td>
                <xsl:call-template name="NoSizeNoTime" />
            </tr>
        </xsl:if>


        <xsl:if test="true()">
            <tr>
                <td align="left" >
    
                    <a>
                    <xsl:attribute name="href">.?browseCatalog=<xsl:value-of select="$remoteHost"/><xsl:value-of select="@xlink:href" /></xsl:attribute>
                    <xsl:value-of select="./@xlink:title"/>
                    </a> 
    
    
                </td>
                <xsl:call-template name="NoSizeNoTime" />
            </tr>
        </xsl:if>

    </xsl:template>


    <!--***********************************************
       -
       -
       -
       -
       -
       -
       -
     -->
    <xsl:template match="thredds:catalogRef" mode="EasierToReadVersion">
        <xsl:param name="indent" />
        <tr>
            <td align="left">

                <xsl:variable name="href" select="./@xlink:href" />
                <xsl:variable name="linkSuffix" select="substring($href,string-length($href) - 3)" />
                <xsl:variable name="linkBody"   select="substring($href,1,string-length($href) - 4)" />

                <xsl:if test="$linkSuffix='.xml'">
                    <xsl:value-of select="$indent"/><a href="{concat($linkBody,'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
               </xsl:if>

                <xsl:if test="not($linkSuffix='.xml')">
                    <xsl:value-of select="$indent"/><a href="{$href}" ><xsl:value-of select="./@xlink:title"/> /</a>
               </xsl:if>
            </td>
            <xsl:call-template name="NoSizeNoTime" />
        </tr>

    </xsl:template>


    <!--***********************************************
       -
       -
       -
       -
       - <datasetScan location="/bes/data" path="data" name="SVN Test Data Archive" serviceName="OPeNDAP-Hyrax">
       -
       -
     -->
    <xsl:template match="thredds:datasetScan" >
        <xsl:param name="indent" />
        <tr>
            <td align="left">
                <xsl:value-of select="$indent"/><a href="{key('service-by-name', @serviceName)/@base}{@path}/catalog.html"><xsl:value-of select="@name" /></a>/
            </td>

            <xsl:call-template name="NoSizeNoTime" />
        </tr>
    </xsl:template>


    <!--***********************************************
       -
       -
       -
       -
       -
       -
       -    <service name="OPeNDAP-Hyrax" serviceType="OPeNDAP" base="/opendap/"/>
     -->


    <xsl:template match="thredds:service" name="serviceBanner" mode="banner">
        <xsl:param name="indent" />

        <tr>
            <td class="small" align="left">
                <xsl:value-of select="$indent"/><xsl:value-of select="@name"/>&#160;&#160;&#160;&#160;&#160;&#160;
            </td>
            <td class="small" align="left">
                <xsl:value-of select="$indent"/><i>serviceType:</i> <xsl:value-of select="@serviceType"/>&#160;&#160;
            </td>
            <td class="small" align="left">
                <xsl:value-of select="$indent"/><i>base:</i> <xsl:value-of select="@base"/>&#160;&#160;
                <br/>
            </td>
            <xsl:apply-templates  mode="banner" >
                <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
            </xsl:apply-templates>

        </tr>
    </xsl:template>




    <!--***********************************************
       -
       -
       -
       -
       -
       -
       -
     -->
    <xsl:template match="thredds:dataset">
        <xsl:param name="indent" />
        <xsl:param name="inheritedMetadata" />

        <xsl:if test="thredds:dataset">
            <tr>
                <td  align="left">
                    <xsl:value-of select="$indent"/><xsl:value-of select="@name" />/

                </td>
                <xsl:call-template name="NoSizeNoTime" />
            </tr>
            <xsl:apply-templates>
                <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
                <!--
                  -   Note that the followiing parameter uses an XPath that
                  -   accumulates inherited thredds:metadata elements as it descends the
                  -   hierarchy.
                  -->
                <xsl:with-param name="inheritedMetadata" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]" />
            </xsl:apply-templates>
         </xsl:if >

        <xsl:if test="not(thredds:dataset)">
            <tr>
                <td class="dark">

                    <xsl:value-of select="$indent"/><a>
                        <xsl:attribute name="href">
                            ?dataset=<xsl:value-of select="preceding::*/last()" />
                        </xsl:attribute>
                        <xsl:value-of select="@name" />
                        </a>

                </td>
                <xsl:call-template name="SizeAndTime" >
                    <xsl:with-param name="currentDataset" select="." />
                    <xsl:with-param name="metadata" select="thredds:metadata" />
                    <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata[boolean($inheritedMetadata)]" />
                </xsl:call-template>
            </tr>
        </xsl:if>

    </xsl:template>

    <xsl:template name="NoSizeNoTime" >
            <td align="center">
                --
            </td>
            <td align="center">
                --
            </td>
    </xsl:template>


    <xsl:template name="SizeAndTime" >
        <xsl:param name="currentDataset" />
        <xsl:param name="metadata" />
        <xsl:param name="inheritedMetadata" />
        <!-- Do the Size -->
        <td class="small" align="center">
            <xsl:choose>

                <xsl:when test="$currentDataset/thredds:dataSize">
                    <xsl:value-of select="$currentDataset/thredds:dataSize" />&#160;<xsl:value-of select="$currentDataset/thredds:dataSize/@units" />
                </xsl:when>

                <xsl:when test="$metadata/thredds:dataSize">
                    <xsl:value-of select="$metadata/thredds:dataSize" />&#160;<xsl:value-of select="$metadata/thredds:dataSize/@units" />
                </xsl:when>

                <xsl:when test="$inheritedMetadata/thredds:dataSize">
                    <xsl:value-of select="$inheritedMetadata/thredds:dataSize" />&#160;<xsl:value-of select="$inheritedMetadata/thredds:dataSize/@units" />
                </xsl:when>

                <xsl:otherwise>--</xsl:otherwise>
            </xsl:choose>
        </td>

        <!-- Do the Time -->
        <td class="small" align="center">
            <xsl:choose>

                <xsl:when test="$currentDataset/thredds:date">
                    <xsl:value-of select="$currentDataset/thredds:date" />
                </xsl:when>

                <xsl:when test="$metadata/thredds:date">
                    <xsl:value-of select="$metadata/thredds:date" />
                </xsl:when>

                <xsl:when test="$inheritedMetadata/thredds:date">
                    <xsl:value-of select="$inheritedMetadata/thredds:date" />
                </xsl:when>

                <xsl:otherwise>--</xsl:otherwise>
            </xsl:choose>
        </td>

    </xsl:template>




    <xsl:template match="thredds:*">
        <xsl:param name="indent" />
    </xsl:template>



</xsl:stylesheet>

