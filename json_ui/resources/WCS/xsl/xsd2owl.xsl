<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet href="./xsltdoc.xsl" type="text/xsl" media="screen"?>
<!-- 
This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike License. 
To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/1.0/ 
or send a letter to Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305, USA.
License: http://rhizomik.net/redefer/xsd2owl.xsl.rdf

This work has been modified from the above to work in a GRDDL sense:

1) preserves original namespaces (with the addition of #)
2) owl:imports dropped as broken
3) rdf:seeAlso added for imports and includes
4) uses xsd:schemaType instead of owl:Ontology to refer to document being processed
5) parent Class added to intersectionOf lists
6) minCardinality=0 classes added to xs:choice translation in addition to owl:unionOf
7) rdfs:isDefinedBy for all classes/properties in orginal schema (not for synthesised names for anonymous complexType)
8) correct xs:group behavior to be enclosed in owl:intersectionOf
9) domain statements added for Properties defined within a complexType, i.e. have no range statement of their own

10) restrictions that correspond to attributes are marked with xsd2owl:propertyIsA xsd:attribute.

11) restrictions that correspond to elements are marked with xsd2owl:propertyIsA xsd:element
12) restrictions are connected to their complexType with xsd2owl:constrains.

13) <union memberTypes="gml:CalDate time dateTime anyURI decimal"/> is converted to unionOf

14) anonymous complexType of group is treated as class of group.

15) added xsd2owl:propertyIsA xsd:element to case where MaxCardinality
is 1 is the only constraint

16) refined xsd:group handling to work with ComplexType directly containing group

17) added owl:cardinality instead of minCardinality/maxCardinality
with the same value.  This means unionOf for choices is now better in
that or is between cardinality statements which each imply min and
maxCardinality.  Still missing the disjunction statement, which will
not be easy because it requires subclass of the class in question plus
pairwise owl:disjointWith (see http://www.w3.org/TR/owl-ref/)
18) EmptyComplexTypes now point to xsd2owl:EmptyComplexType instead of making up a new class by appending Type.
19) choice is improved:  unionof is now only cardinality statements, with additional subClassOf for allvaluesfrom restriction.
20) added xsd2owl:constrains statement  for tacitTypes
21) support MinOccurs=0 of group references by defining a {groupname}MinOccurs0 class for every group, and using it if the
    reference has MinOccurs=0
