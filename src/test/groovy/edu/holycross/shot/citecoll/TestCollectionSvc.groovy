package edu.holycross.shot.citecoll 

import edu.harvard.chs.cite.CiteUrn

import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestCollectionSvc extends GroovyTestCase {
    
   String apiKey =  System.properties['apiKey']
    groovy.xml.Namespace citens = new groovy.xml.Namespace("http://chs.harvard.edu/xmlns/cite")

    @Test void testGetObject() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc

        String tstId = "urn:cite:op:greek.1"
        String versionId = "urn:cite:op:greek.1.1"

        String getObjStr = svc.getObjReply(tstId)
        String getVersStr = svc.getObjReply(versionId)

        // Version and object request alike should produce only
        // one match.
        int expectedSize = 1
        System.err.println "getObjStr " + getObjStr
        def objRoot = new XmlParser().parseText(getObjStr)
        assert  objRoot[citens.reply][citens.citeObject].size() == expectedSize
        
        def versionRoot = new XmlParser().parseText(getVersStr)
        assert versionRoot[citens.reply][citens.citeObject].size() == expectedSize
        def obj = objRoot[citens.reply][citens.citeObject][0]
        def vers = versionRoot[citens.reply][citens.citeObject][0]
        // Object-level query still gives you full version-level URN:
        assert obj.'@urn' == vers.'@urn'

    }


    @Test void testGetCaps() {
        File caps = new File("testdata/unittests-capabilities.xml")
        CollectionService svc = new CollectionService(caps, apiKey)
        System.err.println svc.getCapsReply()
        // set up a canned Caps file to XML compare to reply ...
    }

    @Test void testCollectionSizeReply() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc


        CiteUrn tstId = new CiteUrn("urn:cite:paleog:greek.op1")
        System.err.println "SIZE:  " + svc.getCollSizeReply(tstId)
        
    }



    @Test testCollectionList() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        System.err.println "CONFIG IS " + svc.citeConfig
    }



    @Test void testPropertyRetrieval() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn tstId = new CiteUrn("urn:cite:paleog:greek.op1")
        String expectedName = "URN"
        assert svc.getCanonicalIdProperty(tstId) == expectedName


        assert svc.isOrdered(tstId) == false
        assert svc.isGrouped(tstId) == false


        CiteUrn venAurn = new CiteUrn("urn:cite:hmt:msA.52v")
        assert svc.isOrdered(venAurn)
        assert svc.getOrderedByProperty(venAurn) == "Sequence"
        
    }

    @Test void testGetValidReff() {
         File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn tstId = new CiteUrn("urn:cite:paleog:greek.op1")
        CiteUrn collUrn = new CiteUrn("urn:cite:paleog:greek")
        svc.getValidReffReply(tstId)
        svc.getValidReffReply(collUrn)
    }
}
