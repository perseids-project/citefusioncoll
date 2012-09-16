package edu.holycross.shot.citecoll

import edu.harvard.chs.cite.CiteUrn

import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder


// CHANGE ALL SIGNATURES TO PURE URN:  NO COLL + URN combos

/** A class to support working with a CITE Collection, including
*  support for forming replies to the requests of the CITE Collection Service
*  API.
*/
class CollectionService {
    
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


    /** Constructor parses capabilities file */
    CollectionService(File capsFile, String googleKey) {
        this.capabilitiesFile = capsFile
        this.apiKey = googleKey
        
        this.citeConfig = configureFromFile(this.capabilitiesFile)
        // Should parse caps file against rng schema, and
        // throw exception if could not parse caps file...
    }


    
    String getCanonicalIdProperty(CiteUrn urn) {
        def config =  this.citeConfig[urn.getCollection()]
        return config['canonicalId']
    }


    String getOrderedByProperty(CiteUrn urn) {
        def config =  this.citeConfig[urn.getCollection()]
        return config['orderedBy']
    }


    String getClassName(CiteUrn urn) {
        def config =  this.citeConfig[urn.getCollection()]
        return config['className']
    }

    boolean isOrdered(CiteUrn urn) {
        def config =  this.citeConfig[urn.getCollection()]
        return (config['orderedBy']?.size() > 0)
    }


    boolean isGrouped(CiteUrn urn) {
        def config =  this.citeConfig[urn.getCollection()]
        return (config['groupedBy']?.size() > 0)
    }

    
    /** Creates a string with valid XML reply to the
    * CITE Collection GetCapabilities request.
    * @returns The XML reply, as a String.
    */
    String getCapsReply() {
        StringBuffer replyBuff = new StringBuffer("<GetCapabilities xmlns='http://chs.harvard.edu/xmlns/cite'>\n<reply>\n")

        def xmlserialized = this.capabilitiesFile.getText()
        // Delete xml PI:
        // <?xml version="1.0" encoding="UTF-8"?>
        replyBuff.append(xmlserialized.replaceAll("\\<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?\\>","")         )

        replyBuff.append("\n</reply></GetCapabilities>")
        return replyBuff.toString()
    }

    
    /** Creates a string with valid XML reply to the
    * CITE Collection GetCollectionSize request.
    * @param collection CITE identifier for the collection.
    * @returns The XML reply, as a String.
    * @throws Exception if collectionUrnStr is not a valid URN.
    */
    String getCollSizeReply(String collectionUrnStr) 
    throws Exception {
        try {
            CiteUrn urn = new CiteUrn(collectionUrnStr)
            return getCollSizeReply(urn)
        } catch (Exception e) {
            throw e
        }
    }

    /** Creates a string with valid XML reply to the
    * CITE Collection GetCollectionSize request.
    * @param collection CITE identifier for the collection.
    * @returns The XML reply, as a String.
    */
    String getCollSizeReply(CiteUrn urn) {
        def collConf = this.citeConfig[urn.getCollection()]

        StringBuffer replyBuff = new StringBuffer("<GetCollectionSize xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${urn}</urn>\n<collection>${urn.getCollection()}</collection>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n<count>${getCount(urn)}</count>\n</reply>\n</GetCollectionSize>\n")
        
        return replyBuff.toString()
    }


    /** Creates a string with valid XML reply to the
    * CITE Collection GetPrevNextUrn request when the object
    * is identified by a CITE URN.
    * @param requestUrn CITE URN identifying the object.
    * @returns The XML reply, as a String, or null if the collection is not
    * a configured, ordered collection.
    */
    String getPrevNextReply(String requestUrn) {
        CiteUrn citeUrn = new CiteUrn(requestUrn)
        def collConf = this.citeConfig[citeUrn.getCollection()]

        StringBuffer replyBuff = new StringBuffer("<GetPrevNextUrn xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getPrevNextUrn(requestUrn)}\n</reply>\n</GetPrevNextUrn>\n")
        return replyBuff.toString()
    }


