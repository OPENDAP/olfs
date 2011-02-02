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
        <!ENTITY DAPOBJ  "http://xml.opendap.org/ontologies/opendap-dap-3.2.owl#" >
        <!ENTITY DAP     "http://xml.opendap.org/ns/DAP/3.3#" >
        <!ENTITY XSD     "http://www.w3.org/2001/XMLSchema#" >
        ]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:dapObj="&DAPOBJ;"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:dap="&DAP;"
                xmlns:xml="http://www.w3.org/XML/1998/namespace"
        >

    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="AttributeNames" match="dap:Attribute" use="@name"/>

    <xsl:variable name="XML_BASE"><xsl:value-of select="/dap:Dataset/@xml:base"/></xsl:variable>
    <xsl:variable name="LocalOntology"><xsl:value-of select="$XML_BASE"/>.rdf</xsl:variable>
    <xsl:variable name="LocalAttributeNS"><xsl:value-of select="$XML_BASE"/>/att#</xsl:variable>


    <xsl:strip-space elements="*"/>

    <!-- ###################################################################
      -
      -   Converts a Dataset DDX into an RDF Document.
      -
    -->
    <xsl:template match="/dap:Dataset">

        <rdf:RDF>
            <xsl:attribute name="base" namespace="http://www.w3.org/XML/1998/namespace">
                <xsl:value-of select="$XML_BASE"/>
            </xsl:attribute>

            <owl:Ontology
                    rdf:about="{$LocalOntology}">
                <owl:imports
                        rdf:resource="http://xml.opendap.org/ontologies/opendap-dap-3.2.owl"/>
                <owl:imports
                        rdf:resource="http://xml.opendap.org/ontologies/NetcdfConventionRegistry.owl"/>
            </owl:Ontology>

            <!-- The empty rdf:about defaults to the value of @xml:base -->
            <dapObj:Dataset rdf:about="">

                <rdfs:isDefinedBy rdf:resource="{$LocalOntology}"/>

                <dapObj:dataset_id rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                    <xsl:value-of select="@dataset_id"/>
                </dapObj:dataset_id>

                <xsl:apply-templates select="*" mode="body"/>
            </dapObj:Dataset>

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
    <xsl:template match="dap:Attribute" mode="body">


        <xsl:choose>

            <xsl:when test="@type='Container'">
                <xsl:element name="att:{@name}" namespace="{$LocalAttributeNS}">
                    <xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
                    <xsl:apply-templates select="./*" mode="body"/>
                </xsl:element>
            </xsl:when>

            <xsl:when test="@type='OtherXML'">
                <xsl:apply-templates select="*" mode="OtherXML">
                    <xsl:with-param name="relationship" select="@relationship"/>
                </xsl:apply-templates>
            </xsl:when>

            <xsl:otherwise>

                <xsl:element name="att:{@name}" namespace="{$LocalAttributeNS}">

                    <xsl:if test="dap:value[last()=1]">
                        <xsl:call-template name="attributeType">
                            <xsl:with-param name="thisAttribute" select="."/>
                        </xsl:call-template>
                        <!-- <xsl:comment> Single Value </xsl:comment> -->

                        <xsl:value-of select="."/>

                    </xsl:if>

                    <xsl:if test="dap:value[last()>1]">
                        <xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
                        <!-- <xsl:comment> Multi Value </xsl:comment> -->

                        <xsl:call-template name="attributeValues">
                            <xsl:with-param name="values" select="dap:value"/>
                        </xsl:call-template>

                    </xsl:if>

                </xsl:element>

            </xsl:otherwise>

        </xsl:choose>


    </xsl:template>

    <!--
     - This helper template uses recursion to process the dap:value elements of a dap:Attribute
     - into an ordered list of RDF literals, which given the current state of RDF
     - isn't a very pretty thing.
    -->

    <xsl:template name="attributeValues">


        <xsl:param name="values"/>

        <!--
                <xsl:comment>############################################</xsl:comment>
                <xsl:comment>                                            </xsl:comment>
                <xsl:comment> values                                  </xsl:comment>
                <xsl:copy-of select="$values" />
                <xsl:comment>                                            </xsl:comment>
                <xsl:comment>############################################</xsl:comment>

        -->
        <rdf:first>
            <xsl:call-template name="attributeType">
                <xsl:with-param name="thisAttribute" select="$values[1]/.."/>
            </xsl:call-template>
            <xsl:value-of select="$values[1]"/>
        </rdf:first>
        <rdf:rest>

            <xsl:if test="boolean($values[position()>1])">
                <xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
                <xsl:call-template name="attributeValues">
                    <xsl:with-param name="values" select="$values[position()>1]"/>
                </xsl:call-template>
            </xsl:if>

            <xsl:if test="not(boolean($values[position()>1]))">
                <xsl:attribute name="rdf:resource">http://www.w3.org/1999/02/22-rdf-syntax-ns#nil</xsl:attribute>
            </xsl:if>


        </rdf:rest>

    </xsl:template>


    <xsl:template name="attributeType">

        <xsl:param name="thisAttribute"/>
        <xsl:attribute name="rdf:datatype">
            <xsl:if test="$thisAttribute/@type='Byte'">&XSD;byte</xsl:if>
            <xsl:if test="$thisAttribute/@type='Int16'">&XSD;short</xsl:if>
            <xsl:if test="$thisAttribute/@type='UInt16'">&XSD;unsignedShort</xsl:if>
            <xsl:if test="$thisAttribute/@type='Int32'">&XSD;long</xsl:if>
            <xsl:if test="$thisAttribute/@type='UInt32'">&XSD;unsignedLong</xsl:if>
            <xsl:if test="$thisAttribute/@type='Float32'">&XSD;float</xsl:if>
            <xsl:if test="$thisAttribute/@type='Float64'">&XSD;double</xsl:if>
            <xsl:if test="$thisAttribute/@type='String'">&XSD;string</xsl:if>
            <xsl:if test="$thisAttribute/@type='Url'">&XSD;anyURI</xsl:if>
        </xsl:attribute>

    </xsl:template>


    <!--
     -  mode="anyXMLToRDF"
     -
     - Maps the OtherXML dap:Attribute type to RDF while expressing the appropriate relationship
     - of the XML to the parent DAP object.
     -
     -
     -
    -->
    <xsl:template name="OtherXML" match="*" mode="OtherXML">
        <xsl:param name="relationship"/>

        <xsl:choose>

            <xsl:when test="boolean($relationship) and $relationship='is-a'">
                <xsl:apply-templates select="." mode="anyXMLToRDF"/>
            </xsl:when>
            <xsl:when test="boolean($relationship) and $relationship='has-a'">
                <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="."/>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


    <!--
     -  mode="anyXMLToRDF"
     -
     -  Map arbitrary XML (OtherXML) into RDF.
     -
     -
     -
     -
    -->


    <xsl:template name="textAndattributes">
        <xsl:copy-of select="text()|@*"/>
    </xsl:template>


    <xsl:template match="@*|text()" mode="anyXMLToRDF"/>
    <xsl:template match="*" mode="anyXMLToRDF">

        <owl:sameAs>
            <xsl:apply-templates select="." mode="anyXMLToRDFWorker"/>
        </owl:sameAs>

    </xsl:template>


    <xsl:template match="@*|text()" mode="anyXMLToRDFWorker"/>
    <xsl:template match="*" mode="anyXMLToRDFWorker">
        <xsl:choose>
            <xsl:when test="*">
                <xsl:copy>

                    <!-- XML Attributes with namespaces-->
                    <xsl:for-each select="@*[namespace-uri()!='']">
                        <xsl:attribute name="{local-name()}" namespace="{namespace-uri()}">
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                    </xsl:for-each>

                    <!-- XML Attributes without namespaces-->
                    <xsl:for-each select="@*[namespace-uri()='']">
                        <xsl:apply-templates select="." mode="xmlAttributeNode"/>
                    </xsl:for-each>

                    <!-- XML Text -->
                    <xsl:apply-templates select="text()" mode="xmlTextNode"/>


                    <!-- XML Child Elements -->
                    <xsl:for-each select="*">
                        <xsl:choose>
                            <!-- Child Elements with children elements -->
                            <xsl:when test="*">
                                <dapObj:xmlContains>
                                    <xsl:apply-templates select="." mode="anyXMLToRDFWorker"/>
                                </dapObj:xmlContains>
                            </xsl:when>

                            <!-- Simple Children -->
                            <xsl:otherwise>
                                <xsl:apply-templates select="." mode="anyXMLToRDFWorker"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>

                </xsl:copy>

            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="."/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="@*" mode="xmlTextNode"/>

    <xsl:template match="text()" mode="xmlTextNode">
        <dapObj:xmlText>
            <xsl:value-of select="."/>
        </dapObj:xmlText>
    </xsl:template>


    <xsl:template match="text()" mode="xmlAttributeNode"/>

    <xsl:template match="@*" mode="xmlAttributeNode">
        <dapObj:xmlAttribute dapObj:name="{local-name()}" rdf:value="{.}"/>
    </xsl:template>


    <!--
        <xsl:template match="dap:dataBLOB" mode="body">
            <dapObj:hasdataBLOB>
                <dap:dataBLOB rdf:about="{@href}"/>
            </dapObj:hasdataBLOB>
        </xsl:template>
    -->

    <xsl:template match="dap:Grid" mode="body">
        <dapObj:isContainerOf>
            <dap:Grid>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>

                <xsl:apply-templates select="dap:Array" mode="body"/>
                <xsl:apply-templates select="dap:Map" mode="body"/>

                <xsl:call-template name="localId"/>

            </dap:Grid>
        </dapObj:isContainerOf>
    </xsl:template>


    <xsl:template match="dap:Structure" mode="body">
        <dapObj:isContainerOf>
            <dap:Structure>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>

                <xsl:apply-templates mode="body"/>

                <xsl:call-template name="localId"/>
            </dap:Structure>
        </dapObj:isContainerOf>
    </xsl:template>


    <xsl:template match="dap:Sequence" mode="body">
        <dapObj:isContainerOf>
            <dap:Sequence>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>

                <xsl:apply-templates mode="body"/>

                <xsl:call-template name="localId"/>
            </dap:Sequence>
        </dapObj:isContainerOf>
    </xsl:template>


    <xsl:template match="dap:Array" mode="body">
        <dapObj:isContainerOf>
            <xsl:apply-templates mode="array"/>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:Map" mode="body">
        <dapObj:hasMap>
            <xsl:apply-templates mode="array"/>
        </dapObj:hasMap>
    </xsl:template>


    <xsl:template match="dap:Byte" mode="body">
        <dapObj:isContainerOf>
            <dap:Byte>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:Byte>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:Int16" mode="body">
        <dapObj:isContainerOf>
            <dap:Int16>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:Int16>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:UInt16" mode="body">
        <dapObj:isContainerOf>
            <dap:UInt16>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:UInt16>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:Int32" mode="body">
        <dapObj:isContainerOf>
            <dap:Int32>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:Int32>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:UInt32" mode="body">
        <dapObj:isContainerOf>
            <dap:UInt32>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:UInt32>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:Float32" mode="body">
        <dapObj:isContainerOf>
            <dap:Float32>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:Float32>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:Float64" mode="body">
        <dapObj:isContainerOf>
            <dap:Float64>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:Float64>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:String" mode="body">
        <dapObj:isContainerOf>
            <dap:String>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:String>
        </dapObj:isContainerOf>
    </xsl:template>

    <xsl:template match="dap:Url" mode="body">
        <dapObj:isContainerOf>
            <dap:Url>
                <xsl:attribute name="rdf:ID">
                    <xsl:call-template name="localIdWorker"/>
                </xsl:attribute>
                <xsl:apply-templates select="dap:Attribute" mode="body"/>
                <xsl:call-template name="localId"/>
            </dap:Url>
        </dapObj:isContainerOf>
    </xsl:template>

    <!-- ################################################################### -->


    <!-- ###################################################################
     -
     -    dapObj:localId
     -
     -
    -->
    <xsl:template name="localId">
        <dapObj:localId rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:call-template name="localIdWorker"/></dapObj:localId>
    </xsl:template>

    <xsl:template match="*" name="localIdWorker" mode="localId">
        <xsl:if test="generate-id(.)!=generate-id(/dap:Dataset)">
            <xsl:apply-templates select=".." mode="localId"/>
            <xsl:if test="generate-id(..)!=generate-id(/dap:Dataset) and
                          not(parent::dap:Array) and
                          not(parent::dap:Map)">.</xsl:if><xsl:value-of select="@name"/></xsl:if>
    </xsl:template>
    <!-- ################################################################### -->


    <!-- ###################################################################
     -
     -
     -    Array Mode. ( mode="array" )
     -
     -
    -->
    <xsl:template match="*" mode="array"/>

    <xsl:template name="arrayDimension">
        <dapObj:hasDimensions rdf:parseType="Collection">
            <xsl:for-each select="../dap:dimension">
                <dap:dimension>
                    <dapObj:size>
                        <xsl:value-of select="@size"/>
                    </dapObj:size>
                    <xsl:if test="@name">
                        <dapObj:name>
                            <xsl:value-of select="@name"/>
                        </dapObj:name>
                    </xsl:if>
                </dap:dimension>
            </xsl:for-each>
        </dapObj:hasDimensions>
    </xsl:template>


    <!-- - - - - - - - - - - - - - - - - - -
    -
    - Template:    basicArrayTypeContents
    -
    - All Arrays have this set of stuff
    -
    -
    -->
    <xsl:template name="basicArrayTypeContents">
        <xsl:attribute name="rdf:ID">
            <xsl:call-template name="localIdWorker"/>
        </xsl:attribute>

        <!-- Since at this point the current node is the Array template,
       we need to look to the parent node (the Array) to get our Attribute
       elements. -->
        <xsl:apply-templates select="../dap:Attribute" mode="body"/>

        <!-- The template object should not have Attributes. We
    check for those anyway.... -->
        <xsl:apply-templates select="dap:Attribute" mode="body"/>

        <xsl:call-template name="arrayDimension"/>
        <xsl:call-template name="localId"/>
    </xsl:template>


    <xsl:template match="dap:Array" mode="array">
        <ERROR>Arrays of Arrays ar not permitted in the DAP. Since this XSL
            should be processing a legitimate DDX, this error should never occur.
            (rofl)
        </ERROR>
    </xsl:template>


    <xsl:template match="dap:Grid" mode="array">
        <dap:Grid>
            <xsl:call-template name="basicArrayTypeContents"/>
            <xsl:apply-templates select="dap:Array" mode="body"/>
            <xsl:apply-templates select="dap:Map" mode="body"/>
        </dap:Grid>
    </xsl:template>

    <xsl:template match="dap:Sequence" mode="array">
        <dap:Sequence>
            <xsl:call-template name="basicArrayTypeContents"/>
            <xsl:apply-templates mode="body"/>
        </dap:Sequence>
    </xsl:template>

    <xsl:template match="dap:Structure" mode="array">
        <dap:Structure>
            <xsl:call-template name="basicArrayTypeContents"/>
            <xsl:apply-templates mode="body"/>
        </dap:Structure>
    </xsl:template>

    <xsl:template match="dap:String" mode="array">
        <dap:String>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:String>
    </xsl:template>

    <xsl:template match="dap:Url" mode="array">
        <dap:Url>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:Url>
    </xsl:template>

    <xsl:template match="dap:Byte" mode="array">
        <dap:Byte>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:Byte>
    </xsl:template>

    <xsl:template match="dap:Int16" mode="array">
        <dap:Int16>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:Int16>
    </xsl:template>

    <xsl:template match="dap:UInt16" mode="array">
        <dap:UInt16>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:UInt16>
    </xsl:template>

    <xsl:template match="dap:Int32" mode="array">
        <dap:Int32>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:Int32>
    </xsl:template>

    <xsl:template match="dap:UInt32" mode="array">
        <dap:UInt32>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:UInt32>
    </xsl:template>

    <xsl:template match="dap:Float32" mode="array">
        <dap:Float32>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:Float32>
    </xsl:template>

    <xsl:template match="dap:Float64" mode="array">
        <dap:Float64>
            <xsl:call-template name="basicArrayTypeContents"/>
        </dap:Float64>
    </xsl:template>

    <!-- ################################################################### -->


    <!-- ###################################################################
      -
      -   Summary of Content
      -
    -->
    <xsl:template mode="summary"
                  match="dap:Attribute"/>

    <xsl:template mode="OFF"
                  match="dap:Attribute">


        <xsl:element name="{@name}">

            <xsl:if test="@type='Container'">
                <xsl:apply-templates mode="summary" select="dap:Attribute"/>
            </xsl:if>

            <xsl:if test="not(./Attribute)">
                <xsl:value-of select="dap:value"/>
            </xsl:if>
        </xsl:element>
    </xsl:template>


    <xsl:template mode="summary"
                  match="child::dap:value"/>


    <xsl:template mode="summary"
                  match="*[not(self::dap:Attribute)  and
                         not(parent::dap:Attribute)  and
                         not(self::dap:dataBLOB)]"
            >
        <!-- Applying mode Summary template to <xsl:copy /> -->
        <dapObj:isContainerOf rdf:resource="#{@name}"/>

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
                select="//dap:Attribute[generate-id() = generate-id(key('AttributeNames', @name))]">
            <owl:DatatypeProperty rdf:about="{$LocalAttributeNS}{@name}">
                <rdfs:domain
                        rdf:resource="&DAPOBJ;Container"/>
                <rdfs:isDefinedBy rdf:resource="{$LocalOntology}"/>
            </owl:DatatypeProperty>
        </xsl:for-each>
    </xsl:template>


</xsl:stylesheet>
