package edu.holycross.shot.citequery

import edu.harvard.chs.cite.CiteUrn

import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder


class Query {
    
    /** End point for Google Tables API v1. */
    static String endPoint = "https://www.googleapis.com/fusiontables/v1/"

    /** Google API key required if not using OAuth for authentication. */
    String apiKey

    /** Capabilities file for this service. */
    File capabilitiesFile

    /** XML namespace for all CITE Collection replies. */
    static String CITE_NS = "http://chs.harvard.edu/xmlns/cite"


    /** Groovy Namespace object for use in parsing capabilities document. */
    groovy.xml.Namespace citens = new groovy.xml.Namespace(CITE_NS)

    /** A map representing the data in the capabilities file */
    LinkedHashMap citeConfig

    /** Default property name for ordering query results */
    String orderProperty = null

    /** Constructor parses capabilities file */
    Query(File capsFile, String googleKey) {
        this.capabilitiesFile = capsFile
        this.apiKey = googleKey
        
        this.citeConfig = configureFromFile(this.capabilitiesFile)
        // Should parse caps file against rng schema, and
        // throw exception if could not parse caps file...
    }



    /*  ***********   METHODS FOR USING CONFIGURATION METADATA *************** */

        /** Creates a map of the configuration data 
    * in an XML capabilities file.
    * @param f The XML capabilities file.
    * @returns A map of configuration data or null
    * if the file could not be parsed.
    */
    LinkedHashMap configureFromFile(File f) {
        def root 
        try {
            root = new XmlParser().parse(f)
        } catch (Exception e) {
            return null
        }


        def configuredCollections = [:]
        root[citens.citeCollection].each { c ->
            def propertyList = []
            c[citens.citeProperty].each { cp ->
                def prop = [:]
                prop['name'] = "${cp.'@name'}"
                prop['label'] = "${cp.'@label'}"
                prop['type'] = "${cp.'@type'}"
                propertyList.add(prop)
            } 

            def seq = ""
            if (c.orderedBy) {
                seq = "${c.orderedBy[0].'@property'}"
            }
            def groupProp = null
            if (c.'@groupProperty') {
                groupProp = c.'@groupProperty'
            }
            def citeExtensions = []
            c[citens.citeExtension].each { ce ->
                citeExtensions << "${ce.'@uri'}"
            }

            def collData = [
                "className" : "${c.'@class'}",
                "canonicalId" : "${c.'@canonicalId'}",
                "groupProperty" : groupProp,
                "nsabbr" : "${c[citens.namespaceMapping][0].'@abbr'}", 
                "nsfull" :"${c[citens.namespaceMapping][0].'@fullValue'}",
                "orderedBy" : seq,
                "citeExtensions" : citeExtensions,
                "properties" : propertyList
            ]

            def coll = ["${c.'@name'}" : collData]
            configuredCollections.putAt("${c.'@name'}",collData)
        }

        return configuredCollections
    }

    String getCanonicalIdProperty(CiteUrn urn) {
        def config =  this.citeConfig[urn.getCollection()]
        return config['canonicalId']
    }

    def getClassName(CiteUrn urn) {
        return getClassName(urn.getCollection())
    }

    def getClassName(String collection) {
        def config =  this.citeConfig[collection]
        return config['className']
    }

    /* get ordered list of prop names */
    def getPropNameList(String collectionName) {
        def config =  this.citeConfig[collectionName]
        def propList = []
        config['properties'].each { p ->
            propList.add(p['name'])
        }
        return propList
    }


    String listPropNames(String collectionName) {
        def config =  this.citeConfig[collectionName]
        StringBuffer pNames = new StringBuffer()
        config['properties'].eachWithIndex { p, i ->
            if (i > 0) {
                pNames.append(", '" + p['name'] + "'")
            } else {
                pNames.append("'" + p['name'] + "'")
            }
        }
        return pNames.toString()
    }

/*
    def getResults(String collectionName, String propName, String propValue) {
        String op = "="
        return getResults(collectionName, propName, propValue, op)
    }
*/


    // MODIFY TO CONSULT INFO ON TYPE IN ORDER TO QUOTE VALUE OR NOT APPROPRIATELY
//    def getResults(String collectionName, String propName, String propValue, String op) {

    def getResults(String collectionName, Object triples) {
        

        String propList = listPropNames(collectionName)
        StringBuffer qBuff = new StringBuffer("SELECT ${propList} FROM ${getClassName(collectionName)} ")
        if (triples.size() > 0) {
            qBuff.append (" WHERE ")
        }
        
        String propName
        String propValue
        String op
        System.err.println "query from triples: " + triples.size()
        triples.eachWithIndex { t, i  ->
            System.err.println "triple "  + t
            propName = t[0]
            propValue = t[1]

            if (t.size() == 2) {
                op = "="
            } else {
                op = t[2]                
            }
            if (i > 0) {
                qBuff.append(" AND " )
            }
            qBuff.append(" '" + propName + "' ${op} '" + propValue + "'")
        }
        if (orderProperty) {
            qBuff.append(" ORDER BY ${orderProperty}")
        }
        System.err.println "QUERY :  ${qBuff}" 

        URL queryUrl = new URL(endPoint + "query?sql=" +  URLEncoder.encode(qBuff.toString()) + "&key=${apiKey}")

        System.err.println "QUERY URL :  ${queryUrl}" 

        String rawReply = queryUrl.getText("UTF-8")
        
        JsonSlurper slurp = new JsonSlurper()
        def rows = slurp.parseText(rawReply).rows
    }

}