    /** Creates a string with valid XML reply to the
    * CITE Collection GetPrev request when the object
    * is identified by a CITE URN.
    * @param requestUrn CITE URN identifying the object.
    * @returns A well-formed XML string representing the previous
    * object in the collection, or null if the collection is not
    * a configured, ordered collection.
    */
    String getPrevReply(String requestUrn) {
        CiteUrn citeUrn = new CiteUrn(requestUrn)
        def collConf = this.citeConfig[citeUrn.getCollection()]

        StringBuffer replyBuff = new StringBuffer("<GetPrev xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getPrevObject(requestUrn)}\n</reply>\n</GetPrev>\n")
        return replyBuff.toString()
    }

    /** Creates a string with valid XML reply to the
    * CITE Collection GetNext request when the object
    * is identified by a CITE URN.
    * @param requestUrn CITE URN identifying the object.
    * @returns A well-formed XML string representing the next
    * object in the collection, or null if the collection is not
    * a configured, ordered collection.
    */
    String getNextReply(String requestUrn) {
        CiteUrn citeUrn = new CiteUrn(requestUrn)
        def collConf = this.citeConfig[citeUrn.getCollection()]

        StringBuffer replyBuff = new StringBuffer("<GetNext xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getNextObject(requestUrn)}\n</reply>\n</GetNext>\n")
        return replyBuff.toString()
    }



    /** Creates an XML serialization of the following object
    * in an ordered collection.  This requires two hits on
    * Fusion:  first to find the sequence number of the 
    * identified object, then a retrieval to 
    * get the object with the next sequence value.
    * Queries check the collection's configuration to account
    * for a grouping property defining a CITE Collection within a single 
    * table.
    * @param requestUrn A CITE URN, as a String, identifying an object
    * in an ordered collection.
    * @returns An XML serialization of the next object in the collection,
    * or null if it is not a configured, ordered collection.
    */
    String getNextObject(String urnStr) {
        CiteUrn citeUrn = new CiteUrn (urnStr)
        def collectionId = citeUrn.getCollection()
        def collConf = this.citeConfig[collectionId]

        if (!collConf['orderedBy']) {
            return null
        }

/*
    
        }
        def orderingProp = propList[orderingPropIndex]['name']
//        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn.getNs()}:${citeUrn.getCollection()}.${citeUrn.getObjectId()}" + "'")

        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn}" + "'")
        if (collConf['groupProperty'] != null) {
            qBuff.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }
*/

    }

    /** Creates a string with valid XML reply to the
    * CITE Collection GetLast request when the collection
    * is identified by a collection identifier.
    * @param collectionId CITE identifier for the collection.
    * @returns A well-formed XML string representing the last
    * object in the collection, or null if the collection is not
    * a configured, ordered collection.
    */
    String getLastReply(String collectionId) {

        def collConf = this.citeConfig[collectionId]
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${collectionId}")

        StringBuffer replyBuff = new StringBuffer("<GetLast xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getLastObject(citeUrn)}</reply>\n</GetLast>\n")
        return replyBuff.toString()
    }


    String getObjectPlusReply(String requestUrnStr) {
        try {
            CiteUrn urn = new CiteUrn(requestUrnStr)
            return getObjectPlusReply(urn)
        } catch (Exception e) {
            throw e
        }
    }

    String getObjectPlusReply(CiteUrn requestUrn) {
        
        StringBuffer replyBuff = new StringBuffer("<GetObjectPlus  xmlns='http://chs.harvard.edu/xmlns/cite'>\n")
        replyBuff.append("<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("<reply>\n")

        System.err.println "GOP:  GET data for " + requestUrn
        replyBuff.append( getObjectData(requestUrn))
        replyBuff.append("\n${getPrevNextUrn(requestUrn)}")
        replyBuff.append("\n</reply>\n</GetObjectPlus>")
        return replyBuff.toString()
    }


    /** Creates a well-formed fragment of a CITE reply
    * giving URNs of preceding and following objects in
    * an ordered collection .
    * @param requestUrn The CITE URN identifying the object.
    * @returns A String of well-formed XML
    */


    String getProximateUrn(CiteUrn urn, String proximity) {
        // select ordering prop for urn
        // then get canonical ID prop of plus one or minus one
    }

    String getProximateObject(CiteUrn urn, String proximity) {
    }

    String getPrevNextUrn(String urnStr) {
        CiteUrn citeUrn = new CiteUrn (urnStr)
        def collectionId = citeUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }

        StringBuffer replyBuff = new StringBuffer("<prevnext>")

        replyBuff.append("</prevnext>")
        return replyBuff.toString()
    }


