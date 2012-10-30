package edu.holycross.shot.citecoll

import edu.harvard.chs.cite.CiteUrn

import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder


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





    /*  ***********   METHODS GETTING SERVICE AND COLLECTION METADATA ******* */

    
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

    String getValidReffReply(String requestUrnStr) {
        try {
            CiteUrn citeUrn = new CiteUrn(requestUrnStr)
            return getValidReffReply(citeUrn)
        } catch (Exception e) {
            throw e
        }
    }

    String getValidReffReply(CiteUrn urn) {
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


        StringBuffer replyBuffer = new StringBuffer("<GetValidReff xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${urn}</urn>\n</request>\n<reply>\n")
        rows.each { r ->
            String resultUrnStr = r[0]
            if (filterRows) {
                CiteUrn returnUrn = new CiteUrn(resultUrnStr)
                if (urn.getObjectId() == returnUrn.getObjectId()) {
                    replyBuffer.append("<urn>${resultUrnStr}</urn>")
                }
            } else {
                replyBuffer.append("<urn>${resultUrnStr}</urn>")
            }
        }
        replyBuffer.append("</reply>\n</GetValidReff>")
        return replyBuffer.toString()
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


    /* utils */

    /* get ordered list of prop names */
    def getPropNameList(CiteUrn collectionUrn) {
        return getPropNameList(collectionUrn.getCollection())
    }
    def getPropNameList(String collectionName) {
        def config =  this.citeConfig[collectionName]
        def propList = []
        config['properties'].each { p ->
            propList.add(p['name'])
        }
        return propList
    }


    String getValue(String collectionName, String prop, Object rowList) {

        getPropNameList(collectionName).eachWithIndex { p, i ->
            println "${i}:  ${p}"
        }

    }



    /*  ***********   RETRIEVAL METHODS ************************************** */

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

        replyBuff.append( getObjectData(requestUrn))
        replyBuff.append("\n${getPrevNextUrn(requestUrn)}")
        replyBuff.append("\n</reply>\n</GetObjectPlus>")
        return replyBuff.toString()
    }

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
    * representing a single CITE object uniquely identified by 
    * a CITE URN string.
    * @param requestUrn The CITE URN, as a String, identifying the object.
    * @returns A String of well-formed XML
    */

    String getObjectData(String requestUrnStr) {
        try {
            CiteUrn urn = new CiteUrn(requestUrnStr)
            return getObjectData(urn)
        } catch (Exception e) {
            throw e
        }
    }


    String getObjectData(CiteUrn urn) {
        def objQuery = getObjectQuery(urn)
        def collConf = this.citeConfig[urn.getCollection()]
        String q = endPoint + "query?sql=" + URLEncoder.encode(objQuery) + "&key=${apiKey}"
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows 
        def canonicalId = getCanonicalIdProperty(urn)
        def queryProperties = jslurp.parseText(raw).columns
        

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

            if (urn.getObjectId() == returnUrn.getObjectId()) {
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


    String getObjectQuery(CiteUrn urn) {
        def collConf = this.citeConfig[urn.getCollection()]
        if (!collConf) { return null }

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
            return "SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + urn.toString() +  "'"
        } else {
            String fullQuery =  "SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} LIKE '" + urn.toString() +  "%'"
        }
        // No OR in Google sql!
        // Have to select for all possible matches, then weed out
        // the false hits at receiving end of query. :-(
    }


    /** Constructs an SQL query string for a given
    *  pair of collection and object identifier.
    * @param coll The collection identifier.
    * @param obj The object identifier within the collection.
    * @returns An SQL string that can be submitted as the query
    * parameter to Google Fusion, or null if the requested
    * collection is not configured.
    */





    String getObjectQuery(String urnStr) {
        try {
            CiteUrn urn = new CiteUrn(urnStr)
            return getObjectQuery(urn)

        } catch (Exception e) {
            throw e
        }
    }



    /*  ***********  METHODS FOR NAVIGATING ORDERED COLLECTIONS  ************* */

    /** Creates a string with valid XML reply to the
    * CITE Collection GetPrevNextUrn request when the object
    * is identified by a CITE URN.
    * @param requestUrn CITE URN identifying the object.
    * @returns The XML reply, as a String, or null if the collection is not
    * a configured, ordered collection.
    */


    String getPrevNextReply(String requestUrnStr) {
        try {
            CiteUrn citeUrn = new CiteUrn(requestUrnStr)
            return getPrevNextReply(citeUrn)
        } catch (Exception e) {
            throw e
        }
    }


    String getPrevNextReply(CiteUrn requestUrn) {
        def collConf = this.citeConfig[requestUrn.getCollection()]

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
        try {
            CiteUrn urn = new CiteUrn(requestUrn)
            return getPrevReply(urn)
        } catch (Exception e) {
            throw e
        }
    }



    String getPrevReply(CiteUrn requestUrn) {
        def collConf = this.citeConfig[requestUrn.getCollection()]

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
        try {
            CiteUrn urn = new CiteUrn(urnStr)
            return getNextObject(urn)
        } catch (Exception e ) {
            throw e
        }
    }

    String getNextObject(CiteUrn urn) {
        if (! isOrdered(urn)) {
            return null
        }
        return getProximateObject(urn, "next")
    }

    String getPrevObject(String urnStr) {
        try {
            CiteUrn urn = new CiteUrn(urnStr)
            return getPrevObject(urn)
        } catch (Exception e ) {
            throw e
        }
    }

    String getPrevObject(CiteUrn urn) {
        if (! isOrdered(urn)) {
            return null
        }
        return getProximateObject(urn, "prev")
    }




    /** Creates a string with valid XML reply to the
    * CITE Collection GetLast request when the collection
    * is identified by a collection identifier.
    * @param collectionId CITE identifier for the collection.
    * @returns A well-formed XML string representing the last
    * object in the collection, or null if the collection is not
    * a configured, ordered collection.
    */
    String getLastReply(String urnStr) {
        try {
            CiteUrn urn = new CiteUrn (urnStr)
            return getLastReply(urn)
        } catch (Exception e) {
            throw e
        }
    }

    String getLastReply(CiteUrn urn) {
        def collConf = this.citeConfig[urn.getCollection()]
        String collectionId = urn.getCollection()
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${collectionId}")

        StringBuffer replyBuff = new StringBuffer("<GetLast xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n</request>\n")
//        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
//        replyBuff.append("\n${getLastObject(citeUrn)}</reply>\n</GetLast>\n")
        replyBuff.append("\n${getLastObject(citeUrn)}\n</GetLast>\n")
        return replyBuff.toString()
    }


    String getProximateObject(CiteUrn urn, String prevnext) {
        StringBuffer returnData = new StringBuffer()
        String canonicalProp = getCanonicalIdProperty(urn)
        String orderingProp = getOrderedByProperty(urn)
        String className = getClassName(urn)

        StringBuffer qBuff = new StringBuffer("SELECT ${canonicalProp}, ${orderingProp} FROM ${className} WHERE ${canonicalProp} = '" + "${urn}" + "' ORDER BY ${orderingProp}")        

        String q = endPoint + "query?sql=" + URLEncoder.encode(qBuff.toString()) + "&key=${apiKey}"
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows
        Integer seq
        rows.each { r ->
            try {
                String seqStr = r[1]
                seq = seqStr.toInteger()
                Integer proxVal
                String tagName
                if (prevnext.toLowerCase() == "next") {
                    proxVal = seq + 1
                } else if (prevnext.toLowerCase() == "prev") {
                    proxVal = seq - 1
                }

                
                StringBuffer proxBuff = new StringBuffer("SELECT ${canonicalProp}, ${orderingProp} FROM ${className} WHERE ${orderingProp} = ${proxVal}")        
                String proxQuery = endPoint + "query?sql=" + URLEncoder.encode(proxBuff.toString()) + "&key=${apiKey}"
                URL proxUrl = new URL(proxQuery)
                String proxRaw = proxUrl.getText("UTF-8")

                JsonSlurper proxSlurp = new JsonSlurper()
                def proxRows = proxSlurp.parseText(proxRaw).rows

                proxRows.each { proxRow ->
                    // getObjectData and add to return data...
                    //
                    returnData.append(getObjectData(proxRow[0]))
                }
            } catch (Exception e) {
                throw e
            }
        }

        return returnData.toString()
    }

    /** Creates a well-formed fragment of a CITE reply
    * giving URNs of preceding and following objects in
    * an ordered collection .
    * @param requestUrn The CITE URN identifying the object.
    * @returns A String of well-formed XML
    */


    String getProximateUrn(CiteUrn urn, String prevnext) {
        StringBuffer returnList = new StringBuffer()

        // select ordering prop for urn
        // then get canonical ID prop of plus one or minus one
        String canonicalProp = getCanonicalIdProperty(urn)
        String orderingProp = getOrderedByProperty(urn)
        String className = getClassName(urn)

        StringBuffer qBuff = new StringBuffer("SELECT ${canonicalProp}, ${orderingProp} FROM ${className} WHERE ${canonicalProp} = '" + "${urn}" + "' ORDER BY ${orderingProp}")        

        String q = endPoint + "query?sql=" + URLEncoder.encode(qBuff.toString()) + "&key=${apiKey}"
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows
        Integer seq
        rows.each { r ->
            try {
                String seqStr = r[1]
                seq = seqStr.toInteger()
                Integer proxVal
                String tagName
                if (prevnext.toLowerCase() == "next") {
                    proxVal = seq + 1
                    tagName = "next"
                } else if (prevnext.toLowerCase() == "prev") {
                    proxVal = seq - 1
                    tagName = "prev"
                }
                StringBuffer proxBuff = new StringBuffer("SELECT ${canonicalProp}, ${orderingProp} FROM ${className} WHERE ${orderingProp} = ${proxVal}")        
                String proxQuery = endPoint + "query?sql=" + URLEncoder.encode(proxBuff.toString()) + "&key=${apiKey}"
                URL proxUrl = new URL(proxQuery)
                String proxRaw = proxUrl.getText("UTF-8")

                JsonSlurper proxSlurp = new JsonSlurper()
                def proxRows = proxSlurp.parseText(proxRaw).rows

                proxRows.each { proxRow ->
                    returnList.append( "<${tagName}>${proxRow[0]}</${tagName}>\n")
                }
            } catch (Exception e) {
                throw e
            }
        }
        return returnList.toString()
    }


    String getPrevNextUrn(String urnStr) {
        CiteUrn citeUrn = new CiteUrn (urnStr)
        return getPrevNextUrn(citeUrn)
    }

    String getPrevNextUrn(CiteUrn citeUrn) {
        if (! isOrdered(citeUrn)) {
            return null
        }

        StringBuffer replyBuff = new StringBuffer("<prevnext>\n")
        String prevList = getProximateUrn(citeUrn, 'prev') 
        if (prevList) {
            replyBuff.append(prevList)
        } else {
            replyBuff.append("<prev/>\n")
        }
        String nextList = getProximateUrn(citeUrn, 'next')
        if (nextList) {
            replyBuff.append(nextList)
        } else {
            replyBuff.append("<next/>\n")
        }
        replyBuff.append("</prevnext>\n")
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







    String getFirstReply(String urnStr) {
        try {
            CiteUrn urn = new CiteUrn(urnStr)
            return getFirstReply(urn)
        } catch (Exception e) {
            throw e
        }
    }

    String getFirstReply(CiteUrn urn) {
        def collConf = this.citeConfig[urn.getCollection()]
        String collectionId = urn.getCollection()
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${urn.getCollection()}")

        StringBuffer replyBuff = new StringBuffer("<GetFirst xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n</request>\n")
//        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
//        replyBuff.append("\n${getFirstObject(citeUrn)}</reply>\n</GetFirst>\n")
        replyBuff.append("\n${getFirstObject(citeUrn)}\n</GetFirst>\n")
        return replyBuff.toString()
    }


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
        def canonicalId = getCanonicalIdProperty(requestUrn)
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

        def queryProperties = jslurp.parseText(fullRaw).columns
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        xml.reply {
            objectRows.each { r ->
                // Find canonicalId value for object:
                String urnVal =""
                r.eachWithIndex { p, i ->
                    if (queryProperties[i] == canonicalId) {
                        urnVal = p
                    }
                } 
                citeObject("urn" : urnVal)  { 
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
/*        System.err.println "Extreme obj (${extreme}): " 
        objectRows[0].each {
            System.err.println it
            // format ...
        } */
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







}
