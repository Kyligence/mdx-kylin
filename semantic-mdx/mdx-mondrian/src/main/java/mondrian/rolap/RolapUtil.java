/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2014 Pentaho and others
// All Rights Reserved.
//
// jhyde, 22 December, 2001
*/
package mondrian.rolap;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.support.MdxQueryMetrics;
import io.kylin.mdx.insight.core.sync.DimensionCardinality;
import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.rolap.cache.HierarchyCache;
import io.kylin.mdx.rolap.cache.HierarchyMemberTree;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import mondrian.calc.ExpCompiler;
import mondrian.mdx.MemberExpr;
import mondrian.mdx.NamedSetExpr;
import mondrian.mdx.ResolvedFunCall;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.*;
import mondrian.resource.MondrianResource;
import mondrian.rolap.cache.FixedCachedRowSetImpl;
import mondrian.server.Execution;
import mondrian.server.Locus;
import mondrian.server.Statement;
import mondrian.spi.Dialect;
import mondrian.util.ClassResolver;
import mondrian.xmla.XmlaRequestContext;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.eigenbase.util.property.StringProperty;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static mondrian.olap.fun.FunUtil.compareSiblingMembersByName;

/**
 * Utility methods for classes in the <code>mondrian.rolap</code> package.
 *
 * @author jhyde
 * @since 22 December, 2001
 */
public class RolapUtil {
    public static final Logger MDX_LOGGER = Logger.getLogger("mondrian.mdx");
    public static final Logger SQL_LOGGER = Logger.getLogger("mondrian.sql");
    public static final Logger MONITOR_LOGGER =
            Logger.getLogger("mondrian.server.monitor");
    public static final Logger PROFILE_LOGGER =
            Logger.getLogger("mondrian.profile");

    static final Logger LOGGER = Logger.getLogger(RolapUtil.class);
    public static final Timer sqlExecutedTimer = MdxQueryMetrics.sqlExecutedTimer();
    protected static final Counter sqlExecutedTotal = MdxQueryMetrics.sqlExecutedTotal();
    public static final String MDC_KEY = "qid";

    /**
     * Special cell value indicates that the value is not in cache yet.
     */
    public static final Object valueNotReadyException = RolapResultUtil.ValueNotReadyObject.INSTANCE;

    /**
     * Hook to run when a query is executed. This should not be
     * used at runtime but only for testing.
     */
    private static ExecuteQueryHook queryHook = null;

    /**
     * Special value that represents a null key.
     */
    public static final Comparable sqlNullValue = RolapUtilComparable.INSTANCE;

    /** Name of member that has null key. */
    public static final String NULL_NAME = MondrianProperties.instance().NullMemberRepresentation.get();

    /** Operator for Type Boolean */
    public static final String OP_BOOLEAN = " is ";

    public static final String DEFAULT_FORMAT_STRING = "regular";

    public static final String DEFAULT_FORMAT_STRING_TAG = SemanticConfig.getInstance().formatStringDefaultValue();

