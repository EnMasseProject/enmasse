<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:keycloak="urn:jboss:domain:keycloak-server:1.1">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//keycloak:subsystem">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <spi name="amqp">
                <provider name="amqp-server" enabled="true">
                    <properties>
                        <property name="enableTls" value="true"/>
                        <property name="enableNonTls" value="false"/>
                        <property name="pemKeyPath" value="/opt/enmasse/cert/tls.key" />
                        <property name="pemCertificatePath" value="/opt/enmasse/cert/tls.crt" />
                        <property name="tlsHost" value="0.0.0.0"/>
                        <property name="tlsPort" value="5671"/>
                        <property name="defaultDomain" value="enmasse"/>
                    </properties>
                </provider>
            </spi>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
