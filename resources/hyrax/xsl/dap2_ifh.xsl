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
    xmlns:dap="http://xml.opendap.org/ns/DAP/3.2#">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:param name="serviceContext"/>
    <xsl:param name="docsService"/>
    <xsl:param name="HyraxVersion"/>
    <xsl:param name="JsonLD"/>

    <xsl:variable name="datasetUrl"><xsl:value-of select="/dap:Dataset/@xml:base"/></xsl:variable>


    <xsl:template match="dap:Dataset">
        <xsl:call-template name="copyright"/>
        <xhtml>
            <head>
                <link rel="stylesheet" href="{$docsService}/css/contents.css" type="text/css"/>
                <link rel="stylesheet" href="{$docsService}/css/treeView.css" type="text/css"/>

                <xsl:element name="script">
                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                    <xsl:attribute name="src"><xsl:value-of select="$serviceContext"/>/js/CollapsibleLists.js</xsl:attribute>
                    <xsl:value-of select="' '"/>
                </xsl:element>

                <xsl:element name="script">
                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                    <xsl:attribute name="src"><xsl:value-of select="$serviceContext"/>/js/dap2_buttons.js</xsl:attribute>
                    <xsl:value-of select="' '"/>
                </xsl:element>

                <xsl:element name="script">
                    <xsl:attribute name="type">text/javascript</xsl:attribute>
                    DAP2_URL = new dap2_url("<xsl:value-of select="$datasetUrl"/>");
                </xsl:element>

                <title>Interface From Hell <xsl:value-of select="@name"/></title>
            </head>
            <body>
                
                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->
                
                <img alt="OPeNDAP Logo" src="{$docsService}/images/logo.png"/>
                <h1>DAP2 Dataset Access Form: <xsl:value-of select="@name"/></h1>
                <hr size="1" noshade="noshade"/>
                
                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <form action="">
                    <input type="checkbox" id="debug_checkbox" />DEBUG
                    <script type="text/javascript">
                        DEBUG = new debug_obj(debug_checkbox);
                    </script>
                    
     
                    <table width="100%" border="0">
                        <xsl:call-template name="dataRequestButtonsRow"/>
                        <xsl:call-template name="dataUrlRow"><xsl:with-param name="datasetUrl" select="$datasetUrl"/></xsl:call-template>                       

                        <xsl:call-template name="hrRow"/>
                        <xsl:call-template name="globalAttributesRow"/>
                        
                        <xsl:call-template name="hrRow"/>
                        <xsl:call-template name="VariablesRow"/>
                        
                    </table>
                    
                </form>
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
                
                <xsl:element name="script">
                    <xsl:attribute name="type">application/ld+json</xsl:attribute>
                    <xsl:value-of select="$JsonLD" />
                </xsl:element>
            </body>
            <script>CollapsibleLists.apply(true);</script>

        </xhtml>
        
    </xsl:template>
    
    


    


    
    <!-- ######################################## -->
    <!--            ATOMIC TYPES               -->   
    <!-- INTEGER TYPES -->
    <!--
    <xsl:template match="dap:Int8"> </xsl:template>
    <xsl:template match="dap:UInt8"> </xsl:template>
    <xsl:template match="dap:Int16"> </xsl:template>
    <xsl:template match="dap:UInt16"> </xsl:template>
    <xsl:template match="dap:Int32"> </xsl:template>
    <xsl:template match="dap:UInt32"> </xsl:template>
    <xsl:template match="dap:Int64"> </xsl:template>
    <xsl:template match="dap:UInt64"> </xsl:template>
    -->
    <!-- FLOATING POINT TYPES -->
    <!--
    <xsl:template match="dap:Float32"> </xsl:template>
    <xsl:template match="dap:Float64"> </xsl:template>
    -->
    <!-- STRING TYPES -->
    <!--
    <xsl:template match="dap:String"> </xsl:template>
    <xsl:template match="dap:URI"> </xsl:template>
    -->

    <xsl:template match="dap:*" name="dapObj">
        <xsl:param name="parentContainer"/>
        <xsl:choose>
            <xsl:when test="$parentContainer">
                <li>
                    <xsl:call-template name="VariableWorker">
                        <xsl:with-param name="parentContainer" select="$parentContainer"/>
                    </xsl:call-template>
                </li>                
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="VariableWorker"/>
            </xsl:otherwise>
        </xsl:choose>        
    </xsl:template>
    
    <xsl:template name="VariableWorker">
        <xsl:param name="parentContainer"/>
        <xsl:call-template name="VariableHeader"><xsl:with-param name="parentContainer" select="$parentContainer"/></xsl:call-template>
        <xsl:call-template name="AttributesPresentation"/>                                 
    </xsl:template>
    

    <!-- ######################################## -->
    <!--            CONTAINER TYPES               -->
    
    <xsl:template match="dap:Structure  | dap:Sequence">
        <xsl:param name="parentContainer"/>

        <xsl:choose>
            <xsl:when test="$parentContainer">
                <li>
                    <xsl:call-template name="ContainerWorker">
                        <xsl:with-param name="parentContainer" select="$parentContainer"/>
                    </xsl:call-template>
                </li>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="ContainerWorker">
                    <xsl:with-param name="parentContainer"><xsl:call-template name="computeVarName"/></xsl:with-param>
                </xsl:call-template>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="ContainerWorker">
        <xsl:param name="parentContainer"/>

        <xsl:call-template name="VariableHeader"><xsl:with-param name="parentContainer" select="$parentContainer"/></xsl:call-template>
        <xsl:call-template name="AttributesPresentation"/>                                 
        <div class="tightView">
            <ul class="collapsibleList">
                <li>
                    <div class="small_bold" style="color:#527CC1;">members</div>
                    <ul>
                        <xsl:apply-templates select="./*[not(self::dap:Attribute)]">
                            <xsl:with-param name="parentContainer"><xsl:call-template name="computeVarName"/></xsl:with-param>
                        </xsl:apply-templates>    
                    </ul>
                </li>
            </ul>   
        </div>        
    </xsl:template>
    

    

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
        <xsl:param name="parentContainer"/>
        
        
        <xsl:variable name="myFQN">
            <xsl:call-template name="computeFQN"><xsl:with-param name="separator">.</xsl:with-param></xsl:call-template>
        </xsl:variable>
        
        <xsl:variable name="myJSVarName"><xsl:call-template name="computeVarName"/></xsl:variable>
        
        <xsl:variable name="isContainer"><xsl:call-template name="isContainerType"/></xsl:variable>
        
        <xsl:variable name="checkBoxName" select="concat('get_',$myJSVarName)"/>

        <xsl:variable name="isArray">
            <xsl:choose>
                <xsl:when test="self::dap:Array | self::dap:Map">true</xsl:when>
                <xsl:otherwise>false</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="myType">
            <xsl:choose>
                <xsl:when test="self::dap:Array | self::dap:Map"><xsl:value-of select="name(*[not(self::dap:Attribute) and not(self::dap:dimension)])"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="name(.)"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:element name="script">
            <xsl:attribute name="type">text/javascript</xsl:attribute>
            if(DEBUG.enabled()) alert(
                "myFQN:         <xsl:value-of select="$myFQN"/>\n" +
                "myJSVarName:   <xsl:value-of select="$myJSVarName"/>\n" +
                "checkBoxName:  <xsl:value-of select="$checkBoxName"/>\n" +
                "isContainer:   <xsl:value-of select="$isContainer"/>\n" +
                "isArray:       <xsl:value-of select="$isArray"/>\n" +
                "myType:        <xsl:value-of select="$myType"/>\n" +
                "parentContainer:        <xsl:value-of select="$parentContainer"/>\n"
            );


            <xsl:value-of select="$myJSVarName"/> = new dap_var("<xsl:value-of select="$myFQN"/>", "<xsl:value-of select="$myJSVarName"/>", <xsl:value-of select="$isArray"/>,<xsl:value-of select="$isContainer"/>);

            <xsl:if test="parent::dap:Dataset">
                DAP2_URL.add_dap_var(<xsl:value-of select="$myJSVarName"/>);
            </xsl:if>

            <xsl:value-of select="$myJSVarName"/>.checkBox = "<xsl:value-of select="$checkBoxName"/>";
            <xsl:if test="$parentContainer">
                <xsl:comment>- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -</xsl:comment>
                <xsl:comment>Added by VariableHeader because test="$container" is true.</xsl:comment>
                <xsl:value-of select="$parentContainer"/>.addChildVar(<xsl:value-of select="$myJSVarName"/>);
                <xsl:value-of select="$myJSVarName"/>.parentContainer = <xsl:value-of select="$parentContainer"/>;
                <xsl:comment>- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -</xsl:comment>
            </xsl:if>
        </xsl:element>
        


        <div style="color: black;margin-left:-20px;margin-top:10px">
            <input type="checkbox" id="{$checkBoxName}" onclick="{$myJSVarName}.handle_projection_change({$checkBoxName})"  onfocus="describe_projection()" />
            <xsl:value-of select="@name"/> 
            
            <!--span class="small">
                <xsl:if test="$container">
                    (child of <xsl:value-of select="$container"/>)
                </xsl:if>
            </span-->
            <xsl:call-template name="DimHeader"/>
            <span class="small" style="vertical-align: 15%;"> (Type is <xsl:value-of select ="$myType"/>)</span>
        </div>  
    
        <xsl:call-template name="DimSlicing">
            <xsl:with-param name="myJSVarName" select="$myJSVarName"/>
        </xsl:call-template>
        
        <xsl:if test="$isArray='false' and  not(self::dap:Structure) and parent::dap:Sequence">
            <xsl:call-template name="selectionOperator">
                <xsl:with-param name="myJSVarName" select="$myJSVarName"/>
                <xsl:with-param name="index" select="'0'"/>
            </xsl:call-template>
        </xsl:if>
             
       
    </xsl:template>
    <!-- ################################################################### -->
    
    
    <!-- ###################################################################
     -
     -    Dimension Slicing Controls
     -
     -
    -->   
    <xsl:template name="DimHeader">
        
        <xsl:for-each select="dap:dimension">
            <xsl:variable name="dimSize"><xsl:call-template name="DimSize"/></xsl:variable>
            <span class="medium">[<xsl:if test="@name"><span class="small" style="vertical-align: 15%;"><xsl:value-of select="@name"/>=</span></xsl:if>0<span class="medium" style="vertical-align: 10%;">..</span><xsl:value-of select="$dimSize - 1"/>]</span>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="DimSlicing">
        <xsl:param name="myJSVarName"/>

        <xsl:for-each select="dap:dimension">
            <xsl:variable name="dimSize"><xsl:call-template name="DimSize"/></xsl:variable>
            <xsl:variable name="dimTag" select="concat($myJSVarName,'_dim_',position())"/>
            
            <input type="text" id="{$dimTag}" size="8" oninput="autoResize(event)" onfocus="describe_index()" onChange="DAP2_URL.update_url()" />
            <xsl:element name="script">
                <xsl:attribute name="type">text/javascript</xsl:attribute>
                <xsl:value-of select="$myJSVarName"/>.addDimension(<xsl:value-of select="$dimTag"/>,<xsl:value-of select="$dimSize"/>);
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
    
    
    <xsl:template name="DimSize">
        <xsl:choose>
            <xsl:when test="@size"><xsl:value-of select="@size"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="key('DimensionNames', @name)/@size"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <!-- ###################################################################
     -
     -    Selection Controls
     -
     -
    -->   
    <xsl:template name="selectionOperator">
        <xsl:param name="myJSVarName"/>
        <xsl:param name="index"/>
        
        <xsl:variable name="relOpWidget">
            <xsl:value-of select="$myJSVarName"/>_relOpWidget_<xsl:value-of select="$index"/>
        </xsl:variable>
        <xsl:variable name="rValueWidget">
            <xsl:value-of select="$myJSVarName"/>_rValueWidget_<xsl:value-of select="$index"/>
        </xsl:variable>
        
        
        <span class="medium" style="margin-left: 10px;">
            <xsl:value-of select="@name"/>
        </span>
        <select id="{$relOpWidget}" onfocus="describe_selection()" onchange="DAP2_URL.update_url()">
            <option value="=" selected="">=</option>
            <option value="!=">!=</option>
            <option value="&lt;">&lt;</option>
            <option value="&lt;=">&lt;=</option>
            <option value=">">></option>
            <option value=">=">>=</option>
            <option value="-">--</option>
        </select>
        <input type="text" id="{$rValueWidget}" size="6" onFocus="describe_selection()" onChange="DAP2_URL.update_url()"/>

        <xsl:element name="script">
            <xsl:attribute name="type">text/javascript</xsl:attribute>
            <xsl:value-of select="$myJSVarName"/>.addSelectionClause(<xsl:value-of select="$relOpWidget"/>,<xsl:value-of select="$rValueWidget"/>);
        </xsl:element>

    </xsl:template>
    
    
    
    
  
    
    
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
    <!--            Actions Buttons Row           -->
    <xsl:template name="dataRequestButtonsRow">
        <tr>
            <td align="right"><div class="medium_bold"><a href="opendap_form_help.html#disposition" target="help">Actions</a> </div>
            </td>
            <td >
                <div style="width:100%;margin-left:10px;">
                    <input type="button" value="Get ASCII" onclick="ascii_button()"/>
                    <input type="button" value="Get as NetCDF 3" onclick="binary_button('nc')"/>
                    <input type="button" value="Get as NetCDF 4" onclick="binary_button('dap.nc4')"/>
                    <input type="button" value="Binary (DAP) Object" onclick="binary_button('dods')"/>
                    <input type="button" value="Show Help" onclick="help_button()"/>
                </div>
            </td>
        </tr>

    </xsl:template>
  

    <!-- ######################################## -->
    <!--            Datarequest URL Row           -->
    <xsl:template name="dataUrlRow">
        <xsl:param name="datasetUrl"/>
        <tr>
            <td align="right">
                <div class="medium_bold"><a href="opendap_form_help.html#data_url" target="help">Data URL</a> </div>
            </td>
            <td>
                <input name="url" type="text" style="width:98%;margin-left:10;" value="{$datasetUrl}"></input>
            </td>
        </tr>       
    </xsl:template>
    
    
    <!-- ######################################## -->
    <!--            Global Attributes Row         -->
    <xsl:template name="globalAttributesRow">
        <tr>
            <td align="right" style="vertical-align:text-top">
                <div class="medium_bold" ><a href="opendap_form_help.html#global_attr" target="help">Global Attributes</a> </div>
            </td>
            <td width="100%">
                <div style="width:100%;margin-left:10px;">
                    <xsl:apply-templates select="dap:Attribute"/>
                </div>
            </td>
        </tr>               
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
            <td align="right" style="vertical-align:text-top">
                <div class="medium_bold"><a href="opendap_form_help.html#dataset_variables" target="help">Variables</a> </div>
            </td>
           
            <td >
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



    <!-- ################################################################### -->
    <!-- ################################################################### -->
    <!-- ################################################################### -->
    <!-- ################################################################### -->
    <!-- 
      -  Specialized Machinery For Grid data type
      -
      -
      -
     -->
    <xsl:template match="dap:Gwrid">
        <xsl:param name="parentContainer"/>

        <xsl:variable name="myFQN">
            <xsl:call-template name="computeFQN"><xsl:with-param name="separator">.</xsl:with-param></xsl:call-template>
        </xsl:variable>

        <xsl:variable name="myJSVarName"><xsl:call-template name="computeVarName"/></xsl:variable>

        <xsl:variable name="isContainer">true</xsl:variable>

        <xsl:variable name="checkBoxName" select="concat('get_',$myJSVarName)"/>

        <xsl:variable name="gridArray" select="dap:Array"/>

        <xsl:variable name="myType" select="$gridArray" />


        <xsl:element name="script">
            <xsl:attribute name="type">text/javascript</xsl:attribute>
            <xsl:value-of select="$myJSVarName"/> = new dap_var("<xsl:value-of select="$myFQN"/>", "<xsl:value-of select="$myJSVarName"/>",false,false);

            <xsl:if test="parent::dap:Dataset">
                DAP2_URL.add_dap_var(<xsl:value-of select="$myJSVarName"/>);
            </xsl:if>

            <xsl:value-of select="$myJSVarName"/>.checkBox = "<xsl:value-of select="$checkBoxName"/>";
            <xsl:if test="$parentContainer">
                <xsl:comment>- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -</xsl:comment>
                <xsl:comment>Added by dap:Grid because test="$parentContainer" is true.</xsl:comment>
                <xsl:value-of select="$parentContainer"/>.addChildVar(<xsl:value-of select="$myJSVarName"/>);
                <xsl:value-of select="$myJSVarName"/>.parentContainer = <xsl:value-of select="$parentContainer"/>;
                <xsl:comment>- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -</xsl:comment>
            </xsl:if>
        </xsl:element>



        <div style="color: black;margin-left:-15px;margin-top:10px">
            <input type="checkbox" id="{$checkBoxName}" onclick="{$myJSVarName}.handle_projection_change({$checkBoxName})"  onfocus="describe_projection()" />
            <xsl:value-of select="@name"/>

            <!-- span class="small">
                <xsl:if test="$container">
                    (child of <xsl:value-of select="$container"/>)
                </xsl:if>
            </span -->
            <xsl:apply-templates select="$gridArray">
                <xsl:with-param name="parentContainer"><xsl:call-template name="computeVarName"/></xsl:with-param>
            </xsl:apply-templates>
            <!-- xsl:call-template name="DimHeader" / -->
            <span class="small" style="vertical-align: 15%;"> (Type is <xsl:value-of select ="$myType"/>)</span>
        </div>

        <!-- xsl:call-template name="DimSlicing">
            <xsl:with-param name="myJSVarName" select="$myJSVarName"/>
        </xsl:call-template -->


    </xsl:template>








    <xsl:template match="dap:Grid">
        <xsl:param name="parentContainer"/>

        <!-- script type="text/javascript">DEBUG.myCheckBox.checked=true;</script -->

        <xsl:variable name="gridArray" select="dap:Array"/>
        <xsl:variable name="myFQN">
            <xsl:call-template name="computeFQN"><xsl:with-param name="separator">.</xsl:with-param></xsl:call-template>
        </xsl:variable>
        <xsl:variable name="myJSVarName"><xsl:call-template name="computeVarName"/></xsl:variable>
        <xsl:variable name="checkBoxName" select="concat('get_',$myJSVarName)"/>

        <xsl:variable name="type">Grid</xsl:variable>
        <xsl:variable name="gridDataType" select="name($gridArray/*[not(self::dap:Attribute) and not(self::dap:dimension)])"/>

        <xsl:element name="script">
        <xsl:attribute name="type">text/javascript</xsl:attribute>
        if(DEBUG.enabled()) alert("dap:Grid()\n "+
        "myFQN:         <xsl:value-of select="$myFQN"/>\n " +
        "myJSVarName:   <xsl:value-of select="$myJSVarName"/>\n " +
        "checkBoxName:  <xsl:value-of select="$checkBoxName"/>\n " +
            "type:        <xsl:value-of select="$type"/>\n " +
            "gridDataType:        <xsl:value-of select="$gridDataType"/>\n " +
        "parentContainer:        <xsl:value-of select="$parentContainer"/>\n"
        );
        </xsl:element>

        <xsl:element name="script">
            <xsl:attribute name="type">text/javascript</xsl:attribute>
            <xsl:value-of select="$myJSVarName"/> = new dap_var("<xsl:value-of select="$myFQN"/>", "<xsl:value-of select="$myJSVarName"/>",true,false);


            <xsl:if test="parent::dap:Dataset">
                DAP2_URL.add_dap_var(<xsl:value-of select="$myJSVarName"/>);
            </xsl:if>

            <xsl:value-of select="$myJSVarName"/>.checkBox = "<xsl:value-of select="$checkBoxName"/>";
            <xsl:if test="$parentContainer">
                <xsl:comment>- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -</xsl:comment>
                <xsl:comment>Added by dap:Grid because test="$parentContainer" is true.</xsl:comment>
                <xsl:value-of select="$parentContainer"/>.addChildVar(<xsl:value-of select="$myJSVarName"/>);
                <xsl:value-of select="$myJSVarName"/>.parentContainer = <xsl:value-of select="$parentContainer"/>;
                <xsl:comment>- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -</xsl:comment>
            </xsl:if>
        </xsl:element>



        <div style="color: black;margin-left:-15px;margin-top:10px">
            <input type="checkbox" id="{$checkBoxName}" onclick="{$myJSVarName}.handle_projection_change({$checkBoxName})"  onfocus="describe_projection()" />
            <xsl:value-of select="@name"/>

            <!-- span class="small">
                <xsl:if test="$container">
                    (child of <xsl:value-of select="$container"/>)
                </xsl:if>
            </span -->

            <xsl:for-each select="$gridArray"><xsl:call-template name="DimHeader"/></xsl:for-each>

            <span class="small" style="vertical-align: 15%;"> (Grid of <xsl:value-of select ="$gridDataType"/> values)</span>
        </div>

        <xsl:for-each select="$gridArray">
            <xsl:call-template name="DimSlicing">
                <xsl:with-param name="myJSVarName" select="$myJSVarName"/>
            </xsl:call-template>
            <xsl:call-template name="AttributesPresentation"/>
        </xsl:for-each>

    </xsl:template>



    <xsl:template match="dap:Map | dap:Array">
        <xsl:param name="parentContainer"/>
        <xsl:choose>
            <xsl:when test="$parentContainer">
                <li>
                    <xsl:call-template name="VariableWorker">
                        <xsl:with-param name="parentContainer" select="$parentContainer"/>
                    </xsl:call-template>
                </li>                
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="VariableWorker"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Suppress the "blob" element -->
    <xsl:template match="dap:blob" />

    <!-- ################################################################### -->
    <!-- ################################################################### -->
    <!-- ################################################################### -->
    
    


</xsl:stylesheet>
