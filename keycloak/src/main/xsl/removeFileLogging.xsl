<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:log="urn:jboss:domain:logging:3.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//log:subsystem">
        <xsl:copy>
            <xsl:apply-templates select="node()[name()!='periodic-rotating-file-handler']|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()[not(name()='handler' and @name = 'FILE')]"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

