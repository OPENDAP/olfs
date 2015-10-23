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
        <!ENTITY DAPOBJ  "http://xml.opendap.org/ontologies/opendap-dap-3.2.owl#" >
        <!ENTITY DAP     "http://xml.opendap.org/ns/DAP/3.2#" >
        <!ENTITY XSD     "http://www.w3.org/2001/XMLSchema#" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dapObj="&DAPOBJ;"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dap="&DAP;"
                xmlns:xml="http://www.w3.org/XML/1998/namespace"
        >

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:key name="AttributeNames" match="dap:Attribute" use="@name"/>

    <xsl:variable name="XML_BASE"><xsl:value-of select="/dap:Dataset/@xml:base"/></xsl:variable>
    <xsl:variable name="LocalOntology"><xsl:value-of select="$XML_BASE"/>.rdf</xsl:variable>
    <xsl:variable name="LocalAttributeNS"><xsl:value-of select="$XML_BASE"/>/att#</xsl:variable>


    <xsl:template name="identityTransform" match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>



    <!-- ###################################################################
      -
      -   Converts a Dataset DDX into an RDF Document.
      -
    -->
    <xsl:template match="/dap:Dataset">
        <xsl:copy>
            <xsl:attribute name="rdf:about"></xsl:attribute>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>

    </xsl:template>
    <!-- ################################################################### -->




    <xsl:template match="dap:Grid | dap:Structure | dap:Sequence | dap:Byte | dap:Int16 | dap:Int32 | dap:UInt16 | dap:UInt32 | dap:Float32 | dap:Float64 | dap:String | dap:Url" >
        <xsl:copy>
            <xsl:attribute name="rdf:ID">
                <xsl:call-template name="localIdWorker"/>
            </xsl:attribute>
            <xsl:call-template name="localId"/>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>


    <xsl:template match="dap:Array | dap:Map" >
        <xsl:copy>
            <xsl:attribute name="rdf:ID">
                <xsl:call-template name="localIdWorker"/>
            </xsl:attribute>
            <xsl:call-template name="localId"/>
            <xsl:apply-templates mode="array" select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template name="idT" mode="array" match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>


    <!-- ################################################################### -->




    <!-- ###################################################################
     -
     -    dapObj:localId
     -
     -
    -->
    <xsl:template name="localId">
        <xsl:attribute name="dapObj:localId">
            <xsl:call-template name="localIdWorker"/>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="*" name="localIdWorker" mode="localId">
        <xsl:if test="generate-id(.)!=generate-id(/dap:Dataset)">
            <xsl:apply-templates select=".." mode="localId"/>
            <xsl:if
                test="generate-id(..)!=generate-id(/dap:Dataset)"
                >.</xsl:if>
            <xsl:value-of select="@name"/>
        </xsl:if>
    </xsl:template>

    <!-- ################################################################### -->





</xsl:stylesheet>
