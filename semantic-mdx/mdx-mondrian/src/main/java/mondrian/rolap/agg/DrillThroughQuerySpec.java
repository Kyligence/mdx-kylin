/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.rolap.agg;

import mondrian.olap.MondrianProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapAggregator;
import mondrian.util.Pair;
import mondrian.rolap.RolapSchema;
import mondrian.rolap.RolapStar;
import mondrian.rolap.SqlStatement;
import mondrian.rolap.StarColumnPredicate;
import mondrian.rolap.StarPredicate;
import mondrian.rolap.sql.Clause;
import mondrian.rolap.sql.SqlQueryBuilder;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides the information necessary to generate SQL for a drill-through
 * request.
 *
 * @author jhyde
 * @author Richard M. Emberson
 */
class DrillThroughQuerySpec extends AbstractQuerySpec {
    private final DrillThroughCellRequest request;
    private final int maxRowCount;
    private final List<StarPredicate> listOfStarPredicates;
    private final List<String> columnNames;

    private final boolean doAggregation;

    public DrillThroughQuerySpec(
        DrillThroughCellRequest request,
        int maxRowCount,
        StarPredicate starPredicateSlicer,
        boolean countOnly)
    {
        super(request.getMeasure().getStar(), countOnly);
        this.request = request;
        this.maxRowCount = maxRowCount;
        if (starPredicateSlicer != null) {
            this.listOfStarPredicates =
                Collections.singletonList(starPredicateSlicer);
        } else {
            this.listOfStarPredicates = Collections.emptyList();
        }
        int maxColumnNameLength =
            getStar().getSqlQueryDialect().getMaxColumnNameLength();
        if (maxColumnNameLength == 0) {
            // From java.sql.DatabaseMetaData: "a result of zero means that
            // there is no limit or the limit is not known"
            maxColumnNameLength = Integer.MAX_VALUE;
        }
        this.columnNames =
            computeDistinctColumnNames(request, maxColumnNameLength);
        this.doAggregation = MondrianProperties.instance().SqlWithAggregate.get();
    }

    private static List<String> computeDistinctColumnNames(
        DrillThroughCellRequest request, int maxColumnNameLength)
    {
        final List<String> columnNames = new ArrayList<String>();
        final Set<String> columnNameSet = new HashSet<String>();

        for (RolapStar.Column column : request.getConstrainedColumns()) {
            addColumnName(
                request.getColumnAlias(column),
                column,
                maxColumnNameLength,
                columnNames,
                columnNameSet);
        }

        addColumnName(
            request.getMeasure().getName(),
            request.getMeasure(),
            maxColumnNameLength,
            columnNames,
            columnNameSet);

        return columnNames;
    }

    private static void addColumnName(
        String columnName,
        final RolapStar.Column column,
        int maxColumnNameLength,
        final List<String> columnNames,
        final Set<String> columnNameSet)
    {
        if (columnName == null) {
            columnName = column.getName();
            if (columnName != null) {
                if (columnName.startsWith("$")) {
                    // internal property name. truncate the $.
                    columnName = columnName.replace("$", "");
                }
            } else if (column.getExpression()
                instanceof RolapSchema.PhysRealColumn)
            {
                columnName =
                    ((RolapSchema.PhysRealColumn) column.getExpression()).name;
            } else {
                columnName = "c" + Integer.toString(columnNames.size());
            }
        }
        // Register the column name, and if it's not unique, append numeric
        // suffixes until it is. Also make sure that it is within the
        // range allowed by this SQL dialect.
        String originalColumnName = columnName;
        if (columnName.length() > maxColumnNameLength) {
            columnName = columnName.substring(0, maxColumnNameLength);
        }
        for (int j = 0; !columnNameSet.add(columnName); j++) {
            final String suffix = "_" + Integer.toString(j);
            columnName = originalColumnName;
            if (originalColumnName.length() + suffix.length()
                > maxColumnNameLength)
            {
                columnName =
                    originalColumnName.substring(
                        0, maxColumnNameLength - suffix.length());
            }
            columnName += suffix;
        }
        columnNames.add(columnName);
    }

