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

                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <pre>


                    <table border="0" width="100%">
                        <tr>
                            <th align="left">Dataset</th>
                            <th align="center">ResponseLinks</th>
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
        <tr>
            <td align="left" >

                <xsl:if test="substring(./@xlink:href,string-length(./@xlink:href) - 3)='.xml'">
                    <a href="{concat(substring(./@xlink:href,1,string-length(./@xlink:href) - 4),'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
                </xsl:if>

                <xsl:if test="not(substring(./@xlink:href,string-length(./@xlink:href) - 3))">
                    <a href="{./@xlink:href}" ><xsl:value-of select="./@xlink:title"/> /</a>
                </xsl:if>

            </td>
            <xsl:call-template name="NoServiceLinks" />
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
    <xsl:template match="thredds:catalogRef" mode="EasierToReadVersion">
        <xsl:param name="indent" />
        <tr>
            <td align="left">

                <xsl:variable name="href" select="./@xlink:href" />
                <xsl:variable name="linkSuffix" select="substring($href,string-length($href) - 3)" />
                <xsl:variable name="linkBody" select="substring($href,1,string-length($href) - 4)" />

                <xsl:if test="$linkSuffix='.xml'">
                    <xsl:value-of select="$indent"/><a href="{concat($linkBody,'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
               </xsl:if>

                <xsl:if test="not($linkSuffix='.xml')">
                    <xsl:value-of select="$indent"/><a href="{$href}" ><xsl:value-of select="./@xlink:title"/> /</a>
               </xsl:if>
            </td>
            <xsl:call-template name="NoServiceLinks" />
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

            <xsl:call-template name="NoServiceLinks" />
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
    <xsl:template match="thredds:datasetScan" mode="empty">
        <xsl:param name="indent" />
        <xsl:apply-templates />
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
                <xsl:call-template name="NoServiceLinks" />
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
                <td>
                    <!-- I don't like this next line. Should this really be a link? Why not just in bold...-->
                    <!-- <tr><td><xsl:value-of select="$indent"/><a href="{key('service-by-name', 'OPeNDAP-Hyrax')/@base}{@urlPath}.html" ><xsl:value-of select="@name" /></a></td></tr> -->
                    <tr>
                        <td class="dark"><xsl:value-of select="$indent"/><xsl:value-of select="@name" /></td>
                        <xsl:call-template name="NoServiceLinks" />
                    </tr>

                    <!-- <tr><td class="dark"><xsl:value-of select="$indent"/><xsl:call-template name="generateXPath" /></td></tr> -->


                    <xsl:apply-templates select="thredds:serviceName" mode="ServiceLinks" >
                        <xsl:with-param name="indent"><xsl:value-of select="$indent" /></xsl:with-param>
                        <xsl:with-param name="currentDataset" select="." />
                    </xsl:apply-templates>

                    <xsl:if test="boolean($inheritedMetadata)" >
                        <xsl:apply-templates select="$inheritedMetadata/thredds:serviceName" mode="ServiceLinks" >
                                <xsl:with-param name="indent"><xsl:value-of select="$indent" /></xsl:with-param>
                                <xsl:with-param name="currentDataset" select="." />
                        </xsl:apply-templates>
                    </xsl:if>

                </td>
            </tr>
        </xsl:if>

    </xsl:template>

