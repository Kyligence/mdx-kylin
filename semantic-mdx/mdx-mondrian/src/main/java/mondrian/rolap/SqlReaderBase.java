package mondrian.rolap;

import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.resource.MondrianResource;
import mondrian.rolap.aggmatcher.AggStar;
import mondrian.rolap.sql.*;
import mondrian.server.Execution;
import mondrian.server.Locus;
import mondrian.server.Statement;
import mondrian.spi.Dialect;
import mondrian.util.Pair;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

import static mondrian.rolap.LevelColumnLayout.OrderKeySource.*;

public class SqlReaderBase {
    private static final Logger LOGGER =
        Logger.getLogger(SqlReaderBase.class);
    protected final TupleConstraint constraint;
    protected final List<Target> targets = new ArrayList<Target>();
    /** Stores all columns which are used in generated sql */
    protected Map<RolapCubeLevel, LevelColumnLayout<Integer>> targetsLayoutMap = new HashMap<>();
    protected Map<RolapCubeDimension, List<RolapMeasureGroup>> mapDimToMG;
    protected boolean isMultipleCube;
    protected int emptySets = 0;

    protected static final String columnAliasPrefix = "c";
    private static final String AXES_SOURCE_TYPE_SNAPSHOT = "SNAPSHOT";
    private static final String AXES_SOURCE_TYPE_PARTIAL_CUBE = "PARTIAL_CUBE";
    private static final String AXES_SOURCE_TYPE_ALL_CUBE = "ALL_CUBE";

    public SqlReaderBase(TupleConstraint constraint) {
        this.constraint = constraint;
        initDimAndMeasureGroupRel(constraint);
    }

    private static void addUnionOrderByOrdinal(final SqlQuery sqlQuery)
    {
        // If this is a select on a virtual cube, the query will be
        // a union, so the order by columns need to be numbers,
        // not column name strings or expressions.
        boolean nullable = true;
        final Dialect dialect = sqlQuery.getDialect();
        if (dialect.requiresUnionOrderByExprToBeInSelectClause()
            || dialect.requiresUnionOrderByOrdinal())
        {
            // If the expression is nullable and the dialect
            // sorts NULL values first, the dialect will try to
            // add an expression 'Iif(expr IS NULL, 1, 0)' into
            // the ORDER BY clause, and that is not allowed by this
            // dialect. So, pretend that the expression is not
            // nullable. NULL values, if present, will be sorted
            // wrong, but that's better than generating an invalid
            // query.
            nullable = false;
        }
        final String ordinal = Integer.toString(
            sqlQuery.getCurrentSelectListSize());
        sqlQuery.addOrderBy(
            ordinal, ordinal,
            true, false, nullable, false);
    }

    // we're only reading tuples from the targets that are
    // non-enum targets
    protected List<Target> getPartialTargets() {
        List<Target> partialTargets = new ArrayList<Target>();
        for (Target target : targets) {
            if (target.srcMembers == null) {
                partialTargets.add(target);
            }
        }
        return partialTargets;
    }

    /**
     * Gets an appropriate Execution based on the state of the current
     * locus.  Used for setting StatementLocus.
     */
    protected Execution getExecution() {
        assert targets.size() > 0;
        if (Locus.peek() != null && Locus.peek().execution.getMondrianStatement()
                .getMondrianConnection().getSchema() != null)
        {
            // the current locus has a statement that's associated with
            // a schema.  Use it.
            return Locus.peek().execution;
        } else {
            // no schema defined in the current locus.  This could
            // happen during schema load.  Construct a new execution associated
            // with the schema.
            Statement statement = targets.get(0)
                .getLevel()
                .getHierarchy()
                .getRolapSchema()
                .getInternalConnection()
                .getInternalStatement();
            return new Execution(statement, 0);
        }
    }

    Pair<String, List<SqlStatement.Type>> makeLevelMembersSql(Dialect dialect, boolean hasLimit) {
        // In the case of a virtual cube, if we need to join to the fact
        // table, we do not necessarily have a single underlying fact table,
        // as the underlying base cubes in the virtual cube may all reference
        // different fact tables.
        //
        // Therefore, we need to gather the underlying fact tables by going
        // through the list of measures referenced in the query.  And then
        // we generate one sub-select per fact table, joining against each
        // underlying fact table, unioning the sub-selects.
        List<RolapMeasureGroup> measureGroupList;
        if (MondrianProperties.instance().CustomizeSql.get() || constraint.isJoinRequired()) {
            measureGroupList = constraint.getMeasureGroupList();
        } else if (constraint.getEvaluator() != null
            && constraint.getEvaluator().isNonEmpty())
        {
            measureGroupList = Collections.singletonList(
                constraint.getEvaluator().getMeasureGroup());
        } else {
            measureGroupList = Collections.emptyList();
        }

        // judge how many crossjoin parts
        List<CrossJoinPart> crossJoinParts = resolveCrossjoinParts(measureGroupList, dialect);
        if (crossJoinParts != null && crossJoinParts.size() != 0) {
            return makeMultipleCubeLevelsSql(crossJoinParts, hasLimit);
        }
        return createCubeLevelsSql(dialect, measureGroupList, targets, hasLimit, 0);
    }

    private Pair<String, List<SqlStatement.Type>> createCubeLevelsSql(
            Dialect dialect, List<RolapMeasureGroup> measureGroupList, List<Target> targets, boolean hasLimit, int layoutOffset) {
        switch (measureGroupList.size()) {
            default:
                return generateMultipleCubeLevelsSql(dialect, measureGroupList, targets, hasLimit, layoutOffset);
            case 1:
                return generateSelectForLevels(
                        dialect, measureGroupList.get(0), 0, 1, targets, hasLimit, layoutOffset);
            case 0:
                return generateLevelsSqlFromDimensionSnapshot(dialect, targets, layoutOffset);
        }
    }

