package edu.holycross.shot.citequery

import edu.harvard.chs.cite.CiteUrn

import static org.junit.Assert.*
import org.junit.Test

/** Class to test cite collection service class. 
*/
class TestQuery extends GroovyTestCase {
    String apiKey =  System.properties['apiKey']

    groovy.xml.Namespace citens = new groovy.xml.Namespace("http://chs.harvard.edu/xmlns/cite")

    File testCapsFile = new File("testdata/capabilities.xml")
    File statesCaps = new File("testdata/states-test-caps.xml")

    @Test public void testPropertyPassing() {
        /* Test that configuration of your installation includes
        a project apiKey property that is passed in to the test
        environment as a system property. */
        String expectedKey = "AIzaSyBdhFkxJXvSVkv4MWpD83GpErhWVLcCOT8"
        assert apiKey == expectedKey
    }

    @Test public void testConstructor() {
        Query q = new Query(testCapsFile, apiKey)
        assert q
    }


    @Test public void testIdProperty() {
        Query q = new Query(testCapsFile, apiKey)
        CiteUrn urn = new CiteUrn("urn:cite:rage:ptolgeo")
        String expectedPropertyName = "URN"
        assert q.getCanonicalIdProperty(urn) == expectedPropertyName
    }

    @Test public void testClassName() {
        Query q = new Query(testCapsFile, apiKey)
        String expectedGoogleId = "1GC4S75Vz9GI6sYcLXGWTvgrK7-usKB_eU9MGoGQ"
        // two signatures
        assert q.getClassName("ptolgeo") == expectedGoogleId
        CiteUrn urn = new CiteUrn("urn:cite:rage:ptolgeo")
        assert q.getClassName(urn) == expectedGoogleId

    }

    @Test public void testPropListMethods() {
        Query q = new Query(testCapsFile, apiKey)
        String expectedString = "'URN', 'label', 'PleiadesId'"
        assert q.listPropNames("ptolgeo") == expectedString

        def expectedList = ["URN", "label", "PleiadesId"]
        assert q.getPropNameList("ptolgeo") == expectedList
    }

    @Test public void testSimpleQuery() {
        Query q = new Query(testCapsFile, apiKey)
        String collName = "ptolgeo"
        String val = "urn:cite:rage:ptolgeo.pt-ll-1"
        String prop = "URN"
        def resList = q.getResults(collName, prop, val)
        def res = resList[0]
        def propNames = q.getPropNameList(collName)
        assert propNames[0] == prop
        assert res[0] == val

    }


    @Test public void testStates() {
        Query q = new Query(statesCaps, apiKey)
        String coll = "states"
        String val = "Alabama"
        String prop = "State"
        println "${q.getResults(coll,prop,val)}"
    }

    @Test public void testMatchQuery() {
        Query q = new Query(testCapsFile, apiKey)
        String collName = "ptolgeo"
        String val = "urn:cite:rage:ptolgeo.pt-ll-1"
        String prop = "URN"
//        String op ="MATCHES"
        String op ="STARTS WITH"
//        String op = "="
        def resList = q.getResults(collName, prop, val, op)
        assert resList.size() > 1000

        println "CLAS OF RESL: " + resList.getClass() + " of size " + resList.size()
/*
        def res = resList[0]
        def propNames = q.getPropNameList(collName)
        propNames.eachWithIndex { p, i ->
            println "${p} :  ${res[i]}"
        }
*/
    }



}
