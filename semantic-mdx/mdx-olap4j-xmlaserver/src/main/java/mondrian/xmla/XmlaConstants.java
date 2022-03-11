/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2011 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla;

/**
 * Constants for XML/A.
 *
 * @author Gang Chen
 */
public interface XmlaConstants {

    /* SOAP 1.1 */
    String NS_SOAP_ENV_1_1 =
        "http://schemas.xmlsoap.org/soap/envelope/";
    String NS_SOAP_ENC_1_1 =
        "http://schemas.xmlsoap.org/soap/encoding/";

    /* SOAP 1.2 - currently not supported */
    String NS_SOAP_ENV_1_2 =
        "http://www.w3.org/2003/05/soap-envelope";
    String NS_SOAP_ENC_1_2 =
        "http://www.w3.org/2003/05/soap-encoding";

    /* Namespaces for XML */
    String NS_XSD = "http://www.w3.org/2001/XMLSchema";
    String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /* Namespaces for XML/A */
    String NS_XMLA =
        "urn:schemas-microsoft-com:xml-analysis";
    String NS_XMLA_DDL = "http://schemas.microsoft.com/analysisservices/2003/engine";
    String NS_XMLA_DDL2 = "http://schemas.microsoft.com/analysisservices/2003/engine/2";
    String NS_XMLA_DDL2_2 = "http://schemas.microsoft.com/analysisservices/2003/engine/2/2";
    String NS_XMLA_DDL100 = "http://schemas.microsoft.com/analysisservices/2008/engine/100";
    String NS_XMLA_DDL100_100 = "http://schemas.microsoft.com/analysisservices/2008/engine/100/100";
    String NS_XMLA_DDL200 = "http://schemas.microsoft.com/analysisservices/2010/engine/200";
    String NS_XMLA_DDL200_200 = "http://schemas.microsoft.com/analysisservices/2010/engine/200/200";
    String NS_XMLA_DDL300 = "http://schemas.microsoft.com/analysisservices/2011/engine/300";
    String NS_XMLA_DDL300_300 = "http://schemas.microsoft.com/analysisservices/2011/engine/300/300";
    String NS_XMLA_DDL400 = "http://schemas.microsoft.com/analysisservices/2012/engine/400";
    String NS_XMLA_DDL400_400 = "http://schemas.microsoft.com/analysisservices/2012/engine/400_400";


    String NS_XMLA_MDDATASET =
        "urn:schemas-microsoft-com:xml-analysis:mddataset";
    String NS_XMLA_EMPTY =
        "urn:schemas-microsoft-com:xml-analysis:empty";
    String NS_XMLA_ROWSET =
        "urn:schemas-microsoft-com:xml-analysis:rowset";
    String NS_SQL = "urn:schemas-microsoft-com:xml-sql";
    String MS_XMLA = "http://schemas.microsoft.com/analysisservices/2003/xmla";

    String NS_XMLA_EX =
        "urn:schemas-microsoft-com:xml-analysis:exception";

    String NS_XMLA_MS = "http://schemas.microsoft.com/analysisservices/2003/xmla";

    String NS_SOAP_SECEXT =
        "http://schemas.xmlsoap.org/ws/2002/04/secext";

    String SOAP_PREFIX = "SOAP-ENV";

    String MS_SOAP_PREFIX = "soap";

    /**
     * Soap Header mustUnderstand attribute name.
     */
    String SOAP_MUST_UNDERSTAND_ATTR = "mustUnderstand";

    /**
     * Soap XMLA Header elements and attribute names.
     */
    String XMLA_BEGIN_SESSION      = "BeginSession";
    String XMLA_SESSION            = "Session";
    String XMLA_END_SESSION        = "EndSession";
    String XMLA_SESSION_ID         = "SessionId";
    String XMLA_SECURITY           = "Security";

    // Names of context keys known by both callbacks and Mondrian code.

    // context key for role name storage
    String CONTEXT_ROLE_NAME   = "role_name";
    // context key for language (SOAP or JSON)
    String CONTEXT_MIME_TYPE = "language";
    // context key for session id storage
    String CONTEXT_XMLA_SESSION_ID   = "session_id";

    // Username and password tokens
    String CONTEXT_XMLA_USERNAME = "username";
    String CONTEXT_XMLA_PASSWORD = "password";

    // context key for session state storage
    String CONTEXT_XMLA_SESSION_STATE = "SessionState";
    String CONTEXT_XMLA_SESSION_STATE_BEGIN =
        "SessionStateBegin";
    String CONTEXT_XMLA_SESSION_STATE_WITHIN =
        "SessionStateWithin";
    String CONTEXT_XMLA_SESSION_STATE_END =
        "SessionStateEnd";

    /*************************************************************************
    *
    * The following are XMLA exception fault codes used as faultcode entries
    * in the SOAP Fault element.
    *
    * If Mondrian Exceptions actually used the "id" attributes found in the
    * MondrianResource.xml file, then those would be used as the SOAP Fault
    * detail error code values, but, alas they do not show up as part of
    * the generated Exception Java code so, here we simply duplicate
    * the fault code entry.
    *
    * Currently, SOAP 1.2 errors are not supported.
    *
    *************************************************************************/

