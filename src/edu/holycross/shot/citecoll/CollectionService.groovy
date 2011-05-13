package edu.holycross.shot.citecoll
//
// Possibly add paging for queries that can return multiples
//


import edu.harvard.chs.cite.CiteUrn
import groovy.xml.MarkupBuilder
// Google data APIs:
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.util.ContentType;

class CollectionService {

    static String SERVICE_URL = "https://www.google.com/fusiontables/api/query"
    static String CLIENT_APP = "shot.holycross.edu-fusioncoll-0.1"
    static String CITE_NS = "http://chs.harvard.edu/xmlns/cite"

    groovy.xml.Namespace citens = new groovy.xml.Namespace(CITE_NS)


    /** Capabilities file for this service */
    File capabilitiesFile
    /** A map representing the data in the capabilities file */
    LinkedHashMap citeConfig

    /** Constructor parses capabilities file */
    CollectionService(File capsFile) {
        this.capabilitiesFile = capsFile
        this.citeConfig = configureFromFile(this.capabilitiesFile)
        // throw exception if could not parse caps file...
    }


    /** Creates a string with valid XML reply to the
    * CITE Collection GetCapabilities request.
    */
    String getCapsReply() {
        return this.capabilitiesFile.getText()
    }


    String getFirstReply(String requestUrn, String collectionId) {
        def collConf = this.citeConfig[collectionId]
        CiteUrn citeUrn = new CiteUrn(requestUrn)

        StringBuffer replyBuff = new StringBuffer("<GetFirst xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<urn>${requestUrn}</urn>\n</request>\n")
        replyBuff.append("<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getFirstObject(citeUrn)}\n</reply>\n</GetFirst>\n")
        return replyBuff.toString()
    }




    String getFirstReply(String collectionId) {
        def collConf = this.citeConfig[collectionId]
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${collectionId}")

        StringBuffer replyBuff = new StringBuffer("<GetFirst xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n</request>\n")
        replyBuff.append("<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getFirstObject(citeUrn)}</reply>\n</GetFirst>\n")
        return replyBuff.toString()
    }

    // return null if not an ordered collection
    String getFirstObject(CiteUrn requestUrn) {
        def collectionId = requestUrn.getCollection()
        def collConf = this.citeConfig[collectionId]
        if (!collConf['orderedBy']) {
            return null
        }
        
        // test for ordering field...
        StringBuffer qBuff = new StringBuffer("SELECT MINIMUM(${collConf['orderedBy']}) FROM ${collConf['className']}" )
        if (collConf['groupProperty']) {
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
        if (collConf['groupProperty']) {
            objQuery.append(" AND ${collConf['groupProperty']} = '" + collectionId + "'")
        }

        def objQueryUrl = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(objQuery.toString(), "UTF-8"));
        GDataRequest objrequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, objQueryUrl, ContentType.TEXT_PLAIN)
        objrequest.execute()
        def objReplyLines= objrequest.requestUrl.getText('UTF-8').readLines()

