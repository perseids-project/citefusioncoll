package edu.holycross.shot.citecoll 


import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestCollectionSvc extends GroovyTestCase {
    
   String apiKey =  System.properties['apiKey']

    @Test void testSvc() {
        File caps = new File("testdata/unit-tests-caps.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc



        System.err.println "CONIFGURED WITH API KEY " + svc.apiKey
        System.err.println "SVC: " + svc
        System.err.println "configs: " + svc.citeConfig
        // 3 collections configured in caps file
        int expectedSize = 2

        
//        assert svc.citeConfig.keySet().size() == expectedSize

        String tstCollection = "us-states"
        String tstId = "Alabama"
        
        System.err.println "US States - " + svc.citeConfig[tstCollection]

        System.err.println "Query for URN " + tstUrn + " == " 
        System.err.println svc.getObjectQuery(tstCollection,tstUrn)

        System.err.println svc.getObjectData(tstUrn)
        
    }
 
}
