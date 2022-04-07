/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla.impl;

import mondrian.xmla.*;

import org.olap4j.xmla.server.impl.Util;

import org.apache.log4j.Logger;

import org.w3c.dom.*;

import java.util.*;

import static org.olap4j.metadata.XmlaConstants.Method;

/**
 * Default implementation of {@link mondrian.xmla.XmlaRequest} by DOM API.
 *
 * @author Gang Chen
 */
public class DefaultXmlaRequest
    implements XmlaRequest, XmlaConstants
{
    private static final Logger LOGGER =
        Logger.getLogger(DefaultXmlaRequest.class);

    private static final String MSG_INVALID_XMLA = "Invalid XML/A message";

    public static final String DRILLTHROUGH_PREFIX = "DRILLTHROUGH";

    /* common content */
    protected Method method;
    protected Map<String, String> properties;
    private final String roleName;

    /* EXECUTE content */
    protected boolean cancel;
    protected String statement;
    protected boolean drillthrough;
    protected Map<String, String> parameters;

    /* DISCOVER contnet */
    protected String requestType;
    protected Map<String, Object> restrictions;

    protected final String username;
    protected final String password;
    protected final String sessionId;

    public DefaultXmlaRequest(
        final Element xmlaRoot,
        final String roleName,
        final String username,
        final String password,
        final String sessionId)
        throws XmlaException
    {
        init(xmlaRoot);
        this.roleName = roleName;
        this.username = username;
        this.password = password;
        this.sessionId = sessionId;
    }

    public DefaultXmlaRequest(Method method, Map<String, String> properties, String statement, boolean drillthrough,
                              String requestType, Map<String, Object> restrictions, Map<String, String> parameters,
                              String roleName, String username, String password, String sessionId) throws XmlaException {
        this.method = method;
        this.properties = properties;
        this.statement = statement;
        this.drillthrough = drillthrough;
        this.requestType = requestType;
        this.restrictions = restrictions;
        this.parameters = parameters;
        this.roleName = roleName;
        this.username = username;
        this.password = password;
        this.sessionId = sessionId;
    }

    protected DefaultXmlaRequest(String roleName, String username, String password, String sessionId) {
        this.roleName = roleName;
        this.username = username;
        this.password = password;
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Method getMethod() {
        return method;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, Object> getRestrictions() {
        if (method != Method.DISCOVER) {
            throw new IllegalStateException(
                "Only METHOD_DISCOVER has restrictions");
        }
        return restrictions;
    }

    @Override
    public boolean isCancel() {
        return cancel;
    }

    public String getStatement() {
        if (method != Method.EXECUTE) {
            throw new IllegalStateException(
                "Only METHOD_EXECUTE has statement");
        }
        return statement;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRequestType() {
        if (method != Method.DISCOVER) {
            throw new IllegalStateException(
                "Only METHOD_DISCOVER has requestType");
        }
        return requestType;
    }

    public boolean isDrillThrough() {
        if (method != Method.EXECUTE) {
            throw new IllegalStateException(
                "Only METHOD_EXECUTE determines drillthrough");
        }
        return drillthrough;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    protected final void init(Element xmlaRoot) throws XmlaException {
        if (NS_XMLA.equals(xmlaRoot.getNamespaceURI())) {
            String lname = xmlaRoot.getLocalName();
            if ("Discover".equals(lname)) {
                method = Method.DISCOVER;
                initDiscover(xmlaRoot);
            } else if ("Execute".equals(lname)) {
                method = Method.EXECUTE;
                initExecute(xmlaRoot);
            } else {
                // Note that is code will never be reached because
                // the error will be caught in
                // DefaultXmlaServlet.handleSoapBody first
                StringBuilder buf = new StringBuilder(100);
                buf.append(MSG_INVALID_XMLA);
                buf.append(": Bad method name \"");
                buf.append(lname);
                buf.append("\"");
                throw new XmlaException(
                    CLIENT_FAULT_FC,
                    HSB_BAD_METHOD_CODE,
                    HSB_BAD_METHOD_FAULT_FS,
                    Util.newError(buf.toString()));
            }
        } else {
            // Note that is code will never be reached because
            // the error will be caught in
            // DefaultXmlaServlet.handleSoapBody first
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Bad namespace url \"");
            buf.append(xmlaRoot.getNamespaceURI());
            buf.append("\"");
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_METHOD_NS_CODE,
                HSB_BAD_METHOD_NS_FAULT_FS,
                Util.newError(buf.toString()));
        }
    }

    private void initDiscover(Element discoverRoot) throws XmlaException {
        Element[] childElems =
            XmlaUtil.filterChildElements(
                discoverRoot,
                NS_XMLA,
                "RequestType");
        if (childElems.length != 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of RequestType elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_REQUEST_TYPE_CODE,
                HSB_BAD_REQUEST_TYPE_FAULT_FS,
                Util.newError(buf.toString()));
        }
        requestType = XmlaUtil.textInElement(childElems[0]); // <RequestType>

        childElems =
            XmlaUtil.filterChildElements(
                discoverRoot,
                NS_XMLA,
                "Properties");
        if (childElems.length != 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of Properties elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_PROPERTIES_CODE,
                HSB_BAD_PROPERTIES_FAULT_FS,
                Util.newError(buf.toString()));
        }
        initProperties(childElems[0]); // <Properties><PropertyList>

        childElems =
            XmlaUtil.filterChildElements(
                discoverRoot,
                NS_XMLA,
                "Restrictions");
        if (childElems.length != 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of Restrictions elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_RESTRICTIONS_CODE,
                HSB_BAD_RESTRICTIONS_FAULT_FS,
                Util.newError(buf.toString()));
        }
        initRestrictions(childElems[0]); // <Restriciotns><RestrictionList>
    }

    private void initExecute(Element executeRoot) throws XmlaException {
        Element[] childElems =
            XmlaUtil.filterChildElements(
                executeRoot,
                NS_XMLA,
                "Command");
        if (childElems.length != 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of Command elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_COMMAND_CODE,
                HSB_BAD_COMMAND_FAULT_FS,
                Util.newError(buf.toString()));
        }
        initCommand(childElems[0]); // <Command><Statement>

        childElems =
            XmlaUtil.filterChildElements(
                executeRoot,
                NS_XMLA,
                "Properties");
        if (childElems.length != 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of Properties elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_PROPERTIES_CODE,
                HSB_BAD_PROPERTIES_FAULT_FS,
                Util.newError(buf.toString()));
        }
        initProperties(childElems[0]); // <Properties><PropertyList>

        //fill the missing cube name if there is a catalog available in the property list (only for Excel)
        if (XmlaRequestContext.getContext().clientType.equals(XmlaRequestContext.ClientType.MSOLAP)
                && !statement.contains("[MEMBER_UNIQUE_NAME],[MEMBER_ORDINAL],[MEMBER_CAPTION]")
                && properties != null && properties.containsKey("Catalog")) {
            statement = statement.replaceFirst("FROM\\s+CELL PROPERTIES",
                    "FROM [" + properties.get("Catalog") + "] CELL PROPERTIES");
        }

        childElems = XmlaUtil.filterChildElements(executeRoot, NS_XMLA, "Parameters");
        if (childElems.length > 1) {
            String msg = String.format("%s: Wrong number of Parameters elements: %d",
                        MSG_INVALID_XMLA, childElems.length);
            throw new XmlaException(CLIENT_FAULT_FC, HSB_BAD_PARAMETERS_CODE, HSB_BAD_PARAMETERS_FAULT_FS, Util.newError(msg));
        } else if (childElems.length == 1) {
            initParameters(childElems[0]); // <Parameters><Parameter>
        }
    }

    private void initRestrictions(Element restrictionsRoot)
        throws XmlaException
    {
        Map<String, List<String>> restrictions =
            new HashMap<String, List<String>>();
        Element[] childElems =
            XmlaUtil.filterChildElements(
                restrictionsRoot,
                NS_XMLA,
                "RestrictionList");
        if (childElems.length == 1) {
            NodeList nlst = childElems[0].getChildNodes();
            for (int i = 0, nlen = nlst.getLength(); i < nlen; i++) {
                Node n = nlst.item(i);
                if (n instanceof Element) {
                    Element e = (Element) n;
                    if (NS_XMLA.equals(e.getNamespaceURI())) {
                        String key = e.getLocalName();
                        String value = XmlaUtil.textInElement(e);
                        List<String> values;
                        if (restrictions.containsKey(key)) {
                            values = restrictions.get(key);
                        } else {
                            values = new ArrayList<String>();
                            restrictions.put(key, values);
                        }

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                "DefaultXmlaRequest.initRestrictions: "
                                + " key=\""
                                + key
                                + "\", value=\""
                                + value
                                + "\"");
                        }
                        if (value == null || value.length() == 0) {
                            Element[] valElements = XmlaUtil.filterChildElements(
                                    e,
                                    NS_XMLA,
                                    "Value");
                            for (Element val : valElements) {
                                values.add(XmlaUtil.textInElement(val));
                            }
                        } else {
                            values.add(value);
                        }

                    }
                }
            }
        } else if (childElems.length > 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of RestrictionList elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_RESTRICTION_LIST_CODE,
                HSB_BAD_RESTRICTION_LIST_FAULT_FS,
                Util.newError(buf.toString()));
        }

        // If there is a Catalog property,
        // we have to consider it a constraint as well.
        String key =
            org.olap4j.metadata.XmlaConstants
                .Literal.CATALOG_NAME.name();

        if (this.properties.containsKey(key)
            && !restrictions.containsKey(key))
        {
            List<String> values;
            values = new ArrayList<String>();
            restrictions.put(this.properties.get(key), values);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "DefaultXmlaRequest.initRestrictions: "
                    + " key=\""
                    + key
                    + "\", value=\""
                    + this.properties.get(key)
                    + "\"");
            }
        }

        this.restrictions = (Map) Collections.unmodifiableMap(restrictions);
    }

    private void initProperties(Element propertiesRoot) throws XmlaException {
        Map<String, String> properties = new HashMap<String, String>();
        Element[] childElems =
            XmlaUtil.filterChildElements(
                propertiesRoot,
                NS_XMLA,
                "PropertyList");
        if (childElems.length == 1) {
            NodeList nlst = childElems[0].getChildNodes();
            for (int i = 0, nlen = nlst.getLength(); i < nlen; i++) {
                Node n = nlst.item(i);
                if (n instanceof Element) {
                    Element e = (Element) n;
                    if (NS_XMLA.equals(e.getNamespaceURI())) {
                        String key = e.getLocalName();
                        String value = XmlaUtil.textInElement(e);

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                "DefaultXmlaRequest.initProperties: "
                                + " key=\""
                                + key
                                + "\", value=\""
                                + value
                                + "\"");
                        }

                        properties.put(key, value);
                    }
                }
            }
        } else if (childElems.length > 1) {
            StringBuilder buf = new StringBuilder(100);
            buf.append(MSG_INVALID_XMLA);
            buf.append(": Wrong number of PropertyList elements: ");
            buf.append(childElems.length);
            throw new XmlaException(
                CLIENT_FAULT_FC,
                HSB_BAD_PROPERTIES_LIST_CODE,
                HSB_BAD_PROPERTIES_LIST_FAULT_FS,
                Util.newError(buf.toString()));
        } else {
        }
        this.properties = Collections.unmodifiableMap(properties);
    }


    private void initParameters(Element parameterRoot) throws XmlaException {
        Map<String, String> parameters = new HashMap<String, String>();

        Element[] childElements = XmlaUtil.filterChildElements(parameterRoot, NS_XMLA, "Parameter");
        for (Element parameterElement : childElements) {
            Element[] nameElements = XmlaUtil.filterChildElements(parameterElement, null, "Name");
            Element[] valueElements = XmlaUtil.filterChildElements(parameterElement, null, "Value");

            if (nameElements.length != 1 || valueElements.length != 1) {
                String msg = String.format("%s: Wrong number of Parameter's children elements: {Name: %d, Value: %d}",
                        MSG_INVALID_XMLA, nameElements.length, valueElements.length);
                throw new XmlaException(CLIENT_FAULT_FC, HSB_BAD_PARAMETERS_CODE, HSB_BAD_PARAMETERS_FAULT_FS, Util.newError(msg));
            }

            String name = XmlaUtil.textInElement(nameElements[0]);
            String value = XmlaUtil.textInElement(valueElements[0]);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("DefaultXmlaRequest.initParameters:  name=\"%s\", value=\"%s\"", name, value));
            }

            parameters.put(name, value);
        }
        this.parameters = Collections.unmodifiableMap(parameters);
    }


    private void initCommand(Element commandRoot) throws XmlaException {
        Element[] childElems =
            XmlaUtil.filterChildElements(
                commandRoot,
                NS_XMLA,
                "Statement");
        Element[] cancelElems = XmlaUtil.filterChildElements(commandRoot, NS_XMLA_DDL, "Cancel");

        if (cancelElems.length == 1) {
            cancel = true;
            statement = "";
            drillthrough = false;
        } else if (childElems.length != 1) {
            String buf = MSG_INVALID_XMLA +
                    ": Wrong number of Statement elements: " +
                    childElems.length;
            throw new XmlaException(
                    CLIENT_FAULT_FC,
                    HSB_BAD_STATEMENT_CODE,
                    HSB_BAD_STATEMENT_FAULT_FS,
                    Util.newError(buf));
        } else {
            statement = XmlaUtil.textInElement(childElems[0]).replaceAll("\\r", "");
            drillthrough = statement.toUpperCase().contains(DRILLTHROUGH_PREFIX);
        }
    }
}

// End DefaultXmlaRequest.java
