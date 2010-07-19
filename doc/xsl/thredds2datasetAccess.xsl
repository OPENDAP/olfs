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
]>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:ncml="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
                xmlns:xlink="http://www.w3.org/1999/xlink"

                >
    <xsl:param name="catalogServer" />
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


    <xsl:template match="thredds:catalog">
        <catalog>
            <xsl:apply-templates>
                <xsl:with-param name="indent"/>
                <xsl:with-param name="inheritedMetadta"/>
            </xsl:apply-templates>            
        </catalog>
    </xsl:template>

    <xsl:template match="thredds:service" />




    <!--***********************************************
       -
       -
       -
       -
       -
       -
       -
     -->
    <xsl:template match="thredds:dataset">
        <xsl:param name="indent" />
        <xsl:param name="inheritedMetadata" />

        <xsl:variable name="inheritedServiceName">
            <xsl:if test="$inheritedMetadata">
                <xsl:copy-of select="$inheritedMetadata/descendant::thredds:ServiceName"/>    
            </xsl:if>
        </xsl:variable> 
        
        <xsl:variable name="serviceNames" >
            <xsl:if test="@serviceName">
                <xsl:element name="thredds:serviceName"><xsl:value-of select="@serviceName"/></xsl:element>
            </xsl:if>
            <xsl:copy-of select="thredds:serviceName | thredds:metadata/thredds:serviceName"/>
            <xsl:copy-of select="$inheritedServiceName"/>
        </xsl:variable>


        

        <xsl:choose>
            <xsl:when test="boolean(thredds:dataset) or boolean(thredds:catalogRef)">
                <compoundDataset>
                    <xsl:apply-templates>
                        <xsl:with-param name="indent"><xsl:value-of select="$indent" />&#160;&#160;</xsl:with-param>
                        <!--
                          -   Note that the followiing parameter uses an XPath that
                          -   accumulates inherited thredds:metadata elements as it descends the
                          -   hierarchy.
                          -->
                        <xsl:with-param name="inheritedMetadata" select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]"/>
                         
                    </xsl:apply-templates>
                    
                </compoundDataset>
            </xsl:when>
            <xsl:otherwise>
            <datasetAccess>
                <xsl:attribute name="datasetName"><xsl:value-of select="@name"/></xsl:attribute>

                <xsl:if test="$inheritedServiceName!=''">
                    <inheritedServiceName><xsl:value-of select="$inheritedServiceName"/></inheritedServiceName>
                </xsl:if>
                <serviceNames>
                    <xsl:copy-of select="$serviceNames"/>                    
                </serviceNames>
                <dapServces>
                    <xsl:for-each select="$serviceNames" >
                        <xsl:copy-of select="."/>
                        
                    </xsl:for-each>
                </dapServces>
                




            </datasetAccess>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>



</xsl:stylesheet>

