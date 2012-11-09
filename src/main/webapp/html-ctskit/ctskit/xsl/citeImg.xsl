<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:cite="http://chs.harvard.edu/xmlns/cite"
    version="1.0">

    <xsl:output method="html" omit-xml-declaration="yes"/>
    
    
    <xsl:template match="/">
        <xsl:element name="table">
            <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
            <xsl:element name="caption">
                <xsl:value-of select="//cite:citeObject/@urn"/>
            </xsl:element>
            <xsl:element name="tr">
                <xsl:element name="th">Property</xsl:element>
                <xsl:element name="th">Value</xsl:element>
            </xsl:element>
            <xsl:for-each select="//cite:citeProperty">
                <xsl:element name="tr">
                    <xsl:element name="td">
                        <xsl:value-of select="current()/@label"/>
                </xsl:element>
                    <xsl:element name="td">
                        <xsl:value-of select="current()"/>
                </xsl:element>
                </xsl:element>
            </xsl:for-each>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>