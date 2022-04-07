package io.kylin.mdx.insight.mondrian.subquery;

import mondrian.olap.Query;
import mondrian.olap.SubQuery;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.context.MDDataSet;
import mondrian.xmla.context.MDDataSet_Multidimensional;
import mondrian.xmla.context.XmlaExtra;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.PreparedOlapStatement;

import java.sql.SQLException;

public class SubQueryTest extends SubQueryEnv {

    public static void main(String[] args) {
        new XmlaRequestContext().useMondrian = true;
        new SubQueryTest().run();
    }

    @Override
    public void execute(OlapConnection connection) throws SQLException {
        try (PreparedOlapStatement statement = connection.prepareOlapStatement(MDX[0])) {
            Query query = getQuery(statement);
            if (query instanceof SubQuery) {
                SubQuery subQuery = (SubQuery) query;
                System.out.println("子查询语句");
                System.out.println("MDX: \n" + subQuery.toString());
            } else {
                System.out.println("普通查询语句");
                System.out.println("MDX: \n" + query.toString());
                CellSet cellSet = statement.executeQuery();
                outputCellSet(connection, cellSet);
            }
        }
    }

    private void outputCellSet(OlapConnection connection, CellSet cellSet) {
        final XmlaExtra extra = getConnectionFactory().getExtra();
        extra.setPreferList(connection);
        MDDataSet dataSet = new MDDataSet_Multidimensional(cellSet, null, extra, true, true);
        System.out.println(dataSet.toString());
    }

    private static final String[] MDX = new String[]{
            "// 子查询语句\n" +
                    "SELECT \n" +
                    "  {[Measures].[_COUNT_],[Measures].[MAX_PRICE],[Measures].[MIN_PRICE],[Measures].[SUM_PRICE]} \n" +
                    "      DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,\n" +
                    "  NON EMPTY Hierarchize(\n" +
                    "    AddCalculatedMembers(\n" +
                    "      {DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}\n" +
                    "    )\n" +
                    "  )   DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS\n" +
                    "FROM NON VISUAL (\n" +
                    "  SELECT \n" +
                    "    ({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01]}) ON COLUMNS\n" +
                    "  FROM [learn_kylin]\n" +
                    ") \n" +
                    "CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS",
            "// 非子查询下钻\n" +
                    "WITH \n" +
                    "  SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01]), Descendants([KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01])})))'\n" +
                    "SELECT \n" +
                    "  {[Measures].[_COUNT_],[Measures].[MAX_PRICE],[Measures].[MIN_PRICE],[Measures].[SUM_PRICE]}\n" +
                    "      DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,\n" +
                    "  NON EMPTY Hierarchize(\n" +
                    "    Intersect(\n" +
                    "      AddCalculatedMembers(\n" +
                    "        {DrilldownLevel({DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})},[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT])}\n" +
                    "      ),\n" +
                    "      [XL_Row_Dim_0]\n" +
                    "    )\n" +
                    "  )   DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS\n" +
                    "FROM [learn_kylin]\n" +
                    "CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS",
            "// 普通查询\n" +
                    "SELECT \n" +
                    "  {[Measures].[_COUNT_],[Measures].[MAX_PRICE],[Measures].[MIN_PRICE],[Measures].[SUM_PRICE]} \n" +
                    "      DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS ,\n" +
                    "  NON EMPTY Hierarchize(\n" +
                    "    AddCalculatedMembers(\n" +
                    "      {DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}\n" +
                    "    )\n" +
                    "  )   DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS\n" +
                    "FROM [learn_kylin] \n" +
                    "CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS"
    };

}
