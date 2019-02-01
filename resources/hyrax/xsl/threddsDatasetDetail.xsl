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
<!DOCTYPE xsl:stylesheet [
]>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"

                >
    <xsl:import href="version.xsl"/>
    <xsl:import href="threddsMetadataDetail.xsl" />

    <xsl:param name="serviceContext"/>
    <xsl:param name="docsService"/>
    <xsl:param name="targetDataset" />
    <xsl:param name="remoteCatalog" />
    <xsl:param name="remoteRelativeURL" />
    <xsl:param name="remoteHost" />
    <xsl:param name="typeMatch" />
    <xsl:param name="userId" />
    <xsl:param name="loginLink" />
    <xsl:param name="logoutLink" />


    <xsl:variable name="debug" select="false()"/>

    <xsl:variable name="indentIncrement" select="10"/>


    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>
    <xsl:key name="service-by-type" match="//thredds:service" use="lower-case(@serviceType)"/>



    <xsl:template match="thredds:catalog">
        <html>
            <xsl:if test="$debug">Target Dataset: <xsl:value-of select="$targetDataset"/><br/></xsl:if>
            <xsl:apply-templates />
        </html>
    </xsl:template>

    <xsl:template match="thredds:dataset">
        <xsl:param name="inheritedMetadata" />

        <xsl:variable name="datasetPositionInDocument">
            <xsl:value-of select="count(preceding::*) + count(ancestor::*)"/>
        </xsl:variable>

        <xsl:if test="$debug">Processing Dataset: <xsl:value-of select="$datasetPositionInDocument"/><br/></xsl:if>
        <xsl:choose>

            <!-- Is this the dataset that we are supposed to summary? -->
            <xsl:when test="$targetDataset=$datasetPositionInDocument">

                <!-- todo - Evaluate weather we want to drop the passed inheritedMetadata parameter in favor of the XPath expression

                <xsl:variable name="im" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]"/>
                <xsl:variable name="im_ancestor" select="ancestor::thredds:dataset/thredds:metadata[./@inherited='true']/thredds:serviceName"/>

                <IM>
                    <xsl:copy-of select="$im"/>
                </IM>

                <IM_ANCESTOR>
                    <xsl:copy-of select="$im_ancestor"/>
                </IM_ANCESTOR>

                -->


                <xsl:call-template name="targetDatasetPage">
                <xsl:with-param name="inheritedMetadata" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]" />
                </xsl:call-template>
            </xsl:when>

        <xsl:otherwise >
            <xsl:apply-templates>
                <!--
                  -   Note that the following parameter uses an XPath that
                  -   accumulates inherited thredds:metadata elements as it descends the
                  -   hierarchy.
                  -->
                <xsl:with-param name="inheritedMetadata" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]" />
            </xsl:apply-templates>
         </xsl:otherwise >

        </xsl:choose>

    </xsl:template>


    <!-- ******************************************************
      -  targetDatasetPage template
     -->
    <xsl:template name="targetDatasetPage" >

        <xsl:param name="inheritedMetadata" />

                <!-- ****************************************************** -->
                <!--                      PAGE HEADER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <head>

                    <link rel="stylesheet" href="{$docsService}/css/contents.css" type="text/css"/>
                    <link rel="stylesheet" href="{$docsService}/css/treeView.css" type="text/css"/>
                    <xsl:element name="script">
                        <xsl:attribute name="type">text/javascript</xsl:attribute>
                        <xsl:attribute name="src"><xsl:value-of select="$serviceContext"/>/js/CollapsibleLists.js</xsl:attribute>
                        <xsl:value-of select="' '"/>
                    </xsl:element>
                    <xsl:element name="script">
                        <xsl:attribute name="type">text/javascript</xsl:attribute>
                        <xsl:attribute name="src"><xsl:value-of select="$serviceContext"/>/js/StringToHex.js</xsl:attribute>
                        <xsl:value-of select="' '"/>
                    </xsl:element>

                    <title>THREDDS Dataset: <xsl:value-of select="@name"/></title>

                </head>

                <body>
                    <!-- div class="small">serviceContext: '<xsl:value-of select="$serviceContext"/>'</div -->
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
                                    <xsl:otherwise>
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
                    <table width="100%">
                        <tr>
                            <td width="30%" align="left">
                                <img alt="Logo" src='{$docsService}/images/logo.png'/>
                            </td>
                            <td class="dark" align="left">Hyrax - THREDDS Dataset Detail</td>
                        </tr>
                    </table>

                    <xsl:if test="$debug">
                        <span class="small"> typeMatch: <xsl:copy-of select="$typeMatch"/> </span>
                    </xsl:if>
                    <h1>
                        <div>
                            <xsl:choose>
                                <xsl:when test="@name">
                                    <xsl:value-of select="@name"/>
                                </xsl:when>
                                <xsl:when test="@ID">
                                    <xsl:value-of select="@ID"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <span class="italic">
                                        <span class="medium">
                                            Dataset Missing ID and name attributes.
                                        </span>
                                    </span>
                                </xsl:otherwise>
                            </xsl:choose>
                        </div>
                        <xsl:if test="/thredds:catalog/thredds:service">
                            <div class="small" align="left">
                                <div class="tightView" style="padding-left: 15px;">
                                    <ul class="collapsibleList">
                                        <li>
                                            <span class="small_bold" style="color: black;">Catalog Services</span>
                                            <ul>

                                                <table>
                                                    <tr>
                                                        <th class="small"><u>Service Name</u></th>
                                                        <th class="small"><u>Service Type</u></th>
                                                        <th class="small"><u>Service Base</u></th>
                                                    </tr>

                                                    <xsl:apply-templates select="/thredds:catalog/thredds:service" mode="banner">
                                                        <xsl:with-param name="indent" select="0"/>
                                                    </xsl:apply-templates>
                                                </table>

                                            </ul>
                                        </li>
                                    </ul>
                                </div>
