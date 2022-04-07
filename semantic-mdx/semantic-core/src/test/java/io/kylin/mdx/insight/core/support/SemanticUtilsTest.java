package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.core.entity.DimTableModelRel;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SemanticUtilsTest {

    @Test
    public void testStringUtils() {
        String[] schemaAndTable = SemanticUtils.getSchemaAndTable("DEFAULT.KYLIN_SALES");

        Assert.assertEquals("DEFAULT", schemaAndTable[0]);
        Assert.assertEquals("KYLIN_SALES", schemaAndTable[1]);
    }

    @Test
    public void anyThingChangedInModelsTest() {
        KylinGenericModel kylinGenericModel = new KylinGenericModel();
        boolean result = SemanticUtils.anyThingChangedInModels("test", Collections.singletonList(kylinGenericModel));
        Assert.assertEquals(result, false);
    }

    @Test
    public void convertToDimTableModelRels() {
        NamedDimTable namedDimTable = new NamedDimTable(1);
        List<DimTableModelRel> result = SemanticUtils.convertToDimTableModelRels(Collections.singletonList(namedDimTable));
        Assert.assertEquals(result.size(), 1);
    }


}