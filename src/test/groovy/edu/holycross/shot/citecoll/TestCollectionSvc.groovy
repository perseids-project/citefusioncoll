package edu.holycross.shot.citecoll 


import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestCollectionSvc extends GroovyTestCase {
    
   String apiKey =  System.properties['apiKey']

    @Test void testGetObject() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc

        String tstCollection = "greek"
        String tstId = "urn:cite:paleog:greek.op1"

        def query = svc.getObjectQuery(tstCollection,tstId)

        System.err.println svc.getObjectData(tstCollection,tstId)
        System.err.println svc.getObjReply(tstId)
        
        
    }

    @Test void testFirstLastReplies() {
        assert 1 + 1 == 2
    }


    @Test void testPrevNextUrns() {
        assert 1 + 1 == 2
    }


    @Test void testPrevNextReplies() {
        assert 1 + 1 == 2
    }

    @Test void testCollectionSizeReply() {
        assert 1 + 1 == 2
    }

 
}