    /** Creates an XML serialization of the preceding object
    * in an ordered collection.  This requires two hits on
    * Fusion:  first to find the sequence number of the 
    * identified object, then a retrieval to 
    * get the object with the next sequence value.
    * Queries check the collection's configuration to account
    * for a grouping property defining a CITE Collection within a single 
    * table.
    * @param requestUrn A CITE URN, as a String, identifying an object
    * in an ordered collection.
    * @returns An XML serialization of the previous object in the collection,
    * or null if it is not a configured, ordered collection.
    */
    String getPrevObject(String urnStr) {
        CiteUrn citeUrn = new CiteUrn (urnStr)
        def collectionId = citeUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }
        // Get index of orderedBy
        def propList = collConf['properties']
        def orderingPropIndex
        propList.eachWithIndex { p, i ->
            if (p['name'] == collConf['orderedBy']) {
                orderingPropIndex =  i
            }
        }
        def orderingProp = propList[orderingPropIndex]['name']
        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn}" + "'")
        if (collConf['groupProperty'] != null) {
            qBuff.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }
    }

    /** Creates a string with valid XML reply to the
    * CITE Collection GetFirst request when the collection
    * is identified by a collection identifier.
    * @param collectionId CITE identifier for the collection.
    * @returns The XML reply, as a String, or null if the collection 
    * is not a configured, ordered collection.
    */
    String getFirstReply(String collectionId) {
        def collConf = this.citeConfig[collectionId]
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${collectionId}")

        StringBuffer replyBuff = new StringBuffer("<GetFirst xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getFirstObject(citeUrn)}</reply>\n</GetFirst>\n")
        return replyBuff.toString()
    }

    /** Finds the number of objects in a CITE Collection
    * identified by CITE URN.
    * @param requestUrn CITE URN identifying the Collection.
    * @returns A String representation of the number of
    * objects in the requested Collection, or null if the
    * query to Fusion did not succeed.
    */
    String getCount(CiteUrn requestUrn) {
        def collectionId = requestUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        StringBuffer qBuff = new StringBuffer("SELECT COUNT() FROM ${collConf['className']}" )
        if (collConf['groupProperty'] != null) {
            qBuff.append(" WHERE ${collConf['groupProperty']} = '" + collectionId + "'")
        }

        String q = endPoint + "query?sql=" + URLEncoder.encode(qBuff.toString()) + "&key=${apiKey}"
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows
        def cnt
        rows.each { r ->
            // TEST urnVal : only allow correct rows...
            def urnVal
            r.each { p ->
               cnt = p 
            }
        }
        return cnt
    } 
    // end getCount


    /** Creates an XML serialization of the last object
    * in an ordered collection.  This requires two hits on
    * Fusion:  first to find the maximum value in the property
    * configured with sequencing data, then a retrieval to 
    * get the object with the maximum sequence value.
    * Queries check the collection's configuration to account
    * for a grouping property defining a CITE Collection within a single 
    * table.
    * @param requestUrn A CITE URN identifying an ordered collection.
    * @returns An XML serialization of the last object in the collection,
    * or null if it is not a configured, ordered collection.
    */




    String getLastObject(CiteUrn requestUrn) {
        return getExtremeObject(requestUrn, "MAXIMUM")
    }

    /** Creates an XML serialization of the last object
    * in an ordered collection by passing along request to
    * implementation with overloaded signature using a CITE URN.
    * @param urnStr A CITE URN, as a String, identifying an ordered collection.
    * @returns An XML serialization of the last object in the collection,
    * or null if it is not a configured, ordered collection.
    */
    String getLastObject(String urnStr) {
        return getLastObject(new CiteUrn(urnStr))
    }

    /** Creates an XML serialization of the first object
    * in an ordered collection.  This requires two hits on
    * Fusion:  first to find the minimum value in the property
    * configured with sequencing data, then a retrieval to 
    * get the object with the minimum sequence value.
    * Queries check the collection's configuration to account
    * for a grouping property defining a CITE Collection within a single 
    * table.
    * @param requestUrn A CITE URN identifying an ordered collection.
    * @returns An XML serialization of the first object in the collection,
    * or null if it is not a configured, ordered collection.
    */
    String getFirstObject(CiteUrn requestUrn) {
return getExtremeObject(requestUrn, "MINIMUM")
}

