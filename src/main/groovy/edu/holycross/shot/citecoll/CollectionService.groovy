package edu.holycross.shot.citecoll

import groovy.json.JsonSlurper


import edu.harvard.chs.cite.CiteUrn
import groovy.xml.MarkupBuilder



// REMOVE TEHESE DEPENDENCIES!
import java.util.regex.Pattern
import java.util.regex.MatchResult


// REMOVE ALL THESE DEPENDENCIES!
// Google data APIs:
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.util.ContentType;

class CollectionService {

    boolean debug = false


    // Constants:

    static String endPoint = "https://www.googleapis.com/fusiontables/v1/"

    /** Developer's required Google API key if not using OAuth for authentication */
    String apiKey
    /** Capabilities file for this service */
    File capabilitiesFile


    /** Identifier to Google of this application 
    static String CLIENT_APP = "shot.holycross.edu-fusioncoll-0.2"
*/
    /** XML namespace for all CITE Collection replies */
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
        System.err.println "CONFIG: " + this.citeConfig 
        // throw exception if could not parse caps file...
    }


    /** Creates a string with valid XML reply to the
    * CITE Collection GetCapabilities request.
    * @returns The XML reply, as a String.
    */
    String getCapsReply() {
        return "NEED TO WRAP capabilities file IN PROPER MARKUP"
        //return this.capabilitiesFile.getText()

    }

    /** Creates a string with valid XML reply to the
    * CITE Collection GetCollectionSize request.
    * @param collection CITE identifier for the collection.
    * @returns The XML reply, as a String.
    */
    String getSizeReply(String collectionId) {
        def collConf = this.citeConfig[collectionId]
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${collectionId}")

        StringBuffer replyBuff = new StringBuffer("<GetCollectionSize xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n</request>\n")
        replyBuff.append("\n<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n<size>${getSize(citeUrn)}</size>\n</reply>\n</GetCollectionSize>\n")
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



    // INTERNAL METHOD:

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
        // Get index of orderedBy
        def propList = collConf['properties']
        def orderingPropIndex
        propList.eachWithIndex { p, i ->
            if (p['name'] == collConf['orderedBy']) {
                orderingPropIndex =  i
            }
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


        // Extract collection id...
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
    String getPrevNextUrn(String urnStr) {
        CiteUrn citeUrn = new CiteUrn (urnStr)
        def collectionId = citeUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }

        StringBuffer replyBuff = new StringBuffer("<prevnext>")

        // Get index of orderedBy property
        def propList = collConf['properties']
        def orderingPropIndex
        propList.eachWithIndex { p, i ->
            if (p['name'] == collConf['orderedBy']) {
                orderingPropIndex =  i
            }
        }
        def orderingProp = propList[orderingPropIndex]['name']
//        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn.getNs()}:${citeUrn.getCollection()}.${citeUrn.getObjectId()}" + "'")

        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn}" + "'")
        if (collConf['groupProperty'] != null) {
            qBuff.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }


        def queryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(qBuff.toString(), "UTF-8"));
        GDataRequest grequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, queryUrl, ContentType.TEXT_PLAIN)
        grequest.execute()
        def replyLines= grequest.requestUrl.getText('UTF-8').readLines()
        
        def prevSeq =  Integer.parseInt(replyLines[1],10) - 1
        def nextSeq =  Integer.parseInt(replyLines[1],10) + 1


        StringBuffer prevQuery = new StringBuffer("SELECT ${collConf['canonicalId']} FROM ${collConf['className']} WHERE ${orderingProp} = ${prevSeq}")
        if (collConf['groupProperty'] != null) {
            prevQuery.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }
        StringBuffer nextQuery = new StringBuffer("SELECT ${collConf['canonicalId']} FROM ${collConf['className']} WHERE ${orderingProp} = ${nextSeq}")
        if (collConf['groupProperty'] != null) {
            nextQuery.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }


        def prevQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(prevQuery.toString(), "UTF-8"));
        def nextQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(nextQuery.toString(), "UTF-8"));


        GDataRequest prevrequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, prevQueryUrl, ContentType.TEXT_PLAIN)
        prevrequest.execute()
        def prevReplyLines= prevrequest.requestUrl.getText('UTF-8').readLines()

        if (prevReplyLines.size() != 2) {
            replyBuff.append("<prev/>")
        } else {
            replyBuff.append("<prev>urn:cite:${prevReplyLines[1]}</prev>")
        }

        GDataRequest nextrequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, nextQueryUrl, ContentType.TEXT_PLAIN)
        nextrequest.execute()
        def nextReplyLines= nextrequest.requestUrl.getText('UTF-8').readLines()

        if (nextReplyLines.size() != 2) {
            replyBuff.append("<next/>")
        } else {
            replyBuff.append("<next>urn:cite:${nextReplyLines[1]}</next>")
        }
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
//        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn.getNs()}:${citeUrn.getCollection()}.${citeUrn.getObjectId()}" + "'")
        StringBuffer qBuff = new StringBuffer("SELECT ${orderingProp} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + "${citeUrn}" + "'")
        if (collConf['groupProperty'] != null) {
            qBuff.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }


        def queryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(qBuff.toString(), "UTF-8"));
        GDataRequest grequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, queryUrl, ContentType.TEXT_PLAIN)
        grequest.execute()
        def replyLines= grequest.requestUrl.getText('UTF-8').readLines()
        // Look for 1 more than the this object's sequence number.
        // Test for end of line!
        def seqNum = Integer.parseInt(replyLines[1],10) - 1

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

        StringBuffer objQuery = new StringBuffer("SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${orderingProp} = ${seqNum}")
        if (collConf['groupProperty'] != null)  {
            objQuery.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }


        def objQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(objQuery.toString(), "UTF-8"));
        GDataRequest objrequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, objQueryUrl, ContentType.TEXT_PLAIN)
        objrequest.execute()
        def objReplyLines= objrequest.requestUrl.getText('UTF-8').readLines()

        if (objReplyLines.size() != 2) {
            return ""
        }
        return rowToXml(objReplyLines[1],citeUrn.toString())
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
    String getSize(CiteUrn requestUrn) {
        def collectionId = requestUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        StringBuffer qBuff = new StringBuffer("SELECT COUNT() FROM ${collConf['className']}" )
        if (collConf['groupProperty'] != null) {
            qBuff.append(" WHERE ${collConf['groupProperty']} = '" + collectionId + "'")
        }


        if (this.debug) {
        System.err.println "GETSIZE:  " + qBuff.toString()
        }

        def queryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(qBuff.toString(), "UTF-8"));
        GDataRequest grequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, queryUrl, ContentType.TEXT_PLAIN)
        grequest.execute()
        def replyLines= grequest.requestUrl.getText('UTF-8').readLines()
        if (replyLines.size() != 2) {
            return null
        } else {
            return(replyLines[1])
        }
    } 
    // end getSize


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
        def collectionId = requestUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }
        
        StringBuffer qBuff = new StringBuffer("SELECT MAXIMUM(${collConf['orderedBy']}) FROM ${collConf['className']}" )
        if (collConf['groupProperty'] != null) {
            qBuff.append(" WHERE ${collConf['groupProperty']} = '" + collectionId + "'")
        }

        def maxQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(qBuff.toString(), "UTF-8"));
        GDataRequest grequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, maxQueryUrl, ContentType.TEXT_PLAIN)
        grequest.execute()
        def replyLines= grequest.requestUrl.getText('UTF-8').readLines()
        def maxVal = replyLines[1]

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

        StringBuffer objQuery = new StringBuffer("SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['orderedBy']} = ${maxVal}")
        if (collConf['groupProperty'] != null) {
            objQuery.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }

        def objQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(objQuery.toString(), "UTF-8"));
        GDataRequest objrequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, objQueryUrl, ContentType.TEXT_PLAIN)
        objrequest.execute()
        def objReplyLines= objrequest.requestUrl.getText('UTF-8').readLines()

        return rowToXml(objReplyLines[1],requestUrn.toString())

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
        def collectionId = requestUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }
        
        // test for ordering field...
        StringBuffer qBuff = new StringBuffer("SELECT MINIMUM(${collConf['orderedBy']}) FROM ${collConf['className']}" )
        if (collConf['groupProperty'] != null) {
            qBuff.append(" WHERE ${collConf['groupProperty']} = '" + collectionId + "'")
        }

        def minQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(qBuff.toString(), "UTF-8"));
        GDataRequest grequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, minQueryUrl, ContentType.TEXT_PLAIN)
        grequest.execute()
        def replyLines= grequest.requestUrl.getText('UTF-8').readLines()
        def minVal = replyLines[1]

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

        StringBuffer objQuery = new StringBuffer("SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['orderedBy']} = ${minVal}")
        if (collConf['groupProperty'] != null) {
            objQuery.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }

        def objQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(objQuery.toString(), "UTF-8"));
        GDataRequest objrequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, objQueryUrl, ContentType.TEXT_PLAIN)
        objrequest.execute()
        def objReplyLines= objrequest.requestUrl.getText('UTF-8').readLines()

        return rowToXml(objReplyLines[1],requestUrn.toString())
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
    String getObjectData(String collectionId, String requestUrn) {

        def objQuery = getObjectQuery(collectionId, requestUrn)
        System.err.println "OBJ QUERY FOR ${collectionId}, ${requestUrn}= " + objQuery

        String q = endPoint + "query?sql=" + URLEncoder.encode(objQuery) + "&key=${apiKey}"

        System.err.println "QUERY: " + q
        URL queryUrl = new URL(q)
        String raw = queryUrl.getText("UTF-8")

        JsonSlurper jslurp = new JsonSlurper()
        def rows = jslurp.parseText(raw).rows
        def queryProperties = jslurp.parseText(raw).columns
        def collConf = this.citeConfig[collectionId]


        System.err.println "for cols " + queryProperties
        System.err.println "Configured as " + collConf
        def canonicalId = collConf["canonicalId"]

        

        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)



        rows.each { r ->
            def urnVal
            r.eachWithIndex { p, i ->
                if (queryProperties[i] == canonicalId) {
                    urnVal = p
                }
                
            }
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
        return writer.toString()     
    }


    /** Creates a well-formed fragment of XML in the
    * CITE namespace representing a single row of data
    * corresponding to a given URN.
    * @param row A row of data in csv format as returned by Fusion.
    * @param requestUrn The URN identifying this object.
    * @return A String with an XML serialization of the data.
    */
    String rowToXml(String row, String requestUrn) {
        return ""
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
    String getObjectQuery(String coll, String urn) {
        def collConf = this.citeConfig[coll]
        if (!collConf) { return null }

        def trimUrn = urn //"urn:cite:${collConf['nsabbr']}:${coll}.${obj}"
        System.err.println "URN KEY: " + trimUrn
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

        return "SELECT ${propNames.toString()} FROM ${collConf['className']} WHERE ${collConf['canonicalId']} = '" + trimUrn + "'"


    }


}