        return rowToXml(objReplyLines[1],requestUrn.toString())
    }

    String getFirstObject(String urnStr) {
        return getFirstObject(new CiteUrn(urnStr))
    }


    /** Creates a string with valid XML reply to the
    * CITE Collection GetObject request when the object
    * is identified by a collection identifier and an
    * an object identifier.
    * @param collectionId CITE identifier for the collection.
    * @param objectId CITE identifier for the object within the collection.
    */
    String getObjReply(String collectionId, String objectId) {
        def collConf = this.citeConfig[collectionId]
        CiteUrn citeUrn = new CiteUrn("urn:cite:${collConf['nsabbr']}:${collectionId}.${objectId}")

        StringBuffer replyBuff = new StringBuffer("<GetObject xmlns='http://chs.harvard.edu/xmlns/cite'>\n<request>\n<collection>${collectionId}</collection>\n<id>${objectId}</id>\n</request>\n")
        replyBuff.append("<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getObjectData(citeUrn)}</reply>\n</GetObject>\n")
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
        replyBuff.append("<reply datans='" + collConf['nsabbr'] +"' datansuri='" + collConf['nsfull'] + "'>")
        replyBuff.append("\n${getObjectData(requestUrn)}</reply>\n</GetObject>\n")
    }


    /** Creates a well-formed fragment of a CITE reply
    * representing a single CITE object uniquely identified by CITE URN.
    * @param requestUrn The CITE URN identifying the object.
    * @returns A String of well-formed XML
    */
    String getObjectData(CiteUrn requestUrn) {
        return getObjectData(requestUrn.toString())
    }

    /** Creates a well-formed fragment of a CITE reply
    * representing a single CITE object uniquely identified by 
    * a CITE URN string.
    * @param requestUrn The CITE URN, as a String, identifying the object.
    * @returns A String of well-formed XML
    */
    String getObjectData(String requestUrn) {
        def q =  getObjectQuery(requestUrn)
        def url = new URL(CollectionService.SERVICE_URL + "?sql=" + URLEncoder.encode(q, "UTF-8"));
        GDataRequest grequest = new GoogleService("fusiontables", CollectionService.CLIENT_APP).getRequestFactory().getRequest(RequestType.QUERY, url, ContentType.TEXT_PLAIN)
        grequest.execute()

        def replyLines= grequest.requestUrl.getText('UTF-8').readLines()
        if (replyLines.size() != 2) {
            // exception:  should be a header and one record
        }
        return rowToXml(replyLines[1],requestUrn)
    }

    String rowToXml(String row, String requestUrn) {

        def CiteUrn citeUrn = new CiteUrn(requestUrn)
        def collConf = this.citeConfig[citeUrn.getCollection()]
        def propList = collConf['properties']
        def canonicalIndex
        propList.eachWithIndex { p, i ->
            if (p['name'] == collConf['canonicalId']) {
                canonicalIndex =  i
            }
        }
        
        // This is naive and needs to be fixed:
        def cols = row.split(/,/)
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        
        xml.citeObject("urn" : "urn:cite:${cols[canonicalIndex]}") {
            cols.eachWithIndex { c, i ->
                def currProp = propList[i]
                if (currProp['name'] != collConf['canonicalId']) {
                    citeProperty(name : "${currProp['name']}", label : "${currProp['label']}", type : "${currProp['type']}" ,"${c}")
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
        root[citens.collectionService][citens.citeCollection].each { c ->
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

            def citeExtensions = []
            c[citens.citeExtension].each { ce ->
                citeExtensions << "${ce.'@uri'}"
            }

            def collData = [
                "className" : "${c.'@class'}",
                "canonicalId" : "${c.'@canonicalId'}",
                "groupProperty" : "${c.'@groupProperty'}",
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
    String getObjectQuery(String coll, String obj) {
        def collConf = this.citeConfig[coll]
        if (!collConf) { return null }

        def trimUrn = "${collConf['nsabbr']}:${coll}.${obj}"
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


/*
// Do these belon in this class or calling servlet wrapper?

boolean urnSyntaxOk(String urn) {
    def components = urn.split(/:/)
    if (components.size() != 4) {
        return false
    }
    if (components[0] != 'urn') {
        return false
    }
    if (components[1] != 'cite') {
        return false
    }
    return true
}

def errorMsg =  { msg ->
    StringWriter writer = new StringWriter()
    MarkupBuilder xml = new MarkupBuilder(writer)
    xml.CiteError {
        errorCode("1")
        description("${msg}")
    }
    println writer.toString()    
}


String checkRequest() {
    params.keySet().each { paramKey ->
        def paramList = params[paramKey]
        if (paramList.size() > 1) {
            return "Bad parameter set:  duplicated parameter ${paramKey}"
        }
    }
    // Must include a 'req' parameter:
    if (!params['req']) {
        return  "No 'req' parameter given."
    }
    if (params['req'] == "GetCapabilities") {
        // no other params needed
        return "OK"
    }

    switch (params['req']) {
        case "GetObject":
            
            if (params['urn']) {
            if (!urnSyntaxOk(params['urn'])) {
                return "Invalid urn syntax '" + params['urn'] + "'."
            }
            def components = params['urn'].split(/:/)
            def idparts = components[3].split(/\./)

            if (idparts.size() != 2) {
                return "Invalid urn ${params['urn']}:  id component ${components[3]} must include collection and object id."
            }
            return "OK"
        }
        if ((params['collection']) && (params['id'])) {
            return "OK"
        }
    
        return "${params['req']} request requires either a 'urn' parameter or a combination of a 'collection' and an 'id' parameter for a validly configured collection."
        break

        default:
            return "Unrecognized or unimplemented request '" +  params['req'] + "'"
        break
    }
}

*/
