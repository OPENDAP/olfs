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
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
<!ENTITY DBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:dapwcs="http://www.opendap.org/ns/dapwcs"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0 http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.xsd"
                >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>



    <!--***********************************************
       -
       -
     -->
    <xsl:template match="showCatalog">
        <thredds:catalog>
            <thredds:service name="OPeNDAP-Hyrax" serviceType="OPeNDAP" base="/opendap/hyrax/"/>
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
    <xsl:template match="dataset">


        <xsl:if test="dataset">
            <thredds:dataset name="{name}" ID="{name}">
                <thredds:serviceName>OPeNDAP-Hyrax</thredds:serviceName>
                <thredds:metadata inherited="true">
                    <thredds:serviceName>OPeNDAP-Hyrax</thredds:serviceName>
                    <thredds:authority>opendap.org</thredds:authority>
                    <thredds:dataType>unknown</thredds:dataType>
                    <thredds:dataFormat>unknown</thredds:dataFormat>
                </thredds:metadata>
                <xsl:apply-templates />
            </thredds:dataset>
        </xsl:if>

        <xsl:if test="not(dataset)">

            <xsl:if test="@thredds_collection='true'">
                <thredds:catalogRef name="{name}" xlink:href="{name}/catalog.xml" xlink:title="{name}" xlink:type="simple" >
                    <xsl:attribute name="ID">
                        <xsl:value-of select="../name" /><xsl:if test="not(../name[.='/'])">/</xsl:if><xsl:value-of select="name" />
                    </xsl:attribute>
                </thredds:catalogRef>
            </xsl:if >

            <xsl:if test="not(@thredds_collection='true')">
                <thredds:dataset name="{name}"  >
                    <xsl:attribute name="urlPath">
                        <xsl:value-of select="../name" /><xsl:if test="not(../name[.='/'])">/</xsl:if><xsl:value-of select="name" />
                    </xsl:attribute>
                    <xsl:attribute name="ID">
                        <xsl:value-of select="../name" /><xsl:if test="not(../name[.='/'])">/</xsl:if><xsl:value-of select="name" />
                    </xsl:attribute>
                    <thredds:dataSize units="bytes">
                        <xsl:value-of select="size" />
                    </thredds:dataSize>
                    <thredds:date type="modified">
                        <xsl:value-of select="lastmodified/date" />T<xsl:value-of select="lastmodified/time" />
                    </thredds:date>
                </thredds:dataset>
            </xsl:if >

        </xsl:if>



    </xsl:template>


    <xsl:template match="name">
    </xsl:template>

    <xsl:template match="size">
    </xsl:template>

    <xsl:template match="lastmodified">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="date">
    </xsl:template>

    <xsl:template match="time">
    </xsl:template>

    <xsl:template match="count">
    </xsl:template>



</xsl:stylesheet>

