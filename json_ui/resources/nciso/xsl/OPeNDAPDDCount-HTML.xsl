<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dap="http://xml.opendap.org/ns/DAP/3.2#">
    <xsl:import href="ddx2iso-score-components.xsl"/>
    <xd:doc xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" scope="stylesheet">
        <xd:desc>
            <xd:p><xd:b>Created on:</xd:b>Feb. 4, 2011
            </xd:p>
            <xd:p><xd:b>Author:</xd:b>ted.habermann@noaa.gov
            </xd:p>
            <xd:p/>
        </xd:desc>
    </xd:doc>
    <xsl:param name="docsService"/>
    <xsl:param name="HyraxVersion"/>
    <xsl:output method="xml"/>




    <!-- Display Results Fields -->
    <xsl:template match="/">

        <html>

            <!-- ****************************************************** -->
            <!--                              HEADER                    -->
            <!--                                                        -->
            <!--                                                        -->

            <head>
                <link rel='stylesheet' href='{$docsService}/css/contents.css' type='text/css'/>
                <title>OPeNDAP Hyrax: NetCDF Attribute Convention for Dataset Discovery Report</title>
            </head>

            <style type="text/css">
                table {
                empty-cells:show;
                font-size: 10px;
                }
            </style>
            <body>
                <img alt="OPeNDAP Logo" src='{$docsService}/images/logo.gif'/>




                <!-- ****************************************************** -->
                <!--                  Page Content                          -->
                <!--                                                        -->
                <!--                                                        -->


                <xsl:call-template name="Summary"/>
                <xsl:call-template name="GeneralCharacteristics"/>
                <xsl:call-template name="Identification"/>
                <xsl:call-template name="TextSearch"/>
                <xsl:call-template name="ExtentSearch"/>
                <xsl:call-template name="OtherExtentInfo"/>
                <xsl:call-template name="CreatorSearch"/>
                <xsl:call-template name="ContributorSearch"/>
                <xsl:call-template name="PublisherSearch"/>
                <xsl:call-template name="OtherAttributes"/>

                <!-- ****************************************************** -->
                <!--                      FOOTER                            -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <div class="small" align="left">

                                <xsl:value-of select="$stylesheetTitle"/> Version:
                                <xsl:value-of select="$rubricVersion"/>
                                <br/>
                                <a href="https://www.nosc.noaa.gov/dmc/swg/wiki/index.php?title=NetCDF_Attribute_Convention_for_Dataset_Discovery">
                                    More Information
                                </a>
                            </div>

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

                <h3>OPeNDAP Hyrax (<xsl:value-of select="$HyraxVersion"/>)
                    <br/>
                    <a href='{$docsService}/'>Documentation</a>
                </h3>


            </body>

        </html>
    </xsl:template>


</xsl:stylesheet>
