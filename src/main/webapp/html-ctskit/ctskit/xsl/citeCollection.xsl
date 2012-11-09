<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:cite="http://chs.harvard.edu/xmlns/cite"
    version="1.0">
    <xsl:variable name="ImageServiceGIP">http://amphoreus.hpcc.uh.edu/tomcat/chsimg/Img?request=GetImagePlus&amp;xslt=gip.xsl&amp;urn=</xsl:variable>
    <xsl:variable name="ImageServiceThumb">http://amphoreus.hpcc.uh.edu/tomcat/chsimg/Img?request=GetBinaryImage&amp;w=200&amp;urn=</xsl:variable>
    
    <xsl:output method="html" omit-xml-declaration="yes"/>
    
    
    <xsl:template match="/">
        <xsl:element name="table">
            <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
            <xsl:element name="caption">
                <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
                <xsl:value-of select="//cite:citeObject/@urn"/>
            </xsl:element>
            <xsl:element name="tr">
                <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
                <xsl:element name="th">Property</xsl:element>
                <xsl:element name="th">Value</xsl:element>
            </xsl:element>
            <xsl:for-each select="//cite:citeProperty">
                <xsl:element name="tr">
                    <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
                    <xsl:element name="td">
                        <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
                        <xsl:value-of select="current()/@label"/>
                </xsl:element>
                    <xsl:element name="td">
                        <xsl:attribute name="class">citeCollectionTable</xsl:attribute>
                        
                        <xsl:choose>
                            <xsl:when test="@type = 'string'">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@type = 'markdown'">
                                <span class="md"><xsl:value-of select="."/></span>
                            </xsl:when>
                            <xsl:when test="@type= 'citeurn'">
                                <xsl:element name="a">
                                    <xsl:attribute name="href">api?req=GetObject&amp;urn=<xsl:value-of select="."/></xsl:attribute>
                                    <xsl:apply-templates/>
                                </xsl:element>
                            </xsl:when>
                            <xsl:when test="@type= 'citeimg'">
                                <xsl:if test="string-length(.) &gt; 6">
                                    <xsl:element name="a">
                                        <xsl:attribute name="href"><xsl:value-of select="$ImageServiceGIP"/><xsl:value-of select="."/></xsl:attribute>
                                        <xsl:element name="img">
                                            <xsl:attribute name="src"><xsl:value-of select="$ImageServiceThumb"/><xsl:value-of select="."/></xsl:attribute>
                                        </xsl:element>
                                    </xsl:element>
                                </xsl:if>
                            </xsl:when>
                            <xsl:when test="@type= 'md'">
                                <xsl:value-of select="."/> (md)
                            </xsl:when>
                            
                            
                        </xsl:choose>
                        
                </xsl:element>
                </xsl:element>
            </xsl:for-each>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>