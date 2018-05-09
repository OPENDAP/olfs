<xsl:stylesheet
        version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
        xmlns:ncml="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
        xmlns:xlink="http://www.w3.org/1999/xlink"

>
    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>

    <xsl:variable name="indentIncrement" select="10"/>

    <xsl:template match="*" mode="metadataDetail" />

    <xsl:template match="thredds:metadata" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:param name="currentDataset" />
        <xsl:apply-templates mode="metadataDetail">
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:documentation" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:if test="@type">
            <div style="padding-left: {$indent};"><em>documentation[<b><xsl:value-of select="@type"/>]: </b></em><xsl:value-of select="."/></div>
        </xsl:if>

        <xsl:if test="@xlink:href">
            <div style="padding-left: {$indent};"><em>documentation[<b>Linked Document</b>]: </em><a href="{@xlink:href}"><xsl:value-of select="@xlink:title"/></a></div>
        </xsl:if>
    </xsl:template>




    <xsl:template match="thredds:property" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="propertyDetail">
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:contributor" mode="metadataDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Contributer:</em></span>
        <xsl:apply-templates select="." mode="contributorDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:creator" mode="metadataDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Creator:</em></span>
        <xsl:apply-templates select="." mode="creatorDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
        <br/>
    </xsl:template>

    <xsl:template match="thredds:date" mode="metadataDetail">
        <xsl:param name="indent" />

        <span style="padding-left: {$indent}px;"><em>Date: </em></span>

        <xsl:apply-templates select="." mode="dateDetail" >
            <xsl:with-param name="indent" select="0"/>
        </xsl:apply-templates>
    </xsl:template>


    <xsl:template match="thredds:keyword" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="keywordDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:project" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="projectDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:publisher" mode="metadataDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Publisher:</em></span><br/>
        <xsl:apply-templates select="." mode="publisherDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>

    </xsl:template>

    <xsl:template match="thredds:geospatialCoverage" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="geospatialCoverageDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:timeCoverage" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="timeCoverageDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:variables" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="variablesDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:dataType" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="dataTypeDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:dataSize" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="dataSizeDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:dataFormat" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="dataFormatDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:serviceName" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="serviceNameDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:authority" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="authorityDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:access" mode="metadataDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates select="." mode="accessDetail" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:dataset" mode="metadataDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;" class="small_bold"><xsl:value-of select="@name"/></span>
        <br/>
        <xsl:apply-templates select="." mode="datasetDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
        <br/>
    </xsl:template>


    <!-- ******************************************************
      -  timeCoverageDetail

            <timeCoverage>
                <start>2008-12-29 12:00:00Z</start>
                <end>2009-01-01 18:00:00Z</end>
            </timeCoverage>
            <timeCoverage>
                <start>2008-12-29 12:00:00Z</start>
                <duration>20.2 hours</duration>
            </timeCoverage>
            <timeCoverage>
                <end>2008-12-29 12:00:00Z</end>
                <duration>20.2 hours</duration>
            </timeCoverage>
    -->
    <xsl:template match="thredds:timeCoverage" mode="timeCoverageDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates mode="timeCoverageDetail">
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:start" mode="timeCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>start: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="thredds:end" mode="timeCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>end: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="thredds:duration" mode="timeCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>duration: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <!-- ******************************************************
      -  accessDetail
          <xsd:element name="access">
            <xsd:complexType>
                <xsd:sequence>
                    <xsd:element ref="dataSize" minOccurs="0"/>
                </xsd:sequence>
                <xsd:attribute name="urlPath" type="xsd:token" use="required"/>
                <xsd:attribute name="serviceName" type="xsd:string"/>
                <xsd:attribute name="dataFormat" type="dataFormatTypes"/>
            </xsd:complexType>
          </xsd:element >
    -->
    <xsl:template match="thredds:access" mode="accessDetail">
        <xsl:param name="indent" />

        <span style="padding-left: {$indent}px;"><em>Access:</em></span><br/>

        <span style="padding-left: {$indent+$indentIncrement}px;"><em>urlPath: </em><xsl:value-of select="@urlPath" /></span><br/>
        <span style="padding-left: {$indent+$indentIncrement}px;"><em>serviceName: </em><xsl:value-of select="@serviceName" /></span><br/>
        <xsl:if test="@dataFormat">
            <span style="padding-left: {$indent+$indentIncrement}px;"><em>dataFormat: </em><xsl:value-of select="@dataFormat" /></span><br/>
        </xsl:if>

    </xsl:template>


    <xsl:template match="thredds:dataType" mode="dataTypeDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Data type: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="thredds:dataSize" mode="dataSizeDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Data size: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="thredds:dataFormat" mode="dataFormatDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Data Format: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="thredds:serviceName" mode="serviceNameDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Service Name: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="thredds:authority" mode="authorityDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;"><em>Naming Authority: </em><xsl:value-of select="."/></span><br/>
    </xsl:template>








    <!-- ******************************************************
      -  propertyDetail


             <geospatialCoverage zpositive="down">
               <northsouth>
                 <start>10</start>
                 <size>80</size>
                 <resolution>2</resolution>
                 <units>degrees_north</units>
               </northsouth>
               <eastwest>
                 <start>-130</start>
                 <size>260</size>
                 <resolution>2</resolution>
                 <units>degrees_east</units>
               </eastwest>
               <updown>
                 <start>0</start>
                 <size>22</size>
                 <resolution>0.5</resolution>
                 <units>km</units>
               </updown>
              </geospatialCoverage>

              <geospatialCoverage>
                <name vocabulary="Thredds">global</name>
              </geospatialCoverage>

     -->
    <xsl:template match="thredds:geospatialCoverage" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <div class="small">
            <span style="padding-left: {$indent}px;">
                <em>Geospatial Coverage Instance</em>
            </span>
            <br/>
            <xsl:apply-templates mode="geospatialCoverageDetail" >
                <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
            </xsl:apply-templates>
            <xsl:if test="@zpositive">
                <span style="padding-left: {$indent}px;">
                    <b>z increases in the <xsl:value-of select="@zpositive" /> direction.</b>
                </span>
            </xsl:if>
        </div>
    </xsl:template>


    <xsl:template match="thredds:northsouth" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <b>north-south:</b>
        </span>
        <br/>
        <xsl:apply-templates mode="geospatialCoverageDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:eastwest" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <b>east-west:</b>
        </span>
        <br/>
        <xsl:apply-templates mode="geospatialCoverageDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:updown" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <b>up-down:</b>
        </span>
        <br/>
        <xsl:apply-templates mode="geospatialCoverageDetail" >
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="thredds:start" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <em>start: </em> <xsl:value-of select="." />
        </span>
        <br/>
    </xsl:template>

    <xsl:template match="thredds:size" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <em>size: </em> <xsl:value-of select="." />
        </span>
        <br/>
    </xsl:template>

    <xsl:template match="thredds:resolution" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <em>resolution: </em> <xsl:value-of select="." />
        </span>
        <br/>
    </xsl:template>

    <xsl:template match="thredds:units" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <em>units: </em> <xsl:value-of select="." />
        </span>
        <br/>
    </xsl:template>

    <xsl:template match="thredds:name" mode="geospatialCoverageDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <b>name: </b><em><xsl:value-of select="." /> (<xsl:value-of select="@vocabulary"/> vocabulary)</em>
        </span>
        <br/>
    </xsl:template>





    <!-- ******************************************************
      -  propertyDetail
     -->
    <xsl:template match="thredds:property" mode="propertyDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <b><xsl:value-of select="@name" /></b> = <xsl:value-of select="@value" />
        </span>
        <br/>
    </xsl:template>



    <!-- ******************************************************
      -  contributorDetail
     -->
    <xsl:template match="thredds:contributor" mode="contributorDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <em>Contributor: </em><xsl:value-of select="." />, <xsl:value-of select="@role" />
        </span>
        <br/>
    </xsl:template>



    <!-- ******************************************************
      -  keywordDetail
     -->
    <xsl:template match="thredds:keyword" mode="keywordDetail">
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <em>keyword
                <xsl:if test="@vocabulary" >
                    (vocab: <xsl:value-of select="@vocabulary" />)
                </xsl:if>
            </em>:
            <xsl:value-of select="." />
        </span>
        <br/>

    </xsl:template>

    <!-- ******************************************************
      -  projectDetail
     -->
    <xsl:template match="thredds:project" mode="projectDetail">
        <xsl:param name="indent" />

        <span style="padding-left: {$indent}px;">
            <em>project
                <xsl:if test="@vocabulary" >
                    (vocab: <xsl:value-of select="@vocabulary" />)
                </xsl:if>
            </em>:
            <xsl:value-of select="." />
        </span>
        <br/>
    </xsl:template>

    <!-- ******************************************************
      -  variablesDetail
     -->
    <xsl:template match="thredds:variables" mode="variablesDetail">
        <xsl:param name="indent" />

        <span style="padding-left: {$indent}px;">Variables[<xsl:value-of select="@vocabulary" />]:</span><br/>
        <xsl:apply-templates  mode="variableDetail">
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>
        <xsl:apply-templates  mode="variableMapDetail">
            <xsl:with-param name="indent" select="$indent+$indentIncrement"/>
        </xsl:apply-templates>

    </xsl:template>

    <!-- ******************************************************
      -  variableDetail
     -->
    <xsl:template match="thredds:variable" mode="variableDetail">
        <xsl:param name="indent" />


        <span style="padding-left: {$indent}px;"><xsl:value-of select="@vocabulary_name" /></span>
        <br/>
        <span style="padding-left: {$indent+$indentIncrement}px;"><em>name: </em><xsl:value-of select="@name" /></span>
        <br/>
        <xsl:if test="@units">
            <span style="padding-left: {$indent+$indentIncrement}px;"><em>units: </em><xsl:value-of select="@units" /></span>
            <br/>
        </xsl:if>


    </xsl:template>

    <xsl:template match="*" mode="variableMapDetail">
    </xsl:template>

    <!-- ******************************************************
      -  variableMapDetail
     -->
    <xsl:template match="thredds:variableMap" mode="variableMapDetail">
        <xsl:param name="indent" />

        <span style="padding-left: {$indent}px;"><b>variableMap: </b>

            <a href="{@xlink:href}">
                <xsl:choose>
                    <xsl:when test="@xlink:title">Title: <xsl:value-of select="@xlink:title" /></xsl:when>
                    <xsl:otherwise>Link</xsl:otherwise>
                </xsl:choose>
            </a>
        </span>
        <br/>

    </xsl:template>

    <!-- ******************************************************
      -  datasetDetail
     -->
    <xsl:template match="thredds:dataset" mode="datasetDetail">
        <xsl:param name="indent" />
        <div class="small" style="padding-left: {$indent}px;"><em>ID: </em><xsl:value-of select="@ID" /></div>
        <xsl:apply-templates select="*" mode="metadataDetail">
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>

    </xsl:template>


    <!-- ******************************************************
      -  metadataDetail

          <xsd:group name="threddsMetadataGroup">
              <xsd:choice>
                    <xsd:element name="documentation" type="documentationType"/>
                    <xsd:element ref="metadata"  />
                    <xsd:element ref="property" />

                    <xsd:element ref="contributor"/>
                    <xsd:element name="creator" type="sourceType"/>
                    <xsd:element name="date" type="dateTypeFormatted" />
                    <xsd:element name="keyword" type="controlledVocabulary" />
                    <xsd:element name="project" type="controlledVocabulary" />
                    <xsd:element name="publisher" type="sourceType"/>

                    <xsd:element ref="geospatialCoverage"/>
                    <xsd:element name="timeCoverage" type="timeCoverageType"/>
                    <xsd:element ref="variables"/>

                    <xsd:element name="dataType" type="dataTypes"/>
                    <xsd:element name="dataFormat" type="dataFormatTypes"/>
                    <xsd:element name="serviceName" type="xsd:string" />
                    <xsd:element name="authority" type="xsd:string" />
                   <xsd:element ref="dataSize"/>
                </xsd:choice>
           </xsd:group>
     -->
    <!-- ******************************************************
      -  creatorDetail

            <creator>
                <name vocabulary="DIF">UCAR/UNIDATA</name>
                <contact url="http://www.unidata.ucar.edu/" email="support@unidata.ucar.edu" />
            </creator>
    -->
    <xsl:template match="thredds:creator" mode="creatorDetail">
        <xsl:param name="indent" />
        <xsl:call-template name="sourceType" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:call-template>
    </xsl:template>


    <!-- ******************************************************
      -  publisherDetail

            <publisher>
                <name vocabulary="DIF">UCAR/UNIDATA</name>
                <contact url="http://www.unidata.ucar.edu/" email="support@unidata.ucar.edu" />
            </publisher>
    -->

    <xsl:template match="thredds:publisher" mode="publisherDetail">
        <xsl:param name="indent" />

        <xsl:call-template name="sourceType" >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="sourceType" >
        <xsl:param name="indent" />
        <span style="padding-left: {$indent}px;">
            <b><xsl:value-of select="thredds:name" /></b>
            <xsl:if test="@vocabulary"> (<xsl:value-of select="@vocabulary" />)</xsl:if>
        </span>
        <br/>
        <span style="padding-left: {$indent+$indentIncrement}px;">
            <em>email: <xsl:value-of select="thredds:contact/@email" /></em>
        </span>
        <br/>
        <span style="padding-left: {$indent+$indentIncrement}px;">
            <em>website: <a href="{thredds:contact/@url}"><xsl:value-of select="thredds:contact/@url" /></a></em>
        </span>

    </xsl:template>



    <!-- ******************************************************
      -  documentationDetail
     -->

    <xsl:template match="thredds:documentation" mode="documentationDetail">
        <xsl:param name="indent" />

        <xsl:if test="@type">
            <span style="padding-left: {$indent};">
                <em><b><xsl:value-of select="@type"/>: </b></em><xsl:value-of select="."/>
            </span>
            <br/>
        </xsl:if>

        <xsl:if test="@xlink:href">
            <span style="padding-left: {$indent};">
                <em><b>Linked Document: </b></em><a href="{@xlink:href}"><xsl:value-of select="@xlink:title"/></a>
            </span>
            <br/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="thredds:*" mode="documentationDetail">
        <xsl:param name="indent" />
        <xsl:apply-templates mode="documentationDetail"  >
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:apply-templates>
    </xsl:template>



    <!-- ******************************************************
      -  dateDetail

        <date type="modified">2008-12-23 23:58:40Z</date>
    -->
    <xsl:template match="thredds:date" mode="dateDetail">
        <xsl:param name="indent" />
            <xsl:value-of select="."/>
            <em> (<xsl:value-of select="@type"/>) </em>
        <br/>
    </xsl:template>

    <xsl:template match="thredds:dataSize" mode="sizeDetail">
        <xsl:param name="indent" />

            <xsl:value-of select="."/>
            <em> (<xsl:value-of select="@units"/>) </em>
        <br/>
    </xsl:template>






</xsl:stylesheet>