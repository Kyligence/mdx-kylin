/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.UUIDUtils;
import io.kylin.mdx.insight.core.support.MdxQueryMetrics;
import io.kylin.mdx.ErrorCode;
import io.micrometer.core.instrument.Counter;
import mondrian.xmla.context.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapStatement;
import org.olap4j.PreparedOlapStatement;
import org.olap4j.impl.LcidLocale;
import org.olap4j.metadata.Property;
import org.olap4j.xmla.server.impl.Pair;
import org.olap4j.xmla.server.impl.Util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static mondrian.xmla.XmlaConstants.*;
import static mondrian.xmla.constants.XmlaHandlerConstants.*;
import static mondrian.xmla.utils.XmlaHandlerUtils.*;
import static org.olap4j.metadata.XmlaConstants.*;

/**
 * An <code>XmlaHandler</code> responds to XML for Analysis (XML/A) requests.
 *
 * @author jhyde, Gang Chen
 * @since 27 April, 2003
 */
public abstract class XmlaHandler {

    protected static final Logger LOGGER = Logger.getLogger(XmlaHandler.class);

    protected static final SemanticConfig CONFIG = SemanticConfig.getInstance();

    protected static final Counter mdxQueryRequestTotal = MdxQueryMetrics.mdxQueryRequestTotal();

    private static final Counter mdxQueryRequestFail = MdxQueryMetrics.mdxQueryRequestFail();

    protected final ConnectionFactory connectionFactory;

    /**
     * Creates an <code>XmlaHandler</code>.
     *
     * <p>The connection factory may be null, as long as you override
     * {@link #getConnection(String, String, String, Properties)}.
     *
     * @param connectionFactory Connection factory
     * @param prefix            XML Namespace. Typical value is "xmla", but a value of
     *                          "cxmla" works around an Internet Explorer 7 bug
     */
    public XmlaHandler(ConnectionFactory connectionFactory, String prefix) {
        assert prefix != null;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Processes a request.
     *
     * @param request  XML request, for example, "<SOAP-ENV:Envelope ...>".
     * @param response Destination for response
     * @throws XmlaException on error
     */
    public void process(XmlaRequest request, XmlaResponse response) throws XmlaException {
        Method method = request.getMethod();
        long start = System.currentTimeMillis();
        switch (method) {
            case DISCOVER:
                discover(request, response);
                break;
            case EXECUTE:
                execute(request, response);
                break;
            default:
                throw new XmlaException(CLIENT_FAULT_FC, HSB_BAD_METHOD_CODE, HSB_BAD_METHOD_FAULT_FS,
                        new IllegalArgumentException("Unsupported XML/A method: " + method));
        }
        long end = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XmlaHandler.process: time = " + (end - start));
            LOGGER.debug("XmlaHandler.process: " + Util.printMemory());
        }
    }

    protected abstract void discover(XmlaRequest request, XmlaResponse response);

    protected abstract void execute(XmlaRequest request, XmlaResponse response);

