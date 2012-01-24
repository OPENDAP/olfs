<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet will not work with Xalan or Saxon 6 but does work with
saxon 9. -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xs" version="1.0"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xml="http://www.w3.org/XML/1998/namespace"
        
    xmlns:dap="http://xml.opendap.org/ns/DAP2"

    xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.0 https://code.ecoinformatics.org/code/eml/tags/RELEASE_EML_2_1_0/eml.xsd"
    xmlns:eml="eml://ecoinformatics.org/eml-2.1.0">
    
    <!-- PackageId should be something that's likely to be unique 
        and also to machine and location independent. I'm going to 
        use the 'filename' part of the URL for this. -->
    <xsl:param name="filename"/>
    
    <!-- The dataset's URL on a server. Ideally the EML would stay the same
        as a granule moved, but that's clearly not going to be the case for
        the value of the <url> elements, where this parameter will be used -->
    <xsl:param name="dataset_url"/>
    
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    
    <xsl:variable name="ddx2eml_version">2.1.1</xsl:variable>
    
    <xsl:variable name="Title">
        <xsl:choose>
            <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='Title']">
                <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='Title']"/>
            </xsl:when>
            
            <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='title']">
                <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='title']"/>
            </xsl:when>
            
            <xsl:otherwise>
                Title not found. <xsl:value-of select="$ddx2eml_version"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
          
    <xsl:variable name="Institution">
        <xsl:choose>
            <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='Institution']">
                <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='Institution']"/>
            </xsl:when>
            
            <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='institution']">
                <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='institution']"/>
            </xsl:when>
            
            <xsl:otherwise>
                Institution not found. <xsl:value-of select="$ddx2eml_version"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="Contact">
        <xsl:choose>
            <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='Contact']">
                <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='Contact']"/>
            </xsl:when>
            
            <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='contact']">
                <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='contact']"/>
            </xsl:when>
            
            <xsl:otherwise>
                Contact not found. <xsl:value-of select="$ddx2eml_version"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="@*|text()"/>
    <xsl:template match="*"/>

    <xsl:template match="dap:Dataset">
        <eml:eml>
             <xsl:attribute name="packageId"><xsl:value-of select="$filename"/></xsl:attribute>
            <xsl:attribute name="system">DAP</xsl:attribute>
            <dataset>
                <title>
                    <xsl:value-of select="$Title"/>
                </title>
                <creator>
                    <organizationName><xsl:value-of select="$Institution"/></organizationName>
                </creator>
                <contact>
                    <organizationName><xsl:value-of select="$Institution"/></organizationName>
                    <electronicMailAddress><xsl:value-of select="$Contact"/></electronicMailAddress>
                </contact>
                
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
                <xsl:value-of select="$dataset_url"/>
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
                            <xsl:value-of select="$dataset_url"/>?<xsl:value-of select="@name"/>
                        </url>
                    </online>
                </distribution>
            </physical>

            <coverage>
                <geographicCoverage>
                    <xsl:choose>
                        <xsl:when test="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='northernmost_latitude']">
                            <geographicDescription>This granule has no identifiable information about its projection. 
                                The geographic range covers latitude <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='northernmost_latitude']"/>
                                to <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='southernmost_latitude']"/>
                                and longitude <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='easternmost_longitude']"/>
                                to <xsl:value-of select="//dap:Attribute[@name='NC_GLOBAL'] /dap:Attribute[@name='westernmost_longitude']"/>.
                            </geographicDescription>                                                        
                        </xsl:when>
                        <xsl:otherwise>
                            <geographicDescription>This granule has no identifiable information about its geographic extent or projection. 
                                See the 'boundingCoordinates' element.</geographicDescription>                            
                        </xsl:otherwise>    
                    </xsl:choose>
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
                    <xsl:choose>
                        <xsl:when test="dap:Attribute[@name='long_name']">
                            <attributeDefinition><xsl:value-of select="dap:Attribute[@name='long_name']"/></attributeDefinition>                            
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="getOtherXML">
                                <xsl:with-param name="elementNamespace"/>
                                <xsl:with-param name="elementName">attributeDefinition</xsl:with-param>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                    <measurementScale>
                        <ratio>
                            <unit>
                                <xsl:call-template name="GetEmlUnits">
                                    <xsl:with-param name="unitString"  select="normalize-space(dap:Attribute[@name='units']/dap:value)"/>
                                </xsl:call-template>
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
    <xsl:template match="dap:Int16" mode="dapToEmlTypeConversion">
        <numberType>integer</numberType>
    </xsl:template>
    <xsl:template match="dap:UInt16" mode="dapToEmlTypeConversion">
        <numberType>integer</numberType>
    </xsl:template>
    <xsl:template match="dap:Int32" mode="dapToEmlTypeConversion">
        <numberType>integer</numberType>
    </xsl:template>
    <xsl:template match="dap:UInt32" mode="dapToEmlTypeConversion">
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
                <xsl:comment>ddx2eml transform could not find recognizable map projection information</xsl:comment>
                <!-- dap:error>
                    <dap:msg> The value of the dap:Attribute named Map_Projection: <xsl:copy-of
                            select="$mapProjectionAttribute"/> is not mapped to an allowed
                        horizCoordSysDef. </dap:msg>
                </dap:error -->
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
        <!-- As an alternative, the code could get teh size of the array (i.e.,
            teh number of cells) and the number of degrees and compute the 
            degrees per cell -->
        <xsl:variable name="Latitude_Step" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Latitude_Step']/dap:value"/>
        <xsl:variable name="Longitude_Step" select="//dap:Attribute[@name='NC_GLOBAL']/dap:Attribute[@name='Longitude_Step']/dap:value"/>       
        <cellSizeXDirection><xsl:value-of select="$Longitude_Step"/></cellSizeXDirection>
        <cellSizeYDirection><xsl:value-of select="$Latitude_Step"/></cellSizeYDirection>
     </xsl:template>
        
    <xsl:template name="GetEmlUnits">
        <xsl:param name="unitString"/>
        <xsl:choose>
            <xsl:when test="$unitString='mg m^-3'">
                <standardUnit>milligramsPerCubicMeter</standardUnit>
            </xsl:when>
            <xsl:when test="$unitString='kelvin'">
                <standardUnit>kelvin</standardUnit>
            </xsl:when>
            <xsl:when test="$unitString=''">
                <customUnit>None</customUnit>
            </xsl:when>
            <xsl:otherwise>
                <customUnit><xsl:value-of select="$unitString"/></customUnit>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>

</xsl:stylesheet>
