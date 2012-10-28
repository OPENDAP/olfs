<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2012 OPeNDAP, Inc.
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
  ~ // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  ~ //
  ~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
  ~ /////////////////////////////////////////////////////////////////////////////
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ows="http://www.opengis.net/ows/2.0"
                xmlns:wcs="http://www.opengis.net/wcs/2.0"
                xmlns:gml="http://www.opengis.net/gml/3.2"
        >

    <xsl:template match="/ows:WcsExceptionReport">
        <html>
            <head>
                <xsl:element name="link">
                    <xsl:attribute name="rel">stylesheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                    <xsl:attribute name="href"><xsl:value-of select="$ServicePrefix"/>/docs/css/contents.css</xsl:attribute>
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
                        <td><img alt="Institution Logo" src="{concat($ServicePrefix,'/docs/images/logo.gif')}" /></td>
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






</xsl:stylesheet>