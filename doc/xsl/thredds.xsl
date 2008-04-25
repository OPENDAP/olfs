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
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>


    <xsl:template match="thredds:catalog">
        <html>
            <head>
                <link rel='stylesheet' href='/opendap/docs/css/contents.css'
                      type='text/css'/>
                <title><xsl:value-of select="@name"/></title>

            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="Logo" src='/opendap/docs/images/logo.gif'/>
                <h1>
                    <xsl:value-of select="@name"/>
                    <div class="small" align="left">
                        <xsl:if test="thredds:service">
                            <br/>services:
                            <table>
                                <xsl:apply-templates select="thredds:service" mode="banner">
                                    <xsl:with-param name="indent"> </xsl:with-param>
                                </xsl:apply-templates>
                            </table>
                            <br/>
                        </xsl:if>

                    </div>
                </h1>

                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <pre>


                    <table border="0" width="100%">
                        <tr>
                            <th align="left">Dataset</th>
                            <th align="center">ResponseLinks</th>
                        </tr>


                        <xsl:apply-templates />


                    </table>

                </pre>

                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                        </td>
                        <td>
                            <div class="small" align="right">
                                Hyrax development sponsored by
                                <a href='http://www.nsf.gov/'>NSF</a>
                                ,
                                <a href='http://www.nasa.gov/'>NASA</a>
                                , and
                                <a href='http://www.noaa.gov/'>NOAA</a>
                            </div>
                        </td>
                    </tr>
                </table>

                <!-- ****************************************************** -->
                <!--                                                        -->
                <h3>OPeNDAP Hyrax WCS Gateway

                    <br/>
                    <a href='/opendap/docs/'>Documentation</a>
                </h3>


            </body>
        </html>


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
    <xsl:template match="thredds:catalogRef">
        <xsl:param name="indent" />
        <tr>
            <td align="left">

                <xsl:if test="substring(./@xlink:href,string-length(./@xlink:href) - 3)='.xml'">
                    <xsl:value-of select="$indent"/><a href="{concat(substring(./@xlink:href,1,string-length(./@xlink:href) - 4),'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
                </xsl:if>

                <xsl:if test="not(substring(./@xlink:href,string-length(./@xlink:href) - 3))">
                    <xsl:value-of select="$indent"/><a href="{./@xlink:href}" ><xsl:value-of select="./@xlink:title"/> /</a>
                </xsl:if>

            </td>
            <td align="center">
                &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;&NBSP;
            </td>
        </tr>


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
    <xsl:template match="thredds:catalogRef" mode="EasierToReadVersion">
        <xsl:param name="indent" />
        <tr>
            <td align="left">

                <xsl:variable name="href" select="./@xlink:href" />
                <xsl:variable name="linkSuffix" select="substring($href,string-length($href) - 3)" />
                <xsl:variable name="linkBody" select="substring($href,1,string-length($href) - 4)" />

                <xsl:if test="$linkSuffix='.xml'">
                    <xsl:value-of select="$indent"/><a href="{concat($linkBody,'.html')}" ><xsl:value-of select="./@xlink:title"/> /</a>
               </xsl:if>

                <xsl:if test="not($linkSuffix='.xml')">
                    <xsl:value-of select="$indent"/><a href="{$href}" ><xsl:value-of select="./@xlink:title"/> /</a>
               </xsl:if>
            </td>
            <td align="center">
                &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;&NBSP;
            </td>
        </tr>

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
    <xsl:template match="thredds:datasetScan" >
        <xsl:param name="indent" />
        <tr>
                <td class="small" align="left">
                    <font size="0">
                    <xsl:value-of select="$indent"/><xsl:value-of select="@name" />
                    </font>(datasetScan)<font size="0">/</font>

                    <!-- xsl:if test="./@serviceName">
                        (serviceName: <xsl:value-of select="./@serviceName" />)
                    </xsl:if -->

                </td>

            <td align="center">
                &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;&NBSP;
            </td>
        </tr>
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
    <xsl:template match="thredds:datasetScan" mode="empty">
        <xsl:param name="indent" />
        <xsl:apply-templates />
    </xsl:template>

    <!--***********************************************
       -
       -
       -
       -
       -
       -
       -    <service name="OPeNDAP-Hyrax" serviceType="OPeNDAP" base="/opendap/"/>
     -->
    <xsl:template match="thredds:service" name="service">
        <xsl:param name="indent" />
    </xsl:template>


    <xsl:template match="thredds:service" name="serviceBanner" mode="banner">
        <xsl:param name="indent" />

        <tr>
            <td class="small" align="left">
                <xsl:value-of select="@name"/>&DBSP;&DBSP;&DBSP;
            </td>
            <td class="small" align="left">
                <xsl:value-of select="$indent"/><i>serviceType:</i> <xsl:value-of select="@serviceType"/>&DBSP;
            </td>
            <td class="small" align="left">
                <xsl:value-of select="$indent"/><i>base:</i> <xsl:value-of select="@base"/>&DBSP;
                <br/>
            </td>
        </tr>
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
    <xsl:template match="thredds:serviceName">
        <xsl:param name="indent" />
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
        <xsl:param name="indent" />
        <xsl:if test="thredds:dataset">
            <tr>
                <td class="small" align="left">
                    <font size="0">
                    <xsl:value-of select="$indent"/><xsl:value-of select="@name" />
                    </font>
                    <!-- xsl:if test="./thredds:serviceName">
                        (serviceName: <xsl:value-of select="./thredds:serviceName" />)
                    </xsl:if -->
                    <font size="0">/</font>

                </td>

                <td align="center">
                    &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP; - &NBSP;&NBSP;
                </td>
            </tr>
            <xsl:apply-templates>
                <xsl:with-param name="indent"><xsl:value-of select="$indent" />&DBSP;</xsl:with-param>
            </xsl:apply-templates>
         </xsl:if >

        <xsl:if test="not(thredds:dataset)">
            <xsl:variable name="sn" select="thredds:serviceName" />
            <tr>
                <td>
                    <xsl:value-of select="$indent"/><a href="{//thredds:service[@name = current()/thredds:serviceName]/@base}{@urlPath}.html" ><xsl:value-of select="@name" /></a>

                </td>
                <td align="center">
                    <a href="{//thredds:service[@name = $sn]/@base}{@urlPath}.ddx" >ddx</a>
                    <a href="{//thredds:service[@name = $sn]/@base}{@urlPath}.dds" >dds</a>
                    <a href="{//thredds:service[@name = $sn]/@base}{@urlPath}.das" >das</a>
                    <a href="{//thredds:service[@name = $sn]/@base}{@urlPath}.info" >info</a>
                    <a href="{//thredds:service[@name = $sn]/@base}{@urlPath}.html" >html</a>
                </td>
            </tr>
        </xsl:if>

    </xsl:template>




</xsl:stylesheet>

