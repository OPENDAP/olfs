<?xml version="1.0" encoding="ISO-8859-1"?>
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
    <xsl:param name="remoteHost" />
    <xsl:param name="remoteRelativeURL" />
    <xsl:param name="remoteCatalog" />
    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>



    <!-- This is the identity transform template. If this were the only template
       - then the output would be identical to the source document.
     -->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
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
        <xsl:comment>########## thredds:catalogRef generated from thredds:datasetScan BEGIN ##########</xsl:comment>

        <!--
        ..........................................................
        Input XML:
        <xsl:copy-of select="." />
        ..........................................................

        thredds:metadata/thredds:serviceName: <xsl:value-of select="thredds:metadata/thredds:serviceName" />
        -->


        <xsl:variable name="datasetScanName" select="@name"/>

        <xsl:variable name="path" select="@path" />



        <thredds:catalogRef name="{$datasetScanName}"
                            xlink:title="{$datasetScanName}"
                            xlink:href="{$path}/catalog.xml"
                            xlink:type="simple"/>


        <xsl:comment>########## thredds:catalogRef generated from thredds:datasetScan END ##########</xsl:comment>

    </xsl:template>


</xsl:stylesheet>

