<?xml version="1.0" encoding="ISO-8859-1" ?>
<!--
/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dap="http://xml.opendap.org/ns/DAP/3.2#"
        >
    <xsl:import href="version.xsl" />
    <xsl:param name="datasetID" />
    <xsl:param name="dapService" />
    <xsl:param name="docsService" />
    <xsl:param name="webStartService" />
    <xsl:param name="webStartApplications" />
    <xsl:output method='xhtml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:strip-space elements="*" />


    <xsl:variable name="webStartAppsLinks">

        <p>
        <h4> Data Viewers</h4>
            <xsl:choose>
                <xsl:when test="$webStartApplications">
                    <ul>
                        <xsl:apply-templates mode="WebStartLinks" select="$webStartApplications"/>
                    </ul>
                </xsl:when>
                <xsl:otherwise>
                    <p><i>This server cannot locate a Java WebStart data viewer or a data analysis application for this dataset.</i></p>
                </xsl:otherwise>
            </xsl:choose>
        </p>

    </xsl:variable>
   
    <xsl:template match="wsApp" mode="WebStartLinks">
        <li><a href="{$webStartService}/{@id}?dapService={$dapService}&#38;datasetID={$datasetID}"><xsl:value-of select="@applicationName"/></a></li>
    </xsl:template>



    <xsl:template match="/dap:Dataset">

        <html>





        <!-- ****************************************************** -->
        <!--                      PAGE HEADER                       -->
        <!--                                                        -->
        <!--                                                        -->

        <head>
            <link rel='stylesheet' href='{$docsService}/css/contents.css'
                  type='text/css'/>
            <title>DAP Dataset: <xsl:value-of select="@name"/></title>

        </head>

        <!-- ****************************************************** -->
        <!--                      PAGE BANNER                       -->
        <!--                                                        -->
        <!--                                                        -->
        <body>

            <table width="100%">
                <tr>
                    <td width="30%" align="left"><img alt="Logo" src='{$docsService}/images/logo.gif' /></td>
                </tr>
            </table>

            <h1><font size="0">Dataset: <xsl:value-of select="@name"/> </font><br/>
            <font class="small_italic">(<xsl:value-of select="@xml:base"/>)</font>  </h1>

            <xsl:copy-of select="$webStartAppsLinks"/>

            <!-- xsl:call-template name="DatasetDetail"/ -->



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
            <h1><font size="0">OPeNDAP Hyrax <font class="small">(<xsl:value-of select="$HyraxVersion"/>)</font>

                <br/>
                <a href='{$docsService}/'>Documentation</a>
                </font>
            </h1>

        </body>


        </html>
    </xsl:template>







    <xsl:template  match="@*|text()" />
    <xsl:template  match="@*|text()" mode="Grid" />
    <xsl:template  match="@*|text()" mode="AttributeTextBox" />






    <xsl:template name="DatasetDetail">
        <h3>Dataset Details</h3>
        <xsl:comment><xsl:copy/></xsl:comment>
        <table border="0">
            <tr><td align="right"><font class="medium_bold">Global Attributes:</font></td><td/></tr>
            <tr><td/>
            <td>
                <xsl:call-template  name="AttributeTextBox">
                    <xsl:with-param name="boxID" select="'globalatt_text'"/>
                </xsl:call-template>

            </td>
            </tr>
            <tr><td/><td><hr/></td></tr>

            <tr><td align="right"><font class="medium_bold">Variables:</font></td><td/></tr>

            <xsl:for-each select="dap:*[not(dap:Attribute)]">

                <xsl:comment><xsl:value-of select="local-name()"/>(<xsl:value-of select="@name"/>)</xsl:comment>
                <tr>
                    <td></td>
                    <td>

                        <xsl:apply-templates select="."/>
                            
                    </td>
                </tr>
            </xsl:for-each>

        </table>
          
    </xsl:template>




    <xsl:template match="dap:Grid">
            <td align="right" valign="top"><font class="medium_bold"><xsl:value-of select="@name"/>: </font></td>
            <td align="left">
                <xsl:apply-templates mode="Grid">
                </xsl:apply-templates>
            </td>
    </xsl:template>









    <xsl:template match="dap:Array">
        <dl>
            <dt valign="top"><font class="medium_bold"><xsl:value-of select="@name"/>: </font></dt>
            <dd>
                <font class="small_italic">Array of <xsl:apply-templates mode="ArrayTypeDescription"/></font><font class="medium"><xsl:apply-templates select="dap:dimension"/></font>
                <br/>
                <font class="small">Metadata:</font>
                <xsl:apply-templates select="." mode="AttributeTextBox">
                    <xsl:with-param name="boxID" select="concat(@name,'_text')"/>
                </xsl:apply-templates>
            </dd>
        </dl>
                
    </xsl:template>

    <xsl:template match="dap:Array" mode="Grid">
        <font class="small_italic">Grid of <xsl:apply-templates mode="ArrayTypeDescription"/></font><font class="medium"><xsl:apply-templates select="dap:dimension"/></font>
        <br/>
        <font class="small">Metadata:</font>
        <br/>
        <xsl:apply-templates select="." mode="AttributeTextBox">
            <xsl:with-param name="boxID" select="concat(@name,'_text')"/>
        </xsl:apply-templates>
    </xsl:template>


    <xsl:template match="dap:Map">

        <tr>
            <td align="right" valign="top"><font class="medium_bold"><xsl:value-of select="@name"/>: </font></td>
            <td>
                <font class="small_italic">Map of <xsl:apply-templates mode="ArrayTypeDescription"/></font><font class="medium"><xsl:apply-templates select="dap:dimension"/></font>
                <br/>
                <font class="small">Metadata:</font>
                <xsl:apply-templates select="." mode="AttributeTextBox">
                    <xsl:with-param name="boxID" select="concat(@name,'_text')"/>
                </xsl:apply-templates>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="dap:Structure">
        <xsl:comment>Structure <xsl:value-of select="@name"/> Start</xsl:comment>
        <dl>
            <dt align="left" valign="top">
                <font class="medium_bold"><xsl:value-of select="@name"/>:
                </font>
            </dt>
            <dd>
                <font class="small_italic">Is a Structure</font>
                <br/>
                <font class="small">Metadata:</font>
                <xsl:call-template name="AttributeTextBox">
                    <xsl:with-param name="boxID" select="concat(@name,'_text')"/>
                </xsl:call-template>

                <xsl:comment>Structure <xsl:value-of select="@name"/> Applying Templates</xsl:comment>
                <xsl:apply-templates  />
                <xsl:comment>Structure <xsl:value-of select="@name"/> Done</xsl:comment>

            </dd>
        </dl>

    </xsl:template>

    

    <xsl:template match="dap:dimension">
        [<xsl:value-of select="@name"/>=<xsl:value-of select="@size"/>]
    </xsl:template>







    <xsl:template match="dap:Sequence">
        <tr>
            <td align="right" valign="top"><font class="medium_bold"><xsl:value-of select="@name"/>: </font></td>
            <td>
                <font class="small_italic">Sequence</font>
                <br/>
                <font class="small">Metadata:</font>
                <xsl:apply-templates select="." mode="AttributeTextBox">
                    <xsl:with-param name="boxID" select="concat(@name,'_text')"/>
                </xsl:apply-templates>
            </td>
            <table>
                <xsl:apply-templates/>
            </table>
        </tr>
    </xsl:template>




    <xsl:template  match="*|@*|text()" mode="ArrayTypeDescription" />

    <xsl:template match="dap:Byte" mode="ArrayTypeDescription">
        8 bit bytes
    </xsl:template>

    <xsl:template match="dap:Int16" mode="ArrayTypeDescription">
        16 bit integers
    </xsl:template>

    <xsl:template match="dap:Int32" mode="ArrayTypeDescription">
        32 bit integers
    </xsl:template>

    <xsl:template match="dap:Float32" mode="ArrayTypeDescription">
        32 bit floats
    </xsl:template>

    <xsl:template match="dap:Float64" mode="ArrayTypeDescription">
        64 bit floats
    </xsl:template>


    <xsl:template match="dap:String" mode="ArrayTypeDescription">
        Strings
    </xsl:template>

    <xsl:template match="dap:URL" mode="ArrayTypeDescription">
        URLs
    </xsl:template>

    <xsl:template match="dap:Structure" mode="ArrayTypeDescription">
        Structures
    </xsl:template>

    <xsl:template match="dap:Sequence" mode="ArrayTypeDescription">
        Sequences
    </xsl:template>

    <xsl:template match="dap:Grid" mode="ArrayTypeDescription">
        Grids
    </xsl:template>










    <xsl:template  name="AttributeTextBox">
        <xsl:param name="boxID"/>

        <xsl:choose>
            <xsl:when test="dap:Attribute">
                <br/>
                <textarea name="{$boxID}" rows="5" cols="70">
                    <xsl:for-each select="dap:Attribute">
                        <xsl:apply-templates select="." mode="AttributeText"/>
                    </xsl:for-each>

                </textarea>

            </xsl:when>
            <xsl:otherwise>
                <!-- textarea name="{$boxID}" rows="1" cols="70">No Metadata found.</textarea-->
                <font class="small_italic"> No Metadata found.</font>
            </xsl:otherwise>
        </xsl:choose>


    </xsl:template>

    <xsl:template match="dap:Attribute" mode="AttributeText">
        <xsl:param name="ParentName" />
        <xsl:choose>
            <xsl:when test="dap:Attribute">
                <xsl:apply-templates mode="AttributeText">
                    <xsl:with-param name="ParentName" select="concat($ParentName,@name,'.')"/>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:when test="@type='OtherXML'">
                <xsl:value-of select="@name" />:
                <xsl:copy-of select="*"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($ParentName,@name)" />: <xsl:for-each select="dap:value">
                    <xsl:value-of select="." />
                </xsl:for-each>
                <xsl:value-of select="'&#13;'" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>





</xsl:stylesheet>
