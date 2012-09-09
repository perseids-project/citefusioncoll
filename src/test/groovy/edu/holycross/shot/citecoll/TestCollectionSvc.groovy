package edu.holycross.shot.citecoll 


import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestCollectionSvc extends GroovyTestCase {
    
   String apiKey =  System.properties['apiKey']

    @Test void testSvc() {
//        File caps = new File("testdata/unit-tests-caps.xml")
        File caps = new File("testdata/testedit-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc



        System.err.println "CONIFGURED WITH API KEY " + svc.apiKey
        System.err.println "SVC: " + svc
        System.err.println "configs: " + svc.citeConfig


        // 3 collections configured in caps file
//        int expectedSize = 2
//        assert svc.citeConfig.keySet().size() == expectedSize

//        String tstCollection = "us-states"
        String tstCollection = "greek"
//        String tstId = "Alabama"
       String tstId = "urn:cite:paleog:greek.op1"
        
        System.err.println "collection config  - " + svc.citeConfig[tstCollection]
        def query = svc.getObjectQuery(tstCollection,tstId)
        System.err.println svc.getObjectData(tstCollection,tstId)


        System.err.println svc.getObjReply(tstId)
        
        
    }
 
}
