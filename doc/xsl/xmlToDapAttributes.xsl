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
            >
        <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

        <xsl:strip-space elements="*" />


        <xsl:template match="/">
            <dap:Dataset>
                <xsl:apply-templates/>
            </dap:Dataset>

        </xsl:template>
        <xsl:template match="*">
            <!-- Create a dap:Attribute element to wrap this XML element -->
            <xsl:element name="dap:Attribute">

                <!-- The name of the dap:Attribute is the non-prefixed QName of the source XML element -->
                <xsl:attribute name="name">
                    <xsl:value-of select="local-name()"/>
                </xsl:attribute>

                <!-- Add the namespace of the source XML element as an attribute -->
                <xsl:attribute name="namespace">
                    <xsl:value-of select="namespace-uri()"/>
                </xsl:attribute>

                <!--  Map it to a namespaced dap:Attribute -->

                <xsl:choose>
                    <!--  If it has child elements or xml attributes it's going into an Attribute Container. -->
                    <xsl:when test="* | @*">
                        <xsl:attribute name="type">Container</xsl:attribute>

                        <!-- Make special Attributes for each of the xml elements attributes. -->
                        <xsl:for-each select="@*">
                            <xsl:element name="dap:Attribute">
                                <xsl:attribute name="name">
                                    <xsl:value-of select="local-name()"/>
                                </xsl:attribute>
                                <xsl:attribute name="type">xmlAttributeNode</xsl:attribute>
                                <xsl:attribute name="namespace">
                                    <xsl:value-of select="namespace-uri()"/>
                                </xsl:attribute>
                                <xsl:element name="dap:value">
                                    <xsl:value-of select="."/>
                                </xsl:element>
                            </xsl:element>
                        </xsl:for-each>

                        <!--
                           - And it's text value has to be wrapped in another dap:Attribute because dap:Attribute
                           - elements may not have a type of Container AND have values
                          -->
                        <xsl:if test="text()">
                            <xsl:element name="dap:Attribute">
                                <xsl:attribute name="name">textNode</xsl:attribute>
                                <xsl:attribute name="type">String</xsl:attribute>
                                <xsl:element name="dap:value">
                                    <xsl:copy-of select="text()"/>
                                </xsl:element>
                            </xsl:element>
                        </xsl:if>
                    </xsl:when>

                    <xsl:otherwise>
                        <!--
                           - IT looks like an element with only text (or empty content) so we
                           - can make the dap:Attribute of type String.
                          -->
                        <xsl:attribute name="type">String</xsl:attribute>
                                <xsl:element name="dap:value">
                                    <xsl:copy-of select="text()"/>
                            </xsl:element>

                    </xsl:otherwise>
                </xsl:choose>

                <xsl:apply-templates/>


            </xsl:element>
        </xsl:template>

        <xsl:template name="text">
            <xsl:copy-of select="text()"/>
        </xsl:template>

        <xsl:template  match="@*|text()" />

        <LowerCorner xmlns="http://www.opengis.net/ows/1.1"
                     xmlns:gml="http://www.opengis.net/gml/3.2"
                     gml:crs="urn:ogc:def:crs:EPSG::4326" >-97.8839 21.736</LowerCorner>
    </xsl:stylesheet>
