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
                xmlns:gml="http://www.opengis.net/gml/3.2"
                xmlns:gmlcov="http://www.opengis.net/gmlcov/1.0"
                xmlns:swe="http://www.opengis.net/swe/2.0"
        >
    <xsl:param name="WcsService" />
    <xsl:param name="DocsService" />
    <xsl:param name="UpdateIsRunning"/>
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:strip-space elements="*"/>

    <xsl:variable name="WcsSoftwareVersion">@WCS_SOFTWARE_VERSION@</xsl:variable>
    <xsl:variable name="WcsServiceVersion">2.0</xsl:variable>

    <xsl:variable name="gmlMetadata" select="gml:metaDataProperty" />


    <xsl:template match="/wcs:CoverageDescription">
        <html>
            <head>
                <xsl:element name="link">
                    <xsl:attribute name="rel">stylesheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                    <xsl:attribute name="href"><xsl:value-of select="$DocsService"/>/css/contents.css</xsl:attribute>
                </xsl:element>
                <title>OPeNDAP: Web Coverage Service</title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <table border="0" width="90%">
                    <tr>
                        <td><img alt="Institution Logo" src="{concat($DocsService,'/images/logo.gif')}" /></td>
                        <td align="center">
                            <div  class="xlarge">Web Coverage Service</div>
                        </td>
                    </tr>
                </table>

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
                <h2>
                    <span align="left" class="small">wcs:</span>Coverage Description
                    <a href="{$WcsService}?service=WCS&amp;version=2.0.1&amp;request=DescribeCoverage&amp;coverageId={wcs:CoverageId}"><span class="small">XML</span></a>
                </h2>


                <dl>
                    <dt><span class="medium"><span class="small">gml:</span>id</span></dt>
                    <dd><span class="medium_bold"><xsl:value-of select="@gml:id" /></span></dd>
                </dl>

                <dl>
                    <dt><span class="medium"><span class="small">wcs:</span>CoverageId</span></dt>
                    <dd><span class="medium_bold"><xsl:value-of select="wcs:CoverageId" /></span></dd>
                </dl>





                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->



                <xsl:apply-templates />


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
    <!--         ows:Domain                                     -->
    <!--                                                        -->



    <xsl:template match="gml:metaDataProperty">
        <em>gml:metaDataProperty - </em>
        <xsl:apply-templates/>
        <br/>
    </xsl:template>

    <xsl:template match="gml:description">
        <em>gml:description - </em>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="gml:descriptionReference">
        <em>gml:descriptionReference - </em>
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>

    <xsl:template match="gml:identifier">
        <em>gml:identifier - </em>
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>

    <xsl:template match="gml:name">
        <em>gml:name - </em>
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>


    <xsl:template match="gml:boundedBy">
            <xsl:apply-templates/>
    </xsl:template>

    <!--
    <gml:Envelope axisLabels="lat long" srsDimension="2" srsName="http://www.opengis.net/def/crs/EPSG/0/4326"
                  uomLabels="deg deg">
    -->
    <xsl:template match="gml:Envelope | gml:EnvelopeWithTimePeriod">
        <h2>Bounding Box <span class="small">(gml:boundedBy)</span></h2>
        <dd><span class="medium_bold">SRS: <xsl:value-of select="@srsName" /></span></dd>
        <dd><span class="medium_bold">SRS Dimensions: <xsl:value-of select="@srsDimensions" /></span></dd>
        <dd><span class="medium_bold">Axis Labels: <xsl:value-of select="@axisLabels" /></span></dd>
        <dd><span class="medium_bold">Units Of Measure: <xsl:value-of select="@uomLabels" /></span></dd>

        <div class="medium">
            <xsl:apply-templates/>
        </div>
    </xsl:template>


    <xsl:template match="gml:location">
        <em>gml:location - </em>
        <xsl:apply-templates/>
        <br/>
    </xsl:template>


    <xsl:template match="wcs:CoverageId">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="gml:coverageFunction">
        <em>gml:coverageFunction - </em>
        <xsl:apply-templates/>
        <br/>
    </xsl:template>

    <xsl:template match="gmlcov:metadata">
        <em>gml:coverageMetadata - </em>
        <xsl:apply-templates/>
        <br/>
    </xsl:template>

    <xsl:template match="gml:domainSet">
        <h2>Domain Set <span class="small">(gml:domainSet)</span></h2>

        <div class="medium">
            <xsl:apply-templates/>
        </div>

    </xsl:template>

    <xsl:template match="gmlcov:rangeType">
        <h2>Range Set <span class="small">(gmlcov:rangeType)</span></h2>
        <xsl:apply-templates/>
    </xsl:template>



    <xsl:template match="wcs:ServiceParameters">
        <h2>ServiceParameters</h2>
        <table>
            <xsl:apply-templates/>
        </table>
    </xsl:template>

    <xsl:template match="wcs:CoverageSubtype">
        <tr>
            <td><span class="small">Coverage Subtype: </span></td>
            <td><span style="font-size: 14px;"><code><xsl:value-of select="."/></code></span><br/></td>
        </tr>
    </xsl:template>

    <xsl:template match="wcs:CoverageSubtypeParent">
        <em>wcs:Coverage Subtype Parent: </em>
        <ul>
            <xsl:apply-templates/>
        </ul>
        <br/>
    </xsl:template>

    <xsl:template match="wcs:nativeFormat">
        <tr>
            <td><span class="small">Native Format: </span></td>
            <td><span style="font-size: 14px;"><code><xsl:value-of select="."/></code></span><br/></td>
        </tr>
    </xsl:template>

    <xsl:template match="wcs:Extension">
        <em>wcs:Extension - </em>
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>


    <xsl:template match="swe:DataRecord">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="swe:field">
        <strong><span class="xlarge"><xsl:value-of select="@name"/></span></strong> :
        <xsl:apply-templates mode="fieldDescription"/>
        <span class="small">
            <xsl:apply-templates />
        </span>
        <br/>
    </xsl:template>


    <xsl:template match="@*|text()" mode="fieldDescription"/>

    <xsl:template match="*" mode="fieldDescription">
        <xsl:apply-templates mode="fieldDescription"/>
    </xsl:template>

    <xsl:template match="swe:description" mode="fieldDescription">
        <xsl:value-of select="."/>
        <br/>
    </xsl:template>



    <xsl:template match="swe:uom">
       <dd> <em>Units of Measure: </em> <xsl:value-of select="@code"/></dd>
    </xsl:template>




    <xsl:template match="swe:AllowedValues">
        <dd> <em>Allowed Values: </em> <xsl:value-of select="swe:interval"/> </dd>
    </xsl:template>









    <xsl:template match="swe:Quantity | swe:constraint | swe:description">
        <xsl:apply-templates />
    </xsl:template>





    <xsl:template match="/ows:WcsExceptionReport">
        <html>
            <head>
                <xsl:element name="link">
                    <xsl:attribute name="rel">stylesheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                    <xsl:attribute name="href"><xsl:value-of select="$DocsService"/>/css/contents.css</xsl:attribute>
                </xsl:element>
                <title>OPeNDAP: Web Coverage Service Exception</title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <table border="0" width="90%">
                    <tr>
                        <td><img alt="Institution Logo" src="{concat($DocsService,'/images/logo.gif')}" /></td>
                        <td align="center">
                            <div  class="xlarge">Web Coverage Service Exception</div>
                        </td>
                    </tr>
                </table>




                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->


                <xsl:apply-templates />

                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <div class="small" align="right">
                    OPeNDAP WCS Service development sponsored by <a href='http://www.ioos.gov/'>IOOS</a>
                </div>
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


    <xsl:template match="ows:Exception">
        <em>@exceptionCode - </em>
        <xsl:value-of select="@exceptionCode"/>
        <br/>
        <em>@locator - </em>
        <xsl:value-of select="@locator"/>
        <br/>
        <div class="medium"><xsl:value-of select="ows:ExceptionText"/></div>
    </xsl:template>





    <xsl:template match="*">
        <xsl:choose>
            <xsl:when test="*">
                <li>
                    <em><xsl:value-of select="local-name()"/>:
                    </em>
                </li>
                <ul>
                    <xsl:apply-templates />
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