    @Override
    protected boolean isPartOfSelect(RolapStar.Column col) {
        return request.includeInSelect(col);
    }

    @Override
    protected boolean isPartOfSelect(RolapStar.Measure measure) {
        return request.includeInSelect(measure);
    }

    @Override
    public List<Pair<RolapStar.Measure, String>> getMeasures() {
        final List<RolapStar.Measure> drillThroughMeasures =
            request.getDrillThroughMeasures();
        if (drillThroughMeasures.size() > 0) {
            return new AbstractList<Pair<RolapStar.Measure, String>>() {
                @Override
                public int size() {
                    return drillThroughMeasures.size();
                }

                @Override
                public Pair<RolapStar.Measure, String> get(int index) {
                    final RolapStar.Measure measure =
                        drillThroughMeasures.get(index);
                    return Pair.of(measure, measure.getName());
                }
            };
        } else {
            return Collections.singletonList(
                Pair.of(
                    request.getMeasure(),
                    Util.last(columnNames)));
        }
    }

    @Override
    public List<Pair<RolapStar.Column, String>> getColumns() {
        final RolapStar.Column[] constrainedColumns =
            request.getConstrainedColumns();
        return new AbstractList<Pair<RolapStar.Column, String>>() {
            @Override
            public int size() {
                return constrainedColumns.length;
            }

            @Override
            public Pair<RolapStar.Column, String> get(int index) {
                return Pair.of(
                    constrainedColumns[index],
                    columnNames.get(index));
            }
        };
    }

    @Override
    public StarColumnPredicate getColumnPredicate(final int i) {
        final StarColumnPredicate constraint = request.getValueAt(i);
        return (constraint == null)
            ? Predicates.wildcard(
                new PredicateColumn(
                    RolapSchema.BadRouter.INSTANCE,
                    request.getConstrainedColumns()[i].getExpression()),
                true)
            : constraint;
    }

    @Override
    public Pair<String, List<SqlStatement.Type>> generateSqlQuery(String desc) {
        SqlQueryBuilder queryBuilder = createQueryBuilder(desc);
        nonDistinctGenerateSql(queryBuilder);
        queryBuilder.sqlQuery.setHasLimit(true);
        queryBuilder.sqlQuery.setLimitCount(maxRowCount);
        return queryBuilder.toSqlAndTypes(getStar().getFactAndPath(), null, null);
    }

    @Override
    protected void addMeasure(
        RolapStar.Measure measure,
        String alias,
        final SqlQueryBuilder queryBuilder)
    {
        if (!isPartOfSelect(measure)) {
            return;
        }

        assert measure.getTable() == getStar().getFactTable();
        String exprInner;
        boolean isCountDistinct = measure.getAggregator() == RolapAggregator.DistinctCount;
        if (measure.getExpression() == null) {
            exprInner = doAggregation ? "*" : "1";
        } else {
            exprInner = isCountDistinct && !doAggregation? "1" : measure.getExpression().toSql();
            queryBuilder.addColumn(
                queryBuilder.column(
                    measure.getExpression(), measure.getTable()),
                Clause.FROM);
        }

        if (!countOnly) {
            String exprOuter = doAggregation ? measure.getAggregator().getExpression(exprInner) : exprInner;
            queryBuilder.sqlQuery.addSelect(
                exprOuter,
                isCountDistinct && !doAggregation? SqlStatement.Type.INT : measure.getInternalType(),
                alias);
        }
    }

    @Override
    protected boolean isAggregate() {
        // As per SSAS 2005, the query must include a Group By clause
        // so that each row returned contains the aggregated value
        // of the measures for all rows with similar key values.
        return doAggregation;
    }

    @Override
    protected boolean isOrdered() {
        // We don't order drillthrough queries.
        return false;
    }

    @Override
    protected List<StarPredicate> getPredicateList() {
        return listOfStarPredicates;
    }
}

// End DrillThroughQuerySpec.java
