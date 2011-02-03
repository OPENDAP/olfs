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
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:gmi="http://www.isotc211.org/2005/gmi" xmlns:gmx="http://www.isotc211.org/2005/gmx"
  xmlns:gsr="http://www.isotc211.org/2005/gsr" xmlns:gss="http://www.isotc211.org/2005/gss" xmlns:gts="http://www.isotc211.org/2005/gts" xmlns:gml="http://www.opengis.net/gml"
  xmlns:gmd2="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:odap="http://xml.opendap.org/ns/DAP2">
  <xd:doc xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Created on:</xd:b> October 16, 2010</xd:p>
      <xd:p><xd:b>Author:</xd:b>ted.habermann@noaa.gov</xd:p>
      <xd:p/>
    </xd:desc>
  </xd:doc>
  <xsl:variable name="stylesheetVersion" select="'0.0.0'"/>
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="/">
    <xsl:variable name="globalAttributeCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/Attribute)"/>
    <xsl:variable name="variableCnt" select="count(/odap:Dataset/odap:Grid)"/>
    <xsl:variable name="variableAttributeCnt" select="count(/odap:Dataset/odap:variable/odap:Attribute)"/>
    <xsl:variable name="standardNameCnt" select="count(/odap:Dataset/odap:variable/odap:Attribute[@name='standard_name'])"/>
    <!-- Identifier Fields: 4 possible -->
    <xsl:variable name="idCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='id'])"/>
    <xsl:variable name="identifierNameSpaceCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='naming_authority'])"/>
    <xsl:variable name="metadataConventionCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='Metadata_Conventions'])"/>
    <xsl:variable name="metadataLinkCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='Metadata_Link'])"/>
    <xsl:variable name="identifierTotal" select="$idCnt + $identifierNameSpaceCnt + $metadataConventionCnt + $metadataLinkCnt"/>
    <xsl:variable name="identifierMax">4</xsl:variable>
    <!-- Text Search Fields: 7 possible -->
    <xsl:variable name="titleCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='title'])"/>
    <xsl:variable name="summaryCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='summary'])"/>
    <xsl:variable name="keywordsCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='keywords'])"/>
    <xsl:variable name="keywordsVocabCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='keywords_vocabulary'])"/>
    <xsl:variable name="stdNameVocabCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='standard_name_vocabulary'])"/>
    <xsl:variable name="commentCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='comment'])"/>
    <xsl:variable name="historyCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='history'])"/>
    <xsl:variable name="textSearchTotal" select="$titleCnt + $summaryCnt + $keywordsCnt + $keywordsVocabCnt      + $stdNameVocabCnt + $commentCnt + $historyCnt"/>
    <xsl:variable name="textSearchMax">7</xsl:variable>
    <!-- Extent Search Fields: 17 possible -->
    <xsl:variable name="geospatial_lat_minCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_min'])"/>
    <xsl:variable name="geospatial_lat_maxCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_max'])"/>
    <xsl:variable name="geospatial_lon_minCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_min'])"/>
    <xsl:variable name="geospatial_lon_maxCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_max'])"/>
    <xsl:variable name="timeStartCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_start'])"/>
    <xsl:variable name="timeEndCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_end'])"/>
    <xsl:variable name="vertical_minCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_min'])"/>
    <xsl:variable name="vertical_maxCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_max'])"/>
    <xsl:variable name="extentTotal" select="$geospatial_lat_minCnt + $geospatial_lat_maxCnt + $geospatial_lon_minCnt + $geospatial_lon_maxCnt     + $timeStartCnt + $timeEndCnt + $vertical_minCnt + $vertical_maxCnt"/>
    <xsl:variable name="extentMax">8</xsl:variable>
    <!--  -->
    <xsl:variable name="geospatial_lat_unitsCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_units'])"/>
    <xsl:variable name="geospatial_lat_resolutionCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_resolution'])"/>
    <xsl:variable name="geospatial_lon_unitsCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_units'])"/>
    <xsl:variable name="geospatial_lon_resolutionCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_resolution'])"/>
    <xsl:variable name="timeResCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_resolution'])"/>
    <xsl:variable name="timeDurCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_duration'])"/>
    <xsl:variable name="vertical_unitsCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_units'])"/>
    <xsl:variable name="vertical_resolutionCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_resolution'])"/>
    <xsl:variable name="vertical_positiveCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_positive'])"/>
    <xsl:variable name="otherExtentTotal"
      select="$geospatial_lat_resolutionCnt + $geospatial_lat_unitsCnt     + $geospatial_lon_resolutionCnt + $geospatial_lon_unitsCnt     + $timeResCnt + $timeDurCnt     + $vertical_unitsCnt + $vertical_resolutionCnt + $vertical_positiveCnt"/>
    <xsl:variable name="otherExtentMax">9</xsl:variable>
    <!-- Responsible Party Fields: 14 possible -->
    <xsl:variable name="creatorNameCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='creator_name'])"/>
    <xsl:variable name="creatorURLCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='creator_url'])"/>
    <xsl:variable name="creatorEmailCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='creator_email'])"/>
    <xsl:variable name="creatorDateCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='date_created'])"/>
    <xsl:variable name="modifiedDateCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='date_modified'])"/>
    <xsl:variable name="issuedDateCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='date_issued'])"/>
    <xsl:variable name="creatorInstCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='institution'])"/>
    <xsl:variable name="creatorProjCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='project'])"/>
    <xsl:variable name="creatorAckCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='acknowledgment'])"/>
    <xsl:variable name="dateCnt" select="$creatorDateCnt + $modifiedDateCnt + $issuedDateCnt"/>
    <xsl:variable name="creatorTotal" select="$creatorNameCnt + $creatorURLCnt + $creatorEmailCnt + $creatorDateCnt + $modifiedDateCnt + $issuedDateCnt + $creatorInstCnt + $creatorProjCnt + $creatorAckCnt"/>
    <xsl:variable name="creatorMax">9</xsl:variable>
    <!--  -->
    <xsl:variable name="contributorNameCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='contributor_name'])"/>
    <xsl:variable name="contributorRoleCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='contributor_role'])"/>
    <xsl:variable name="contributorTotal" select="$contributorNameCnt + $contributorRoleCnt"/>
    <xsl:variable name="contributorMax">2</xsl:variable>
    <!--  -->
    <xsl:variable name="publisherNameCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='publisher_name'])"/>
    <xsl:variable name="publisherURLCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='publisher_url'])"/>
    <xsl:variable name="publisherEmailCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='publisher_email'])"/>
    <xsl:variable name="publisherTotal" select="$publisherNameCnt + $publisherURLCnt + $publisherEmailCnt"/>
    <xsl:variable name="publisherMax">3</xsl:variable>
    <!--  -->
    <xsl:variable name="responsiblePartyCnt" select="$creatorNameCnt + $contributorNameCnt + $publisherNameCnt"/>
    <xsl:variable name="responsiblePartyTotal" select="$creatorTotal + $contributorTotal + $publisherTotal"/>
    <xsl:variable name="responsiblePartyMax">14</xsl:variable>
    <!-- Other Fields: 2 possible -->
    <xsl:variable name="cdmTypeCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='cdm_data_type'])"/>
    <xsl:variable name="procLevelCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='processing_level'])"/>
    <xsl:variable name="licenseCnt" select="count(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='license'])"/>
    <xsl:variable name="otherTotal" select="$cdmTypeCnt + $procLevelCnt + $licenseCnt"/>
    <xsl:variable name="otherMax">3</xsl:variable>
    <xsl:variable name="spiralTotal" select="$identifierTotal + $textSearchTotal + $extentTotal + $otherExtentTotal + $otherTotal + $responsiblePartyTotal"/>
    <xsl:variable name="spiralMax" select="$identifierMax + $otherMax + $textSearchMax + $creatorMax + $extentMax + $responsiblePartyMax"/>
    <!--                        -->
    <!--    Write ISO Metadata  -->
    <!--                        -->
    <gmi:MI_Metadata>
      <xsl:attribute name="xsi:schemaLocation">
        <xsl:value-of select="'http://www.isotc211.org/2005/gmi http://www.ngdc.noaa.gov/metadata/published/xsd/schema.xsd'"/>
      </xsl:attribute>
      <gmd:fileIdentifier>
        <xsl:choose>
          <xsl:when test="$idCnt">
            <gco:CharacterString>
              <xsl:choose>
                <xsl:when test="$identifierNameSpaceCnt">
                  <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='naming_authority']/odap:value,'&quot;','')"/>:</xsl:when>
              </xsl:choose>
              <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='id']/odap:value,'&quot;','')"/>
            </gco:CharacterString>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="gco:nilReason">
              <xsl:value-of select="'missing'"/>
            </xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
      </gmd:fileIdentifier>
      <gmd:language>
        <xsl:call-template name="writeCodelist">
          <xsl:with-param name="codeListName" select="'gmd:LanguageCode'"/>
          <xsl:with-param name="codeListValue" select="'eng'"/>
        </xsl:call-template>
      </gmd:language>
      <gmd:characterSet>
        <xsl:call-template name="writeCodelist">
          <xsl:with-param name="codeListName" select="'gmd:MD_CharacterSetCode'"/>
          <xsl:with-param name="codeListValue" select="'UTF8'"/>
        </xsl:call-template>
      </gmd:characterSet>
      <gmd:hierarchyLevel>
        <xsl:call-template name="writeCodelist">
          <xsl:with-param name="codeListName" select="'gmd:MD_ScopeCode'"/>
          <xsl:with-param name="codeListValue" select="'dataset'"/>
        </xsl:call-template>
      </gmd:hierarchyLevel>
      <gmd:contact>
        <xsl:attribute name="gco:nilReason">
          <xsl:value-of select="'unknown'"/>
        </xsl:attribute>
      </gmd:contact>
      <gmd:dateStamp>
        <xsl:attribute name="gco:nilReason">
          <xsl:value-of select="'unknown'"/>
        </xsl:attribute>
      </gmd:dateStamp>
      <gmd:metadataStandardName>
        <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data</gco:CharacterString>
      </gmd:metadataStandardName>
      <gmd:metadataStandardVersion>
        <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>
      </gmd:metadataStandardVersion>
      <gmd:spatialRepresentationInfo>
        <xsl:choose>
          <xsl:when test="$otherExtentTotal">
            <gmd:MD_GridSpatialRepresentation>
              <gmd:numberOfDimensions/>
              <xsl:if test="$geospatial_lon_unitsCnt">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'column'"/>
                  <xsl:with-param name="dimensionUnits" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_units']/odap:value,'&quot;','')"/>
                  <xsl:with-param name="dimensionResolution" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_resolution']/odap:value,'&quot;','')"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="$geospatial_lat_unitsCnt">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'row'"/>
                  <xsl:with-param name="dimensionUnits" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_units']/odap:value,'&quot;','')"/>
                  <xsl:with-param name="dimensionResolution" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_resolution']/odap:value,'&quot;','')"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="$vertical_unitsCnt">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'vertical'"/>
                  <xsl:with-param name="dimensionUnits" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_units']/odap:value,'&quot;','')"/>
                  <xsl:with-param name="dimensionResolution" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_resolution']/odap:value,'&quot;','')"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="$timeResCnt">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'temporal'"/>
                  <xsl:with-param name="dimensionUnits" select="'unknown'"/>
                  <xsl:with-param name="dimensionResolution" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_resolution']/odap:value,'&quot;','')"/>
                </xsl:call-template>
              </xsl:if>
              <gmd:cellGeometry>
                <xsl:call-template name="writeCodelist">
                  <xsl:with-param name="codeListName" select="'gmd:MD_CellGeometryCode'"/>
                  <xsl:with-param name="codeListValue" select="'area'"/>
                </xsl:call-template>
              </gmd:cellGeometry>
              <gmd:transformationParameterAvailability gco:nilReason="unknown"/>
            </gmd:MD_GridSpatialRepresentation>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="gco:nilReason">
              <xsl:value-of select="'missing'"/>
            </xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
      </gmd:spatialRepresentationInfo>
      <gmd:identificationInfo>
        <gmd:MD_DataIdentification>
          <gmd:citation>
            <gmd:CI_Citation>
              <gmd:title>
                <xsl:call-template name="writeCharacterString">
                  <xsl:with-param name="testValue" select="$titleCnt"/>
                  <xsl:with-param name="stringToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='title']/odap:value,'&quot;','')"/>
                </xsl:call-template>
              </gmd:title>
              <xsl:choose>
                <xsl:when test="$dateCnt">
                  <xsl:call-template name="writeDate">
                    <xsl:with-param name="testValue" select="$creatorDateCnt"/>
                    <xsl:with-param name="dateToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='date_created']/odap:value,'&quot;','')"/>
                    <xsl:with-param name="dateType" select="'creation'"/>
                  </xsl:call-template>
                  <xsl:call-template name="writeDate">
                    <xsl:with-param name="testValue" select="$issuedDateCnt"/>
                    <xsl:with-param name="dateToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='date_issued']/odap:value,'&quot;','')"/>
                    <xsl:with-param name="dateType" select="'issued'"/>
                  </xsl:call-template>
                  <xsl:call-template name="writeDate">
                    <xsl:with-param name="testValue" select="$modifiedDateCnt"/>
                    <xsl:with-param name="dateToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='date_modified']/odap:value,'&quot;','')"/>
                    <xsl:with-param name="dateType" select="'revision'"/>
                  </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                  <gmd:date>
                    <xsl:attribute name="gco:nilReason">
                      <xsl:value-of select="'missing'"/>
                    </xsl:attribute>
                  </gmd:date>
                </xsl:otherwise>
              </xsl:choose>
              <gmd:identifier>
                <xsl:choose>
                  <xsl:when test="$idCnt">
                    <gmd:MD_Identifier>
                      <xsl:if test="/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='naming_authority']">
                        <gmd:authority>
                          <gmd:CI_Citation>
                            <gmd:title>
                              <gco:CharacterString>
                                <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='naming_authority']/odap:value,'&quot;','')"/>
                              </gco:CharacterString>
                            </gmd:title>
                            <gmd:date>
                              <xsl:attribute name="gco:nilReason">
                                <xsl:value-of select="'inapplicable'"/>
                              </xsl:attribute>
                            </gmd:date>
                          </gmd:CI_Citation>
                        </gmd:authority>
                      </xsl:if>
                      <gmd:code>
                        <gco:CharacterString>
                          <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='id']/odap:value,'&quot;','')"/>
                        </gco:CharacterString>
                      </gmd:code>
                    </gmd:MD_Identifier>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:attribute name="gco:nilReason">
                      <xsl:value-of select="'missing'"/>
                    </xsl:attribute>
                  </xsl:otherwise>
                </xsl:choose>
              </gmd:identifier>
              <xsl:choose>
                <xsl:when test="$responsiblePartyCnt">
                  <xsl:if test="$creatorTotal">
                    <xsl:call-template name="writeResponsibleParty">
                      <xsl:with-param name="testValue" select="$creatorTotal"/>
                      <xsl:with-param name="individualName" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='creator_name']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="organisationName" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='institution']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="email" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='creator_email']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="url" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='creator_url']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="roleCode" select="'originator'"/>
                    </xsl:call-template>
                  </xsl:if>
                  <xsl:if test="$publisherTotal">
                    <xsl:call-template name="writeResponsibleParty">
                      <xsl:with-param name="testValue" select="$publisherTotal"/>
                      <xsl:with-param name="individualName"/>
                      <xsl:with-param name="organisationName" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='publisher_name']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="email" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='publisher_email']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="url" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='publisher_url']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="roleCode" select="'publisher'"/>
                    </xsl:call-template>
                  </xsl:if>
                  <xsl:if test="$contributorTotal">
                    <xsl:call-template name="writeResponsibleParty">
                      <xsl:with-param name="testValue" select="$contributorTotal"/>
                      <xsl:with-param name="individualName" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='contributor_name']/odap:value,'&quot;','')"/>
                      <xsl:with-param name="organisationName"/>
                      <xsl:with-param name="email"/>
                      <xsl:with-param name="url"/>
                      <xsl:with-param name="roleCode" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='contributor_role']/odap:value,'&quot;','')"/>
                    </xsl:call-template>
                  </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                  <gmd:citedResponsibleParty>
                    <xsl:attribute name="gco:nilReason">
                      <xsl:value-of select="'missing'"/>
                    </xsl:attribute>
                  </gmd:citedResponsibleParty>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:if test="$commentCnt">
                <gmd:otherCitationDetails>
                  <gco:CharacterString>
                    <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='comment']/odap:value,'&quot;','')"/>
                  </gco:CharacterString>
                </gmd:otherCitationDetails>
              </xsl:if>
            </gmd:CI_Citation>
          </gmd:citation>
          <gmd:abstract>
            <xsl:call-template name="writeCharacterString">
              <xsl:with-param name="testValue" select="$summaryCnt"/>
              <xsl:with-param name="stringToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='summary']/odap:value,'&quot;','')"/>
            </xsl:call-template>
          </gmd:abstract>
          <gmd:credit>
            <xsl:call-template name="writeCharacterString">
              <xsl:with-param name="testValue" select="$creatorAckCnt"/>
              <xsl:with-param name="stringToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='acknowledgment']/odap:value,'&quot;','')"/>
            </xsl:call-template>
          </gmd:credit>
          <xsl:if test="$keywordsCnt">
            <gmd:descriptiveKeywords>
              <gmd:MD_Keywords>
                <gmd:keyword>
                  <gco:CharacterString>
                    <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='keywords']/odap:value,'&quot;','')"/>
                  </gco:CharacterString>
                </gmd:keyword>
                <gmd:type>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_KeywordTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'theme'"/>
                  </xsl:call-template>
                </gmd:type>
                <gmd:thesaurusName>
                  <gmd:CI_Citation>
                    <gmd:title>
                      <gco:CharacterString>
                        <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='keywords_vocabulary']/odap:value,'&quot;','')"/>
                      </gco:CharacterString>
                    </gmd:title>
                    <gmd:date>
                      <xsl:attribute name="gco:nilReason">
                        <xsl:value-of select="'unknown'"/>
                      </xsl:attribute>
                    </gmd:date>
                  </gmd:CI_Citation>
                </gmd:thesaurusName>
              </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
          </xsl:if>
          <xsl:if test="$standardNameCnt">
            <gmd:descriptiveKeywords>
              <gmd:MD_Keywords>
                <xsl:for-each select="/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:variable/odap:Attribute[@name='standard_name']">
                  <gmd:keyword>
                    <gco:CharacterString>
                      <xsl:value-of select="./odap:value"/>
                    </gco:CharacterString>
                  </gmd:keyword>
                </xsl:for-each>
                <gmd:type>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_KeywordTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'theme'"/>
                  </xsl:call-template>
                </gmd:type>
                <gmd:thesaurusName>
                  <gmd:CI_Citation>
                    <gmd:title>
                      <xsl:call-template name="writeCharacterString">
                        <xsl:with-param name="testValue" select="$stdNameVocabCnt"/>
                        <xsl:with-param name="stringToWrite" select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='standard_name_vocabulary']/odap:value,'&quot;','')"/>
                      </xsl:call-template>
                    </gmd:title>
                    <gmd:date gco:nilReason="unknown"/>
                  </gmd:CI_Citation>
                </gmd:thesaurusName>
              </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
          </xsl:if>
          <xsl:if test="$licenseCnt">
            <gmd:resourceConstraints>
              <gmd:MD_LegalConstraints>
                <gmd:useLimitation>
                  <gco:CharacterString>
                    <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='license']/odap:value,'&quot;','')"/>
                  </gco:CharacterString>
                </gmd:useLimitation>
              </gmd:MD_LegalConstraints>
            </gmd:resourceConstraints>
          </xsl:if>
          <xsl:if test="$creatorProjCnt">
            <gmd:aggregationInfo>
              <gmd:MD_AggregateInformation>
                <gmd:aggregateDataSetName>
                  <gmd:CI_Citation>
                    <gmd:title>
                      <gco:CharacterString>
                        <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='project']/odap:value,'&quot;','')"/>
                      </gco:CharacterString>
                    </gmd:title>
                    <gmd:date gco:nilReason="inapplicable"/>
                  </gmd:CI_Citation>
                </gmd:aggregateDataSetName>
                <gmd:associationType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_AssociationTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'largerWorkCitation'"/>
                  </xsl:call-template>
                </gmd:associationType>
                <gmd:initiativeType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_InitiativeTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'project'"/>
                  </xsl:call-template>
                </gmd:initiativeType>
              </gmd:MD_AggregateInformation>
            </gmd:aggregationInfo>
          </xsl:if>
          <xsl:if test="$cdmTypeCnt">
            <gmd:aggregationInfo>
              <gmd:MD_AggregateInformation>
                <gmd:aggregateDataSetIdentifier>
                  <gmd:MD_Identifier>
                    <gmd:authority>
                      <gmd:CI_Citation>
                        <gmd:title>
                          <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>
                        </gmd:title>
                        <gmd:date gco:nilReason="inapplicable"/>
                      </gmd:CI_Citation>
                    </gmd:authority>
                    <gmd:code>
                      <gco:CharacterString>
                        <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='cdm_data_type']/odap:value,'&quot;','')"/>
                      </gco:CharacterString>
                    </gmd:code>
                  </gmd:MD_Identifier>
                </gmd:aggregateDataSetIdentifier>
                <gmd:associationType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_AssociationTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'largerWorkCitation'"/>
                  </xsl:call-template>
                </gmd:associationType>
                <gmd:initiativeType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_InitiativeTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'project'"/>
                  </xsl:call-template>
                </gmd:initiativeType>
              </gmd:MD_AggregateInformation>
            </gmd:aggregationInfo>
          </xsl:if>
          <gmd:language>
            <gco:CharacterString>eng</gco:CharacterString>
          </gmd:language>
          <gmd:topicCategory>
            <gmd:MD_TopicCategoryCode>climatologyMeteorologyAtmosphere</gmd:MD_TopicCategoryCode>
          </gmd:topicCategory>
          <gmd:extent>
            <xsl:choose>
              <xsl:when test="$extentTotal">
                <gmd:EX_Extent>
                  <xsl:attribute name="id">
                    <xsl:value-of select="'boundingExtent'"/>
                  </xsl:attribute>
                  <xsl:if test="$geospatial_lat_minCnt">
                    <gmd:geographicElement>
                      <gmd:EX_GeographicBoundingBox id="boundingGeographicBoundingBox">
                        <gmd:extentTypeCode>
                          <gco:Boolean>1</gco:Boolean>
                        </gmd:extentTypeCode>
                        <gmd:westBoundLongitude>
                          <gco:Decimal>
                            <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_min']/odap:value,'&quot;','')"/>
                          </gco:Decimal>
                        </gmd:westBoundLongitude>
                        <gmd:eastBoundLongitude>
                          <gco:Decimal>
                            <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lon_max']/odap:value,'&quot;','')"/>
                          </gco:Decimal>
                        </gmd:eastBoundLongitude>
                        <gmd:southBoundLatitude>
                          <gco:Decimal>
                            <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_min']/odap:value,'&quot;','')"/>
                          </gco:Decimal>
                        </gmd:southBoundLatitude>
                        <gmd:northBoundLatitude>
                          <gco:Decimal>
                            <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_lat_max']/odap:value,'&quot;','')"/>
                          </gco:Decimal>
                        </gmd:northBoundLatitude>
                      </gmd:EX_GeographicBoundingBox>
                    </gmd:geographicElement>
                  </xsl:if>
                  <xsl:if test="$timeStartCnt">
                    <gmd:temporalElement>
                      <gmd:EX_TemporalExtent>
                        <xsl:attribute name="id">
                          <xsl:value-of select="'boundingTemporalExtent'"/>
                        </xsl:attribute>
                        <gmd:extent>
                          <gml:TimePeriod gml:id="timePeriod_id">
                            <gml:beginPosition>
                              <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_start']/odap:value,'&quot;','')"/>
                            </gml:beginPosition>
                            <gml:endPosition>
                              <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='time_coverage_end']/odap:value,'&quot;','')"/>
                            </gml:endPosition>
                          </gml:TimePeriod>
                        </gmd:extent>
                      </gmd:EX_TemporalExtent>
                    </gmd:temporalElement>
                  </xsl:if>
                  <xsl:if test="$vertical_minCnt">
                    <gmd:verticalElement>
                      <gmd:EX_VerticalExtent>
                        <gmd:minimumValue>
                          <gco:Real>
                            <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_min']/odap:value,'&quot;','')"/>
                          </gco:Real>
                        </gmd:minimumValue>
                        <gmd:maximumValue>
                          <gco:Real>
                            <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='geospatial_vertical_max']/odap:value,'&quot;','')"/>
                          </gco:Real>
                        </gmd:maximumValue>
                        <gmd:verticalCRS>
                          <xsl:attribute name="gco:nilReason">
                            <xsl:value-of select="'missing'"/>
                          </xsl:attribute>
                        </gmd:verticalCRS>
                      </gmd:EX_VerticalExtent>
                    </gmd:verticalElement>
                  </xsl:if>
                </gmd:EX_Extent>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="gco:nilReason">
                  <xsl:value-of select="'missing'"/>
                </xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
          </gmd:extent>
        </gmd:MD_DataIdentification>
      </gmd:identificationInfo>
      <xsl:if test="$variableCnt">
        <gmd:contentInfo>
          <gmi:MI_CoverageDescription>
            <gmd:AttributeDescription>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'unknown'"/>
              </xsl:attribute>
            </gmd:AttributeDescription>
            <gmd:contentType>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'unknown'"/>
              </xsl:attribute>
            </gmd:contentType>
            <xsl:for-each select="/odap:Dataset/odap:Grid">
              <xsl:call-template name="writeVariable">
                <xsl:with-param name="variableName" select="translate(./@name,'&quot;','')"/>
                <xsl:with-param name="variableLongName" select="translate(./odap:Attribute[@name='long_name']/odap:value,'&quot;','')"/>
                <xsl:with-param name="variableType" select="translate(./@type,'&quot;','')"/>
                <xsl:with-param name="maxValue" select="translate(./odap:Attribute[@name='valid_max']/odap:value,'&quot;','')"/>
                <xsl:with-param name="minValue" select="translate(./odap:Attribute[@name='valid_min']/odap:value,'&quot;','')"/>
                <xsl:with-param name="variableUnits" select="translate(./odap:Attribute[@name='units']/odap:value,'&quot;','')"/>
                <xsl:with-param name="scaleFactor" select="translate(./odap:Attribute[@name='scale_factor']/odap:value,'&quot;','')"/>
                <xsl:with-param name="offset" select="translate(./odap:Attribute[@name='offset']/odap:value,'&quot;','')"/>
              </xsl:call-template>
            </xsl:for-each>
          </gmi:MI_CoverageDescription>
        </gmd:contentInfo>
      </xsl:if>
      <xsl:if test="$historyCnt">
        <gmd:dataQualityInfo>
          <gmd:DQ_DataQuality>
            <gmd:scope>
              <gmd:DQ_Scope>
                <gmd:level>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_ScopeCode'"/>
                    <xsl:with-param name="codeListValue" select="'dataset'"/>
                  </xsl:call-template>
                </gmd:level>
              </gmd:DQ_Scope>
            </gmd:scope>
            <gmd:lineage>
              <gmd:LI_Lineage>
                <gmd:statement>
                  <gco:CharacterString>
                    <xsl:value-of select="translate(/odap:Dataset/odap:Attribute[@name='NC_GLOBAL']/odap:Attribute[@name='history']/odap:value,'&quot;','')"/>
                  </gco:CharacterString>
                </gmd:statement>
              </gmd:LI_Lineage>
            </gmd:lineage>
          </gmd:DQ_DataQuality>
        </gmd:dataQualityInfo>
      </xsl:if>
      <gmd:metadataMaintenance>
        <gmd:MD_MaintenanceInformation>
          <gmd:maintenanceAndUpdateFrequency gco:nilReason="unknown"/>
          <gmd:maintenanceNote>
            <gco:CharacterString>This record was translated from NcML using OPeNDAP2MI.xsl Version <xsl:value-of select="$stylesheetVersion"/></gco:CharacterString>
          </gmd:maintenanceNote>
        </gmd:MD_MaintenanceInformation>
      </gmd:metadataMaintenance>
    </gmi:MI_Metadata>
  </xsl:template>
  <xsl:template name="writeCodelist">
    <xsl:param name="codeListName"/>
    <xsl:param name="codeListValue"/>
    <xsl:variable name="codeListLocation" select="'http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml'"/>
    <xsl:element name="{$codeListName}">
      <xsl:attribute name="codeList">
        <xsl:value-of select="$codeListLocation"/>
        <xsl:value-of select="'#'"/>
        <xsl:value-of select="$codeListName"/>
      </xsl:attribute>
      <xsl:attribute name="codeListValue">
        <xsl:value-of select="$codeListValue"/>
      </xsl:attribute>
      <xsl:value-of select="$codeListValue"/>
    </xsl:element>
  </xsl:template>
  <xsl:template name="writeCharacterString">
    <xsl:param name="testValue"/>
    <xsl:param name="stringToWrite"/>
    <xsl:choose>
      <xsl:when test="$testValue">
        <gco:CharacterString>
          <xsl:value-of select="$stringToWrite"/>
        </gco:CharacterString>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="gco:nilReason">
          <xsl:value-of select="'missing'"/>
        </xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="writeDate">
    <xsl:param name="testValue"/>
    <xsl:param name="dateToWrite"/>
    <xsl:param name="dateType"/>
    <xsl:choose>
      <xsl:when test="contains($dateToWrite, 'T' ) ">
        <gmd:date>
          <gmd:CI_Date>
            <gmd:date>
              <gco:DateTime>
                <xsl:value-of select="$dateToWrite"/>
              </gco:DateTime>
            </gmd:date>
            <gmd:dateType>
              <xsl:call-template name="writeCodelist">
                <xsl:with-param name="codeListName" select="'gmd:CI_DateTypeCode'"/>
                <xsl:with-param name="codeListValue" select="$dateType"/>
              </xsl:call-template>
            </gmd:dateType>
          </gmd:CI_Date>
        </gmd:date>
      </xsl:when>
      <xsl:otherwise>
        <gmd:date>
          <gmd:CI_Date>
            <gmd:date>
              <gco:Date>
                <xsl:value-of select="$dateToWrite"/>
              </gco:Date>
            </gmd:date>
            <gmd:dateType>
              <xsl:call-template name="writeCodelist">
                <xsl:with-param name="codeListName" select="'gmd:CI_DateTypeCode'"/>
                <xsl:with-param name="codeListValue" select="$dateType"/>
              </xsl:call-template>
            </gmd:dateType>
          </gmd:CI_Date>
        </gmd:date>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="writeResponsibleParty">
    <xsl:param name="testValue"/>
    <xsl:param name="individualName"/>
    <xsl:param name="organisationName"/>
    <xsl:param name="email"/>
    <xsl:param name="url"/>
    <xsl:param name="roleCode"/>
    <xsl:choose>
      <xsl:when test="$testValue">
        <gmd:citedResponsibleParty>
          <gmd:CI_ResponsibleParty>
            <gmd:individualName>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="testValue" select="$individualName"/>
                <xsl:with-param name="stringToWrite" select="$individualName"/>
              </xsl:call-template>
            </gmd:individualName>
            <gmd:organisationName>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="testValue" select="$organisationName"/>
                <xsl:with-param name="stringToWrite" select="$organisationName"/>
              </xsl:call-template>
            </gmd:organisationName>
            <gmd:contactInfo>
              <xsl:choose>
                <xsl:when test="$email or $url">
                  <gmd:CI_Contact>
                    <xsl:if test="$email">
                      <gmd:address>
                        <gmd:CI_Address>
                          <gmd:electronicMailAddress>
                            <gco:CharacterString>
                              <xsl:value-of select="$email"/>
                            </gco:CharacterString>
                          </gmd:electronicMailAddress>
                        </gmd:CI_Address>
                      </gmd:address>
                    </xsl:if>
                    <xsl:if test="$url">
                      <gmd:onlineResource>
                        <gmd:CI_OnlineResource>
                          <gmd:linkage>
                            <gmd:URL>
                              <xsl:value-of select="$url"/>
                            </gmd:URL>
                          </gmd:linkage>
                          <gmd:protocol>
                            <gco:CharacterString>http</gco:CharacterString>
                          </gmd:protocol>
                          <gmd:applicationProfile>
                            <gco:CharacterString>web browser</gco:CharacterString>
                          </gmd:applicationProfile>
                          <gmd:name>
                            <xsl:attribute name="gco:nilReason">
                              <xsl:value-of select="'unknown'"/>
                            </xsl:attribute>
                          </gmd:name>
                          <gmd:description>
                            <xsl:attribute name="gco:nilReason">
                              <xsl:value-of select="'unknown'"/>
                            </xsl:attribute>
                          </gmd:description>
                          <gmd:function>
                            <xsl:call-template name="writeCodelist">
                              <xsl:with-param name="codeListName" select="'gmd:CI_OnLineFunctionCode'"/>
                              <xsl:with-param name="codeListValue" select="'information'"/>
                            </xsl:call-template>
                          </gmd:function>
                        </gmd:CI_OnlineResource>
                      </gmd:onlineResource>
                    </xsl:if>
                  </gmd:CI_Contact>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:attribute name="gco:nilReason">
                    <xsl:value-of select="'missing'"/>
                  </xsl:attribute>
                </xsl:otherwise>
              </xsl:choose>
            </gmd:contactInfo>
            <gmd:role>
              <xsl:call-template name="writeCodelist">
                <xsl:with-param name="codeListName" select="'gmd:CI_RoleCode'"/>
                <xsl:with-param name="codeListValue" select="$roleCode"/>
              </xsl:call-template>
            </gmd:role>
          </gmd:CI_ResponsibleParty>
        </gmd:citedResponsibleParty>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="writeDimension">
    <xsl:param name="dimensionType"/>
    <xsl:param name="dimensionUnits"/>
    <xsl:param name="dimensionResolution"/>
    <gmd:axisDimensionProperties>
      <gmd:MD_Dimension>
        <gmd:dimensionName>
          <xsl:call-template name="writeCodelist">
            <xsl:with-param name="codeListName" select="'gmd:MD_DimensionNameTypeCode'"/>
            <xsl:with-param name="codeListValue" select="$dimensionType"/>
          </xsl:call-template>
        </gmd:dimensionName>
        <gmd:dimensionSize/>
        <gmd:resolution>
          <gco:Measure>
            <xsl:attribute name="uom">
              <xsl:value-of select="$dimensionUnits"/>
            </xsl:attribute>
            <xsl:value-of select="$dimensionResolution"/>
          </gco:Measure>
        </gmd:resolution>
      </gmd:MD_Dimension>
    </gmd:axisDimensionProperties>
  </xsl:template>
  <xsl:template name="writeVariable">
    <xsl:param name="variableName"/>
    <xsl:param name="variableLongName"/>
    <xsl:param name="variableType"/>
    <xsl:param name="maxValue"/>
    <xsl:param name="minValue"/>
    <xsl:param name="variableUnits"/>
    <xsl:param name="scaleFactor"/>
    <xsl:param name="offset"/>
    <gmd:dimension>
      <gmd:MD_Band>
        <gmd:sequenceIdentifier>
          <gco:MemberName>
            <gco:aName>
              <gco:CharacterString>
                <xsl:value-of select="$variableName"/>
              </gco:CharacterString>
            </gco:aName>
            <gco:attributeType>
              <gco:TypeName>
                <gco:aName>
                  <gco:CharacterString>
                    <xsl:value-of select="$variableType"/>
                  </gco:CharacterString>
                </gco:aName>
              </gco:TypeName>
            </gco:attributeType>
          </gco:MemberName>
        </gmd:sequenceIdentifier>
        <xsl:if test="$variableLongName">
          <gmd:descriptor>
            <xsl:call-template name="writeCharacterString">
              <xsl:with-param name="testValue" select="$variableLongName"/>
              <xsl:with-param name="stringToWrite" select="$variableLongName"/>
            </xsl:call-template>
          </gmd:descriptor>
        </xsl:if>
        <xsl:if test="$maxValue">
          <gmd:maxValue>
            <gco:Real>
              <xsl:value-of select="$maxValue"/>
            </gco:Real>
          </gmd:maxValue>
        </xsl:if>
        <xsl:if test="$minValue">
          <gmd:minValue>
            <gco:Real>
              <xsl:value-of select="$minValue"/>
            </gco:Real>
          </gmd:minValue>
        </xsl:if>
        <xsl:if test="$variableUnits">
          <gmd:units>
            <xsl:attribute name="xlink:href">
              <xsl:value-of select="'http://someUnitsDictionary.xml#'"/>
              <xsl:value-of select="$variableUnits"/>
            </xsl:attribute>
          </gmd:units>
        </xsl:if>
        <xsl:if test="$scaleFactor">
          <gmd:scaleFactor>
            <gco:Real>
              <xsl:value-of select="$scaleFactor"/>
            </gco:Real>
          </gmd:scaleFactor>
        </xsl:if>
        <xsl:if test="$offset">
          <gmd:offset>
            <gco:Real>
              <xsl:value-of select="$offset"/>
            </gco:Real>
          </gmd:offset>
        </xsl:if>
      </gmd:MD_Band>
    </gmd:dimension>
  </xsl:template>
</xsl:stylesheet>

