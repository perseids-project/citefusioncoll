package edu.holycross.shot.citecoll 

import edu.harvard.chs.cite.CiteUrn

import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestOrderedCollection extends GroovyTestCase {
    
   String apiKey =  System.properties['apiKey']
    groovy.xml.Namespace citens = new groovy.xml.Namespace("http://chs.harvard.edu/xmlns/cite")

    // TESTS FOR ORDERED COLLECTIONS:

    @Test void testFirstLastReplies() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn collUrn = new CiteUrn("urn:cite:paleog:greek")
        // returns null on unordered collection

        assert svc.getFirstObject(collUrn) == null
        CiteUrn orderedUrn = new CiteUrn("urn:cite:hmt:msA")
        svc.getFirstObject(orderedUrn) 
        svc.getLastObject(orderedUrn) 
    }

    @Test void testPrevNextUrns() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc

        CiteUrn orderedUrn = new CiteUrn("urn:cite:hmt:msA.1r")
        System.err.println "PREV-NEXT: " + svc.getPrevNextReply(orderedUrn)

        System.err.println "CONFIG:  " + svc.citeConfig
        svc.citeConfig.keySet().each  {  k ->
            System.err.println "\tkey: " + k
        }

    }

    @Test void testPrevNextReplies() {
        File caps = new File("testdata/unittests-capabilities.xml")
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn orderedUrn = new CiteUrn("urn:cite:hmt:msA.1r")
        System.err.println "NEX T OBJ => " + svc.getNextObject(orderedUrn)
    }


 
}