    private Pair<String, List<SqlStatement.Type>> generateMultipleCubeLevelsSql(Dialect dialect, List<RolapMeasureGroup> measureGroupList, List<Target> targets, boolean hasLimit, int layoutOffset) {
        if (constraint instanceof RolapNativeFilter.FilterDescendantsConstraint
                && ((RolapNativeFilter.FilterDescendantsConstraint) constraint).isLeaves())
            return generateSelectForLevels(
                dialect, measureGroupList.get(0), 0, 1, targets, hasLimit, layoutOffset);

        // generate sub-selects, each one joining with one of
        // the fact table referenced

        List<RolapMeasureGroup> joiningMeasureGroupList =
                getFullyJoiningMeasureGroups(measureGroupList, targets);
        if (joiningMeasureGroupList.size() == 0) {
            return sqlForEmptyTuple(dialect, measureGroupList);
        }

                // Save the original measure in the context
//          Member originalMeasure = constraint.getEvaluator().getMembers()[0];
        StringBuilder buf = new StringBuilder();
        List<SqlStatement.Type> types = null;
        for (int i = 0; i < joiningMeasureGroupList.size(); i++) {
            final RolapMeasureGroup measureGroup =
                    joiningMeasureGroupList.get(i);
            // Use the measure from the corresponding base cube in the
            // context to find the correct join path to the base fact
            // table.
            //
            // Any measure is fine since the constraint logic only uses it
            // to find the correct fact table to join to.
            Util.deprecated(
                    "todo: push the star into the context somehow, and remove this commented-out logic",
                    false);
//              Member measureInCurrentbaseCube = star.getMeasures().get(0);
//              constraint.getEvaluator().setContext(
//                  measureInCurrentbaseCube);

            if (i > 0) {
                buf.append(Util.getNlBySqlFormatted())
                        .append("union")
                        .append(Util.getNlBySqlFormatted());
            }
            if (joiningMeasureGroupList.size() > 1) {
                buf.append("(");
            }
            Pair<String, List<SqlStatement.Type>> pair =
                    generateSelectForLevels(
                            dialect,
                            measureGroup, i, joiningMeasureGroupList.size(), targets, hasLimit, layoutOffset);
            int idxOfLimit = pair.left.indexOf("limit");
            if (idxOfLimit != -1) {
                buf.append(pair.left, 0, idxOfLimit);
            } else {
                buf.append(pair.left);
            }
            if (joiningMeasureGroupList.size() > 1) {
                buf.append(")");
            }
            types = pair.right;
        }
        if (MondrianProperties.instance().CustomizeSql.get()) {
            // get the count of all sql columns
            int colCounts = types.size();

            StringBuilder preBuff = new StringBuilder();
            preBuff.append("select ");
            for (int i = 0; i < colCounts; i++) {
                if (i != 0) {
                    preBuff.append(", ");
                }
                preBuff.append("\"").append(columnAliasPrefix).append(i).append("\"");
            }
            preBuff.append( " from ( ");
            buf.insert(0, preBuff.toString());
            buf.append(")");
            if (hasLimit) {
                buf.append(" limit ");
                buf.append(MondrianProperties.instance().MaxPullSize.get());
            }
        }
        // Restore the original measure member
//            constraint.getEvaluator().setContext(originalMeasure);
        return Pair.of(buf.toString(), types);
    }

    private Pair<String, List<SqlStatement.Type>> generateLevelsSqlFromDimensionSnapshot(Dialect dialect, List<Target> partialTargets, int layoutOffset) {
        partialTargets = partialTargets == null ? targets : partialTargets;
        String s = "while generating query to retrieve members of level(s) from dimension snapshot " + partialTargets;

        final Map<RolapCubeDimension, List<Target>> mapDimension2Targets = new LinkedHashMap<>();
        RolapStarSet starSet = new RolapStarSet(null, null, null);
        for (Target target : partialTargets) {
            if (target.getSrcMembers() == null && !target.level.isAll()) {
                RolapCubeDimension dimension = target.level.getDimension();
                if (mapDimension2Targets.containsKey(dimension)) {
                    mapDimension2Targets.get(dimension).add(target);
                } else {
                    List<Target> unevaluatedTargets = new ArrayList<>();
                    unevaluatedTargets.add(target);
                    mapDimension2Targets.put(dimension, unevaluatedTargets);
                }
            }
        }

        List<Pair<String, List<SqlStatement.Type>>> sqlPairs = new ArrayList<>();

        if (!mapDimension2Targets.isEmpty()) {
            for (Map.Entry<RolapCubeDimension, List<Target>> entry : mapDimension2Targets.entrySet()) {
                List<Target> groupTargets = entry.getValue();
                ColumnLayoutBuilder columnLayoutBuilder = new ColumnLayoutBuilder(layoutOffset);
                SqlQueryBuilder queryBuilder = new SqlQueryBuilder(dialect, s, columnLayoutBuilder);
                // Control whether to add a limit clause
                queryBuilder.sqlQuery.setHasLimit(false);
                for (Target target : groupTargets) {
                    addLevelMemberSql(
                            queryBuilder,
                            target.getLevel(),
                            starSet,
                            0,
                            1);
                    ColumnLayout columnLayout = queryBuilder.layoutBuilder.toLayout(layoutOffset);
                    target.setColumnLayout(columnLayout);
                    targetsLayoutMap.putAll(columnLayout.levelLayoutMap);
                }
                starSet.setCubeDimension(entry.getKey());
                constraint.addConstraint(queryBuilder, starSet);
                sqlPairs.add(queryBuilder.toSqlAndTypes());
                layoutOffset += queryBuilder.sqlQuery.getCurrentSelectListSize();
            }
        }

        // merge sql pairs
        return mergeSqlPairs(sqlPairs);
    }

    private Pair<String, List<SqlStatement.Type>> mergeSqlPairs(List<Pair<String, List<SqlStatement.Type>>> sqlPairs) {
        if (sqlPairs == null || sqlPairs.size() == 0) {
            return null;
        }

        if (sqlPairs.size() == 1) {
            return sqlPairs.get(0);
        }

        List<SqlStatement.Type> types = new ArrayList<>();
        StringBuilder buff = new StringBuilder();
        buff.append("select * from ");
        for (Pair<String, List<SqlStatement.Type>> pair : sqlPairs) {
            types.addAll(pair.getValue());
            buff.append("(").append(pair.left).append("),");
        }
        buff.deleteCharAt(buff.length() - 1);
        buff.append(" limit ").append(MondrianProperties.instance().MaxPullSize.get());
        return Pair.of(buff.toString(), types);
    }

    private Pair<String, List<SqlStatement.Type>> makeMultipleCubeLevelsSql(List<CrossJoinPart> crossJoinParts, boolean hasLimit) {
        List<SqlStatement.Type> types = new ArrayList<>();
        StringBuilder buff = new StringBuilder();
        int layoutOffset = 0;

        buff.append("select * from ");
        for (CrossJoinPart crossJoinPart : crossJoinParts) {
            Pair<String, List<SqlStatement.Type>> partialPair = createCrossjoinPartSql(crossJoinPart, layoutOffset);
            layoutOffset += partialPair.right.size();
            types.addAll(partialPair.right);
            buff.append("(").append(partialPair.left).append("),");
        }
        buff.deleteCharAt(buff.length() - 1);
        if (hasLimit)
            buff.append(" limit ").append(MondrianProperties.instance().MaxPullSize.get());
        return Pair.of(buff.toString(), types);
    }

