<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:jboss="urn:jboss:domain:5.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//jboss:security-realm/jboss:server-identities/jboss:ssl">
        <xsl:copy>
            <jboss:keystore path="certificates.keystore" relative-to="jboss.server.config.dir" alias="server" keystore-password="enmasse" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()[not(name()='keystore' and @path='certificates.keystore')]"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