String getExtremeObject(CiteUrn requestUrn, String extreme) {
        def collectionId = requestUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }
        
        StringBuffer qBuff = new StringBuffer("SELECT ${extreme}(${collConf['orderedBy']}) FROM ${collConf['className']}" )
        /* ?
        // test for ordering field...
        if (collConf['groupProperty'] != null) {
            qBuff.append(" WHERE ${collConf['groupProperty']} = '" + collectionId + "'")
        }
        */


        String q = endPoint + "query?sql=" + URLEncoder.encode(qBuff.toString()) + "&key=${apiKey}"
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows
        
        def extremeSequence = rows[0][0]

        def props = collConf['properties']
        StringBuffer propNames =  new StringBuffer()
        props.eachWithIndex { p, i ->
            if (i != 0) {
                propNames.append(", ${p['name']}")
            } else {
                propNames.append(p['name'])
            }
        }

        String objQuery = "SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['orderedBy']} = ${extremeSequence}"
        
        
        String fullq = endPoint + "query?sql=" + URLEncoder.encode(objQuery) + "&key=${apiKey}"
        URL fullQueryUrl = new URL(fullq)
        String fullRaw = fullQueryUrl.getText("UTF-8")

        JsonSlurper fullslurp = new JsonSlurper()
        def objectRows = fullslurp.parseText(fullRaw).rows
        System.err.println "Extreme obj (${extreme}): " 
        objectRows[0].each {
            System.err.println it
        }
    }



    /** Creates an XML serialization of the first object
    * in an ordered collection by passing along request to
    * implementation with overloaded signature using a CITE URN.
    * @param urnStr A CITE URN, as a String, identifying an ordered collection.
    * @returns An XML serialization of the first object in the collection,
    * or null if it is not a configured, ordered collection.
    */
    String getFirstObject(String urnStr) {
        return getFirstObject(new CiteUrn(urnStr))
    }

    String getValidReffReply(String requestUrn) {
        CiteUrn citeUrn = new CiteUrn(requestUrn)
        def collConf = this.citeConfig[citeUrn.getCollection()]
        StringBuffer replyBuff = new StringBuffer("<GetValidReff xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getValidReff(citeUrn)}</reply>\n</GetValidReff>\n")
    }



    String getValidReff(CiteUrn urn) {
        def collConf = this.citeConfig[urn.getCollection()]
        StringBuffer query = new StringBuffer("SELECT ${getCanonicalIdProperty(urn)} FROM ${getClassName(urn)}")
        boolean filterRows = false
        if (urn.hasObjectId()) {
            query.append(" WHERE ${getCanonicalIdProperty(urn)} LIKE '"  + urn + "%'")
           filterRows = true
        }
        
        String q = endPoint + "query?sql=" + URLEncoder.encode(query.toString()) + "&key=${apiKey}"

        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows
        rows.each { r ->
            String resultUrnStr = r[0]
            if (filterRows) {
                CiteUrn returnUrn = new CiteUrn(resultUrnStr)
                if (urn.getObjectId() == returnUrn.getObjectId()) {
                    System.err.println resultUrnStr
                }
            } else {
                System.err.println resultUrnStr
            }
        }

    } // GVR


    /** Creates a string with valid XML reply to the
    * CITE Collection GetObject request when the object
    * is identified by a CITE URN.
    * @param URN identifying the object.
    */
    String getObjReply(String requestUrn) {
        CiteUrn citeUrn = new CiteUrn(requestUrn)
        def collConf = this.citeConfig[citeUrn.getCollection()]
        StringBuffer replyBuff = new StringBuffer("<GetObject xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getObjectData(citeUrn)}</reply>\n</GetObject>\n")
    }


    /** Creates a well-formed fragment of a CITE reply
    * representing a single CITE object uniquely identified by CITE URN.
    * @param requestUrn The CITE URN identifying the object.
    * @returns A String of well-formed XML
    */
    String getObjectData(CiteUrn requestUrn) {
        String collectionId = requestUrn.getCollection()
        return getObjectData(collectionId, requestUrn.toString())
    }

    /** Creates a well-formed fragment of a CITE reply
    * representing a single CITE object uniquely identified by 
    * a CITE URN string.
    * @param requestUrn The CITE URN, as a String, identifying the object.
    * @returns A String of well-formed XML
    */
    String getObjectData(String collectionId, String requestUrnStr) {

        def objQuery = getObjectQuery(collectionId, requestUrnStr)
        String q = endPoint + "query?sql=" + URLEncoder.encode(objQuery) + "&key=${apiKey}"
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows 
       def queryProperties = jslurp.parseText(raw).columns
        def collConf = this.citeConfig[collectionId]
        def canonicalId = collConf["canonicalId"]

        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        rows.each { r ->
          
            // TEST urnVal : only allow correct rows...
            def urnVal
            r.eachWithIndex { p, i ->
                if (queryProperties[i] == canonicalId) {
                    urnVal = p
                }
                
            } 
           CiteUrn returnUrn = new CiteUrn(urnVal)
            CiteUrn requestUrn = new CiteUrn(requestUrnStr)
            if (requestUrn.getObjectId() == returnUrn.getObjectId()) {
                xml.citeObject("urn" : urnVal)  { 
                    r.eachWithIndex { prop, i ->
                        if (queryProperties[i] != canonicalId) {
                            collConf["properties"].each { confProp ->
                                if (confProp["name"] == queryProperties[i]) {
                                    citeProperty(name: queryProperties[i], label: confProp["label"], type : confProp["type"], "${prop}")
                                }
                            }
                        }
                    }
                }
            }
        }
        return writer.toString()     
    }


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

    /** Constructs an SQL query string for a given
    * CITE URN.
    * @param urn The CITE URN.
    * @returns An SQL string that can be submitted as the query
    * parameter to Google Fusion, or null if the requested
    * collection is not configured.
    */
    String getObjectQuery(String urn) {
        def citeUrn = new CiteUrn(urn)
        def coll = citeUrn.getCollection()
        def obj = citeUrn.getObjectId()
        return getObjectQuery(coll,obj)
    }


    /** Constructs an SQL query string for a given
    *  pair of collection and object identifier.
    * @param coll The collection identifier.
    * @param obj The object identifier within the collection.
    * @returns An SQL string that can be submitted as the query
    * parameter to Google Fusion, or null if the requested
    * collection is not configured.
    */
    String getObjectQuery(String coll, String urnStr) {

        def collConf = this.citeConfig[coll]
        if (!collConf) { return null }

        CiteUrn urn = new CiteUrn(urnStr)

        // simplify syntax:
        def props = collConf['properties']
        StringBuffer propNames =  new StringBuffer()
        props.eachWithIndex { p, i ->
            if (i != 0) {
                propNames.append(", ${p['name']}")
            } else {
                propNames.append(p['name'])
            }
        }

        if (urn.hasVersion()) {
            return "SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + urnStr +  "'"
        } else {
            String fullQuery =  "SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} LIKE '" + urnStr +  "%'"
        }
        // No OR in Google sql!
        // Have to select for all possible matches, then weed out
        // the false hits at receiving end of query. :-(
    }


}
