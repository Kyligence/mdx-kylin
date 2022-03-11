package io.kylin.mdx.web.transfer;

import mondrian.xmla.XmlaRequestContext;
import org.junit.Assert;
import org.junit.Test;

public class TransferRuleManagerTest {

    @Test
    public void testFail() {
        String expect1 = TransferRuleManager.applyAllRules(null, "", false);
        Assert.assertEquals("", expect1);
        String expect2 = TransferRuleManager.applyAllRules("Unknown", "", false);
        Assert.assertEquals("", expect2);
    }

    @Test
    public void test() {
        TransferRuleManager.applyAllRules(XmlaRequestContext.ClientType.SMARTBI, "", false);
        TransferRuleManager.applyAllRules(XmlaRequestContext.ClientType.POWERBI, "", true);
        TransferRuleManager.applyAllRules(XmlaRequestContext.ClientType.POWERBI_DESKTOP, "", true);
        TransferRuleManager.applyAllRules(XmlaRequestContext.ClientType.MSOLAP, "DRILLTHROUGH", true);
        TransferRuleManager.applyAllRules(XmlaRequestContext.ClientType.MSOLAP, "WITH MEMBER [Measures].[TEMP(count)(290714814)(0)] AS 'Count(AddCalculatedMembers([Internet Order Date].[Order Calendar Date-Hierarchy].CurrentMember.Children))',   SOLVE_ORDER = 65535 SELECT {[Measures].[TEMP(count)(290714814)(0)]} DIMENSION PROPERTIES [MEMBER_UNIQUE_NAME],[MEMBER_ORDINAL],[MEMBER_CAPTION] ON COLUMNS, NON EMPTY AddCalculatedMembers([Internet Order Date].[Order Calendar Date-Hierarchy].[CALENDARYEAR].&amp;[2013].Children) DIMENSION PROPERTIES [MEMBER_UNIQUE_NAME],[MEMBER_ORDINAL],[MEMBER_CAPTION] ON ROWS FROM [AdWorks_M2M]", true);
    }

    @Test
    public void testExcelOn1() {
        new XmlaRequestContext();
        String origin = "SELECT\n" +
                "{[Measures].[COUNT_ALL]} ON 0,\n" +
                "NON EMPTY CrossJoin(\n" +
                "Hierarchize(AddCalculatedMembers({DrilldownLevel({[PART].[P_SIZE].&[1]})})),\n" +
                "Hierarchize(AddCalculatedMembers({DrilldownLevel({[PART].[P_COLOR].&[almond]})}))\n" +
                ") ON 1\n" +
                "FROM [BenchMark]";
        String expect = "SELECT" +
                " {[Measures].[COUNT_ALL]} ON 0," +
                " NON EMPTY NonEmptyCrossJoin([PART].[P_SIZE].&[1],[PART].[P_COLOR].&[almond]) ON 1" +
                " FROM [BenchMark]";
        String result = TransferRuleManager.applyAllRules(XmlaRequestContext.ClientType.MSOLAP, origin, false);
        Assert.assertEquals(expect, result);
    }

}