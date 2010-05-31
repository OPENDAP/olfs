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
    <xsl:param name="remoteHost" />
    <xsl:param name="remoteRelativeURL" />
    <xsl:param name="remoteCatalog" />
    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

    <xsl:template match="thredds:catalog">

        <xsl:copy>
            <xsl:apply-templates />
            
        </xsl:copy>

    </xsl:template>

    <xsl:template match="thredds:*">

        <xsl:copy-of select="." />

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
                <xsl:comment>######################## DATASET SCAN ######################</xsl:comment>
                <!--
                ..........................................................
                Input XML:
                <xsl:copy-of select="." />
                ..........................................................

                thredds:metadata/thredds:serviceName: <xsl:value-of select="thredds:metadata/thredds:serviceName" />
                -->

                <xsl:variable name="serviceName" select="thredds:metadata/thredds:serviceName"/>
                <!-- serviceName: <xsl:value-of select="$serviceName" /> -->

                <xsl:variable name="datasetScanLocation" select="@location"/>
                <!-- datasetScanLocation = '<xsl:value-of select="$datasetScanLocation"/>'-->

                <xsl:variable name="datasetScanName" select="@name"/>

                <!-- Get the service definition form the key (a hash map) -->
                <xsl:variable name="serviceElement" select="key('service-by-name', $serviceName)" />


                <!--
                service-by-name:
                <xsl:copy-of select="$serviceElement" />
                -->

                <!-- Get the service definition form the key (a hash map) -->
                <!--xsl:variable name="dapServices" select="$serviceElement/thredds:service[@serviceType='OPENDAP']"/-->
                <xsl:variable name="dapServices" select="$serviceElement[@serviceType='OPENDAP'] | $serviceElement/thredds:service[@serviceType='OPENDAP'] "/>

                <xsl:for-each select="$dapServices">

                    <xsl:variable name="base" select="@base" />
                    <!-- base = '<xsl:value-of select="$base"/>' -->

                    <xsl:variable name="lastCharOfBase" select="substring($base,string-length($base))" />
                    <!-- lastCharOfBase = '<xsl:value-of select="$lastCharOfBase"/>' -->

                    <xsl:variable name="catalogURL">
                        <xsl:choose>

                            <xsl:when test="$lastCharOfBase='/' and starts-with($datasetScanLocation,'/')">
                                <xsl:variable name="location" select="substring($datasetScanLocation,2,string-length($datasetScanLocation))" />
                                <xsl:variable name="targetURL" select="concat($base,$location)" />
                                <xsl:value-of select="$targetURL"/>
                            </xsl:when>

                            <xsl:when test="$lastCharOfBase!='/' and not(starts-with($datasetScanLocation,'/'))">
                                <xsl:variable name="targetURL" select="concat($base,'/',$datasetScanLocation)" />
                                <xsl:value-of select="$targetURL"/>
                            </xsl:when>

                            <xsl:otherwise>
                                <xsl:variable name="targetURL" select="concat($base,$datasetScanLocation)" />
                                <xsl:value-of select="$targetURL"/>
                            </xsl:otherwise>

                        </xsl:choose>

                    </xsl:variable>


                    <thredds:catalogRef name="{$datasetScanName}"
                                        xlink:title="{$datasetScanName}"
                                        xlink:href="{$catalogURL}"
                                        xlink:type="simple"/>
                                        



                </xsl:for-each>


    </xsl:template>


</xsl:stylesheet>

