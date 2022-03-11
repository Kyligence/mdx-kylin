package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.core.dao.*;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class DatasetServiceTest {
    @Mock
    private NamedDimTableMapper namedDimTableMapper;

    @Mock
    private NamedDimColMapper namedDimColMapper;

    @Mock
    private DimTableModelRelMapper dimTableModelRelMapper;

    @InjectMocks
    private DatasetServiceImpl datasetService;

    @Mock
    private NamedMeasureMapper namedMeasureMapper;

    @Mock
    private DatasetMapper datasetMapper;

    @Test
    public void testDeleteTableLogically() {
        NamedDimTable namedDimTable = new NamedDimTable();
        namedDimTable.setDatasetId(1);
        namedDimTable.setModel("tpc");
        namedDimTable.setDimTable("sale");
        List<NamedDimTable> toDeleteTables = Arrays.asList(namedDimTable);
        datasetService.deleteTableLogically(toDeleteTables);
    }

    @Test
    public void test() {
        datasetService.deleteNamedMeasure(1);
        datasetService.updateDatasetStatusAndBrokenInfo(1, DatasetStatus.NORMAL, "test");
        datasetService.deleteOneDataset(null);
        datasetService.updateNamedMeasure(1, new NamedMeasure());
    }


}
