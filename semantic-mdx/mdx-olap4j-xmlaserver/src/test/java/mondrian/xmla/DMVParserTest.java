package mondrian.xmla;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;

public class DMVParserTest {
    private static class TestDMV {
        String dmvStatement;
        Set<String> selectColumns;
        String requestType;
        Map<String, DMVParser.DMVRestriction> selectRestrictions;

        public TestDMV(String dmvStatement, String[] selectColumns, String requestType, String[] restrictionColumns, DMVParser.DMVRestriction[] dmvRestrictions) {
            this.dmvStatement = dmvStatement;

            if (selectColumns != null) {
                this.selectColumns = new HashSet<>();
                this.selectColumns.addAll(Arrays.asList(selectColumns));
            }

            this.requestType = requestType;

            if (restrictionColumns != null && dmvRestrictions != null && restrictionColumns.length == dmvRestrictions.length) {
                selectRestrictions = new HashMap<>();
                for (int i = 0; i < restrictionColumns.length; i++) {
                    this.selectRestrictions.put(restrictionColumns[i], dmvRestrictions[i]);
                }
            }
        }
    }

    private static TestDMV[] testDMVs;

    private static Map<String, String> parameters;

    static {
        TestDMV testDMV1 = new TestDMV(
                "select [CATALOG_NAME] from $system.DBSCHEMA_CATALOGS",
                new String[]{"CATALOG_NAME"},
                "DBSCHEMA_CATALOGS",
                new String[]{},
                new DMVParser.DMVRestriction[]{});

        TestDMV testDMV2 = new TestDMV(
                "select [CUBE_NAME], [BASE_CUBE_NAME], [CUBE_CAPTION] from " +
                        "$system.mdschema_cubes where [CUBE_SOURCE] = 1",
                new String[]{"CUBE_NAME", "BASE_CUBE_NAME", "CUBE_CAPTION"},
                "mdschema_cubes",
                new String[]{"CUBE_SOURCE"},
                new DMVParser.DMVRestriction[]{new DMVParser.DMVEqualsRestriction("1")});

        TestDMV testDMV3 = new TestDMV("select [MEASURE_UNIQUE_NAME], [MEASURE_CAPTION], " +
                "[DATA_TYPE], [MEASUREGROUP_NAME], [MEASURE_DISPLAY_FOLDER] from " +
                "$system.mdschema_measures where [CUBE_NAME] = @CubeName and [MEASURE_IS_VISIBLE]",
                new String[]{"MEASURE_UNIQUE_NAME", "MEASURE_CAPTION", "DATA_TYPE", "MEASUREGROUP_NAME",
                        "MEASURE_DISPLAY_FOLDER"},
                "mdschema_measures",
                new String[]{"CUBE_NAME", "MEASURE_IS_VISIBLE"},
                new DMVParser.DMVRestriction[]{
                        new DMVParser.DMVEqualsRestriction("a"),
                        DMVParser.DMVIfRestriction.getInstance()});

        TestDMV testDMV4 = new TestDMV("select [KPI_NAME], [KPI_CAPTION]," +
                "[MEASUREGROUP_NAME], [KPI_DISPLAY_FOLDER], [KPI_GOAL], [KPI_STATUS], " +
                "[KPI_TREND], [KPI_VALUE] from $system.mdschema_kpis where [CUBE_NAME] = @CubeName",
                new String[]{"KPI_NAME", "KPI_CAPTION", "MEASUREGROUP_NAME", "KPI_DISPLAY_FOLDER",
                        "KPI_GOAL", "KPI_STATUS", "KPI_TREND", "KPI_VALUE"},
                "mdschema_kpis",
                new String[]{"CUBE_NAME"},
                new DMVParser.DMVRestriction[]{new DMVParser.DMVEqualsRestriction("a")});

        TestDMV testDMV5 = new TestDMV("select [DIMENSION_UNIQUE_NAME], [DIMENSION_CAPTION]" +
                "from $system.mdschema_dimensions where [CUBE_NAME] = @CubeName and " +
                "[DIMENSION_UNIQUE_NAME] <> '[Measures]'",
                new String[]{"DIMENSION_UNIQUE_NAME", "DIMENSION_CAPTION"},
                "mdschema_dimensions",
                new String[]{"CUBE_NAME", "DIMENSION_UNIQUE_NAME"},
                new DMVParser.DMVRestriction[]{
                        new DMVParser.DMVEqualsRestriction("a"),
                        new DMVParser.DMVNotEqualsRestriction("[Measures]")});

        TestDMV testDMV6 = new TestDMV("select [DIMENSION_UNIQUE_NAME], " +
                "[HIERARCHY_UNIQUE_NAME], [HIERARCHY_CAPTION], [HIERARCHY_DISPLAY_FOLDER], " +
                "[HIERARCHY_ORIGIN], [HIERARCHY_IS_VISIBLE] from $system.mdschema_hierarchies " +
                "where [CUBE_NAME] = @CubeName and [DIMENSION_UNIQUE_NAME] <> '[Measures]'",
                new String[]{"DIMENSION_UNIQUE_NAME", "HIERARCHY_UNIQUE_NAME", "HIERARCHY_CAPTION",
                "HIERARCHY_DISPLAY_FOLDER", "HIERARCHY_ORIGIN", "HIERARCHY_IS_VISIBLE"},
                "mdschema_hierarchies",
                new String[]{"CUBE_NAME", "DIMENSION_UNIQUE_NAME"},
                new DMVParser.DMVRestriction[]{
                        new DMVParser.DMVEqualsRestriction("a"),
                        new DMVParser.DMVNotEqualsRestriction("[Measures]")});

        TestDMV testDMV7 = new TestDMV("select [DIMENSION_UNIQUE_NAME], " +
                "[HIERARCHY_UNIQUE_NAME], [LEVEL_UNIQUE_NAME], [LEVEL_NUMBER], [LEVEL_CAPTION]" +
                "from $system.mdschema_levels where [CUBE_NAME] = @CubeName and [LEVEL_NAME] <> " +
                "'(All)' and [DIMENSION_UNIQUE_NAME] <> '[Measures]'",
                new String[]{"DIMENSION_UNIQUE_NAME", "HIERARCHY_UNIQUE_NAME", "LEVEL_UNIQUE_NAME",
                "LEVEL_NUMBER", "LEVEL_CAPTION"},
                "mdschema_levels",
                new String[]{"CUBE_NAME", "LEVEL_NAME", "DIMENSION_UNIQUE_NAME"},
                new DMVParser.DMVRestriction[]{
                        new DMVParser.DMVEqualsRestriction("a"),
                        new DMVParser.DMVNotEqualsRestriction("(All)"),
                        new DMVParser.DMVNotEqualsRestriction("[Measures]")});

        testDMVs = new TestDMV[]{testDMV1, testDMV2, testDMV3, testDMV4, testDMV5, testDMV6, testDMV7};

        parameters = new HashMap<>();
        parameters.put("CubeName", "a");
    }

    private void testDMVParser(TestDMV testDMV) {
        DMVParser dmvParser = new DMVParser(testDMV.dmvStatement);
        Assert.assertTrue(dmvParser.find());
        Assert.assertEquals(testDMV.selectColumns, dmvParser.getSelectColumns());
        Assert.assertEquals(testDMV.requestType, dmvParser.getRequestType());
        Assert.assertEquals(testDMV.selectRestrictions, dmvParser.getSelectRestrictions(parameters));
    }

    @Test
    public void testDMVParser() {
        for (TestDMV testDMV : testDMVs) {
            testDMVParser(testDMV);
        }
    }
}
