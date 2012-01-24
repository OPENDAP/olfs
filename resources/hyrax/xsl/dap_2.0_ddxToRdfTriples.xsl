<?xml version="1.0" encoding="ISO-8859-1"?>
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
<!DOCTYPE xsl:stylesheet [
        <!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
        <!ENTITY DBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;&amp;nbsp;</xsl:text>" >
        <!ENTITY BASE "http://source.url/for/ddx/document.ddx" >
        <!ENTITY ATT  "http://source.url/for/ddx/document.ddx/att#" >
        <!ENTITY DAP  "http://iridl.ldeo.columbia.edu/ontologies/opendap.owl#" >
        <!ENTITY DAP2 "http://xml.opendap.org/ns/DAP2" >
        ]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:cf="http://iridl.ldeo.columbia.edu/ontologies/cf-att.owl#"
                xmlns:dap="&DAP;"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:dap2="&DAP2;"
                xsi:schemaLocation="&DAP2;  http://xml.opendap.org/dap/dap2.xsd"
                xmlns:att="&ATT;"

        >
    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="AttributeNames" match="dap2:Attribute" use="@name" />



    <!-- ###################################################################
      -
      -   Converts a Dataset DDX into an RDF Document.
      -
    -->
    <xsl:template match="/dap2:Dataset">
        <rdf:RDF  xml:base="&BASE;" >
            <owl:Ontology
                    rdf:about="&BASE;">
                <owl:imports
                        rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/opendap.owl"/>
                <owl:imports
                        rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/cf-att.owl"/>
            </owl:Ontology>

            <dap:Dataset rdf:about="">
                <xsl:apply-templates select="*" mode="body"/>
            </dap:Dataset>


            <xsl:call-template name="AttPropDef"/>


        </rdf:RDF>
    </xsl:template>
    <!-- ################################################################### -->



    <!-- ###################################################################
     -
     -
     -    Body of content. ( mode="body" )
     -
     -
    -->
    <xsl:template match="dap2:Attribute" mode="body" >
        <xsl:element name="att:{@name}">
            <xsl:if test="@type='Container'" >
                <xsl:attribute name="rdf:parseType" >Resource</xsl:attribute>
                <xsl:apply-templates select="./*" mode="body"/>
            </xsl:if>

            <xsl:if test="not(@type='Container')" >
                <xsl:attribute name="rdf:datatype" >&DAP2;<xsl:value-of select="@type" /></xsl:attribute>
                <xsl:value-of select="./dap2:value" />
            </xsl:if>
        </xsl:element>
    </xsl:template>




    <xsl:template match="dap2:dataBLOB" mode="body">
        <dap:hasdataBLOB>
            <dap:dataBLOB rdf:about="{@href}"/>
        </dap:hasdataBLOB>
    </xsl:template>


    <xsl:template match="dap2:Grid" mode="body">
        <dap:isContainerOf>
            <dap:Grid>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>

                <xsl:apply-templates select="dap2:Array" mode="body"/>
                <xsl:apply-templates select="dap2:Map" mode="body"/>

                <xsl:call-template name="constraintTag"/>

            </dap:Grid>
        </dap:isContainerOf>
    </xsl:template>


    <xsl:template match="dap2:Structure" mode="body">
        <dap:isContainerOf>
            <dap:Structure>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>

                <xsl:apply-templates  mode="body"/>

                <xsl:call-template name="constraintTag"/>
            </dap:Structure>
        </dap:isContainerOf>
    </xsl:template>


    <xsl:template match="dap2:Sequence" mode="body">
        <dap:isContainerOf>
            <dap:Sequence>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>

                <xsl:apply-templates  mode="body"/>

                <xsl:call-template name="constraintTag"/>
            </dap:Sequence>
        </dap:isContainerOf>
    </xsl:template>


    <xsl:template match="dap2:Array" mode="body">
        <dap:isContainerOf>
            <dap:Array>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>

                <xsl:apply-templates mode="array"/>

                <xsl:call-template name="constraintTag"/>

            </dap:Array>
        </dap:isContainerOf>
    </xsl:template>


    <xsl:template match="dap2:Map" mode="body">
        <dap:hasMap>
            <dap:Array>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
                <xsl:apply-templates mode="array"/>
                <xsl:call-template name="constraintTag"/>
            </dap:Array>
        </dap:hasMap>
    </xsl:template>

    <xsl:template match="dap2:Byte" mode="body">
        <dap:isContainerOf>
            <dap:A_Byte>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_Byte>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:Int16" mode="body">
        <dap:isContainerOf>
            <dap:A_Int16>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_Int16>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:UInt16" mode="body">
        <dap:isContainerOf>
            <dap:A_UInt16>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_UInt16>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:Int32" mode="body">
        <dap:isContainerOf>
            <dap:A_Int32>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_Int32>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:UInt32" mode="body">
        <dap:isContainerOf>
            <dap:A_UInt32>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_UInt32>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:Float32" mode="body">
        <dap:isContainerOf>
            <dap:A_Float32>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_Float32>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:Float64" mode="body">
        <dap:isContainerOf>
            <dap:A_Float64>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_Float64>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:String" mode="body">
        <dap:isContainerOf>
            <dap:A_String>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_String>
        </dap:isContainerOf>
    </xsl:template>

    <xsl:template match="dap2:Url" mode="body">
        <dap:isContainerOf>
            <dap:A_Url>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:call-template name="constraintTag"/>
                <xsl:apply-templates select="dap2:Attribute" mode="body"/>
            </dap:A_Url>
        </dap:isContainerOf>
    </xsl:template>

    <!-- ################################################################### -->




    <!-- ###################################################################
     -
     -    dap:constraintTag
     -
     -
    -->
    <xsl:template name="constraintTag">
        <dap:constraintTag rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
            <xsl:call-template name="constraintTagWorker" />
        </dap:constraintTag>
    </xsl:template>

    <xsl:template match="*" name="constraintTagWorker" mode="constraintTag">
        <xsl:if test="generate-id(.)!=generate-id(/dap2:Dataset)">
            <xsl:apply-templates select=".." mode="constraintTag"/>
            <xsl:if test="generate-id(..)!=generate-id(/dap2:Dataset) and
                          not(parent::dap2:Array)">.</xsl:if>
            <xsl:value-of select="@name"/>
        </xsl:if>
    </xsl:template>
    <!-- ################################################################### -->




    <!-- ###################################################################
     -
     -
     -    Array Mode. ( mode="array" )
     -
     -
    -->
    <xsl:template match="*" mode="array" />

    <xsl:template match="dap2:dimension" mode="array">
        <dap:hasDimension rdf:parseType="Resource" >
            <dap:size><xsl:value-of select="@size" /></dap:size>
            <xsl:if test="@name">
                <dap:name><xsl:value-of select="@name" /></dap:name>
            </xsl:if>
        </dap:hasDimension>
    </xsl:template>

    <xsl:template match="dap2:Array" mode="array">
        <dap:hasDataType>
            <dap:Array>
                <xsl:apply-templates mode="array"/>
            </dap:Array>
        </dap:hasDataType>
    </xsl:template>


    <xsl:template match="dap2:Grid" mode="array">
        <dap:hasDataType>
            <dap:Grid>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap2:Array" mode="body"/>
                <xsl:apply-templates select="dap2:Map" mode="body"/>

                <xsl:call-template name="constraintTag"/>
            </dap:Grid>
        </dap:hasDataType>
    </xsl:template>

    <xsl:template match="dap2:Sequence" mode="array">
        <dap:hasDataType>
            <dap:Sequence>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:apply-templates  mode="body"/>
                <xsl:call-template name="constraintTag"/>
            </dap:Sequence>
        </dap:hasDataType>
    </xsl:template>

    <xsl:template match="dap2:Structure" mode="array">
        <dap:hasDataType>
            <dap:Structure>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="constraintTagWorker"/>
                </xsl:attribute>
                <xsl:apply-templates  mode="body"/>
                <xsl:call-template name="constraintTag"/>
            </dap:Structure>
        </dap:hasDataType>
    </xsl:template>

    <xsl:template match="dap2:String" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;String"/>
    </xsl:template>

    <xsl:template match="dap2:Url" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;Url"/>
    </xsl:template>

    <xsl:template match="dap2:Byte" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;Byte"/>
    </xsl:template>

    <xsl:template match="dap2:Int16" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;Int16"/>
    </xsl:template>

    <xsl:template match="dap2:UInt16" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;UInt16"/>
    </xsl:template>

    <xsl:template match="dap2:Int32" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;Int32"/>
    </xsl:template>

    <xsl:template match="dap2:UInt32" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;UInt32"/>
    </xsl:template>

    <xsl:template match="dap2:Float32" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;Float32"/>
    </xsl:template>

    <xsl:template match="dap2:Float64" mode="array">
        <dap:hasDataType rdf:resource="&DAP2;Float64"/>
    </xsl:template>

    <!-- ################################################################### -->






    <!-- ###################################################################
      -
      -   Summary of Content
      -
    -->
    <xsl:template mode="summary"
                  match="dap2:Attribute"  />

    <xsl:template mode="OFF"
                  match="dap2:Attribute"  >


        <xsl:element name="{@name}">

            <xsl:if test="@type='Container'">
                <xsl:apply-templates mode="summary" select="dap2:Attribute" />
            </xsl:if>

            <xsl:if test="not(./Attribute)">
                <xsl:value-of select="dap2:value"/>
            </xsl:if>
        </xsl:element>
    </xsl:template>


    <xsl:template mode="summary"
                  match="child::dap2:value"  />


    <xsl:template mode="summary"
                  match="*[not(self::dap2:Attribute)  and
                         not(parent::dap2:Attribute)  and
                         not(self::dap2:dataBLOB)]"
                  >
        <!-- Applying mode Summary template to <xsl:copy /> -->
        <dap:isContainerOf rdf:resource="#{@name}"/>

    </xsl:template>

    <!-- ################################################################### -->



    <!-- ###################################################################
     -
     -
     -    Convert each Attribute to an RDF property. ( mode="AttPropDef" )
     -
     -
    -->
    <xsl:template name="AttPropDef">
        <xsl:for-each
                select="//dap2:Attribute[generate-id() = generate-id(key('AttributeNames', @name))]">
            <owl:DatatypeProperty rdf:about="&ATT;{@name}">
                <rdfs:domain
                        rdf:resource="&DAP;Container"/>
                <rdfs:isDefinedBy rdf:resource=""/>
            </owl:DatatypeProperty>
        </xsl:for-each>
    </xsl:template>


</xsl:stylesheet>