<xsl:template name="generateXPath"><xsl:for-each select="ancestor::*">/<xsl:value-of select="name()"/>[<xsl:number/>]</xsl:for-each>/<xsl:value-of select="name()"/>[<xsl:number/>]</xsl:template>


    <xsl:template match="thredds:serviceName" mode="ServiceLinks" >
        <xsl:param name="indent" />
        <xsl:param name="currentDataset" />

        <xsl:apply-templates select="key('service-by-name', .)" mode="ServiceLinks" >
            <xsl:with-param name="indent"><xsl:value-of select="$indent" /></xsl:with-param>
            <xsl:with-param name="currentDataset" select="$currentDataset" />
        </xsl:apply-templates>


    </xsl:template>



    <xsl:template name="NoServiceLinks" >
            <td align="center">
                &#160; - &#160; - &#160; - &#160; - &#160; - &#160;&#160;
            </td>
    </xsl:template>







    <xsl:template match="thredds:service" mode="ServiceLinks" >
        <xsl:param name="indent" />
        <xsl:param name="currentDataset" />
        <tr>
            <td>
                <xsl:value-of select="$indent"/>&#160;&#160;<xsl:value-of select="./@name" />  <font size="1pt"> (<xsl:value-of select="./@serviceType" />)</font>
            </td>

            <xsl:if test="./@serviceType[.='Compound']" >

                <xsl:apply-templates select="./thredds:service" mode="ServiceLinks" >
                        <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
                        <xsl:with-param name="currentDataset" select="$currentDataset" />
                </xsl:apply-templates>

            </xsl:if>


            <!-- Produces service URL's for the OPeNDAP serviceType -->
            <xsl:if test="./@serviceType[.='OPeNDAP'] |
                          ./@serviceType[.='OPENDAP'] |
                          ./@serviceType[.='OpenDAP'] |
                          ./@serviceType[.='OpenDap'] |
                          ./@serviceType[.='openDap'] |
                          ./@serviceType[.='opendap']"
                          >

                <td align="center">
                    <a href="{./@base}{$currentDataset/@urlPath}.ddx" >ddx</a>
                    <a href="{./@base}{$currentDataset/@urlPath}.dds" >dds</a>
                    <a href="{./@base}{$currentDataset/@urlPath}.das" >das</a>
                    <a href="{./@base}{$currentDataset/@urlPath}.info" >info</a>
                    <a href="{./@base}{$currentDataset/@urlPath}.html" >html</a>
                </td>

            </xsl:if>

             <!-- Produces service URL's for the WCS serviceType -->
           <xsl:if test="./@serviceType[.='WCS']" >
                <td align="center">
                    <a href="{./@base}{$currentDataset/@urlPath}.ddx" >CoverageDescription</a>
                </td>
            </xsl:if>


             <!-- Produces service URL's for the HTTPServer serviceType -->
           <xsl:if test="./@serviceType[.='HTTPServer']" >
                <td align="center">
                    <a href="{./@base}{$currentDataset/@urlPath}" >File Download</a>
                </td>
            </xsl:if>




        </tr>
    </xsl:template>


    <!--***********************************************
      <xsd:group name="threddsMetadataGroup">
          <xsd:choice>
                <xsd:element name="documentation" type="documentationType"/>
                <xsd:element ref="metadata"  />
                <xsd:element ref="property" />

                <xsd:element ref="contributor"/>
                <xsd:element name="creator" type="sourceType"/>
                <xsd:element name="date" type="dateTypeFormatted" />
                <xsd:element name="keyword" type="controlledVocabulary" />
                <xsd:element name="project" type="controlledVocabulary" />
                <xsd:element name="publisher" type="sourceType"/>

                <xsd:element ref="geospatialCoverage"/>
                <xsd:element name="timeCoverage" type="timeCoverageType"/>
                <xsd:element ref="variables"/>

                <xsd:element name="dataType" type="dataTypes"/>
                <xsd:element name="dataFormat" type="dataFormatTypes"/>
                <xsd:element name="serviceName" type="xsd:string" />
                <xsd:element name="authority" type="xsd:string" />
               <xsd:element ref="dataSize"/>
            </xsd:choice>
       </xsd:group>
     -->



    <xsl:template match="thredds:documentation">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:metadata">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:property">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:contributor">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:creator">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:date">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:keyword">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:project">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:publisher">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:geospatialCoverage">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:timeCoverage">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:variables">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:dataType">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:dataFormat">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:serviceName">
        <xsl:param name="indent" />
    </xsl:template>

    <xsl:template match="thredds:authority">
        <xsl:param name="indent" />
    </xsl:template>




</xsl:stylesheet>

