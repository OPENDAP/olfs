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
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dap="http://xml.opendap.org/ns/DAP2" >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:strip-space elements="*" />


<!--
    <xsl:template match="dap:*">
        <xsl:copy >
            <xsl:call-template name="textAndattributes" />
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*"><xsl:apply-templates /></xsl:template>

-->



    <xsl:template match="*">
        <xsl:choose>
            <xsl:when test="namespace-uri()='http://xml.opendap.org/ns/DAP2'">
                <xsl:copy >
                    <xsl:call-template name="textAndattributes" />
                    <xsl:apply-templates />
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



    <xsl:template  match="@*|text()" />

    <xsl:template name="textAndattributes" ><xsl:copy-of select="text()|@*" /></xsl:template>

</xsl:stylesheet>
