package io.kylin.mdx.web.transfer.rule.excel;

import org.junit.Assert;
import org.junit.Test;

public class ExcelQueryPartOptimizeRuleTest {

    @Test
    public void validBracket() {
        ExcelQueryPartOptimizeRule rule = new ExcelQueryPartOptimizeRule();
        // 基础验证
        Assert.assertTrue(rule.validBracket(""));
        Assert.assertFalse(rule.validBracket("("));
        Assert.assertFalse(rule.validBracket(")"));
        Assert.assertFalse(rule.validBracket("(("));
        Assert.assertTrue(rule.validBracket("()"));
        Assert.assertFalse(rule.validBracket("))"));
        Assert.assertFalse(rule.validBracket("(()"));
        Assert.assertTrue(rule.validBracket("(())"));
        Assert.assertFalse(rule.validBracket("(((("));
        Assert.assertFalse(rule.validBracket("((()"));
        Assert.assertFalse(rule.validBracket(")())"));
        Assert.assertTrue(rule.validBracket("(({))}"));
        // MDX 验证
        Assert.assertTrue(rule.validBracket("SELECT " +
                "{[Measures].[_COUNT_],[Measures].[SUM_PRICE],[Measures].[MAX_PRICE],[Measures].[MIN_PRICE]} DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS , " +
                "NON EMPTY Hierarchize(AddCalculatedMembers({DrillDownLevelTop(DrilldownMember({{DrilldownLevel({[KYLIN_CAL_DT].[cal_dt-Hierarchy].[All]})}}, {[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT].&[2012-01-01]}),10,[KYLIN_CAL_DT].[cal_dt-Hierarchy].[YEAR_BEG_DT],[Measures].[_COUNT_])})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON ROWS  " +
                "FROM [learn_kylin] CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS"));
    }

}