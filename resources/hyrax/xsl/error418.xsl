<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2015 OPeNDAP, Inc.
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
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
                >
    <xsl:param name="serviceContext" />

    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:variable name="docsService">/docs</xsl:variable>
    <xsl:variable name="dapService">/hyrax</xsl:variable>


    <xsl:template match="/bes:BESError">

        <html>
            <head>
                <meta http-equiv="Content-Type"
                      content="text/html; charset=UTF-8"/>
                <link rel='stylesheet' href='{$serviceContext}{$docsService}/css/contents.css'
                      type='text/css' />
                <title>Hyrax: BES Timed Out</title>
            </head>

            <body>
                <p align="left"> &NBSP; </p>
                <h1 align="center">Hyrax : BES Timed Out (418)</h1>
                <hr align="left" size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <img alt="I'm a teapot!!"
                                 title="I can't make coffee. I'm a teapot!"
                                 src="{$serviceContext}{$docsService}/images/teapot.jpg"
                                 width="350" height="300" />
                        </td>
                        <td>
                            <p align="left">I'm sorry, the request you made just took way too long.</p>
                            <p align="left">The specific message associated with your request was:
                            </p>
                            <blockquote>
                                <p>
                                    <strong>
                                        <xsl:value-of select="bes:Message"/>
                                    </strong>
                                </p>
                            </blockquote>
                        </td>
                    </tr>
                </table>
                <hr align="left" size="1" noshade="noshade"/>
                <h1 align="center">Hyrax : BES Timed Out (418)</h1>
                <p align="left"></p>
            </body>
        </html>


    </xsl:template >
</xsl:stylesheet>