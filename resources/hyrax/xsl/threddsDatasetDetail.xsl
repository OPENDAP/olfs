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
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"

                >
    <xsl:import href="version.xsl"/>
    <xsl:param name="docsService"/>
    <xsl:param name="targetDataset" />
    <xsl:param name="remoteCatalog" />
    <xsl:param name="remoteHost" />
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>



    <xsl:template match="thredds:catalog">
        <html>
            <xsl:apply-templates />
        </html>
    </xsl:template>

    <xsl:template match="thredds:dataset">
        <xsl:param name="inheritedMetadata" />

        <xsl:variable name="datasetPositionInDocument">
            <xsl:value-of select="count(preceding::*)"/>
        </xsl:variable>

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
                  -   Note that the followiing parameter uses an XPath that
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
                    <link rel='stylesheet' href='{$docsService}/css/contents.css'
                          type='text/css'/>
                    <title>THREDDS Dataset: <xsl:value-of select="@name"/></title>

                </head>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->
                <body>


                    <table width="100%">
                        <tr>
                            <td width="30%" align="left">
                                <img alt="Logo" src='{$docsService}/images/logo.gif'/>
                            </td>
                            <td class="dark" align="left">Hyrax - THREDDS Dataset Detail</td>
                        </tr>
                    </table>

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
                    </h1>

                    <div>
                        <span class="small_italic">ID:</span>
                        <span class="medium_bold">
                            <xsl:value-of select="@ID"/>
                        </span>
                    </div>

                    <div>
                        <span class="small_italic">name:</span>
                        <span class="medium_bold">
                            <xsl:value-of select="@name"/>
                        </span>
                    </div>

                    <div style="padding-bottom: 5px;">
                        <span class="small_italic">Catalog:</span>
                        <span class="small_bold">
                            <SCRIPT LANGUAGE="JavaScript">
                                <xsl:comment>
                                    {
                                    catalog = location.href.split("?");
                                    document.write(''+catalog[0]);
                                    }
                                </xsl:comment>
                            </SCRIPT>
                        </span>

                    </div>

                    <hr size="1" noshade="noshade"/>

                    <!-- ****************************************************** -->
                    <!--                 PAGE BODY CONTENT                      -->
                    <!--                                                        -->
                    <!--                                                        -->




                    <xsl:call-template name="doServiceLinks">
                        <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata" />
                    </xsl:call-template>




                    <h2>MetaData Summary:</h2>




                    <xsl:variable name="docTest" select="thredds:documentation |
                                        thredds:metadata/thredds:documentation |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:documentation" />
                    <xsl:if test="$docTest" >
                        <div class="medium_bold">Documentation: </div>
                        <ul class="small">
                            <xsl:apply-templates select="$docTest" mode="documentationDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="dateTest" select="thredds:date |
                                        thredds:metadata/thredds:date |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:date" />
                    <xsl:if test="$dateTest" >
                        <div class="medium_bold">Dates: </div>
                        <ul class="small">
                            <xsl:apply-templates select="$dateTest" mode="dateDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="timeCoverageTest" select="thredds:timeCoverage |
                                        thredds:metadata/thredds:timeCoverage |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:timeCoverage" />
                    <xsl:if test="$timeCoverageTest" >
                        <div class="medium_bold">Time Coverage: </div>
                        <ul class="small">
                            <xsl:apply-templates select="$timeCoverageTest" mode="timeCoverageDetail" />
                        </ul>
                    </xsl:if>



                    <xsl:variable name="geoCvrTest" select="thredds:geospatialCoverage |
                                        thredds:metadata/thredds:geospatialCoverage |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:geospatialCoverage" />
                    <xsl:if test="$geoCvrTest" >
                        <div class="medium_bold">Geospatial Coverage: </div>
                            <xsl:apply-templates select="$geoCvrTest" mode="geospatialCoverageDetail" />
                    </xsl:if>

                    <xsl:variable name="creatorTest" select="thredds:creator |
                                        thredds:metadata/thredds:creator |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:creator" />
                    <xsl:if test="$creatorTest" >
                        <div class="medium_bold">Creators: </div>
                        <ul class="small">
                            <xsl:apply-templates select="$creatorTest" mode="creatorDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="publisherTest" select="thredds:publisher |
                                        thredds:metadata/thredds:publisher |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:publisher" />
                    <xsl:if test="$publisherTest" >
                        <div class="medium_bold">Publishers: </div>
                        <ul class="small">
                            <xsl:apply-templates select="$publisherTest" mode="publisherDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="propTest" select="thredds:property |
                                        thredds:metadata/thredds:property |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:property" />
                    <xsl:if test="$propTest" >
                        <div class="medium_bold">Properties: </div>
                        <ul class="small">
                            <xsl:apply-templates  select="$propTest" mode="propertyDetail" />
                        </ul>
                    </xsl:if>


                    <hr/>


                    <h2>Metadata Detail: </h2>

                    <ul class="small">
                        <xsl:apply-templates select="." mode="metadataDetail" >
                            <xsl:with-param name="currentDataset" select="." />
                        </xsl:apply-templates>

                        <xsl:variable name="metadataTest" select="$inheritedMetadata[boolean($inheritedMetadata)]" />
                        <xsl:if test="$metadataTest" >
                            <xsl:apply-templates select="$metadataTest" mode="metadataDetail" >
                                <xsl:with-param name="currentDataset" select="." />
                            </xsl:apply-templates>
                        </xsl:if>
                    </ul>





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
                thredds:access"
                    >
                <h2>Access:</h2>
            </xsl:when>
            <xsl:otherwise>
                No serviceName or access found for this dataset.
            </xsl:otherwise>
        </xsl:choose>

        <table>
            <!--div class="small">- - - - - - - - - - - - - - - - - - - thredds:serviceName</div-->
            <xsl:apply-templates select="key('service-by-name', thredds:serviceName)" mode="ServiceLinks">
                <xsl:with-param name="urlPath" select="@urlPath"/>
            </xsl:apply-templates>


            <!--div class="small">- - - - - - - - - - - - - - - - - - - thredds:metadata/thredds:serviceName </div-->
            <xsl:apply-templates select="key('service-by-name', thredds:metadata/thredds:serviceName)"
                                 mode="ServiceLinks">
                <xsl:with-param name="urlPath" select="@urlPath"/>
            </xsl:apply-templates>


            <!--div class="small">- - - - - - - - - - - - - - - - - - - @serviceName</div-->
            <xsl:apply-templates select="key('service-by-name', @serviceName)" mode="ServiceLinks">
                <xsl:with-param name="urlPath" select="@urlPath"/>
            </xsl:apply-templates>


            <!--div class="small">- - - - - - - - - - - - - - - - - - - $inheritedMetadata[boolean($inheritedMetadata)]/thredds:serviceName</div-->
            <xsl:apply-templates
                    select="key('service-by-name', $inheritedMetadata[boolean($inheritedMetadata)]/thredds:serviceName)"
                    mode="ServiceLinks">
                <xsl:with-param name="urlPath" select="@urlPath"/>
            </xsl:apply-templates>


            <!--div class="small">- - - - - - - - - - - - - - - - - - - thredds:access/@serviceName</div-->

            <xsl:apply-templates select="thredds:access" mode="ServiceLinks">
                <xsl:with-param name="currentDataset" select="."/>
                <xsl:with-param name="inheritedMetadata" select="$inheritedMetadata"/>
            </xsl:apply-templates>
        </table>
        <hr/>

    </xsl:template>


    <xsl:template match="thredds:access" mode="ServiceLinks" >
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




    <xsl:template match="*" mode="ServiceLinks" >
        <xsl:param name="urlPath" />
    </xsl:template>


    <xsl:template match="thredds:service" mode="ServiceLinks" >
        <xsl:param name="urlPath" />

        <xsl:choose>
            <xsl:when test="./@serviceType[.='Compound']">
                <tr>
                    <td colspan="2">
                        <div class="small" style="color: grey;"><xsl:value-of select="./@name" />:</div>
                    </td>
                </tr>
                <xsl:apply-templates select="./thredds:service" mode="ServiceLinks" >
                        <xsl:with-param name="urlPath" select="$urlPath" />
                </xsl:apply-templates>
                <tr>
                    <td colspan="2">&#160;</td>
                </tr>

            </xsl:when>

            <xsl:otherwise>

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
                                No Service Links Available (Missing thredds:dataset/@urlPath or thredds:access/@urlPath)
                                </xsl:when>


                                <xsl:when test="matches(./@serviceType, 'opendap', 'i')">

                                    <a title="Browser accessible form for requesting data."
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}.html">Data Request Form</a>&#160;

                                    <xsl:if test="not($remoteHost)">
                                        <a title="RDF representation of the DDX."
                                           href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}.rdf">rdf</a>&#160;
                                    </xsl:if>

                                    <a title="The DAP DDX document for this dataset."
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}.ddx">ddx</a>&#160;
                                    <a title="The DAP DDS document for this dataset."
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}.dds">dds</a>&#160;
                                    <a title="The DAP DAS document for this dataset."
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}.das">das</a>&#160;
                                    <a title="Browser accessible informational page regarding this dataset."
                                       href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}.info">info</a>&#160;
                                </xsl:when>


                                <!-- Produces service URL's for the HTTPServer serviceType -->
                                <xsl:when test="matches(./@serviceType, 'HTTPServer', 'i')">
                                    <a
                                            title="This link provides file download access via HTTP."
                                            href="{$remoteHost[$remoteHost]}{./@base}{$urlPath}" >File Download</a>
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

            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>




    <!-- ******************************************************
      -
     -->

    <xsl:template match="*">
   </xsl:template>




    <!-- ******************************************************
      -  documentationDetail
     -->

    <xsl:template match="thredds:documentation" mode="documentationDetail">

        <xsl:if test="@type">
            <li><em><b><xsl:value-of select="@type"/>: </b></em><xsl:value-of select="."/></li>
        </xsl:if>

        <xsl:if test="@xlink:href">
            <li><em><b>Linked Document: </b></em><a href="{@xlink:href}"><xsl:value-of select="@xlink:title"/></a></li>
        </xsl:if>
    </xsl:template>

    <xsl:template match="thredds:*" mode="documentationDetail">
        <xsl:apply-templates mode="documentationDetail" />
    </xsl:template>



    <!-- ******************************************************
      -  dateDetail

        <date type="modified">2008-12-23 23:58:40Z</date>
    -->
    <xsl:template match="thredds:date" mode="dateDetail">
        <li><xsl:value-of select="."/> <em>(<xsl:value-of select="@type"/>)</em></li>
    </xsl:template>



    <!-- ******************************************************
      -  creatorDetail

            <creator>
                <name vocabulary="DIF">UCAR/UNIDATA</name>
                <contact url="http://www.unidata.ucar.edu/" email="support@unidata.ucar.edu" />
            </creator>
    -->
    <xsl:template match="thredds:creator" mode="creatorDetail">
        <xsl:call-template name="sourceType" />
    </xsl:template>


    <!-- ******************************************************
      -  publisherDetail

            <publisher>
                <name vocabulary="DIF">UCAR/UNIDATA</name>
                <contact url="http://www.unidata.ucar.edu/" email="support@unidata.ucar.edu" />
            </publisher>
    -->

    <xsl:template match="thredds:publisher" mode="publisherDetail">
        <xsl:call-template name="sourceType" />
    </xsl:template>

    <xsl:template name="sourceType" >
        <li>
            <b><xsl:value-of select="thredds:name" /></b>
            <xsl:if test="@vocabulary"> (<xsl:value-of select="@vocabulary" />)</xsl:if>
            <ul>
                <li><em>email: <xsl:value-of select="thredds:contact/@email" /></em></li>
                <li><em><a href="{thredds:contact/@url}"><xsl:value-of select="thredds:contact/@url" /></a></em></li>
            </ul>
        </li>
    </xsl:template>


    <!-- ******************************************************
      -  timeCoverageDetail

            <timeCoverage>
                <start>2008-12-29 12:00:00Z</start>
                <end>2009-01-01 18:00:00Z</end>
            </timeCoverage>
            <timeCoverage>
                <start>2008-12-29 12:00:00Z</start>
                <duration>20.2 hours</duration>
            </timeCoverage>
            <timeCoverage>
                <end>2008-12-29 12:00:00Z</end>
                <duration>20.2 hours</duration>
            </timeCoverage>
    -->
    <xsl:template match="thredds:timeCoverage" mode="timeCoverageDetail">
            <xsl:apply-templates mode="timeCoverageDetail" />
    </xsl:template>

    <xsl:template match="thredds:start" mode="timeCoverageDetail">
        <li>start: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <xsl:template match="thredds:end" mode="timeCoverageDetail">
        <li>end: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <xsl:template match="thredds:duration" mode="timeCoverageDetail">
        <li>duration: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <!-- ******************************************************
      -  accessDetail
          <xsd:element name="access">
            <xsd:complexType>
                <xsd:sequence>
                    <xsd:element ref="dataSize" minOccurs="0"/>
                </xsd:sequence>
                <xsd:attribute name="urlPath" type="xsd:token" use="required"/>
                <xsd:attribute name="serviceName" type="xsd:string"/>
                <xsd:attribute name="dataFormat" type="dataFormatTypes"/>
            </xsd:complexType>
          </xsd:element >
    -->
    <xsl:template match="thredds:access" mode="accessDetail">
        <li><em>Access:</em>
            <ul>
                <li><em>urlPath: </em><xsl:value-of select="@urlPath" /></li>
                <li><em>serviceName: </em><xsl:value-of select="@serviceName" /></li>
                <li><em>dataFormat</em><xsl:value-of select="@dataFormat" /></li>
                <xsl:apply-templates mode="dataSizeDetail" />
            </ul>
        </li>
    </xsl:template>


    <xsl:template match="thredds:dataType" mode="dataTypeDetail">
         <li><em>Data type: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:dataSize" mode="dataSizeDetail">
         <li><em>Data size: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:dataFormat" mode="dataFormatDetail">
         <li><em>Data Format: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:serviceName" mode="serviceNameDetail">
         <li><em>Service Name: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:authority" mode="authorityDetail">
         <li><em>Naming Authority: </em><xsl:value-of select="."/></li>
    </xsl:template>






    <!-- ******************************************************
      -  propertyDetail


             <geospatialCoverage zpositive="down">
               <northsouth>
                 <start>10</start>
                 <size>80</size>
                 <resolution>2</resolution>
                 <units>degrees_north</units>
               </northsouth>
               <eastwest>
                 <start>-130</start>
                 <size>260</size>
                 <resolution>2</resolution>
                 <units>degrees_east</units>
               </eastwest>
               <updown>
                 <start>0</start>
                 <size>22</size>
                 <resolution>0.5</resolution>
                 <units>km</units>
               </updown>
              </geospatialCoverage>

              <geospatialCoverage>
                <name vocabulary="Thredds">global</name>
              </geospatialCoverage>

     -->
    <xsl:template match="thredds:geospatialCoverage" mode="geospatialCoverageDetail">
        <div class="small">
        <em>Geospatial Coverage Instance</em>
        <ul>
            <xsl:apply-templates mode="geospatialCoverageDetail" />
            <xsl:if test="@zpositive">
                <li><b>z increases in the <xsl:value-of select="@zpositive" /> direction.</b></li>
            </xsl:if>
        </ul>
        </div>
    </xsl:template>


    <xsl:template match="thredds:northsouth" mode="geospatialCoverageDetail">
        <li>
            <b>north-south:</b>
            <ul>
                <xsl:apply-templates mode="geospatialCoverageDetail" />
            </ul>
        </li>
    </xsl:template>

    <xsl:template match="thredds:eastwest" mode="geospatialCoverageDetail">
        <li>
            <b>east-west:</b>
            <ul>
                <xsl:apply-templates mode="geospatialCoverageDetail" />
            </ul>
        </li>
    </xsl:template>

    <xsl:template match="thredds:updown" mode="geospatialCoverageDetail">
        <li>
            <b>up-down:</b>
            <ul>
                <xsl:apply-templates mode="geospatialCoverageDetail" />
            </ul>
        </li>
    </xsl:template>

    <xsl:template match="thredds:start" mode="geospatialCoverageDetail">
        <li>start: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <xsl:template match="thredds:size" mode="geospatialCoverageDetail">
        <li>size: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <xsl:template match="thredds:resolution" mode="geospatialCoverageDetail">
        <li>resolution: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <xsl:template match="thredds:units" mode="geospatialCoverageDetail">
        <li>units: <em><xsl:value-of select="." /></em></li>
    </xsl:template>

    <xsl:template match="thredds:name" mode="geospatialCoverageDetail">
        <li><b>name: </b><em><xsl:value-of select="." /> (<xsl:value-of select="@vocabulary"/> vocabulary)</em></li>
    </xsl:template>





    <!-- ******************************************************
      -  propertyDetail
     -->
    <xsl:template match="thredds:property" mode="propertyDetail">
        <li><b><xsl:value-of select="@name" /></b> = <xsl:value-of select="@value" /></li>
    </xsl:template>



    <!-- ******************************************************
      -  contributorDetail
     -->
    <xsl:template match="thredds:contributor" mode="contributorDetail">
        <li><em>Contributor: </em><xsl:value-of select="." />, <xsl:value-of select="@role" /></li>
    </xsl:template>



    <!-- ******************************************************
      -  keywordDetail
     -->
    <xsl:template match="thredds:keyword" mode="keywordDetail">
        <li>
            <em>keyword
            <xsl:if test="@vocabulary" >
                (vocab: <xsl:value-of select="@vocabulary" />):
            </xsl:if>
            </em><xsl:value-of select="." />
        </li>
    </xsl:template>

    <!-- ******************************************************
      -  projectDetail
     -->
    <xsl:template match="thredds:project" mode="projectDetail">
        <li>
            <em>project
            <xsl:if test="@vocabulary" >
                (vocab: <xsl:value-of select="@vocabulary" />):
            </xsl:if>
            </em><xsl:value-of select="." />
        </li>
    </xsl:template>

    <!-- ******************************************************
      -  variablesDetail
     -->
    <xsl:template match="thredds:variables" mode="variablesDetail">
        <li>
        Variables[<xsl:value-of select="@vocabulary" />]:
        <ul>
            <xsl:apply-templates  mode="variableDetail" />
            <xsl:apply-templates  mode="variableMapDetail" />
        </ul>
        </li>
    </xsl:template>

    <!-- ******************************************************
      -  variableDetail
     -->
    <xsl:template match="thredds:variable" mode="variableDetail">
        <li> <b><xsl:value-of select="@vocabulary_name" />[</b><xsl:value-of select="@name" /><b>]</b>
             <xsl:if test="@units">
                <em>units: <xsl:value-of select="@units" /></em>
             </xsl:if>
        </li>
    </xsl:template>

    <xsl:template match="*" mode="variableMapDetail">
    </xsl:template>

    <!-- ******************************************************
      -  variableMapDetail
     -->
    <xsl:template match="thredds:variableMap" mode="variableMapDetail">
        <li>
            <b>variableMap: </b>
            <a href="{@xlink:href}">
                <xsl:choose>
                    <xsl:when test="@xlink:title">Title: <xsl:value-of select="@xlink:title" /></xsl:when>
                    <xsl:otherwise>Link</xsl:otherwise>
                </xsl:choose>
            </a>
        </li>
    </xsl:template>

    <!-- ******************************************************
      -  datasetDetail
     -->
    <xsl:template match="thredds:dataset" mode="datasetDetail">
        <li>
            <b>Dataset Metadata: </b>
            <ul>
                <li><em>name: </em><xsl:value-of select="@name" /></li>
                <xsl:apply-templates select="*" mode="metadataDetail"/>
            </ul>
        </li>
    </xsl:template>


    <!-- ******************************************************
      -  metadataDetail

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


    <xsl:template match="*" mode="metadataDetail" />

    <xsl:template match="thredds:metadata" mode="metadataDetail">
        <xsl:param name="currentDataset" />
        <li><b><xsl:if test="./@inherited[.='true']">Inherited</xsl:if> Metadata Group:</b></li>
        <ul>
        <xsl:apply-templates mode="metadataDetail" />
        </ul>
    </xsl:template>

    <xsl:template match="thredds:documentation" mode="metadataDetail">
        <xsl:if test="@type">
            <li><em>documentation[<b><xsl:value-of select="@type"/>]: </b></em><xsl:value-of select="."/></li>
        </xsl:if>

        <xsl:if test="@xlink:href">
            <li><em>documentation[<b>Linked Document</b>]: </em><a href="{@xlink:href}"><xsl:value-of select="@xlink:title"/></a></li>
        </xsl:if>
    </xsl:template>




    <xsl:template match="thredds:property" mode="metadataDetail">
        <xsl:apply-templates select="." mode="propertyDetail" />
    </xsl:template>

    <xsl:template match="thredds:contributor" mode="metadataDetail">
        <li><em>Contributer:</em><ul><xsl:apply-templates select="." mode="contributorDetail" /></ul> </li>
    </xsl:template>

    <xsl:template match="thredds:creator" mode="metadataDetail">
        <li><em>Creator:</em><ul><xsl:apply-templates select="." mode="creatorDetail" /></ul> </li>
    </xsl:template>

    <xsl:template match="thredds:date" mode="metadataDetail">
        <xsl:apply-templates select="." mode="dateDetail" />
    </xsl:template>


    <xsl:template match="thredds:keyword" mode="metadataDetail">
        <xsl:apply-templates select="." mode="keywordDetail" />
    </xsl:template>

    <xsl:template match="thredds:project" mode="metadataDetail">
        <xsl:apply-templates select="." mode="projectDetail" />
    </xsl:template>

    <xsl:template match="thredds:publisher" mode="metadataDetail">
        <li><em>Publisher:</em><ul><xsl:apply-templates select="." mode="publisherDetail" /></ul> </li>
    </xsl:template>

    <xsl:template match="thredds:geospatialCoverage" mode="metadataDetail">
        <xsl:apply-templates select="." mode="geospatialCoverageDetail" />
    </xsl:template>

    <xsl:template match="thredds:timeCoverage" mode="metadataDetail">
        <xsl:apply-templates select="." mode="timeCoverageDetail" />
    </xsl:template>

    <xsl:template match="thredds:variables" mode="metadataDetail">
        <xsl:apply-templates select="." mode="variablesDetail" />
    </xsl:template>

    <xsl:template match="thredds:dataType" mode="metadataDetail">
        <xsl:apply-templates select="." mode="dataTypeDetail" />
    </xsl:template>

    <xsl:template match="thredds:dataSize" mode="metadataDetail">
        <xsl:apply-templates select="." mode="dataSizeDetail" />
    </xsl:template>

    <xsl:template match="thredds:dataFormat" mode="metadataDetail">
        <xsl:apply-templates select="." mode="dataFormatDetail" />
    </xsl:template>

    <xsl:template match="thredds:serviceName" mode="metadataDetail">
        <xsl:apply-templates select="." mode="serviceNameDetail" />
    </xsl:template>

    <xsl:template match="thredds:authority" mode="metadataDetail">
        <xsl:apply-templates select="." mode="authorityDetail" />
    </xsl:template>

    <xsl:template match="thredds:access" mode="metadataDetail">
        <xsl:apply-templates select="." mode="accessDetail" />
    </xsl:template>

    <xsl:template match="thredds:dataset" mode="metadataDetail">
        <xsl:apply-templates select="." mode="datasetDetail" />
    </xsl:template>





</xsl:stylesheet>