22) domain statements weakened to domainContains, don't want use of a property to imply that the subject is in a new class
23) added support for form=qualified/unqualified
24) fixed support for AttributeGroup
25) added xsd2owl:hasEmptySequence to flag empty sequence declarations; also changed code so that empty sequences no longer result in an <rdfs:subClassOf /> statement
26) tacitTypes now has many more xsd2owl:constrains statements
27) attributeGroup improvement
28) 25) broke nested sequences: this is fixed.
-->
<xsl:stylesheet version="2.0" xmlns:xo="http://rhizomik.net/redefer/xsl/xsd2owl-functions.xsl" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsdr="http://www.w3.org/2001/XMLSchema#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:xsd2owl="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#" xmlns:crosswalk="http://iridl.ldeo.columbia.edu/ontologies/iricrosswalk.owl#">

	<xsl:import href="xsd2owl-functions.xsl"/>
	<xsl:output media-type="text/xml" version="1.0" encoding="UTF-8" indent="yes" use-character-maps="owl"/>
	
	<xsl:strip-space elements="*"/>
	
	<xsl:character-map name="owl">
		<xsl:output-character character="&amp;" string="&amp;"/>
	</xsl:character-map>
	
	<!-- Used to avoid repeated creation of the properties implicitly created as elements or attributes inside 
		complexTypes, groups and attribute groups declarations  and contains(xo:makeRDFAbsoluteRefFunc(@name),'http://www.w3.org/2001/XMLSchema#') -->
	<xsl:key name="distinctElements" match="//xsd:element[@name and (ancestor::xsd:complexType or ancestor::xsd:group) and 
																		not(xo:existsElemOrAtt(/xsd:schema, @name))]" use="@name"/>

	<xsl:key name="distinctAttributes" match="//xsd:attribute[@name and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup) and
																		not(xo:existsElemOrAtt(/xsd:schema, @name))]" use="@name"/>

	<!-- Get the default namespace and build a entity definition for it-->
	<xsl:variable name="targetNamespace">
		<xsl:value-of select="/xsd:schema/@targetNamespace"/>
	</xsl:variable>
	<xsl:variable name="targetNamespaceDelimited">
               <xsl:choose>
               <xsl:when test="ends-with(/xsd:schema/@targetNamespace,'#') or ends-with(/xsd:schema/@targetNamespace,'/')or ends-with(/xsd:schema/@targetNamespace,':')">
		<xsl:value-of select="/xsd:schema/@targetNamespace"/>
               </xsl:when>
               <xsl:otherwise>
		<xsl:value-of select="/xsd:schema/@targetNamespace"/>
                <xsl:text disable-output-escaping="yes">#</xsl:text>
               </xsl:otherwise>
               </xsl:choose>
	</xsl:variable>
	<xsl:variable name="elementFormDefault">
	<xsl:choose>
	<xsl:when test="/xsd:schema/@elementFormDefault">
		<xsl:value-of select="/xsd:schema/@elementFormDefault"/>
	</xsl:when>
	<xsl:otherwise>unqualified</xsl:otherwise>
	</xsl:choose>
	</xsl:variable>
	<xsl:variable name="attributeFormDefault">
	<xsl:choose>
	<xsl:when test="/xsd:schema/@attributeFormDefault">
		<xsl:value-of select="/xsd:schema/@attributeFormDefault"/>
	</xsl:when>
	<xsl:otherwise>unqualified</xsl:otherwise>
	</xsl:choose>
	</xsl:variable>
	<xsl:variable name="baseEntity">
		<xsl:text disable-output-escaping="yes">&amp;</xsl:text>
		<xsl:for-each select="/xsd:schema/namespace::*">
			<xsl:if test=". = $targetNamespace">
				<xsl:value-of select="name()"/>
			</xsl:if>
		</xsl:for-each>
		<xsl:text disable-output-escaping="yes">;</xsl:text>
	</xsl:variable>								 
	     
	<!-- Match the xsd:schema element to generate the entity definitions from the used namespaces.
	Then, the rdf:RDF element and the the ontology subelement are generated.
	Finally, the rest of the XML Schema is processed.
	-->
	<xsl:template match="/xsd:schema">
		<!-- Generate entity definitions for each namespace -->
		<xsl:text disable-output-escaping="yes">&#10;&lt;!DOCTYPE rdf:RDF [&#10;</xsl:text>
		<!-- Allways include xsd entity and thus ignore if also defined in input xsd -->
		<xsl:text disable-output-escaping="yes">&#09;&lt;!ENTITY xsd 'http://www.w3.org/2001/XMLSchema#'&gt;&#10;</xsl:text>
		<xsl:for-each select="namespace::*[not(name()='' or name()='xsd')]">
			<xsl:text disable-output-escaping="yes">&#09;&lt;!ENTITY </xsl:text>
			<xsl:value-of select="name()"/>
			<xsl:text disable-output-escaping="yes"> '</xsl:text>
					<xsl:value-of select="."/>
			               <xsl:if test="not(ends-with(.,'#') or ends-with(.,'/')or ends-with(.,':'))">
						<xsl:text disable-output-escaping="yes">#</xsl:text>
					</xsl:if>
			<xsl:text disable-output-escaping="yes">'&gt;&#10;</xsl:text>
		</xsl:for-each>
		<xsl:text disable-output-escaping="yes">]&gt;&#10;</xsl:text>
		<!-- Build the rdf:RDF element with the namespace declarations for the declared namespace entities -->
		<rdf:RDF>
			<!-- Detect the namespaces used in the XMLSchema that must be copied to the OWL ontology -->
			<xsl:variable name="used_namespaces">
				 <xsl:for-each select="namespace::*[not(name()=''or name()='xsd' or name()='xml')]">
					<xsl:element name="{name()}:x" namespace="&#38;{name()};"/>
				</xsl:for-each>
			</xsl:variable>
			<!-- Copy the required namespaces collected in the "used_namespaces" variable declaration, which acts as their temporal container -->
			<!--xsl:copy-of select="node-set($used_namespaces)/*/namespace::*"/-->
			<xsl:copy-of select="$used_namespaces/*/namespace::*"/>
			<xsdr:schemaType rdf:about="">
                            <xsl:if test="/xsd:schema/@targetNamespace">
			   <xsdr:targetNamespace><xsl:value-of select="/xsd:schema/@targetNamespace" /></xsdr:targetNamespace>
                            </xsl:if>
			   <xsd2owl:dependsOn rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl" />
				<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
				<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
				<!-- Create the xsd:imports elements corresponding to the xsd:import elements -->
				<xsl:for-each select="./xsd:import">
					<xsl:variable name="namespaceAtt">
						<xsl:value-of select="@namespace"/>
					</xsl:variable>
					<xsl:variable name="schemaLocationAtt">
						<xsl:value-of select="@schemaLocation"/>
					</xsl:variable>
					<!-- If there is an alias for the namespace, use it. Otherwise, use the namespace URI -->
					<xsl:variable name="importRef">
						<xsl:for-each select="namespace::*">
							<xsl:if test=". = $namespaceAtt">
								<xsl:value-of select="name()"/>
							</xsl:if>
						</xsl:for-each>
					</xsl:variable>
						<xsd2owl:imports rdf:resource="{$schemaLocationAtt}"/>
				</xsl:for-each>
				<xsl:for-each select="./xsd:include">
					<xsl:variable name="schemaLocationAtt">
						<xsl:value-of select="@schemaLocation"/>
					</xsl:variable>
					<!-- If there is an alias for the namespace, use it. Otherwise, use the namespace URI -->
						<xsd2owl:imports rdf:resource="{$schemaLocationAtt}"/>
				</xsl:for-each>
			</xsdr:schemaType>
			<xsl:apply-templates/>
			
			<!-- Generate the OWL class definitions for the complexTypes totally declared inside other ones,
				in order to avoid repetitions: and generate-id()=generate-id(key('newClasses',@name)) -->
			<!--xsl:for-each select="//xsd:element[not(@type or @ref) and (ancestor::xsd:complexType or ancestor::xsd:group)]/xsd:complexType">
				<xsl:call-template name="processComplexType"/>
			</xsl:for-each-->

		        <!-- defines {groupname}MinOccurs0 so it can be used for references to the group which have MinOccurs0 -->
                        <xsl:for-each select="//xsd:group[@name]">
	                     <owl:Class rdf:about="{$targetNamespaceDelimited}{@name}MinOccurs0">
				<xsd2owl:isConstrainedBy rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<owl:unionOf rdf:parseType="Collection">
				<rdf:Description rdf:about="{$targetNamespaceDelimited}{@name}" />
				<owl:Class>
					<owl:intersectionOf  rdf:parseType="Collection">
					<xsl:for-each select=".//xsd:element[@ref]">
                                  <owl:Restriction>
				  <owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
				  <owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="0"/>
				  </owl:cardinality>
		        	 </owl:Restriction>
	                                </xsl:for-each>
					</owl:intersectionOf>
				</owl:Class>
                                </owl:unionOf>
                             </owl:Class>
			</xsl:for-each>
	


			<!-- Don't Add the any ObjectProperty for xsd:any - it is defined in XMLSchema
			<owl:ObjectProperty rdf:ID="any"/ -->
			
			<!-- Explicitly create the new properties defined inside complexTypes, groups and attributGroups  using the key to select only distinct ones -->
			<xsl:for-each select="//xsd:element[@name and (ancestor::xsd:complexType or ancestor::xsd:group) and generate-id()=generate-id(key('distinctElements',@name)[1])] | 
												//xsd:attribute[@name and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup) and generate-id()=generate-id(key('distinctAttributes',@name)[1])]"> 
				<xsl:sort select="@name" order="ascending"/>
				<!-- If it can be determined to be an element or attribute associated with datatype only values (i.e. simpleTypes)
					then map it to a owl:DatatypeProperty. If it is associated with objectype only values (i.e. complexTypes) then
					map it to owl:ObjectProperty. Otherwise, use rdf:Property to cope with both kinds of values -->
				<xsl:variable name="currentName"><xsl:value-of select="@name"/></xsl:variable>
				<xsl:choose>
					<!--xsl:when test="xo:isDatatype(.,//xsd:simpleType[@name],namespace::*) and not(xo:isObjectype(.,//xsd:complexType[@name],namespace::*))"-->
					<xsl:when test="xo:allDatatype(//xsd:element[@name=$currentName and (ancestor::xsd:complexType or ancestor::xsd:group)] | 
										 //xsd:attribute[@name=$currentName and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup)], 
										 //xsd:complexType[@name], namespace::*)">
						<owl:DatatypeProperty rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
				<xsl:if test="ancestor::xsd:complexType/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				</owl:DatatypeProperty>
					</xsl:when>
					<!--xsl:when test="xo:isObjectype(.,//xsd:complexType[@name],namespace::*) and not(xo:isDatatype(.,//xsd:simpleType[@name],namespace::*))"-->
					<xsl:when test="xo:allObjectype(//xsd:element[@name=$currentName and (ancestor::xsd:complexType or ancestor::xsd:group)] | 
										 //xsd:attribute[@name=$currentName and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup)],
										 //xsd:simpleType[@name], namespace::*)">
						<owl:ObjectProperty rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
				<xsl:if test="ancestor::xsd:complexType">
				<xsl:if test="ancestor::xsd:complexType/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				<xsl:if test="not(ancestor::xsd:complexType/@name) and ancestor::xsd:element/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				</xsl:if>
				</xsl:if>
				</owl:ObjectProperty>
					</xsl:when>
					<xsl:otherwise>
						<rdf:Property rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
				<xsl:if test="ancestor::xsd:complexType">
				<xsl:if test="ancestor::xsd:complexType/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				<xsl:if test="not(ancestor::xsd:complexType/@name) and ancestor::xsd:element/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				</xsl:if>
				</xsl:if>
				</rdf:Property>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</rdf:RDF>
		
		<!--Generate separated document with the datatype declarations for explicit and implicit simpleTypes -->
		<!--xsl:result-document href="datatypes.xml">        		
        		<!- Generate the definitions for the simpleTypes totally declared inside other ones ->
			<xsl:for-each select="//xsd:element[not(@type or @ref) and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup or ancestor::xsd:group) and generate-id()=generate-id(key('newClasses',@name))]/xsd:simpleType |
								  //xsd:attribute[not(@type or @ref) and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup or ancestor::xsd:group) and generate-id()=generate-id(key('newClasses',@name))]/xsd:simpleType">
				<xsl:call-template name="processSimpleType"/>
			</xsl:for-each>
        		
        		<!- Match simpleType definitions ->
			<xsl:for-each select="//xsd:simpleType">
				<xsl:if test="@name">
					<owl:Class rdf:about="{$targetNamespaceDelimited}{@name}"/>
				</xsl:if>
				<xsl:if test="not(@name)">
					<xsl:choose>
						<!- If it is an anonymous simpleType embeded in a element definition generate
						a rdf:ID derived from the element definition name ->
						<xsl:when test="parent::xsd:element[@name]">
							<owl:Class rdf:about="{$targetNamespaceDelimited}{parent::xsd:element/@name}Range"/>
						</xsl:when>
						<xsl:when test="parent::xsd:attribute[@name]">
							<owl:Class rdf:about="{$targetNamespaceDelimited}{parent::xsd:attribute/@name}Range"/>
						</xsl:when>
						<xsl:otherwise>
							<owl:Class/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
			</xsl:for-each>	
    		</xsl:result-document-->
	</xsl:template>
	
	<!-- Match XML Schema element definitions to generate OWL ObjectProperties and DatatypeProperties:
      1. Map substitutionGroup attribute to subPropertyOf relation 
      2. Map type attribute or embeded complexType to range relation -->
	<xsl:template match="xsd:schema/xsd:element[@name]|xsd:schema/xsd:attribute[@name and not(xo:existsElem(/xsd:schema/xsd:element,@name))]">
		<!-- Use the same criteria than for elements and attributed defined inside complexTypes, groups and attributGroups -->
		<xsl:choose>
			<xsl:when test="xo:isDatatype(.,//xsd:simpleType[@name],namespace::*) and 
							 not(xo:isObjectype(.,//xsd:complexType[@name],namespace::*))">
				<owl:DatatypeProperty rdf:about="{$targetNamespaceDelimited}{@name}">
				    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
					<xsl:call-template name="processElemDef"/>
				<xsl:if test="ancestor::xsd:complexType/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				</owl:DatatypeProperty>
			</xsl:when>
			<xsl:when test="xo:isObjectype(.,//xsd:complexType[@name],namespace::*) and 
							not(xo:isDatatype(.,//xsd:simpleType[@name],namespace::*))">
				<owl:ObjectProperty rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
				<xsl:if test="ancestor::xsd:complexType/@name">
				<rdfs:domainContains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
					<xsl:call-template name="processElemDef"/>
				</owl:ObjectProperty>
			</xsl:when>
			<xsl:otherwise>
				<rdf:Property rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
					<xsl:call-template name="processElemDef"/>
				</rdf:Property>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="processElemDef">
		<!--	The class hierarchy is defined by the substitutionGroup attribute -->
		<xsl:if test="@substitutionGroup">
			<rdfs:subPropertyOf rdf:resource="{xo:rdfUri(@substitutionGroup, namespace::*)}"/>
		</xsl:if>
		<!-- The type attribute or the embeded complexType define equivalent classes -->
		<xsl:choose>
			<xsl:when test="@type">
				<rdfs:range rdf:resource="{xo:rangeUri(., //xsd:simpleType[@name], namespace::*)}"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="./xsd:complexType and ./xsd:complexType/child::*">
					<!-- Generate anonymous class definition from complexType -->
					<rdfs:range>
						<xsl:apply-templates/>
					</rdfs:range>
				</xsl:if>
				<xsl:if test="./xsd:complexType and not (./xsd:complexType/child::*)">
					<!-- Generate anonymous class definition from complexType -->
					<rdfs:range rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#EmptyComplexType" />
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- Match XML Schema complexType or group definitions to generate classes, if the
		embededType has a value this is a complex type defined inside and element, distinguish
		its name from the name of the element using the embededType param value -->
	<xsl:template name="processComplexType" match="xsd:complexType|xsd:group|xsd:attributeGroup">
		<xsl:if test="@name">
			<owl:Class rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
		<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
				<xsl:apply-templates/>
			</owl:Class>
		</xsl:if>
		<xsl:if test="not(@name)">
			<xsl:choose>
				<xsl:when test="child::xsd:group[@ref and (@minOccurs=0) ]">
					<owl:Class rdf:about="{xo:rdfUri(child::xsd:group/@ref, namespace::*)}MinOccurs0" >
	                                 <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
                                        </owl:Class>
				</xsl:when>
				<xsl:when test="child::xsd:group[@ref]">
					<owl:Class rdf:about="{xo:rdfUri(child::xsd:group/@ref, namespace::*)}" >
	                                 <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
                                        </owl:Class>
				</xsl:when>
				<xsl:when test="parent::xsd:element[@name] and child::*">
					<owl:Class rdf:about="{$targetNamespaceDelimited}{../@name}TacitType">
				<xsl:if test="xsd:annotation/xsd:documentation/@source">
			             <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
						<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
						<xsl:apply-templates/>
					</owl:Class>
				</xsl:when>
				<xsl:otherwise>
					<owl:Class>	<!--rdf:ID="_:{generate-id()}"-->
						<xsl:apply-templates/>
					</owl:Class>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<!-- Match extension or restriction base to generate subClassOf relation for complexType definitions -->
	<!-- Avoid the creation of subclasses of XSD datatypes -->
	<xsl:template match="xsd:extension[@base]| xsd:restriction[@base and parent::xsd:complexContent]">
		<xsl:if test="not(xo:isXsdUri(@base, namespace::*))">
			<rdfs:subClassOf rdf:resource="{xo:rdfUri(@base, namespace::*)}"/>
		</xsl:if>
		<xsl:if test="parent::xsd:simpleContent">
			<rdfs:subClassOf>
				<owl:Restriction>
                                        <xsl:choose>
					<xsl:when test="ancestor::xsd:complexType/@name">
                                	<xsd2owl:isSimpleContentOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
					</xsl:when>
					<xsl:when test="ancestor::xsd:element/@name">
                                	<xsd2owl:isSimpleContentOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
					</xsl:when>
                                        </xsl:choose>
				<owl:onProperty rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#value"/>
				<owl:allValuesFrom rdf:resource="{xo:rdfUri(@base, namespace::*)}"/>
				</owl:Restriction>
			</rdfs:subClassOf>
		</xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- For xsd:sequence or xsd:choice that is not inside other sequences or choices
	generate the initial rdfs:subClassOf relation that links it to the class it defines -->
	<xsl:template match="xsd:sequence[not(parent::xsd:sequence) and not(parent::xsd:choice)]">
		<xsl:choose>
			<xsl:when test="count(child::*)>0">
			<rdfs:subClassOf>
			<xsl:call-template name="processSequence"/>
			</rdfs:subClassOf>
			</xsl:when>
			<xsl:otherwise>
				<xsd2owl:hasEmptySequence rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="xsd:choice[not(parent::xsd:sequence) and not(parent::xsd:choice)]">
		<rdfs:subClassOf>
			<xsl:call-template name="processChoice"/>
		</rdfs:subClassOf>
	</xsl:template>
	
	<!-- Match xsd:sequence to generate a owl:intersectionOf if number of childs > 0 -->
	<xsl:template name="processSequence" match="xsd:sequence">
				<owl:Class>
					<owl:intersectionOf rdf:parseType="Collection">
						<xsl:if test="parent::xsd:extension">
							<rdf:Description rdf:about="{xo:rdfUri(parent::xsd:extension/@base, namespace::*)}">
	<xsl:if test="ancestor::xsd:complexType/@name">
            <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
	</xsl:if>
</rdf:Description>
						</xsl:if>
						<xsl:apply-templates/>
					</owl:intersectionOf>
				</owl:Class>
	</xsl:template>
	<xsl:template name="processAll" match="xsd:all">
		<xsl:choose>
			<xsl:when test="count(child::*)>0">
			<rdfs:subClassOf>
				<owl:Class>
					<owl:intersectionOf rdf:parseType="Collection">
						<xsl:if test="parent::xsd:extension">
							<rdf:Description rdf:about="{xo:rdfUri(parent::xsd:extension/@base, namespace::*)}">
	<xsl:if test="ancestor::xsd:complexType/@name">
            <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
	</xsl:if>
</rdf:Description>
						</xsl:if>
						<xsl:apply-templates/>
					</owl:intersectionOf>
				</owl:Class>
			</rdfs:subClassOf>
			</xsl:when>
			<xsl:otherwise>
				<xsd2owl:hasEmptySequence rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Match xsd:choice to generate a owl:unionOf (disjoint?) if number of childs > 1-->
	<xsl:template name="processChoice" match="xsd:choice">
		<xsl:choose>
			<xsl:when test="count(child::*)>0">
				<owl:Class>
                                <rdfs:subClassOf><owl:Class>
					<owl:unionOf rdf:parseType="Collection">
						<xsl:for-each select="child::xsd:element[@ref]">
 				<owl:Restriction>
				  <owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
				  <owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="1"/>
				  </owl:cardinality>
		        	 </owl:Restriction>
				</xsl:for-each>
						<xsl:for-each select="child::xsd:element[@name]">
 				<owl:Restriction>
				  <owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				  <owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="1"/>
				  </owl:cardinality>
		        	 </owl:Restriction>
				</xsl:for-each>
                                 </owl:unionOf>
                                 </owl:Class></rdfs:subClassOf>
					<xsd2owl:hasDisjointSubClasses rdf:parseType="Collection">
						<xsl:for-each select="child::xsd:element[@ref]">
 				<owl:Restriction>
				  <owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
				  <owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="1"/>
				  </owl:cardinality>
		        	 </owl:Restriction>
				</xsl:for-each>
						<xsl:for-each select="child::xsd:element[@name]">
 				<owl:Restriction>
				  <owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				  <owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="1"/>
				  </owl:cardinality>
		        	 </owl:Restriction>
				</xsl:for-each>
                                 </xsd2owl:hasDisjointSubClasses>
		        	<xsl:for-each select="child::xsd:element[@ref]">
				<rdfs:subClassOf>
                        	 <owl:Restriction>
				  <owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
                                    <xsl:choose>
                                    <xsl:when test="ancestor::xsd:group/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:group/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
                                    </xsl:when>
                                    <xsl:when test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
                                    </xsl:when>
                                    </xsl:choose>
				  <owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="0"/>
				  </owl:minCardinality>
		        	 </owl:Restriction>
			        </rdfs:subClassOf>
		             </xsl:for-each>
		        	<xsl:for-each select="child::xsd:element[@name]">
				<rdfs:subClassOf>
                        	 <owl:Restriction>
				  <owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				  <owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="0"/>
				  </owl:minCardinality>
		        	 </owl:Restriction>
			        </rdfs:subClassOf>
		             </xsl:for-each>
	                      <xsl:for-each select="child::xsd:element[@ref and @type]">
				<rdfs:subClassOf>
                        	 <owl:Restriction>
				  <owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
                                 <owl:allValuesFrom rdf:resource="{xo:rangeUri(., //xsd:simpleType[@ref], namespace::*)}"/>
		        	 </owl:Restriction>
                                    <xsl:choose>
                                    <xsl:when test="ancestor::xsd:group/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:group/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
                                    </xsl:when>
                                    <xsl:when test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
                                    </xsl:when>
                                    </xsl:choose>
			        </rdfs:subClassOf>
		             </xsl:for-each>
	                      <xsl:for-each select="child::xsd:element[@name and @type]">
				<rdfs:subClassOf>
                        	 <owl:Restriction>
				  <owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
			          <owl:allValuesFrom rdf:resource="{xo:rangeUri(., //xsd:simpleType[@name], namespace::*)}"/>
                                    <xsl:choose>
                                    <xsl:when test="ancestor::xsd:group/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:group/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
                                    </xsl:when>
                                    <xsl:when test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
                                    </xsl:when>
                                    </xsl:choose>
		        	 </owl:Restriction>
			        </rdfs:subClassOf>
		             </xsl:for-each>
				</owl:Class>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
<xsl:template name="processSimpleType" match="xsd:simpleType">
<xsl:if test="@name">
					<owl:Class rdf:about="{$targetNamespaceDelimited}{@name}">
			    <rdfs:isDefinedBy rdf:resource=""/>
				<xsl:if test="xsd:annotation/xsd:documentation/@source">
			   <rdfs:seeAlso rdf:resource="{xsd:annotation/xsd:documentation/@source}" />
				</xsl:if>
				<xsl:if test="xsd:annotation/xsd:documentation">
				<rdfs:comment><xsl:value-of select="xsd:annotation/xsd:documentation"/></rdfs:comment>
				</xsl:if>
	<xsl:if test="child::xsd:union[@memberTypes]">
	<xsl:apply-templates/>
</xsl:if>
</owl:Class>
				</xsl:if>
</xsl:template>
<xsl:template name="processUnion" match="xsd:union">
<xsl:variable name="root" select="ancestor::xsd:simpleType" />
<owl:unionOf rdf:parseType="Collection">
<xsl:variable name="tokenizedMemberTypes" select="tokenize(@memberTypes,' ')"/>
<xsl:for-each select="$tokenizedMemberTypes">
<xsl:variable name="cQName" select="resolve-QName(.,$root)" />
<xsl:variable name="cnsQName" select="namespace-uri-from-QName($cQName)" />
<rdf:Description rdf:about="{$cnsQName}#{local-name-from-QName($cQName)}" />
    </xsl:for-each>
</owl:unionOf>
</xsl:template>
	<!-- Match xsd:annotations  to generate rdfs:comments -->
	<xsl:template match="xsd:annotation/xsd:documentation">
		<!--rdfs:comment><xsl:value-of select="."/></rdfs:comment>
		<xsl:if test="@source">
			<rdfs:seeAlso><xsl:value-of select="@source"/></rdfs:seeAlso>
		</xsl:if-->
	</xsl:template>
	<xsl:template match="xsd:annotation/xsd:appinfo">
		<!--rdfs:comment rdf:parseType="Literal"><xsl:value-of select="."/></rdfs:comment-->
	</xsl:template>
	
	<!-- Match elements inside complexType to generate owl:Restrictions over the owl:Class defined by the complexType -->
	<!-- Match elements declared inside the complexType with a reference to an external type -->
	<xsl:template match="xsd:element[@name and @type and (ancestor::xsd:complexType or ancestor::xsd:group)]">
		<owl:Restriction>
                                <xsl:choose>
			          <xsl:when test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				  </xsl:when>
			          <xsl:when test="ancestor::xsd:element/@name">
                                   <xsd2owl:isQualifiedElementOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				  </xsl:when>
                                </xsl:choose>
			<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
			<owl:allValuesFrom rdf:resource="{xo:rangeUri(., //xsd:simpleType[@name], namespace::*)}"/>
		</owl:Restriction>
		<xsl:call-template name="localCardinality">
			<xsl:with-param name="min" select="(@minOccurs | parent::*/@minOccurs)[1]"/>
			<xsl:with-param name="max" select="(@maxOccurs | parent::*/@maxOccurs)[1]"/>
			<xsl:with-param name="property" select="@name"/>
			<xsl:with-param name="forceRestriction" select="false()"/>
		</xsl:call-template>
	</xsl:template>
	
	<!-- Match elements declared outside the complexType and referenced from it -->
	<xsl:template match="xsd:element[@ref and (ancestor::xsd:complexType or ancestor::xsd:group)]">
		<xsl:call-template name="cardinality">
			<xsl:with-param name="min" select="(@minOccurs | parent::*/@minOccurs)[1]"/>
			<xsl:with-param name="max" select="(@maxOccurs | parent::*/@maxOccurs)[1]"/>
			<xsl:with-param name="property" select="xo:rdfUri(@ref, namespace::*)"/>
			<xsl:with-param name="forceRestriction" select="true()"/>
		</xsl:call-template>
	</xsl:template>
	
	<!-- Match elements totally declared inside the complexType, if simpleType generate URI,
		otherwise embed class declaration for complexType -->
	<xsl:template match="xsd:element[not(@type or @ref) and (ancestor::xsd:complexType or ancestor::xsd:group)]">
		<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
			<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
			<xsl:choose>
				<xsl:when test="count(./xsd:simpleType)>0">
					<owl:allValuesFrom rdf:resource="{xo:newRangeUri(., $baseEntity)}"/>
				</xsl:when>
				<xsl:otherwise>
					<owl:allValuesFrom>
						<xsl:apply-templates select="./*"/>
					</owl:allValuesFrom>
				</xsl:otherwise>
			</xsl:choose>
		</owl:Restriction>
		<xsl:call-template name="localCardinality">
			<xsl:with-param name="min" select="(@minOccurs | parent::*/@minOccurs)[1]"/>
			<xsl:with-param name="max" select="(@maxOccurs | parent::*/@maxOccurs)[1]"/>
			<xsl:with-param name="property" select="@name"/>
			<xsl:with-param name="forceRestriction" select="false()"/>
		</xsl:call-template>
	</xsl:template>
	
	<!-- Generate cardinality constraints. Default maxCardinality and minCardinality equal to 1, if no maxOccurs or minOccurs specified -->
	<xsl:template name="cardinality">
		<xsl:param name="min"/>
		<xsl:param name="max"/>
		<xsl:param name="property"/>
		<xsl:param name="forceRestriction"/>
		<xsl:variable name="minOccurs">
			<xsl:choose>
				<xsl:when test="$min">
					<xsl:value-of select="$min"/>
				</xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="maxOccurs">
			<xsl:choose>
				<xsl:when test="$max">
					<xsl:value-of select="$max"/>
				</xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="$minOccurs=$maxOccurs">
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
			          <xsl:if test="ancestor::xsd:element/@name">
                                   <xsd2owl:isQualifiedElementOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				  </xsl:if>
				<owl:onProperty rdf:resource="{$property}"/>
				<owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$minOccurs"/>
				</owl:cardinality>
			</owl:Restriction>
		</xsl:if>
		<xsl:if test="$minOccurs!='0' and $minOccurs!=$maxOccurs">
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
			          <xsl:if test="ancestor::xsd:element/@name">
                                   <xsd2owl:isQualifiedElementOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				  </xsl:if>
				<owl:onProperty rdf:resource="{$property}"/>
				<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$minOccurs"/>
				</owl:minCardinality>
			</owl:Restriction>
		</xsl:if>
		<xsl:if test="$maxOccurs!='unbounded'and $minOccurs!=$maxOccurs">
			<owl:Restriction>
				<owl:onProperty rdf:resource="{$property}"/>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
				<xsl:if test="ancestor::xsd:element/@name">
                                   <xsd2owl:isQualifiedElementOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				</xsl:if>
		<xsl:if test="$minOccurs='0' and $forceRestriction">
				<xsd2owl:propertyIsA rdf:resource="&amp;xsd;element" />
	        </xsl:if>
				<owl:maxCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$maxOccurs"/>
				</owl:maxCardinality>
			</owl:Restriction>
		</xsl:if>
		<!-- If restriction not needed because min=0 and max="unbounded", generate it if forceRestriction="true" 
			  because there is not any other restriction on the property -->
		<xsl:if test="$minOccurs='0' and $maxOccurs='unbounded' and $forceRestriction">
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
				<xsl:if test="ancestor::xsd:element/@name">
                                   <xsd2owl:isQualifiedElementOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				</xsl:if>
				<owl:onProperty rdf:resource="{$property}"/>
				<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$minOccurs"/>
				</owl:minCardinality>
			</owl:Restriction>
		</xsl:if>
	</xsl:template>
        <xsl:template name="isElementOf">
<xsl:param name="name" />
<xsl:param name="targetns" />
				<xsl:choose>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedElementOf rdf:resource="{$targetns}{$name}" />
				</xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedElementOf rdf:resource="{$targetns}{$name}" />
				</xsl:when>
				<xsl:when test="$elementFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedElementOf rdf:resource="{$targetns}{$name}" />
				</xsl:when>
				<xsl:when test="$elementFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedElementOf rdf:resource="{$targetns}{$name}" />
				</xsl:when>
				</xsl:choose>
	</xsl:template>
	<xsl:template name="localCardinality">
		<xsl:param name="min"/>
		<xsl:param name="max"/>
		<xsl:param name="property"/>
		<xsl:param name="forceRestriction"/>
		<xsl:variable name="minOccurs">
			<xsl:choose>
				<xsl:when test="$min">
					<xsl:value-of select="$min"/>
				</xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="maxOccurs">
			<xsl:choose>
				<xsl:when test="$max">
					<xsl:value-of select="$max"/>
				</xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
                <xsl:if test="$minOccurs=$maxOccurs">
			<owl:Restriction>
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{$property}"/>
				<xsl:if test="ancestor::xsd:complexType/@name">
                                <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				<owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$minOccurs"/>
				</owl:cardinality>
			</owl:Restriction>
		</xsl:if>
		<xsl:if test="($minOccurs!='0') and ($minOccurs!=$maxOccurs)">
			<owl:Restriction>
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{$property}"/>
				<xsl:if test="ancestor::xsd:complexType/@name">
                                <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$minOccurs"/>
				</owl:minCardinality>
			</owl:Restriction>
		</xsl:if>
		<xsl:if test="($maxOccurs!='unbounded') and ($minOccurs!=$maxOccurs)">
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
                                <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{$property}"/>
				<owl:maxCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$maxOccurs"/>
				</owl:maxCardinality>
			</owl:Restriction>
		</xsl:if>
		<!-- If restriction not needed because min=0 and max="unbounded", generate it if forceRestriction="true" 
			  because there is not any other restriction on the property -->
		<xsl:if test="$minOccurs='0' and $maxOccurs='unbounded' and $forceRestriction">
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
                                <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:if>
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{$property}"/>
				<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
					<xsl:value-of select="$minOccurs"/>
				</owl:minCardinality>
			</owl:Restriction>
		</xsl:if>
	</xsl:template>
	
	<!-- Match attribute definitions inside complexType to generate owl:Restricitons -->
	<xsl:template match="xsd:attribute[@name and @type and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup)]">
		<rdfs:subClassOf>
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
				<xsl:choose>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
                                </xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
				<xsl:if test="ancestor::xsd:element/@name">
				<xsl:choose>
				<xsl:when test="@ref">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
                                </xsl:when>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
                                </xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
				<xsl:if test="ancestor::xsd:attributeGroup/@name">
				<xsl:choose>
				<xsl:when test="@ref">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
                                </xsl:when>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
                                </xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
				<owl:allValuesFrom rdf:resource="{xo:rangeUri(., //xsd:simpleType[@name], namespace::*)}"/>
			</owl:Restriction>
		</rdfs:subClassOf>
	        <xsl:choose>
		<xsl:when test="@use='required'">
			<rdfs:subClassOf>
				<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
					<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
					<owl:cardinality rdf:datatype="&amp;xsd;nonNegativeInteger">1</owl:cardinality>
				</owl:Restriction>
			</rdfs:subClassOf>
	        </xsl:when>
	        <xsl:otherwise>
			<rdfs:subClassOf>
				<owl:Restriction>
					<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
					<owl:maxCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">1</owl:maxCardinality>
				</owl:Restriction>
			</rdfs:subClassOf>
		</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!--  Match attributes declared outside the complexType and referenced from it-->
	<xsl:template match="xsd:attribute[@ref and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup)]">
		<xsl:variable name="minOccurs">
			<xsl:choose>
				<xsl:when test="@use='required'">1</xsl:when>
				<xsl:otherwise>0</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<rdfs:subClassOf>
			<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
				<xsl:choose>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
				<xsl:if test="ancestor::xsd:attributeGroup/@name">
				<xsl:choose>
				<xsl:when test="@ref">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				</xsl:when>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				</xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
				<owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
				<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">
				<xsl:value-of select="$minOccurs"/>
				</owl:minCardinality>
			</owl:Restriction>
		</rdfs:subClassOf>
		<rdfs:subClassOf>
			<owl:Restriction>
				<owl:onProperty rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
				<owl:maxCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">1</owl:maxCardinality>
			</owl:Restriction>
		</rdfs:subClassOf>
	</xsl:template>
	
	<!-- Match attributes totally declared inside the complexType -->
	<xsl:template match="xsd:attribute[not(@type or @ref) and (ancestor::xsd:complexType or ancestor::xsd:attributeGroup)]">
	   <xsl:if test="not(@use='required')">
   			<rdfs:subClassOf>
				<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
				<xsl:choose>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>

				</xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />

				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>

				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
				<xsl:if test="ancestor::xsd:attributeGroup/@name">
				<xsl:choose>
				<xsl:when test="@form = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>

				</xsl:when>
				<xsl:when test="@form = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'qualified'">
                                <xsd2owl:isQualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>

				</xsl:when>
				<xsl:when test="$attributeFormDefault = 'unqualified'">
                                <xsd2owl:isUnqualifiedAttributeOf rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:attributeGroup/@name}" />
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="{$targetNamespaceDelimited}{@name}" />
				<crosswalk:makesThesePropertiesEquivalent rdf:resource="http://iridl.ldeo.columbia.edu/ontologies/xsd2owl/nonamespace#{@name}" />
				</xsl:when>
				</xsl:choose>
				</xsl:if>
					<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">0</owl:minCardinality>
				</owl:Restriction>
			</rdfs:subClassOf>
			<rdfs:subClassOf>
			<owl:Restriction>
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				<owl:maxCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">1</owl:maxCardinality>
			</owl:Restriction>
			</rdfs:subClassOf>
		</xsl:if>
		<!--
		<rdfs:subClassOf>
			<owl:Restriction>
				<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
				<owl:allValuesFrom rdf:resource="{xo:newRangeUri(., $baseEntity)}"/>
			</owl:Restriction>
		</rdfs:subClassOf>
		-->
		<xsl:if test="@use='required'">
			<rdfs:subClassOf>
				<owl:Restriction>
				<xsl:if test="ancestor::xsd:complexType/@name">
			<xsl:call-template name="isElementOf">
			<xsl:with-param name="name" select="ancestor::xsd:complexType/@name" />
			<xsl:with-param name="targetns" select="$targetNamespaceDelimited" />
			</xsl:call-template>
				</xsl:if>
					<owl:onProperty rdf:resource="{$targetNamespaceDelimited}{@name}"/>
					<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">1</owl:minCardinality>
				</owl:Restriction>
			</rdfs:subClassOf>
		</xsl:if>
	</xsl:template>
	
	<!-- Match group references as the corresponding class for the group -->
	<xsl:template match="xsd:group[@ref and ancestor::xsd:complexType and not(parent::xsd:extension) and not(parent::xsd:restriction) and not(parent::xsd:complexType)]">
<xsl:choose>
<xsl:when test="@ref and (@minOccurs=0)">
		<owl:Class rdf:about="{xo:rdfUri(@ref, namespace::*)}MinOccurs0" >
	            <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
	        </owl:Class>
	</xsl:when>
	<xsl:otherwise>
                <owl:Class rdf:about="{xo:rdfUri(@ref, namespace::*)}" >
	            <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
	        </owl:Class>
	</xsl:otherwise>
</xsl:choose>
	</xsl:template>
	<xsl:template match="xsd:group[@ref and parent::xsd:complexType]">
		<rdfs:subClassOf rdf:resource="{xo:rdfUri(@ref, namespace::*)}"/>
	</xsl:template>
	
	<!-- Match group as attributeGroup references as subClassOf references to the corresponding group class -->
	<xsl:template match="xsd:group[@ref and parent::xsd:extension]">
                 <rdfs:subClassOf><owl:Class><owl:intersectionOf rdf:parseType="Collection">
                <rdf:Description rdf:about="{xo:rdfUri(parent::xsd:extension/@base, namespace::*)}" />
		<rdf:Description rdf:about="{xo:rdfUri(@ref, namespace::*)}"/>
			</owl:intersectionOf></owl:Class></rdfs:subClassOf>
	</xsl:template>
	<xsl:template match="xsd:group[@ref and parent::xsd:restriction]">
                 <rdfs:subClassOf><owl:Class><owl:intersectionOf rdf:parseType="Collection">
                <rdf:Description rdf:about="{xo:rdfUri(parent::xsd:restriction/@base, namespace::*)}" />
		<rdf:Description rdf:about="{xo:rdfUri(@ref, namespace::*)}"/>
			</owl:intersectionOf></owl:Class></rdfs:subClassOf>
	</xsl:template>

	<!-- Match attributeGroup references as subClassOf references to the corresponding attributeGroup class -->
	<xsl:template match="xsd:attributeGroup[@ref and ancestor::xsd:complexType]">
		<rdfs:subClassOf>
                         <rdf:Description rdf:about="{xo:rdfUri(@ref, namespace::*)}" >
<xsl:choose>
			          <xsl:when test="ancestor::xsd:complexType/@name">
	            <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:complexType/@name}" />
</xsl:when>
			          <xsl:when test="ancestor::xsd:element/@name">
                   <xsd2owl:constrains rdf:resource="{$targetNamespaceDelimited}{ancestor::xsd:element/@name}TacitType" />
 </xsl:when>
                                </xsl:choose>
	        </rdf:Description>
                </rdfs:subClassOf>
	</xsl:template>
	
	<!-- Match any definitions inside complexType to generate owl:Restricitons -->
	<xsl:template match="xsd:any">
		<owl:Restriction>
			<owl:onProperty rdf:resource="&amp;xsd;any"/>
			<owl:minCardinality rdf:datatype="&amp;xsd;nonNegativeInteger">0</owl:minCardinality>
		</owl:Restriction>
	</xsl:template>

</xsl:stylesheet>
