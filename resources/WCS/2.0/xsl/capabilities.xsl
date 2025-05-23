<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2017 OPeNDAP, Inc.
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
                xmlns:wcs="http://www.opengis.net/wcs/2.0"
                xmlns:ows="http://www.opengis.net/ows/2.0"
                xmlns:wcseo="http://www.opengis.net/wcs/wcseo/1.0"
                xmlns:gml="http://www.opengis.net/gml/3.2"
                xmlns:xlink="http://www.w3.org/1999/xlink"
        >
    <xsl:param name="WcsService" />
    <xsl:param name="DocsService" />
    <xsl:param name="UpdateIsRunning"/>
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:strip-space elements="*"/>

    <xsl:decimal-format name="CoordinateFormatter" />

    <xsl:variable name="WcsSoftwareVersion">@WcsSoftwareVersion@</xsl:variable>
    <xsl:variable name="WcsServiceVersion">2.0</xsl:variable>

    <xsl:template match="/wcs:Capabilities">
        <html>
            <head>
                <xsl:element name="link">
                    <xsl:attribute name="rel">stylesheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                    <xsl:attribute name="href"><xsl:value-of select="$DocsService"/>/css/contents.css</xsl:attribute>
                </xsl:element>

                <xsl:choose>
                    <xsl:when test="ows:ServiceIdentification/ows:Title">
                        <title>
                            <xsl:value-of select="ows:ServiceIdentification/ows:Title"/>
                        </title>
                    </xsl:when>
                    <xsl:otherwise>
                        <title>OPeNDAP Hyrax: WCS Capabilities</title>
                    </xsl:otherwise>
                </xsl:choose>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->


                <table border="0" width="90%"><tr>

                    <td><img alt="Institution Logo" src="{concat($DocsService,'/images/logo.svg')}" /></td>

                    <td align="center"><div  class="xlarge"> Web Coverage Service</div></td>

                </tr></table>
                <xsl:if test="$UpdateIsRunning">
                    <table border="0" width="100%">
                        <tr>
                            <td align="right"><div class="small">
                                <xsl:choose>
                                    <xsl:when test="$UpdateIsRunning='true'">WCS catalog is currently being updated.</xsl:when>
                                    <xsl:otherwise>WCS catalog is up to date.</xsl:otherwise>
                                </xsl:choose>
                            </div></td>
                        </tr>
                    </table>

                </xsl:if>

                <h1>
                    <xsl:choose>
                        <xsl:when test="ows:ServiceIdentification/ows:Title">
                                <xsl:value-of select="ows:ServiceIdentification/ows:Title"/>
                        </xsl:when>
                        <xsl:otherwise>WCS Capabilities</xsl:otherwise>
                    </xsl:choose>
                       - <xsl:element name="a">
                            <xsl:attribute name="style">font-family:Tahoma,Arial,sans-serif; color:white; background-color:#527CC1; font-size:16px; padding-left: 5px;</xsl:attribute >
                            <xsl:attribute name="href">?service=WCS&amp;request=GetCapabilities</xsl:attribute >
                            GetCapabilities
                        </xsl:element>
                </h1>
                <xsl:apply-templates select="ows:ServiceIdentification"/>


                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->

                <xsl:apply-templates select="wcs:Contents"/>
                <xsl:apply-templates select="wcs:ServiceMetadata"/>
                <xsl:apply-templates select="ows:OperationsMetadata"/>
                <xsl:apply-templates select="ows:ServiceProvider"/>


                <!-- ****************************************************** -->
                <!--                       Terminus Links                   -->
                <!--                                                        -->
                <!--                                                        -->
                <h2 align="left">OPeNDAP WCS Test Pages</h2>
                <div class= "medium">
                <ul>
                    <li>
                        <a href="{$WcsService}/test?service=WCS&amp;request=GetCapabilities">KVP Test Page</a>
                        - Parses a KVP request and returns a page
                        reporting any problems.
                    </li>
                    <br/>
                    <li>
                        <a href="{$WcsService}/echoXML?service=WCS&amp;request=GetCapabilities">Return KVP as XML</a>
                        - Translates a KVP encoded request into
                        an XML encoded version of the request.
                    </li>
                    <br/>
                </ul>
                <hr/>
                </div>
                <h2>WCS Request Form</h2>

                You may enter an XML encoded WCS query into the box below.
                <br/>
                A WCS response will be returned.

                <form action="{$WcsService}/form" method="post">
                    <p>
                        <textarea name="WCS_QUERY" rows="20" cols="80">Insert your WCS query here...</textarea>
                    </p>
                    <input type="submit" value="Send"/>
                    <input type="reset"/>
                </form>


                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <!-- div class="small" align="right">
                    OPeNDAP WCS Service development sponsored by <a href='http://www.ioos.gov/'>IOOS</a>
                </div -->
                <!-- ****************************************************** -->
                <!--         HERE IS THE HYRAX VERSION NUMBER               -->
                <!--                                                        -->
                <h3>WCS-<xsl:value-of select="$WcsServiceVersion"/>
                    <span class="small"> (OPeNDAP WCS service implementation version
                        <xsl:value-of select="$WcsSoftwareVersion"/>)</span>
                    <br />
                    <span class="uuid">
                        ServerUUID=e93c3d09-a5d9-49a0-a912-a0ca16430b91-WCS
                    </span>
                </h3>

            </body>
        </html>
    </xsl:template>


    <!-- ****************************************************** -->
    <!--         ows:ServiceIdentification                      -->
    <!--                                                        -->
    <xsl:template match="ows:ServiceIdentification">
        <div class="small">
            <xsl:apply-templates />
            <hr size="1" noshade="noshade"/>
            <xsl:apply-templates mode="profiles"/>
        </div>
    </xsl:template>



    <!-- ****************************************************** -->
    <!--         ows:ServiceProvider                            -->
    <!--                                                        -->
    <xsl:template match="ows:ServiceProvider">
        <h3>Service Provider</h3>

        <div class="medium">
            <xsl:apply-templates mode="provider"/>
        </div>

    </xsl:template>



    <!-- ****************************************************** -->
    <!--         ows:OperationsMetadata                         -->
    <!--                                                        -->
    <xsl:template match="ows:OperationsMetadata">
        <h3>WCS Operations Metadata</h3>

        <div class="medium">
                <xsl:apply-templates mode="operMetadata"/>
        </div>

    </xsl:template>



    <!-- ****************************************************** -->
    <!--         wcs:Contents                                   -->
    <!--                                                        -->
    <xsl:template match="wcs:Contents">
        <h3>Available Coverages</h3>
        <table border="0" width="100%" style="font-size: 14px; font-family: courier;" >
            <xsl:choose>
                <xsl:when test="wcs:CoverageSummary">
                    <xsl:apply-templates select="wcs:CoverageSummary"/>
                </xsl:when>
                <xsl:otherwise>
                    <tr>
                        <td align="left"><span class="bold_italic">No Coverages Found</span></td>
                        <!-- <th align="center">Description</th> -->
                        <td align="center">[----.--, ----.--] [----.--, ----.--]</td>
                    </tr>
                </xsl:otherwise>
            </xsl:choose>
        </table>



        <h3>Available EO DatasetSeries</h3>
        <table border="0" width="100%" style="font-size: 14px; font-family: courier;" >
            <xsl:choose>
                <xsl:when test="wcs:Extension/wcseo:DatasetSeriesSummary">
                    <xsl:apply-templates select="wcs:Extension/wcseo:DatasetSeriesSummary"/>
                </xsl:when>
                <xsl:otherwise>
                    <tr>
                        <td align="left"><span class="bold_italic">No EO DatasetSeries Found</span></td>
                        <!-- <th align="center">Description</th> -->
                        <td align="center">[----.--, ----.--] [----.--, ----.--]</td>
                    </tr>
                </xsl:otherwise>
            </xsl:choose>
        </table>










        <hr size="1" noshade="noshade"/>
        <!-- xsl:call-template name="ServerIDs"/ -->
        <xsl:if test="wcs:SupportedCRS">
            <table border="0" width="100%">
                <tr>
                    <th align="left">
                        Supported Coordinate Reference Systems:
                    </th>
                </tr>
                <tr>
                    <td>
                        <ul>
                            <xsl:apply-templates select="wcs:SupportedCRS"/>

                        </ul>
                    </td>
                </tr>
            </table>
        </xsl:if>
        <xsl:if test="wcs:SupportedFormat">
            <table border="0" width="100%">
                <tr> <th align="left">Supported Data Formats:</th></tr>
                <tr>
                    <td>
                        <ul>
                            <xsl:apply-templates select="wcs:SupportedFormat"/>

                        </ul>
                    </td>
                </tr>
            </table>
        </xsl:if>
    </xsl:template>


    <xsl:template match="wcs:CoverageSummary">

        <tr>
            <td align="left">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$WcsService"/>/describeCoverage?<xsl:value-of select="wcs:CoverageId"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="ows:Title">
                            <xsl:value-of select="ows:Title"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <b><xsl:value-of select="wcs:CoverageId"/></b>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
                <span class="small"> <xsl:apply-templates select="ows:Abstract"/> </span>

            </td>
            <td align="center">
                <xsl:apply-templates select="ows:BoundingBox"/>
            </td>
        </tr>

    </xsl:template>




    <!--
    <wcseo:DatasetSeriesSummary>
      <wcseo:DatasetSeriesId>MODIS_L3_chl-a</wcseo:DatasetSeriesId>
      <ows:WGS84BoundingBox>
        <ows:LowerCorner>-90.0  -180.0</ows:LowerCorner>
        <ows:UpperCorner>90.0  180.0</ows:UpperCorner>
      </ows:WGS84BoundingBox>
      <gml:TimePeriod xmlns:gml="http://www.opengis.net/gml/3.2" gml:id="MODIS_L3_chl-a_timePeriod">
        <gml:beginPosition>2002-07-02T17:00:00.0 -0700</gml:beginPosition>
        <gml:endPosition>2002-07-06T16:59:59.0 -0700</gml:endPosition>
      </gml:TimePeriod>
    </wcseo:DatasetSeriesSummary>