   /**
    * This is the namespace used to qualify the faultcode identifier.
    */
    String MONDRIAN_NAMESPACE = "http://mondrian.sourceforge.net";
    String FAULT_NS_PREFIX = "XA";

    String FAULT_ACTOR = "Mondrian";

    // soap 1.1 default faultcodes
    String VERSION_MISSMATCH_FAULT_FC = "VersionMismatch";
    String MUST_UNDERSTAND_FAULT_FC = "MustUnderstand";
    String CLIENT_FAULT_FC = "Client";
    String SERVER_FAULT_FC = "Server";

      //<faultcode>XA:Mondrian.XML.88BA1202</faultcode>
    String FAULT_FC_PREFIX = "Mondrian";
    String FAULT_FS_PREFIX = "The Mondrian XML: ";

    /////////////////////////////////////////////////////////////////////////
    // Unmarshall Soap Message : USM
    /////////////////////////////////////////////////////////////////////////
    String USM_REQUEST_STATE_CODE = "00USMA01";
    String USM_REQUEST_STATE_FAULT_FS =
            "Request input method invoked at illegal time";

    String USM_REQUEST_INPUT_CODE = "00USMA02";
    String USM_REQUEST_INPUT_FAULT_FS =
            "Request input Exception occurred";

    String USM_DOM_FACTORY_CODE = "00USMB01";
    String USM_DOM_FACTORY_FAULT_FS =
        "DocumentBuilder cannot be created which satisfies the configuration "
        + "requested";

    String USM_DOM_PARSE_IO_CODE = "00USMC01";
    String USM_DOM_PARSE_IO_FAULT_FS =
        "DOM parse IO errors occur";

    String USM_DOM_PARSE_CODE = "00USMC02";
    String USM_DOM_PARSE_FAULT_FS =
        "DOM parse errors occur";

    // unknown error while unmarshalling soap message
    String USM_UNKNOWN_CODE = "00USMU01";
    String USM_UNKNOWN_FAULT_FS =
            "Unknown error unmarshalling soap message";

    /////////////////////////////////////////////////////////////////////////
    // Callback http header : CHH
    /////////////////////////////////////////////////////////////////////////
    String CHH_CODE = "00CHHA01";
    String CHH_FAULT_FS =
            "Error in Callback processHttpHeader";

    String CHH_AUTHORIZATION_CODE = "00CHHA02";
    String CHH_AUTHORIZATION_FAULT_FS =
            "Error in Callback processHttpHeader Authorization";

    /////////////////////////////////////////////////////////////////////////
    // Callback Pre-Action : CPREA
    /////////////////////////////////////////////////////////////////////////
    String CPREA_CODE = "00CPREA01";
    String CPREA_FAULT_FS =
            "Error in Callback PreAction";

/*
    String CPREA_AUTHORIZATION_CODE = "00CPREA02";
    String CPREA_AUTHORIZATION_FAULT_FS =
            "Error Callback PreAction Authorization";
*/

    /////////////////////////////////////////////////////////////////////////
    // Handle Soap Header : HSH
    /////////////////////////////////////////////////////////////////////////
    String HSH_MUST_UNDERSTAND_CODE = "00HSHA01";
    String HSH_MUST_UNDERSTAND_FAULT_FS =
            "SOAP Header must understand element not recognized";

    // This is used to signal XMLA clients supporting Soap header session ids
    // that the client's metadata may no longer be valid.
    String HSH_BAD_SESSION_ID_CODE = "00HSHB01";
    String HSH_BAD_SESSION_ID_FAULT_FS =
            "Bad Session Id, re-start session";

    // unknown error while handle soap header
    String HSH_UNKNOWN_CODE = "00HSHU01";
    String HSH_UNKNOWN_FAULT_FS =
            "Unknown error handle soap header";

    /////////////////////////////////////////////////////////////////////////
    // Handle Soap Body : HSB
    /////////////////////////////////////////////////////////////////////////
    String HSB_BAD_SOAP_BODY_CODE = "00HSBA01";
    String HSB_BAD_SOAP_BODY_FAULT_FS =
            "SOAP Body not correctly formed";

    String HSB_PROCESS_CODE = "00HSBB01";
    String HSB_PROCESS_FAULT_FS =
            "XMLA SOAP Body processing error";

    String HSB_BAD_METHOD_CODE = "00HSBB02";
    String HSB_BAD_METHOD_FAULT_FS =
            "XMLA SOAP bad method";

    String HSB_BAD_METHOD_NS_CODE = "00HSBB03";
    String HSB_BAD_METHOD_NS_FAULT_FS =
            "XMLA SOAP bad method namespace";

    String HSB_BAD_REQUEST_TYPE_CODE = "00HSBB04";
    String HSB_BAD_REQUEST_TYPE_FAULT_FS =
            "XMLA SOAP bad Discover RequestType element";

    String HSB_BAD_RESTRICTIONS_CODE = "00HSBB05";
    String HSB_BAD_RESTRICTIONS_FAULT_FS =
            "XMLA SOAP bad Discover Restrictions element";

