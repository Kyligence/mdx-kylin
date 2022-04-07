package io.kylin.mdx.insight.core.model.acl;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class AclDimensionTableTest {

    @Test
    public void test() {
        AclDimensionTable dimensionTable = new AclDimensionTable();
        AclHierarchy hierarchy = new AclHierarchy();
        hierarchy.setName("CAL_DT");
        hierarchy.setDimCols(Arrays.asList("YEAR", "MONTH", "WEEK"));
        dimensionTable.setHierarchys(Collections.singletonList(
                hierarchy
        ));
        assertEquals(dimensionTable.getHierarchy("CAL_DT").getDimCols().size(), 3);
    }

}