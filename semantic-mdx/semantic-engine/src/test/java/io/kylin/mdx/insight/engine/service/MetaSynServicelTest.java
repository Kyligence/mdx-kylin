package io.kylin.mdx.insight.engine.service;

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.manager.*;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import io.kylin.mdx.insight.core.service.*;
import io.kylin.mdx.insight.engine.manager.SyncManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetaSynServicelTest {
    @Mock
    private CubeManager cubeManager;

    @Mock
    private GroupManager groupManager;

    @Mock
    private SegmentManager segmentManager;

    @Mock
    private UserService userService;

    @Mock
    private DatasetService datasetService;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private BrokenService brokenService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MetadataService metadataService;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private ModelService modelService;

    @Autowired
    private SyncManager syncManager;

    @InjectMocks
    private MetaSyncServiceImpl metaSyncService;

    @BeforeClass
    public static void before() {
        System.setProperty("converter.mock", "true");
        System.setProperty("MDX_HOME", "src/test/resources/kylin");
        System.setProperty("MDX_CONF", "src/test/resources/kylin/conf");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/access/ProjectInstance/AdventureWorks"), "/ke/ke_access_project.json");
    }


    @Test
    public void fireObserversAsyncTest() {
        metaSyncService.addObserver(syncManager);
        metaSyncService.removeObserver(syncManager);
    }

    @Test
    public void syncCheckTest() {
        Boolean status = metaSyncService.syncCheck();
        assertEquals(status, true);
    }

    // TODO: remove this
    //    @Test
    //    public void loadKiLicenseTest() throws SemanticException {
    //        metaSyncService.loadKiLicense();
    //    }

    @Test
    public void syncProjectsTest() throws SemanticException {
        metaSyncService.syncProjects();
    }

    @Test
    public void syncDatasetTest() throws SemanticException {
        DatasetEntity datasetEntity = DatasetEntity.builder().project("test").dataset("test").build();
        datasetEntity.setStatus("BROKEN");
        KylinGenericModel kylinGenericModel = new KylinGenericModel();
        kylinGenericModel.setModelName("model");
        when(datasetService.getProjectsRelatedDataset()).thenReturn(Arrays.asList("model"));
        when(projectManager.getAllProject()).thenReturn(Sets.newHashSet("model"));
        when(cubeManager.getCubeModelByKylin(any())).thenReturn(Collections.singletonList(kylinGenericModel));
        when(datasetService.selectDatasetsBySearch(any())).thenReturn(Collections.singletonList(datasetEntity));
        metaSyncService.syncDataset();
    }


    @Test
    public void syncCubeTest() throws SemanticException {
        KylinGenericModel kylinGenericModel = new KylinGenericModel();
        kylinGenericModel.setModelName("model");
        when(projectManager.getProjectNamesByCache()).thenReturn(Arrays.asList("test1", "test2"));
        when(cubeManager.getCubeByKylin(any())).thenReturn(Sets.newHashSet("test1, test2"));
        when(cubeManager.getCubeByCache(any())).thenReturn(Sets.newHashSet("test1, test3"));
        when(cubeManager.getCubeModelByKylin(any())).thenReturn(Collections.singletonList(kylinGenericModel));
        metaSyncService.syncCube();
    }

    @Test
    public void syncUserTest() throws SemanticException {
        KylinUserInfo kylinUserInfo = new KylinUserInfo("ADMIN", null);
        when(userService.getUsersByKylin()).thenReturn(Arrays.asList(kylinUserInfo));
        when(userService.getUsersNameByKylin()).thenReturn(Sets.newHashSet("admin1", "admin2"));
        when(userService.getUsersNameByDatabase()).thenReturn(Sets.newHashSet("admin1", "admin3"));
        metaSyncService.syncUser();
    }

    @Test
    public void syncGroupTest() {
        metaSyncService.syncGroup();
    }

    @Test
    public void syncSegmentTest() {
        when(datasetService.getProjectsRelatedDataset()).thenReturn(Arrays.asList("test1", "test2"));
        when(projectManager.getAllProject()).thenReturn(Sets.newHashSet("test1", "test4"));
        when(segmentManager.getSegmentByCache(any())).thenReturn(Sets.newHashSet("test1", "test2"));
        when(segmentManager.getSegmentByKylin(any())).thenReturn(Sets.newHashSet("test1", "test4"));
        when(restTemplate.exchange(
                Matchers.eq("http://127.0.0.1:7080/mdx/xmla/test1/clearCache"),
                Matchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<String>>any())).thenReturn(new ResponseEntity(HttpStatus.OK));
        metaSyncService.syncSegment();
    }

    @Test
    public void clearProjectCacheTestTest() {
        when(restTemplate.exchange(
                Matchers.eq("http://127.0.0.1:7080/mdx/xmla/test/clearCache"),
                Matchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<String>>any())).thenReturn(new ResponseEntity(HttpStatus.OK));
        metaSyncService.clearProjectCache("test");
    }

    @Test
    public void syncProjectAclChangeTestTest() {
        when(projectManager.getAllProject()).thenReturn(Sets.newHashSet("test1", "test4"));
        when(userService.getUsersByProjectFromCache(any())).thenReturn(Arrays.asList("test1", "test4"));
        metaSyncService.syncProjectAclChange();
    }

}
