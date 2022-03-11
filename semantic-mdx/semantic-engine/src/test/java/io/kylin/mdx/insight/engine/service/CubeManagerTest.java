package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.engine.manager.CubeManagerImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CubeManagerTest {
    @Mock
    private SemanticAdapter semanticAdapter;

    @InjectMocks
    private CubeManagerImpl cubeManager;

    @Mock
    private ModelService modelService;

    @Test
    public void test() {
        String project = "tpc";
        when(semanticAdapter.getNoCacheCubeNames(project)).thenReturn(Arrays.asList("a"));
        cubeManager.getCubeByKylin(project);
        KylinGenericModel kylinGenericModel = new KylinGenericModel();
        kylinGenericModel.setModelName("test");
        when(modelService.getCachedGenericModels(project)).thenReturn(Arrays.asList(kylinGenericModel));
        cubeManager.getCubeByCache(project);
        cubeManager.getCubeModelByCache(project);
        when(semanticAdapter.getNocacheGenericModels(project)).thenReturn(Arrays.asList(kylinGenericModel));
        cubeManager.getCubeModelByKylin(project);
    }

}
