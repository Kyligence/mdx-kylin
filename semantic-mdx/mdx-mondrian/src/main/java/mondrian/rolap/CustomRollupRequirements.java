package mondrian.rolap;

import mondrian.rolap.sql.CustomizeSqlQuery;
import mondrian.rolap.sql.SqlQueryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for Custom Rollup SQL generation.
 */
public class CustomRollupRequirements {
    /**
     * M2M tables which Custom Rollup Hierarchies are defined on and not selected by the current MDX query.<br>
     * Null for non-Custom-Rollup queries.
     */
    private Set<RolapSchema.PhysRelation> nonQueryingM2MTables;
    /**
     * Weight columns which are defined on Custom Rollup Hierarchies.<br>
     * Null for non-Custom-Rollup queries.
     */
    private List<RolapSchema.PhysColumn> weightColumns;

    public CustomRollupRequirements() {
        this.nonQueryingM2MTables = null;
        this.weightColumns = null;
    }

    public void addCustomRollupLevel(RolapCubeLevel level) {
        if (level.getHierarchy().isCustomRollup()) {
            if (nonQueryingM2MTables == null) {
                nonQueryingM2MTables = new LinkedHashSet<>();
                for (RolapCubeHierarchy hierarchy : level.getCube().getHierarchyList()) {
                    if (hierarchy.isCustomRollup()) {
                        nonQueryingM2MTables.add(hierarchy.getDimension().getKeyTable());
                    }
                }
            }
            nonQueryingM2MTables.remove(level.getHierarchy().getDimension().getKeyTable());

            if (weightColumns == null) {
                weightColumns = new ArrayList<>();
            }
            for (RolapCubeLevel currentLevel = level.getChildLevel(); currentLevel != null; currentLevel = currentLevel.getChildLevel()) {
                RolapAttribute weightAttribute = currentLevel.getWeightAttribute();
                if (weightAttribute != null) {
                    weightColumns.addAll(weightAttribute.getKeyList());
                }
            }
        }
    }

    public void writeNonQueryingM2MTables(SqlQueryBuilder queryBuilder) {
        if (nonQueryingM2MTables != null && queryBuilder.sqlQuery instanceof CustomizeSqlQuery) {
            CustomizeSqlQuery customizeSqlQuery = ((CustomizeSqlQuery)queryBuilder.sqlQuery);
            customizeSqlQuery.initNonQueryingM2MTable();
            for (RolapSchema.PhysRelation table : nonQueryingM2MTables) {
                customizeSqlQuery.addNonQueryingM2MTable(table);
            }

            customizeSqlQuery.setWeightedColumns(Collections.unmodifiableList(weightColumns));
        }
    }

    public String getWeightedMeasureExpression(RolapStar.Measure starMeasure, SqlQueryBuilder queryBuilder) {
        String innerMeasureAlias;
        if (starMeasure.isSum() && weightColumns != null) {
            StringBuilder expressionBuilder = new StringBuilder();
            for (RolapSchema.PhysColumn weightColumn : weightColumns) {
                expressionBuilder.append(weightColumn.toSql());
                expressionBuilder.append(" * ");
            }
            expressionBuilder.append(queryBuilder.getDialect().quoteIdentifier(starMeasure.getTable().getAlias()));
            expressionBuilder.append(".$");
            expressionBuilder.append(starMeasure.getName());
            expressionBuilder.append('$');
            innerMeasureAlias = expressionBuilder.toString();
        } else {
            innerMeasureAlias = starMeasure.getExpression().toSql();
        }
        return innerMeasureAlias;
    }

    @Override
    public String toString() {
        return "CustomRollupRequirements{" +
                "nonQueryingM2MTables=" + nonQueryingM2MTables +
                ", weightColumns=" + weightColumns +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomRollupRequirements that = (CustomRollupRequirements)o;
        return Objects.equals(nonQueryingM2MTables, that.nonQueryingM2MTables)
                && Objects.equals(weightColumns, that.weightColumns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonQueryingM2MTables, weightColumns);
    }

    public List<String> getNonQueryingM2MTableAliases() {
        if (nonQueryingM2MTables == null) {
            return Collections.emptyList();
        }

        return nonQueryingM2MTables.stream()
                .map(RolapSchema.PhysRelation::getAlias)
                .collect(Collectors.toList());
    }

    public List<String> getWeightColumnSqls() {
        if (weightColumns == null) {
            return Collections.emptyList();
        }

        return weightColumns.stream()
                .map(RolapSchema.PhysColumn::toSql)
                .collect(Collectors.toList());
    }
}
