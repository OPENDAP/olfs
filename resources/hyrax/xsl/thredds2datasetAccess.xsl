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
    <xsl:param name="catalogServer"/>
    <xsl:param name="catalogUrlPath"/>
    <xsl:output method='xml' encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>
    <xsl:variable name="documentRoot" select="/"/>


    <!-- This is the identity transform template. If this were the only template
       - then the output would be identical to the source document.
     -->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="thredds:catalog">
        <xsl:element name="catalog">
            <xsl:apply-templates>
                <xsl:with-param name="inheritedMetadata">
                    <xsl:element name="emptyNode"/>
                </xsl:with-param>
            </xsl:apply-templates>
        </xsl:element>

    </xsl:template>

    <xsl:template match="thredds:service"/>
    <xsl:template match="thredds:metadata"/>
    
    <xsl:template match="thredds:catalogRef">
        <xsl:element name="thredds:catalogRef">
            <xsl:attribute name="name" select="@name"/>
            <xsl:attribute name="ID" select="@ID"/>
            <xsl:attribute name="xlink:title" select="@xlink:title"/>
            <xsl:attribute name="xlink:type" select="@xlink:type"/>
            <xsl:attribute name="xlink:href">
                <xsl:choose>
                    <xsl:when test="starts-with(@xlink:href,'http://')" >
                        <xsl:value-of select="@xlink:href"/>
                    </xsl:when>
                    <xsl:when test="starts-with(@xlink:href,'/')" >
                        <xsl:value-of select="concat($catalogServer,@xlink:href)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat($catalogUrlPath,@xlink:href)"/>   
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates/>
         </xsl:element>
    </xsl:template>
    

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
        <xsl:param name="inheritedMetadata"/>

        <xsl:variable name="inheritedServiceNames">
            <xsl:apply-templates mode="serviceNames" select="$inheritedMetadata"/>
        </xsl:variable>


        <xsl:variable name="allMyServiceNames">
            <xsl:if test="@serviceName">
                <xsl:element name="thredds:serviceName">
                    <xsl:value-of select="@serviceName"/>
                </xsl:element>
            </xsl:if>
            <xsl:copy-of select="thredds:serviceName | thredds:metadata/thredds:serviceName"/>
            <xsl:copy-of select="$inheritedServiceNames"/>
        </xsl:variable>

        <xsl:variable name="defaultService">
            <xsl:choose>
                <xsl:when test="./thredds:serviceName">
                    <xsl:copy-of select="./thredds:serviceName"/>
                </xsl:when>
                <xsl:when test="./thredds:serviceName">
                    <xsl:copy-of select="./thredds:metadata/thredds:serviceName"/>
                </xsl:when>
                <xsl:when test="@serviceName">
                    <xsl:element name="thredds:serviceName">
                        <xsl:value-of select="@serviceName"/>
                    </xsl:element>
                </xsl:when>
                <xsl:when test="$inheritedServiceNames">
                    <xsl:copy-of select="$inheritedServiceNames"/>
                </xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:variable>


        <xsl:variable name="allDapServices">
            <xsl:call-template name="dapServices">
                <xsl:with-param name="serviceNames" select="$allMyServiceNames"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="defaultDapService">
            <xsl:call-template name="dapServices">
                <xsl:with-param name="serviceNames" select="$defaultService"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="urlPath" select="@urlPath"/>


        <xsl:choose>
            <xsl:when test="boolean(thredds:dataset) or boolean(thredds:catalogRef)">
                <xsl:element name="CompoundDataset">
                    <xsl:attribute name="name" select="@name"/>
                    <xsl:apply-templates>
                        <!--
                          -   Note that the followiing parameter uses an XPath that
                          -   accumulates inherited thredds:metadata elements as it descends the
                          -   hierarchy.
                          -->
                        <xsl:with-param name="inheritedMetadata"
                                        select="thredds:metadata[./@inherited='true']|$inheritedMetadata[boolean($inheritedMetadata)]"/>
                    </xsl:apply-templates>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <!--
                <xsl:if test="@urlPath">
                    <xsl:for-each select="$defaultDapService/thredds:service">
                        <dapDatasetAcces>
                            <xsl:attribute name="url" ><xsl:value-of select="concat($catalogServer,@base,$urlPath)"/></xsl:attribute>          
                        </dapDatasetAcces>
                    </xsl:for-each>
                     
                </xsl:if>
                -->
                <xsl:element name="dataset">
                    <xsl:attribute name="name" select="@name"/>
                    <xsl:if test="@urlPath">
                        <xsl:for-each select="$allDapServices/thredds:service">
                            <xsl:element name="dapDatasetAccess">
                                <xsl:attribute name="serviceName" select="@name"/>
                                <xsl:attribute name="url">
                                    <xsl:value-of select="concat($catalogServer,@base,$urlPath)"/>
                                </xsl:attribute>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:if>
                    <xsl:for-each select="thredds:access">
                        <xsl:choose>
                            <xsl:when test="@serviceName">
                                <xsl:variable name="service"
                                              select="key('service-by-name',@serviceName,$documentRoot)"/>
                                <xsl:variable name="dapService">
                                    <xsl:apply-templates mode="dapServices" select="$service"/>
                                </xsl:variable>
                                <!--
                                <xsl:copy-of select="$service"/>
                                <xsl:copy-of select="$dapService"/>
                                -->
                                <xsl:if test="$dapService">
                                    <xsl:element name="dapDatasetAccess">
                                        <xsl:attribute name="serviceName" select="$dapService/thredds:service/@name"/>
                                        <xsl:attribute name="url">
                                            <xsl:value-of select="concat($catalogServer,$dapService/thredds:service/@base,@urlPath)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:if test="$defaultDapService/thredds:service">
                                    <xsl:element name="dapDatasetAccess">
                                        <xsl:attribute name="serviceName" select="$defaultDapService/thredds:service/@name"/>
                                        <xsl:attribute name="url">
                                            <xsl:value-of
                                                    select="concat($catalogServer,$defaultDapService/thredds:service/@base,@urlPath)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template mode="dapServices" match="thredds:service">
        <xsl:copy-of select="descendant-or-self::thredds:service[lower-case(@serviceType)='opendap']"/>
    </xsl:template>


    <xsl:template name="dapServices">
        <xsl:param name="serviceNames"/>
        <xsl:for-each select="$serviceNames/thredds:serviceName">
            <xsl:apply-templates mode="dapServices" select="key('service-by-name',.,$documentRoot)"/>
        </xsl:for-each>
    </xsl:template>


    <xsl:template mode="serviceNames" match="@* | node()">
        <xsl:copy-of select="thredds:serviceName"/>
        <xsl:apply-templates mode="serviceNames"/>
    </xsl:template>


</xsl:stylesheet>