    private Pair<String, List<SqlStatement.Type>> createCrossjoinPartSql(CrossJoinPart crossJoinPart, int layoutOffset) {
        return createCubeLevelsSql(crossJoinPart.dialect, crossJoinPart.measureGroups, crossJoinPart.targets, false, layoutOffset);
    }

    private List<CrossJoinPart> resolveCrossjoinParts(List<RolapMeasureGroup> measureGroupList, Dialect dialect) {
        if (measureGroupList == null || measureGroupList.size() == 0
                || mapDimToMG == null || !isMultipleCube) {
            return null;
        } else {
            List<CrossJoinPart> crossJoinParts = new ArrayList<>();
            CrossJoinPart crossJoinPart = new CrossJoinPart();
            crossJoinPart.dialect = dialect;
            crossJoinParts.add(crossJoinPart);
            for (Target target : targets) {
                if (target.level.getDepth() != 0) {
                    RolapCubeDimension dimension = target.level.getDimension();
                    List<RolapMeasureGroup> dimRelatedMgs = new ArrayList<>();
                    for (RolapMeasureGroup measureGroup : measureGroupList) {
                        if (measureGroup.existsLink(dimension)) {
                            dimRelatedMgs.add(measureGroup);
                        }
                    }
                    if (dimRelatedMgs.size() == 0) {
                        resolveUnrelatedDimension(dimension, dimRelatedMgs);
                    }
                    if (crossJoinPart.measureGroups == null) {
                        crossJoinPart.measureGroups = dimRelatedMgs;
                        crossJoinPart.targets = new ArrayList<>();
                        crossJoinPart.targets.add(target);
                    } else if (crossJoinPart.measureGroups.size() == dimRelatedMgs.size()
                            && crossJoinPart.measureGroups.containsAll(dimRelatedMgs)){
                        crossJoinPart.targets.add(target);
                    } else {
                        crossJoinPart = new CrossJoinPart();
                        crossJoinPart.dialect = dialect;
                        crossJoinParts.add(crossJoinPart);
                        crossJoinPart.measureGroups = dimRelatedMgs;
                        crossJoinPart.targets = new ArrayList<>();
                        crossJoinPart.targets.add(target);

                    }
                }
            }
            return crossJoinParts;
        }

    }

    private void resolveUnrelatedDimension(RolapCubeDimension dimension, List<RolapMeasureGroup> dimRelatedMgs) {
        if (MondrianProperties.instance().PureAxesCalculateSourceType.get().equalsIgnoreCase(AXES_SOURCE_TYPE_SNAPSHOT)) {
            return;
        }
        if (MondrianProperties.instance().PureAxesCalculateSourceType.get().equalsIgnoreCase(AXES_SOURCE_TYPE_PARTIAL_CUBE)) {
            dimRelatedMgs.add(mapDimToMG.get(dimension).get(0));
            return;
        }
        if (MondrianProperties.instance().PureAxesCalculateSourceType.get().equalsIgnoreCase(AXES_SOURCE_TYPE_ALL_CUBE)) {
            dimRelatedMgs.addAll(mapDimToMG.get(dimension));
            return;
        }
        dimRelatedMgs.add(mapDimToMG.get(dimension).get(0));
    }


    private List<RolapMeasureGroup> getFullyJoiningMeasureGroups(
        List<RolapMeasureGroup> measureGroupList, List<Target> targets)
    {
        final List<RolapMeasureGroup> list = new ArrayList<RolapMeasureGroup>();
        for (RolapMeasureGroup measureGroup : measureGroupList) {
            if (allTargetsJoin(measureGroup, targets)) {
                list.add(measureGroup);
            }
        }
        return list;
    }

    private boolean allTargetsJoin(RolapMeasureGroup measureGroup, List<Target> targets) {
        for (Target target : targets) {
            if (!measureGroup.existsLink(target.level.cubeDimension)) {
                return false;
            }
        }
        return true;
    }

    Pair<String, List<SqlStatement.Type>> sqlForEmptyTuple(
        Dialect dialect,
        final List<RolapMeasureGroup> measureGroupList)
    {
        final SqlQuery sqlQuery = SqlQuery.newQuery(dialect, null);
        sqlQuery.addSelect("0", null);
        sqlQuery.addFrom(
            measureGroupList.get(0).getStar().getFactTable().getRelation(),
            null, true);
        final StringBuilder buf = new StringBuilder();
        dialect.quoteBooleanLiteral(buf, false);
        sqlQuery.addWhere(buf.toString());
        return sqlQuery.toSqlAndTypes();
    }

