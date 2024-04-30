<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:jxb="https://jakarta.ee/xml/ns/jaxb"
				xmlns:if="http://www.kscs.com/util/jaxb/interface"
				xmlns="http://www.kscs.com/util/jaxb/interface"
				xmlns:kscs="http://www.kscs.com/util/jaxb/bindings"
		exclude-result-prefixes="jxb kscs">

	<xsl:output method="xml" indent="yes"/>

	<xsl:template match="/">
		<if:interfaces>
			<xsl:apply-templates select="//kscs:interface"/>
		</if:interfaces>
	</xsl:template>

	<xsl:template match="kscs:interface">
		<xsl:variable name="scd" select="parent::jxb:bindings/@scd"/>
		<xsl:variable name="type" select="substring-before($scd,'::')"/>
		<xsl:variable name="prefix" select="substring-before(substring-after($scd, '::'), ':')"/>
		<if:interface name="{@ref}">
			<if:schemaComponent namespace="{namespace::*[local-name() = $prefix]}" type="{$type}" name="{substring-after($scd, concat($prefix, ':'))}"/>
		</if:interface>
	</xsl:template>
</xsl:stylesheet>
