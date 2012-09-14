package edu.holycross.shot.citecoll 


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

        String tstCollection = "greek"
        String tstId = "urn:cite:paleog:greek.op1"

        def query = svc.getObjectQuery(tstCollection,tstId)

        System.err.println svc.getObjectData(tstCollection,tstId)
        String getObjStr = svc.getObjReply(tstId)
        def root = new XmlParser().parseText(getObjStr)
        System.err.println "Parsed == " + root
        root[citens.reply][citens.citeObject].each {
            System.err.println "\t ${it.'@urn'}"
        }
        System.err.println "TOTAL FINDS = " +   root[citens.reply][citens.citeObject].size()
    }


    @Test void testGetCaps() {
        File caps = new File("testdata/unittests-capabilities.xml")
        CollectionService svc = new CollectionService(caps, apiKey)
        System.err.println svc.getCapsReply()


    }

    @Test void testCollectionSizeReply() {
        assert 1 + 1 == 2
    }


    // TESTS FOR ORDERED COLLECTIONS:

    @Test void testFirstLastReplies() {
        assert 1 + 1 == 2
    }

    @Test void testPrevNextUrns() {
        assert 1 + 1 == 2
    }

    @Test void testPrevNextReplies() {
        assert 1 + 1 == 2
    }


 
}
