<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0" xmlns:cite="http://chs.harvard.edu/xmlns/cite">
    
    <xsl:import href="header.xsl"/>
    <xsl:output encoding="UTF-8" indent="no" method="html"/>
    
    
    <xsl:template match="/">
    
        <html>
            <head>
                <meta charset="utf-8"></meta>
                <link rel="stylesheet" href="css/normalize.css"></link>
                <link rel="stylesheet" href="css/simple.css"></link>
                <link rel="stylesheet" href="css/tei.css"></link>
                <link rel="stylesheet" href="css/citeCollection.css"></link>
                <title>CITE Collection Service · Get Capabilities</title>
            </head>
            <body>
                <header>
                    <xsl:call-template name="header"/>
                </header>
                <article>
                    <xsl:apply-templates/>
                </article>
                
                <footer>
                    <xsl:call-template name="footer"/>
                </footer>
                
            </body>
            
        </html>
    
    
    </xsl:template>
    
<xsl:template match="cite:request">
        <h2>Requested Collection</h2>
        <p><xsl:apply-templates select="./cite:urn"/></p>
</xsl:template>
    
<xsl:template match="cite:reply">
        <h2>Valid Citations in this Collection</h2>
        <ul>
        <xsl:apply-templates/>
        </ul>
    </xsl:template>
    
    <xsl:template match="cite:reply/cite:urn">
        <li>
            <xsl:element name="a">
                <xsl:attribute name="href">api?req=GetObject&amp;urn=<xsl:value-of select="."/></xsl:attribute>
            <xsl:apply-templates/>
            </xsl:element>
        
        </li>
    </xsl:template>
    
    
<!--    <xsl:template match="@*|node()" priority="-1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>-->
</xsl:stylesheet>