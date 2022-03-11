package io.kylin.mdx.insight.core.service;

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.dao.CommonDimRelationMapper;
import io.kylin.mdx.insight.core.entity.CommonDimRelation;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.engine.service.BrokenServiceImpl;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class BrokenServiceTest extends BaseEnvSetting {
    @Mock
    private DatasetService datasetService;

    @Mock
    private CommonDimRelationMapper commonDimRelationMapper;

    @InjectMocks
    private BrokenServiceImpl brokenService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetDatasetsBroken() {
        String project = "TPC";
        Set<String> deletedCubeNames = Sets.newHashSet("model", "modelRelated");
        List<CommonDimRelation> commonDimRelations = new ArrayList<>();
        CommonDimRelation commonDimRelation = new CommonDimRelation();
        commonDimRelation.setDatasetId(1);
        commonDimRelation.setRelation("relation");
        commonDimRelation.setModelRelated("modelRelated");
        commonDimRelation.setId(1);
        commonDimRelation.setModel("model");
        commonDimRelations.add(commonDimRelation);

        when(datasetService.updateDatasetStatusAndBrokenInfo(anyInt(), any(), anyString())).thenReturn(1);
        when(commonDimRelationMapper.selectDimRelationsWithProject(project, deletedCubeNames)).thenReturn(commonDimRelations);
        brokenService.setDatasetsBroken(project, deletedCubeNames);
    }

    @Test
    public void testRecoverOneDatasetNormal() {
        when(datasetService.updateDatasetStatusAndBrokenInfo(any(), any(), any())).thenReturn(1);
        brokenService.recoverOneDatasetNormal(1);
    }
    @Test
    public void testTryRecoverDataset() {
        String project = "TPC";
        Set<String> cubeNames = Sets.newHashSet("model", "modelRelated");
        List<DatasetEntity> brokenDatasets = new ArrayList<>();
        DatasetEntity entityO = new DatasetEntity();
        entityO.setId(1);
        entityO.setBrokenMsg("{\"dimension_cols\":[{\"datasetBrokenType\":\"MANY_TO_MANY_KEY_DELETED\",\"name\":\"model.sales.id\"}],\"dimension_tables\":[{\"datasetBrokenType\":\"COMMON_TABLE_DELETED\",\"name\":\"modelF.modelFour\"},{\"datasetBrokenType\":\"BRIDGE_TABLE_DELETED\",\"name\":\"modelX.modelSix\"}],\"hierarchys\":[{\"datasetBrokenType\":\"HIERARCHY_DIM_COL_DELETED\",\"name\":\"model.table.hierarchy\",\"obj\":\"model.table.column.weightColumn\"}],\"models\":[{\"datasetBrokenType\":\"MODEL_DELETED\",\"name\":\"modelT\"},{\"datasetBrokenType\":\"MODEL_DELETED\",\"name\":\"modelO\"},{\"datasetBrokenType\":\"MODEL_DELETED\",\"name\":\"modelS\"}]}");
        brokenDatasets.add(entityO);
        DatasetEntity entityT = new DatasetEntity();
        entityO.setId(2);
        brokenDatasets.add(entityT);
        when(datasetService.selectDatasetsBySearch(any())).thenReturn(brokenDatasets);
        brokenService.tryRecoverDataset(project, cubeNames);

        Set<String> cubeNamesOther = Sets.newHashSet("modelT", "modelS", "modelO");
        brokenService.tryRecoverDataset(project, cubeNamesOther);

    }
}
