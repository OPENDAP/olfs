<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2013 OPeNDAP, Inc.
  ~ // Author: Nathan David Potter  <ndp@opendap.org>
  ~ //
  ~ // This library is free software; you can redistribute it and/or
  ~ // modify it under the terms of the GNU Lesser General Public
  ~ // License as published by the Free Software Foundation; either
  ~ // version 2.1 of the License, or (at your option) any later version.
  ~ //
  ~ // This library is distributed in the hope that it will be useful,
  ~ // but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  ~ // Lesser General Public License for more details.
  ~ //
  ~ // You should have received a copy of the GNU Lesser General Public
  ~ // License along with this library; if not, write to the Free Software
  ~ // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
  ~ //
  ~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
  ~ /////////////////////////////////////////////////////////////////////////////
  -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:dap="http://xml.opendap.org/ns/DAP/3.2#"
        xmlns:w10n="http://w10n.org/w10n-draft-20091228/ns#">

    <xsl:import href="version.xsl" />

    <xsl:param name="serviceContext"/>
    <xsl:param name="w10nName"/>
    <xsl:param name="w10nType"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:variable name="docsService"><xsl:value-of select="$serviceContext"/>/docs</xsl:variable>
    <xsl:variable name="datasetUrl"><xsl:value-of select="/dap:Dataset/@xml:base"/></xsl:variable>
    
    
    <xsl:key name="DimensionNames" match="dap:Dimension" use="@name"/>
    

    <xsl:variable name="requestedVariable" select="/dap:Dataset/w10n:requestedVariable"/>




    
    <xsl:template match="dap:Dataset">




        <xsl:call-template name="copyright"/>





        <xhtml>
            <head>
                <link rel="stylesheet" href="{$docsService}/css/contents.css" type="text/css"/>
                <link rel="stylesheet" href="{$docsService}/css/treeView.css" type="text/css"/>
                <script type="text/javascript" src="{$serviceContext}/js/CollapsibleLists.js"><xsl:value-of select="' '"/></script>

                <title>w10n Service: meta for <xsl:value-of select="$w10nType"/> <xsl:value-of select="$w10nName"/></title>
            </head>
            <body>
                
                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->
                
                <table width="100%" >
                    <tr>
                        <td width="206px"><img alt="OPeNDAP Logo" src='{$docsService}/images/logo.gif'/></td>
                        <td align="center" class="xxlarge">w10n Service</td>
                    </tr>
                </table>


                <h1>
                    <span class="small">meta for <xsl:value-of select="$w10nType" /> </span>
                    <xsl:value-of select="$w10nName"/>
                </h1>
                <table width="100%">
                    <tr>
                        <td><span class="small"><a href="..">Parent Node</a></span></td>
                        <td align="right">
                            <span class="small">
                                JSON <a href=".?output=json"> meta </a>
                                <xsl:if test="$w10nType='leaf'">
                                    <span style="padding-left: 5px;">
                                        <a href="{substring($w10nName,0,string-length($w10nName))}?output=json"> data </a>
                                    </span>
                                </xsl:if>
                            </span>

                        </td>
                    </tr>
                </table>

                <hr size="1" noshade="noshade"/>
                
                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->

                <table width="100%" border="0">

                    <xsl:call-template name="globalAttributesRow"/>


                    <xsl:call-template name="VariablesRow"/>

                </table>

                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td> </td>
                        <td>
                            <div class="small" align="right"> Hyrax development sponsored by <a
                                href="http://www.nsf.gov/">NSF</a> , <a href="http://www.nasa.gov/"
                                    >NASA</a> , and <a href="http://www.noaa.gov/">NOAA</a>
                            </div>
                        </td>
                    </tr>
                </table>
                
                <!-- ****************************************************** -->
                <!--         HERE IS THE HYRAX VERSION NUMBER               -->
                <!--                                                        -->
                <h3>OPeNDAP Hyrax (<xsl:value-of select="$HyraxVersion"/>) <br/>
                    <a href="{$docsService}/">Documentation</a>
                </h3>
                
            </body>
            <script>CollapsibleLists.apply(true);</script>
            
        </xhtml>
        
    </xsl:template>
    
    

    <!-- ######################################## -->
    <!--  All DAP TYPES EXCEPT WHERE SPECIALIZED  -->


    <xsl:template match="dap:*" name="dapObj">

        <xsl:param name="container"/>
        <xsl:choose>
            <xsl:when test="$container">
                <li>
                    <xsl:call-template name="VariableWorker"><xsl:with-param name="container" select="$container"/></xsl:call-template>
                </li>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="VariableWorker"/>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <xsl:template name="VariableWorker">
        <xsl:param name="container"/>
        <xsl:call-template name="VariableHeader"><xsl:with-param name="container" select="$container"/></xsl:call-template>
        <xsl:call-template name="AttributesPresentation"/>

    </xsl:template>



    <!-- ######################################## -->
    <!--            CONTAINER TYPES               -->

    <xsl:template match="dap:Structure | dap:Grid | dap:Sequence">
        <xsl:param name="container"/>
        <xsl:choose>
            <xsl:when test="$container">
                <li>
                    <xsl:call-template name="ContainerWorker"><xsl:with-param name="container" select="$container"/></xsl:call-template>
                </li>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="ContainerWorker"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="ContainerWorker">
        <xsl:param name="container"/>

        <xsl:call-template name="VariableHeader"><xsl:with-param name="container" select="$container"/></xsl:call-template>
        <xsl:call-template name="AttributesPresentation"/>
        <div class="tightView">
            <ul class="collapsibleList">
                <li>
                    <div class="small_bold" style="color:#527CC1;">members</div>
                    <ul>
                        <xsl:apply-templates select="./*[not(self::dap:Attribute)]">
                            <xsl:with-param name="container"><xsl:call-template name="computeFQN">
                                <xsl:with-param name="separator">/</xsl:with-param>
                            </xsl:call-template></xsl:with-param>
                        </xsl:apply-templates>
                    </ul>
                </li>
            </ul>
        </div>
    </xsl:template>



    <!-- ######################################## -->
    <!-- Ignore the blob -->
    <xsl:template match="dap:blob" />


    <!-- ###################################################################
     -
     -    computeVarName
     -
     -
    -->
    <xsl:template match="*" name="computeVarName" mode="computeVarName">
        <xsl:variable name="myFQFunctionName">
            <xsl:call-template name="computeFQN"><xsl:with-param name="separator">_</xsl:with-param></xsl:call-template>
        </xsl:variable>
        <xsl:value-of select="concat('org_opendap_',$myFQFunctionName)"/>
    </xsl:template>
    <!-- ################################################################### -->

    <!-- ###################################################################
     -
     -    computeFQN
     -
     -
    -->
    <xsl:template match="*" name="computeFQN" mode="computeFQN">
        <xsl:param name="separator"/>
        <xsl:if test="generate-id(.)!=generate-id(/dap:Dataset)">
            <xsl:apply-templates select=".." mode="computeFQN">
                <xsl:with-param name="separator" select="$separator"/>
            </xsl:apply-templates>
            <xsl:if test="generate-id(..)!=generate-id(/dap:Dataset) and not(parent::dap:Map)" >
                <xsl:value-of select="$separator"/>
            </xsl:if>
            <xsl:value-of select="@name"/>
        </xsl:if>
    </xsl:template>
    <!-- ################################################################### -->




    

    <!-- ###################################################################
     -
     -    isContainerType
     -
     -
    -->   
    <xsl:template  name="isContainerType" >
        <xsl:choose>
            <xsl:when test="self::dap:Structure | self::dap:Grid | self::dap:Sequence">true</xsl:when>
            <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
    </xsl:template>   
    <!-- ################################################################### -->
    
    
    
    
    <!-- ###################################################################
     -
     -    Add Basic Variable controls
     -
     -
    -->
    <xsl:template name="VariableHeader">
        <xsl:param name="container"/>
        <xsl:choose>
            <xsl:when test="$container">
                <div style="color: black;margin-left:-5px;margin-top:10px">
                    <xsl:call-template name="VariableHeaderWorker">
                        <xsl:with-param name="container" select="$container"/>
                    </xsl:call-template>
                </div>
            </xsl:when>
            <xsl:otherwise>
                <div style="color: black;margin-left:-15px;margin-top:10px">
                    <xsl:call-template name="VariableHeaderWorker">
                        <xsl:with-param name="container" select="$container"/>
                    </xsl:call-template>
                </div>
            </xsl:otherwise>
        </xsl:choose>





       
    </xsl:template>


    <xsl:template name="VariableHeaderWorker" >
        <xsl:param name="container"/>


        <xsl:variable name="myType">
            <xsl:choose>
                <xsl:when test="self::dap:Array"><xsl:value-of select="name(*[not(self::dap:Attribute) and not(self::dap:dimension)])"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="name(.)"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>


        <xsl:choose>
            <xsl:when test="$container">
                <a href="{$container}/{@name}/"><xsl:value-of select="@name"/></a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$w10nType='node' ">
                        <a href="{@name}/"><xsl:value-of select="@name"/></a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@name"/>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:otherwise>
        </xsl:choose>

        <xsl:call-template name="DimHeader"/>
        <span class="small" style="vertical-align: 15%;"> (Type is <xsl:value-of select ="$myType"/>)</span>


    </xsl:template>

    <!-- ################################################################### -->
    
    
    <!-- ###################################################################
     -
     -    Dimension Display
     -
     -
    -->   
    <xsl:template name="DimHeader">
        
        <xsl:for-each select="dap:dimension">
            <xsl:variable name="dimSize"><xsl:call-template name="DimSize"/></xsl:variable>
            <span class="medium">[<xsl:if test="@name"><span class="small" style="vertical-align: 15%;"><xsl:value-of select="@name"/>=</span></xsl:if>0<span class="medium" style="vertical-align: 10%;">..</span><xsl:value-of select="$dimSize - 1"/>]</span>
        </xsl:for-each>
    </xsl:template>

    
    <xsl:template name="DimSize">
        <xsl:choose>
            <xsl:when test="@size"><xsl:value-of select="@size"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="key('DimensionNames', @name)/@size"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    

  

    
    
    
    
    <!-- ######################################## -->
    <!--            ATTRIBUTE TYPES               -->
    
    <xsl:template name="AttributesPresentation">
        <xsl:choose>
            <xsl:when test="dap:Attribute">
                <div class="tightView">
                    
                    <ul class="collapsibleList">
                        <li>
                            <div class="small_bold" style="color:#527CC1;">attributes</div>
                            <ul>
                                <xsl:apply-templates select="dap:Attribute" />
                            </ul>
                        </li>
                    </ul>   
                </div>
            </xsl:when>
            <xsl:otherwise>
                <div class="small_italic" style="color:#B0C6EB;">no attributes</div>   
            </xsl:otherwise>
        </xsl:choose>   
    </xsl:template>
    
    
    

    <xsl:template match="dap:Attribute">
        
        <xsl:choose>
            <xsl:when test="dap:Attribute">
                
                <ul class="collapsibleList">
                    <li>                    
                        <div class="tightView">  
                        <div class="small_bold"><xsl:value-of select="@name"/></div>
                        <ul>
                            <xsl:apply-templates />
                        </ul>
                        </div>
                    </li>
                </ul> 
                
            </xsl:when>
            <xsl:otherwise>
                <li>
                    <div class="small">
                        <span class="bold"><xsl:value-of select="@name"/></span>
                        <xsl:apply-templates />
                    </div>  
                </li>
               
            </xsl:otherwise>
        </xsl:choose>
         
    </xsl:template>
    
    
    <xsl:template match="dap:value">
        <div style="margin-left:15px;margin-bottom:5px;"><xsl:value-of select="."/></div>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    
    
    <!-- ############################################################################################ -->
    <!--           Page Components                                                                    -->
    


    <!-- ######################################## -->
    <!--            Global Attributes Row         -->
    <xsl:template name="globalAttributesRow">

        <xsl:if test="dap:Attribute" >

            <tr>
                <td align="right" style="vertical-align:text-top">
                    <div class="medium_bold" >Global Attributes</div>
                </td>
                <td width="100%">
                    <div style="width:100%;margin-left:10px;">
                        <xsl:apply-templates select="dap:Attribute"/>
                    </div>
                </td>
            </tr>

            <xsl:call-template name="hrRow"/>


        </xsl:if>
    </xsl:template>
    
    <!-- ######################################## -->
    <!--            HR Row                        -->    
    <xsl:template name="hrRow">
        <tr>
            <td ><hr size="1" noshade="noshade" width="80%"/></td>
            <td ><hr size="1" noshade="noshade" width="100%"/></td>
        </tr>               
    </xsl:template>
    
    
    
    
    <!-- ######################################## -->
    <!--            Variables Row                 -->    
    <xsl:template name="VariablesRow">
        <tr>

            <!-- If there is more than one child variable then print the variables header -->
            <xsl:if test="count(*) &gt; 1">
                <td align="right" style="vertical-align:text-top">
                    <div class="medium_bold">Variables</div>
                </td>

            </xsl:if>


            <td width="100%">
                <div style="margin-left:25px;">
                    <xsl:apply-templates select="./*[not(self::dap:Attribute)]"/>    
                </div>                
            </td>
        </tr>               
    </xsl:template>
    






    <!-- ######################################## -->
    <!--            COPYRIGHT NOTICE              -->

    <xsl:template name="copyright">
        <xsl:comment>
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2013 OPeNDAP, Inc.
  ~ // Author: Nathan David Potter  &lt;ndp@opendap.org&gt;
  ~ //
  ~ // This library is free software; you can redistribute it and/or
  ~ // modify it under the terms of the GNU Lesser General Public
  ~ // License as published by the Free Software Foundation; either
  ~ // version 2.1 of the License, or (at your option) any later version.
  ~ //
  ~ // This library is distributed in the hope that it will be useful,
  ~ // but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  ~ // Lesser General Public License for more details.
  ~ //
  ~ // You should have received a copy of the GNU Lesser General Public
  ~ // License along with this library; if not, write to the Free Software
  ~ // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
  ~ //
  ~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
  ~ /////////////////////////////////////////////////////////////////////////////
</xsl:comment>
    </xsl:template>





</xsl:stylesheet>
