<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2016 OPeNDAP, Inc.
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
<!DOCTYPE stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:dap="http://xml.opendap.org/ns/DAP/4.0#"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        >
    <xsl:import href="version.xsl" />

    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:variable name="serviceContext">/@SERVICE_CONTEXT@</xsl:variable>
    <xsl:variable name="docsService"><xsl:value-of select="$serviceContext"/>/docs</xsl:variable>


    <xsl:variable name="expectedDelay" select="/dap:AsynchronousResponse/dap:expectedDelay" />
    <xsl:variable name="responseLifeTime" select="/dap:AsynchronousResponse/dap:responseLifeTime" />

    <xsl:variable name="dataAccessUrl" select="/dap:AsynchronousResponse/dap:link/@href" />
    <xsl:variable name="status" select="/dap:AsynchronousResponse/@status" />


    <xsl:variable name="reasonCode" select="/dap:AsynchronousResponse/dap:reason/@code" />
    <xsl:variable name="description" select="/dap:AsynchronousResponse/dap:description" />

    <xsl:template match="dap:AsynchronousResponse">
        <html>
            <xsl:call-template name="StyleAndScript"/>
            <head>
                <link rel='stylesheet' href='{$docsService}/css/contents.css' type='text/css'/>
                <title>OPeNDAP Hyrax: Asynchronous Response<xsl:value-of select="@xml:base"/></title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->


                <table width="100%" border="0">
                    <tr>
                        <td><img alt="OPeNDAP Logo" src='{$docsService}/images/logo.png'/></td>
                        <td>
                            <div class="xlarge" style="text-align:middle">Asynchronous Response</div>
                        </td>
                     </tr>
                </table>

                <h1>
                    Dataset: <xsl:value-of select="@xml:base"/>
                    <xsl:if test="$dataAccessUrl">
                        <div class="small">Request Url: <xsl:value-of select="$dataAccessUrl"/> </div>
                    </xsl:if>
                </h1>
                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->


                <xsl:choose>
                    <xsl:when test="$status='required'">
                        <xsl:call-template name="required"/>
                    </xsl:when>

                    <xsl:when test="$status='accepted'">
                        <xsl:call-template name="accepted"/>
                    </xsl:when>

                    <xsl:when test="$status='pending'">
                        <xsl:call-template name="pending"/>
                    </xsl:when>

                    <xsl:when test="$status='gone'">
                        <xsl:call-template name="gone"/>
                    </xsl:when>

                    <xsl:when test="$status='rejected'">
                        <xsl:call-template name="rejected"/>
                    </xsl:when>

                    <xsl:otherwise>
                        <xsl:call-template name="error"/>
                    </xsl:otherwise>
                </xsl:choose>







                <br />
                <br />

                <div id="debug"></div>


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
                <!--         HERE IS THE HYRAX VERSION NUMBER               -->
                <!--                                                        -->
                <h3>OPeNDAP Hyrax (<xsl:value-of select="$HyraxVersion"/>)

                    <br/>
                    <a href='{$docsService}/'>Documentation</a>
                </h3>

            </body>

            <script type="text/javascript">

                var expectedAccessDelay=parseInt("<xsl:value-of select="$expectedDelay"/>")*1000; // seconds -> milliseconds
                var responseLifeTime=parseInt("<xsl:value-of select="$responseLifeTime"/>")*1000; // seconds -> milliseconds

                var startTime = pageLoadTime + expectedAccessDelay;
                var endTime = startTime + responseLifeTime
                var startDate= new Date(startTime);
                var endDate= new Date(endTime) ;

                var dataAccessUrl="<xsl:value-of select="$dataAccessUrl"/>"

                updateProgressBar();
            </script>

        </html>
    </xsl:template>





    <xsl:template name="required">
        <div class="large_bold">The server has indicated the its response will be asynchronous.</div>
        <br/>
        <div class="medium">
          I estimate that your data may be accessed in roughly <xsl:value-of select="$expectedDelay"/> seconds.
        </div>
        <br/>
        <div class="medium">
            Because the thing you asked for is going to take a while to produce you must indicate to the
            server that you are willing to wait for the response. <br/>
        </div>
        <br/>

        <div id="dataAccessLink">
            <a href="{$dataAccessUrl}?async=0">Click this link to initiate an asynchronous data access transaction.</a>
        </div>


    </xsl:template>


    <xsl:template name="accepted">
        <div class="large_bold">Congratulations! The server has accepted your request.</div>
        <div class="medium">
            However, the thing you asked for is going to take a while to produce. <br/>
            Please wait...
        </div>
        <br/>
        <div class="medium">
          I estimate that your data may be accessed in roughly:
        </div>
        <br/>

        <center>
        <div id="progress_container">
            <div id="timeRemaining"> </div>
            <div id="progress" style="width: 0%"></div>
        </div>
        <br />

        <div id="dataAccessLink">
            <font color="grey">Data Access:  <xsl:value-of select="$dataAccessUrl"/></font>
        </div>
        </center>

    </xsl:template>


    <xsl:template name="pending">
        <br/>
        <div class="large_bold">The requested resource is a pending asynchronous response. Please try again later..</div>
        <br/>
    </xsl:template>


    <xsl:template name="gone">
        <br/>
        <div class="large_bold">The requested resource was a cached asynchronous response. It's GONE.</div>
        <br/>

    </xsl:template>


    <xsl:template name="rejected">
        <br/>
        <div class="large_bold">The request for an asynchronous response has be rejected.</div>
        <br/>

        <div class="medium"><span class="medium_bold">Reason Code: </span> <xsl:value-of select="$reasonCode"/></div>
        <div class="medium"><span class="medium_bold">Description: </span> <xsl:value-of select="$description"/></div>

    </xsl:template>


    <xsl:template name="error">
        <br/>
        <div class="large_bold">OUCH! The server returned unexpected content.</div>
        <br/>

    </xsl:template>








    <xsl:template name="StyleAndScript">

        <STYLE TYPE="text/css" MEDIA="screen">
        <![CDATA[
            <!--
            /* progress container */
            div#progress_container {
                border: 6px double #ccc;
                width: 80%;
                margin: 0px;
                padding: 0px;
                text-align: left;
                align: center;
            }

            /* progress bar */
            div#progress {
                color: black;
                background-color: #FF8D40;
                height: 12px;
                padding-bottom: 2px;
                font-size: 12px;
                text-align: center;
                overflow: hidden;
            }

            /* Seconds Remaining */
            div#timeRemaining {
                color: black;
                background-color: #FFFFFF;
                height: 24px;
                padding-bottom: 2px;
                font-size: 18px;
                text-align: center;
                overflow: hidden;
            }
            -->
        ]]>
        </STYLE>
        <script type="text/javascript">
        <![CDATA[

        var pageLoadDate = new Date();
        var pageLoadTime = pageLoadDate.getTime();

        function updateProgressBar() {


            var currentDate = new Date();
            var currentTime = currentDate.getTime();
            var pbar = document.getElementById("progress");
            var tR = document.getElementById("timeRemaining");
            var dAL = document.getElementById("dataAccessLink");
            var totalWaitTime = startTime - pageLoadTime;


            if(startTime > currentTime){
                //alert("start > current");
                var remainingWaitTime = startTime - currentTime;
                var percentComplete = 100*(totalWaitTime - remainingWaitTime)/totalWaitTime;
                pbar.style.width = percentComplete + '%';

                tR.innerHTML = "<strong>"+ Math.round(remainingWaitTime/1000) +"</strong> seconds";

            }
            else if(startTime<=currentTime && currentTime<=endTime) {
                pbar.style.background = '#00FF00';
                pbar.style.width = '100%';
                pbar.innerHTML = currentDate.toLocaleString();
                tR.innerHTML = "Your data should be available for access until: "+endDate.toLocaleString();

                dAL.innerHTML = "<a href='"+dataAccessUrl+"'>Data Access: " + dataAccessUrl + "</a>";

            }
            else {
                pbar.style.background = '#000000';
                pbar.style.width = '100%';
                pbar.innerHTML = '';
                tR.innerHTML = "Your response has expired.";
                dAL.innerHTML = "<font color='grey'>Data Access: " + dataAccessUrl + "</font>";

            }


            // Now that we have updated the progress bar, start the cycle again...
            besLogTailTimer = setTimeout("updateProgressBar()", 1000);
            



        }

        function updateDebugInfo() {
            var dbg = document.getElementById("debug");

            debug.innerHTML =
                "<table>" +
                "<tr><td>expectedAccessDelay</td><td>" + expectedAccessDelay +"</td></tr>" +
                "<tr><td>responseLifeTime</td><td>" + responseLifeTime +"</td></tr>" +
                "<tr><td>StartDate</td><td>" + startDate.toLocaleString() +"</td></tr>" +
                "<tr><td>EndDate</td><td>" + endDate.toLocaleString() + "</td></tr>" +
                "<tr><td>PageLoadDate</td><td>" + pageLoadDate.toLocaleString() +"</td></tr>" +
                "<tr><td>CurrentDate</td><td>" + currentDate.toLocaleString() + "</td></tr>" +
                "<tr><td>PageLoadTime</td><td>" + pageLoadTime + "</td></tr>" +
                "<tr><td>CurrentTime</td><td>" + currentTime + "</td></tr>" +
                "<tr><td>StartTime</td><td>" + startTime + "</td></tr>" +
                "<tr><td>EndTime</td><td>" + endTime + "</td></tr>"  +
                "<tr><td>start-current</td><td>" + (startTime - currentTime) + "</td></tr>" +
                "<tr><td>end-current</td><td>" + (endTime - currentTime) + "</td></tr>" +
                "</table>" ;
        }


        ]]>
        </script>


    </xsl:template>




</xsl:stylesheet>