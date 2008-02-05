<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE stylesheet [
        <!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
        ]>
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
                <title>Hyrax: ERROR</title>
                <style type="text/css">
                    <!--
                    .style1 {
                        font-size: 24px;
                        font-weight: bold;
                    }
                    -->
                </style>
            </head>

            <body>
                <p>&NBSP;</p>
                <h1 align="center">Hyrax Error</h1>
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <img alt="A Bad Thing Happened Here..."
                                 src="/opendap/docs/images/superman.jpg"
                                 width="320" height="426"/>
                        </td>
                        <td>
                            <p align="center" class="style1">OUCH!</p>
                            <p align="center">
                                Something Bad Happened On This Server.
                            </p>
                            <p align="center">
                                The error message associated with this error is:
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
                <hr size="1" noshade="noshade"/>
                <h1 align="center">Hyrax Error</h1>
                <p align="center"></p>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
