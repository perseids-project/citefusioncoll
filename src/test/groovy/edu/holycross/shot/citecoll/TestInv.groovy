package edu.holycross.shot.citecoll 

import edu.harvard.chs.cite.CiteUrn

import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestInv extends GroovyTestCase {
    
    String apiKey =  System.properties['apiKey']
    groovy.xml.Namespace citens = new groovy.xml.Namespace("http://chs.harvard.edu/xmlns/cite")


    File caps = new File("testdata/units2-caps.xml")


    @Test void testGetCaps() {
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc.citeConfig.keySet().size() == 1
    }




    @Test void testPropertyRetrieval() {
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn tstId = new CiteUrn("urn:cite:hmt:codices.msA")
        String expectedName = "codex"
        assert svc.getCanonicalIdProperty(tstId) == expectedName


        assert svc.isOrdered(tstId) == false
        assert svc.isGrouped(tstId) == false

    }


    @Test void testPropMetadata() {
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn collUrn = new CiteUrn("urn:cite:hmt:codices")
        def expectedNames = ["codex", "description"]
        assert svc.getPropNameList(collUrn) == expectedNames

        def typeList = svc.getPropTypeList(collUrn)
        assert typeList[0] == "citeurn"

        def labelList = svc.getPropLabelList(collUrn)
        assert labelList[0] == "Codex URN"

    }

    @Test void testGetValidReff() {
        assert (caps.exists())
        CollectionService svc = new CollectionService(caps, apiKey)
        assert svc
        CiteUrn collUrn = new CiteUrn("urn:cite:hmt:codices")
        // parse and test xml restuls...
    }

}
