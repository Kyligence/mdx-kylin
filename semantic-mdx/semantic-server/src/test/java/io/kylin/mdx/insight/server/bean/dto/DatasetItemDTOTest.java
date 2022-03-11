package io.kylin.mdx.insight.server.bean.dto;

import org.junit.Assert;
import org.junit.Test;

public class DatasetItemDTOTest {
    @Test
    public void dataSetItemEquals() {
        DatasetItemDTO datasetItemDTO = new DatasetItemDTO("1", "1");
        DatasetItemDTO datasetItemDTO1 = new DatasetItemDTO("1", "1");
        datasetItemDTO.equals(datasetItemDTO1);
        Assert.assertEquals(datasetItemDTO.hashCode(), datasetItemDTO1.hashCode());
    }
}