    /**
     * Generates the SQL string corresponding to the levels referenced.
     *
     * @param dialect Database dialect
     * @param measureGroup Measure group whose fact table to join to, or null
     * @param selectOrdinal Ordinal of this SELECT statement in UNION
     * @param selectCount Number of SELECT statements in UNION
     * @return SQL statement string and types
     */
    Pair<String, List<SqlStatement.Type>> generateSelectForLevels(
        Dialect dialect,
        RolapMeasureGroup measureGroup,
        int selectOrdinal,
        int selectCount,
        List<Target> partialTargets,
        boolean sqlQueryhasLimit,
        int layoutOffset)
    {
        partialTargets = partialTargets == null ? targets : partialTargets;
        String s =
            "while generating query to retrieve members of level(s) " + partialTargets;

        Evaluator evaluator = getEvaluator(constraint);
        final RolapStarSet starSet;
        if (measureGroup != null) {
            final AggStar aggStar = null;
            // = chooseAggStar(constraint, measureGroup, evaluator);
            final RolapMeasureGroup aggMeasureGroup = null; // TODO:
            starSet =
                new RolapStarSet(
                    measureGroup.getStar(), measureGroup, aggMeasureGroup);
        } else {
            starSet = new RolapStarSet(null, null, null);
        }

        // Find targets whose members are not enumerated.
        // if we're going to be enumerating the values for this target,
        // then we don't need to generate sql for it.
        List<Target> unevaluatedTargets = new ArrayList<Target>();

        // Distinct dimensions. (In case two or more levels come from the same
        // dimension, e.g. [Customer].[Gender] and [Customer].[Marital Status].)
        final Set<RolapCubeDimension> dimensions =
            new LinkedHashSet<>();

        for (Target target : partialTargets) {
            if (target.getSrcMembers() == null && !target.level.isAll()) {
                unevaluatedTargets.add(target);
                dimensions.add(target.level.getDimension());
            }
        }

        ColumnLayoutBuilder columnLayoutBuilder =
            new ColumnLayoutBuilder(layoutOffset);
        SqlQueryBuilder queryBuilder =
            new SqlQueryBuilder(
                dialect,
                s,
                columnLayoutBuilder);

        // Allow query to use optimization hints from the table definition
        queryBuilder.sqlQuery.setAllowHints(true);

        // Control whether to add a limit clause
        queryBuilder.sqlQuery.setHasLimit(sqlQueryhasLimit);

        // add the selects for all levels to fetch
        if (!unevaluatedTargets.isEmpty()) {
            if (measureGroup != null) {
                queryBuilder.fact = measureGroup;
            } else if (MondrianProperties.instance()
                    .FilterChildlessSnowflakeMembers.get())
            {
                queryBuilder.joinToDimensionKey = true;
            }
            for (Target target : unevaluatedTargets) {
                addLevelMemberSql(
                    queryBuilder,
                    target.getLevel(),
                    starSet,
                    selectOrdinal,
                    selectCount);
                ColumnLayout columnLayout = queryBuilder.layoutBuilder.toLayout();
                target.setColumnLayout(columnLayout);
                targetsLayoutMap.putAll(columnLayout.levelLayoutMap);
            }
        }

        boolean hasScopeFunction = false;
        for (Member member : evaluator.getQuery().getMeasuresMembers()) {
            if (member instanceof CalculatedMember) {
                Exp measureExp = member.getExpression();
                if (containScopeFunction(measureExp)) {
                    hasScopeFunction = true;
                    break;
                }
            }
        }
        if (!hasScopeFunction || MondrianProperties.instance().AxisCalcNonEmptyPushdownWithScope.get()) {
            constraint.addConstraint(queryBuilder, starSet);
        }
        if (this instanceof SqlTupleReader && ((SqlTupleReader) this).maxRows > 0) {
            queryBuilder.sqlQuery.setHasLimit(true);
            queryBuilder.sqlQuery.setLimitCount(((SqlTupleReader) this).maxRows);
        }
        if (queryBuilder.sqlQuery instanceof CustomizeSqlQuery) {
            if (measureGroup == null) {
                // get all used levels
                Set<RolapCubeDimension> cubeDimensions = new HashSet<>();
                for (Target target : partialTargets) {
                    cubeDimensions.add(target.level.cubeDimension);
                }
                List<RolapMeasureGroup> measureGroups = targets.get(0).getLevel().getCube().getMeasureGroups();
                for (RolapMeasureGroup measureGroup1 : measureGroups) {
                    // check available measure group
                    boolean containsAllDimension = true;
                    for (RolapCubeDimension cubeDimension : cubeDimensions) {
                        containsAllDimension = measureGroup1.dimensionMap3.containsKey(cubeDimension);
                    }
                    if (containsAllDimension) {
                        measureGroup = measureGroup1;
                    }
                }
            }
            // We should not link the fact table if we only want to query hierarchy descendants
            Pair<RolapStar.Table, Map<String, RolapSchema.PhysPath>> factAndPath =
                    constraint instanceof RolapNativeFilter.FilterDescendantsConstraint
                            && ((RolapNativeFilter.FilterDescendantsConstraint) constraint).isLeaves()
                            ? null
                            : measureGroup.getStar().getFactAndPath();
            if (measureGroup != null) {
                for (RolapCubeDimension dimension : measureGroup.dimensionMap3.keySet()) {
                    SqlQueryBuilder.Column keyColumn = queryBuilder.column(
                            dimension.getKeyAttribute().getKeyList().get(0),
                            dimension);
                    ((CustomizeSqlQuery)queryBuilder.sqlQuery).addFilter(keyColumn);
                }
            }
            return queryBuilder.toSqlAndTypes(factAndPath, measureGroup.getStar().getManyToManyPk(), measureGroup.getStar().getBridgeTable());
        } else {
            return queryBuilder.toSqlAndTypes();
        }
    }

