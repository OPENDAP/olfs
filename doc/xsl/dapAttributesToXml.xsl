<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2009 OPeNDAP, Inc.
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
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dap="http://xml.opendap.org/ns/DAP2"
                xmlns:wcs="http://www.opengis.net/wcs/1.1"
                xmlns:ows="http://www.opengis.net/ows/1.1"
                xmlns:owcs="http://www.opengis.net/wcs/1.1/ows"
                xmlns:gml="http://www.opengis.net/gml/3.2"
                xmlns:xlink="http://www.w3.org/1999/xlink"
        >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>


    <!--
       - xmlElementNode
      -->
    <xsl:template match="dap:Attribute[@type='xmlElementNode']" name="xmlEleementNode">
        <xsl:choose>
            <xsl:when test="@prefix">
                <xsl:choose>
                    <xsl:when test="@namespace">
                        <xsl:element name="{@prefix}:{@name}" namespace="{@namespace}">
                            <xsl:apply-templates/>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise >
                        <xsl:element name="{@prefix}:{@name}">
                            <xsl:apply-templates/>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise >
                <xsl:choose>
                    <xsl:when test="@namespace">
                        <xsl:element name="{@name}" namespace="{@namespace}">
                            <xsl:apply-templates/>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="{@name}">
                            <xsl:apply-templates/>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>



    <!--
       - xmlAttributeNode
      -->
    <xsl:template match="dap:Attribute[@type='xmlAttributeNode']" name="xmlAttributeNode">
            <xsl:choose>
                <xsl:when test="@prefix">
                    <xsl:attribute name="{@prefix}:{@name}">
                        <xsl:value-of select="normalize-space(dap:value)" />
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="@namespace">
                    <xsl:attribute name="{@name}" namespace="{@namespace}">
                        <xsl:value-of select="normalize-space(dap:value)" />
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="normalize-space(dap:value)" /></xsl:otherwise>
            </xsl:choose>

    </xsl:template>

    <!--
       - xmlTextNode
      -->
    <xsl:template match="dap:Attribute[@type='xmlTextNode']" name="xmlTextNode">
        <xsl:value-of select="dap:value" />
    </xsl:template>


    <!--
       - supress regular text and attributes.
      -->

    <xsl:template  match="@*|text()" />


</xsl:stylesheet>