-->
    <xsl:template match="wcseo:DatasetSeriesSummary">

        <tr>
            <td align="left">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <!-- xsl:value-of select="$ServicePrefix"/>/describeCoverage?<xsl:value-of select="wcseo:DatasetSeriesId"/ -->
                        <xsl:value-of select="$WcsService"/>?service=WCS&amp;version=2.0.1&amp;request=DescribeEOCoverageSet&amp;eoId=<xsl:value-of select="wcseo:DatasetSeriesId"/>

                        <!--
                        http://localhost:8080/WCS-2.0?service=WCS&version=2.0.1&request=DescribeCoverage&coverageId=MODIS_AQUA_L3_CHLA_DAILY_4KM_R_002_nc4_min_eo
                         -->

                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="ows:Title">
                            <xsl:value-of select="ows:Title"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <b><xsl:value-of select="wcseo:DatasetSeriesId"/></b>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
                <span class="small"> <xsl:apply-templates select="ows:Abstract"/> </span>

            </td>
            <td align="center">
                <xsl:apply-templates select="ows:WGS84BoundingBox"/>
                <br/>
                <xsl:apply-templates select="gml:TimePeriod"/>
            </td>
        </tr>

    </xsl:template>

    <xsl:template match="gml:TimePeriod">
        begin: [<xsl:value-of select="gml:beginPosition"/>]
        <br />
        end: [<xsl:value-of select="gml:endPosition"/>]
    </xsl:template>



    <xsl:template match="ows:BoundingBox | ows:WGS84BoundingBox">

        <xsl:variable name="numberFormat">+000.00;-000.00</xsl:variable>
        <xsl:variable name="lowerLon">
            <xsl:value-of select="format-number(number(substring-before(ows:LowerCorner,' ')),$numberFormat,'CoordinateFormatter')"/>
        </xsl:variable>

        <xsl:variable name="lowerLat">
            <xsl:value-of select="format-number(number(substring-after(ows:LowerCorner,' ')),$numberFormat,'CoordinateFormatter')"/>
        </xsl:variable>

        <xsl:variable name="upperLon">
            <xsl:value-of select="format-number(number(substring-before(ows:UpperCorner,' ')),$numberFormat,'CoordinateFormatter')"/>
        </xsl:variable>

        <xsl:variable name="upperLat">
            <xsl:value-of select="format-number(number(substring-after(ows:UpperCorner,' ')),$numberFormat,'CoordinateFormatter')"/>
        </xsl:variable>

        <xsl:if test="@crs">
            <div class="small">crs=<xsl:value-of select="@crs"/></div>
        </xsl:if>

        [<xsl:value-of select="concat($lowerLat,', ',$lowerLon)"/>] [<xsl:value-of select="concat($upperLat,', ',$upperLon)"/>]

    </xsl:template>


    <xsl:template match="wcs:SupportedCRS">
        <li>
            <xsl:value-of select="."/>
        </li>
    </xsl:template>


    <xsl:template match="wcs:formatSupported">
        <dd><span class="small"><xsl:value-of select="."/></span></dd>
    </xsl:template>

    <xsl:template match="wcs:ServiceMetadata">
        <dl>
            <dt><span class="small_bold">Supported Formats</span></dt>
            <xsl:apply-templates select="wcs:formatSupported" />
        </dl>
    </xsl:template>



    <xsl:template match="ows:Keywords">
        <em>Keywords: </em>
        <xsl:apply-templates/>
        <br/>
    </xsl:template>

    <xsl:template match="ows:Keyword">
        [<xsl:value-of select="."/>]
    </xsl:template>

    <xsl:template match="ows:ServiceType">
        <em>Service: </em>
        <xsl:value-of select="."/>
        version
        <xsl:value-of select="../ows:ServiceTypeVersion"/>
        <br/>
    </xsl:template>

    <xsl:template match="*" mode="profiles"/>
    <xsl:template match="ows:Profile" mode="profiles">
        <em>Profile: </em> <xsl:value-of select="."/>
        <br/>
    </xsl:template>


    <xsl:template match="ows:Fees">
        <em>Fees: </em>
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>

    <xsl:template match="ows:AccessConstraints">
        <em>Access Constraints: </em>
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>



    <xsl:template match="ows:DCP" mode="operMetadata">
        <xsl:apply-templates mode="operMetadata"/>
    </xsl:template>

    <xsl:template match="ows:Value" mode="operMetadata">
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="ows:Operation" mode="operMetadata">
        <xsl:value-of select="local-name(.)"/>:
        <xsl:value-of select="@name"/>
        <ul>
            <xsl:apply-templates mode="operMetadata"/>
        </ul>
        <br/>
    </xsl:template>


    <xsl:template match="ows:Constraint" mode="operMetadata">
        <li><xsl:value-of select="@name"/>:

            <xsl:for-each select="ows:DefaultValue">
                [<xsl:value-of select="."/>],
            </xsl:for-each>
        </li>
    </xsl:template>



    <xsl:template match="ows:HTTP" mode="operMetadata">
        <xsl:for-each select="*">
            <li>
                HTTP
                <xsl:value-of select="concat(local-name(.),' ')"/>
                <xsl:if test="ows:Constraint">
                    (<xsl:value-of select="ows:Constraint/ows:AllowedValues/ows:Value"/>)
                </xsl:if>
                <span style="text-decoration: underline;">
                    <xsl:value-of select="./@xlink:href"/>
                </span>
            </li>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="ows:Parameter" mode="operMetadata">
        <li><xsl:value-of select="@name"/>:

            <xsl:for-each select="ows:AllowedValues/ows:Value">
                [<xsl:value-of select="."/>],
            </xsl:for-each>
        </li>
    </xsl:template>

    <xsl:template match="ows:Abstract">
        <xsl:value-of select="."/>
        <br/>
        <br/>
    </xsl:template>



    <xsl:template match="ows:ProviderSite" mode="providerDetail"> 
        <li>
            <em><xsl:value-of select="local-name(.)"/>: </em>
            <xsl:element name="a">
                <xsl:attribute name="href">
                    <xsl:value-of select="@xlink:href"/>
                </xsl:attribute>
                <xsl:value-of select="@xlink:href"/>
            </xsl:element>
        </li>
    </xsl:template>

    
    <xsl:template match="*" mode="providerDetail"> 
        <li>
            <em><xsl:value-of select="local-name(.)"/>: </em>
            <xsl:value-of select="."/>
        </li>
    </xsl:template>
    
    
    


    <xsl:template match="*" mode="provider">
        <xsl:choose>
            <xsl:when test="*">
                <li>
                    <em><xsl:value-of select="local-name(.)"/>: </em>
                </li>
                <ul>
                    <xsl:apply-templates mode="provider"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="." mode="providerDetail"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>









    <xsl:template match="@*|text()"/>


</xsl:stylesheet>