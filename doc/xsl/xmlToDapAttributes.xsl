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

            <!--  Make it type xmlElementNode -->
            <xsl:attribute name="type">xmlElementNode</xsl:attribute>

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

            <!-- A 1:1 mapping would fail here because the DAP does not allow a
             - a dap:Attributes@type to be 'Container' AND have XML text content.
             - We need another value for dap:Attribute@type, say 'xmlText'?
            -->
            <!-- Make special Attributes for source xml elements text node. -->
            <xsl:if test="text()">
                <xsl:element name="dap:Attribute">
                    <xsl:attribute name="name">textNode</xsl:attribute>
                    <xsl:attribute name="type">xmlTextNode</xsl:attribute>
                    <xsl:element name="dap:value">
                        <xsl:copy-of select="text()"/>
                    </xsl:element>
                </xsl:element>
            </xsl:if>

            <xsl:apply-templates/>


        </xsl:element>
    </xsl:template>

    <xsl:template name="text">
        <xsl:copy-of select="text()"/>
    </xsl:template>

    <xsl:template  match="@*|text()" />


</xsl:stylesheet>
