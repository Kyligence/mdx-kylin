package io.kylin.mdx.insight.mondrian.query;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.engine.bean.SimpleSchema;
import io.kylin.mdx.insight.engine.service.parser.DefaultMemberValidatorImpl;
import io.kylin.mdx.insight.engine.support.ExprParseException;
import mondrian.parser.TokenMgrError;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

public class DefaultMemberTest extends BaseEnvSetting {
    private DefaultMemberValidatorImpl defaultMemberValidatorImpl;
    private SimpleSchema simpleSchema;

    @Before
    public void before() {
        simpleSchema = new SimpleSchema();
        defaultMemberValidatorImpl = new DefaultMemberValidatorImpl();

        SimpleSchema.DimensionTable dimensionTable = new SimpleSchema.DimensionTable();
        dimensionTable.setAlias("KYLIN_CAL_DT");
        dimensionTable.setTableColAliases(Collections.singleton(new SimpleSchema.DimensionCol("YEAR_BEG_DT", 0)));

        Set<SimpleSchema.DimensionTable> dimensionTables = Collections.singleton(dimensionTable);
        simpleSchema.setDimensionTables(dimensionTables);
    }

    @Test
    public void testCorrectDefaultMembers() throws ExprParseException {
        testOneCase("[KYLIN_CAL_DT].[YEAR_BEG_DT].&[2019]", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT");
        testOneCase("[KYLIN_CAL_DT].[YEAR_BEG_DT].[YEAR_BEG_DT].&[2019]", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT");

        testOneCase("[KYLIN_CAL_DT].[YEAR_BEG_DT].&[2019].firstsibling", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT");
        testOneCase("iif(1 > 0, [KYLIN_CAL_DT].[YEAR_BEG_DT].&[2019].lastsibling, [KYLIN_CAL_DT].[YEAR_BEG_DT].&[2019].firstsibling)", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT");
    }

    @Test
    public void testWrongDefaultMembers() throws ExprParseException {
        testOneCase("aaa]", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT", TokenMgrError.class);
        testOneCase("bbb", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT", ExprParseException.class);
        testOneCase("[g4wff4].[$]", "any_cube.KYLIN_CAL_DT.YEAR_BEG_DT", ExprParseException.class);
    }

    private void testOneCase(String defaultMember,
                             String dimensionPath) throws ExprParseException {
        testOneCase(defaultMember, dimensionPath, null);
    }

    private void testOneCase(String defaultMember,
                             String dimensionPath,
                             Class<? extends Throwable> throwableClass) throws ExprParseException {
        try {
            defaultMemberValidatorImpl.validateDefaultMember(defaultMember, dimensionPath, simpleSchema);
        } catch (Throwable t) {
            if (throwableClass == null || !throwableClass.isInstance(t)) {
                throw t;
            }
        }
    }
}
