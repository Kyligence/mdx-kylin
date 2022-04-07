/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2014 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla.impl;

import io.kylin.mdx.ExceptionUtils;
import mondrian.xmla.*;
import mondrian.xmla.handler.chunk.ByteArrayDataChunk;
import mondrian.xmla.handler.chunk.CommonResultDataChunk;
import mondrian.xmla.handler.chunk.XmlaDataChunk;
import mondrian.xmla.utils.RequestCancelUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of XML/A servlet.
 *
 * @author Gang Chen
 */
public abstract class DefaultXmlaServlet extends XmlaServlet {

    private static final String NL = System.getProperty("line.separator");

    /**
     * Servlet config parameter that determines whether the xmla servlet
     * requires authenticated sessions.
     */
    private static final String REQUIRE_AUTHENTICATED_SESSIONS = "requireAuthenticatedSessions";

    private DocumentBuilderFactory domFactory = null;

    private boolean requireAuthenticatedSessions = false;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.domFactory = getDocumentBuilderFactory();
        this.requireAuthenticatedSessions = Boolean
                .parseBoolean(servletConfig.getInitParameter(REQUIRE_AUTHENTICATED_SESSIONS));
    }

    public static DocumentBuilderFactory getDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setNamespaceAware(true);
        return factory;
    }

    @Override
    protected void unmarshallSoapMessage(HttpServletRequest request, Element[] requestSoapParts) throws XmlaException {
        try {
            InputStream inputStream;
            try {
                inputStream = request.getInputStream();
            } catch (IllegalStateException ex) {
                throw new XmlaException(SERVER_FAULT_FC, USM_REQUEST_STATE_CODE, USM_REQUEST_STATE_FAULT_FS, ex);
            } catch (IOException ex) {
                // This is either Client or Server
                throw new XmlaException(SERVER_FAULT_FC, USM_REQUEST_INPUT_CODE, USM_REQUEST_INPUT_FAULT_FS, ex);
            }

            unmarshallSoapMessage(inputStream, domFactory, requestSoapParts);
        } catch (XmlaException xex) {
            throw xex;
        } catch (Exception ex) {
            throw new XmlaException(SERVER_FAULT_FC, USM_UNKNOWN_CODE, USM_UNKNOWN_FAULT_FS, ex);
        }
    }

    public static void unmarshallSoapMessage(InputStream inputStream, DocumentBuilderFactory domFactory, Element[] requestSoapParts) throws XmlaException {
        DocumentBuilder domBuilder;
        try {
            domBuilder = domFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new XmlaException(SERVER_FAULT_FC, USM_DOM_FACTORY_CODE, USM_DOM_FACTORY_FAULT_FS, ex);
        }

        Document soapDoc;
        try {
            soapDoc = domBuilder.parse(new InputSource(inputStream));
        } catch (IOException ex) {
            // This is either Client or Server
            throw new XmlaException(SERVER_FAULT_FC, USM_DOM_PARSE_IO_CODE, USM_DOM_PARSE_IO_FAULT_FS, ex);
        } catch (SAXException ex) {
            // Assume client passed bad xml
            throw new XmlaException(CLIENT_FAULT_FC, USM_DOM_PARSE_CODE, USM_DOM_PARSE_FAULT_FS, ex);
        }

        /* Check SOAP message */
        Element envElem = soapDoc.getDocumentElement();

        if (LOGGER.isDebugEnabled()) {
            final StringWriter writer = new StringWriter();
            writer.write("XML/A request content");
            writer.write(NL);
            XmlaUtil.element2Text(envElem, writer);
            LOGGER.debug(writer.toString());
        }

        if ("Envelope".equals(envElem.getLocalName())) {
            if (!(NS_SOAP_ENV_1_1.equals(envElem.getNamespaceURI()))) {
                throw new XmlaException(CLIENT_FAULT_FC, USM_DOM_PARSE_CODE, USM_DOM_PARSE_FAULT_FS,
                        new SAXException("Invalid SOAP message: " + "Envelope element not in SOAP namespace"));
            }
        } else {
            throw new XmlaException(CLIENT_FAULT_FC, USM_DOM_PARSE_CODE, USM_DOM_PARSE_FAULT_FS,
                    new SAXException("Invalid SOAP message: " + "Top element not Envelope"));
        }

        Element[] childs = XmlaUtil.filterChildElements(envElem, NS_SOAP_ENV_1_1, "Header");
        if (childs.length > 1) {
            throw new XmlaException(CLIENT_FAULT_FC, USM_DOM_PARSE_CODE, USM_DOM_PARSE_FAULT_FS,
                    new SAXException("Invalid SOAP message: " + "More than one Header elements"));
        }
        requestSoapParts[0] = childs.length == 1 ? childs[0] : null;

        childs = XmlaUtil.filterChildElements(envElem, NS_SOAP_ENV_1_1, "Body");
        if (childs.length != 1) {
            throw new XmlaException(CLIENT_FAULT_FC, USM_DOM_PARSE_CODE, USM_DOM_PARSE_FAULT_FS,
                    new SAXException("Invalid SOAP message: " + "Does not have one Body element"));
        }
        requestSoapParts[1] = childs[0];
    }

    /**
     * {@inheritDoc}
     *
     * <p>See if there is a "mustUnderstand" header element.
     * If there is a BeginSession element, then generate a session id and
     * add to context Map.</p>
     *
     * <p>Excel 2000 and Excel XP generate both a BeginSession, Session and
     * EndSession mustUnderstand=1
     * in the "urn:schemas-microsoft-com:xml-analysis" namespace
     * Header elements and a NamespaceCompatibility mustUnderstand=0
     * in the "http://schemas.microsoft.com/analysisservices/2003/xmla"
     * namespace. Here we handle only the session Header elements.
     *
     * <p>We also handle the Security element.</p>
     */
    @Override
    public void handleSoapHeader(HttpServletResponse response, Element[] requestSoapParts,
                                 XmlaResult result, Map<String, Object> context) throws XmlaException {
        try {
            Element hdrElem = requestSoapParts[0];
            if ((hdrElem == null) || (!hdrElem.hasChildNodes())) {
                return;
            }

            String encoding = response.getCharacterEncoding();

            byte[] bytes = null;

            NodeList nlst = hdrElem.getChildNodes();
            int nlen = nlst.getLength();
            boolean authenticatedSession = false;
            boolean beginSession = false;
            for (int i = 0; i < nlen; i++) {
                Node n = nlst.item(i);
                if (!(n instanceof Element)) {
                    continue;
                }
                Element e = (Element) n;
                String localName = e.getLocalName();

                if (localName.equals(XMLA_SECURITY) && NS_SOAP_SECEXT.equals(e.getNamespaceURI())) {
                    // Example:
                    //
                    // <Security xmlns="http://schemas.xmlsoap.org/ws/2002/04/secext">
                    //   <UsernameToken>
                    //     <Username>MICHELE</Username>
                    //     <Password Type="PasswordText">ROSSI</Password>
                    //   </UsernameToken>
                    // </Security>
                    // <BeginSession mustUnderstand="1"
                    //   xmlns="urn:schemas-microsoft-com:xml-analysis" />
                    NodeList childNodes = e.getChildNodes();
                    Element userNameToken = (Element) childNodes.item(1);
                    NodeList userNamePassword = userNameToken.getChildNodes();
                    Element username = (Element) userNamePassword.item(1);
                    Element password = (Element) userNamePassword.item(3);
                    String userNameStr = username.getChildNodes().item(0).getNodeValue();
                    context.put(CONTEXT_XMLA_USERNAME, userNameStr);
                    String passwordStr = "";

                    if (password.getChildNodes().item(0) != null) {
                        passwordStr = password.getChildNodes().item(0).getNodeValue();
                    }

                    context.put(CONTEXT_XMLA_PASSWORD, passwordStr);

                    if ("".equals(passwordStr) || null == passwordStr) {
                        LOGGER.warn("Security header for user [" + userNameStr + "] provided without password");
                    }
                    authenticatedSession = true;
                    continue;
                }

                // Make sure Element has mustUnderstand=1 attribute.
                Attr attr = e.getAttributeNode(SOAP_MUST_UNDERSTAND_ATTR);
                boolean mustUnderstandValue = true;
                // Excel
                // attr != null
                // && attr.getValue() != null
                // && attr.getValue().equals("1");

                if (!mustUnderstandValue) {
                    continue;
                }

                // Is it an XMLA element
                if (!NS_XMLA.equals(e.getNamespaceURI())) {
                    continue;
                }
                // So, an XMLA mustUnderstand-er
                // Do we know what to do with it
                // We understand:
                //    BeginSession
                //    Session
                //    EndSession

                String sessionIdStr;
                if (localName.equals(XMLA_BEGIN_SESSION)) {
                    sessionIdStr = SessionManager.generateSessionId(context, getCallbacks());

                    context.put(CONTEXT_XMLA_SESSION_ID, sessionIdStr);
                    context.put(CONTEXT_XMLA_SESSION_STATE, CONTEXT_XMLA_SESSION_STATE_BEGIN);

                } else if (localName.equals(XMLA_SESSION)) {
                    sessionIdStr = SessionManager.getSessionIdFromRequest(e, context);

                    SessionManager.SessionInfo sessionInfo = SessionManager.getSessionInfo(sessionIdStr);

                    if (sessionInfo != null) {
                        context.put(CONTEXT_XMLA_USERNAME, sessionInfo.username);
                        context.put(CONTEXT_XMLA_PASSWORD, sessionInfo.password);
                    }

                    context.put(CONTEXT_XMLA_SESSION_ID, sessionIdStr);
                    context.put(CONTEXT_XMLA_SESSION_STATE, CONTEXT_XMLA_SESSION_STATE_WITHIN);

                } else if (localName.equals(XMLA_END_SESSION)) {
                    sessionIdStr = SessionManager.getSessionIdFromRequest(e, context);

                    context.put(CONTEXT_XMLA_SESSION_ID, sessionIdStr);
                    context.put(CONTEXT_XMLA_SESSION_STATE, CONTEXT_XMLA_SESSION_STATE_END);

                } else {
                    // error
                    String msg = "Invalid XML/A message: Unknown " + "\"mustUnderstand\" XMLA Header element \""
                            + localName + "\"";
                    throw new XmlaException(MUST_UNDERSTAND_FAULT_FC, HSH_MUST_UNDERSTAND_CODE,
                            HSH_MUST_UNDERSTAND_FAULT_FS, new RuntimeException(msg));
                }

                if (localName.equals(XMLA_BEGIN_SESSION)) {
                    String buf = "<Session " +
                            XMLA_SESSION_ID +
                            "=\"" +
                            sessionIdStr +
                            "\" " +
                            "xmlns=\"" +
                            NS_XMLA +
                            "\" />";
                    bytes = buf.getBytes(encoding);
                }

                if (authenticatedSession) {
                    String username = (String) context.get(CONTEXT_XMLA_USERNAME);
                    String password = (String) context.get(CONTEXT_XMLA_PASSWORD);
                    String sessionId = (String) context.get(CONTEXT_XMLA_SESSION_ID);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("New authenticated session; storing credentials [" + username
                                + "/********] for session id [" + sessionId + "]");
                    }

                    SessionManager.saveSessionInfo(username, password, sessionId);
                } else {
                    if (beginSession && requireAuthenticatedSessions) {
                        throw new XmlaException(XmlaConstants.CLIENT_FAULT_FC, XmlaConstants.CHH_AUTHORIZATION_CODE,
                                XmlaConstants.CHH_AUTHORIZATION_FAULT_FS,
                                new Exception("Session Credentials NOT PROVIDED"));
                    }
                }
            }
            result.header = bytes;
        } catch (XmlaException xex) {
            throw xex;
        } catch (Exception ex) {
            throw new XmlaException(SERVER_FAULT_FC, HSH_UNKNOWN_CODE, HSH_UNKNOWN_FAULT_FS, ex);
        }
    }

    @Override
    public void handleSoapBody(HttpServletResponse response, Element[] requestSoapParts, XmlaResult result,
                               Map<String, Object> context) throws XmlaException {
        try {
            Element bodyElem = requestSoapParts[1];
            Element[] dreqs = XmlaUtil.filterChildElements(bodyElem, NS_XMLA, "Discover");
            Element[] ereqs = XmlaUtil.filterChildElements(bodyElem, NS_XMLA, "Execute");
            if (dreqs.length + ereqs.length != 1) {
                throw new XmlaException(CLIENT_FAULT_FC, HSB_BAD_SOAP_BODY_CODE, HSB_BAD_SOAP_BODY_FAULT_FS,
                        new RuntimeException("Invalid XML/A message: Body has " + dreqs.length
                                + " Discover Requests and " + ereqs.length + " Execute Requests"));
            }

            Element xmlaReqElem = (dreqs.length == 0 ? ereqs[0] : dreqs[0]);

            // use context variable 'role_name' as this request's XML/A role
            String roleName = (String) context.get(CONTEXT_ROLE_NAME);

            String username = (String) context.get(CONTEXT_XMLA_USERNAME);
            String password = (String) context.get(CONTEXT_XMLA_PASSWORD);
            String sessionId = (String) context.get(CONTEXT_XMLA_SESSION_ID);
            XmlaRequest xmlaReq = new DefaultXmlaRequest(xmlaReqElem, roleName, username, password, sessionId);

            if (xmlaReq.isCancel()
                    && StringUtils.isNotBlank(sessionId)
                    && !SessionManager.existsSession(sessionId)
                    && RequestCancelUtils.existCancelCluster(sessionId)) {
                return;
            }
            SessionManager.addSession(sessionId);

            // "ResponseMimeType" may be in the context if the "Accept" HTTP
            // header was specified. But override if the SOAP request has the
            // "ResponseMimeType" property.
            Enumeration.ResponseMimeType responseMimeType = Enumeration.ResponseMimeType.SOAP;
            final String responseMimeTypeName = xmlaReq.getProperties().get("ResponseMimeType");
            if (responseMimeTypeName != null) {
                responseMimeType = Enumeration.ResponseMimeType.MAP.get(responseMimeTypeName);
                if (responseMimeType != null) {
                    context.put(CONTEXT_MIME_TYPE, responseMimeType);
                } else {
                    responseMimeType = Enumeration.ResponseMimeType.SOAP;
                }
            }
            result.responseMimeType = responseMimeType;

            XmlaResponse xmlaRes = new DirectXmlaResponse(result);
            try {
                getXmlaHandler().process(xmlaReq, xmlaRes);
            } catch (XmlaException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new XmlaException(SERVER_FAULT_FC, HSB_PROCESS_CODE, HSB_PROCESS_FAULT_FS, ex);
            }
        } catch (XmlaException xex) {
            throw xex;
        } catch (Exception ex) {
            throw new XmlaException(SERVER_FAULT_FC, HSB_UNKNOWN_CODE, HSB_UNKNOWN_FAULT_FS, ex);
        }
    }

    @Override
    protected void marshallSoapMessage(HttpServletResponse response, XmlaResult result,
                                       Enumeration.ResponseMimeType responseMimeType)
            throws XmlaException {
        XmlaDataChunk[] chunks = null;
        try {
            // If CharacterEncoding was set in web.xml, use this value
            long start = System.currentTimeMillis();
            XmlaRequestContext context = Objects.requireNonNull(XmlaRequestContext.getContext());
            context.runningStatistics.marshallSoapMessageTimeStart = System.currentTimeMillis();

            String encoding = (charEncoding != null) ? charEncoding : response.getCharacterEncoding();

            /*
             * Since we just reset response, encoding and content-type were
             * reset too
             */
            if (charEncoding != null) {
                response.setCharacterEncoding(charEncoding);
            }

            switch (responseMimeType) {
                case JSON:
                    response.setContentType("application/json");
                    break;
                case SOAP:
                default:
                    response.setContentType("text/xml");
                    break;
            }

            // The setCharacterEncoding, setContentType, or setLocale method
            // must be called BEFORE getWriter or getOutputStream and before
            // committing the response for the character encoding to be used.
            //
            // See javax.servlet.ServletResponse

            byte[] soapHeader = result.header;
            XmlaDataChunk soapBody = result.body;

            try {
                switch (responseMimeType) {
                    case JSON:
                        chunks = new XmlaDataChunk[]{soapBody};
                        break;

                    case SOAP:
                    default:
                        String nl = context.compactResult ? "" : NL;
                        if (XmlaRequestContext.ClientType.POWERBI.equals(context.clientType)) {
                            StringBuilder s1 = new StringBuilder();
                            if (!context.fromGateway) {
                                s1.append("<?xml version=\"1.0\" encoding=\"").append(encoding).append("\"?>").append(nl);
                            }
                            s1.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">").append(nl);
                            s1.append("<SOAP-ENV:Header>");
                            String s2 = "</SOAP-ENV:Header>" + nl + "<" + SOAP_PREFIX + ":Body>" + nl;
                            String s4 = nl + "</" + SOAP_PREFIX + ":Body>" + nl + "</" + SOAP_PREFIX + ":Envelope>" + nl;
                            chunks = new XmlaDataChunk[]{
                                    new ByteArrayDataChunk(s1.toString().getBytes(encoding)),
                                    new ByteArrayDataChunk(soapHeader),
                                    new ByteArrayDataChunk(s2.getBytes(encoding)),
                                    soapBody,
                                    new ByteArrayDataChunk(s4.getBytes(encoding))
                            };
                            break;
                        } else {
                            StringBuilder s1 = new StringBuilder();
                            if (!context.fromGateway) {
                                s1.append("<?xml version=\"1.0\" encoding=\"").append(encoding).append("\"?>").append(nl);
                            }
                            s1.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
                            if (soapHeader != null) {
                                s1.append(nl).append("<soap:Header>");
                            }
                            StringBuilder s2 = new StringBuilder();
                            if (soapHeader != null) {
                                s2.append("</soap:Header>").append(nl);
                            }
                            s2.append("<").append(MS_SOAP_PREFIX).append(":Body>").append(nl);
                            String s4 = nl + "</" + MS_SOAP_PREFIX + ":Body>" + nl + "</" + MS_SOAP_PREFIX + ":Envelope>" + nl;
                            chunks = new XmlaDataChunk[]{
                                    new ByteArrayDataChunk(s1.toString().getBytes(encoding)),
                                    new ByteArrayDataChunk(soapHeader),
                                    new ByteArrayDataChunk(s2.toString().getBytes(encoding)),
                                    soapBody,
                                    new ByteArrayDataChunk(s4.getBytes(encoding))
                            };
                            break;
                        }
                }
            } catch (UnsupportedEncodingException uee) {
                LOGGER.warn("This should be handled at begin of processing request", uee);
            }

            // 最终输出过程
            if (result.responseMimeType != null) {
                responseMimeType = result.responseMimeType;
            }
            try {
                int xmlResponseBodySize = writeToResponse(response, chunks, responseMimeType);
                long end = System.currentTimeMillis();
                context.runningStatistics.marshallSoapMessageTime = end - start;
                context.runningStatistics.networkPackage = xmlResponseBodySize;
            } catch (IOException ioe) {
                LOGGER.warn("Exception when transferring bytes over sockets", ioe);
            }
        } catch (XmlaException xex) {
            throw xex;
        } catch (Exception ex) {
            throw new XmlaException(SERVER_FAULT_FC, MSM_UNKNOWN_CODE, MSM_UNKNOWN_FAULT_FS, ex);
        }
    }

    private int writeToResponse(HttpServletResponse response, XmlaDataChunk[] byteChunks,
                                Enumeration.ResponseMimeType responseMimeType) throws IOException {
        // write header
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context.fromGateway) {
            response.setHeader("success", String.valueOf(context.runningStatistics.success));
        }
        // write data
        OutputStream outputStream = response.getOutputStream();
        int xmlResponseBodySize = 0;
        for (XmlaDataChunk chunk : byteChunks) {
            if (chunk instanceof CommonResultDataChunk) {
                CommonResultDataChunk queryChunk = (CommonResultDataChunk) chunk;
                queryChunk.compact = XmlaRequestContext.getContext().compactResult;
                queryChunk.encoding = response.getCharacterEncoding();
                queryChunk.responseMimeType = responseMimeType;
            }
            if (chunk != null) {
                chunk.write(outputStream);
                xmlResponseBodySize += chunk.count();
            }
        }
        outputStream.flush();
        return xmlResponseBodySize;
    }

    /**
     * This produces a SOAP 1.1 version Fault element - not a 1.2 version.
     */
    @Override
    protected void handleFault(HttpServletResponse response, XmlaResult result, Phase phase, Throwable t) {
        if (Objects.nonNull(XmlaRequestContext.getContext())) {
            throw new RuntimeException(ExceptionUtils.getRootCause(t), t.getCause());
        }
        // Regardless of whats been put into the response so far, clear
        // it out.
        response.reset();

        // NOTE: if you can think of better/other status codes to use
        // for the various phases, please make changes.
        // I think that XMLA faults always returns OK.
        switch (phase) {
            case VALIDATE_HTTP_HEAD:
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                break;
            case INITIAL_PARSE:
            case CALLBACK_PRE_ACTION:
            case PROCESS_HEADER:
            case PROCESS_BODY:
            case CALLBACK_POST_ACTION:
            case SEND_RESPONSE:
                response.setStatus(HttpServletResponse.SC_OK);
                break;
        }

        String code;
        String faultCode;
        String faultString;
        if (t instanceof XmlaException) {
            XmlaException xex = (XmlaException) t;
            faultString = xex.getFaultString();
            faultCode = XmlaException.formatFaultCode(xex);

        } else {
            // some unexpected Throwable
            code = UNKNOWN_ERROR_CODE;
            faultString = UNKNOWN_ERROR_FAULT_FS;
            faultCode = XmlaException.formatFaultCode(SERVER_FAULT_FC, code);
        }

        String encoding = response.getCharacterEncoding();

        ByteArrayOutputStream osBuf = new ByteArrayOutputStream();
        try {
            SaxWriter writer = new DefaultSaxWriter(osBuf, encoding);
            writer.startDocument();
            writer.startElement(SOAP_PREFIX + ":Fault");

            // The faultcode element is intended for use by software to provide
            // an algorithmic mechanism for identifying the fault. The faultcode
            // MUST be present in a SOAP Fault element and the faultcode value
            // MUST be a qualified name
            writer.startElement("faultcode");
            writer.characters(faultCode);
            writer.endElement();

            // The faultstring element is intended to provide a human readable
            // explanation of the fault and is not intended for algorithmic
            // processing.
            writer.startElement("faultstring");
            writer.characters(faultString);
            writer.endElement();

            // The faultactor element is intended to provide information about
            // who caused the fault to happen within the message path
            writer.startElement("faultactor");
            writer.characters(FAULT_ACTOR);
            writer.endElement();

            writer.startElement("detail");
            writer.startElement("Error", "Description", faultString, "ErrorCode", "3238658052", "Source",
                    "Microsoft SQL Server 2012 Analysis Services");
            writer.endElement(); // error
            writer.endElement(); // detail

            writer.endElement(); // </Fault>
            writer.endDocument();
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn("This should be handled at begin of processing request", uee);
        } catch (Exception e) {
            LOGGER.error("Unexcepted runtime exception when handing SOAP fault :(");
        }

        result.body = new ByteArrayDataChunk(osBuf.toByteArray());
    }

    @Override
    protected void handleSoapFinal(HttpServletResponse response, Map<String, Object> context) {
        String sessionId = (String) context.get(CONTEXT_XMLA_SESSION_ID);
        if (StringUtils.isNotBlank(sessionId)) {
            SessionManager.removeSession(sessionId);
        }
    }

}
// End DefaultXmlaServlet.java
