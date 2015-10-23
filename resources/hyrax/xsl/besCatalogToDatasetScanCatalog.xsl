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
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
<!ENTITY DBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:exsl="http://exslt.org/common"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
        >
    <xsl:param name="catalogDatasetID" />
    <!-- xsl:param name="dapService"/ -->
    <!-- xsl:param name="allowDirectDataSourceAccess"/ -->
    <!-- xsl:param name="useDAP2ResourceUrlResponse"/ -->
    <!-- xsl:param name="ncWmsServiceBase"/ -->
    <!-- xsl:param name="ncWmsDynamicServiceId"/ -->


    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>



    <xsl:variable name="besPrefix">
        <xsl:choose>
            <xsl:when test="/bes:response/bes:showCatalog/bes:dataset/@prefix!='/'">
                <xsl:value-of select="concat(/bes:response/bes:showCatalog/bes:dataset/@prefix,'/')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="/bes:response/bes:showCatalog/bes:dataset/@prefix"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>


    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:showCatalog">
        <thredds:catalog>
            <xsl:apply-templates />
        </thredds:catalog>
    </xsl:template>





    <!--***********************************************
       -
       -
     -->
    <xsl:template match="response">
            <xsl:apply-templates />
    </xsl:template>


    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:dataset">




        <xsl:choose>
            <xsl:when test="bes:dataset">
                <!--
                This dataset is the  top level dataset. The only dataset that the bes
                returns that will contain child datasets is the top level dataset in
                the showCatalog response. This a major assumption and this XSLT will
                fail if the bes changes this arrangement
                -->

                <xsl:variable name="name">
                    <xsl:choose>
                        <xsl:when test="@name='/'" >
                            <xsl:value-of select="@prefix" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="concat($besPrefix,@name)" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:variable name="ID">
                    <xsl:choose>
                        <xsl:when test="$name='/'" >
                            <xsl:value-of select="$catalogDatasetID" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="concat($catalogDatasetID,$name,'/')" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <thredds:dataset name="{$name}" ID="{$ID}">
                    <xsl:apply-templates />
                </thredds:dataset>
            </xsl:when>

            <xsl:otherwise>
                <!-- It's not a top level dataset... -->

                <xsl:variable name="ID">
                    <xsl:choose>
                        <xsl:when test="../@name='/'">
                            <xsl:value-of select="concat($catalogDatasetID,$besPrefix,@name)" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="concat($catalogDatasetID,$besPrefix,../@name,'/',@name)" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <!-- <sanityCheck besDapService="{$besDapService}" parentName="{../@name}" myName="{@name}"  ID="{$ID}"/>  -->

                <xsl:choose>
                    <xsl:when test="@node='true'">
                        <!-- This dataset is a node, aka a directory or collection -->

                        <thredds:catalogRef name="{@name}" xlink:href="{@name}/catalog.xml" xlink:title="{@name}" xlink:type="simple" ID="{$ID}/" />
                    </xsl:when >

                    <xsl:otherwise>
                        <!-- This dataset  a simple dataset, aka a file or a granule or a leaf -->

                        <thredds:dataset name="{@name}" ID="{$ID}" >
                            <thredds:dataSize units="bytes"><xsl:value-of select="@size" /></thredds:dataSize>
                            <thredds:date type="modified"><xsl:value-of select="@lastModified" /></thredds:date>
                            <xsl:copy-of select="bes:serviceRef" />
                            <!-- xsl:call-template name="DatasetAccess"/ -->
                        </thredds:dataset>
                    </xsl:otherwise >

                </xsl:choose>
            </xsl:otherwise>

        </xsl:choose>


    </xsl:template>






</xsl:stylesheet>

