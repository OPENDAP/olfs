<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2013 OPeNDAP, Inc.
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
<!DOCTYPE xsl:stylesheet [
]>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"

        >
    <xsl:import href="version.xsl"/>
    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>


    <xsl:variable name="docsService">/s3/docs</xsl:variable>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

    <!-- Get the service definition from the key (a hash map) -->
    <xsl:variable name="dapServiceBase" select="key('service-by-name', 'dap')/@base" />



    <xsl:template match="thredds:catalog">
        <html>
            <head>
                <link rel='stylesheet' href='{$docsService}/css/contents.css'
                      type='text/css'/>
                <title>
                    <xsl:if test="@name"> <xsl:value-of select="@name"/> : </xsl:if><xsl:value-of select="thredds:dataset/@name"/>
                </title>

            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="Logo" src='{$docsService}/images/logo.gif'/>
                <h1>
                    <xsl:if test="@name">
                        <xsl:value-of select="@name"/> :
                    </xsl:if>

                    <xsl:value-of select="thredds:dataset/@name"/>



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
                            <th align="center">DAP2 Links</th>
                            <th align="center">DAP4 Links</th>
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
                            <div class="small" align="left">THREDDS Catalog XML presentation.</div>
                            <div class="small" align="left">Use 'View Page Source' to see XML content.</div>
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
                <h3><font size="0">OPeNDAP Hyrax </font><font class="small">(<xsl:value-of select="$HyraxVersion"/>)</font>

                    <br/>
                    <a href='{$docsService}/'>Documentation</a>
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
        <!-- xsl:param name="inheritedMetadata" / -->

        <!-- tr><td>catalogRef located</td><td>processing</td><td>BEGIN</td></tr -->

        <tr>
            <td align="left" >

                <xsl:value-of select="$indent"/>

                <a href="{./@xlink:href}" ><xsl:value-of select="./@xlink:title"/> /</a>

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
       -    <service name="OPeNDAP-Hyrax" serviceType="OPeNDAP" base="{$dapService}"/>
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
        <!-- xsl:param name="inheritedMetadata" / -->


        <!-- tr><td>dataset "<xsl:value-of select="@name"/>" located</td><td>processing</td><td>BEGIN</td></tr -->

        <xsl:variable name="datasetPositionInDocument">
            <xsl:value-of select="count(preceding::*)"/>
        </xsl:variable>

        <!--
        <h3>count(preceding::*): <xsl:value-of select="count(preceding::*)"/></h3>
        <h3>position():  <xsl:value-of select="position()"/></h3>

        -->

        <xsl:choose>
            <xsl:when test="boolean(thredds:dataset) or boolean(thredds:catalogRef)">
                <tr>
                    <td  class="dark" align="left">
                        <xsl:value-of select="$indent"/>
                            <xsl:choose>
                                <xsl:when test="@name='/'">Catalog of /</xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="@name"/>
                                </xsl:otherwise>
                            </xsl:choose>
                    </td>
                    <xsl:call-template name="NoSizeNoTime" />
                </tr>

                <!-- tr><td>dataset "<xsl:value-of select="@name" />" </td><td>contains child datasets or catalogRefs</td><td>calling apply-templates</td></tr -->

                <xsl:apply-templates>
                    <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
                    <!--
                      -   Note that the following parameter uses an XPath that
                      -   accumulates inherited thredds:metadata elements as it descends the
                      -   hierarchy.
                      -->
                    <!-- xsl:with-param name="inheritedMetadata" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]" /-->
                </xsl:apply-templates>

                <!-- tr><td>dataset "<xsl:value-of select="@name" />" </td><td>apply-templates</td><td>completed</td></tr -->

            </xsl:when>
            <xsl:otherwise>
                <!-- tr><td>current dataset</td><td>has no</td><td>dataset or catalogRef children</td></tr -->
                <tr>
                    <td class="dark">
                        <xsl:value-of select="$indent"/>

                        <!-- dl>
                            <dt>$dapServiceBase</dt><dd><xsl:value-of select="$dapServiceBase"/></dd>
                            <dt>@urlPath</dt><dd><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/></dd>
                        </dl -->

                        <a><xsl:attribute name="href"><xsl:value-of select="$dapServiceBase"/><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/>.html</xsl:attribute>
                            <xsl:value-of select="@name"/>
                        </a>


                    </td>
                    <xsl:call-template name="SizeAndTime" >
                        <xsl:with-param name="currentDataset" select="." />
                        <xsl:with-param name="metadata" select="thredds:metadata" />
                        <!--xsl:with-param name="inheritedMetadata" select="$inheritedMetadata[boolean($inheritedMetadata)]" / -->
                    </xsl:call-template>
                    <td align="center">
                        <a style="padding-right: 10px;"><xsl:attribute name="href"><xsl:value-of select="$dapServiceBase"/><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/>.dds</xsl:attribute>dds</a>
                        <a style="padding-right: 10px;"><xsl:attribute name="href"><xsl:value-of select="$dapServiceBase"/><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/>.das</xsl:attribute>das</a>
                        <a style="padding-right: 10px;"><xsl:attribute name="href"><xsl:value-of select="$dapServiceBase"/><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/>.html</xsl:attribute>request form</a>

                    </td>
                    <td align="center">
                        <a style="padding-right: 10px;"><xsl:attribute name="href"><xsl:value-of select="$dapServiceBase"/><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/>.dmr.xml</xsl:attribute>dmr</a>
                        <a style="padding-right: 10px;"><xsl:attribute name="href"><xsl:value-of select="$dapServiceBase"/><xsl:value-of select="thredds:access[./@serviceName='dap']/@urlPath"/>.dmr.html</xsl:attribute>request form</a>

                    </td>
                </tr>
                <xsl:apply-templates>
                    <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
                </xsl:apply-templates>


            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <xsl:template name="NoSizeNoTime" >
        <td align="center">
            --
        </td>
        <td align="center">
            --
        </td>
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
        <!-- xsl:param name="inheritedMetadata" /-->
        <!-- Do the Size -->
        <td class="small" align="center">
            <xsl:choose>

                <xsl:when test="$currentDataset/thredds:dataSize">
                    <xsl:value-of select="$currentDataset/thredds:dataSize" />&#160;<xsl:value-of select="$currentDataset/thredds:dataSize/@units" />
                </xsl:when>

                <xsl:when test="$metadata/thredds:dataSize">
                    <xsl:value-of select="$metadata/thredds:dataSize" />&#160;<xsl:value-of select="$metadata/thredds:dataSize/@units" />
                </xsl:when>

                <!-- xsl:when test="$inheritedMetadata/thredds:dataSize">
                    <xsl:value-of select="$inheritedMetadata/thredds:dataSize" />&#160;<xsl:value-of select="$inheritedMetadata/thredds:dataSize/@units" />
                </xsl:when -->

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

                <!-- xsl:when test="$inheritedMetadata/thredds:date">
                    <xsl:value-of select="$inheritedMetadata/thredds:date" />
                </xsl:when -->

                <xsl:otherwise>--</xsl:otherwise>
            </xsl:choose>
        </td>

    </xsl:template>




    <xsl:template match="thredds:*">
        <xsl:param name="indent" />
    </xsl:template>



</xsl:stylesheet>