    String HSB_BAD_PROPERTIES_CODE = "00HSBB06";
    String HSB_BAD_PROPERTIES_FAULT_FS =
            "XMLA SOAP bad Discover or Execute Properties element";

    String HSB_BAD_COMMAND_CODE = "00HSBB07";
    String HSB_BAD_COMMAND_FAULT_FS =
            "XMLA SOAP bad Execute Command element";

    String HSB_BAD_RESTRICTION_LIST_CODE = "00HSBB08";
    String HSB_BAD_RESTRICTION_LIST_FAULT_FS =
            "XMLA SOAP too many Discover RestrictionList element";

    String HSB_BAD_PROPERTIES_LIST_CODE = "00HSBB09";
    String HSB_BAD_PROPERTIES_LIST_FAULT_FS =
            "XMLA SOAP bad Discover or Execute PropertyList element";

    String HSB_BAD_STATEMENT_CODE = "00HSBB10";
    String HSB_BAD_STATEMENT_FAULT_FS =
            "XMLA SOAP bad Execute Statement element";

    String HSB_BAD_PARAMETERS_CODE = "00HSBB11";
    String HSB_BAD_PARAMETERS_FAULT_FS =
            "XMLA SOAP bad Execute Parameters element";

    String HSB_BAD_NON_NULLABLE_COLUMN_CODE = "00HSBB16";
    String HSB_BAD_NON_NULLABLE_COLUMN_FAULT_FS =
            "XMLA SOAP non-nullable column";


    String HSB_CONNECTION_DATA_SOURCE_CODE = "00HSBC01";
    String HSB_CONNECTION_DATA_SOURCE_FAULT_FS =
            "XMLA connection datasource not found";

    String HSB_ACCESS_DENIED_CODE = "00HSBC02";
    String HSB_ACCESS_DENIED_FAULT_FS =
            "XMLA connection with role must be authenticated";

    String HSB_PARSE_QUERY_CODE = "00HSBD01";
    String HSB_PARSE_QUERY_FAULT_FS =
        "XMLA MDX parse failed";

    String HSB_EXECUTE_QUERY_CODE = "00HSBD02";
    String HSB_EXECUTE_QUERY_FAULT_FS =
        "XMLA MDX execute failed";

    String HSB_PARSE_DMV_CODE = "00HSBD03";
    String HSB_PARSE_DMV_FAULT_FS =
            "XMLA DMV parse failed";

    String HSB_DISCOVER_FORMAT_CODE = "00HSBE01";
    String HSB_DISCOVER_FORMAT_FAULT_FS =
            "XMLA Discover format error";

    String HSB_DRILL_THROUGH_FORMAT_CODE = "00HSBE02";
    String HSB_DRILL_THROUGH_FORMAT_FAULT_FS =
            "XMLA Drill Through format error";

    String HSB_DISCOVER_UNPARSE_CODE = "00HSBE02";
    String HSB_DISCOVER_UNPARSE_FAULT_FS =
            "XMLA Discover unparse results error";

    String HSB_EXECUTE_UNPARSE_CODE = "00HSBE03";
    String HSB_EXECUTE_UNPARSE_FAULT_FS =
            "XMLA Execute unparse results error";

    String HSB_EXECUTE_DMV_UNPARSE_CODE = "00HSBE04";
    String HSB_EXECUTE_DMV_UNPARSE_FAULT_FS =
            "XMLA Execute (DMV) unparse results error";

    String HSB_DRILL_THROUGH_NOT_ALLOWED_CODE = "00HSBF01";
    String HSB_DRILL_THROUGH_NOT_ALLOWED_FAULT_FS =
            "XMLA Drill Through not allowed";

    String HSB_DRILL_THROUGH_SQL_CODE = "00HSBF02";
    String HSB_DRILL_THROUGH_SQL_FAULT_FS =
            "XMLA Drill Through SQL error";

    // unknown error while handle soap body
    String HSB_UNKNOWN_CODE = "00HSBU01";
    String HSB_UNKNOWN_FAULT_FS =
            "Unknown error handle soap body";

    /////////////////////////////////////////////////////////////////////////
    // Callback Post-Action : CPOSTA
    /////////////////////////////////////////////////////////////////////////
    String CPOSTA_CODE = "00CPOSTA01";
    String CPOSTA_FAULT_FS =
            "Error in Callback PostAction";

    /////////////////////////////////////////////////////////////////////////
    // Marshall Soap Message : MSM
    /////////////////////////////////////////////////////////////////////////

    // unknown error while marshalling soap message
    String MSM_UNKNOWN_CODE = "00MSMU01";
    String MSM_UNKNOWN_FAULT_FS =
            "Unknown error marshalling soap message";

    /////////////////////////////////////////////////////////////////////////
    // Unknown error : UE
    /////////////////////////////////////////////////////////////////////////
    String UNKNOWN_ERROR_CODE = "00UE001";
    // While this is actually "unknown", for users "internal"
    // is a better term
    String UNKNOWN_ERROR_FAULT_FS = "Internal Error";

}

// End XmlaConstants.java
