package edu.holycross.shot.citecoll 


import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestCollectionSvc extends GroovyTestCase {
    
   String apiKey =  System.properties['apiKey']

    @Test void testSvc() {
        File caps = new File("testdata/testedit-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps)
        assert svc
        //System.err.println svc.getCapsReply()
        String tstUrn = "urn:cite:hmt:DMK.1"
        System.err.println svc.getObjectData(tstUrn)
        
    }
 
}
