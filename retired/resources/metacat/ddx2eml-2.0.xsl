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

<!-- This stylesheet will not work with Xalan or Saxon 6 but does work with
saxon 9. -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xs" version="1.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

                xmlns:dap="http://xml.opendap.org/ns/DAP2"

                xmlns:eml="eml://ecoinformatics.org/eml-2.0.1"
        >
    
    <xsl:param name="datasetId"/>
    <xsl:param name="XML_BASE"/>
    
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <!--xsl:variable name="XML_BASE"><xsl:value-of select="/dap:Dataset/@xml:base" /></xsl:variable-->

    <xsl:variable name="Title" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Title']"/>
    
    <xsl:template match="@*|text()"/>
    <xsl:template match="*"/>

    <xsl:template match="dap:Dataset">
        <eml:eml
            packageId="http://www.marine.csiro.au/dods/nph-dods/dods-data/ocean_colour/czcs/global/8_day/netcdf/czcs_1978_8D_chlor.nc.eml"
            system="DAP2.0">
            <dataset>
                <title>
                    <xsl:value-of select="$Title"/>
                </title>
                <xsl:call-template name="getOtherXML">
                    <xsl:with-param name="elementNamespace"></xsl:with-param>
                    <xsl:with-param name="elementName">*</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates/>
            </dataset>
        </eml:eml>
    </xsl:template>

    <xsl:template match="dap:Grid[./dap:Map/@name='lat' and ./dap:Map/@name='lon']">
        <spatialRaster>
            <entityName>
                <xsl:value-of select="$datasetId"/>
            </entityName>
            <physical>
                <!-- Change this to the DAP variable to access??? -->
                <objectName>
                    <xsl:value-of select="@name"/>
                </objectName>
                <dataFormat>
                    <externallyDefinedFormat>
                        <formatName>DAP</formatName>
                    </externallyDefinedFormat>
                </dataFormat>
                <distribution>
                    <online>
                        <url>
                            <xsl:value-of select="$XML_BASE"/>
                        </url>
                    </online>
                </distribution>
            </physical>

            <coverage>
                <geographicCoverage>
                    <geographicDescription/>
                    <boundingCoordinates>
                        <xsl:apply-templates mode="spatialCoordinateMapsLongitude"/>
                        <xsl:apply-templates mode="spatialCoordinateMapsLatitude"/>                   
                    </boundingCoordinates>
                </geographicCoverage>
                <xsl:apply-templates mode="temporalCoverage"/>
            </coverage>

            <attributeList>
                <!-- An attribute in EML is roughly a variable in DAP -->
                <attribute>
                    <attributeName>
                        <xsl:value-of select="@name"/>
                    </attributeName>
                    <xsl:call-template name="getOtherXML">
                        <xsl:with-param name="elementNamespace"/>
                        <xsl:with-param name="elementName">attributeDefinition</xsl:with-param>
                    </xsl:call-template>
                    <measurementScale>
                        <ratio>
                            <unit>
                                <standardUnit>
                                    <xsl:call-template name="GetEmlUnits">
                                        <xsl:with-param name="unitString"  select="normalize-space(dap:Attribute[@name='units']/dap:value)" />
                                    </xsl:call-template>
                                    
                                </standardUnit>
                            </unit>
                            <numericDomain>
                                <xsl:apply-templates mode="ArrayTemplateTypeConverstion"/>
                            </numericDomain>
                        </ratio>
                    </measurementScale>
                </attribute>
            </attributeList>

            <xsl:call-template name="MapProjection_To_SpatialReference"/>

        </spatialRaster>
    </xsl:template>

    <xsl:template name="dap:Array" match="dap:Array" mode="ArrayTemplateTypeConverstion">
        <xsl:apply-templates mode="ArrayTemplateTypeConverstion"/>
    </xsl:template>
    <xsl:template name="dap:dimension" match="dap:dimension" mode="ArrayTemplateTypeConverstion"/>
    <xsl:template name="dap:Attribute" match="dap:Attribute" mode="ArrayTemplateTypeConverstion"/>
    <xsl:template name="dap:Map" match="dap:Map" mode="ArrayTemplateTypeConverstion"/>
    <xsl:template name="dap" match="dap:*" mode="ArrayTemplateTypeConverstion">
        <xsl:apply-templates select="." mode="dapToEmlTypeConversion"/>
    </xsl:template>
    <xsl:template match="@*|text()" mode="ArrayTemplateTypeConverstion"/>
    <xsl:template match="*" mode="ArrayTemplateTypeConverstion"/>
    
    <!--
        -  DAP Types to EML Type Converter
        -
        -
        -
        -  EML Schema fragment for numberType
            
           <xs:element name="numberType" type="NumberType">
               <xs:annotation>
                   <xs:appinfo>
                       <doc:tooltip>number type</doc:tooltip>
                       <doc:documentation>The type of number recorded in
                           this attribute.  Values can be 'whole', 'natural',
                           'integer' or 'real'.</doc:documentation>
                   </xs:appinfo>
               </xs:annotation>
           </xs:element>
    -->
    
    <xsl:template match="dap:*" mode="dapToEmlTypeConversion">
        <dap:error>
            <dap:msg> Type conversion failed. Reason: The DAP type <xsl:copy-of select="."/> has no
                representtion in EML. </dap:msg>
        </dap:error>
    </xsl:template>
    <xsl:template match="dap:Byte" mode="dapToEmlTypeConversion">
        <numberType>integer</numberType>
    </xsl:template>
    <xsl:template match="dap:In6" mode="dapToEmlTypeConversion">
        <numberType>integer</numberType>
    </xsl:template>
    <xsl:template match="dap:Int32" mode="dapToEmlTypeConversion">
        <numberType>integer</numberType>
    </xsl:template>
    <xsl:template match="dap:Float32" mode="dapToEmlTypeConversion">
        <numberType>real</numberType>
    </xsl:template>
    <xsl:template match="dap:Float64" mode="dapToEmlTypeConversion">
        <numberType>real</numberType>
    </xsl:template>
    <xsl:template match="@*|text()" mode="dapToEmlTypeConversion"/>
    <xsl:template match="*" mode="dapToEmlTypeConversion"/>

    <!-- Spatial Coordinate Map Longitude mode -->
    <xsl:template match="dap:Map[@name='lon' or @name='longitude']" mode="spatialCoordinateMapsLongitude">
        <westBoundingCoordinate>
            <xsl:value-of select="normalize-space(dap:Attribute[@name='valid_min'])"/>
        </westBoundingCoordinate>
        <eastBoundingCoordinate>
            <xsl:value-of select="normalize-space(dap:Attribute[@name='valid_max'])"/>
        </eastBoundingCoordinate>
    </xsl:template>
    <xsl:template match="@*|text()" mode="spatialCoordinateMapsLongitude"/>
    <xsl:template match="*" mode="spatialCoordinateMapsLongitude"/>
    
    <!-- Spatial Coordinate Map Latitude mode -->
    <xsl:template match="dap:Map[@name='lat' or @name='latitude']" mode="spatialCoordinateMapsLatitude">
        <northBoundingCoordinate>
            <xsl:value-of select="normalize-space(dap:Attribute[@name='valid_max'])"/>
        </northBoundingCoordinate>
        <southBoundingCoordinate>
            <xsl:value-of select="normalize-space(dap:Attribute[@name='valid_min'])"/>
        </southBoundingCoordinate>
    </xsl:template>
    <xsl:template match="@*|text()" mode="spatialCoordinateMapsLatitude"/>
    <xsl:template match="*" mode="spatialCoordinateMapsLatitude"/>

    <!-- Temporal Coverage Map mode -->
    <xsl:template match="dap:Map[@name='time' or @name='Time']" mode="temporalCoverage">
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">temporalCoverage</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="@*|text()" mode="temporalCoverage"/>
    <xsl:template match="*" mode="temporalCoverage"/>

    <xsl:template name="getOtherXML">
        <xsl:param name="elementNamespace"/>
        <xsl:param name="elementName"/>

        <xsl:for-each select="dap:Attribute[@type='OtherXML']">
            
            <!-- xsl:comment>dap:Attribute ns: '<xsl:value-of select="$ns"/>'</xsl:comment -->
            <xsl:apply-templates mode="getOtherXML">
                <xsl:with-param name="elementNamespace" select="$elementNamespace"/>
                <xsl:with-param name="elementName" select="$elementName"/>
            </xsl:apply-templates>
            
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="@*|text()" mode="getOtherXML"/>
    <xsl:template match="*" mode="getOtherXML"/>

    <xsl:template match="*" mode="getOtherXML">
        <xsl:param name="elementNamespace"/>
        <xsl:param name="elementName"/>
        
        <!-- xsl:variable name="isDesiredElement"
            select="if($elementName='*' or (local-name()=$elementName and namespace-uri()=$elementNamespace)) then true() else false()"/>
        <!- -xsl:comment>
            $elementNamespace: '<xsl:value-of select="$elementNamespace"/>'
            $elementName:      '<xsl:value-of select="$elementName"/>'
            $ns:               '<xsl:value-of select="$ns"/>'
            ./@name:           '<xsl:value-of select="./@name"/>'
            $isDesiredElement: '<xsl:value-of select="$isDesiredElement"/>'
            </xsl:comment- ->
        <xsl:if test="$isDesiredElement"><!- - Don't process the namespace Attribute - ->
            <xsl:copy-of select="."/>
        </xsl:if-->
        <xsl:choose>
            <xsl:when test="$elementName = '*'">
                <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:when test="local-name() = $elementName and namespace-uri() = $elementNamespace">
                <xsl:copy-of select="."/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    
    <!--
    
    <xsl:template match="dap:Attribute[@name='OtherXML']" >
        <xsl:variable name="ns" select="normalize-space(dap:Attribute[@name='namespace']/dap:value)"/> 
        <xsl:comment>dap:Attribute ns: '<xsl:value-of select="$ns"/>'</xsl:comment>
        <xsl:apply-templates mode="OtherXML">
            <xsl:with-param name="parent_ns" select="$ns" />
        </xsl:apply-templates>
    </xsl:template>
     -->

    <xsl:template match="dap:value" mode="MultiValuedAttribute">
        <xsl:param name="ns"/>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template name="MultiValuedAttribute">
        <xsl:param name="ns"/>

        <xsl:choose>
            <xsl:when test="@name='keywordSet'">
                <xsl:apply-templates mode="keywordSet">
                    <xsl:with-param name="ns" select="$ns"/>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates mode="MultiValuedAttribute">
                    <xsl:with-param name="ns" select="$ns"/>
                </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <xsl:template match="dap:value" mode="keywordSet">
        <xsl:param name="ns"/>
        <xsl:element name="keyword" namespace="{$ns}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template name="SpatialReference">
        <spatialReference>
            <horizCoordSysDef name="Sphere_Equidistant_Cylindrical">
                <geogCoordSys>
                    <datum/>
                    <spheroid/>
                    <primeMeridian longitude="0"/>
                    <unit name="degree"/>
                </geogCoordSys>
            </horizCoordSysDef>
            <!-- horizCoordSysName>Sphere_Equidistant_Cylindrical</horizCoordSysName -->
        </spatialReference>

    </xsl:template>

    <xsl:template name="MapProjection_To_SpatialReference">
        <xsl:variable name="mapProjectionAttribute" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Map_Projection']"/>
        <xsl:variable name="mapProjectionString" select="$mapProjectionAttribute/dap:value"/>
        <xsl:choose>
            <xsl:when test="$mapProjectionString='Equidistant Cylindrical'">
                <spatialReference>
                    <horizCoordSysDef name="Sphere_Equidistant_Cylindrical">
                        <geogCoordSys>
                            <datum/>
                            <spheroid/>
                            <primeMeridian longitude="0"/>
                            <xsl:call-template name="CoordinateSystemUnits"/>
                        </geogCoordSys>
                    </horizCoordSysDef>
                </spatialReference>
            </xsl:when>
            <xsl:otherwise>
                <dap:error>
                    <dap:msg> The value of the dap:Attribute named Map_Projection: <xsl:copy-of
                            select="$mapProjectionAttribute"/> is not mapped to an allowed
                        horizCoordSysDef. </dap:msg>
                </dap:error>
            </xsl:otherwise>
        </xsl:choose>

        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">horizontalAccuracy</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">verticalAccuracy</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getCellSize"/>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">numberOfBands</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">rasterOrigin</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">rows</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">columns</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">verticals</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="getOtherXML">
            <xsl:with-param name="elementNamespace"/>
            <xsl:with-param name="elementName">cellGeometry</xsl:with-param>
        </xsl:call-template>
       
    </xsl:template>

    <xsl:template name="CoordinateSystemUnits">
        <xsl:variable name="unitsAttribute" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Longitude_Units']"/>
        <xsl:variable name="units" select="$unitsAttribute/dap:value"/>
        <xsl:choose>
            <xsl:when test="$units='degrees East'">
                <unit name="degree"/>
            </xsl:when>
            <xsl:otherwise>
                <dap:error>
                    <dap:msg> The value of the dap:Attribute named Longitude_Units: <xsl:copy-of
                            select="$unitsAttribute"/> is not mapped to an allowed unit value.
                    </dap:msg>
                </dap:error>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <xsl:template name="getCellSize">
        <xsl:variable name="Latitude_Step" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Latitude_Step']/dap:value"/>
        <xsl:variable name="Longitude_Step" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Longitude_Step']/dap:value"/>       
        <cellSizeXDirection><xsl:value-of select="$Longitude_Step"/></cellSizeXDirection>
        <cellSizeYDirection><xsl:value-of select="$Latitude_Step"/></cellSizeYDirection>
     </xsl:template>
        
    <xsl:template name="GetEmlUnits">
        <xsl:param name="unitString"/>
        <xsl:choose>
            <xsl:when test="$unitString='mg m^-3'">milligramsPerCubicMeter</xsl:when>
            <xsl:otherwise>
                <dap:error>
                    <dap:msg> The value of the units string: <xsl:copy-of
                        select="$unitString"/> is not mapped to an allowed EML unit value.
                    </dap:msg>
                </dap:error>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>

</xsl:stylesheet>
