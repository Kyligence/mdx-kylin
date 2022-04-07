package io.kylin.mdx.insight.core.model.acl;

import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class AclDimensionColTest {

    @Test
    public void test() {
        SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol dimCol =
                new SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol();
        dimCol.setName("MONTH_BEG_DT");
        dimCol.setAlias("MONTH_BEG_DT");
        dimCol.setProperties(Arrays.asList(
                new SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol.AugmentProperty(
                        "QTR_BEG_DT", "QTR_BEG_DT"
                ),
                new SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol.AugmentProperty(
                        "YEAR_BEG_DT", "YEAR_BEG_DT"
                )
        ));
        AclDimensionCol newDimCol = new AclDimensionCol(dimCol);
        assertEquals(newDimCol.getProperties().size(), 2);
    }

}