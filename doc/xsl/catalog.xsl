<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
<!ENTITY DBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:dapwcs="http://www.opendap.org/ns/dapwcs"

                >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>



    <!--***********************************************
       -
       -
     -->
    <xsl:template match="showCatalog">
        <thredds:catalog>
            <thredds:service name="OPeNDAP-Hyrax" serviceType="OPeNDAP" base="/opendap/hyrax/"/>
            <xsl:apply-templates />
        </thredds:catalog>
    </xsl:template>



    <!--***********************************************
       -
       -
     -->
    <xsl:template match="response">
            <xsl:apply-templates />
    </xsl:template>


    <!--***********************************************
       -
       -
     -->
    <xsl:template match="dataset">


        <xsl:if test="dataset">
            <thredds:dataset name="{name}" ID="{name}">
                <thredds:serviceName>OPeNDAP-Hyrax</thredds:serviceName>
                <thredds:metadata inherited="true">
                    <thredds:serviceName>OPeNDAP-Hyrax</thredds:serviceName>
                    <thredds:authority>opendap.org</thredds:authority>
                </thredds:metadata>
                <xsl:apply-templates />
            </thredds:dataset>
        </xsl:if>

        <xsl:if test="not(dataset)">

            <xsl:if test="@thredds_collection='true'">
                <thredds:catalogRef name="{name}" xlink:href="{name}/catalog.xml" xlink:title="{name}" ID="{../name}/{name}"/>
            </xsl:if >

            <xsl:if test="not(@thredds_collection='true')">
                <thredds:dataset name="{name}" urlPath="{../name}/{name}" ID="{../name}/{name}">
                    <thredds:dataSize units="bytes">
                        <xsl:value-of select="size" />
                    </thredds:dataSize>
                    <thredds:date type="modified">
                        <xsl:value-of select="lastmodified/date" />T<xsl:value-of select="lastmodified/time" />
                    </thredds:date>
                </thredds:dataset>
            </xsl:if >

        </xsl:if>



    </xsl:template>


    <xsl:template match="name">
    </xsl:template>

    <xsl:template match="size">
    </xsl:template>

    <xsl:template match="lastmodified">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="date">
    </xsl:template>

    <xsl:template match="time">
    </xsl:template>

    <xsl:template match="count">
    </xsl:template>



</xsl:stylesheet>

