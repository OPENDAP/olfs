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
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
        >
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:variable name="serviceContext">/opendap</xsl:variable>
    <xsl:variable name="docsService">/docs</xsl:variable>
    <xsl:variable name="dapService">/hyrax</xsl:variable>
    

    <xsl:template match="/bes:BESError">
        <html>
            <head>
                <meta http-equiv="Content-Type"
                      content="text/html; charset=ISO-8859-1"/>
                <link rel='stylesheet' href='{$serviceContext}{$docsService}/css/contents.css'
                      type='text/css' />
                <title>Hyrax: ERROR</title>
            </head>

            <body>
                <p> &NBSP; </p>
                <h1 align="center">Hyrax Error</h1>
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <img alt="A Bad Thing Happened Here..."
                                 src="{$serviceContext}{$docsService}/images/superman.jpg"
                                 width="320" height="426"/>
                        </td>
                        <td>
                            <p align="center"><strong>OUCH!</strong></p>
                            <p align="center">
                                <strong>
                                    Something Bad Happened On This Server.
                                </strong>
                            </p>
                            <p> &NBSP; </p>
                            <p> &NBSP; </p>
                            <p align="center">
                                The error message associated with this error is:
                            </p>

                            <blockquote>
                                <p align="center">
                                    <strong>
                                        <xsl:value-of select="bes:Message"/>
                                    </strong>
                                </p>
                            </blockquote>
                            <p> &NBSP; </p>
                            <p> &NBSP; </p>
                            <p align="center">If you think that the server is
                                broken (that the URL you submitted should have
                                worked), then please contact the user
                                support coordinator for this server at:
                                <a href="mailto:{bes:Administrator}">
                                    <xsl:value-of select="bes:Administrator"/>
                                </a>
                            </p>

                        </td>
                    </tr>
                </table>
                <hr size="1" noshade="noshade"/>
                <h1 align="center">Hyrax Error</h1>
                <p align="center"></p>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
