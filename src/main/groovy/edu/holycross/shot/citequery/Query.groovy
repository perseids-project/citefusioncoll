package edu.holycross.shot.citequery

import edu.holycross.shot.citecoll.CollectionService
import edu.harvard.chs.cite.CiteUrn

import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder


class Query {
    
    /** End point for Google Tables API v1. */
    static String endPoint = "https://www.googleapis.com/fusiontables/v1/"

    /** Service to query
    */
    CollectionService svc

    /** XML namespace for all CITE Collection replies. */
    static String CITE_NS = "http://chs.harvard.edu/xmlns/cite"


    /** Groovy Namespace object for use in parsing capabilities document. */
    groovy.xml.Namespace citens = new groovy.xml.Namespace(CITE_NS)

    /** Default property name for ordering query results */
    String orderProperty = null

    Query(CollectionService citeService) {
        this.svc = citeService
    }


    //def getResults(String collectionName, String propName, String propValue) {
        //String op = "="
        //return getResults(collectionName, propName, propValue, op)
    //}
    // throw excpetion if collectionName is null
    String listPropNames(String collectionName) 
    throws Exception {
        def config =  svc.citeConfig[collectionName]
        if (!config) {
            throw new Exception("No configuration found for collection ${collectionName}")
        } else {
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
    }



    // MODIFY TO CONSULT INFO ON TYPE IN ORDER TO QUOTE VALUE OR NOT APPROPRIATELY
//    def getResults(String collectionName, String propName, String propValue, String op) {

    def getResults(CiteUrn collectionUrn, Object triples) {
        String propList = listPropNames(collectionUrn.getCollection())
        StringBuffer qBuff = new StringBuffer("SELECT ${propList} FROM ${svc.getClassName(collectionUrn)} ")
        if (triples.size() > 0) {
            qBuff.append (" WHERE ")
        }
        
        String propName
        String propValue
        String op
        //System.err.println "query from triples: " + triples.size()
        triples.eachWithIndex { t, i  ->
            //System.err.println "triple "  + t
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
        //System.err.println "QUERY :  ${qBuff}" 

        URL queryUrl = new URL(endPoint + "query?sql=" +  URLEncoder.encode(qBuff.toString()) + "&key=${svc.apiKey}")

        //System.err.println "QUERY URL :  ${queryUrl}" 

        String rawReply = queryUrl.getText("UTF-8")
        
        JsonSlurper slurp = new JsonSlurper()
        def rows = slurp.parseText(rawReply).rows
    }

}
