<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet will not work with Xalan or Saxon 6 but does work with
saxon 9. -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xs"
    version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xml="http://www.w3.org/XML/1998/namespace"
    xmlns:fn="http://www.w3.org/2005/xpath-functions" 
    xmlns:dap="http://xml.opendap.org/ns/DAP2"
    xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.0 https://code.ecoinformatics.org/code/eml/tags/RELEASE_EML_2_1_0/eml.xsd"
    xmlns:eml="eml://ecoinformatics.org/eml-2.1.0">

    <!-- This odd variable definition is how you get a newline in the output
        which goes a long way toward making the result readable -->
    <xsl:variable name="newline">
<xsl:text>
</xsl:text>
    </xsl:variable>
    
    <xsl:variable name="newline-space">
<xsl:text>
            
</xsl:text>
    </xsl:variable>


    <!-- PackageId should be something that's likely to be unique 
        and also be machine and location independent. I'm going to 
        use the 'filename' part of the URL for this. -->
    <xsl:param name="filename"/>

    <!-- The dataset's URL on a server. Ideally the EML would stay the same
        as a granule moved, but that's clearly not going to be the case for
        the value of the <url> elements, where this parameter will be used -->
    <xsl:param name="dataset_url"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:variable name="ddx2eml_version">3.0.0</xsl:variable>

    <xsl:template match="@*|text()"/>
    <xsl:template match="*"/>

    <xsl:template match="dap:Dataset">
        <eml:eml>
            <xsl:attribute name="packageId">
                <xsl:value-of select="$filename"/>
            </xsl:attribute>
            <xsl:attribute name="system">DAP</xsl:attribute>
            <dataset>
                <xsl:call-template name="preface"/>
                <!-- Find the correct place for this -->
                <!--<xsl:call-template name="abstract"/-->
                
                <xsl:call-template name="cf_1.0_entity_information"/>
            </dataset>
        </eml:eml>
    </xsl:template>
    
    <xsl:template name="preface">
        <title>
            <xsl:choose>
                <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='title']">
                    <xsl:value-of select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='title']"/>
                </xsl:when>
                <xsl:otherwise> Title not found. ddx2eml version: <xsl:value-of
                    select="$ddx2eml_version"/>
                </xsl:otherwise>
            </xsl:choose>
        </title>
        
        <xsl:variable name="Institution">
            <xsl:choose>
                <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='institution']">
                    <xsl:value-of select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='institution']"/>
                </xsl:when>
                <xsl:otherwise> Institution not found. ddx2eml version: <xsl:value-of
                    select="$ddx2eml_version"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <creator>
            <organizationName>
                <xsl:value-of select="$Institution"/>
            </organizationName>
        </creator>
        
        <contact>
            <organizationName>
                <xsl:value-of select="$Institution"/>
            </organizationName>
            <electronicMailAddress>
                <xsl:choose>
                    <xsl:when test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='contact']">
                        <xsl:value-of select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='contact']"/>
                    </xsl:when>
                    <xsl:otherwise> Contact not found. ddx2eml version: <xsl:value-of
                        select="$ddx2eml_version"/>
                    </xsl:otherwise>
                </xsl:choose>
            </electronicMailAddress>
        </contact>
    </xsl:template>
    
    <xsl:template name="abstract">
        <abstract> This URL contains the following DAP Grid variables: <!-- Extract all of the variables using a loop -->
            <xsl:for-each select="dap:Grid">
                <xsl:value-of select="normalize-space(dap:Attribute[@name='long_name'])"/> (<xsl:value-of select="normalize-space(@name)"/>)
            </xsl:for-each>
            <xsl:text>
            </xsl:text>            
            <!-- Add references and comment (CF-1.0) attributes, if present -->
            <xsl:if test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='references']">
                <xsl:value-of select="normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='references'])"/>
            </xsl:if>
            <xsl:text>
                
            </xsl:text>
            <xsl:if test="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='comment']">
                <xsl:value-of select="normalize-space(//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[fn:lower-case(@name)='comment'])"/>
            </xsl:if>
        </abstract>
    </xsl:template>
        
    <xsl:template name="cf_1.0_entity_information">
        <otherEntity>
        <entityName>
            <xsl:value-of select="$filename"/>
        </entityName>

        <physical>
            <objectName>
                <xsl:value-of select="$dataset_url"/>
            </objectName>
            <encodingMethod> DAP XDR </encodingMethod>
            <dataFormat>
                <externallyDefinedFormat>
                    <formatName>DAP</formatName>
                    <formatVersion>2.0</formatVersion>
                </externallyDefinedFormat>
            </dataFormat>
        </physical>

        <coverage>
            <geographicCoverage>
                <geographicDescription>See 'boundingCoordinates'.</geographicDescription>

                <boundingCoordinates>
                    <westBoundingCoordinate>
                        <xsl:value-of
                            select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='westernmost_longitude']"
                        />
                    </westBoundingCoordinate>
                    <eastBoundingCoordinate>
                        <xsl:value-of
                            select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='easternmost_longitude']"
                        />
                    </eastBoundingCoordinate>
                    <northBoundingCoordinate>
                        <xsl:value-of
                            select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='northernmost_latitude']"
                        />
                    </northBoundingCoordinate>
                    <southBoundingCoordinate>
                        <xsl:value-of
                            select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='southernmost_latitude']"
                        />
                    </southBoundingCoordinate>
                </boundingCoordinates>
            </geographicCoverage>
            <temporalCoverage>
                <rangeOfDates>
                    <beginDate>
                        <calendarDate>
                            <xsl:value-of
                                select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='start_date']"
                            />
                        </calendarDate>
                        <time>
                            <xsl:value-of
                                select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='start_time']"
                            />
                        </time>
                    </beginDate>
                    <endDate>
                        <calendarDate>
                            <xsl:value-of
                                select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='stop_date']"
                            />
                        </calendarDate>
                        <time><xsl:value-of select="//dap:Attribute[fn:matches(@name,'GLOBAL')]/dap:Attribute[@name='stop_time']"
                            />
                        </time>
                    </endDate>
                </rangeOfDates>
            </temporalCoverage>
        </coverage>

        <entityType>Online data accessible using DAP 2.0</entityType>
 
        </otherEntity>
    </xsl:template>

</xsl:stylesheet>
