package edu.holycross.shot.citequery

import edu.harvard.chs.cite.CiteUrn
import edu.holycross.shot.citecoll.CollectionService

import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection querying
*/
class TestQuery extends GroovyTestCase {
    String apiKey =  System.properties['apiKey']

    groovy.xml.Namespace citens = new groovy.xml.Namespace("http://chs.harvard.edu/xmlns/cite")

    File testCapsFile = new File("testdata/capabilities.xml")
    File statesCaps = new File("testdata/states-test-caps.xml")
    File opCaps = new File("testdata/op-caps.xml")
    File normalsCaps = new File("testdata/normalized-caps.xml")

    @Test public void testPropertyPassing() {
        /* Test that configuration of your installation includes
        a project apiKey property that is passed in to the test
        environment as a system property. */
        String expectedKey = "AIzaSyBdhFkxJXvSVkv4MWpD83GpErhWVLcCOT8"
        assert apiKey == expectedKey
    }

    @Test public void testConstructor() {
        CollectionService svc = new CollectionService(testCapsFile, apiKey)
        Query q = new Query(svc)
        assert q
    }


    @Test public void testIdProperty() {
        CollectionService svc = new CollectionService(testCapsFile, apiKey)
        Query q = new Query(svc)
        CiteUrn urn = new CiteUrn("urn:cite:rage:ptolgeo")
        String expectedPropertyName = "URN"
        assert q.svc.getCanonicalIdProperty(urn) == expectedPropertyName
    }

    @Test public void testClassName() {
        CollectionService svc = new CollectionService(testCapsFile, apiKey)
        Query q = new Query(svc)

        String expectedGoogleId = "1GC4S75Vz9GI6sYcLXGWTvgrK7-usKB_eU9MGoGQ"
        CiteUrn urn = new CiteUrn("urn:cite:rage:ptolgeo")
        assert q.svc.getClassName(urn) == expectedGoogleId

    }

    @Test public void testPropListMethods() {
        CollectionService svc = new CollectionService(testCapsFile, apiKey)
        Query q = new Query(svc)

        String expectedString = "'URN', 'label', 'PleiadesId'"
        assert q.listPropNames("ptolgeo") == expectedString

        def expectedList = ["URN", "label", "PleiadesId"]
        assert q.svc.getPropNameList("ptolgeo") == expectedList
    }

    @Test public void testSimpleQuery() {
        CollectionService svc = new CollectionService(testCapsFile, apiKey)
        Query q = new Query(svc)

        String prop = "URN"
        String val = "urn:cite:rage:ptolgeo.pt-ll-1"
        CiteUrn collUrn = new CiteUrn(val)

        def triples = []
        def defaultOp = [prop, val]
        triples.add(defaultOp)

        def resList = q.getResults(collUrn, triples)
        def res = resList[0]
        def propNames = q.svc.getPropNameList(collUrn)
        assert propNames[0] == prop
        assert res[0] == val

    }

/*
    @Test public void testUtf8() {
        File debugFile = new File("testdata/debug.txt")
        OutputStreamWriter dbstream = new OutputStreamWriter(debugFile, "UTF-8")
        File src = new File("testdata/utf8data.txt")
        CollectionService svc = new CollectionService(normalsCaps, apiKey)
        Query q = new Query(svc)

        // File contains a list of values to try on Token property
        CiteUrn collUrn = new CiteUrn("urn:cite:hmt:normalized")
        String prop = "Token"
        String srcText = src.getText("UTF-8")
        srcText.eachLine { l ->
            dbstream.write "Token #${l}#\n"
            def triples = []
            def defaultOp = [prop, l]
            triples.add(defaultOp)
            def resList = q.getResults(collUrn, triples)
            def propNames = q.svc.getPropNameList(collUrn)
            assert propNames[1] == prop
            // I think URL encoder is messing utf8 values.
  //          def res = resList[0]
//            assert res[0] == l
        }
        dbstream.close()
        
    }
*/

    @Test public void testStates() {
        CollectionService svc = new CollectionService(statesCaps, apiKey)
        Query q = new Query(svc)

        CiteUrn collUrn = new CiteUrn("urn:cite:usstates:states")

        String val = "Alabama"
        String prop = "State"
        def explicitOp = [prop, val, "="]
        def triples = []
        triples.add(explicitOp)
        assert q.getResults(collUrn,triples).size() == 1

        // There are 50 states:
        assert q.getResults(collUrn, []).size() == 50
        // 4 have names starting with 'A'
        assert  q.getResults(collUrn,[[ 'State', 'A', ' STARTS WITH ']]).size() == 4
        // But 1 of those is very tiny in population
        assert  q.getResults(collUrn,[[ 'State', 'A', ' STARTS WITH '], ['Population, 2000 census', '2000000', '>']]).size() == 3


    }


    @Test public void testForImgMap() {
        CollectionService svc = new CollectionService(opCaps, apiKey)
        Query q = new Query(svc)

        CiteUrn collUrn = new CiteUrn("urn:cite:op:greek")
        CiteUrn imgUrn = new CiteUrn("urn:cite:fufolioimg:AthPol.131_3v_1_col_13")
        // add test for urn with roi
        String prop = "Description"
        String imgProp = "ImageUrn"

        println "Vis agg = " +    q.getVisualAggregate(collUrn,imgUrn,prop,imgProp)

    }


//    @Test public void testMatchQuery() {

/*
        Query q = new Query(testCapsFile, apiKey)
        String collName = "ptolgeo"
        String val = "urn:cite:rage:ptolgeo.pt-ll-2"
        String prop = "URN"
//        String op ="MATCHES"
        String op ="STARTS WITH"
//        String op = "="

        def tList = []
        def triple = [prop, val, op]
        tList.add(triple)
        def resList = q.getResults(collName, tList)
        assert resList.size() > 10

        //println "CLAS OF RESL: " + resList.getClass() + " of size " + resList.size()*/
/*
        def res = resList[0]
        def propNames = q.getPropNameList(collName)
        propNames.eachWithIndex { p, i ->
            println "${p} :  ${res[i]}"
        }
*/
//    }


}
