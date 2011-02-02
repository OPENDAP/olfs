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
<!DOCTYPE xsl:stylesheet [
]>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:ncml="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
                xmlns:xlink="http://www.w3.org/1999/xlink"

                >
    <xsl:import href="version.xsl"/>
    <xsl:param name="dapService"/>
    <xsl:param name="docsService"/>
    <xsl:param name="remoteHost" />
    <xsl:param name="remoteRelativeURL" />
    <xsl:param name="remoteCatalog" />
    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>



    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

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
                    <xsl:if test="@name"> <xsl:value-of select="@name"/> : </xsl:if><xsl:value-of select="thredds:dataset/@name"/>
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

                <xsl:if test="$remoteCatalog">
                    <h3>Remote Catalog:
                        <xsl:value-of select="$remoteCatalog"/>
                    </h3>
                </xsl:if>

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

                            <xsl:choose>
                                <xsl:when test="$remoteCatalog">
                                    <a href="{$remoteCatalog}">XML</a>
                                </xsl:when>

                                <xsl:otherwise>
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
                                </xsl:otherwise>
                            </xsl:choose>

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

        <xsl:if test="not($remoteHost)">
            <tr>
                <td align="left" >

                    <!-- If the href ends in .xml, change it to .html
                         so the link in the presentation points to
                         another HTML page. -->
                    <xsl:value-of select="$indent"/>

                    <xsl:choose>

                        <!-- Does it point towards a remote catalog?. -->
                        <!--
                        <xsl:when test="starts-with(@xlink:href,'http://')">
                            <a>
                            <xsl:attribute name="href">?browseCatalog=<xsl:value-of select="@xlink:href" /></xsl:attribute>
                                <xsl:choose>
                                    <xsl:when test="@xlink:title"><xsl:value-of select="./@xlink:title"/>/</xsl:when>
                                    <xsl:otherwise>Link</xsl:otherwise>
                                </xsl:choose>
                            </a>
                        </xsl:when>
                        -->

                        <!-- Does it end in '.xml'? --> <!--Then replace that with '.html'   -->
                        <xsl:when test="substring(./@xlink:href,string-length(./@xlink:href) - 3)='.xml'">
                            <!--Then replace that with '.html'   -->
                            <a href="{concat(substring(./@xlink:href,1,string-length(./@xlink:href) - 4),'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
                        </xsl:when>

                        <!-- Since it doesn't end in .xml we don't know how to promote it, so leave it be. -->
                        <xsl:otherwise>
                            <a href="{./@xlink:href}" ><xsl:value-of select="./@xlink:title"/> /</a>
                        </xsl:otherwise>
                    </xsl:choose>

                </td>
                <xsl:call-template name="NoSizeNoTime" />
            </tr>
        </xsl:if>


        <!--

        <xsl:if test="$remoteHost">
            <tr>
                <td align="left" >

                    <ul class="small">
                        <li><em>xlink:href: </em><xsl:value-of select="@xlink:href" /></li>
                        <li><em>remoteHost: </em><xsl:value-of select="$remoteHost" /></li>
                        <li><em>remoteRelativeURL: </em><xsl:value-of select="$remoteRelativeURL" /></li>
                    </ul>


                    <xsl:value-of select="$indent"/>
                    <a>
                    <xsl:choose>
                        <xsl:when test="starts-with(@xlink:href,'http://')">
                            <xsl:attribute name="href">?browseCatalog=<xsl:value-of select="@xlink:href" /></xsl:attribute>
                        </xsl:when>
                        <xsl:when test="starts-with(@xlink:href,'/')">
                            <xsl:attribute name="href">?browseCatalog=<xsl:value-of select="$remoteHost"/><xsl:value-of select="@xlink:href" /></xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="href">?browseCatalog=<xsl:value-of select="$remoteRelativeURL"/><xsl:value-of select="@xlink:href" /></xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:value-of select="./@xlink:title"/>/
                    </a>
                </td>
                <xsl:call-template name="NoSizeNoTime" />
            </tr>
        </xsl:if>

        -->

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

                ERROR! thredds:datasetScan element should not be reaching this style sheet!!<br />
                Offending Element (view Source):<br />
                    <xsl:copy-of select="."/>


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






      <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
        <aggregation dimName="time" type="joinExisting" recheckEvery="720 min">
          <variableAgg name="MHchla" />
          <scan location="/u00/satellite/MH/chla/8day/" suffix=".nc" />
        </aggregation>
      </netcdf>

       -
     -->
    <xsl:template match="ncml:netcdf" >
        <xsl:param name="indent" />
        <tr>
            <td align="left">
                <xsl:value-of select="$indent"/>NcML
                <xsl:apply-templates >
                    <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
                </xsl:apply-templates>
            </td>

            <xsl:call-template name="NoSizeNoTime" />

        </tr>
    </xsl:template>


    <xsl:template match="ncml:aggregation" >
        <xsl:param name="indent" />
                - Aggregation<br/>
                <xsl:value-of select="$indent"/>Dimension:  <xsl:value-of select="@dimName"/><br/>
                <xsl:value-of select="$indent"/>Type:<xsl:value-of select="@type"/><br/>
                <xsl:value-of select="$indent"/>Rescan Interval: <xsl:value-of select="@recheckEvery"/><br/>
        <xsl:apply-templates >
            <xsl:with-param name="indent"><xsl:value-of select="$indent" /></xsl:with-param>
        </xsl:apply-templates>

    </xsl:template>

    <xsl:template match="ncml:variableAgg" >
        <xsl:param name="indent" />
                <xsl:value-of select="$indent"/>Aggregation Variable: <xsl:value-of select="@name"/><br/>
    </xsl:template>


    <xsl:template match="ncml:scan" >
        <xsl:param name="indent" />
                <xsl:value-of select="$indent"/>Scan Location: <xsl:value-of select="@location"/><br/>
                <xsl:value-of select="$indent"/>Scan File Suffix: <xsl:value-of select="@suffix"/><br/>
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
        <xsl:param name="inheritedMetadata" />


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
                        <a>
                            <xsl:if test="$remoteCatalog">
                                <xsl:attribute name="href">?browseDataset=<xsl:value-of select="$datasetPositionInDocument"/>&amp;<xsl:value-of select="$remoteCatalog"/></xsl:attribute>
                            </xsl:if>
                            <xsl:if test="not($remoteCatalog)">
                                <xsl:attribute name="href">?dataset=<xsl:value-of select="$datasetPositionInDocument"/></xsl:attribute>
                            </xsl:if>

                            <xsl:choose>
                                <xsl:when test="@name='/'">
                                    Catalog of /
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="@name"/>/
                                </xsl:otherwise>
                            </xsl:choose>
                        </a>
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


            </xsl:when>
            <xsl:otherwise>
                <tr>
                    <td class="dark">

                        <xsl:value-of select="$indent"/>
                        <a>
                            <xsl:if test="$remoteCatalog">
                                <xsl:attribute name="href">?browseDataset=<xsl:value-of select="$datasetPositionInDocument"/>&amp;<xsl:value-of select="$remoteCatalog"/></xsl:attribute>
                            </xsl:if>
                            <xsl:if test="not($remoteCatalog)">
                                <xsl:attribute name="href">
                                    ?dataset=<xsl:value-of select="$datasetPositionInDocument"/>
                                </xsl:attribute>
                            </xsl:if>

                            <xsl:value-of select="@name"/>
                        </a>

                    </td>
                    <xsl:call-template name="SizeAndTime" >
                        <xsl:with-param name="currentDataset" select="." />
                        <xsl:with-param name="metadata" select="thredds:metadata" />
                        <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata[boolean($inheritedMetadata)]" />
                    </xsl:call-template>
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

