<?xml version="1.0" encoding="UTF-8"?>
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfx="http://www.w3.org/1999/02/22-rdf-syntax-ns"
                xmlns:nons="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#"
                version="1.0">

    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:template match="*" mode="xml2rdf">
        <xsl:variable name="myElementNamespace">
            <xsl:call-template name="getConvertedNamespace"/>
        </xsl:variable>
        <xsl:variable name="myTextContent">
            <xsl:call-template name="getTextFromJustThisNode"/>
        </xsl:variable>

        <xsl:choose>
            <!-- does the element have rdf:resource (should be only) -->
            <xsl:when test="@rdf:resource|@rdfx:resource">
                <xsl:element name="{local-name()}" namespace="{$myElementNamespace}">
                    <xsl:if test="@rdf:resource">
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="@rdf:resource"/>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:if test="@rdfx:resource">
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="@rdfx:resource"/>
                        </xsl:attribute>
                    </xsl:if>
                </xsl:element>
            </xsl:when>
            <!-- Does the element have either child elements, or xml attributes? -->
            <xsl:when test="child::* or @*">

                <!-- That means it's an rdf:Resource -->
                <xsl:element name="{local-name()}" namespace="{$myElementNamespace}">
                    <rdf:Description>
                        <xsl:if test="@rdf:ID">
                            <xsl:attribute name="rdf:ID">
                                <xsl:value-of select="@rdf:ID"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if test="@rdfx:ID">
                            <xsl:attribute name="rdf:ID">
                                <xsl:value-of select="@rdfx:ID"/>
                            </xsl:attribute>
                        </xsl:if>

                        <xsl:if test="@rdf:about">
                            <xsl:attribute name="rdf:about">
                                <xsl:value-of select="@rdf:about"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if test="@rdfx:about">
                            <xsl:attribute name="rdf:about">
                                <xsl:value-of select="@rdfx:about"/>
                            </xsl:attribute>
                        </xsl:if>

                        <xsl:if test="@xml:base">
                            <xsl:attribute name="xml:base">
                                <xsl:value-of select="@xml:base"/>
                            </xsl:attribute>
                        </xsl:if>

                        <!-- Convert the attributes to elements -->
                        <xsl:for-each
                                select="@*[
                                generate-id(.) != generate-id(../@rdf:about)  and
                                generate-id(.) != generate-id(../@rdfx:about) and
                                generate-id(.) != generate-id(../@rdf:ID)     and
                                generate-id(.) != generate-id(../@rdfx:ID)    and
                                generate-id(.) != generate-id(../@xml:base)
                                ]">
                            <xsl:variable name="attNamespace">
                                <xsl:call-template name="getConvertedNamespace"/>
                            </xsl:variable>
                            <xsl:element name="{local-name()}" namespace="{$attNamespace}">
                                <xsl:value-of select="."/>
                            </xsl:element>
                        </xsl:for-each>

                        <!-- Process the children -->
                        <xsl:apply-templates mode="xml2rdf"/>

                        <!-- Add the text content of the node wrapped in an rdf:value element -->
                        <xsl:if test="$myTextContent!=''">
                            <xsl:element name="rdf:value">
                                <xsl:value-of select="$myTextContent"/>
                            </xsl:element>
                        </xsl:if>

                    </rdf:Description>

                </xsl:element>
            </xsl:when>

            <!-- Is the element empty?? -->
            <xsl:when test="not(child::*) and not(@*) and not(text())">
                <!-- Then tag it as empty -->
                <xsl:element name="{local-name()}" namespace="{$myElementNamespace}">
                    <xsl:attribute name="rdf:resource">http://www.w3.org/1999/02/22-rdf-syntax-ns#nil</xsl:attribute>
                </xsl:element>
            </xsl:when>

            <!-- Otherwise it must be a element with simple text content so we just need to copy it. 
But we still need to transform the namespace to end with a '#' if it doesn't already. -->
            <xsl:otherwise>
                <xsl:element name="{local-name()}" namespace="{$myElementNamespace}">
                    <xsl:value-of select="."/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <!-- Suppress all text and attribute output other than what we define for this mode -->
    <xsl:template match="@*|text()" mode="xml2rdf"/>


    <!-- 
      Convert namespaces to our RDF namespace scheme:
       - Assign items not in any namespace to our nonamespace namespace.
       - Add a trailing '#' to namespaces that don't already have one.
    -->
    <xsl:template name="getConvertedNamespace">
        <xsl:choose>
            <!-- If the node is not in a namespace, return our special "no namespace" namespace -->
            <xsl:when test="namespace-uri()=''">
                <xsl:value-of select="'http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#'"/>
            </xsl:when>

            <xsl:otherwise>
                <xsl:choose>
                    <!-- If the namespace ends in '#', use it as is -->
                    <xsl:when test="substring(namespace-uri(),string-length(namespace-uri()))='#'">
                        <xsl:value-of select="namespace-uri()"/>
                    </xsl:when>
                    <!-- Otherwise add a '#' to the end of the namespace and use that. -->
                    <xsl:otherwise>
                        <xsl:value-of select="concat(namespace-uri(),'#')"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


    <!-- Get the text content for just the current node, no recursion. -->
    <xsl:template name="getTextFromJustThisNode">
        <xsl:for-each select="text()">
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:for-each>
    </xsl:template>


</xsl:stylesheet>