    private boolean containScopeFunction(Exp measureExp) {
        if (measureExp instanceof ResolvedFunCall) {
            ResolvedFunCall measureCall = (ResolvedFunCall) measureExp;
            if (measureCall.getFunName().equals("Scope")) {
                return true;
            }
            Exp[] args = measureCall.getArgs();
            for (Exp arg : args) {
                if (containScopeFunction(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>Determines whether the GROUP BY clause is required, based on the
     * schema definitions of the hierarchy and level properties.</p>
     *
     * <p>The GROUP BY clause may only be eliminated if the level identified by
     * the uniqueKeyLevelName exists, the query is at a depth to include it,
     * and all properties in the included levels are functionally dependent on
     * their level values.</p>
     *
     * @param sqlQuery     The query object being constructed
     * @param hierarchy    Hierarchy of the cube
     * @param levels       Levels in this hierarchy
     * @param levelDepth   Level depth at which the query is occurring
     * @return whether the GROUP BY is needed
     *
     */
    private boolean isGroupByNeeded(
        SqlQuery sqlQuery,
        RolapHierarchy hierarchy,
        List<? extends RolapCubeLevel> levels,
        int levelDepth)
    {
        // REVIEW: The functionality of this method in mondrian-3.x depended on
        // the attribute Hierarchy.uniqueKeyLevelName, which does not exist in
        // mondrian-4.0.
        return true;
    }

    /**
     * Generates the SQL statement to access members of <code>level</code>. For
     * example, <blockquote>
     * <pre>SELECT "country", "state_province", "city"
     * FROM "customer"
     * GROUP BY "country", "state_province", "city", "init", "bar"
     * ORDER BY "country", "state_province", "city"</pre>
     * </blockquote> accesses the "City" level of the "Customers"
     * hierarchy. Note that:<ul>
     *
     * <li><code>"country", "state_province"</code> are the parent keys;</li>
     *
     * <li><code>"city"</code> is the level key;</li>
     *
     * <li><code>"init", "bar"</code> are member properties.</li>
     * </ul>
     *
     * @param queryBuilder the query object being constructed
     * @param level level to be added to the sql query
     * @param starSet Star set
     * @param selectOrdinal Ordinal of this SELECT statement in UNION
     * @param selectCount Number of SELECT statements in UNION
     */
    protected void addLevelMemberSql(
        SqlQueryBuilder queryBuilder,
        RolapCubeLevel level,
        final RolapStarSet starSet,
        int selectOrdinal,
        int selectCount)
    {
        assert (selectCount > 0 && selectOrdinal >= 0)
            && (selectOrdinal < selectCount);

        final boolean isUnion = selectCount > 1;
        final SqlQueryBuilder.Joiner joiner =
            SqlQueryBuilder.DimensionJoiner.of(
                starSet.getMeasureGroup(), level.getDimension());
        final SqlQuery sqlQuery = queryBuilder.sqlQuery;
        final ColumnLayoutBuilder layoutBuilder = queryBuilder.layoutBuilder;
        RolapCubeHierarchy hierarchy = level.getHierarchy();

        // lookup RolapHierarchy of base cube that matches this hierarchy
        if (starSet.cube != null
            && !hierarchy.getCube().equals(starSet.cube))
        {
            Util.deprecated("don't think this is ever the case", true);
            // replace the hierarchy with the underlying base cube hierarchy
            // in the case of virtual cubes
            hierarchy = starSet.cube.findBaseCubeHierarchy(hierarchy);
        }

        final int levelDepth = level.getDepth();
        boolean needsGroupBy =
            isGroupByNeeded(
                sqlQuery, hierarchy, hierarchy.getLevelList(), levelDepth);

        final RolapMeasureGroup measureGroup = starSet.getMeasureGroup();

        for (int i = 0; i <= levelDepth; i++) {
            final RolapCubeLevel currLevel = hierarchy.getLevelList().get(i);
            if (currLevel.isAll()) {
                continue;
            }

            final LevelLayoutBuilder levelLayoutBuilder =
                layoutBuilder.createLayoutFor(currLevel);

            // Determine if the aggregate table contains the collapsed level
            boolean levelCollapsed =
                (starSet.getAggStar() != null)
                && SqlMemberSource.isLevelCollapsed(
                    starSet.getAggStar(), level, measureGroup);
            if (levelCollapsed) {
                // an earlier check was made in chooseAggStar() to verify
                // that this is a single column level
                RolapStar.Column starColumn =
                    currLevel.getBaseStarKeyColumn(measureGroup);
                int bitPos = starColumn.getBitPosition();
                AggStar.Table.Column aggColumn =
                    starSet.getAggStar().lookupColumn(bitPos);
                String q = aggColumn.generateExprString(sqlQuery);
                String alias =
                    sqlQuery.addSelectGroupBy(
                        q,
                        starColumn.getExpression().getInternalType());
                layoutBuilder.register(q, alias);
                sqlQuery.addOrderBy(q, alias, true, false, true, true);
                aggColumn.getTable().addToFrom(sqlQuery, false, true);
                continue;
            }
            final RolapAttribute attribute = currLevel.getAttribute();

            if (currLevel.getParentAttribute() != null) {
                List<RolapSchema.PhysColumn> parentExps =
                        currLevel.getParentAttribute().getKeyList();
                Clause clause =
                        selectOrdinal == selectCount - 1
                                ? Clause.SELECT_GROUP_ORDER
                                : Clause.SELECT_GROUP;
                for (RolapSchema.PhysColumn parentExp : parentExps) {
                    levelLayoutBuilder.parentOrdinalList.add(
                            queryBuilder.addColumn(
                                    queryBuilder.column(parentExp, level.cubeDimension),
                                    clause, joiner, null));
                }
            }

            final Clause clause =
                    isUnion
                            ? Clause.SELECT.maybeGroup(needsGroupBy)
                            : Clause.SELECT_ORDER.maybeGroup(needsGroupBy);

            for (RolapSchema.PhysColumn column : currLevel.getOrderByList()) {
                int newOrderByOrdinal = queryBuilder.addColumn(
                        queryBuilder.column(column, level.cubeDimension),
                        clause, joiner, null);
                if (!levelLayoutBuilder.orderByOrdinalList.contains(newOrderByOrdinal))
                    levelLayoutBuilder.orderByOrdinalList.add(newOrderByOrdinal);
            }

            for (RolapSchema.PhysColumn column : attribute.getKeyList()) {
                int newKeyOrdinal = queryBuilder.addColumn(
                        queryBuilder.column(column, level.cubeDimension),
                        clause, joiner, null);
                if (!levelLayoutBuilder.keyOrdinalList.contains(newKeyOrdinal))
                    levelLayoutBuilder.keyOrdinalList.add(newKeyOrdinal);
            }

            levelLayoutBuilder.nameOrdinal =
                    queryBuilder.addColumn(
                            queryBuilder.column(
                                    attribute.getNameExp(), level.cubeDimension),
                            Clause.SELECT.maybeGroup(needsGroupBy),
                            joiner,
                            null);

            levelLayoutBuilder.captionOrdinal =
                    queryBuilder.addColumn(
                            queryBuilder.column(
                                    attribute.getCaptionExp(), level.cubeDimension),
                            Clause.SELECT.maybeGroup(needsGroupBy),
                            joiner,
                            null);

            if (attribute.getValueExp() != null) {
                levelLayoutBuilder.valueOrdinal = queryBuilder.addColumn(
                        queryBuilder.column(attribute.getValueExp(), level.cubeDimension),
                        Clause.SELECT.maybeGroup(needsGroupBy), joiner, null);
            }

            constraint.addLevelConstraint(
                    sqlQuery, starSet, currLevel);

            if (levelCollapsed) {
                // add join between key and aggstar
                // join to dimension tables starting
                // at the lowest granularity and working
                // towards the fact table
                for (RolapSchema.PhysColumn column : attribute.getKeyList()) {
                    hierarchy.addToFromInverse(sqlQuery, column);
                }

                RolapStar.Column starColumn =
                    currLevel.getBaseStarKeyColumn(
                        measureGroup);
                int bitPos = starColumn.getBitPosition();
                AggStar.Table.Column aggColumn =
                    starSet.getAggStar().lookupColumn(bitPos);
                assert attribute.getKeyList().size() == 1 : "TODO:";
                sqlQuery.addWhere(
                    aggColumn.getExpression().toSql()
                    + " = "
                    + attribute.getKeyList().get(0).toSql());
            }

            // If this is a select on a virtual cube, the query will be
            // a union, so the order by columns need to be numbers,
            // not column name strings or expressions.
            if (isUnion && selectOrdinal == selectCount - 1) {
                addUnionOrderByOrdinal(sqlQuery);
            }

            if (!isUnion) {
                for (RolapSchema.PhysColumn column : currLevel.getOrderByList())
                {
                    if (sqlQuery.getDialect().requiresOrderByAlias()) {
                        // if order by alias is required the column needs to be
                        // in the select list with an alias.
                        queryBuilder.addColumn(
                            queryBuilder.column(
                                column, currLevel.cubeDimension),
                            Clause.SELECT_ORDER, joiner, null);
                    } else {
                        sqlQuery.addOrderBy(column.toSql(), true, false, true);
                    }
                }
            }

            for (RolapProperty property
                : currLevel.attribute.getExplicitProperties())
            {
                // FIXME: For now assume that properties have a single-column
                //    key and name etc. are the same.
                assert property.attribute.getKeyList().size() == 1;
                RolapSchema.PhysColumn column =
                    property.attribute.getKeyList().get(0);
                String propSql = column.toSql();
                int ordinal = layoutBuilder.lookup(propSql);
                if (ordinal < 0) {
                    String alias =
                        sqlQuery.addSelect(propSql, column.getInternalType());
                    ordinal = layoutBuilder.register(propSql, alias);
                    if (needsGroupBy) {
                        // Certain dialects allow us to eliminate properties
                        // from the group by that are functionally dependent
                        // on the level value
                        if (!sqlQuery.getDialect().allowsSelectNotInGroupBy()
                            || !property.dependsOnLevelValue())
                        {
                            sqlQuery.addGroupBy(propSql, alias);
                        }
                    }
                }
                levelLayoutBuilder.propertyOrdinalList.add(ordinal);
            }
        }

        // Add lower levels' relations to the FROM clause to filter out members
        // that have no children. For backwards compatibility, but less
        // efficient.
        if (measureGroup != null) {
            queryBuilder.joinToDimensionKey = true;
        }
    }

    /**
     * Obtains the evaluator used to find an aggregate table to support
     * the Tuple constraint.
     *
     * @param constraint Constraint
     * @return evaluator for constraint
     */
    protected Evaluator getEvaluator(TupleConstraint constraint) {
        if (constraint instanceof SqlContextConstraint) {
            return constraint.getEvaluator();
        }
        if (constraint instanceof DescendantsConstraint) {
            DescendantsConstraint descConstraint =
                (DescendantsConstraint) constraint;
            MemberChildrenConstraint mcc =
                descConstraint.getMemberChildrenConstraint(null);
            if (mcc instanceof SqlContextConstraint) {
                SqlContextConstraint scc = (SqlContextConstraint) mcc;
                return scc.getEvaluator();
            }
        }
        return null;
    }

    protected void initDimAndMeasureGroupRel(TupleConstraint constraint) {
        if (constraint.getMeasureGroupList() != null && constraint.getMeasureGroupList().size() != 0) {
            mapDimToMG = new HashMap<>();
            RolapCube cube = constraint.getMeasureGroupList().get(0).getCube();
            isMultipleCube = cube.getMeasureGroups().size() > 1;
            for (RolapMeasureGroup mg : cube.getMeasureGroups()) {
                for (RolapCubeDimension dimension : mg.dimensionMap3.keySet()) {
                    if (mapDimToMG.containsKey(dimension)) {
                        mapDimToMG.get(dimension).add(mg);
                    } else {
                        List<RolapMeasureGroup> mgList = new ArrayList<>();
                        mapDimToMG.put(dimension, mgList);
                        mgList.add(mg);
                    }
                }
            }
        }
    }

    public void incrementEmptySets() {
        emptySets++;
    }

    public void addLevelMembers(
        RolapCubeLevel level,
        TupleReader.MemberBuilder memberBuilder,
        List<RolapMember> srcMembers)
    {
        targets.add(new Target(level, memberBuilder, srcMembers));
    }

    /**
     *
     * @see Util#deprecated(Object) add javadoc and make top level
     */
    public static class ColumnLayoutBuilder {
        private final List<String> exprList = new ArrayList<String>();
        private final List<String> aliasList = new ArrayList<String>();
        public final Map<RolapCubeLevel, LevelLayoutBuilder> levelLayoutMap =
            new IdentityHashMap<RolapCubeLevel, LevelLayoutBuilder>();
        LevelLayoutBuilder currentLevelLayout;
        final List<SqlStatement.Type> types = new ArrayList<SqlStatement.Type>();
        private int layoutOffset;

        /**
         * Creates a ColumnLayoutBuilder.
         */
        public ColumnLayoutBuilder() {
        }

        public ColumnLayoutBuilder(int layoutOffset) {
            this.layoutOffset = layoutOffset;
        }

        /**
         * Returns the ordinal of a given expression in the SELECT clause.
         *
         * @param sql SQL expression
         * @return Ordinal of expression, or -1 if not found
         */
        public int lookup(String sql) {
            return exprList.indexOf(sql);
        }

        public int getLayoutOffset() {
            return layoutOffset;
        }

        /**
         * Registers a given expression in the SELECT clause, or searches for
         * an existing expression, and returns the ordinal.
         *
         * @param sql SQL expression
         * @param alias Alias, or null
         * @return Ordinal of expression
         */
        public int register(String sql, String alias) {
            int ordinal = exprList.size();
            exprList.add(sql);
            aliasList.add(alias);
            return ordinal;
        }

        public ColumnLayout toLayout() {
            return toLayout(layoutOffset);
        }

        public ColumnLayout toLayout(int layoutOffset) {
            return new ColumnLayout(convert(levelLayoutMap.values(), layoutOffset));
        }

        private Map<RolapCubeLevel, LevelColumnLayout<Integer>> convert(
            Collection<LevelLayoutBuilder> builders, int layoutOffset)
        {
            final Map<RolapCubeLevel, LevelColumnLayout<Integer>> map = new IdentityHashMap<>();
            for (LevelLayoutBuilder builder : builders) {
                if (builder != null) {
                    map.put(builder.level, convert(builder, layoutOffset));
                }
            }
            return map;
        }

        private LevelColumnLayout<Integer> convert(LevelLayoutBuilder builder, int layoutOffset) {
            return builder == null ? null : builder.toLayout(layoutOffset);
        }

        public LevelLayoutBuilder createLayoutFor(RolapCubeLevel level) {
            LevelLayoutBuilder builder = levelLayoutMap.get(level);
            if (builder == null) {
                builder = new LevelLayoutBuilder(level);
                levelLayoutMap.put(level, builder);
            }
            currentLevelLayout = builder;
            return builder;
        }

        public List<SqlStatement.Type> getTypes() {
            return types;
        }

        public String getAlias(int i) {
            return aliasList.get(i);
        }

        public int getExprSize() {
            return exprList.size();
        }
    }

    /**
     * Builder for {@link LevelColumnLayout}.
     *
     * @see Util#deprecated(Object) make top level
     */
    public static class LevelLayoutBuilder {
        public List<Integer> keyOrdinalList = new ArrayList<Integer>();
        public Integer nameOrdinal = null;
        public List<Integer> orderByOrdinalList = new ArrayList<Integer>();
        public Integer captionOrdinal = null;
        public Integer valueOrdinal = null;
        public final List<Integer> propertyOrdinalList = new ArrayList<Integer>();
        public final List<Integer> parentOrdinalList =
            new ArrayList<Integer>();
        public boolean filterNeeded = false;
        public final RolapCubeLevel level;

        public LevelLayoutBuilder(RolapCubeLevel level) {
            this.level = level;
        }

        public LevelColumnLayout<Integer> toLayout() {
            return toLayout(0);
        }

        public LevelColumnLayout<Integer> toLayout(int layoutOffset) {
            boolean assignOrderKeys =
                MondrianProperties.instance().CompareSiblingsByOrderKey.get()
                || Util.deprecated(true, false); // TODO: remove property

            LevelColumnLayout.OrderKeySource orderBySource = NONE;
            if (assignOrderKeys) {
                if (orderByOrdinalList.equals(keyOrdinalList)) {
                    orderBySource = KEY;
                } else if (orderByOrdinalList.equals(
                        Collections.singletonList(nameOrdinal)))
                {
                    orderBySource = NAME;
                } else {
                    orderBySource = MAPPED;
                }
            }

            return new LevelColumnLayout<>(
                    newOrdinalWithOffset(keyOrdinalList, layoutOffset),
                    newOrdinalWithOffset(nameOrdinal, layoutOffset),
                    newOrdinalWithOffset(captionOrdinal, layoutOffset),
                    orderBySource,
                    orderBySource == MAPPED
                            ? newOrdinalWithOffset(orderByOrdinalList, layoutOffset)
                            : null,
                    newOrdinalWithOffset(valueOrdinal, layoutOffset),
                    newOrdinalWithOffset(propertyOrdinalList, layoutOffset),
                    newOrdinalWithOffset(parentOrdinalList, layoutOffset),
                    filterNeeded);
        }

        private static List<Integer> newOrdinalWithOffset(List<Integer> ordinalList, int offset) {
            if (ordinalList != null) {
                List<Integer> newOrdinalList = new ArrayList<>(ordinalList.size());
                for (int i = 0; i < ordinalList.size(); i++) {
                    newOrdinalList.add(ordinalList.get(i) + offset);
                }
                return newOrdinalList;
            }
            return null;
        }

        private static Integer newOrdinalWithOffset(Integer ordinal, int offset) {
            if (ordinal != null) {
                return ordinal + offset;
            }
            return null;
        }
    }

    /**
     * Description of where to find attri
     *
     * butes within each row.
     */
    public static class ColumnLayout {
        public final Map<RolapCubeLevel, LevelColumnLayout<Integer>> levelLayoutMap;

        public ColumnLayout(
            final Map<RolapCubeLevel,
                LevelColumnLayout<Integer>> levelLayoutMap)
        {
            this.levelLayoutMap = levelLayoutMap;
        }
    }

    /**
     * Describes a level to be added to the SQL query and the constraints on it.
     */
    protected class Target implements Comparable<Target> {
        final List<RolapMember> srcMembers;
        final RolapCubeLevel level;
        private RolapMember currMember;
        private List<RolapMember> list;
        final Object cacheLock;
        final TupleReader.MemberBuilder memberBuilder;

        final MemberCache cache;
        ColumnLayout columnLayout;

        RolapCubeLevel[] levels;
        int levelDepth;
        boolean parentChild;
        List<RolapMember> members;
        List<List<RolapMember>> siblings;
        // if set, the rows for this target come from the array rather
        // than native sql
        // current member within the current result set row
        // for this target

        public Target(
            RolapCubeLevel level,
            TupleReader.MemberBuilder memberBuilder,
            List<RolapMember> srcMembers)
        {
            this.srcMembers = srcMembers;
            this.level = level;
            cacheLock = memberBuilder.getMemberCacheLock();
            this.memberBuilder = memberBuilder;
            this.cache = memberBuilder.getMemberCache();
        }

        @Override
        public int compareTo(Target o) {
            if (getLevel().getHierarchy().equals(o.getLevel().getHierarchy())) {
                return Integer.compare(getLevel().getDepth(), o.getLevel().getDepth());
            }
            return 0;
        }

        public void setList(final List<RolapMember> list) {
            this.list = list;
        }

        public List<RolapMember> getSrcMembers() {
            return srcMembers;
        }

        public RolapCubeLevel getLevel() {
            return level;
        }

        public RolapMember getCurrMember() {
            return this.currMember;
        }

        public void setCurrMember(final RolapMember m) {
            this.currMember = m;
        }

        public List<RolapMember> getList() {
            return list;
        }

        public String toString() {
            return level.getUniqueName();
        }

        /**
         * Adds a row to the collection.
         *
         * @param stmt Statement
         * @return Whether successfully added
         * @throws SQLException On error
         */
        public final RolapMember addRow(SqlStatement stmt) throws SQLException {
            synchronized (cacheLock) {
                return internalAddRow(stmt);
            }
        }

        public void add(final RolapMember member) {
            this.getList().add(member);
        }

        public void open() {
            levels = level.getHierarchy().getLevelList().toArray(
                new RolapCubeLevel[
                    level.getHierarchy().getLevelList().size()]);
            setList(new ArrayList<RolapMember>());
            levelDepth = level.getDepth();
            parentChild = level.isParentChild();
            // members[i] is the current member of level#i, and siblings[i]
            // is the current member of level#i plus its siblings
            final int levelCount = levels.length;
            members =
                new ArrayList<RolapMember>(
                    Collections.<RolapMember>nCopies(levelCount, null));
            siblings = new ArrayList<List<RolapMember>>(levelCount + 1);
            for (int i = 0; i < levelCount + 1; i++) {
                siblings.add(new ArrayList<RolapMember>());
            }
        }

        RolapMember internalAddRow(
            SqlStatement stmt)
            throws SQLException
        {
            RolapMember member = null;
            if (getCurrMember() != null) {
                setCurrMember(member);
            } else {
                for (int i = 0; i <= levelDepth; i++) {
                    RolapCubeLevel childLevel = levels[i];
                    if (childLevel.isAll()) {
                        member = memberBuilder.allMember();
                        continue;
                    }
                    final LevelColumnLayout<Integer> layout =
                        columnLayout.levelLayoutMap.get(childLevel);
                    RolapMember parentMember = member;
                    final Map<Object, SqlStatement.Accessor> accessors =
                        stmt.getAccessors();
                    pc:
                    if (parentChild) {
                        Comparable[] parentKeys =
                            new Comparable[layout.getParentKeys().size()];
                        for (int j = 0; j < layout.getParentKeys().size();
                             j++)
                        {
                            int parentOrdinal = layout.getParentKeys().get(j);
                            Comparable value =
                                accessors.get(parentOrdinal).get();
                            if (value == null) {
                                // member is at top of hierarchy; its parent is
                                // the 'all' member. Convert null to placeholder
                                // value for uniformity in hashmaps.
                                break pc;
                            } else if (value.toString().equals(
                                    childLevel.getNullParentValue()))
                            {
                                // member is at top of hierarchy; its parent is
                                // the 'all' member
                                break pc;
                            } else {
                                parentKeys[j] = value;
                            }
                        }
                        Object parentKey =
                            parentKeys.length == 1
                                ? parentKeys[0]
                                : Arrays.asList(parentKeys);
                        parentMember = cache.getMember(level, parentKey);
                        if (parentMember == null) {
                            LOGGER.warn(
                                MondrianResource.instance()
                                    .LevelTableParentNotFound.str(
                                        childLevel.getUniqueName(),
                                        parentKey.toString()));
                        }
                    }
                    Comparable[] keyValues =
                        new Comparable[layout.getKeys().size()];
                    for (int j = 0; j < layout.getKeys().size(); j++) {
                        int keyOrdinal = layout.getKeys().get(j);
                        Comparable value = accessors.get(keyOrdinal).get();
                        keyValues[j] = SqlMemberSource.toComparable(value);
                    }
                    final Object key = RolapMember.Key.quick(keyValues);
                    member = cache.getMember(childLevel, key);
                    if (member == null) {
                        if (constraint instanceof
                            RolapNativeCrossJoin.NonEmptyCrossJoinConstraint
                            && childLevel.isParentChild())
                        {
                            member =
                                ((RolapNativeCrossJoin
                                    .NonEmptyCrossJoinConstraint) constraint)
                                    .findMember(key);
                        }
                        if (member == null) {
                            final Comparable keyClone =
                                RolapMember.Key.create(keyValues);

                            final Comparable captionValue;
                            if (layout.getCaptionKey() >= 0) {
                                captionValue =
                                    accessors.get(layout.getCaptionKey()).get();
                            } else {
                                captionValue = null;
                            }

                            final Comparable nameObject;
                            final String nameValue;
                            if (layout.getNameKey() >= 0) {
                                nameObject =
                                    accessors.get(layout.getNameKey()).get();
                                nameValue =
                                    nameObject == null
                                        ? RolapUtil.mdxNullLiteral()
                                        : String.valueOf(nameObject);
                            } else {
                                nameObject = null;
                                nameValue = null;
                            }

                            final Comparable orderKey;
                            switch (layout.getOrderBySource()) {
                            case NONE:
                                orderKey = null;
                                break;
                            case KEY:
                                orderKey = keyClone;
                                break;
                            case NAME:
                                orderKey = nameObject;
                                break;
                            case MAPPED:
                                orderKey =
                                    SqlMemberSource.getCompositeKey(
                                        accessors, layout.getOrderByKeys());
                                break;
                            default:
                                throw Util.unexpected(
                                    layout.getOrderBySource());
                            }

                            final Comparable value;
                            if (layout.getValueKey() != null && layout.getValueKey() >= 0) {
                                value = accessors.get(layout.getValueKey()).get();
                            } else {
                                value = null;
                            }

                            member = memberBuilder.makeMember(
                                parentMember, childLevel, keyClone,
                                captionValue, nameValue,
                                orderKey, value, parentChild, stmt, layout);
                        }
                    }

                    final RolapMember prevMember = members.get(i);
                    // TODO: is this block ever entered?
                    if (member != prevMember && prevMember != null) {
                        // Flush list we've been building.
                        List<RolapMember> children = siblings.get(i + 1);
                        if (children != null) {
                            MemberChildrenConstraint mcc =
                                constraint.getMemberChildrenConstraint(
                                    prevMember);
                            if (mcc != null) {
                                cache.putChildren(
                                    prevMember, mcc, children);
                            }
                        }
                        // Start a new list, if the cache needs one. (We don't
                        // synchronize, so it's possible that the cache will
                        // have one by the time we complete it.)
                        MemberChildrenConstraint mcc =
                            constraint.getMemberChildrenConstraint(member);
                        // we keep a reference to cachedChildren so they don't
                        // get garbage-collected
                        List<RolapMember> cachedChildren =
                            cache.getChildrenFromCache(member, mcc);
                        if (i < levelDepth && cachedChildren == null) {
                            siblings.set(i + 1, new ArrayList<RolapMember>());
                        } else {
                            // don't bother building up a list
                            siblings.set(i + 1, null);
                        }
                        // Record new current member of this level.
                        members.set(i, member);
                        // If we're building a list of siblings at this level,
                        // we haven't seen this one before, so add it.
                        if (siblings.get(i) != null) {
                            if (keyValues == null) {
                                // TODO:  keyValues is never null.
                                // This mishandles null keys.
                                addAsOldestSibling(siblings.get(i), member);
                            } else {
                                siblings.get(i).add(member);
                            }
                        }
                    }
                }
                setCurrMember(member);
            }
            if (constraint instanceof RolapNativeFilter.FilterDescendantsConstraint
                    && ((RolapNativeFilter.FilterDescendantsConstraint) constraint).isLeaves()
                    && (member == null || member.isNull())) {
                return null;
            }

            getList().add(member);
            return member;
        }

        public void setColumnLayout(ColumnLayout columnLayout) {
            this.columnLayout = columnLayout;
        }

        public List<Member> close() {
            synchronized (cacheLock) {
                return internalClose();
            }
        }

        /**
         * Cleans up after all rows have been processed, and returns the list of
         * members.
         *
         * @return list of members
         */
        public List<Member> internalClose() {
            for (int i = 0; i < members.size(); i++) {
                RolapMember member = members.get(i);
                final List<RolapMember> children = siblings.get(i + 1);
                if (member != null && children != null) {
                    // If we are finding the members of a particular level, and
                    // we happen to find some of the children of an ancestor of
                    // that level, we can't be sure that we have found all of
                    // the children, so don't put them in the cache.
                    if (member.getDepth() < level.getDepth()) {
                        continue;
                    }
                    MemberChildrenConstraint mcc =
                        constraint.getMemberChildrenConstraint(member);
                    if (mcc != null) {
                        cache.putChildren(member, mcc, children);
                    }
                }
            }
            return Util.cast(getList());
        }

        /**
         * Adds <code>member</code> just before the first element in
         * <code>list</code> which has the same parent.
         */
        private void addAsOldestSibling(
            List<RolapMember> list,
            RolapMember member)
        {
            int i = list.size();
            while (--i >= 0) {
                RolapMember sibling = list.get(i);
                if (sibling.getParentMember() != member.getParentMember()) {
                    break;
                }
            }
            list.add(i + 1, member);
        }
    }

    private class CrossJoinPart {
        private List<Target> targets;
        private List<RolapMeasureGroup> measureGroups;
        private Dialect dialect;
    }
}
