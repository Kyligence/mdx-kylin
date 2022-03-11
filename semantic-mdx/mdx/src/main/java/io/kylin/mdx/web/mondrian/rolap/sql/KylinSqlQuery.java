/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.web.mondrian.rolap.sql;

import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.core.MdxConfig;
import mondrian.olap.MondrianProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapSchema;
import mondrian.rolap.RolapStar;
import mondrian.rolap.sql.CustomizeSqlQuery;
import mondrian.rolap.sql.SqlQuery;
import mondrian.rolap.sql.SqlQueryBuilder;
import mondrian.spi.Dialect;
import mondrian.util.Pair;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KylinSqlQuery extends CustomizeSqlQuery {

    private String m2mPk;

    private String bridgeTable;

    private boolean innerQuery;

    private mondrian.util.Pair<RolapStar.Table, Set<List<RolapSchema.PhysLink>>> factAndLinks;

    private mondrian.util.Pair<RolapStar.Table, Map<String, RolapSchema.PhysPath>> factAndPath;

    private List<SqlQueryBuilder.Column> dimensionCols = new ArrayList<>(8);

    private List<SqlQueryBuilder.Column> filterCols = new ArrayList<>(8);

    private Map<Integer, RolapStar.Measure> measureIndexMapper = new LinkedHashMap<>(4);

    private Collection<RolapSchema.PhysColumn> weightColumns;

    private Set<RolapSchema.PhysRelation> nonQueryingM2MTables;

    private Set<String> modelHints;

    private String repeatCol;

    public KylinSqlQuery(Dialect dialect) {
        super(dialect);
    }

    private KylinSqlQuery(Dialect dialect, boolean innerQuery) {
        this(dialect);
        this.innerQuery = innerQuery;
    }

    @Override
    public void setM2mPk(String m2mPk) {
        this.m2mPk = m2mPk;
    }

    @Override
    public void setBridgeTable(String bridgeTable) {
        this.bridgeTable = bridgeTable;
    }

    @Override
    public void setFactAndPath(mondrian.util.Pair<RolapStar.Table, Map<String, RolapSchema.PhysPath>> factAndPath) {
        this.factAndPath = factAndPath;
    }

    private void setRepeatCol(String repeatCol) {
        this.repeatCol = repeatCol;
    }

    private void setDimensionCols(List<SqlQueryBuilder.Column> dimensionCols) {
        this.dimensionCols = dimensionCols;
    }

    private void setFilterCols(List<SqlQueryBuilder.Column> filterCols) {
        this.filterCols = filterCols;
    }

    public void setMeasureIndexMapper(Map<Integer, RolapStar.Measure> measureIndexMapper) {
        this.measureIndexMapper = measureIndexMapper;
    }

    @Override
    public void initNonQueryingM2MTable() {
        nonQueryingM2MTables = new HashSet<>();
    }

    @Override
    public void addNonQueryingM2MTable(RolapSchema.PhysRelation table) {
        if (nonQueryingM2MTables == null) {
            initNonQueryingM2MTable();
        }
        nonQueryingM2MTables.add(table);
    }

    @Override
    public void removeNonQueryingM2MTable(RolapSchema.PhysRelation table) {
        if (nonQueryingM2MTables != null) {
            nonQueryingM2MTables.remove(table);
        }
    }

    @Override
    public void addDimension(SqlQueryBuilder.Column dimensionCol) {
        this.dimensionCols.add(dimensionCol);
    }

    @Override
    public void addFilter(SqlQueryBuilder.Column filterCol) {
        this.filterCols.add(filterCol);
    }

    /**
     * Must be called after the select clause has been added.
     */
    @Override
    public void addMeasure(RolapStar.Measure measure) {
        this.measureIndexMapper.put(select.size() - 1, measure);

        boolean addModelHints = MondrianProperties.instance().AddModelPriorityHints.get();
        if (!addModelHints) {
            return;
        }

        if (modelHints == null) {
            // Only support one model hint currently.
            modelHints = new LinkedHashSet<>(1);
        }
        modelHints.add(measure.getMeasureGroupName());
    }

    @Override
    public void setWeightedColumns(Collection<RolapSchema.PhysColumn> weightColumns) {
        this.weightColumns = weightColumns;
    }

    private boolean isCustomRollup() {
        return nonQueryingM2MTables != null && measureIndexMapper.entrySet().stream().allMatch(entry -> entry.getValue().isSum());
    }

    @Override
    public StringBuilder toBuffer(StringBuilder buf, String prefix) {
        if (factAndPath != null) {
            createFactAndLinks();
        }
        if (needCreateM2MSQL()) {
            return createM2MSQL(buf, prefix);
        }

        Map<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> tableLinkedColumnsMapper = null;
        Map<RolapStar.Measure, String> measureAliasAndInnerAliasMapper = null;
        if (isCustomRollup() && factAndLinks != null && !Utils.isCollectionEmpty(factAndLinks.right)) {
            tableLinkedColumnsMapper = getTableLinkedColumns();
            measureAliasAndInnerAliasMapper = replaceMeasureExpressions(tableLinkedColumnsMapper);
        }

        final String first = generateSelectFirstString();
        select.toBuffer(buf, generateFormattedSql, prefix, first, ", ", "", "");
        String fromSep = factAndPath != null ? " inner join " : ", ";
        // use default join clause
        FromClauseList fromClauseList = new FromClauseList(true);
        if (factAndPath != null) {
            buildJoinClause(fromClauseList, tableLinkedColumnsMapper, measureAliasAndInnerAliasMapper);
        } else {
            Set<String> tables = new LinkedHashSet<>();
            for (SqlQueryBuilder.Column column : dimensionCols) {
                tables.add(column.getTable().toString());
            }
            fromClauseList.addAll(tables);
        }

        fromClauseList.toBuffer(buf, generateFormattedSql, prefix, " from ", fromSep, "", "");
        where.toBuffer(buf, generateFormattedSql, prefix, " where ", " and ", "", "");
        groupBy.toBuffer(buf, generateFormattedSql, prefix, " group by ", ", ", "", "");
        XmlaRequestContext context = XmlaRequestContext.getContext();
        having.toBuffer(buf, generateFormattedSql, prefix, " having ", " and ", "", "");
        if (MdxConfig.getInstance().isEnableSortSqlResult()) {
            orderBy.toBuffer(buf, generateFormattedSql, prefix, " order by ", ", ", "", "");
        }
        // set limit
        if (!hasLimit) {
            return buf;
        }

        buf.append(Util.getNlBySqlFormatted());
        int fullPullMaxSize = MondrianProperties.instance().MaxPullSize.get();
        if (limitCount > 0) {
            if ((context.queryPage != null && limitCount < context.queryPage.pageSize) ||
                    ((context.queryPage == null) && (limitCount < fullPullMaxSize))) {
                buf.append("limit ").append(limitCount);
                if (offset > 0) {
                    buf.append("\noffset ").append(offset);
                }
                return buf;
            }
        }
        if (context.queryPage != null) {
            if (context.queryPage.inOnePage && context.queryPage.startPage == 0) {
                buf.append("limit ").append(context.queryPage.pageSize);
            } else if (context.queryPage.inOnePage && context.queryPage.startPage != 0) {
                buf.append("limit ").append(context.queryPage.pageSize).append(" offset ").append(context.queryPage.startPage * context.queryPage.pageSize);
            } else if (!context.queryPage.inOnePage) {
                buf.append("limit ").append(context.queryPage.queryEnd - context.queryPage.queryStart).append(" offset ").append(context.queryPage.queryStart);
            }
        } else {
            buf.append("limit ").append(fullPullMaxSize);
        }
        return buf;
    }

    private Map<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> getTableLinkedColumns() {
        Function<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> linkedHashSetGenerator =
                ignored -> new LinkedHashSet<>();

        Map<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> tableLinkedColumnsMapper = new HashMap<>();
        for (List<RolapSchema.PhysLink> links : factAndLinks.getRight()) {
            for (RolapSchema.PhysLink link : links) {
                tableLinkedColumnsMapper
                        .computeIfAbsent(link.getTo(), linkedHashSetGenerator)
                        .addAll(link.getToColumnList());
                tableLinkedColumnsMapper
                        .computeIfAbsent(link.getFrom(), linkedHashSetGenerator)
                        .addAll(link.getFromColumnList());
            }
        }

        RolapStar.Table factTable = factAndLinks.getLeft();
        // The fact table subquery should also select dimension columns.
        Set<RolapSchema.PhysColumn> factTableColumns = tableLinkedColumnsMapper
                .computeIfAbsent(factTable.getRelation(), linkedHashSetGenerator);
        for (SqlQueryBuilder.Column column : dimensionCols) {
            if (Objects.equals(factAndLinks.getLeft().getRelation(), column.getTable().getPhysRelation())) {
                factTableColumns.add(column.getPhysColumn());
            }
        }
        if (weightColumns != null) {
            for (RolapSchema.PhysColumn column : weightColumns) {
                if (Objects.equals(factTable.getRelation(), column.relation)) {
                    factTableColumns.add(column);
                }
            }
        }
        if (filterCols != null) {
            for (SqlQueryBuilder.Column column : filterCols) {
                if (Objects.equals(factTable.getRelation(), column.getTable().getPhysRelation())) {
                    factTableColumns.add(column.getPhysColumn());
                }
            }
        }

        return tableLinkedColumnsMapper;
    }

    private Map<RolapStar.Measure, String> replaceMeasureExpressions(
            Map<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> tableLinkedColumnsMapper) {
        RolapSchema.PhysRelation factTable = factAndLinks.getLeft().getRelation();
        Map<RolapStar.Measure, String> measureAliasAndInnerAliasMapper = measureIndexMapper.entrySet().stream()
                .filter(entry -> entry.getValue().isSum())
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        entry -> getFactSubQueryMeasureAlias(entry.getValue(), tableLinkedColumnsMapper.get(factTable))));

        for (int i = 0; i < select.size(); i++) {
            RolapStar.Measure measure = measureIndexMapper.get(i);
            if (measure == null) {
                continue;
            }

            String innerAlias = measureAliasAndInnerAliasMapper.get(measure);
            if (innerAlias == null) {
                continue;
            }

            String newExpression = select.get(i).replaceFirst(
                    "\\$" + StringEscapeUtils.escapeJava(measure.getName()) + "\\$\\)",
                    getDialect().quoteIdentifier(innerAlias) + ')');
            select.set(i, newExpression);
        }

        return measureAliasAndInnerAliasMapper;
    }

    @Override
    protected String generateSelectFirstString() {
        StringBuilder selectFirstBuilder = new StringBuilder("select ");
        if (modelHints != null && modelHints.size() == 1) {
            selectFirstBuilder.append("/*+ MODEL_PRIORITY(");
            selectFirstBuilder.append(String.join(", ", modelHints));
            selectFirstBuilder.append(") */ ");
        }
        if (distinct) {
            selectFirstBuilder.append("distinct ");
        }
        return selectFirstBuilder.toString();
    }

    private void createFactAndLinks() {
        Map<String, RolapSchema.PhysPath> pathMap = factAndPath.right;
        Set<List<RolapSchema.PhysLink>> links = new HashSet<>(8);
        for (SqlQueryBuilder.Column dimensionCol : dimensionCols) {
            RolapSchema.PhysPath path = pathMap.get(dimensionCol.getSql());
            if (path.getLinks() != null && path.getLinks().size() != 0) {
                links.add(path.getLinks());
            }
        }

        if (isCellCalc() && MondrianProperties.instance().JoinDimsForCellCalc.get()) {
            for (Map.Entry<String, RolapSchema.PhysPath> entry : pathMap.entrySet()) {
                RolapSchema.PhysPath path = entry.getValue();
                if (path.getLinks() != null && path.getLinks().size() != 0) {
                    links.add(path.getLinks());
                }
            }
        }

        for (SqlQueryBuilder.Column filterCol : filterCols) {
            RolapSchema.PhysPath path = pathMap.get(filterCol.getSql());
            if (path.getLinks() != null || path.getLinks().size() != 0) {
                links.add(path.getLinks());
            }
        }
        for (Map.Entry<Integer, RolapStar.Measure> entry : measureIndexMapper.entrySet()) {
            RolapStar.Measure measure = entry.getValue();

            if (measure.getExpression() == null) {
                continue;
            }
            RolapSchema.PhysPath path = pathMap.get(measure.getExpression().toSql());
            if (path != null && path.getLinks() != null && path.getLinks().size() != 0) {
                links.add(path.getLinks());
            }
        }
        if (repeatCol != null) {
            RolapSchema.PhysPath path = pathMap.get(repeatCol);
            if (path != null && path.getLinks() != null && path.getLinks().size() != 0) {
                links.add(path.getLinks());
            }
        }
        factAndLinks = new Pair<>(factAndPath.getKey(), links);
    }

    @Override
    protected boolean addFromTable(
            final String schema,
            final String name,
            final String alias,
            final String filter,
            final Map<String, String> hintMap,
            final String parentAlias,
            final String joinCondition,
            final boolean failIfExists)
    {
        return true;
    }

    public StringBuilder toOriginBuffWithLimit(StringBuilder buf, String prefix) {
        return super.toBuffer(buf, prefix);
    }

    private StringBuilder createM2MSQL(StringBuilder buf, String prefix) {
        KylinSqlQuery innerQuery = new KylinSqlQuery(dialect, true);
        innerQuery.setHasLimit(false);
        innerQuery.setFactAndPath(factAndPath);
        innerQuery.setDimensionCols(dimensionCols);
        innerQuery.setFilterCols(filterCols);
        innerQuery.setMeasureIndexMapper(measureIndexMapper);
        innerQuery.setCellCalc(cellCalc);
        // m2mPk may be not in fact table
        String repeatBasisCol;
        int dot = m2mPk.indexOf('.');
        if (dot != -1) {
            String tableAlias = m2mPk.substring(0, dot);
            String colName = m2mPk.substring(dot + 1);
            repeatBasisCol = "\"" + tableAlias + "\".\"" + colName.toUpperCase() + "\"";
            innerQuery.setRepeatCol(repeatBasisCol);
        } else {
            repeatBasisCol = "\"" + factAndPath.getKey().getAlias() + "\".\"" + m2mPk.toUpperCase() + "\"";
        }
        innerQuery.addSelectGroupBy(repeatBasisCol, null);
        for (SqlQueryBuilder.Column column : dimensionCols) {
            innerQuery.addSelectGroupBy(column.getSql(), null);
            innerQuery.addOrderBy(column.getSql(), true, false, true);
        }
        // add measures
        int i = 0;
        for (Map.Entry<Integer, RolapStar.Measure> entry : measureIndexMapper.entrySet()) {
            RolapStar.Measure measure = entry.getValue();
            String exprInner;
            if (measure.getExpression() == null) {
                exprInner = "*";
            } else {
                exprInner = measure.getExpression().toSql();
            }
            String exprOuter = measure.getAggregator().getExpression(exprInner) + "/count(*)";
            innerQuery.addSelect(exprOuter, measure.getInternalType(), "m" + i);
            i++;
        }
        // add from
        innerQuery.setDistinct(distinct);
        innerQuery.where.addAll(where);

        // generate outer query
        KylinSqlQuery outerQuery = new KylinSqlQuery(dialect, false);
        outerQuery.setDistinct(false);
        for (i = 0; i < dimensionCols.size(); i++) {
            String selectCol = "\""  + "c" + (i + 1) + "\"";
            outerQuery.select.add(selectCol);
            outerQuery.groupBy.add(selectCol);
            outerQuery.orderBy.add(selectCol);
        }
        for (i = 0; i < measureIndexMapper.size(); i++) {
            outerQuery.select.add("sum(\"" + "m" + i + "\") as \"m" + i + '\"');
        }
        outerQuery.from.add("(" + Util.getNlBySqlFormatted() + innerQuery.toString() + Util.getNlBySqlFormatted() + ")");
        return outerQuery.toOriginBuffWithLimit(buf, prefix);
    }

    private boolean needCreateM2MSQL() {
        if (measureIndexMapper.size() == 0 || m2mPk == null || m2mPk.trim().length() == 0 || isCustomRollup() || innerQuery) {
            return false;
        } else {
            for (Map.Entry<Integer, RolapStar.Measure> entry : measureIndexMapper.entrySet()) {
                if (entry.getValue().isM2MAccumulative()) {
                    return true;
                }
            }
            return false;
        }
    }

    private void buildJoinClause(KylinSqlQuery.FromClauseList fromClauseList,
                                 Map<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> tableLinkedColumnsMapper,
                                 Map<RolapStar.Measure, String> measureAliasAndInnerAliasMapper) {
        RolapStar.Table factTable = factAndLinks.left;
        Set<List<RolapSchema.PhysLink>> linkSets = factAndLinks.right;
        boolean needJoinClause = linkSets != null && linkSets.size() != 0;

        // add fact table
        if (isCustomRollup() && needJoinClause) {
            fromClauseList.add(generateFactTableSubQuery(factTable, tableLinkedColumnsMapper, measureAliasAndInnerAliasMapper));
        } else {
            fromClauseList.add(generateJoinClauseStr((RolapSchema.PhysTable) factTable.getRelation(), null));
        }

        if (needJoinClause) {
            // add join tables
            Set<RolapSchema.PhysLink> usedLinkSet = new HashSet<>();
            for (List<RolapSchema.PhysLink> links : linkSets) {
                for (RolapSchema.PhysLink link : links) {
                    if (usedLinkSet.contains(link)) {
                        continue;
                    }

                    String subQuery;
                    if (isCustomRollup() && nonQueryingM2MTables.contains(link.getTo())) {
                        Set<RolapSchema.PhysColumn> nonQueryingM2MTableColumns = tableLinkedColumnsMapper.get(link.getTo());
                        subQuery = generateNonQueryingM2MTableSubQuery((RolapSchema.PhysTable)link.getTo(), nonQueryingM2MTableColumns, link);
                    } else {
                        subQuery = generateJoinClauseStr((RolapSchema.PhysTable)link.getTo(), link);
                    }

                    fromClauseList.add(subQuery);
                    fromClauseList.seps.add(" " + link.type + " join ");
                    usedLinkSet.add(link);
                }
            }
        }

    }

    private String generateFactTableSubQuery(RolapStar.Table factTable,
                                             Map<RolapSchema.PhysRelation, Set<RolapSchema.PhysColumn>> tableLinkedColumnsMapper,
                                             Map<RolapStar.Measure, String> measureAliasAndInnerAliasMapper) {
        List<RolapStar.Measure> sumMeasures = measureIndexMapper.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(RolapStar.Measure::isSum)
                .collect(Collectors.toList());

        StringBuilder factInnerQueryBuilder = new StringBuilder();
        factInnerQueryBuilder.append('(').append(Util.nl).append("select ");

        String keyColumns = tableLinkedColumnsMapper.get(factTable.getRelation()).stream()
                .map(key -> dialect.quoteIdentifier(key.name))
                .collect(Collectors.joining(", "));

        String measureColumns = sumMeasures.stream()
                .map(measure -> {
                    String quotedName = dialect.quoteIdentifier(measure.getExpression().name);
                    String alias = dialect.quoteIdentifier(measureAliasAndInnerAliasMapper.get(measure));
                    return measure.getAggregator().getExpression(quotedName) + " as " + alias;
                })
                .collect(Collectors.joining(", "));

        factInnerQueryBuilder.append(keyColumns);
        if (!keyColumns.isEmpty() && !measureColumns.isEmpty()) {
            factInnerQueryBuilder.append(", ");
        }
        factInnerQueryBuilder.append(measureColumns);

        RolapSchema.PhysTable factTableRelation = (RolapSchema.PhysTable)factTable.getRelation();
        factInnerQueryBuilder.append(Util.nl).append("from ");
        factInnerQueryBuilder.append(dialect.quoteIdentifier(factTableRelation.getSchemaName()));
        factInnerQueryBuilder.append('.');
        factInnerQueryBuilder.append(dialect.quoteIdentifier(factTableRelation.getName()));

        factInnerQueryBuilder.append(Util.nl).append("group by ");
        factInnerQueryBuilder.append(keyColumns);

        factInnerQueryBuilder.append(") as ");
        factInnerQueryBuilder.append(dialect.quoteIdentifier(factTable.getAlias()));
        factInnerQueryBuilder.append(Util.nl);

        return factInnerQueryBuilder.toString();
    }

    private static String getFactSubQueryMeasureAlias(RolapStar.Measure starMeasure, Set<RolapSchema.PhysColumn> factColumns) {
        assert starMeasure.isSum();

        String measureAlias = starMeasure.getExpression().name.toUpperCase() + "_SUM";
        Set<String> unavailableSuffixes = null;
        for (RolapSchema.PhysColumn column : factColumns) {
            String columnName = column.name;

            int duplicateIndex = columnName.toUpperCase().indexOf(measureAlias);
            if (duplicateIndex >= 0) {
                if (unavailableSuffixes == null) {
                    unavailableSuffixes = new HashSet<>();
                }

                if (columnName.length() == measureAlias.length()) {
                    unavailableSuffixes.add("0");
                } else {
                    String columnSuffix = columnName.substring(measureAlias.length());
                    if (columnSuffix.startsWith("_")) {
                        String columnSuffixContent = columnSuffix.substring(1);
                        unavailableSuffixes.add(columnSuffixContent);
                    }
                }
            }
        }

        if (unavailableSuffixes != null) {
            int i = 0;
            while (unavailableSuffixes.contains(String.valueOf(i))) {
                i++;
            }

            measureAlias = measureAlias + '_' + i;
        }

        return measureAlias;
    }

    private String generateJoinClauseStr(RolapSchema.PhysTable table, RolapSchema.PhysLink link) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(table.getSchemaName());
        sb.append("\".\"");
        sb.append(table.getName());
        sb.append("\" as \"");
        sb.append(table.getAlias());
        sb.append("\"");
        if (link != null) {
            sb.append(" on ");
            sb.append(link.sql);
        }
        return sb.toString();
    }

    private String generateNonQueryingM2MTableSubQuery(RolapSchema.PhysTable table,
                                                       Set<RolapSchema.PhysColumn> nonQueryingM2MTableColumns,
                                                       RolapSchema.PhysLink link) {
        StringBuilder tableInnerQueryBuilder = new StringBuilder();
        tableInnerQueryBuilder.append('(').append(Util.nl).append("select ");

        String selectColumns = nonQueryingM2MTableColumns.stream()
                .map(column -> dialect.quoteIdentifier(column.name))
                .collect(Collectors.joining(", "));
        tableInnerQueryBuilder.append(selectColumns);

        tableInnerQueryBuilder.append(Util.nl).append("from ");
        tableInnerQueryBuilder.append(dialect.quoteIdentifier(table.getSchemaName()));
        tableInnerQueryBuilder.append('.');
        tableInnerQueryBuilder.append(dialect.quoteIdentifier(table.getName()));

        tableInnerQueryBuilder.append(Util.nl).append("group by ");
        tableInnerQueryBuilder.append(selectColumns);

        tableInnerQueryBuilder.append(") as ");
        tableInnerQueryBuilder.append(dialect.quoteIdentifier(table.getAlias()));
        tableInnerQueryBuilder.append(Util.nl);

        if (link != null) {
            tableInnerQueryBuilder.append(" on ");
            tableInnerQueryBuilder.append(link.sql);
        }

        return tableInnerQueryBuilder.toString();
    }

    static class FromClauseList extends SqlQuery.FromClauseList {

        public List<String> seps = new ArrayList<>();

        FromClauseList(boolean allowsDups) {
            super(allowsDups);
        }

        @Override
        protected void toBuffer(
                final StringBuilder buf,
                final String first,
                final String sep,
                final String last) {
            buf.append(first);
            for (int n = 0; n < this.size(); n++) {
                if (n > 0) {
                    if (this.seps.size() > 0 && this.size() == this.seps.size() + 1) {
                        buf.append(this.seps.get(n - 1));
                    } else {
                        buf.append(sep);
                    }
                }
                buf.append(this.get(n));
            }
            buf.append(last);
        }
    }


    @Override
    public void addOrderBy(
            String expr,
            boolean ascending,
            boolean prepend,
            boolean nullable)
    {
        if (MondrianProperties.instance().EnableOrderByInSQL.get()) {
            super.addOrderBy(expr, expr, ascending, prepend, nullable, true);
        }
    }

    @Override
    public void addOrderBy(
            String expr,
            String alias,
            boolean ascending,
            boolean prepend,
            boolean nullable,
            boolean collateNullsLast)
    {
        if (MondrianProperties.instance().EnableOrderByInSQL.get()) {
            super.addOrderBy(expr, expr, ascending, prepend, nullable, collateNullsLast);
        }
    }
}