    protected QueryResult executeDrillThroughQuery(XmlaRequest request) throws XmlaException {
        checkFormat(request);

        XmlaRequestContext context = getContextByRequest(request);
        String mdx = optimizeMdx(context, request.getStatement());

        final Map<String, String> properties = request.getProperties();
        String tabFields = properties.get(PropertyDefinition.TableFields.name());
        if (tabFields != null && tabFields.length() == 0) {
            tabFields = null;
        }
        final String advancedFlag = properties.get(PropertyDefinition.AdvancedFlag.name());
        final boolean advanced = Boolean.parseBoolean(advancedFlag);
        OlapConnection connection = null;
        OlapStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection(request, Collections.<String, String>emptyMap());
            statement = connection.createStatement();
            final XmlaExtra extra = connectionFactory.getExtra();
            final boolean enableRowCount = extra.isTotalCountEnabled();
            final int[] rowCountSlot = enableRowCount ? new int[]{0} : null;
            resultSet = extra.executeDrillthrough(statement, mdx, advanced, tabFields, rowCountSlot);
            int rowCount = enableRowCount ? rowCountSlot[0] : -1;
            return new TabularRowSet(resultSet, rowCount);
        } catch (XmlaException xex) {
            throw xex;
        } catch (SQLException sqle) {
            throw new XmlaException(SERVER_FAULT_FC, HSB_DRILL_THROUGH_SQL_CODE, HSB_DRILL_THROUGH_SQL_FAULT_FS,
                    Util.newError(sqle, "Error in drill through"), ErrorCode.DRILLTHROUGH_ERROR);
        } catch (RuntimeException e) {
            // NOTE: One important error is "cannot drill through on the cell"
            throw new XmlaException(SERVER_FAULT_FC, HSB_DRILL_THROUGH_SQL_CODE, HSB_DRILL_THROUGH_SQL_FAULT_FS, e);
        } finally {
            close(resultSet, statement, connection);
        }
    }

    protected QueryResult executeQuery(XmlaRequest request) throws XmlaException {
        String mdx = request.getStatement();
        if ((mdx == null) || (mdx.length() == 0)) {
            return null;
        }
        // The total number of queries + 1
        mdxQueryRequestTotal.increment();
        XmlaRequestContext context = getContextByRequest(request);
        context.mdxQuery = mdx;

        if (StringUtils.isBlank(context.runningStatistics.queryID)) {
            context.runningStatistics.queryID = UUIDUtils.randomUUID();
        }
        String qid = "[Query " + context.runningStatistics.queryID + "] ";
        MDC.put("qid", qid);
        context.setParameter("qid", qid);

        // extended query is like REFRESH CUBE, select * from $system.xxx
        Pair<Boolean, QueryResult> pair = checkAndExecuteExtendedQuery(context, request);
        Boolean isExecuted = pair.getLeft();
        if (isExecuted != null && isExecuted) {
            return pair.getRight();
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("mdx: \"" + mdx + "\"");
        }

        if (context.mdxRewriter != null) {
            Pair<Boolean, String> result = context.mdxRewriter.rewrite(mdx);
            if (Pair.isPass(result)) {
                mdx = result.getRight();
                context.mdxQueryRewritten = mdx;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("rewrite mdx: \"" + mdx + "\"");
                }
            }
        }

        mdx = optimizeMdx(context, mdx);

        context.runningStatistics.beforeConnectionTime = System.currentTimeMillis() - context.runningStatistics.start;

        checkFormat(request);

        OlapConnection connection = null;
        PreparedOlapStatement statement = null;
        CellSet cellSet = null;
        boolean success = false;
        Throwable tempEx = null;
        try {
            long beforeConnection = System.currentTimeMillis();
            connection = getConnection(request, Collections.emptyMap());
            context.runningStatistics.connectionTime = System.currentTimeMillis() - beforeConnection;
            // @see https://github.com/olap4j/olap4j-xmlaserver/issues/9
            final XmlaExtra extra = connectionFactory.getExtra();
            extra.setPreferList(connection);
            try {
                statement = connection.prepareOlapStatement(mdx);
            } catch (XmlaException ex) {
                tempEx = ex;
                throw ex;
            } catch (Exception ex) {
                tempEx = ex;
                throw new XmlaException(CLIENT_FAULT_FC, HSB_PARSE_QUERY_CODE, HSB_PARSE_QUERY_FAULT_FS, ex);
            }
            if (request.getSessionId() != null) {
                connectionFactory.putStatementWithSession(request.getSessionId(), statement);
            }

            if (CONFIG.isEnableCheckReject() && context.mdxRejecter != null) {
                if (context.mdxRejecter.isReject(statement)) {
                    context.redirectMdx = context.mdxRejecter.redirect();
                    return null;
                }
            }

            try {
                cellSet = statement.executeQuery();

                final Format format = getFormat(request, null);
                final Content content = getContent(request);
                final Enumeration.ResponseMimeType responseMimeType = getResponseMimeType(request);
                final MDDataSet dataSet;

                if (format == Format.Tabular) {
                    dataSet = new MDDataSet_Tabular(cellSet, connection);
                } else {
                    dataSet = new MDDataSet_Multidimensional(cellSet, connection, extra,
                            content != Content.DataIncludeDefaultSlicer,
                            responseMimeType == Enumeration.ResponseMimeType.JSON);
                }
                success = true;
                return dataSet;
            } catch (XmlaException ex) {
                tempEx = ex;
                throw ex;
            } catch (Exception ex) {
                tempEx = ex;
                throw new XmlaException(SERVER_FAULT_FC, HSB_EXECUTE_QUERY_CODE, HSB_EXECUTE_QUERY_FAULT_FS, ex);
            }
        } finally {
            context.runningStatistics.success = success && context.errorMsg == null;
            if (!success && tempEx != null) {
                context.runningStatistics.checkException(tempEx);
            }
            if (!success) {
                // Number of query failures
                mdxQueryRequestFail.increment();
                close(cellSet, statement, connection);
            }
            if (request.getSessionId() != null) {
                connectionFactory.removeStatementWithSession(request.getSessionId());
            }
        }
    }

    private String optimizeMdx(XmlaRequestContext context, String mdx) {
        if (context != null && context.mdxOptimizer != null) {
            mdx = context.mdxOptimizer.rewriteMdx(mdx);
            context.mdxQueryOptimized = mdx;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("optimize mdx: \"" + mdx + "\"");
            }
        }
        return mdx;
    }

    private void checkFormat(XmlaRequest request) throws XmlaException {
        // Check response's rowset format in request
        final Map<String, String> properties = request.getProperties();
        if (request.isDrillThrough()) {
            Format format = getFormat(request, null);
            if (format != Format.Tabular) {
                throw new XmlaException(CLIENT_FAULT_FC, HSB_DRILL_THROUGH_FORMAT_CODE,
                        HSB_DRILL_THROUGH_FORMAT_FAULT_FS, new UnsupportedOperationException(
                        "<Format>: only 'Tabular' allowed when drilling " + "through"), ErrorCode.DRILLTHROUGH_ONLY_SUPPORT_TABULAR_FORMAT);
            }
        } else {
            final String axisFormatName = properties.get(PropertyDefinition.AxisFormat.name());
            if (axisFormatName != null) {
                AxisFormat axisFormat = Util.lookup(AxisFormat.class, axisFormatName, null);

                if (axisFormat != AxisFormat.TupleFormat) {
                    throw new UnsupportedOperationException("<AxisFormat>: only 'TupleFormat' currently supported");
                }
            }
        }
    }

    private Pair<Boolean, QueryResult> checkAndExecuteExtendedQuery(XmlaRequestContext context, XmlaRequest request) {
        // check for refresh cube
        String mdx = request.getStatement();
        if (mdx.startsWith("REFRESH CUBE")) {
            if (context != null) {
                context.runningStatistics.success = true;
                context.runningStatistics.datasetName = mdx.substring(14, mdx.length() - 1);
            }
            return new Pair<>(true, null);
        }
        return new Pair<>(false, null);
    }

    private static void setLocale(String localeIdentifier, Properties props) {
        if (localeIdentifier != null) {
            try {
                if (localeIdentifier.equals("1024")) {
                    localeIdentifier = "1033";
                }
                final short lcid = Short.parseShort(localeIdentifier);
                final Locale locale = LcidLocale.lcidToLocale(lcid);
                if (locale != null) {
                    props.put(JDBC_LOCALE, locale.toString());
                }
            } catch (NumberFormatException nfe) {
                try {
                    Locale locale = Util.parseLocale(localeIdentifier);
                    props.put(JDBC_LOCALE, locale.toString());
                } catch (RuntimeException re) {
                    // probably a bad locale string; fall through
                }
            }
        }
    }

    /**
     * Returns a new OlapConnection opened with the credentials specified in the
     * XMLA request or an existing connection if one can be found associated
     * with the request session id.
     *
     * @param request Request
     * @param propMap Extra properties
     */
    public OlapConnection getConnection(XmlaRequest request, Map<String, String> propMap) {
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            // With a Simba O2X Client session ID is only null when
            // serving "discover datasources".
            //
            // Let's have a magic ID for the non-authenticated session.
            //
            // REVIEW: Security hole?
            sessionId = "<no_session>";
        }
        LOGGER.debug(
                "Creating new connection for user [" + request.getUsername() + "] and session [" + sessionId + "]");

        Properties props = new Properties();
        for (Map.Entry<String, String> entry : propMap.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        if (request.getUsername() != null) {
            props.put(JDBC_USER, request.getUsername());
        }
        if (request.getPassword() != null) {
            props.put(JDBC_PASSWORD, request.getPassword());
        }
        String localeIdentifier = request.getProperties().get(PropertyDefinition.LocaleIdentifier.name());
        setLocale(localeIdentifier, props);

        final String databaseName = request.getProperties().get(PropertyDefinition.DataSourceInfo.name());

        String catalogName = request.getProperties().get(PropertyDefinition.Catalog.name());

        if (catalogName == null) {
            catalogName = XmlaRequestContext.getContext().currentCatalog;
        }

        if (catalogName == null && request.getMethod() == Method.DISCOVER
                && request.getRestrictions().containsKey(Property.StandardMemberProperty.CATALOG_NAME.name())) {
            Object restriction = request.getRestrictions().get(Property.StandardMemberProperty.CATALOG_NAME.name());
            if (restriction instanceof List) {
                final List requiredValues = (List) restriction;
                catalogName = String.valueOf(requiredValues.get(0));
            } else {
                throw Util.newInternal("unexpected restriction type: " + restriction.getClass());
            }
        }

        return getConnection(databaseName, catalogName, request.getRoleName(), props);
    }

    /**
     * Gets a Connection given a catalog (and implicitly the catalog's data
     * source) and the name of a user role.
     *
     * <p>If you want to pass in a role object, and you are making the call
     * within the same JVM (i.e. not RPC), register the role using
     * {@code mondrian.olap.MondrianServer.getLockBox()} and pass in the moniker
     * for the generated lock box entry. The server will retrieve the role from
     * the moniker.
     *
     * @param catalog Catalog name
     * @param schema  Schema name
     * @param role    User role name
     * @return Connection
     * @throws XmlaException If error occurs
     */
    protected OlapConnection getConnection(String catalog, String schema, final String role) throws XmlaException {
        return this.getConnection(catalog, schema, role, new Properties());
    }

    /**
     * Gets a Connection given a catalog (and implicitly the catalog's data
     * source) and the name of a user role.
     *
     * <p>If you want to pass in a role object, and you are making the call
     * within the same JVM (i.e. not RPC), register the role using
     * {@code mondrian.olap.MondrianServer.getLockBox()} and pass in the moniker
     * for the generated lock box entry. The server will retrieve the role from
     * the moniker.
     *
     * @param catalog Catalog name
     * @param schema  Schema name
     * @param role    User role name
     * @param props   Properties to pass down to the native driver.
     * @return Connection
     * @throws XmlaException If error occurs
     */
    protected OlapConnection getConnection(String catalog, String schema, final String role, Properties props)
            throws XmlaException {
        try {
            return connectionFactory.getConnection(catalog, schema, role, props);
        } catch (SecurityException e) {
            throw new XmlaException(CLIENT_FAULT_FC, HSB_ACCESS_DENIED_CODE, HSB_ACCESS_DENIED_FAULT_FS, e);
        } catch (SQLException e) {
            throw new XmlaException(CLIENT_FAULT_FC, HSB_CONNECTION_DATA_SOURCE_CODE,
                    HSB_CONNECTION_DATA_SOURCE_FAULT_FS, e);
        }
    }

    private static void close(ResultSet resultSet, OlapStatement statement, OlapConnection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

}

// End XmlaHandler.java
