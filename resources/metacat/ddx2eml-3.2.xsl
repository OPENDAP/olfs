<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet will not work with Xalan or Saxon 6 but does work with
saxon 9. -->

<xsl:stylesheet exclude-result-prefixes="xs"
    version="2.0" 
    xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.0 https://code.ecoinformatics.org/code/eml/tags/RELEASE_EML_2_1_0/eml.xsd"
    xmlns:xml="http://www.w3.org/XML/1998/namespace"

    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:fn="http://www.w3.org/2005/xpath-functions" 

    xmlns:dap="http://xml.opendap.org/ns/DAP2"
    xmlns:eml="eml://ecoinformatics.org/eml-2.1.0">

    <!-- EML's PackageId should be something that's likely to be unique 
        and also be machine and location independent. I'm going to 
        use the 'filename' part of the URL for this. -->
    <xsl:param name="filename"/>

    <!-- The dataset's URL on a server. Ideally the EML would stay the same
        as a granule moved, but that's clearly not going to be the case for
        the value of the <url> elements, where this parameter will be used -->
    <xsl:param name="dataset_url"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:variable name="ddx2eml_version">3.2.0</xsl:variable>

    <xsl:template match="@*|text()"/>
    <xsl:template match="*"/>

    <xsl:template match="dap:Dataset">
        <eml:eml>
            <xsl:attribute name="packageId">
                <xsl:value-of select="$filename"/>
            </xsl:attribute>
            <xsl:attribute name="system">DAP</xsl:attribute>
            
            <!-- TODO: Here we should test for Conventions == CF-1.0 or 1.1 -->
            
            <dataset>
                <xsl:call-template name="title"/>
                <xsl:call-template name="creator"/>
                <xsl:call-template name="abstract"/>
                <xsl:call-template name="coverage"/>
                <xsl:call-template name="contact"/>  
                
                <xsl:call-template name="entity_information"/>
            </dataset>
        </eml:eml>
    </xsl:template>

    <xsl:template name="abstract">
        <abstract>
            <para>This URL contains the following DAP Grid variables:
                <xsl:for-each select="dap:Grid">
                    <xsl:value-of select="fn:normalize-space(dap:Attribute[@name='long_name'])"/> (<xsl:value-of select="fn:normalize-space(@name)"/>)
                </xsl:for-each>
                
                <xsl:for-each select="dap:Array">
                    <xsl:value-of select="fn:normalize-space(dap:Attribute[@name='long_name'])"/> (<xsl:value-of select="fn:normalize-space(@name)"/>)
                </xsl:for-each>
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
                <xsl:otherwise> Title not found. ddx2eml version: <xsl:value-of
                        select="$ddx2eml_version"/>
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
                <xsl:otherwise> Institution not found. ddx2eml version: <xsl:value-of
                        select="$ddx2eml_version"/>
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
                    <xsl:otherwise> Contact not found. ddx2eml version: <xsl:value-of
                            select="$ddx2eml_version"/>
                    </xsl:otherwise>
                </xsl:choose>
            </electronicMailAddress>
        </contact>
    </xsl:template>

    <xsl:template name="coverage">
        <coverage id="coverage_dataset">
            <geographicCoverage>
                <geographicDescription>See 'boundingCoordinates'.</geographicDescription>
                
                <boundingCoordinates>
                    <westBoundingCoordinate>
                        <xsl:choose>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='westernmost_longitude']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='westernmost_longitude'])"/>
                            </xsl:when>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_leflon']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_leflon'])"/>
                            </xsl:when>
                            <xsl:otherwise> westBoundingCoordinate not found. ddx2eml version: <xsl:value-of
                                select="$ddx2eml_version"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </westBoundingCoordinate>
                    <eastBoundingCoordinate>
                        <xsl:choose>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='easternmost_longitude']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='easternmost_longitude'])"/>
                            </xsl:when>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_ritlon']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_ritlon'])"/>
                            </xsl:when>
                            <xsl:otherwise> eastBoundingCoordinate not found. ddx2eml version: <xsl:value-of
                                select="$ddx2eml_version"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </eastBoundingCoordinate>
                    <northBoundingCoordinate>
                        <xsl:choose>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='northernmost_latitude']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='northernmost_latitude'])"/>
                            </xsl:when>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_toplat']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_toplat'])"/>
                            </xsl:when>
                            <xsl:otherwise> northBoundingCoordinate not found. ddx2eml version: <xsl:value-of
                                select="$ddx2eml_version"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </northBoundingCoordinate>
                    <southBoundingCoordinate>
                        <xsl:choose>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='southernmost_latitude']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='southernmost_latitude'])"/>
                            </xsl:when>
                            <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_botlat']">
                                <xsl:value-of select="fn:normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='dsp_nav_earth_botlat'])"/>
                            </xsl:when>
                            <xsl:otherwise> southBoundingCoordinate not found. ddx2eml version: <xsl:value-of
                                select="$ddx2eml_version"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </southBoundingCoordinate>
                </boundingCoordinates>
            </geographicCoverage>
            <temporalCoverage>
                <rangeOfDates>
                    <beginDate>
                        <calendarDate>
                            <xsl:call-template name="iso8601-date">
                                <xsl:with-param name="date"
                                    select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='start_date']"
                                />
                            </xsl:call-template>
                        </calendarDate>
                        <time>
                            <xsl:call-template name="iso8601-time">
                                <xsl:with-param name="time"
                                    select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='start_time']"
                                />
                            </xsl:call-template>
                            <!--xsl:value-of select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='start_time']"/-->
                        </time>
                    </beginDate>
                    <endDate>
                        <calendarDate>
                            <xsl:call-template name="iso8601-date">
                                <xsl:with-param name="date"
                                    select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='stop_date']"
                                />
                            </xsl:call-template>
                        </calendarDate>
                        <time>
                            <xsl:call-template name="iso8601-time">
                                <xsl:with-param name="time"
                                    select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='stop_time']"
                                />
                            </xsl:call-template>
                            <!--xsl:value-of select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='stop_time']"/-->
                        </time>
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
            <xsl:otherwise>Unknown: <xsl:value-of select="$ddx2eml_version"/></xsl:otherwise>
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
            <xsl:otherwise>Unknown iso 8601 time: <xsl:value-of select="$ddx2eml_version"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="entity_information">
        
            <!--xsl:value-of select="fn:normalize-space(dap:Attribute[@name='long_name'])"/> (<xsl:value-of select="fn:normalize-space(@name)"/>) -->

        <otherEntity>
            <entityName>
                <!-- The name identifies the entity in the dataset: file name, name of database table, etc. -->
                <xsl:value-of select="$filename"/>
            </entityName>
            <entityDescription>
                <!-- Text generally describing the entity, its type, and
                    relevant information about the data in the entity. -->
                This data set is referenced by a single URL.
            </entityDescription>
            <physical>
                <objectName>
                    <!-- The name of the data object. This is possibly distinct
                        from the entity name in that one physical object can
                        contain multiple entities, even though that is not a
                        recommended practice. The objectName often is the
                        filename of a file in a file system or that is
                        accessible on the network.-->
                    <xsl:value-of select="$filename"/>
                </objectName>
                <encodingMethod>DAP XDR</encodingMethod>
                <dataFormat>
                    <externallyDefinedFormat>
                        <formatName>DAP</formatName>
                        <formatVersion>2.0</formatVersion>
                    </externallyDefinedFormat>
                </dataFormat>
                <distribution>
                    <online>
                        <url>
                            <xsl:value-of select="$dataset_url"/>
                        </url>
                    </online>
                </distribution>
            </physical>
            <coverage>
                <references>coverage_dataset</references>
            </coverage>
            <entityType>Online data accessible using DAP 2.0</entityType>
            
        </otherEntity>
        
    </xsl:template>

</xsl:stylesheet>
