package mondrian.rolap;

import mondrian.olap.Util;
import mondrian.rolap.sql.TupleConstraint;
import mondrian.server.monitor.SqlStatementEvent;
import mondrian.spi.Dialect;
import mondrian.util.Pair;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Similar to {@link SqlTupleReader}, but reads literal values from datasource.
 * Currently it is only used with {@link RolapNativeCount}.
 */
public class SqlDescendantsLeavesAggrNumValuesReader extends SqlReaderBase {
    public SqlDescendantsLeavesAggrNumValuesReader(TupleConstraint constraint) {
        super(constraint);
    }

    public Map<List<RolapMember>, Integer> readIntValues(
            Dialect dialect, DataSource dataSource, String aggrExp,
            List<RolapMember> currentMembers) {
        String message = "Populating int aggregation value for " + targets;

        Pair<String, List<SqlStatement.Type>> pair = makeLevelMembersSql(dialect, false);
        int selectColumnCount = pair.right.size();

        // Currentmember indices in targets
        List<Integer> currentMemberTargetIndices = new ArrayList<>();
        // Target indices in sql
        Set<Integer> targetColumnIndices = new TreeSet<>();
        for (RolapMember currentMember : currentMembers) {
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i).getLevel().equals(currentMember.getLevel()))
                    currentMemberTargetIndices.add(i);
            }

            for (RolapCubeLevel currentLevel = currentMember.getLevel()
                 ;currentLevel != null;
                 currentLevel = currentLevel.getParentLevel()) {
                if (targetsLayoutMap.get(currentLevel) != null) {
                    getAllNonNegativeKeys(targetColumnIndices, targetsLayoutMap.get(currentLevel));
                }
            }
        }
        String sql = generateSql(pair.left, aggrExp, selectColumnCount, targetColumnIndices);

        SqlStatement stmt = null;
        try {
            stmt = RolapUtil.executeQuery(
                    dataSource, sql,
                    new SqlStatement.StatementLocus(
                            getExecution(),
                            "SqlDescendantsLeavesAggrNumValuesReader.readIntValue " + getPartialTargets(),
                            message,
                            SqlStatementEvent.Purpose.TUPLES, 0));
            ResultSet resultSet = stmt.getResultSet();

            Map<List<RolapMember>, Integer> result = new HashMap<>();
            if (!targetColumnIndices.isEmpty()) {
                // Read members and their aggregations from the result
                for (int currentMemberTargetIndex : currentMemberTargetIndices) {
                    targets.get(currentMemberTargetIndex).open();
                }
                while (resultSet.next()) {
                    List<RolapMember> members = new ArrayList<>();
                    for (int currentMemberTargetIndex : currentMemberTargetIndices) {
                        Target target = targets.get(currentMemberTargetIndex);
                        target.setCurrMember(null);
                        RolapMember member = target.addRow(stmt);
                        members.add(member);
                    }
                    result.put(members, resultSet.getInt(selectColumnCount + 1));
                    ++stmt.rowCount;
                }
                for (int currentMemberTargetIndex : currentMemberTargetIndices) {
                    targets.get(currentMemberTargetIndex).close();
                }
            } else {
                resultSet.next();
                result.put(currentMembers, resultSet.getInt(1));
            }
            return result;
        } catch (SQLException e) {
            throw stmt.handle(e);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Filter all keys in a column layout, only get non-negative keys with no duplicate.
     */
    private static void getAllNonNegativeKeys(Set<Integer> target, LevelColumnLayout<Integer> source) {
        if (target == null || source == null)
            return;

        Collection<Integer> allKeys = source.getAllKeys();
        if (allKeys == null)
            return;

        for (int i : allKeys) {
            if (i >= 0)
                target.add(i);
        }
    }

    private Pair<CharSequence, CharSequence> getSelectGroupByColumns(int originalColumnCount, Set<Integer> targetColumns) {
        String[] selectColumns = new String[originalColumnCount];
        String[] groupByColumns = new String[targetColumns.size()];
        // Just add some placeholders, which will not be used.
        // These placeholders are to ensure that the columns in the
        // generated sql are in the same order as the original sql.
        for (int i = 0; i < originalColumnCount; i++) {
            selectColumns[i] = "0";
        }

        int i = 0;
        for (int targetColumn : targetColumns) {
            selectColumns[targetColumn] = groupByColumns[i++] =
                    '"' + columnAliasPrefix + targetColumn + '"';
        }

        String selectString = String.join(", ", selectColumns);
        String groupByString = String.join(", ", groupByColumns);

        return new Pair<>(selectString, groupByString);
    }

    private String generateSql(String sql, String aggrExp, int originalColumnCount, Set<Integer> targetColumns) {
        assert sql != null && !sql.isEmpty();

        if (!targetColumns.isEmpty()) {
            Pair<CharSequence, CharSequence> selectGroupByColumns = getSelectGroupByColumns(originalColumnCount, targetColumns);
            sql = "select " + selectGroupByColumns.left + ", " + aggrExp +
                    " from (" + Util.getNlBySqlFormatted() + sql + Util.getNlBySqlFormatted()
                    + ") group by " + selectGroupByColumns.right + Util.getNlBySqlFormatted()
                    + "order by " + selectGroupByColumns.right;
        } else {
            // All currentmembers are [All]
            sql = "select " + aggrExp + " from (" + Util.getNlBySqlFormatted() + sql + Util.getNlBySqlFormatted() + ")";
        }
        return sql;
    }

    public Object getCacheKey() {
        List<Object> key = new ArrayList<>();
        key.add(constraint.getCacheKey());
        key.add(SqlDescendantsLeavesAggrNumValuesReader.class);
        for (Target target : targets) {
            if (target.srcMembers != null) {
                key.add(target.getLevel());
            }
        }
        return key;
    }
}
