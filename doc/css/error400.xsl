<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        >
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:template match="/BESError">

        <html>
            <head>
                <meta http-equiv="Content-Type"
                      content="text/html; charset=ISO-8859-1"/>
                <link rel='stylesheet' href='/opendap/docs/css/contents.css'
                      type='text/css' />
                <title>Hyrax: Bad Request</title>
            </head>

            <body>
                <p align="left"></p>
                <h1 align="center">Hyrax : Bad Request (400)</h1>
                <hr align="left" size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <img alt="BadRequest"
                                 src="/opendap/docs/images/BadDapRequest.gif"
                                 width="323" height="350"/>
                        </td>
                        <td>
                            <p align="left">It appears that you have submitted a
                                BadRequest.
                            </p>
                            <p align="left">The error message associated with
                                your request was:
                            </p>
                            <blockquote>
                                <p>
                                    <strong>
                                        <xsl:value-of select="Message"/>
                                    </strong>
                                </p>
                            </blockquote>
                            <p align="left">If you think that the server is
                                broken (that the URL you submitted should have
                                worked), then please contact the user
                                support coordinator for this server at:
                                <a href="mailto:{Administrator}">
                                    <xsl:value-of select="Administrator"/>
                                </a>
                            </p>
                        </td>
                    </tr>
                </table>
                <hr align="left" size="1" noshade="noshade"/>
                <h1 align="center">Hyrax : Bad Request (400)</h1>
                <p align="left"></p>
            </body>
        </html>


    </xsl:template >
</xsl:stylesheet>