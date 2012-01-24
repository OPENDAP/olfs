<?xml version="1.0" encoding="UTF-8"?>

<!-- 
This stylesheet will not work with Xalan or Saxon 6 but does work with
saxon 9. 
-->

<xsl:stylesheet exclude-result-prefixes="xs" version="2.0"
    xmlns:xml="http://www.w3.org/XML/1998/namespace" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:dap="http://xml.opendap.org/ns/DAP2">

    <!-- The start and end date for the discrete URLs in the dataset. Passed
    as an individual param to simplify parsing -->
    <xsl:param name="date_range"/>

    <!-- N tuples of files, URLs and dates that make up the dateset -->
    <xsl:param name="url_date_file"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:variable name="many_ddx2ncml_version">1.0.0</xsl:variable>

    <xsl:template match="@*|text()"/>
    <xsl:template match="*"/>

    <xsl:template match="dap:Dataset">
        <netcdf title="joinNew Aggregation with explicit string coordValue.">
            <attribute type="Structure" name="NCML_Global">
                <xsl:element name="attribute">
                    <xsl:attribute name="type">String</xsl:attribute>
                    <xsl:attribute name="name">startDate</xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="fn:substring-before($date_range, ' ')"/></xsl:attribute>
                </xsl:element>
                
                <xsl:element name="attribute">
                    <xsl:attribute name="type">String</xsl:attribute>
                    <xsl:attribute name="name">endDate</xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="fn:substring-after($date_range, ' ')"/></xsl:attribute>
                </xsl:element>
            </attribute>
            <aggregation type="joinNew" dimName="time">
                <!-- Choose variables for the aggreagation -->
                <!-- 
                <variableAgg name="u"/>
                <variableAgg name="v"/>
                --> 
                <xsl:for-each select="dap:Grid">
                    <xsl:element name="variableAgg">
                        <xsl:attribute name="name">
                            <xsl:value-of select="fn:normalize-space(@name)"/>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:for-each>
                
                <!-- Add each URL explicitly -->
                <!-- 
                <netcdf title="Dataset 1" location="data/ncml/fnoc1.nc" coordValue="Station_1"/>
                <netcdf title="Dataset 2" location="data/ncml/fnoc1.nc" coordValue="Station_2"/>
                <netcdf title="Dataset 3" location="data/ncml/fnoc1.nc" coordValue="Station_3"/>
                -->
                <xsl:call-template name="write_many_urls">
                    <xsl:with-param name="url_date_file" select="$url_date_file"/>
                </xsl:call-template>
                
                <!-- Inherit all attributes from files -->
                
                <!-- Add attributes for new 'time' coordinate variable -->
                <variable name="time" type="string">
                    <!-- Metadata here will also show up in the Grid map -->
                    <!-- String _CoordinateAxisType "Time" -->
                    <attribute name="_CoordinateAxisType" type="string">Time</attribute>
                </variable>
                
            </aggregation>
            
        </netcdf>
    </xsl:template>


    <xsl:template name="write_many_urls">
        <xsl:param name="url_date_file"/>

        <!-- Peel off the first tuple -->
        <xsl:variable name="first" select="fn:substring-before($url_date_file, ' ')"/>
        <xsl:variable name="rest" select="fn:substring-after($url_date_file, ' ')"/>

        <!-- <xsl:comment>first: <xsl:value-of select="$first"/></xsl:comment> -->
        
        <!-- Now separate the tuple's parts -->
        <xsl:variable name="dataset_url" select="fn:substring-before($first, '*')"/>
        <xsl:variable name="tmp" select="fn:substring-after($first, '*')"/>
        <xsl:variable name="date" select="fn:substring-before($tmp, '*')"/>
        <xsl:variable name="filename" select="fn:substring-after($tmp, '*')"/>

        <!-- Write the single OtherEntity element -->
        <xsl:call-template name="write_one_url">
            <xsl:with-param name="dataset_url" select="$dataset_url"/>
            <xsl:with-param name="date" select="$date"/>
            <xsl:with-param name="filename" select="$filename"/>
        </xsl:call-template>

        <!-- Call on rest of arg -->
        <xsl:if test="$rest">
            <xsl:call-template name="write_many_urls">
                <xsl:with-param name="url_date_file" select="$rest"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="write_one_url">
        <xsl:param name="filename"/>
        <xsl:param name="dataset_url"/>
        <xsl:param name="date"/>
        <!--
        <xsl:comment> filename: <xsl:value-of select="$filename"/></xsl:comment>
        <xsl:comment> dataset_url: <xsl:value-of select="$dataset_url"/></xsl:comment>
        <xsl:comment> date: <xsl:value-of select="$date"/></xsl:comment>
        -->
        <!-- <netcdf title="Dataset 1" location="data/ncml/fnoc1.nc" coordValue="1.2"/>
        -->
        <xsl:element name="netcdf">
            <xsl:attribute name="title"><xsl:value-of select="$dataset_url"/></xsl:attribute>
            <xsl:attribute name="location"><xsl:value-of select="$filename"/></xsl:attribute>
            <xsl:attribute name="coordValue"><xsl:value-of select="$date"/></xsl:attribute>
        </xsl:element>
    </xsl:template>

    <!-- Not used -->
    
    <xsl:template name="abstract">
        <abstract>
            <para>This Dataset contains the following DAP Grid variables: <xsl:for-each select="dap:Grid">
                <xsl:value-of select="fn:normalize-space(dap:Attribute[@name='long_name'])"/>
                (<xsl:value-of select="fn:normalize-space(@name)"/>) </xsl:for-each> This
                Dataset contains the following DAP Array variables: <xsl:for-each select="dap:Array">
                    <xsl:value-of select="fn:normalize-space(dap:Attribute[@name='long_name'])"/>
                    (<xsl:value-of select="fn:normalize-space(@name)"/>) </xsl:for-each>
            </para>
            <xsl:text>
            </xsl:text>
            <!-- Add references and comment (CF-1.0) attributes, if present -->
            <xsl:if
                test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='references']">
                <para>
                    <xsl:value-of
                        select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='references'])"
                    />
                </para>
            </xsl:if>
            <xsl:text>
                
            </xsl:text>
            <xsl:if
                test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='comment']">
                <para>
                    <xsl:value-of
                        select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='comment'])"
                    />
                </para>
            </xsl:if>
        </abstract>
    </xsl:template>
    
    <xsl:template name="title">
        <title>
            <xsl:choose>
                <xsl:when
                    test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='title']">
                    <xsl:value-of
                        select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='title']"
                    />
                </xsl:when>
                <xsl:otherwise> Title not found. many_ddx2eml version: <xsl:value-of
                    select="$many_ddx2ncml_version"/>
                </xsl:otherwise>
            </xsl:choose>
        </title>
    </xsl:template>
    
    <xsl:variable name="Institution">
        <xsl:choose>
            <xsl:when
                test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='institution']">
                <xsl:value-of
                    select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='institution']"
                />
            </xsl:when>
            <xsl:otherwise> Institution not found. many_ddx2eml version: <xsl:value-of
                select="$many_ddx2ncml_version"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    
    <xsl:template name="creator">
        <creator>
            <organizationName>
                <xsl:value-of select="$Institution"/>
            </organizationName>
        </creator>
    </xsl:template>
    
    <xsl:template name="contact">
        <contact>
            <organizationName>
                <xsl:value-of select="$Institution"/>
            </organizationName>
            <electronicMailAddress>
                <xsl:choose>
                    <xsl:when
                        test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='contact']">
                        <xsl:value-of
                            select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='contact']"
                        />
                    </xsl:when>
                    <xsl:otherwise> Contact not found. many_ddx2eml version: <xsl:value-of
                        select="$many_ddx2ncml_version"/>
                    </xsl:otherwise>
                </xsl:choose>
            </electronicMailAddress>
        </contact>
    </xsl:template>
    
    <xsl:template name="coverage">
        <xsl:param name="date_range"/>
        
        <coverage id="coverage_dataset">
            <geographicCoverage>
                <geographicDescription>See 'boundingCoordinates'.</geographicDescription>
                
                <boundingCoordinates>
                    <westBoundingCoordinate>
                        <xsl:value-of
                            select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='westernmost_longitude'])"
                        />
                    </westBoundingCoordinate>
                    <eastBoundingCoordinate>
                        <xsl:value-of
                            select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='easternmost_longitude'])"
                        />
                    </eastBoundingCoordinate>
                    <northBoundingCoordinate>
                        <xsl:value-of
                            select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='northernmost_latitude'])"
                        />
                    </northBoundingCoordinate>
                    <southBoundingCoordinate>
                        <xsl:value-of
                            select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='southernmost_latitude'])"
                        />
                    </southBoundingCoordinate>
                </boundingCoordinates>
            </geographicCoverage>
            
            <xsl:variable name="start_date" select="fn:substring-before($date_range, ' ')"/>
            <xsl:variable name="end_date" select="fn:substring-after($date_range, ' ')"/>
            <!-- 
                <xsl:comment> start date: <xsl:value-of select="$start_date"/></xsl:comment>
                <xsl:comment> end date: <xsl:value-of select="$end_date"/></xsl:comment>
            -->
            <temporalCoverage>
                <rangeOfDates>
                    <beginDate>
                        <calendarDate>
                            <xsl:call-template name="iso8601-date">
                                <xsl:with-param name="date" select="$start_date"/>
                            </xsl:call-template>
                        </calendarDate>
                    </beginDate>
                    <endDate>
                        <calendarDate>
                            <xsl:call-template name="iso8601-date">
                                <xsl:with-param name="date" select="$end_date"/>
                            </xsl:call-template>
                        </calendarDate>
                    </endDate>
                </rangeOfDates>
            </temporalCoverage>
        </coverage>
    </xsl:template>
    
    <!-- This code handles parsing the dates and times in a CF-1.0 compliant
        dataset -->
    
    <xsl:template name="iso8601-date">
        <xsl:param name="date"/>
        <xsl:choose>
            <xsl:when test="fn:matches($date, '(\n|\s)*(\d{4}-\d{2}-\d{2}).*(\n|\s)*')">
                <xsl:value-of
                    select="fn:replace($date, '(\n|\s)*(\d{4}-\d{2}-\d{2}).*(\n|\s)*', '$2')"/>
            </xsl:when>
            <xsl:otherwise>Unknown: <xsl:value-of select="$many_ddx2ncml_version"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="iso8601-time">
        <xsl:param name="time"/>
        <!-- xsl:comment>Raw param value to iso8601-time is: <xsl:value-of select="$time"/></xsl:comment -->
        <xsl:choose>
            <xsl:when test="fn:matches($time, '(\n|\s)*.*(UTC|utc|Z)(\d{2}:\d{2}:\d{2}).*(\n|\s)*')">
                <xsl:value-of
                    select="fn:replace($time, '(\n|\s)*.*(\d{2}:\d{2}:\d{2}).*(\n|\s)*', '$2Z')"/>
            </xsl:when>
            <xsl:when test="fn:matches($time, '(\n|\s)*.*T(\d{2}:\d{2}:\d{2}).*(\n|\s)*')">
                <xsl:value-of
                    select="fn:replace($time, '(\n|\s)*.*(\d{2}:\d{2}:\d{2}).*(\n|\s)*', '$2')"/>
            </xsl:when>
            <xsl:otherwise>Unknown iso 8601 time: <xsl:value-of select="$many_ddx2ncml_version"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
</xsl:stylesheet>
