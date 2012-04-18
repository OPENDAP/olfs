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
<!DOCTYPE stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:dap="http://xml.opendap.org/ns/DAP/4.0#"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xlink="http://www.w3.org/1999/xlink"
        >
    <xsl:import href="version.xsl" />

    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:variable name="docsService">/opendap/docs</xsl:variable>


    <xsl:variable name="beginTime" select="/dap:Dataset/dap:async/dap:beginAccess" />
    <xsl:variable name="endTime" select="/dap:Dataset/dap:async/dap:endAccess" />

    <xsl:variable name="dataAccessUrl" select="/dap:Dataset/dap:async/@xlink:href" />



    <xsl:template match="dap:Dataset">
        <html>
            <xsl:call-template name="StyleAndScript"/>
            <head>
                <link rel='stylesheet' href='{$docsService}/css/contents.css'
                      type='text/css'/>
                <title>OPeNDAP Hyrax: Dataset Service Description for <xsl:value-of select="@xml:base"/></title>
            </head>
            <body>

                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->


                <table width="100%" border="0">
                    <tr>
                        <td><img alt="OPeNDAP Logo" src='{$docsService}/images/logo.gif'/></td>
                        <td>
                            <div class="xlarge" style="text-align:middle">Asynchronous Response</div>
                        </td>
                     </tr>
                </table>

                <h1>
                    Dataset: <xsl:value-of select="@xml:base"/>
                    <div class="small">Request Url: <xsl:value-of select="dap:async/@xlink:href"/> </div>
                </h1>
                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
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

                <br />
                <br />



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
                  beginAccessTime="<xsl:value-of select="$beginTime"/>";
                  endAccessTime="<xsl:value-of select="$endTime"/>";
                  var startTime = Date.parse(beginAccessTime);
                  var endTime = Date.parse(endAccessTime);
                  var endDate = new Date(endTime);
                  var dataAccessUrl="<xsl:value-of select="$dataAccessUrl"/>"

                  updateProgessBar();
            </script>

        </html>
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
        /**
         * Date.parse with progressive enhancement for ISO 8601 <https://github.com/csnover/js-iso8601>
         * NON-CONFORMANT EDITION.
         * © 2011 Colin Snover <http://zetafleet.com>
         * Released under MIT license.
         */
        (function (Date, undefined) {
            var origParse = Date.parse, numericKeys = [ 1, 4, 5, 6, 10, 11 ];
            Date.parse = function (date) {
                var timestamp, struct, minutesOffset = 0;

                //              1 YYYY                 2 MM        3 DD              4 HH     5 mm        6 ss            7 msec         8 Z 9 ±    10 tzHH    11 tzmm
                if ((struct = /^(\d{4}|[+\-]\d{6})(?:-?(\d{2})(?:-?(\d{2}))?)?(?:[ T]?(\d{2}):?(\d{2})(?::?(\d{2})(?:[,\.](\d{3,}))?)?(?:(Z)|([+\-])(\d{2})(?::?(\d{2}))?)?)?$/.exec(date))) {
                    // avoid NaN timestamps caused by ?undefined? values being passed to Date.UTC
                    for (var i = 0, k; (k = numericKeys[i]); ++i) {
                        struct[k] = +struct[k] || 0;
                    }

                    // allow undefined days and months
                    struct[2] = (+struct[2] || 1) - 1;
                    struct[3] = +struct[3] || 1;

                    // allow arbitrary sub-second precision beyond milliseconds
                    struct[7] = struct[7] ? +struct[7].substr(0, 3) : 0;

                    // timestamps without timezone identifiers should be considered local time
                    if (struct[8] === undefined && struct[9] === undefined) {
                        timestamp = +new Date(struct[1], struct[2], struct[3], struct[4], struct[5], struct[6], struct[7]);
                    }
                    else {
                        if (struct[8] !== 'Z' && struct[9] !== undefined) {
                            minutesOffset = struct[10] * 60 + struct[11];

                            if (struct[9] === '+') {
                                minutesOffset = 0 - minutesOffset;
                            }
                        }

                        timestamp = Date.UTC(struct[1], struct[2], struct[3], struct[4], struct[5] + minutesOffset, struct[6], struct[7]);
                    }
                }
                else {
                    timestamp = origParse ? origParse(date) : NaN;
                }

                return timestamp;
            };
        }(Date));


        var d = new Date();
        var pageLoadTime = d.getTime();

        function updateProgessBar() {

            var currentDate   = new Date();
            var now = currentDate.getTime();
            var pbar = document.getElementById("progress");
            var tR = document.getElementById("timeRemaining");
            var dAL = document.getElementById("dataAccessLink");
            var totalWaitTime = startTime - pageLoadTime;


            if(startTime>now){
                var remainingWaitTime = startTime - now;
                var percentComplete = 100*(totalWaitTime - remainingWaitTime)/totalWaitTime;
                pbar.style.width = percentComplete + '%';

                tR.innerHTML = "<strong>"+remainingWaitTime/1000 +"</strong> seconds";

            }
            else if (startTime<=now && now<=endTime) {
                pbar.style.background = '#00FF00';
                pbar.style.width = '100%';
                pbar.innerHTML = currentDate.toLocaleString();
                tR.innerHTML = "Your Data Should Be Available For Access Until: "+endDate.toLocaleString();

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
            besLogTailTimer = setTimeout("updateProgessBar()", 1000);

        }
        ]]>
        </script>


    </xsl:template>




</xsl:stylesheet>