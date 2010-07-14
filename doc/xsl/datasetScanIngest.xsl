<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE xsl:stylesheet []>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:ncml="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
                xmlns:xlink="http://www.w3.org/1999/xlink"

                >
    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>

    <xsl:template match="@* | node()">
            <xsl:apply-templates />
    </xsl:template>


    <xsl:template match="thredds:service">
        <xsl:copy-of select="."/>
    </xsl:template>

    

    <xsl:template match="thredds:catalog">
        <catalogIngest>
            <xsl:apply-templates />
        </catalogIngest>
    </xsl:template>

    <xsl:template match="thredds:datasetScan[thredds:metadata/@inherited='true']">

        <xsl:variable name="serviceName" select="thredds:metadata/thredds:serviceName"/>

        <xsl:variable name="datasetScanLocation">
            <xsl:choose>
                <xsl:when test="substring(@location,string-length(@location))='/'">
                    <xsl:value-of select="@location"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat(@location,'/')"/>
                </xsl:otherwise>

            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="datasetScanName" select="@name"/>

        <xsl:variable name="serviceElement" select="key('service-by-name', $serviceName)"/>

        <xsl:variable name="dapServices"
                      select="$serviceElement[@serviceType='OPENDAP'] | $serviceElement/thredds:service[@serviceType='OPENDAP'] "/>

        <datasetScanIngest name="{$datasetScanName}">

            <xsl:for-each select="$dapServices">

                <xsl:variable name="base" select="@base"/>

                <xsl:variable name="lastCharOfBase" select="substring($base,string-length($base))"/>

                <xsl:variable name="metadataRootPath">
                    <xsl:choose>

                        <xsl:when test="$lastCharOfBase='/' and starts-with($datasetScanLocation,'/')">
                            <xsl:variable name="location"
                                          select="substring($datasetScanLocation,2,string-length($datasetScanLocation))"/>
                            <xsl:variable name="targetURL" select="concat($base,$location)"/>
                            <xsl:value-of select="$targetURL"/>
                        </xsl:when>

                        <xsl:when test="$lastCharOfBase!='/' and not(starts-with($datasetScanLocation,'/'))">
                            <xsl:variable name="targetURL" select="concat($base,'/',$datasetScanLocation)"/>
                            <xsl:value-of select="$targetURL"/>
                        </xsl:when>

                        <xsl:otherwise>
                            <xsl:variable name="targetURL" select="concat($base,$datasetScanLocation)"/>
                            <xsl:value-of select="$targetURL"/>
                        </xsl:otherwise>

                    </xsl:choose>

                </xsl:variable>

                <metadataRootPath>
                    <xsl:value-of select="$metadataRootPath"/>
                </metadataRootPath>

            </xsl:for-each>

            <xsl:copy-of select="thredds:metadata[@inherited='true']"/>

        </datasetScanIngest>

    </xsl:template>


</xsl:stylesheet>

