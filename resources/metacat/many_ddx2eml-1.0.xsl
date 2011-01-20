<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet will not work with Xalan or Saxon 6 but does work with
saxon 9. 

Unlike
-->

<xsl:stylesheet exclude-result-prefixes="xs" version="2.0"
    xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.0 https://code.ecoinformatics.org/code/eml/tags/RELEASE_EML_2_1_0/eml.xsd"
    xmlns:xml="http://www.w3.org/XML/1998/namespace" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:fn="http://www.w3.org/2005/xpath-functions" 
    xmlns:dap="http://xml.opendap.org/ns/DAP2"
    xmlns:eml="eml://ecoinformatics.org/eml-2.1.0">

    <!-- The name of the file in the first URL for the dataset (the URL used
        to get the DDX that is the template used to make the bulk of the EML
        document -->
    <xsl:param name="filename"/>

    <!-- The start and end date for the discrete URLs in the dataset. Passed
    as an individual param to simplify parsing -->
    <xsl:param name="date_range"/>

    <!-- N tuples of files, URLs and dates that make up the dateset -->
    <xsl:param name="url_date_file"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:variable name="many_ddx2eml_version">1.0.0</xsl:variable>

    <xsl:template match="@*|text()"/>
    <xsl:template match="*"/>

    <xsl:template match="dap:Dataset">
        <eml:eml>
            <xsl:attribute name="packageId">
                <!-- TODO: IS there a better choice than te first filename? -->
                <xsl:value-of select="$filename"/>
            </xsl:attribute>
            <xsl:attribute name="system">DAP</xsl:attribute>

            <!-- TODO: Here we should test for Conventions == CF-1.0 or 1.1 -->

            <dataset>
                <xsl:call-template name="title"/>
                <xsl:call-template name="creator"/>
                <xsl:call-template name="abstract"/>
                <xsl:call-template name="coverage">
                    <xsl:with-param name="date_range" select="$date_range"/>
                </xsl:call-template>
                <xsl:call-template name="contact"/>

                <xsl:call-template name="write_many_other_entity">
                    <xsl:with-param name="url_date_file" select="$url_date_file"/>
                </xsl:call-template>
            </dataset>
        </eml:eml>
    </xsl:template>

    <xsl:template name="abstract">
        <abstract>
            <para>This Dataset contains the following DAP Grid variables: <xsl:for-each
                    select="dap:Grid">
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
                        select="$many_ddx2eml_version"/>
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
                    select="$many_ddx2eml_version"/>
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
                            select="$many_ddx2eml_version"/>
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
            <xsl:otherwise>Unknown iso 8601 date: <xsl:value-of select="$many_ddx2eml_version"/></xsl:otherwise>
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
            <xsl:otherwise>Unknown iso 8601 time: <xsl:value-of select="$many_ddx2eml_version"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="write_many_other_entity">
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
        <xsl:call-template name="write_single_other_entity">
            <xsl:with-param name="dataset_url" select="$dataset_url"/>
            <xsl:with-param name="date" select="$date"/>
            <xsl:with-param name="filename" select="$filename"/>
        </xsl:call-template>

        <!-- Call on rest of arg -->
        <xsl:if test="$rest">
            <xsl:call-template name="write_many_other_entity">
                <xsl:with-param name="url_date_file" select="$rest"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="write_single_other_entity">
        <xsl:param name="filename"/>
        <xsl:param name="dataset_url"/>
        <xsl:param name="date"/>
        <!--
        <xsl:comment> filename: <xsl:value-of select="$filename"/></xsl:comment>
        <xsl:comment> dataset_url: <xsl:value-of select="$dataset_url"/></xsl:comment>
        <xsl:comment> date: <xsl:value-of select="$date"/></xsl:comment>
        -->
        <otherEntity>
            <entityName>
                <!-- The name identifies the entity in the dataset: file name, name of database table, etc. -->
                <xsl:value-of select="$filename"/>
            </entityName>
            <entityDescription>
                <!-- Text generally describing the entity, its type, and
                    relevant information about the data in the entity. -->
                This is one file in a multi-file dataset. </entityDescription>
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
                <temporalCoverage>
                    <singleDateTime>
                        <calendarDate>
                            <xsl:call-template name="iso8601-date">
                                <xsl:with-param name="date" select="$date"/>
                            </xsl:call-template>
                        </calendarDate>
                        <time>
                            <xsl:call-template name="iso8601-time">
                                <xsl:with-param name="time" select="$date"/>
                            </xsl:call-template>
                        </time>
                    </singleDateTime>
                </temporalCoverage>
            </coverage>

            <entityType>Online data accessible using DAP 2.0</entityType>

        </otherEntity>

    </xsl:template>

</xsl:stylesheet>
