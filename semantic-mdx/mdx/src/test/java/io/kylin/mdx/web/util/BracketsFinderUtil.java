package io.kylin.mdx.web.util;

import org.junit.Assert;
import org.junit.Test;

public class BracketsFinderUtil {

    @Test
    public void BracketsFinderTest() {
        String mdx = "SELECT\n" +
                "\t\t\t\t\t{[Measures].[test_exclude]} DIMENSION PROPERTIES [MEMBER_UNIQUE_NAME],[MEMBER_ORDINAL],[MEMBER_CAPTION] ON COLUMNS,\n" +
                "\t\t\t\t\tNON EMPTY CROSSJOIN( [KYLIN_CAL_DT].[YEAR_OF_CAL_ID].[YEAR_OF_CAL_ID].AllMembers, [SELLER_COUNTRY].[NAME].[NAME].AllMembers) DIMENSION PROPERTIES [MEMBER_UNIQUE_NAME],[MEMBER_ORDINAL],[MEMBER_CAPTION] ON ROWS\n" +
                "\t\t\t\t\tFROM [test_cm]";
        Brackets brackets = BracketsFinder.find(mdx, 9);
        Assert.assertNotNull(brackets);
    }
}
