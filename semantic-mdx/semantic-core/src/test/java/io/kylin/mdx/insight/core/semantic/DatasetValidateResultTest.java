package io.kylin.mdx.insight.core.semantic;

import io.kylin.mdx.insight.core.entity.CustomHierarchy;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.model.generic.ColumnIdentity;
import io.kylin.mdx.insight.core.sync.DatasetValidateResult;
import org.junit.Test;

public class DatasetValidateResultTest {

    @Test
    public void testDatasetValidateResult() {

        DatasetValidateResult datasetValidateResult = new DatasetValidateResult();

        CustomHierarchy customHierarchy = new CustomHierarchy();
        datasetValidateResult.addBrokenHierarchy(customHierarchy, 0x1);

        datasetValidateResult.addBridgeDimTableBroken("model", "table");

        ColumnIdentity columnIdentity = new ColumnIdentity();
        datasetValidateResult.addBrokenManyToManyKey("model", columnIdentity);

        datasetValidateResult.addBrokenModelName("model");

        NamedDimCol namedDimCol = new NamedDimCol();
        datasetValidateResult.addBrokenNameColumn(namedDimCol);

        datasetValidateResult.addBrokenProperties(namedDimCol, "property");
        datasetValidateResult.addBrokenValueColumn(namedDimCol);
        datasetValidateResult.addCommonTableBroken("model", "table");
    }
}