<!--
                                <div class="tightView" style="padding-left: 15px;">
                                    <ul class="collapsibleList">
                                        <li>
                                                <span class="small_bold" style="color: black;">Catalog Services</span>
                                                <ul>

                                                    <table>
                                                        <tr>
                                                            <th class="small"><u>Service Name</u></th>
                                                            <th class="small"><u>Service Type</u></th>
                                                            <th class="small"><u>Service Base</u></th>
                                                        </tr>

                                                        <xsl:apply-templates select="/thredds:catalog/thredds:service" mode="banner">
                                                            <xsl:with-param name="indent" select="0"/>
                                                        </xsl:apply-templates>
                                                    </table>

                                                </ul>
                                        </li>
                                    </ul>
                                </div>
-->
                            </div>
                        </xsl:if>


                    </h1>

                    <xsl:variable name="sizeTest" select="thredds:dataSize" />
                    <xsl:variable name="dateTest" select="thredds:date |
                                    thredds:metadata/thredds:date |
                                    $inheritedMetadata[boolean($inheritedMetadata)]/thredds:date" />

                    <table>
                        <xsl:if test="$debug">
                            <tr>
                                <td>
                                    <span class="small_italic">ID:</span>
                                </td>
                                <td>
                                    <span class="medium_bold" style="padding-left: 5px;">
                                        <xsl:value-of select="@ID"/>
                                    </span>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <span class="small_italic">name:</span>
                                </td>
                                <td>
                                    <span class="medium_bold" style="padding-left: 5px;">
                                        <xsl:value-of select="@name"/>
                                    </span>
                                </td>
                            </tr>
                        </xsl:if>

                        <xsl:if test="$sizeTest" >
                            <tr>
                                <td class="small_italic">size:</td>
                                <td class="small_bold" style="padding-left: 5px;">
                                    <xsl:apply-templates select="$sizeTest" mode="sizeDetail">
                                        <xsl:with-param name="indent" select="0"/>
                                    </xsl:apply-templates>
                                </td>
                            </tr>
                        </xsl:if>

                        <xsl:if test="$dateTest" >
                            <tr>
                                <td class="small_italic">date:</td>
                                <td class="small_bold" style="padding-left: 5px;">
                                    <xsl:apply-templates select="$dateTest" mode="dateDetail">
                                        <xsl:with-param name="indent" select="0"/>
                                    </xsl:apply-templates>
                                </td>
                            </tr>
                        </xsl:if>


                    <tr>

                            <xsl:choose>
                                <xsl:when test="$remoteCatalog" >
                                    <td class="small_italic">
                                        <div title="The origin server's THREDDS catalog on which this dataset resides.">
                                            Remote<br/>Catalog:
                                        </div>
                                    </td>
                                    <td id="catalog_linky_poo" class="small_bold" style="color: black;padding-left: 5px;"> </td>

                                    <a href="{replace($remoteCatalog, '\.xml$', '.html')}" title="Source Catalog as HTML">HTML</a>
                                    <a href="{$remoteCatalog}" title="Source Catalog As XML">XML</a>

                                </xsl:when>
                                <xsl:otherwise>
                                    <td class="small_italic">Catalog:</td>
                                    <td id="catalog_linky_poo" class="small_bold" style="color: black;"> </td>
                                    <xsl:element name="script">
                                        <xsl:attribute name="type">text/javascript</xsl:attribute>
                                        var catalog = location.href.split("?");
                                        var catalog_link = document.createElement('a');
                                        var linkText = document.createTextNode(catalog[0]);
                                        catalog_link.appendChild(linkText);
                                        catalog_link.title = catalog[0];
                                        catalog_link.href = catalog[0];
                                        catalog_link.class = "small_bold"
                                        document.getElementById("catalog_linky_poo").appendChild(catalog_link);
                                    </xsl:element>

                                </xsl:otherwise>
                            </xsl:choose>
                    </tr>
                    </table>

                    <hr size="1" noshade="noshade"/>

                    <!-- ****************************************************** -->
                    <!--                 PAGE BODY CONTENT                      -->
                    <!--                                                        -->
                    <!--                                                        -->


                    <xsl:choose>
                        <xsl:when test="thredds:access | @urlPath">
                            <xsl:call-template name="doServiceLinks">
                                <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata" />
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:when test="thredds:dataset">
                            <xsl:apply-templates />
                        </xsl:when>
                        <xsl:when test="thredds:catalogRef ">
                            <xsl:apply-templates select="thredds:catalogRef" mode="ServiceLinks"/>
                        </xsl:when>
                        <xsl:otherwise>No Access, No Child Catalogs.</xsl:otherwise>
                    </xsl:choose>

                    <h2>MetaData Summary:</h2>

                    <xsl:variable name="docTest" select="thredds:documentation |
                                        thredds:metadata/thredds:documentation |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:documentation" />
                    <xsl:if test="$docTest" >
                        <p>
                            <div class="medium_bold">Documentation: </div>
                            <span class="small">
                                <xsl:apply-templates select="$docTest" mode="documentationDetail" >
                                    <xsl:with-param name="indent" select="$indentIncrement"/>
                                </xsl:apply-templates>
                            </span>
                        </p>
                    </xsl:if>




                    <xsl:variable name="timeCoverageTest" select="thredds:timeCoverage |
                                        thredds:metadata/thredds:timeCoverage |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:timeCoverage" />
                    <xsl:if test="$timeCoverageTest" >
                        <p>
                            <div class="medium_bold">Time Coverage: </div>
                            <span class="small">
                                <xsl:apply-templates select="$timeCoverageTest" mode="timeCoverageDetail"  >
                                    <xsl:with-param name="indent" select="$indentIncrement"/>
                                </xsl:apply-templates>
                            </span>
                        </p>
                    </xsl:if>



                    <xsl:variable name="geoCvrTest" select="thredds:geospatialCoverage |
                                        thredds:metadata/thredds:geospatialCoverage |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:geospatialCoverage" />
                    <xsl:if test="$geoCvrTest" >
                        <p>
                            <div class="medium_bold">Geospatial Coverage: </div>
                            <span class="small">
                                <xsl:apply-templates select="$geoCvrTest" mode="geospatialCoverageDetail"  >
                                    <xsl:with-param name="indent" select="$indentIncrement"/>
                                </xsl:apply-templates>
                            </span>
                        </p>
                    </xsl:if>

                    <xsl:variable name="creatorTest" select="thredds:creator |
                                        thredds:metadata/thredds:creator |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:creator" />
                    <xsl:if test="$creatorTest" >
                        <p>
                            <div class="medium_bold">Creators: </div>
                            <span class="small">
                                <xsl:apply-templates select="$creatorTest" mode="creatorDetail"  >
                                    <xsl:with-param name="indent" select="$indentIncrement"/>
                                </xsl:apply-templates>
                            </span>
                        </p>
                    </xsl:if>


                    <xsl:variable name="publisherTest" select="thredds:publisher |
                                        thredds:metadata/thredds:publisher |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:publisher" />
                    <xsl:if test="$publisherTest" >
                        <p>
                            <div class="medium_bold">Publishers: </div>
                            <span class="small">
                                <xsl:apply-templates select="$publisherTest" mode="publisherDetail"  >
                                    <xsl:with-param name="indent" select="$indentIncrement"/>
                                </xsl:apply-templates>
                            </span>
                        </p>
                    </xsl:if>


                    <xsl:variable name="propTest" select="thredds:property |
                                        thredds:metadata/thredds:property |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:property" />
                    <xsl:if test="$propTest" >
                        <p>
                            <div class="medium_bold">Properties: </div>
                            <span class="small">
                                <xsl:apply-templates  select="$propTest" mode="propertyDetail" >
                                    <xsl:with-param name="indent" select="$indentIncrement"/>
                                </xsl:apply-templates>
                            </span>
                        </p>
                    </xsl:if>


                    <hr size="1" noshade="noshade"/>


                    <h2>Metadata Detail: </h2>



                    <ul class="collapsibleList" style="font-size: 10px;">
                        <li>
                            <div class="tightView">
                                <span class="small_bold">Dataset Metadata</span>
                                <ul>
                                    <xsl:apply-templates select="." mode="metadataDetail" >
                                        <xsl:with-param name="indent" select="0"/>
                                        <xsl:with-param name="currentDataset" select="." />
                                    </xsl:apply-templates>

                                </ul>
                            </div>

                        </li>


                    </ul>



                    <xsl:variable name="metadataTest" select="$inheritedMetadata[boolean($inheritedMetadata)]" />
                    <xsl:if test="$metadataTest" >
                        <ul class="collapsibleList" style="font-size: 10px;">
                            <li>
                                <div class="tightView">
                                    <span class="small_bold">Inherited Metadata</span>
                                    <ul>
                                        <xsl:apply-templates select="$metadataTest" mode="metadataDetail" >
                                            <xsl:with-param name="indent" select="0"/>
                                            <xsl:with-param name="currentDataset" select="." />
                                        </xsl:apply-templates>
                                    </ul>
                                </div>

                            </li>
                        </ul>

                    </xsl:if>





                    <!-- ****************************************************** -->
                    <!--                              FOOTER                    -->
                    <!--                                                        -->
                    <!--                                                        -->
                    <hr size="1" noshade="noshade"/>
                    <table width="100%" border="0">
                        <tr>
                            <td>
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
                    <h1><font size="0">OPeNDAP Hyrax <font class="small">(<xsl:value-of select="$HyraxVersion"/>)</font>

                        <br/>
                        <a href='{$docsService}/'>Documentation</a>
                        </font>
                    </h1>

                </body>
        <script>CollapsibleLists.apply(true);</script>

    </xsl:template>



    <xsl:template match="thredds:service" name="serviceBanner" mode="banner">
        <xsl:param name="indent" />

        <tr>
            <td class="small" style="align: left; padding-left: {$indent}px;">
                <xsl:value-of select="@name"/>
            </td>
            <td class="small" style="align: left; padding-left: {$indent}px;">
                <xsl:value-of select="@serviceType"/>
            </td>
            <td class="small" style="align: left; padding-left: {$indent}px;">
                <xsl:value-of select="@base"/>
                <br/>
            </td>
            <xsl:apply-templates  mode="banner" >
                <xsl:with-param name="indent"><xsl:value-of select="$indent+$indentIncrement" /></xsl:with-param>
            </xsl:apply-templates>
        </tr>

        <xsl:if test="lower-case(@serviceType)='compound'" >
            <tr>
                <td><hr size="1" noshade="noshade" /></td>
                <td><hr size="1" noshade="noshade" /></td>
                <td><hr size="1" noshade="noshade" /></td>
            </tr>
        </xsl:if>

    </xsl:template>




    <!-- ******************************************************
      -  ServiceLinks
      -
      - This template produces data access (service) links for
      - the presentation of the thredds dataset.
      -
      -
      -
      - Note: The xsl:choose at the beginning should have all of
      - the tests in it that appear in the individual xsl:apply-template
      - calls in the subsequent code..
      -
      -
      -
      -
     -->
    <xsl:template name="doServiceLinks">
        <xsl:param name="inheritedMetadata"/>

        <xsl:choose>
            <xsl:when test="
                thredds:serviceName |
                thredds:metadata/thredds:serviceName |
                @serviceName |
                $inheritedMetadata[boolean($inheritedMetadata)]/thredds:serviceName |
                thredds:access/@serviceName"
            >
                <h2>Data Access:</h2>
            </xsl:when>
            <xsl:otherwise>
                No serviceName could be found for this dataset.
            </xsl:otherwise>
        </xsl:choose>

        <hr size="1" noshade="noshade"/>

        <xsl:variable name="serviceName" select="key('service-by-name', thredds:serviceName)" />
        <xsl:variable name="metadataServiceName" select="key('service-by-name', thredds:metadata/thredds:serviceName)" />
        <xsl:variable name="attrServceName" select="key('service-by-name', @serviceName)" />
        <xsl:variable name="inheritedServiceName" select="key('service-by-name', $inheritedMetadata[boolean($inheritedMetadata)]/thredds:serviceName)" />
        <xsl:variable name="access" select="key('service-by-name', thredds:access/@serviceName)" />
        <xsl:variable name="myServices">
            <xsl:choose>
                <xsl:when test="$serviceName">
                    <xsl:apply-templates mode="copy" select="$serviceName"/>
                </xsl:when>

                <xsl:when test="$metadataServiceName">
                    <xsl:apply-templates mode="copy" select="$metadataServiceName"/>
                </xsl:when>

                <xsl:when test="$attrServceName">
                    <xsl:apply-templates mode="copy" select="$attrServceName"/>
                </xsl:when>

                <xsl:when test="$inheritedServiceName">
                    <xsl:apply-templates mode="copy" select="$inheritedServiceName"/>
                </xsl:when>

                <xsl:when test="$access">
                    <xsl:apply-templates mode="copy" select="$access"/>
                </xsl:when>

                <xsl:otherwise>
                    <tr> <td>No Services Were Found.</td> </tr>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:if test="$debug">
            <div class="row">
                <span class="small" style="align: right;">myServices:</span>
                <span class="small" style="align: left;">
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

