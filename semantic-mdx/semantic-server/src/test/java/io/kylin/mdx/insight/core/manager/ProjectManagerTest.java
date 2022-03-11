package io.kylin.mdx.insight.core.manager;

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.acl.AclTableModel;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.sync.MetaStore;
import io.kylin.mdx.insight.engine.manager.ProjectManagerImpl;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class ProjectManagerTest extends BaseEnvSetting {

    @Mock
    private ModelService modelService;

    @Mock
    private MetaStore metaStore;

    @Mock
    private SemanticAdapter semanticAdapter;

    @InjectMocks
    private ProjectManagerImpl projectManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() {
        ProjectManagerImpl.setAllProject(Sets.newHashSet("tpc"));
        AclProjectModel model = new AclProjectModel("type", "tpc", "tpc");
        projectManager.getProjectNamesByCache();
        AclTableModel aclTableModel = new AclTableModel();
        aclTableModel.setTable("sales");
        model.setModel("sales", aclTableModel);
        when(metaStore.getAclProjectModel(anyString(), anyString())).thenReturn(model);
        AclProjectModel modelNew = new AclProjectModel("type", "tpc", "tpc");
        AclTableModel aclTableModelNew = new AclTableModel();
        aclTableModelNew.setTable("sales");
        aclTableModelNew.setInvisible(true);
        modelNew.setModel("sales", aclTableModelNew);
        when(semanticAdapter.getAclModel(anyString(), anyString(), anyString(), anyList())).thenReturn(modelNew);
        projectManager.doProjectAcl("admin", "tpc");
    }

    @Test
    public void testVerifyProjectListChange() {
        when(semanticAdapter.getActualProjectSet(any())).thenReturn(Sets.newHashSet("ab", "ef"));
        ProjectManagerImpl.setAllProject(Sets.newHashSet("ab", "cd"));
        projectManager.verifyProjectListChange();
    }

    @Test
    public void testVerifyProjectListChangeDiff() {
        when(semanticAdapter.getActualProjectSet(any())).thenReturn(Sets.newHashSet("ab"));
        ProjectManagerImpl.setAllProject(Sets.newHashSet("ab", "cd"));
        projectManager.verifyProjectListChange();
    }

    @Test
    public void testGetUserAccessProjects() {
        boolean isConvertorMock = SemanticConfig.getInstance().isDisableAsyncHttpCall();
        SemanticConfig.getInstance().getProperties().put("insight.semantic.http.async.disable", "true");
        when(semanticAdapter.getActualProjectSet(any())).thenReturn(Sets.newHashSet("ab"));
        when(semanticAdapter.getAccessInfo(any())).thenReturn("accessInfo");
        projectManager.getUserAccessProjects(new ConnectionInfo());
        SemanticConfig.getInstance().getProperties().put("insight.semantic.http.async.disable", isConvertorMock);

    }
}
