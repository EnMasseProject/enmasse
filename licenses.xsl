<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="utf-8" standalone="no" media-type="text/html" />
    <xsl:param name="version"/>
    <xsl:param name="applicationDisplayName"/>
    <xsl:param name="projectArtifactId"/>
    <xsl:param name="licenseAdviceText"/>
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz '" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ!'" />

    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
                <style>
                    table {
                    border-collapse: collapse;
                    }

                    table, th, td {
                    border: 1px solid navy;
                    }

                    th {
                    text-align: left;
                    background-color: #BCC6CC;

                    }

                    th, td {
                    padding: 2px;
                    text-align: left;
                    }

                    tr:nth-child(even) {
                    background-color: #f2f2f2;
                    }
                </style>
            </head>
            <body>
                <h2><xsl:value-of select="$applicationDisplayName"/><xsl:text>/</xsl:text><xsl:value-of select="$projectArtifactId"/><xsl:text>:</xsl:text><xsl:value-of select="$version"/></h2>
                <xsl:if test="$licenseAdviceText">
                    <p><xsl:value-of select="$licenseAdviceText"/></p>
                </xsl:if>
                <table>
                    <tr>
                        <th>Package Group</th>
                        <th>Package Artifact</th>
                        <th>Package Version</th>
                        <th>Remote Licenses</th>
                        <th>Local Licenses</th>
                    </tr>
                    <xsl:apply-templates select="//dependencies/dependency">
                        <xsl:sort select="concat(groupId, '.', artifactId)"/>
                    </xsl:apply-templates>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="dependency">
        <tr>
            <td>
                <xsl:value-of select="groupId"/>
            </td>
            <td>
                <xsl:value-of select="artifactId"/>
            </td>
            <td>
                <xsl:value-of select="version"/>
            </td>
            <td>
                <xsl:for-each select="licenses/license">
                    <xsl:choose>
                        <xsl:when test="name = 'Public Domain'">
                            <xsl:value-of select="name"/>
                            <br/>
                        </xsl:when>
                        <xsl:otherwise>
                            <a href="{./url}">
                                <xsl:value-of select="name"/>
                            </a>
                            <br/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </td>
            <td>
                <xsl:for-each select="licenses/license">
                    <a href="{file}">
                        <xsl:value-of select="file"/>
                    </a>
                    <br/>
                </xsl:for-each>
            </td>
        </tr>

    </xsl:template>

    <xsl:template name="remap-local-filename">
        <xsl:param name="groupId"/>
        <xsl:param name="artifactId"/>
        <xsl:param name="name"/>
        <xsl:param name="url"/>

        <xsl:variable name="path">
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="value" select="$url" />
                <xsl:with-param name="search" select="'/'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="afterlastdot">
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="value" select="$path" />
                <xsl:with-param name="search" select="'.'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="ext">
            <xsl:choose>
                <xsl:when test="$afterlastdot = 'php'">
                    <xsl:text>html</xsl:text>
                </xsl:when>
                <xsl:when test="string-length($afterlastdot) &gt; 2 and string-length($afterlastdot) &lt;= 4">
                    <xsl:value-of select="$afterlastdot"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>txt</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- There is insufficient information in the XML to reliably predict the name of the license file
             all case.  This is a best-effort approach. -->
        <!-- Java: see org.codehaus.mojo.license.AbstractDownloadLicensesMojo#getLicenseFileName -->
        <xsl:value-of select="translate(translate(concat($groupId, '.', $artifactId, '_', $name, '.', $ext), $uppercase, $lowercase),' ','_')"/>
    </xsl:template>

    <xsl:template name="substring-after-last">
        <xsl:param name="value" />
        <xsl:param name="search"/>

        <xsl:choose>
            <xsl:when test="contains($value, $search)">
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="value" select="substring-after($value, $search)" />
                    <xsl:with-param name="search" select="$search" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
