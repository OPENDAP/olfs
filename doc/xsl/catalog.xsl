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
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
<!ENTITY DBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
                >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

    <xsl:variable name="serviceContext">/opendap</xsl:variable>
    <xsl:variable name="dapService">/hyrax</xsl:variable>



    <xsl:variable name="besPrefix">        
        <xsl:if test="/bes:response/bes:showCatalog/bes:dataset/@prefix != '/' ">
            <xsl:value-of select="/bes:response/bes:showCatalog/bes:dataset/@prefix"/>
        </xsl:if>
    </xsl:variable>
    
    <xsl:variable name="context" select="concat($serviceContext,$dapService,$besPrefix)"/>


    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:showCatalog">
        <thredds:catalog>
            <thredds:service name="dap" serviceType="OPeNDAP" base="{$context}"/>
            <thredds:service name="file" serviceType="HTTPServer" base="{$context}"/>
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
                <thredds:dataset name="{@name}" ID="{$context}{@name}">
                    <xsl:apply-templates />
                </thredds:dataset>
            </xsl:when>

            <xsl:otherwise>

                <xsl:if test="@node='true'">
                    <thredds:catalogRef name="{@name}" xlink:href="{@name}/catalog.xml" xlink:title="{@name}" xlink:type="simple" >
                        <xsl:attribute name="ID">
                            <xsl:value-of select="../@name" /><xsl:if test="../@name[.!='/']">/</xsl:if><xsl:value-of select="@name" />
                        </xsl:attribute>
                    </thredds:catalogRef>
                </xsl:if >

                <xsl:if test="not(@node='true')">
                    <thredds:dataset name="{@name}"  >
                        <xsl:attribute name="ID">
                            <xsl:value-of select="$context"/><xsl:value-of select="../@name" /><xsl:if test="../@name[.!='/']">/</xsl:if><xsl:value-of select="@name" />
                        </xsl:attribute>
                        <thredds:dataSize units="bytes"><xsl:value-of select="@size" /></thredds:dataSize>
                        <thredds:date type="modified"><xsl:value-of select="@lastModified" /></thredds:date>
                        <xsl:call-template name="DatasetAccess"/>
                    </thredds:dataset>
                </xsl:if >
            </xsl:otherwise>

        </xsl:choose>


    </xsl:template>

    <xsl:template name="DatasetAccess">

        <thredds:access>
        <xsl:choose>
            <xsl:when test="bes:serviceRef">
                <xsl:attribute name="serviceName"><xsl:value-of select="bes:serviceRef"/></xsl:attribute>
                <xsl:attribute name="urlPath">
                    <xsl:value-of select="../@name" /><xsl:if test="../@name[.!='/']">/</xsl:if><xsl:value-of select="@name" />
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
                <xsl:attribute name="serviceName">file</xsl:attribute>
                <xsl:attribute name="urlPath">
                    <xsl:value-of select="../@name" /><xsl:if test="../@name[.!='/']">/</xsl:if><xsl:value-of select="@name" />
                </xsl:attribute>
            </xsl:otherwise>
        </xsl:choose>
        </thredds:access>

    </xsl:template>


</xsl:stylesheet>

