package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.acl.AclTableModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class MetaStoreTest {

    @Test
    public void testUserGroupStore() {
        MetaStore metaStore = MetaStore.getInstance();
        KylinUserInfo userInfo1 = new KylinUserInfo("Admin", KylinUserInfo.as("All", "Dev", "Ops"));
        KylinUserInfo userInfo2 = new KylinUserInfo("Test", KylinUserInfo.as("All"));
        KylinUserInfo userInfo3 = new KylinUserInfo("root", new ArrayList<>());
        metaStore.syncUserAndGroup(Arrays.asList(userInfo1, userInfo2, userInfo3));
        // 验证结果
        assertEquals(metaStore.getGroupsByUser("ADMIN"), Arrays.asList("All", "Dev", "Ops"));
        assertEquals(metaStore.getGroupsByUser("Root"), Collections.emptyList());
        assertEquals(metaStore.getGroupsByUser("Ops"), Collections.emptyList());
        assertEquals(metaStore.getGroupsByUser(null), Collections.emptyList());
        assertEquals(metaStore.getUsersByGroup("Dev"), Collections.singletonList("ADMIN"));
        assertEquals(metaStore.getUsersByGroup("All"), Arrays.asList("ADMIN", "TEST"));
        assertEquals(metaStore.getUsersByGroup("Empty"), Collections.emptyList());
        assertEquals(metaStore.getUsersByGroup(null), Collections.emptyList());
        assertEquals(metaStore.getOriginalName("ADMIN"), "Admin");
        assertEquals(metaStore.getOriginalName("admin"), "Admin");
        assertEquals(metaStore.getOriginalName("NoUser"), "NoUser");
        assertNull(metaStore.getOriginalName(null));
    }

    @Test
    public void testProjectAclStore() {
        MetaStore metaStore = MetaStore.getInstance();
        AclProjectModel projectModel = new AclProjectModel("user", "Admin", "project");
        projectModel.setModel("table", new AclTableModel("table"));
        metaStore.recordAclProjectModel(projectModel);
        assertEquals(metaStore.getAllUserOnProject("project"), Collections.singleton("Admin"));
        AclProjectModel newModel = metaStore.getAclProjectModel("Admin", "project");
        assertEquals(projectModel, newModel);
    }

    @Test
    public void testRefreshSchema() {
        MetaStore metaStore = MetaStore.getInstance();
        metaStore.addForceRefreshSchema("project", "user");
        assertTrue(metaStore.isForceRefreshSchema("project", "user"));
        assertFalse(metaStore.isForceRefreshSchema("project", "user"));
    }

}