<xsl:apply-templates select="$myServices" mode="copy" />

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                </span>
            </div>
            <div class="row">
                <span class="small" style="align: right;">@urlPath:</span>
                <span class="small" style="align: left;"><xsl:value-of select="@urlPath"/></span>
            </div>
        </xsl:if>

        <xsl:call-template  name="DoBrokerLinks">
            <xsl:with-param name="currentDataset" select="."/>
            <xsl:with-param name="services" select="$myServices"/>
        </xsl:call-template>

        <hr size="1" noshade="noshade"/>

        <ul class="collapsibleList" style="font-size: 10px;">
            <li>
                <div class="tightView">
                    <span class="small_bold">Native Services</span>
                    <span>(origin server: <xsl:value-of select="$remoteHost"/>)</span>
                </div>
                <ul>
                    <li>
                        <xsl:apply-templates select="./thredds:access" mode="AccessLinks">
                            <xsl:with-param name="currentDataset" select="."/>
                            <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata"/>
                        </xsl:apply-templates>

                        <xsl:if test="boolean(@urlPath)">
                            <xsl:apply-templates select="$myServices" mode="ServiceLinksDiv">
                                <xsl:with-param name="urlPath" select="@urlPath"/>
                                <xsl:with-param name="indent" select="0"/>
                            </xsl:apply-templates>
                        </xsl:if>

                    </li>
                </ul>

            </li>
        </ul>


        <hr size="1" noshade="noshade" />


    </xsl:template>




    <xsl:template match="@*|node()" mode="copy">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="copy"/>
        </xsl:copy>
    </xsl:template>







    <xsl:template match="thredds:access" mode="AccessLinks" >
        <xsl:param name="currentDataset" />
        <xsl:param name="inheritedMetadata" />

        <xsl:variable name="urlPath" select="@urlPath"/>


        <!-- Since the thredds:access element isn't required to have a serviceName attribute we have to check... -->
        <xsl:choose>
            <xsl:when test="@serviceName">
                <xsl:apply-templates select="key('service-by-name', @serviceName)" mode="ServiceLinks" >
                    <xsl:with-param name="urlPath" select="$urlPath" />
                </xsl:apply-templates>
            </xsl:when>

            <!-- and if the serviceName attribute is missing we have to try to determine the "default" service" -->
            <xsl:otherwise>
                <xsl:variable name="defaultServiceName">
                    <xsl:call-template name="getDefaultService">
                        <xsl:with-param name="currentDataset" select="$currentDataset" />
                        <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata" />
                    </xsl:call-template>
                </xsl:variable>

                <xsl:apply-templates select="key('service-by-name',  $defaultServiceName)" mode="ServiceLinks" >
                    <xsl:with-param name="urlPath" select="$urlPath" />
                </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



    <xsl:template name="getDefaultService">
        <xsl:param name="currentDataset" />
        <xsl:param name="inheritedMetadata" />
        <xsl:choose>
            <xsl:when test="$currentDataset/thredds:serviceName">
                <xsl:value-of select="$currentDataset/thredds:serviceName"/>
            </xsl:when>
            <xsl:when test="$currentDataset/thredds:metadata/thredds:serviceName">
                <xsl:value-of select="$currentDataset/thredds:metadata/thredds:serviceName"/>
            </xsl:when>
            <xsl:when test="$currentDataset/@serviceName">
                <xsl:value-of select="$currentDataset/@serviceName"/>
            </xsl:when>
            <xsl:when test="$inheritedMetadata/thredds:serviceName">
                <xsl:value-of select="$inheritedMetadata/thredds:serviceName"/>
            </xsl:when>
            <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
    </xsl:template>




    <xsl:template match="*" mode="ServiceLinksOLDOFF" >
        <xsl:param name="urlPath" />
        <xsl:apply-templates mode="ServiceLinksOLDOFF" >
            <xsl:with-param name="urlPath" select="$urlPath"/>
        </xsl:apply-templates>
    </xsl:template>


    <xsl:template match="thredds:service" mode="ServiceLinksOLDOFF" >
        <xsl:param name="urlPath" />

        <xsl:choose>
            <xsl:when test="./@serviceType[.='Compound']">
                <tr>
                    <td colspan="2">
                        <div class="small">Compound Service: <xsl:value-of select="./@name" /></div>
                    </td>
                </tr>
                <xsl:apply-templates mode="ServiceLinksOLDOFF" >
                    <xsl:with-param name="urlPath" select="$urlPath"/>
                </xsl:apply-templates>
            </xsl:when>

            <xsl:otherwise>
                <xsl:variable name="resourceUrl" ><xsl:value-of select="$remoteHost[$remoteHost]"/><xsl:value-of select="./@base"/><xsl:value-of select="$urlPath"/></xsl:variable>

                <xsl:if test="$debug"><tr> <td class="small">thredds:service() - - - - - - - - - - - resourceUrl:</td><td> <xsl:value-of select="$resourceUrl"/></td> </tr></xsl:if>

                <tr>
                    <td align="right">
                        <span class="medium_bold"
                              style="margin-left: 10px;">
                            <div style="display:inline;" title="Service Name" ><xsl:value-of select="./@name"/></div>
                        </span>

                        <span class="small">(<div style="display:inline;" title="Service Type" ><xsl:value-of select="./@serviceType"/></div>)
                        </span>

                    </td>
                    <td align="left">
                        <span class="small_bold"
                             style="margin-left: 10px;">
                            <xsl:choose>

                                <!-- Check to see if we can build an access URL. -->
                                <xsl:when test="not($urlPath)">
                                No Service Links Available (Missing thredds:dataset/@urlPath or thredds:access/@urlPath) : urlPath: <xsl:value-of
                                        select="$urlPath"/>
                                </xsl:when>


                                <!--

                                DAP2 Service Links

                                -->
                                <xsl:when test="matches(./@serviceType, 'opendap', 'i')">

                                    <a style="padding-right: 3px" title="Browser accessible form for requesting data."
                                       href="{$resourceUrl}.html">Data Request Form</a>

                                    <xsl:if test="not($remoteHost)">
                                        <a style="padding-right: 3px"  title="RDF representation of the DDX."
                                           href="{$resourceUrl}.rdf">rdf</a>
                                    </xsl:if>

                                    <a style="padding-right: 3px" title="The DAP DDX document for this dataset."
                                       href="{$resourceUrl}.ddx">ddx</a>
                                    <a style="padding-right: 3px" title="The DAP DDS document for this dataset."
                                       href="{$resourceUrl}.dds">dds</a>
                                    <a style="padding-right: 3px" title="The DAP DAS document for this dataset."
                                       href="{$resourceUrl}.das">das</a>
                                    <a style="padding-right: 3px" title="Browser accessible informational page regarding this dataset."
                                       href="{$resourceUrl}.info">info</a>
                                </xsl:when>

                                <!--

                                DAP4 Service Links

                                -->
                                <xsl:when test="matches(./@serviceType, 'dap4', 'i')">

                                    <a style="padding-right: 3px" title="Browser accessible form for requesting data."
                                       href="{$resourceUrl}.dmr.html">Data Request Form</a>

                                    <a style="padding-right: 3px" title="The DAP DMR document for this dataset."
                                       href="{$resourceUrl}.dmr.xml">dmr</a>

                                    <xsl:if test="not($remoteHost)">
                                        <a style="padding-right: 3px" title="RDF representation of the DMR."
                                           href="{$resourceUrl}.dmr.rdf">rdf</a>
                                    </xsl:if>

                                </xsl:when>

                                <!--

                                Produce service URL's for the HTTPServer and File serviceType

                                -->
                                <xsl:when test="matches(./@serviceType, 'HTTPServer', 'i') or matches(./@serviceType, 'File', 'i')">
                                    <a
                                            title="This link provides file download access via HTTP."
                                            href="{$resourceUrl}" >File Download</a>
                                </xsl:when>


                            <!-- #####################################################
                              -
                              -
                              - Here is where you would add code to provide data access
                              - for a new service.
                              -
                              - Simply add a when statement with one or more link items.
                              -
                              - In this example:

                                    <xsl:when test="./@serviceType[.='YOUR_SERVICE_TYPE']" >
                                        <a href="{$remoteHost[$remoteHost]}{./@base}{$currentDataset/@urlPath}" >TEXT_OF_RESPONSE_LINK</a>
                                    </xsl:when>

                              - The href is formed to be the minium correct URL for a THREDDS catalog listing:
                              -
                              -     remoteHost + thredds:service/@base + thredds:dataset/@urlPath
                              -
                              - Example:
                              -
                              -    "http://test.opendap.org:8080" + "/opendap/" + "data/nc/fnoc1.nc"
                              -
                              - This should be how correct data access links can be formed from a THREDDS catalog.
                              -
                              -->


                            <!--
                              - No way to map service to a particular set of access URLs, so
                              - Give them the baseic service link.
                              -->
                                <xsl:otherwise>
                                    <a
                                            title="No additional links are available."
                                            href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" ><xsl:value-of select="./@serviceType"/></a>

                                </xsl:otherwise>

                        </xsl:choose>
                        </span>

                    </td>
                </tr>
                <!-- tr><td><hr size="1" noshade="noshade"/></td><td><hr size="1" noshade="noshade"/></td></tr  -->

            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
















    <xsl:template match="*" mode="ServiceLinksDiv" >
        <xsl:param name="urlPath" />
        <xsl:param name="indent" />
        <xsl:apply-templates mode="ServiceLinksDiv" >
            <xsl:with-param name="urlPath" select="$urlPath"/>
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>


    <xsl:template match="thredds:service" mode="ServiceLinksDiv" >
        <xsl:param name="urlPath" />
        <xsl:param name="indent" />

        <xsl:choose>
            <xsl:when test="./@serviceType[.='Compound']">
                <div style="padding-left: {$indent};">
                    <span class="small_bold"><xsl:value-of select="./@name" /> </span><span class="small">(THREDDS Compound Service)</span>
                </div>
                <xsl:apply-templates mode="ServiceLinksDiv" >
                    <xsl:with-param name="urlPath" select="$urlPath"/>
                    <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
                </xsl:apply-templates>
            </xsl:when>

            <xsl:otherwise>
                <xsl:variable name="resourceUrl" ><xsl:value-of select="$remoteHost[$remoteHost]"/><xsl:value-of select="./@base"/><xsl:value-of select="$urlPath"/></xsl:variable>

                <xsl:if test="$debug">
                    <div style="padding-left: {$indent};">
                        <span class="small">thredds:service() - - - - - - - - - - - resourceUrl:</span>
                        <span style="padding-left: {$indent};"> <xsl:value-of select="$resourceUrl"/></span>
                    </div>
                </xsl:if>

                <div class="row">
                        <span class="small_bold"
                              style="margin-left: 10px;">
                            <xsl:choose>

                                <!-- Check to see if we can build an access URL. -->
                                <xsl:when test="not($urlPath)">
                                    No Service Links Available (Missing thredds:dataset/@urlPath or thredds:access/@urlPath) : urlPath: <xsl:value-of
                                        select="$urlPath"/>
                                </xsl:when>


                                <!--

                                DAP2 Service Links

                                -->
                                <xsl:when test="matches(./@serviceType, 'opendap', 'i')">

                                    <a style="padding-right: 3px" title="Browser accessible form for requesting data"
                                       href="{$resourceUrl}.html">DAP2 Data Request Form</a>

                                    <xsl:if test="not($remoteHost)">
                                        <a style="padding-right: 3px"  title="RDF representation of the DDX"
                                           href="{$resourceUrl}.rdf">rdf</a>
                                    </xsl:if>

                                    <a style="padding-right: 3px" title="The DAP DDX document for this dataset"
                                       href="{$resourceUrl}.ddx">ddx</a>
                                    <a style="padding-right: 3px" title="The DAP DDS document for this dataset"
                                       href="{$resourceUrl}.dds">dds</a>
                                    <a style="padding-right: 3px" title="The DAP DAS document for this dataset"
                                       href="{$resourceUrl}.das">das</a>
                                    <a style="padding-right: 3px" title="Browser accessible informational page regarding this dataset"
                                       href="{$resourceUrl}.info">info</a>
                                </xsl:when>

                                <!--

                                DAP4 Service Links

                                -->
                                <xsl:when test="matches(./@serviceType, 'dap4', 'i')">

                                    <a style="padding-right: 3px" title="Browser accessible form for requesting data"
                                       href="{$resourceUrl}.dmr.html">DAP4 Data Request Form</a>

                                    <a style="padding-right: 3px" title="The DAP DMR document for this dataset"
                                       href="{$resourceUrl}.dmr.xml">dmr</a>

                                    <xsl:if test="not($remoteHost)">
                                        <a style="padding-right: 3px" title="RDF representation of the DMR"
                                           href="{$resourceUrl}.dmr.rdf">rdf</a>
                                    </xsl:if>

                                </xsl:when>

                                <!--
                                WCS Service link
                                -->
                                <xsl:when test="matches(./@serviceType, 'wcs', 'i')">
                                    <a title="WCS Service - GetCapabilities Response"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}?service=WCS&amp;request=GetCapabilities" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>

                                <!--
                                WMS Service Link
                                -->
                                <xsl:when test="matches(./@serviceType, 'wms', 'i')">
                                    <a title="WMS Service - GetCapabilities Response"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}?service=WMS&amp;request=GetCapabilities" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>

                                <!--
                                Produce service URL's for the HTTPServer and File serviceType
                                -->
                                <xsl:when test="matches(./@serviceType, 'HTTPServer', 'i') or matches(./@serviceType, 'File', 'i')">
                                    <a title="Complete File Download via HTTP"
                                       href="{$resourceUrl}" >File Download</a>
                                </xsl:when>

                                <!--
                                Netcdfsubset Link
                                -->
                                <xsl:when test="matches(./@serviceType, 'netcdfsubset', 'i')">
                                    <a title="Netcdfsubset Endpoint - REQUIRES user supplied constraint expression for use!"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>


                                <!--
                                NcML Response Link
                                -->
                                <xsl:when test="matches(./@serviceType, 'ncml', 'i')">
                                    <a title="NcML Dataset Description"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>

                                <!--
                                UDDC Link
                                -->
                                <xsl:when test="matches(./@serviceType, 'uddc', 'i')">
                                    <a title="UNIDATA Dataset Discovery Conformance Report"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>

                                <!--
                                ISO-19115 Link
                                -->
                                <xsl:when test="matches(./@serviceType, 'iso', 'i')">
                                    <a title="ISO-19115 Metadata"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>


                                <!--
                                CDM Remote Link
                                -->
                                <xsl:when test="matches(./@serviceType, 'cdmremote', 'i')">
                                    <a title="CDMRemote Service Endpoint - REQUIRES user supplied constraint expression for use!"
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:when>


                                <!-- #####################################################
                                  -
                                  -
                                  - Here is where you would add code to provide data access
                                  - for a new service.
                                  -
                                  - Simply add a when statement with one or more link items.
                                  -
                                  - In this example:

                                        <xsl:when test="./@serviceType[.='YOUR_SERVICE_TYPE']" >
                                            <a href="{$remoteHost[$remoteHost]}{./@base}{$currentDataset/@urlPath}" >TEXT_OF_RESPONSE_LINK</a>
                                        </xsl:when>

                                  - The href is formed to be the minium correct URL for a THREDDS catalog listing:
                                  -
                                  -     remoteHost + thredds:service/@base + thredds:dataset/@urlPath
                                  -
                                  - Example:
                                  -
                                  -    "http://test.opendap.org:8080" + "/opendap/" + "data/nc/fnoc1.nc"
                                  -
                                  - This should be how correct data access links can be formed from a THREDDS catalog.
                                  -
                                  -->


                                <!--
                                  - No way to map service to a particular set of access URLs, so
                                  - Give them the basic service link.
                                  -->
                                <xsl:otherwise>
                                    <a title="No Service Description Available."
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >
                                        <xsl:value-of select="./@serviceType"/>
                                    </a>
                                </xsl:otherwise>

                            </xsl:choose>
                        </span>

                    </div>
                <!-- tr><td><hr size="1" noshade="noshade"/></td><td><hr size="1" noshade="noshade"/></td></tr  -->

            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


















    <!-- ******************************************************
     -  getBrokerUrl
     -
     - This template produces Broker Access link for
     - the thredds dataset.
     -
     -
     -
     -
    -->
    <xsl:template name="DoBrokerLinks">
        <xsl:param name="currentDataset" />
        <xsl:param name="services" />

        <xsl:variable name="dap4" select="$services/descendant-or-self::thredds:service[lower-case(@serviceType)='dap4']" />
        <xsl:variable name="dap2" select="$services/descendant-or-self::thredds:service[lower-case(@serviceType)='opendap']" />
        <xsl:variable name="httpServer" select="$services/descendant-or-self::thredds:service[lower-case(@serviceType)='httpserver']" />

        <xsl:variable name="urlPath">
            <xsl:choose>
                <xsl:when test="$currentDataset/@urlPath"><xsl:value-of select="$currentDataset/@urlPath"/></xsl:when>
                <xsl:when test="./thredds:access/@urlPath"><xsl:value-of select="./thredds:access[1]/@urlPath"/></xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:variable>


        <xsl:variable name="resourceUrl">
            <xsl:choose>
                <!-- Priority is given to a DAP4 service because it:
                      - Requires a single request to get the data and the metadata
                      - The result is compact and easy to range GET
                -->
                <xsl:when test="$dap4">
                    <xsl:if test="$debug">DAP4:</xsl:if>
                    <xsl:value-of select="$remoteHost[$remoteHost]"/><xsl:value-of select="$dap4/@base"/><xsl:value-of select="$urlPath"/>
                </xsl:when>

                <!-- If we can deal with the native data type then we broker that, and we test this by
                seeing if an HTTP acces is available and if the name matches the BES typeMatch -->
                <xsl:when test="$httpServer and matches($urlPath,$typeMatch)">
                    <xsl:if test="$debug">MATCH:</xsl:if>
                    <xsl:value-of select="$remoteHost[$remoteHost]"/><xsl:value-of select="$httpServer/@base"/><xsl:value-of select="$urlPath"/>
                </xsl:when>

                <!-- Otherwise we'll use DAP2 if we can get it -->
                <xsl:when test="$dap2">
                    <xsl:if test="$debug">DAP2:</xsl:if>
                    <xsl:value-of select="$remoteHost[$remoteHost]"/><xsl:value-of select="$dap2/@base"/><xsl:value-of select="$urlPath"/>
                </xsl:when>

                <!-- HTTP services only make sense if we know if the data type can be ingested. -->
                <!-- xsl:when test="$httpServer">
                    HTTP: <xsl:value-of select="$remoteHost[$remoteHost]"/><xsl:value-of select="$httpServer/@base"/><xsl:value-of select="$urlPath"/>
                </xsl:when -->

                <xsl:otherwise>http://google.com/</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="$debug"><tr> <td class="small">thredds:service() DoBrokerLinks - resourceUrl: <xsl:value-of select="$resourceUrl"/></td> </tr></xsl:if>

        <xsl:call-template name="BrokerLinks">
            <xsl:with-param name="resourceUrl" select="$resourceUrl" />
        </xsl:call-template>

    </xsl:template>



    <xsl:template name="BrokerLinks">
        <xsl:param name="resourceUrl"/>


        <xsl:if test="$debug"> <tr> <td class="small">BrokerLinks() - - - BEGIN</td> </tr> </xsl:if>
        <xsl:if test="$debug"> <tr> <td class="small">BrokerLinks() - resourceUrl():</td><td class="small"><xsl:value-of select="$resourceUrl"/> </td> </tr> </xsl:if>
        <xsl:if test="$debug">
            <tr>
                <td class="small">BrokerLinks() - - - encoded():</td>
                <td class="small">
                    <xsl:element name="script">
                        <xsl:attribute name="type">text/javascript</xsl:attribute>
                        document.write(convertToHex("<xsl:value-of select="$resourceUrl"/>"));
                    </xsl:element>
                </td>
            </tr>
        </xsl:if>

        <!-- Make the link -->
        <tr>
            <td align="center" style="margin-left: 10px;width: 20%">
                <div class="medium_bold"
                     title="If the origin server does not offer a data service that you can utilize, Hyrax may broker the request and return the data in a form that you find more usable.">
                    Hyrax Broker
                </div>
            </td>
            <td >
                <div class="small_bold" align="left" style="margin-left: 10px;">
                    <table>
                        <!-- - - - - - - - - - Data Request Forms - - - - - - - - - - -->
                        <tr>
                            <td class="small" align="right" style="margin-left: 20px;">
                                <div title="Data request forms provide a user interface for selecting/subsetting data and choosing a return data format that suits your needs.">
                                    data request forms
                                </div>
                            </td>
                            <td class="medium_bold" align="left">

                                <!-- Make link object -->
                                <a id="{$resourceUrl}-D2IFH"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 Data Request Form"
                                   href="TBD">DAP2
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".html";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-D2IFH").setAttribute("href",
                                    ifhLink);
                                </xsl:element>

                                <!-- Make link object -->
                                <a id="{$resourceUrl}-D4IFH"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP4 Data Request Form"
                                   href="TBD">DAP4
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".dmr.html";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-D4IFH").setAttribute("href",
                                    ifhLink);
                                </xsl:element>
                            </td>
                        </tr>

                        <!-- - - - - - - - - - Metadata Responses - - - - - - - - - - -->
                        <tr>
                            <td class="small" align="right">
                                <div title="Metadata responses allow you to see the structure of the dataset as well as the dataset's available semantic metadata.">
                                    metadata responses
                                </div>
                            </td>
                            <td class="small_bold" align="left" style="margin-left: 10px;">

                                <!-- Make link object -->
                                <a id="{$resourceUrl}-DMR"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP4 DMR response"
                                   href="TBD">dmr
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".dmr.xml";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-DMR").setAttribute("href",
                                    ifhLink);
                                </xsl:element>

                                <!-- Make link object -->
                                <a id="{$resourceUrl}-DDS"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 DDS response"
                                   href="TBD">dds
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".dds";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-DDS").setAttribute("href",
                                    ifhLink);
                                </xsl:element>


                                <!-- Make link object -->
                                <a id="{$resourceUrl}-DAS"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 DAS response"
                                   href="TBD">das
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".das";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-DAS").setAttribute("href",
                                    ifhLink);
                                </xsl:element>

                                <!-- Make link object -->
                                <a id="{$resourceUrl}-DDX"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 DDX response"
                                   href="TBD">ddx
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".das";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-DDX").setAttribute("href",
                                    ifhLink);
                                </xsl:element>


                                <!-- Make link object -->
                                <a id="{$resourceUrl}-INFO"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 INFO response"
                                   href="TBD">info
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".info";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-INFO").setAttribute("href",
                                    ifhLink);
                                </xsl:element>

                                <!-- Make link object -->
                                <a id="{$resourceUrl}-RDF"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 RDF response"
                                   href="TBD">rdf
                                </a>
                                <!-- Set the href value -->
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".rdf";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-RDF").setAttribute("href",
                                    ifhLink);
                                </xsl:element>

                            </td>
                        </tr>
                        <!-- - - - - - - - - - Data Responses - - - - - - - - - - -->
                        <!-- tr>
                            <td class="small" align="right">data responses</td>
                            <td class="small_bold" align="left" style="margin-left: 10px;">

                                <a id="{$resourceUrl}-DAP"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP4 Data (.dap) response"
                                   href="TBD">dap4
                                </a>
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".dap";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-DAP").setAttribute("href",
                                    ifhLink);
                                </xsl:element>


                                <a id="{$resourceUrl}-DODS"
                                   class="medium-bold"
                                   style="padding-right: 3px;"
                                   title="Broker: DAP2 Data (.dods) response"
                                   href="TBD">dap2
                                </a>
                                <xsl:element name="script">
                                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                                    ifhLink="<xsl:value-of
                                        select="$serviceContext"/>"+"/gateway/"+convertToHex("<xsl:value-of
                                        select="$resourceUrl"/>") + ".dods";
                                    document.getElementById("<xsl:value-of select="$resourceUrl"/>-DODS").setAttribute("href",
                                    ifhLink);
                                </xsl:element>

                            </td>
                        </tr -->
                    </table>
                </div>
            </td>
        </tr>
        <xsl:if test="true()">
            <div class="row">
                <span class="small" align="right" >Broker Access URL:</span>
                <span>
                    <span class="small" style="margin-left: 10px;" align="left" >
                        <a href="{$resourceUrl}"><xsl:value-of select="$resourceUrl"/> </a>
                    </span>
                </span>
            </div>
        </xsl:if>

        <xsl:if test="$debug">
            <tr>
                <td class="small" style="margin-left: 10px;">BrokerLinks() - - - END</td>
            </tr>
        </xsl:if>
    </xsl:template>


    <!-- ******************************************************
      -
     -->
    <xsl:template match="*"/>










    <xsl:template match="thredds:catalogRef" mode="ServiceLinksOLDOFF">
        <xsl:param name="indent" />

        <xsl:variable name="myIndent" select="$indent+$indentIncrement" />

        <xsl:if test="not($remoteHost)">
            <tr>
                <td style="align: left; padding-left: {$myIndent}px;" >

                    <!-- If the href ends in .xml, change it to .html
                         so the link in the presentation points to
                         another HTML page. -->

                    <xsl:choose>

                        <!-- Does it point towards a remote catalog?. -->
                        <!-- -->
                        <xsl:when test="starts-with(@xlink:href,'http://')">
                            <a>
                                <xsl:attribute name="href">?browseCatalog=<xsl:value-of select="@xlink:href" /></xsl:attribute>
                                <xsl:choose>
                                    <xsl:when test="@xlink:title"><xsl:value-of select="./@xlink:title"/>/</xsl:when>
                                    <xsl:otherwise>Link</xsl:otherwise>
                                </xsl:choose>
                            </a>
                        </xsl:when>
                        <!-- -->

                        <!-- Does it end in '.xml'? --> <!--Then replace that with '.html'   -->
                        <xsl:when test="substring(./@xlink:href,string-length(./@xlink:href) - 3)='.xml'">
                            <!--Then replace that with '.html'   -->
                            <a href="{concat(substring(./@xlink:href,1,string-length(./@xlink:href) - 4),'.html')}" ><xsl:value-of select="./@name"/> /</a>
                        </xsl:when>

                        <!-- Since it doesn't end in .xml we don't know how to promote it, so leave it be. -->
                        <xsl:otherwise>
                            <a href="{./@xlink:href}" ><xsl:value-of select="./@name"/> /</a>
                        </xsl:otherwise>
                    </xsl:choose>

                </td>
            </tr>
        </xsl:if>


        <!-- -->

        <xsl:if test="$remoteHost">
            <tr>
                <td style="align: left; padding-left: {$myIndent}px;" >


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
                    <ul class="small">
                        <li><em>xlink:href: </em><xsl:value-of select="@xlink:href" /></li>
                        <li><em>remoteHost: </em><xsl:value-of select="$remoteHost" /></li>
                        <li><em>remoteRelativeURL: </em><xsl:value-of select="$remoteRelativeURL" /></li>
                    </ul>

                </td>
            </tr>
        </xsl:if>

        <!-- -->

    </xsl:template>





    <xsl:template match="thredds:catalogRef" mode="ServiceLinksDiv">
        <xsl:param name="indent" />

        <xsl:variable name="myIndent" select="$indent+$indentIncrement" />

        <xsl:if test="not($remoteHost)">
            <div class="row">
                <span style="align: left; padding-left: {$myIndent}px;" >

                    <!-- If the href ends in .xml, change it to .html
                         so the link in the presentation points to
                         another HTML page. -->

                    <xsl:choose>

                        <!-- Does it point towards a remote catalog?. -->
                        <!-- -->
                        <xsl:when test="starts-with(@xlink:href,'http://')">
                            <a>
                                <xsl:attribute name="href">?browseCatalog=<xsl:value-of select="@xlink:href" /></xsl:attribute>
                                <xsl:choose>
                                    <xsl:when test="@xlink:title"><xsl:value-of select="./@xlink:title"/>/</xsl:when>
                                    <xsl:otherwise>Link</xsl:otherwise>
                                </xsl:choose>
                            </a>
                        </xsl:when>
                        <!-- -->

                        <!-- Does it end in '.xml'? --> <!--Then replace that with '.html'   -->
                        <xsl:when test="substring(./@xlink:href,string-length(./@xlink:href) - 3)='.xml'">
                            <!--Then replace that with '.html'   -->
                            <a href="{concat(substring(./@xlink:href,1,string-length(./@xlink:href) - 4),'.html')}" ><xsl:value-of select="./@name"/> /</a>
                        </xsl:when>

                        <!-- Since it doesn't end in .xml we don't know how to promote it, so leave it be. -->
                        <xsl:otherwise>
                            <a href="{./@xlink:href}" ><xsl:value-of select="./@name"/> /</a>
                        </xsl:otherwise>
                    </xsl:choose>

                </span>
            </div>
        </xsl:if>


        <!-- -->

        <xsl:if test="$remoteHost">
            <div clss="row">
                <span style="align: left; padding-left: {$myIndent}px;" >


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
                    <ul class="small">
                        <li><em>xlink:href: </em><xsl:value-of select="@xlink:href" /></li>
                        <li><em>remoteHost: </em><xsl:value-of select="$remoteHost" /></li>
                        <li><em>remoteRelativeURL: </em><xsl:value-of select="$remoteRelativeURL" /></li>
                    </ul>

                </span>
            </div>
        </xsl:if>

        <!-- -->

    </xsl:template>



</xsl:stylesheet>

