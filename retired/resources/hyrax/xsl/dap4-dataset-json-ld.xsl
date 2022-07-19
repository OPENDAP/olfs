<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2018 OPeNDAP, Inc.
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
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dap="http://xml.opendap.org/ns/DAP/4.0#"
>

    <xsl:variable name="datasetUrl">
        <xsl:value-of select="/dap:Dataset/@xml:base"/>
    </xsl:variable>

    <xsl:variable name="DATASET_LICENSE">DATASET License URL</xsl:variable>
    <xsl:variable name="DATASET_HEADLINE">DATASET HEADLINE STRING</xsl:variable>
    <xsl:variable name="DATASET_DESCRIPTION">DATASET DESCRIPTION STRING</xsl:variable>

    <xsl:template match="dap:Dataset">
        <xsl:call-template name="json-ld-Dataset"/>
    </xsl:template>

    <xsl:template name="json-ld-Dataset">
        <xsl:element name="script" >
            <xsl:attribute name="type">application/ld+json</xsl:attribute>
            {
            "@context": "http://schema.org",
            "@type": "Dataset",
            "name": "<xsl:value-of select="@name"/>",
            "headline": "<xsl:value-of select="$DATASET_HEADLINE"/>",
            "description": "<xsl:value-of select="$DATASET_DESCRIPTION"/>",
            "url": "<xsl:value-of select="$datasetUrl"/>",
            "includedInDataCatalog": {
            "@type": "DataCatalog",
            "name": "Hyrax Data Server (OPeNDAP)",
            "sameAs": "@CATALOG_URL@"
            },
            "keywords": [
            "look",
            "in",
            "metadata",
            "?"
            ],
            "license":"<xsl:value-of select="$DATASET_LICENSE"/>",
            "variableMeasured": [ <xsl:apply-templates mode="json-ld" /> ],
            "creator": {
            "@type": "Organization",
            "name": "NASA/GSFC/OBPG",
            "email": "data@oceancolor.gsfc.nasa.gov",
            "sameAs": "https://oceandata.sci.gsfc.nasa.gov"
            },
            "publisher": {
            "@type": "Organization",
            "name": "@PublisherName@",
            "address": {
            "@type": "@PostalAddress@",
            "addressCountry": "@Country@",
            "addressLocality": "@Street,City@",
            "addressRegion": "@State@",
            "postalCode": "@PostalCode@"
            },
            "telephone": "@PublisherPhoneNumber@",
            "email": "@PublisherEmail@",
            "sameAs": "@OrganizationLandingPageURL@"
            },
            "dateCreated": "2017-10-21T13:56:40Z",
            "identifier": "gov.noaa.pfeg.coastwatch/jplAquariusSSS7DayV5",
            "temporalCoverage": "2011-08-27T00:00:00Z/2015-06-04T00:00:00Z",
            "spatialCoverage": {
            "@type": "Place",
            "geo": {
            "@type": "GeoShape",
            "box": "-179.5 -89.5 179.5 89.5"
            }
            }
        </xsl:element>
    </xsl:template>

    <xsl:template match="dap:Float64|dap:Float32" mode="json-ld">
        {
        "@type": "PropertyValue",
        "name": "<xsl:value-of select="@name"/>",
        <xsl:if test="dap:Attribute[@name='standard_name']">
            "alternateName": "<xsl:value-of select="dap:Attribute[@name='standard_name']/dap:Value"/>",
        </xsl:if>
        <xsl:if test="dap:Attribute[@name='long_name']">
            "description": "<xsl:value-of select="dap:Attribute[@name='long_name']/dap:Value"/>",
        </xsl:if>
        "valueReference": [ <xsl:for-each select="dap:Attribute" >
        <xsl:apply-templates select="." mode="json-ld" /><xsl:if test="position()!=last()">,</xsl:if>
    </xsl:for-each>
        ],
        <xsl:if test="dap:Attribute[@name='vmax']">
            "maxValue": "<xsl:value-of select="dap:Attribute[@name='vmax']/dap:Value"/>",
        </xsl:if>

        <xsl:if test="dap:Attribute[@name='vmin']">
            "minValue": "<xsl:value-of select="dap:Attribute[@name='vmin']/dap:Value"/>",
        </xsl:if>
        "propertyID": "<xsl:choose>
        <xsl:when test="dap:Attribute[@name='standard_name']"><xsl:value-of select="dap:Attribute[@name='standard_name']"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="@name"/></xsl:otherwise>
    </xsl:choose>",

        <xsl:if test="dap:Attribute[@name='units']">
            "unitText": "<xsl:value-of select="dap:Attribute[@name='units']/dap:Value"/>",
        </xsl:if>},
    </xsl:template>

    <xsl:template match="dap:Attribute" mode="json-ld">
        {
        "@type": "PropertyValue",
        "name": "<xsl:value-of select="@name"/>",
        <xsl:if test="dap:Attribute">"value": [
            <xsl:for-each select="dap:Attribute"><xsl:apply-templates select="." mode="json-ld" /><xsl:if test="position()!=last()">,</xsl:if></xsl:for-each>
            ]
        </xsl:if>
        <xsl:if test="dap:Value"><xsl:apply-templates mode="json-ld" /><xsl:if test="position()!=last()">,</xsl:if>
        </xsl:if>
        }
    </xsl:template>

    <xsl:template match="dap:Value" mode="json-ld">"value": "<xsl:value-of select="."/>"</xsl:template>

    <xsl:template match="*" mode="json-ld" />

</xsl:stylesheet>