<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
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
                xmlns:wcs="http://www.opengis.net/wcs/1.1"
                xmlns:ows="http://www.opengis.net/ows/1.1"
                xmlns:xlink="http://www.w3.org/1999/xlink"
        >
    <xsl:param name="ServicePrefix" />
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:strip-space elements="*"/>

    <xsl:variable name="WcsServiceVersion">1.0.0</xsl:variable>



    <xsl:template match="/wcs:CoverageDescription">
        <html>
            <head>
                <xsl:element name="link">
                    <xsl:attribute name="rel">stylesheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                    <xsl:attribute name="href"><xsl:value-of select="$ServicePrefix"/>/docs/css/contents.css</xsl:attribute>
                </xsl:element>
                <xsl:choose>
                    <xsl:when test="ows:Title">
                        <title>
                            <xsl:value-of select="ows:Title"></xsl:value-of>
                        </title>
                    </xsl:when>
                    <xsl:otherwise>
                        <title>OPeNDAP: Web Coverage Service</title>
                    </xsl:otherwise>
                </xsl:choose>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <table border="0" width="90%"><tr>

                    <td><img alt="Institution Logo" src="{concat($ServicePrefix,'/docs/images/logo.gif')}" /></td>

                    <td align="center"><div  class="xlarge"> Web Coverage Service</div></td>
                    
                </tr></table>
                <xsl:choose>
                    <xsl:when test="ows:Title">
                        <h2>
                            WCS Coverage: <br/><xsl:value-of select="ows:Title"></xsl:value-of>
                            <font class="small"><a href="{$ServicePrefix}?service=WCS&amp;version=1.1.2&amp;request=DescribeCoverage&amp;identifiers={wcs:Identifier}">XML</a></font>
                        </h2>
                    </xsl:when>
                    <xsl:otherwise>
                        <h2><font class="small">wcs:</font>Coverage Description
                            <a href="{$ServicePrefix}?service=WCS&amp;version=1.1.2&amp;request=DescribeCoverage&amp;identifiers={wcs:Identifier}"><font class="small">XML</font></a>
                        </h2>
                    </xsl:otherwise>
                </xsl:choose>

                <div class="small">
                    <xsl:apply-templates select="ows:Abstract"/>
                    <xsl:apply-templates select="ows:Keywords"/>
                </div>

                <dl>
                    <dt><span class="medium">wcs:Identifier</span><br/></dt>
                    <dd><span class="medium_bold"><xsl:value-of select="wcs:Identifier" /></span></dd>
                </dl>





                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->


                <xsl:apply-templates select="wcs:Domain"/>
                <xsl:apply-templates select="wcs:Range"/>
                <div class="medium">
                <h3>Supported Coordinate Reference Systems</h3>
                <ul>
                <xsl:apply-templates select="wcs:SupportedCRS"/>
                </ul>
                 <h3>Supported Formats</h3>
                <ul>
                <xsl:apply-templates select="wcs:SupportedFormat"/>
                </ul>
                </div>


                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <div class="small" align="right">
                    Hyrax development sponsored by<a href='http://www.nsf.gov/'>NSF</a>,<a href='http://www.nasa.gov/'>
                    NASA</a>, and
                    <a href='http://www.noaa.gov/'>NOAA</a>
                </div>
                <!-- ****************************************************** -->
                <!--         HERE IS THE HYRAX VERSION NUMBER               -->
                <!--                                                        -->
                <h3>OPeNDAP WCS Service - <xsl:value-of select="$WcsServiceVersion"/><br/>
                    <span class="uuid">
                        ServerUUID=e93c3d09-a5d9-49a0-a912-a0ca16430b91-contents
                    </span>
                </h3>

            </body>
        </html>
    </xsl:template>


    <!-- ****************************************************** -->
    <!--         ows:Domain                                     -->
    <!--                                                        -->
    <xsl:template match="wcs:Domain">
        <h3>Domain</h3>

        <div class="medium">
            <xsl:apply-templates/>
        </div>

    </xsl:template>


    <xsl:template match="wcs:TemporalDomain">
        Temporal Domain:
        <ul>
                <xsl:apply-templates mode="simple"/>
        </ul>
    </xsl:template>


    <xsl:template match="wcs:SpatialDomain">
        Spatial Domain:
        <ul>
                <xsl:apply-templates/>
        </ul>
    </xsl:template>


    <xsl:template match="ows:BoundingBox">
        <li>Bounding Box <span class="small">(crs='<xsl:value-of select="@crs"/>')</span>
        <ul>
        <li><span class="small" align="left">lowerCorner: </span>[<xsl:value-of select="ows:LowerCorner"/>]</li>
        <li><span class="small" align="left">upperCorner: </span>[<xsl:value-of select="ows:UpperCorner"/>]</li>
        </ul>
        </li>
    </xsl:template>


    <xsl:template match="wcs:SupportedCRS">
        <li>
            <xsl:value-of select="."/>
        </li>
    </xsl:template>


    <xsl:template match="wcs:SupportedFormat">
        <li>
            <xsl:value-of select="."/>
        </li>
    </xsl:template>


    <xsl:template match="ows:Keywords">
        <em>Keywords:</em>
        <xsl:apply-templates/>
        <br/>
    </xsl:template>

    <xsl:template match="ows:Keyword">
        [<xsl:value-of select="."/>]
    </xsl:template>


    <xsl:template match="ows:Abstract">
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>

    <xsl:template match="wcs:Range">
        <h3>Range</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>


    <xsl:template match="wcs:Field">
        <li>field: <font class="medium_bold"> <xsl:value-of select="wcs:Identifier"/></font>
        <ul>
            <xsl:apply-templates mode="simple"/>
        </ul>
        </li>
    </xsl:template>



    <xsl:template match="*" mode="simple">
        <xsl:choose>
            <xsl:when test="*">
                <li>
                    <em><xsl:value-of select="local-name()"/>:
                    </em>
                </li>
                <ul>
                    <xsl:apply-templates mode="simple"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <li>
                    <em><xsl:value-of select="local-name()"/>:
                    </em>
                    <xsl:value-of select="."/>
                </li>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="@*|text()"/>


</xsl:stylesheet>