    /**
     * Wraps a schema reader in a proxy so that each call to schema reader
     * has a locus for profiling purposes.
     *
     * @param connection Connection
     * @param schemaReader Schema reader
     * @return Wrapped schema reader
     */
    public static SchemaReader locusSchemaReader(
            RolapConnection connection,
            final SchemaReader schemaReader)
    {
        final Statement statement = connection.getInternalStatement();
        final Execution execution = new Execution(statement, 0);
        final Locus locus =
                new Locus(
                        execution,
                        "Schema reader",
                        null);
        return (SchemaReader) Proxy.newProxyInstance(
                SchemaReader.class.getClassLoader(),
                new Class[]{SchemaReader.class},
                new InvocationHandler() {
                    public Object invoke(
                            Object proxy,
                            Method method,
                            Object[] args)
                            throws Throwable
                    {
                        Locus.push(locus);
                        try {
                            return method.invoke(schemaReader, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        } finally {
                            Locus.pop(locus);
                        }
                    }
                }
        );
    }

    /**
     * Sets the query-execution hook used by tests. This method and
     * {@link #setHook(mondrian.rolap.RolapUtil.ExecuteQueryHook)} are
     * synchronized to ensure a memory barrier.
     *
     * @return Query execution hook
     */
    public static synchronized ExecuteQueryHook getHook() {
        return queryHook;
    }

    public static synchronized void setHook(ExecuteQueryHook hook) {
        queryHook = hook;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find all members from an MDX AST.
     */
    public static void findMembersFromExp(Exp exp, Set<Member> members) {
        if (exp instanceof MemberExpr) {
            members.add(((MemberExpr) exp).getMember());
            return;
        }
        if (exp instanceof ResolvedFunCall) {
            for (Exp arg : ((ResolvedFunCall) exp).getArgs()) {
                findMembersFromExp(arg, members);
            }
        }
        if (exp instanceof NamedSetExpr) {
            findMembersFromExp(((NamedSetExpr) exp).getNamedSet().getExp(), members);
        }
    }

    public static Exp makeAggregateExpr(final List<? extends Member> childMemberList) {
        Exp[] memberExprs = new Exp[childMemberList.size()];
        for (int i = 0; i < childMemberList.size(); i++) {
            final Member childMember = childMemberList.get(i);
            memberExprs[i] = new MemberExpr(childMember);
        }
        return new UnresolvedFunCall(
                "Aggregate",
                new Exp[] {
                        new UnresolvedFunCall(
                                "{}",
                                Syntax.Braces,
                                memberExprs)
                });
    }

    /**
     * Comparable value, equal only to itself. Used to represent the NULL value,
     * as returned from a SQL query.
     *
     * @see #mdxNullLiteral()
     */
    private static final class RolapUtilComparable
            implements Comparable, Serializable
    {
        private static final long serialVersionUID = -2595758291465179116L;

        public static final RolapUtilComparable INSTANCE =
                new RolapUtilComparable();

        // singleton
        private RolapUtilComparable() {
        }

        // do not override equals and hashCode -- use identity

        @Override
        public String toString() {
            return NULL_NAME;
        }

        @Override
        public int compareTo(Object o) {
            // collates after everything (except itself)
            return o == this ? 0 : -1;
        }
    }

    /**
     * A comparator singleton instance which can handle the presence of
     * {@link RolapUtilComparable} instances in a collection.
     */
    public static final Comparator ROLAP_COMPARATOR =
            new RolapUtilComparator();

    private static final class RolapUtilComparator<T extends Comparable<T>>
            implements Comparator<T>
    {
        @Override
        public int compare(T o1, T o2) {
            try {
                return o1.compareTo(o2);
            } catch (ClassCastException cce) {
                if (o2 == RolapUtilComparable.INSTANCE) {
                    return 1;
                }
                throw new MondrianException(cce);
            }
        }
    }

    /**
     * Runtime NullMemberRepresentation property change not taken into
     * consideration
     */
    private static String mdxNullLiteral = null;

    public static final String SQL_FALSE_LITERAL = "FALSE";

    public static String mdxNullLiteral() {
        if (mdxNullLiteral == null) {
            reloadNullLiteral();
        }
        return mdxNullLiteral;
    }

    public static void reloadNullLiteral() {
        mdxNullLiteral =
                MondrianProperties.instance().NullMemberRepresentation.get();
    }

    /**
     * Names of classes of drivers we've loaded (or have tried to load).
     *
     * <p>NOTE: Synchronization policy: Lock the {@link RolapConnection} class
     * before modifying or using this member.
     */
    private static final Set<String> loadedDrivers = new HashSet<String>();

    public static <T> long getMaxCardinality(Collection<T> levels, Function<T, RolapCubeLevel> levelConverter) {
        return levels.stream()
                .map(levelConverter)
                .filter(level -> !level.isAll() && !level.isMeasure())
                .mapToLong(RolapUtil::getCardinality)
                .max().orElse(1L);
    }

    public static long getMaxCardinality(Collection<RolapCubeLevel> levels) {
        return levels.stream()
                .filter(level -> !level.isAll() && !level.isMeasure())
                .mapToLong(RolapUtil::getCardinality)
                .max().orElse(1L);
    }

    public static long getCardinality(RolapCubeLevel level) {
        if (level.getAttribute() instanceof RolapSharedAttribute) {
            return 1L;
        }

        RolapCubeHierarchy hierarchy = level.getHierarchy();
        HierarchyCache hierarchyCache = CacheManager.getCacheManager().getHierarchyCache();
        if (hierarchyCache.isCacheEnabled()) {
            HierarchyMemberTree tree = hierarchyCache.getMemberTreeIfPresent(hierarchy);
            if (tree != null) {
                return tree.getMembersCountByLevelDepth(hierarchy.getLevelList().size() - 1);
            }
        }

        return getCardinalityFromMetaData(level).orElse(0L);
    }

    public static OptionalLong getCardinalityFromMetaData(RolapCubeLevel level) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        Map<String, Long> cardinalityMap =
                DimensionCardinality.getCardinalityMap(context.currentProject, context.currentCatalog);

        RolapSchema.PhysRelation keyRelation = level.getDimension().getKeyTable();
        if (!(keyRelation instanceof RolapSchema.PhysTable)) {
            return OptionalLong.empty();
        }
        String key = String.format("[%s].%s",
                ((RolapSchema.PhysTable)keyRelation).getSchemaName(),
                level.getAttribute().getUniqueName());
        Long result = cardinalityMap.get(key);
        return result == null ? OptionalLong.empty() : OptionalLong.of(result);
    }

    static RolapMember lookupMember(
            MemberReader reader,
            List<Id.Segment> uniqueNameParts,
            boolean failIfNotFound)
    {
        RolapMember member =
                lookupMemberInternal(
                        uniqueNameParts, null, reader, failIfNotFound);
        if (member != null) {
            return member;
        }

        // If this hierarchy has an 'all' member, we can omit it.
        // For example, '[Gender].[(All Gender)].[F]' can be abbreviated
        // '[Gender].[F]'.
        final List<RolapMember> rootMembers = reader.getRootMembers();
        if (rootMembers.size() == 1) {
            final RolapMember rootMember = rootMembers.get(0);
            if (rootMember.isAll()) {
                member =
                        lookupMemberInternal(
                                uniqueNameParts, rootMember, reader, failIfNotFound);
            }
        }
        return member;
    }

    private static RolapMember lookupMemberInternal(
            List<Id.Segment> segments,
            RolapMember member,
            MemberReader reader,
            boolean failIfNotFound)
    {
        for (Id.Segment segment : segments) {
            if (!(segment instanceof Id.NameSegment)) {
                break;
            }
            final Id.NameSegment nameSegment = (Id.NameSegment) segment;
            List<RolapMember> children;
            if (member == null) {
                children = reader.getRootMembers();
            } else {
                children = new ArrayList<RolapMember>();
                reader.getMemberChildren(member, children);
                member = null;
            }
            for (RolapMember child : children) {
                if (child.getName().equals(nameSegment.name)) {
                    member = child;
                    break;
                }
            }
            if (member == null) {
                break;
            }
        }
        if (member == null && failIfNotFound) {
            throw MondrianResource.instance().MdxCantFindMember.ex(
                    Util.implode(segments));
        }
        return member;
    }

    public static Exp pathExtract(Exp exp, Object... path) {
        for (int i = 0; i < path.length; i += 2) {
            if (!(exp instanceof ResolvedFunCall)) {
                return null;
            }

            ResolvedFunCall resolvedFunCall = (ResolvedFunCall)exp;
            if (path[i] instanceof String) {
                String funName = (String)path[i];
                if (!funName.equalsIgnoreCase(resolvedFunCall.getFunName())) {
                    return null;
                }
            } else {
                Class<? extends FunDef> funClass = (Class<? extends FunDef>)path[i];
                if (!funClass.isInstance(resolvedFunCall.getFunDef())) {
                    return null;
                }
            }

            int argIndex = (int)path[i + 1];
            exp = ((ResolvedFunCall)exp).getArg(argIndex);
        }
        return exp;
    }

    /**
     * Executes a query, printing to the trace log if tracing is enabled.
     *
     * <p>If the query fails, it wraps the {@link SQLException} in a runtime
     * exception with <code>message</code> as description, and closes the result
     * set.
     *
     * <p>If it succeeds, the caller must call the {@link SqlStatement#close}
     * method of the returned {@link SqlStatement}.
     *
     * @param dataSource DataSource
     * @param sql SQL string
     * @param locus Locus of execution
     * @return ResultSet
     */
    public static SqlStatement executeQuery(
            DataSource dataSource,
            String sql,
            Locus locus) {
        return executeQuery(dataSource, sql, null, 0, 0, locus, -1, -1, null);
    }

    /**
     * Executes a query.
     *
     * <p>If the query fails, it wraps the {@link SQLException} in a runtime
     * exception with <code>message</code> as description, and closes the result
     * set.
     *
     * <p>If it succeeds, the caller must call the {@link SqlStatement#close}
     * method of the returned {@link SqlStatement}.
     *
     *
     * @param dataSource DataSource
     * @param sql SQL string
     * @param types Suggested types of columns, or null;
     *     if present, must have one element for each SQL column;
     *     each not-null entry overrides deduced JDBC type of the column
     * @param maxRowCount Maximum number of rows to retrieve, <= 0 if unlimited
     * @param firstRowOrdinal Ordinal of row to skip to (1-based), or 0 to
     *   start from beginning
     * @param locus Execution context of this statement
     * @param resultSetType Result set type, or -1 to use default
     * @param resultSetConcurrency Result set concurrency, or -1 to use default
     * @return ResultSet
     */
    public static SqlStatement executeQuery(
            DataSource dataSource,
            String sql,
            List<SqlStatement.Type> types,
            int maxRowCount,
            int firstRowOrdinal,
            Locus locus,
            int resultSetType,
            int resultSetConcurrency,
            Util.Function1<java.sql.Statement, Void> callback) {
        XmlaRequestContext localContext = XmlaRequestContext.getContext();
        if (Objects.nonNull(localContext) && Objects.nonNull(localContext.runningStatistics)) {
            if (MDC.get(MDC_KEY) == null) {
                String qid = localContext.getParameter(MDC_KEY);
                if (qid != null && !"".equals(qid)) {
                    MDC.put(MDC_KEY, qid);
                }
            }
            localContext.runningStatistics.mapExtendExecuteSql.put(new SqlExtend(locus, sql),
                    new XmlaRequestContext.ExecuteSql(sql));
        }

        long exeSqlStart = System.currentTimeMillis();
        RolapSchema schema = locus.execution.getMondrianStatement().getSchema();
        SqlStatement stmt =
                new SqlStatement(
                        dataSource, sql, types, maxRowCount, firstRowOrdinal, locus,
                        resultSetType, resultSetConcurrency, callback, schema);
        sqlExecutedTotal.increment();
        stmt.execute();
        long exeSqlEnd = System.currentTimeMillis();
        if (localContext != null && localContext.runningStatistics != null) {
            SqlExtend sqlExtend = new SqlExtend(locus, sql);
            long sqlExecuteTime = exeSqlEnd - exeSqlStart;
            // Record the SQL query time
            sqlExecutedTimer.record(Duration.ofMillis(sqlExecuteTime));
            localContext.runningStatistics.setSqlRunTime(sqlExtend, sqlExecuteTime);
            // TODO: adapt query id for kylin
            // String keQueryId = ((FixedCachedRowSetImpl) stmt.getResultSet()).getQueryId();
            // localContext.runningStatistics.setKeQueryId(sqlExtend, keQueryId);
        }
        return stmt;
    }

    /**
     * Raises an alert that native SQL evaluation could not be used
     * in a case where it might have been beneficial, but some
     * limitation in Mondrian's implementation prevented it.
     * (Do not call this in cases where native evaluation would
     * have been wasted effort.)
     *
     * @param functionName name of function for which native evaluation
     * was skipped
     *
     * @param reason reason why native evaluation was skipped
     */
    public static void alertNonNative(
            String functionName,
            String reason)
            throws NativeEvaluationUnsupportedException
    {
        // No i18n for log message, but yes for excn
        String alertMsg =
                "Unable to use native SQL evaluation for '" + functionName
                        + "'; reason:  " + reason;

        StringProperty alertProperty =
                MondrianProperties.instance().AlertNativeEvaluationUnsupported;
        String alertValue = alertProperty.get();

        if (alertValue.equalsIgnoreCase(
                org.apache.log4j.Level.WARN.toString()))
        {
            LOGGER.warn(alertMsg);
        } else if (alertValue.equalsIgnoreCase(
                org.apache.log4j.Level.ERROR.toString()))
        {
            LOGGER.error(alertMsg);
            throw MondrianResource.instance().NativeEvaluationUnsupported.ex(
                    functionName);
        }
    }

    /**
     * Loads a set of JDBC drivers.
     *
     * @param jdbcDrivers A string consisting of the comma-separated names
     *  of JDBC driver classes. For example
     *  <code>"sun.jdbc.odbc.JdbcOdbcDriver,com.mysql.jdbc.Driver"</code>.
     */
    public static synchronized void loadDrivers(String jdbcDrivers) {
        StringTokenizer tok = new StringTokenizer(jdbcDrivers, ",");
        while (tok.hasMoreTokens()) {
            String jdbcDriver = tok.nextToken();
            if (loadedDrivers.add(jdbcDriver)) {
                try {
                    ClassResolver.INSTANCE.forName(jdbcDriver, true);
                    LOGGER.info(
                            "Mondrian: JDBC driver "
                                    + jdbcDriver + " loaded successfully");
                } catch (ClassNotFoundException e) {
                    LOGGER.warn(
                            "Mondrian: Warning: JDBC driver "
                                    + jdbcDriver + " not found");
                }
            }
        }
    }

    /**
     * Creates a compiler which will generate programs which will test
     * whether the dependencies declared via
     * {@link mondrian.calc.Calc#dependsOn(Hierarchy)} are accurate.
     */
    public static ExpCompiler createDependencyTestingCompiler(
            ExpCompiler compiler)
    {
        return new RolapDependencyTestingEvaluator.DteCompiler(compiler);
    }

    /**
     * Locates a member specified by its member name, from an array of
     * members.  If an exact match isn't found, but a matchType of BEFORE
     * or AFTER is specified, then the closest matching member is returned.
     *
     *
     * @param members array of members to search from
     * @param parent parent member corresponding to the member being searched
     * for
     * @param level level of the member
     * @param searchSegment member name
     * @param matchType match type
     *
     * @return matching member (if it exists) or the closest matching one
     * in the case of a BEFORE or AFTER search
     */
    public static RolapMember findBestMemberMatch(
            List<RolapMember> members,
            RolapMember parent,
            RolapCubeLevel level,
            Id.Segment searchSegment,
            MatchType matchType)
    {
        String searchName = null;
        if (searchSegment instanceof Id.NameSegment) {
            searchName = ((Id.NameSegment) searchSegment).name;
        }
        if (searchSegment instanceof Id.KeySegment) {
            searchName = searchSegment.getKeyParts().get(0).name;
        }
        switch (matchType) {
            case FIRST:
                return members.get(0);
            case LAST:
                return members.get(members.size() - 1);
            default:
                // fall through
        }
        // create a member corresponding to the member we're trying
        // to locate so we can use it to hierarchically compare against
        // the members array
        RolapMember searchMember = null;
        RolapMember bestMatch = null;
        for (RolapMember member : members) {
            int rc;
            if (searchSegment.quoting == Id.Quoting.KEY
                    && member instanceof RolapMember)
            {
                List<Comparable> keyList = member.getKeyAsList();
                if (keyList.get(keyList.size() - 1).toString().equals(searchName)) {
                    return member;
                }
            }
            if (matchType.isExact()) {
                rc = Util.compareName(member.getName(), searchName);
            } else {
                if (searchMember == null) {
                    searchMember =
                            level.getHierarchy().createMember(
                                    parent, level, searchName, null);
                }
                rc =
                        compareSiblingMembersByName(
                                member,
                                searchMember);
            }
            if (rc == 0) {
                return member;
            }
            if (matchType == MatchType.BEFORE) {
                if (rc < 0
                        && (bestMatch == null
                        || compareSiblingMembersByName(member, bestMatch)
                        > 0))
                {
                    bestMatch = member;
                }
            } else if (matchType == MatchType.AFTER) {
                if (rc > 0
                        && (bestMatch == null
                        || compareSiblingMembersByName(member, bestMatch)
                        < 0))
                {
                    bestMatch = member;
                }
            }
        }
        if (matchType.isExact()) {
            return null;
        }
        return bestMatch;
    }

    public static ExpCompiler createProfilingCompiler(ExpCompiler compiler) {
        return new RolapProfilingEvaluator.ProfilingEvaluatorCompiler(
                compiler);
    }

    public static RolapSchema.PhysView convertInlineTableToRelation(
            RolapSchema.PhysInlineTable inlineTable,
            final Dialect dialect)
    {
        List<String> columnNames = new ArrayList<String>();
        List<String> columnTypes = new ArrayList<String>();
        for (RolapSchema.PhysColumn col : inlineTable.columnsByName.values()) {
            columnNames.add(col.name);
            columnTypes.add(col.datatype.name());
        }
        final String sql =
                dialect.generateInline(
                        columnNames,
                        columnTypes,
                        inlineTable.rowList);
        return new RolapSchema.PhysView(
                inlineTable.physSchema,
                inlineTable.alias,
                sql);
    }

    /**
     * Creates a dummy evaluator.
     */
    public static Evaluator createEvaluator(
            Statement statement)
    {
        Execution dummyExecution = new Execution(statement, 0);
        final RolapResult result = new RolapResult(dummyExecution, false, false, false);
        return result.getRootEvaluator();
    }

    public interface ExecuteQueryHook {
        void onExecuteQuery(String sql);
    }

    /**
     * Remove quotes from a format string, and replace "regular" format strings with nulls.
     * @param formatString The input format string
     * @return The processed format string
     */
    public static String processFormatString(String formatString) {
        boolean preserveRegularFormat = XmlaRequestContext.getContext().shouldPreserveRegularFormat();
        return processFormatString(formatString, preserveRegularFormat);
    }

    /**
     * Remove quotes from a format string, and replace "regular" format strings with nulls.
     * @param formatString The input format string
     * @return The processed format string
     */
    public static String processFormatString(String formatString, boolean preserveRegularFormat) {
        if (formatString != null) {
            if (formatString.startsWith("\"") && formatString.endsWith("\"")) {
                formatString = formatString.substring(1, formatString.length() - 1);
            }

            if (DEFAULT_FORMAT_STRING.equalsIgnoreCase(formatString)) {
                formatString = preserveRegularFormat ? DEFAULT_FORMAT_STRING_TAG : null;
            }
        }
        return formatString;
    }

    /**
     * Remove "qid" from the log4j MDC class, this will make the thread retrieve the correct query id.
     */
    public static void removeLog4jQueryIdRecord() {
        MDC.remove(MDC_KEY);
    }
}

// End RolapUtil.java
