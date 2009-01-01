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
    <xsl:param name="targetDataset" />
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

    <xsl:template match="thredds:catalog">
        <html>
            <xsl:apply-templates />
        </html>
    </xsl:template>

    <xsl:template match="thredds:dataset">
        <xsl:param name="inheritedMetadata" />


        <xsl:if test="thredds:dataset">
            <xsl:apply-templates>
                <!--
                  -   Note that the followiing parameter uses an XPath that
                  -   accumulates inherited thredds:metadata elements as it descends the
                  -   hierarchy.
                  -->
                <xsl:with-param name="inheritedMetadata" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]" />
            </xsl:apply-templates>
         </xsl:if >

        <xsl:if test="not(thredds:dataset)">

            <!-- Is this the dataset that we are supposed to summary? -->
            <xsl:if test="$targetDataset=preceding::*/last()">

                <!-- ****************************************************** -->
                <!--                      PAGE HEADER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <head>
                    <link rel='stylesheet' href='/opendap/docs/css/thredds.css'
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
                            <td width="30%" align="left"><img alt="Logo" src='/opendap/docs/images/logo.gif' /></td>
                            <td class="dark" align="left">Hyrax - THREDDS Dataset Detail</td>
                        </tr>
                    </table>
                    <h1><font size="0">
                        Dataset: <xsl:value-of select="@name" />
                        <br/>
                        </font><font size="-2">
                            <SCRIPT LANGUAGE="JavaScript">
                            <xsl:comment >
                            {
                                catalog = location.href.split("?");
                                document.write('Catalog: '+catalog[0]);
                            }
                            </xsl:comment>
                        </SCRIPT>
                    </font></h1>

                    <hr size="1" noshade="noshade"/>

                    <!-- ****************************************************** -->
                    <!--                 PAGE BODY CONTENT                      -->
                    <!--                                                        -->
                    <!--                                                        -->


                    <h2>MetaData Summary:</h2>

                    <xsl:variable name="serviceNameTest" select="thredds:serviceName |
                                        thredds:metadata/thredds:serviceName |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:serviceName" />
                    <xsl:if test="$serviceNameTest" >
                        <h3>Access: </h3>
                        <table class="small">
                            <xsl:apply-templates select="$serviceNameTest" mode="ServiceLinks" >
                                 <xsl:with-param name="currentDataset" select="." />
                            </xsl:apply-templates>
                        </table>
                    </xsl:if>


                    <xsl:variable name="docTest" select="thredds:documentation |
                                        thredds:metadata/thredds:documentation |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:documentation" />
                    <xsl:if test="$docTest" >
                        <h3>Documentation: </h3>
                        <ul class="small">
                            <xsl:apply-templates select="$docTest" mode="documentationDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="dateTest" select="thredds:date |
                                        thredds:metadata/thredds:date |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:date" />
                    <xsl:if test="$dateTest" >
                        <h3>Dates: </h3>
                        <ul class="small">
                            <xsl:apply-templates select="$dateTest" mode="dateDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="timeCoverageTest" select="thredds:timeCoverage |
                                        thredds:metadata/thredds:timeCoverage |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:timeCoverage" />
                    <xsl:if test="$timeCoverageTest" >
                        <h3>Time Coverage: </h3>
                        <ul class="small">
                            <xsl:apply-templates select="$timeCoverageTest" mode="timeCoverageDetail" />
                        </ul>
                    </xsl:if>



                    <xsl:variable name="geoCvrTest" select="thredds:geospatialCoverage |
                                        thredds:metadata/thredds:geospatialCoverage |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:geospatialCoverage" />
                    <xsl:if test="$geoCvrTest" >
                        <h3>Geospatial Coverage: </h3>
                            <xsl:apply-templates select="$geoCvrTest" mode="geospatialCoverageDetail" />
                    </xsl:if>

                    <xsl:variable name="creatorTest" select="thredds:creator |
                                        thredds:metadata/thredds:creator |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:creator" />
                    <xsl:if test="$creatorTest" >
                        <h3>Creators: </h3>
                        <ul class="small">
                            <xsl:apply-templates select="$creatorTest" mode="creatorDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="publisherTest" select="thredds:publisher |
                                        thredds:metadata/thredds:publisher |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:publisher" />
                    <xsl:if test="$publisherTest" >
                        <h3>Publishers: </h3>
                        <ul class="small">
                            <xsl:apply-templates select="$publisherTest" mode="publisherDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="propTest" select="thredds:property |
                                        thredds:metadata/thredds:property |
                                        $inheritedMetadata[boolean($inheritedMetadata)]/thredds:property" />
                    <xsl:if test="$propTest" >
                        <h3>Properties: </h3>
                        <ul class="small">
                            <xsl:apply-templates  select="$propTest" mode="propertyDetail" />
                        </ul>
                    </xsl:if>


                    <xsl:variable name="metadataTest" select="thredds:metadata |
                                        $inheritedMetadata[boolean($inheritedMetadata)]" />
                    <xsl:if test="$metadataTest" >
                        <h2>Metadata Detail: </h2>
                        <ul class="small">
                            <xsl:apply-templates select="$metadataTest" mode="metadataSumary" >
                                <xsl:with-param name="currentDataset" select="." />
                            </xsl:apply-templates>
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
                    <h1><font size="0">OPeNDAP Hyrax <font class="small">(THREDDS Catalog Dataset Detail)</font>

                        <br/>
                        <a href='/opendap/docs/'>Documentation</a>
                        </font>
                    </h1>

                </body>

            </xsl:if>
        </xsl:if>



    </xsl:template>


    <!-- ******************************************************
      -  ServiceLinks
     -->
    <xsl:template match="*" mode="ServiceLinks" >
        <xsl:param name="currentDataset" />
    </xsl:template>


    <xsl:template match="thredds:serviceName" mode="ServiceLinks" >
        <xsl:param name="currentDataset" />
        <xsl:apply-templates select="key('service-by-name', .)" mode="ServiceLinks" >
            <xsl:with-param name="currentDataset" select="$currentDataset" />
        </xsl:apply-templates>

    </xsl:template>



    <xsl:template match="thredds:service" mode="ServiceLinks" >
        <xsl:param name="currentDataset" />

            <xsl:if test="./@serviceType[.='Compound']" >
                <td><xsl:value-of select="./@name" />:</td>

                <xsl:apply-templates select="./thredds:service" mode="ServiceLinks" >
                        <xsl:with-param name="currentDataset" select="$currentDataset" />
                </xsl:apply-templates>

            </xsl:if>

            <xsl:if test="not(./@serviceType[.='Compound'])" >


                    <tr>
                    <td>
                        <b>&#160;&#160;<xsl:value-of select="./@name" /></b>
                        (<xsl:value-of select="./@serviceType" />)&#160;&#160;&#160;&#160;
                    </td>

                    <td>
                        <xsl:choose>

                            <!-- Produces service URL's for the OPeNDAP serviceType -->
                            <xsl:when test="./@serviceType[.='OPeNDAP'] |
                                              ./@serviceType[.='OPENDAP'] |
                                              ./@serviceType[.='OpenDAP'] |
                                              ./@serviceType[.='OpenDap'] |
                                              ./@serviceType[.='openDap'] |
                                              ./@serviceType[.='opendap']">

                                <a href="{./@base}{$currentDataset/@urlPath}.ddx" >ddx</a>&#160;
                                <a href="{./@base}{$currentDataset/@urlPath}.dds" >dds</a>&#160;
                                <a href="{./@base}{$currentDataset/@urlPath}.das" >das</a>&#160;
                                <a href="{./@base}{$currentDataset/@urlPath}.info" >info</a>&#160;
                                <a href="{./@base}{$currentDataset/@urlPath}.html" >html request form</a>
                            </xsl:when>

                            <!-- Produces service URL's for the WCS serviceType -->
                            <xsl:when test="./@serviceType[.='WCS']" >
                                <a href="{./@base}{$currentDataset/@urlPath}.ddx" >CoverageDescription</a>
                            </xsl:when>


                            <!-- Produces service URL's for the HTTPServer serviceType -->
                            <xsl:when test="./@serviceType[.='HTTPServer']" >
                                <a href="{./@base}{$currentDataset/@urlPath}" >File Download</a>
                            </xsl:when>

                            <xsl:otherwise>
                                <em>Service Links Not Available.</em>
                            </xsl:otherwise>

                        </xsl:choose>
                    </td>
                    </tr>

            </xsl:if>
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
        <li><xsl:value-of select="."/> <em><b>(<xsl:value-of select="@type"/>)</b></em></li>
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
        <li> <b><xsl:value-of select="@vocabulary_name" />[</b><xsl:value-of select="@name" /><b>]</b> <xsl:if test="@units">
             <tr><td>units: </td><td><xsl:value-of select="@units" /></td></tr>
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
      -  metadataSumary

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


    <xsl:template match="*" mode="metadataSumary" />

    <xsl:template match="thredds:metadata" mode="metadataSumary">
        <xsl:param name="currentDataset" />
        <li><b><xsl:if test="./@inherited[.='true']">Inherited</xsl:if> Metadata Group:</b></li>
        <ul>
        <xsl:apply-templates mode="metadataSumary" />
        </ul>
    </xsl:template>

    <xsl:template match="thredds:documentation" mode="metadataSumary">
        <xsl:if test="@type">
            <li><em>documentation[<b><xsl:value-of select="@type"/>]: </b></em><xsl:value-of select="."/></li>
        </xsl:if>

        <xsl:if test="@xlink:href">
            <li><em>documentation[<b>Linked Document</b>]: </em><a href="{@xlink:href}"><xsl:value-of select="@xlink:title"/></a></li>
        </xsl:if>
    </xsl:template>




    <xsl:template match="thredds:property" mode="metadataSumary">
        <xsl:apply-templates select="." mode="propertyDetail" />
    </xsl:template>

    <xsl:template match="thredds:contributor" mode="metadataSumary">
        Contributer:<xsl:apply-templates select="." mode="contributorDetail" />
    </xsl:template>

    <xsl:template match="thredds:creator" mode="metadataSumary">
        Creator:<xsl:apply-templates select="." mode="creatorDetail" />
    </xsl:template>

    <xsl:template match="thredds:date" mode="metadataSumary">
        <xsl:apply-templates select="." mode="dateDetail" />
    </xsl:template>


    <xsl:template match="thredds:keyword" mode="metadataSumary">
        <xsl:apply-templates select="." mode="keywordDetail" />
    </xsl:template>

    <xsl:template match="thredds:project" mode="metadataSumary">
        <xsl:apply-templates select="." mode="projectDetail" />
    </xsl:template>

    <xsl:template match="thredds:publisher" mode="metadataSumary">
        Publisher:<xsl:apply-templates select="." mode="publisherDetail" />
    </xsl:template>

    <xsl:template match="thredds:geospatialCoverage" mode="metadataSumary">
        <xsl:apply-templates select="." mode="geospatialCoverageDetail" />
    </xsl:template>

    <xsl:template match="thredds:timeCoverage" mode="metadataSumary">
        <xsl:apply-templates select="." mode="timeCoverageDetail" />
    </xsl:template>

    <xsl:template match="thredds:variables" mode="metadataSumary">
        <xsl:apply-templates select="." mode="variablesDetail" />
    </xsl:template>

    <xsl:template match="thredds:dataType" mode="metadataSumary">
         <li><em>Data type: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:dataSize" mode="metadataSumary">
         <li><em>Data size: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:dataFormat" mode="metadataSumary">
         <li><em>Data Format: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:serviceName" mode="metadataSumary">
         <li><em>Service Name: </em><xsl:value-of select="."/></li>
    </xsl:template>

    <xsl:template match="thredds:authority" mode="metadataSumary">
         <li><em>Naming Authority: </em><xsl:value-of select="."/></li>
    </xsl:template>




</xsl:stylesheet>

